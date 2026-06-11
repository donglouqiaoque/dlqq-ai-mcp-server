package com.donglouqiaoque.aitools.mcp.tools;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

/**
 * BLE蓝牙低功耗工具
 * 提供网络接口供AI连接控制
 *
 * 支持跨平台，使用Python bleak库通过Socket通信
 */
public class BleTools {

    // 多实例管理 - 每个端口对应一个实例
    private static final Map<Integer, BleTools> instances = new ConcurrentHashMap<>();

    // 服务器
    private ServerSocket serverSocket;
    private Thread serverThread;
    private boolean isRunning = false;
    private int serverPort = 0;

    // BLE管理器（用于真实BLE通信）
    private BleManager bleManager;
    private boolean useRealBle = false;  // 标记是否使用真实BLE

    // BLE连接状态
    private boolean isConnected = false;
    private String connectedDeviceAddress = null;
    private String connectedDeviceName = null;

    // 扫描到的设备列表 (address -> name)
    private final Map<String, String> scannedDevices = new ConcurrentHashMap<>();
    private boolean isScanning = false;

    // 服务和特征缓存
    private final Map<String, BleService> discoveredServices = new ConcurrentHashMap<>();

    // 数据缓冲区
    private final List<String> dataBuffer;
    private int maxBufferSize = 5 * 1024 * 1024; // 默认5MB
    private int currentBufferSize = 0;
    private long totalReceivedBytes = 0;
    private long totalSentBytes = 0;
    private int lineCounter = 0;

    // GUI窗口
    private JFrame guiWindow = null;
    private JTextArea logArea;
    private JLabel statusLabel;
    private JComboBox<String> deviceCombo;
    private JButton connectButton;
    private JButton scanButton;
    private StatusIcon statusIcon;
    
    // 设备列表显示
    private JList<String> deviceList;
    private DefaultListModel<String> deviceListModel;

    // 显示格式控制（文本/HEX）
    private boolean displayAsHex = false;

    // 线程池
    private final ExecutorService executor;

    // BLE服务/特征数据结构
    public static class BleService {
        public final String uuid;
        public final String name;
        public final Map<String, BleCharacteristic> characteristics = new HashMap<>();

        public BleService(String uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }
    }

    public static class BleCharacteristic {
        public final String uuid;
        public final String name;
        public final List<String> properties; // read, write, notify, indicate

        public BleCharacteristic(String uuid, String name, List<String> properties) {
            this.uuid = uuid;
            this.name = name;
            this.properties = properties;
        }
    }

    private final int instancePort;
    private final int pythonServicePort;

    private BleTools(int javaPort, int pythonPort) {
        this.instancePort = javaPort;
        this.pythonServicePort = pythonPort;
        this.dataBuffer = Collections.synchronizedList(new ArrayList<>());
        this.executor = Executors.newCachedThreadPool();

        // 为每个实例创建独立的BLE管理器，Python使用指定端口
        this.bleManager = new BleManager(pythonServicePort);
    }

    /**
     * 获取指定端口的实例（使用默认Python端口）
     */
    public static synchronized BleTools getInstance(int port) {
        return instances.computeIfAbsent(port, k -> new BleTools(k, k + 10000));
    }

    /**
     * 获取指定端口的实例（指定Python端口）
     */
    public static synchronized BleTools getInstance(int javaPort, int pythonPort) {
        return instances.computeIfAbsent(javaPort, k -> new BleTools(k, pythonPort));
    }

    /**
     * 检查指定端口的实例是否正在运行
     * 使用端口占用检测，支持跨进程判断
     */
    public static boolean isInstanceRunning(int port) {
        // 先检查内存中的实例
        BleTools tools = instances.get(port);
        if (tools != null && tools.isRunning) {
            return true;
        }
        // 跨进程检测：尝试连接端口，如果成功则说明实例在运行
        try (Socket socket = new Socket("127.0.0.1", port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 检查指定端口的实例是否存在
     */
    public static boolean hasInstance(int port) {
        return instances.containsKey(port);
    }

    /**
     * 验证端口号参数
     */
    private static String validatePort(int port) {
        if (port < 1 || port > 65535) {
            return "错误：端口号必须在 1~65535 范围内";
        }
        return null;
    }

    /**
     * 验证端口号参数（用于工具方法）
     */
    public static String checkPortParam(Integer port) {
        if (port == null) {
            return "错误：缺少 port 参数，请指定BLE工具实例端口号。使用 dlqq_run_ble 启动BLE工具";
        }
        return validatePort(port);
    }

    // ==================== 工具方法（供MCP调用） ====================

    /**
     * 检查Python BLE库是否已安装
     * @return 库状态信息
     */
    public static String bleLibHas() {
        StringBuilder sb = new StringBuilder();
        sb.append("Python BLE库状态:\n\n");

        // 检查Python
        boolean hasPython = BleManager.hasPython();
        String pythonVersion = BleManager.getPythonVersion();
        sb.append("Python: ").append(hasPython ? "已安装" : "未安装");
        if (pythonVersion != null) {
            sb.append(" (").append(pythonVersion).append(")");
        }
        sb.append("\n\n");

        if (!hasPython) {
            sb.append("请先安装Python 3.8+ 并确保python命令可用\n");
            return sb.toString();
        }

        // 检查库
        Map<String, BleManager.LibraryInfo> libs = BleManager.checkLibraries();
        boolean allInstalled = true;

        for (Map.Entry<String, BleManager.LibraryInfo> entry : libs.entrySet()) {
            String name = entry.getKey();
            BleManager.LibraryInfo info = entry.getValue();

            sb.append(name).append(": ");
            if (info.installed) {
                sb.append("已安装");
                if (info.currentVersion != null) {
                    sb.append(" (版本 ").append(info.currentVersion).append(")");
                }
                if (info.needsUpdate()) {
                    sb.append(" [可更新到 ").append(info.latestVersion).append("]");
                }
            } else {
                sb.append("未安装");
                allInstalled = false;
            }
            sb.append("\n");
        }

        sb.append("\n");
        if (allInstalled) {
            sb.append("✓ 所有库已就绪，可以使用BLE功能");
        } else {
            sb.append("✗ 部分库未安装，请使用 dlqq_ble_lib_install 安装");
        }

        return sb.toString();
    }

    /**
     * 安装Python BLE库
     * @return 安装结果
     */
    public static String bleLibInstall() {
        StringBuilder sb = new StringBuilder();

        // 检查Python
        if (!BleManager.hasPython()) {
            return "错误：未找到Python。请先安装Python 3.8+ (https://python.org)";
        }

        sb.append("开始安装BLE依赖库...\n");
        sb.append("Python: ").append(BleManager.getPythonVersion()).append("\n\n");

        // 执行安装
        BleManager.InstallResult result = BleManager.installLibraries(msg -> {
            sb.append(msg).append("\n");
        });

        sb.append("\n").append(result.message).append("\n");

        if (result.packageStatus != null) {
            for (Map.Entry<String, Boolean> entry : result.packageStatus.entrySet()) {
                sb.append("  - ").append(entry.getKey()).append(": ")
                  .append(entry.getValue() ? "✓ 成功" : "✗ 失败").append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * 检查并更新Python BLE库
     * 如果有更新可用，弹出对话框让用户选择
     * @return 检查结果
     */
    public static String bleLibUpdate() {
        StringBuilder sb = new StringBuilder();
        sb.append("检查BLE库更新...\n\n");

        // 检查Python
        if (!BleManager.hasPython()) {
            return "错误：未找到Python。请先安装Python 3.8+";
        }

        // 检查更新
        Map<String, Boolean> updates = BleManager.checkUpdates();
        Map<String, BleManager.LibraryInfo> libs = BleManager.checkLibraries();

        boolean hasUpdate = false;
        List<String> updatablePackages = new ArrayList<>();

        for (Map.Entry<String, Boolean> entry : updates.entrySet()) {
            String pkgName = entry.getKey();
            boolean needsUpdate = entry.getValue();
            BleManager.LibraryInfo info = libs.get(pkgName);

            if (info != null) {
                sb.append(pkgName).append(": ");
                if (info.installed) {
                    sb.append("当前版本 ").append(info.currentVersion);
                    if (needsUpdate) {
                        sb.append(" → 最新版本 ").append(info.latestVersion);
                        hasUpdate = true;
                        updatablePackages.add(pkgName);
                    } else {
                        sb.append(" (已是最新)");
                    }
                } else {
                    sb.append("未安装");
                }
                sb.append("\n");
            }
        }

        if (!hasUpdate) {
            sb.append("\n✓ 所有库都是最新版本");
            return sb.toString();
        }

        sb.append("\n发现更新可用！\n");

        // 弹出确认对话框
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] userConfirmed = {false};

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("BLE库更新");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);

            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            JLabel label = new JLabel("<html>发现BLE库更新可用！<br><br>" +
                    "更新内容：" + String.join(", ", updatablePackages) + "<br><br>是否立即更新？</html>");
            panel.add(label, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel();
            JButton yesButton = new JButton("立即更新");
            JButton noButton = new JButton("稍后");

            yesButton.addActionListener(e -> {
                userConfirmed[0] = true;
                frame.dispose();
                latch.countDown();
            });

            noButton.addActionListener(e -> {
                userConfirmed[0] = false;
                frame.dispose();
                latch.countDown();
            });

            buttonPanel.add(yesButton);
            buttonPanel.add(noButton);
            panel.add(buttonPanel, BorderLayout.SOUTH);

            frame.add(panel);
            frame.setVisible(true);
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (userConfirmed[0]) {
            sb.append("\n用户选择：立即更新\n\n");
            BleManager.InstallResult result = BleManager.installLibraries(msg -> {
                sb.append(msg).append("\n");
            });
            sb.append("\n").append(result.message);
        } else {
            sb.append("\n用户选择：稍后更新");
        }

        return sb.toString();
    }


    /**
     * 释放Python脚本到项目目录下的.dlqq文件夹
     * @return 释放结果
     */
    public static String bleReleaseScript() {
        StringBuilder sb = new StringBuilder();
        sb.append("释放BLE Python脚本...\n\n");

        try {
            // 获取项目目录（工作目录）
            Path projectDir = Paths.get(System.getProperty("user.dir"));
            Path dlqqDir = projectDir.resolve(".dlqq");
            Path pythonDir = dlqqDir.resolve("python");
            Path scriptFile = pythonDir.resolve("ble_service.py");

            sb.append("项目目录: ").append(projectDir.toAbsolutePath()).append("\n");
            sb.append("目标目录: ").append(pythonDir.toAbsolutePath()).append("\n\n");

            // 检查并创建 .dlqq 目录
            if (!Files.exists(dlqqDir)) {
                Files.createDirectories(dlqqDir);
                sb.append("✓ 创建目录: .dlqq\n");
            }

            // 检查并创建 python 子目录
            if (!Files.exists(pythonDir)) {
                Files.createDirectories(pythonDir);
                sb.append("✓ 创建目录: .dlqq/python\n");
            }

            // 从资源中提取脚本
            String scriptResourcePath = "/py/ble_service.py";
            try (InputStream is = BleTools.class.getResourceAsStream(scriptResourcePath)) {
                if (is == null) {
                    sb.append("✗ 错误：找不到资源文件 ").append(scriptResourcePath).append("\n");
                    return sb.toString();
                }
                Files.copy(is, scriptFile, StandardCopyOption.REPLACE_EXISTING);
            }

            sb.append("✓ Python脚本已释放到: ").append(scriptFile.toAbsolutePath()).append("\n\n");
            sb.append("脚本大小: ").append(Files.size(scriptFile)).append(" 字节\n");

            // 验证文件是否存在
            if (Files.exists(scriptFile)) {
                sb.append("\n✓ 释放成功！\n");
                sb.append("后续BLE工具将优先从此目录加载Python脚本。");
            } else {
                sb.append("\n✗ 释放失败：文件未找到\n");
            }

        } catch (Exception e) {
            sb.append("\n✗ 释放失败: ").append(e.getMessage()).append("\n");
            e.printStackTrace();
        }

        return sb.toString();
    }
    /**
     * 1. 运行BLE工具 - 打开BLE工具窗口
     *
     * @param port Java服务器端口号
     * @param pythonPort Python服务端口号（可选，默认为Java端口+10000）
     * @return 服务器启动结果
     */
    public static String runBle(int port, Integer pythonPort) {
        String portError = validatePort(port);
        if (portError != null) {
            return portError;
        }

        // 如果未指定Python端口，使用默认规则
        if (pythonPort == null) {
            pythonPort = port + 10000;
        }

        // 验证Python端口
        if (pythonPort < 1 || pythonPort > 65535) {
            return "错误：Python端口号必须在 1~65535 范围内";
        }

        if (isInstanceRunning(port)) {
            return "BLE工具已在运行中，端口: " + port;
        }

        // 检查是否已有其他端口的实例在运行
        for (Map.Entry<Integer, BleTools> entry : instances.entrySet()) {
            if (entry.getValue().isRunning && entry.getKey() != port) {
                return "错误：端口 " + entry.getKey() + " 的BLE工具实例正在运行，请先关闭该实例后再启动新实例";
            }
        }

        BleTools tools = getInstance(port, pythonPort);

        try {
            // 启动BLE管理器(Python服务)
            if (!tools.bleManager.initialize()) {
                instances.remove(port);
                return "启动BLE工具失败: Python BLE服务启动失败，请检查Python和bleak库是否已安装";
            }

            tools.startServer(port);
            // 同时打开GUI窗口
            SwingUtilities.invokeLater(() -> tools.createGuiWindow());
            return "BLE工具已启动，Java端口: " + port + "，Python端口: " + pythonPort + "，请使用 localhost:" + port + " 连接";
        } catch (Exception e) {
            // 启动失败，移除实例
            instances.remove(port);
            return "启动BLE工具失败: " + e.getMessage();
        }
    }

    /**
     * 2. 扫描BLE设备
     *
     * @param port 服务器端口号
     * @param timeoutMs 扫描超时时间（毫秒，默认10000）
     * @return 扫描结果
     */
    public static String scanBleDevices(int port, int timeoutMs) {
        String portError = checkPortParam(port);
        if (portError != null) {
            return portError;
        }

        if (!isInstanceRunning(port)) {
            return "错误：端口 " + port + " 的BLE工具实例未启动，请先使用 dlqq_run_ble 启动";
        }

        BleTools tools = getInstance(port);

        if (tools.isScanning) {
            return "扫描正在进行中，请稍候...";
        }

        // 清空之前的扫描结果
        tools.scannedDevices.clear();
        
        // 清空GUI设备列表
        if (tools.deviceListModel != null) {
            SwingUtilities.invokeLater(() -> tools.deviceListModel.clear());
        }

        // 设置扫描标志
        tools.isScanning = true;
        tools.addToBuffer("SYS", "开始扫描BLE设备，超时: " + timeoutMs + "ms");

        // 启动真实BLE扫描
        tools.executor.submit(() -> {
            try {
                // 使用Windows BLE管理器进行真实扫描
                tools.bleManager.startScan(new BleManager.ScanCallback() {
                    @Override
                    public void onDeviceFound(BleManager.BleDevice device) {
                        tools.scannedDevices.put(device.getAddress(), device.getName());
                        String deviceInfo = device.getName() + " [" + device.getAddress() + "] " + device.getRssi() + " dBm";
                        tools.addToBuffer("SYS", "发现设备: " + deviceInfo);
                        
                        // 更新GUI设备列表
                        if (tools.deviceListModel != null) {
                            SwingUtilities.invokeLater(() -> {
                                // 检查是否已存在
                                boolean exists = false;
                                for (int i = 0; i < tools.deviceListModel.size(); i++) {
                                    if (tools.deviceListModel.get(i).contains(device.getAddress())) {
                                        exists = true;
                                        break;
                                    }
                                }
                                if (!exists) {
                                    tools.deviceListModel.addElement(deviceInfo);
                                }
                            });
                        }
                    }

                    @Override
                    public void onScanComplete() {
                        tools.isScanning = false;
                        tools.addToBuffer("SYS", "扫描完成，共发现 " + tools.scannedDevices.size() + " 个设备");
                        
                        // 更新设备列表标题
                        if (tools.deviceList != null && tools.guiWindow != null) {
                            SwingUtilities.invokeLater(() -> {
                                JPanel parent = (JPanel) tools.deviceList.getParent().getParent();
                                if (parent.getBorder() instanceof TitledBorder) {
                                    ((TitledBorder) parent.getBorder()).setTitle("扫描到的设备列表 (" + tools.scannedDevices.size() + ")");
                                    parent.repaint();
                                }
                            });
                        }
                    }

                    @Override
                    public void onScanFailed(String error) {
                        tools.isScanning = false;
                        tools.addToBuffer("SYS", "扫描失败: " + error);
                    }
                }, timeoutMs);
                
                // 等待扫描完成
                while (tools.isScanning && tools.bleManager.isScanning()) {
                    Thread.sleep(100);
                }
            } catch (Exception e) {
                tools.addToBuffer("SYS", "扫描异常: " + e.getMessage());
            } finally {
                tools.isScanning = false;
            }
        });

        return "已开始扫描BLE设备，扫描时间: " + timeoutMs + "ms，请使用 dlqq_ble_get_devices 查看结果";
    }

    /**
     * 2a. 获取扫描到的设备列表
     *
     * @param port 服务器端口号
     * @return 设备列表
     */
    public static String getBleDevices(int port) {
        String portError = checkPortParam(port);
        if (portError != null) {
            return portError;
        }

        if (!isInstanceRunning(port)) {
            return "错误：端口 " + port + " 的BLE工具实例未启动";
        }

        BleTools tools = getInstance(port);

        StringBuilder sb = new StringBuilder();
        sb.append("扫描到的BLE设备:\n");

        if (tools.scannedDevices.isEmpty()) {
            sb.append("  未发现BLE设备\n");
        } else {
            int index = 1;
            for (Map.Entry<String, String> entry : tools.scannedDevices.entrySet()) {
                sb.append(String.format("  %d. %s - %s", index++, entry.getKey(), entry.getValue()));
                if (entry.getKey().equals(tools.connectedDeviceAddress)) {
                    sb.append(" [已连接]");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * 3. 连接BLE设备
     *
     * @param port 服务器端口号
     * @param deviceAddress 设备地址（MAC地址或UUID）
     * @return 连接结果
     */
    public static String connectBleDevice(int port, String deviceAddress) {
        String portError = checkPortParam(port);
        if (portError != null) {
            return portError;
        }

        if (!isInstanceRunning(port)) {
            return "错误：端口 " + port + " 的BLE工具实例未启动";
        }

        BleTools tools = getInstance(port);

        if (tools.isConnected) {
            return "错误：已有设备连接，请先断开当前连接";
        }

        if (deviceAddress == null || deviceAddress.trim().isEmpty()) {
            return "错误：设备地址不能为空";
        }

        // 使用Windows BLE管理器进行真实连接
        String deviceName = tools.scannedDevices.getOrDefault(deviceAddress, "Unknown Device");
        final String[] resultMessage = {null};
        
        boolean connected = tools.bleManager.connect(deviceAddress, new BleManager.ConnectionCallback() {
            @Override
            public void onConnected(BleManager.BleDevice device) {
                tools.isConnected = true;
                tools.connectedDeviceAddress = deviceAddress;
                tools.connectedDeviceName = device.getName();
                resultMessage[0] = "已连接到BLE设备: " + device.getName() + " [" + deviceAddress + "]";
                tools.addToBuffer("SYS", resultMessage[0]);
            }
            
            @Override
            public void onDisconnected(BleManager.BleDevice device) {
                tools.addToBuffer("SYS", "设备断开连接: " + device.getName());
            }
            
            @Override
            public void onConnectionFailed(String address, String error) {
                resultMessage[0] = "连接失败: " + error;
                tools.addToBuffer("SYS", resultMessage[0]);
            }
        });
        
        if (!connected) {
            return "连接失败";
        }

        // 更新GUI
        SwingUtilities.invokeLater(() -> {
            if (tools.connectButton != null) {
                tools.connectButton.setText("断开连接");
            }
            if (tools.statusIcon != null) {
                tools.statusIcon.setOpen(true);
            }
        });

        return resultMessage[0] != null ? resultMessage[0] : "已连接到BLE设备: " + deviceName + " [" + deviceAddress + "]";
    }

    /**
     * 4. 断开BLE设备连接
     *
     * @param port 服务器端口号
     * @return 断开结果
     */
    public static String disconnectBleDevice(int port) {
        String portError = checkPortParam(port);
        if (portError != null) {
            return portError;
        }

        if (!isInstanceRunning(port)) {
            return "错误：端口 " + port + " 的BLE工具实例未启动";
        }

        BleTools tools = getInstance(port);

        if (!tools.isConnected) {
            return "错误：当前没有连接的设备";
        }

        String deviceName = tools.connectedDeviceName;
        String deviceAddress = tools.connectedDeviceAddress;

        // 使用Windows BLE管理器断开连接
        tools.bleManager.disconnect();
        
        tools.isConnected = false;
        tools.connectedDeviceAddress = null;
        tools.connectedDeviceName = null;
        tools.discoveredServices.clear();

        tools.addToBuffer("SYS", "已断开与设备的连接: " + deviceName + " [" + deviceAddress + "]");

        // 更新GUI
        SwingUtilities.invokeLater(() -> {
            if (tools.connectButton != null) {
                tools.connectButton.setText("连接设备");
            }
            if (tools.statusIcon != null) {
                tools.statusIcon.setOpen(false);
            }
        });

        return "已断开BLE设备: " + deviceName + " [" + deviceAddress + "]";
    }

    /**
     * 5. 发现BLE服务和特征
     *
     * @param port 服务器端口号
     * @return 发现结果
     */
    public static String discoverBleServices(int port) {
        String portError = checkPortParam(port);
        if (portError != null) {
            return portError;
        }

        if (!isInstanceRunning(port)) {
            return "错误：端口 " + port + " 的BLE工具实例未启动";
        }

        BleTools tools = getInstance(port);

        if (!tools.isConnected) {
            return "错误：未连接BLE设备，请先使用 dlqq_ble_connect 连接设备";
        }

        // 使用Windows BLE管理器发现服务
        tools.discoveredServices.clear();
        List<BleManager.BleService> services = tools.bleManager.discoverServices();
        
        for (BleManager.BleService svc : services) {
            BleService bleService = new BleService(svc.getUuid(), svc.getName());
            for (BleManager.BleCharacteristic chr : svc.getCharacteristics()) {
                bleService.characteristics.put(chr.getUuid(), 
                    new BleCharacteristic(chr.getUuid(), chr.getName(), chr.getProperties()));
            }
            tools.discoveredServices.put(bleService.uuid, bleService);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("发现的BLE服务和特征:\n\n");

        int serviceIndex = 1;
        for (BleService service : tools.discoveredServices.values()) {
            sb.append(String.format("服务 %d: %s (%s)\n", serviceIndex++, service.name, service.uuid));

            int charIndex = 1;
            for (BleCharacteristic characteristic : service.characteristics.values()) {
                sb.append(String.format("  特征 %d: %s (%s)\n", charIndex++, characteristic.name, characteristic.uuid));
                sb.append(String.format("    属性: %s\n", String.join(", ", characteristic.properties)));
            }
            sb.append("\n");
        }

        tools.addToBuffer("SYS", "服务发现完成，共发现 " + tools.discoveredServices.size() + " 个服务");

        return sb.toString();
    }

    /**
     * 6. 读取特征值
     *
     * @param port 服务器端口号
     * @param serviceUuid 服务UUID
     * @param charUuid 特征UUID
     * @return 读取结果
     */
    public static String readBleCharacteristic(int port, String serviceUuid, String charUuid) {
        String portError = checkPortParam(port);
        if (portError != null) {
            return portError;
        }

        if (!isInstanceRunning(port)) {
            return "错误：端口 " + port + " 的BLE工具实例未启动";
        }

        BleTools tools = getInstance(port);

        if (!tools.isConnected) {
            return "错误：未连接BLE设备";
        }

        if (serviceUuid == null || serviceUuid.trim().isEmpty()) {
            return "错误：服务UUID不能为空";
        }
        if (charUuid == null || charUuid.trim().isEmpty()) {
            return "错误：特征UUID不能为空";
        }

        // 使用Windows BLE管理器读取特征
        byte[] data = tools.bleManager.readCharacteristic(serviceUuid, charUuid);
        
        if (data == null) {
            return "读取特征失败";
        }
        
        String textValue = new String(data, StandardCharsets.UTF_8);
        String hexValue = bytesToHex(data);

        tools.addToBuffer("RX", "读取特征 [" + serviceUuid + "/" + charUuid + "]: " + hexValue + " (" + textValue + ")");

        return "特征值:\n" +
               "  HEX: " + hexValue + "\n" +
               "  文本: " + textValue + "\n" +
               "  长度: " + data.length + " 字节";
    }

    /**
     * 7. 写入特征值（文本）
     *
     * @param port 服务器端口号
     * @param serviceUuid 服务UUID
     * @param charUuid 特征UUID
     * @param text 要写入的文本
     * @return 写入结果
     */
    public static String writeBleCharacteristicText(int port, String serviceUuid, String charUuid, String text) {
        String portError = checkPortParam(port);
        if (portError != null) {
            return portError;
        }

        if (!isInstanceRunning(port)) {
            return "错误：端口 " + port + " 的BLE工具实例未启动";
        }

        BleTools tools = getInstance(port);

        if (!tools.isConnected) {
            return "错误：未连接BLE设备";
        }

        if (serviceUuid == null || serviceUuid.trim().isEmpty()) {
            return "错误：服务UUID不能为空";
        }
        if (charUuid == null || charUuid.trim().isEmpty()) {
            return "错误：特征UUID不能为空";
        }
        if (text == null || text.isEmpty()) {
            return "错误：写入内容不能为空";
        }

        // 使用Windows BLE管理器写入特征
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        boolean success = tools.bleManager.writeCharacteristic(serviceUuid, charUuid, data, true);
        
        if (!success) {
            return "写入特征失败";
        }
        
        String hexValue = bytesToHex(data);
        tools.addToBuffer("TX", "写入特征 [" + serviceUuid + "/" + charUuid + "]: " + text + " (" + hexValue + ")");

        return "已写入 " + data.length + " 字节到特征 [" + charUuid + "]";
    }

    /**
     * 7a. 写入特征值（HEX）
     *
     * @param port 服务器端口号
     * @param serviceUuid 服务UUID
     * @param charUuid 特征UUID
     * @param hexArray HEX字节数组
     * @return 写入结果
     */
    public static String writeBleCharacteristicHex(int port, String serviceUuid, String charUuid, List<String> hexArray) {
        String portError = checkPortParam(port);
        if (portError != null) {
            return portError;
        }

        if (!isInstanceRunning(port)) {
            return "错误：端口 " + port + " 的BLE工具实例未启动";
        }

        BleTools tools = getInstance(port);

        if (!tools.isConnected) {
            return "错误：未连接BLE设备";
        }

        if (serviceUuid == null || serviceUuid.trim().isEmpty()) {
            return "错误：服务UUID不能为空";
        }
        if (charUuid == null || charUuid.trim().isEmpty()) {
            return "错误：特征UUID不能为空";
        }
        if (hexArray == null || hexArray.isEmpty()) {
            return "错误：HEX数据不能为空";
        }

        try {
            // 解析HEX数据
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            StringBuilder hexString = new StringBuilder();

            for (String hex : hexArray) {
                if (hex == null || hex.trim().isEmpty()) {
                    continue;
                }
                String cleanHex = hex.trim().replaceAll("\\s", "");
                if (cleanHex.length() % 2 != 0) {
                    return "错误：16进制字符串长度必须是偶数: " + hex;
                }
                for (int i = 0; i < cleanHex.length(); i += 2) {
                    int byteValue = Integer.parseInt(cleanHex.substring(i, i + 2), 16);
                    baos.write(byteValue);
                    hexString.append(String.format("%02X ", byteValue));
                }
            }

            byte[] data = baos.toByteArray();

            // 使用Windows BLE管理器写入特征
            boolean success = tools.bleManager.writeCharacteristic(serviceUuid, charUuid, data, true);
            
            if (!success) {
                return "写入特征失败";
            }
            
            tools.addToBuffer("TX", "写入特征 [" + serviceUuid + "/" + charUuid + "] (HEX): " + hexString.toString().trim());

            return "已写入 " + data.length + " 字节到特征 [" + charUuid + "] (HEX: " + hexString.toString().trim() + ")";
        } catch (Exception e) {
            return "HEX数据解析错误: " + e.getMessage();
        }
    }

    /**
     * 8. 订阅通知
     *
     * @param port 服务器端口号
     * @param serviceUuid 服务UUID
     * @param charUuid 特征UUID
     * @param enable 是否启用通知
     * @return 订阅结果
     */
    public static String subscribeBleNotifications(int port, String serviceUuid, String charUuid, boolean enable) {
        String portError = checkPortParam(port);
        if (portError != null) {
            return portError;
        }

        if (!isInstanceRunning(port)) {
            return "错误：端口 " + port + " 的BLE工具实例未启动";
        }

        BleTools tools = getInstance(port);

        if (!tools.isConnected) {
            return "错误：未连接BLE设备";
        }

        if (serviceUuid == null || serviceUuid.trim().isEmpty()) {
            return "错误：服务UUID不能为空";
        }
        if (charUuid == null || charUuid.trim().isEmpty()) {
            return "错误：特征UUID不能为空";
        }

        // 使用Windows BLE管理器订阅/取消订阅通知
        String action = enable ? "订阅" : "取消订阅";
        boolean success;
        
        if (enable) {
            success = tools.bleManager.subscribeNotification(serviceUuid, charUuid, 
                (svcUuid, chrUuid, data) -> {
                    String hexData = bytesToHex(data);
                    String textData = new String(data, StandardCharsets.UTF_8);
                    tools.addToBuffer("RX", "通知 [" + svcUuid + "/" + chrUuid + "]: " + hexData + " (" + textData + ")");
                });
        } else {
            success = tools.bleManager.unsubscribeNotification(serviceUuid, charUuid);
        }
        
        if (!success) {
            return action + "特征通知失败";
        }
        
        tools.addToBuffer("SYS", action + "特征通知 [" + serviceUuid + "/" + charUuid + "]");

        return action + "特征通知成功 [" + charUuid + "]";
    }

    /**
     * 9. 获取BLE日志数据
     */
    public static String getBleLog(Integer port, Integer startLine, Integer endLine, Integer lineCount, int timeoutMs) {
        String portError = checkPortParam(port);
        if (portError != null) {
            return portError;
        }

        if (!isInstanceRunning(port)) {
            return "错误：端口 " + port + " 的BLE工具实例未启动";
        }

        BleTools tools = getInstance(port);
        return tools.getLogCore(startLine, endLine, lineCount, timeoutMs);
    }

    /**
     * 9a. 获取BLE日志（强制文本格式）
     */
    public static String getBleLogText(Integer port, Integer startLine, Integer endLine, Integer lineCount, int timeoutMs) {
        return getBleLog(port, startLine, endLine, lineCount, timeoutMs);
    }

    /**
     * 9b. 获取BLE日志（强制HEX格式）
     */
    public static String getBleLogHex(Integer port, Integer startLine, Integer endLine, Integer lineCount, int timeoutMs) {
        String portError = checkPortParam(port);
        if (portError != null) {
            return portError;
        }

        if (!isInstanceRunning(port)) {
            return "错误：端口 " + port + " 的BLE工具实例未启动";
        }

        BleTools tools = getInstance(port);
        String rawLog = tools.getLogCore(startLine, endLine, lineCount, timeoutMs);

        if (rawLog.startsWith("错误：") || rawLog.startsWith("等待超时")) {
            return rawLog;
        }

        if (rawLog.isEmpty()) {
            return rawLog;
        }

        // 转换为HEX格式
        String[] lines = rawLog.split("\n");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            sb.append(tools.convertLineToHex(lines[i]));
            if (i < lines.length - 1) {
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * 10. 清除BLE日志
     */
    public static String clearBleLog(int port) {
        String portError = checkPortParam(port);
        if (portError != null) {
            return portError;
        }

        if (!isInstanceRunning(port)) {
            return "错误：端口 " + port + " 的BLE工具实例未启动";
        }

        BleTools tools = getInstance(port);

        synchronized (tools.dataBuffer) {
            int clearedLines = tools.dataBuffer.size();
            tools.dataBuffer.clear();
            tools.currentBufferSize = 0;
            tools.totalReceivedBytes = 0;
            tools.totalSentBytes = 0;
            tools.lineCounter = 0;

            SwingUtilities.invokeLater(() -> {
                if (tools.logArea != null) {
                    tools.logArea.setText("");
                }
            });

            tools.updateStatusLabel();

            return "已清除 " + clearedLines + " 行数据及所有统计数据";
        }
    }

    /**
     * 10a. 获取缓冲区行数
     */
    public static String getLineCount(int port) {
        String portError = checkPortParam(port);
        if (portError != null) {
            return portError;
        }

        if (!isInstanceRunning(port)) {
            return "错误：端口 " + port + " 的BLE工具实例未启动";
        }

        BleTools tools = getInstance(port);

        synchronized (tools.dataBuffer) {
            return String.valueOf(tools.dataBuffer.size());
        }
    }

    /**
     * 11. 设置缓冲区大小
     */
    public static String setBleBufferSize(int port, int sizeBytes) {
        String portError = checkPortParam(port);
        if (portError != null) {
            return portError;
        }

        if (!isInstanceRunning(port)) {
            return "错误：端口 " + port + " 的BLE工具实例未启动";
        }

        BleTools tools = getInstance(port);

        if (sizeBytes <= 0) {
            return "错误：缓冲区大小必须大于0";
        }

        tools.maxBufferSize = sizeBytes;

        synchronized (tools.dataBuffer) {
            while (tools.currentBufferSize > tools.maxBufferSize && !tools.dataBuffer.isEmpty()) {
                String removed = tools.dataBuffer.remove(0);
                tools.currentBufferSize -= removed.getBytes(StandardCharsets.UTF_8).length;
            }
        }

        return "缓冲区大小已设置为 " + sizeBytes + " 字节 (" + (sizeBytes / 1024 / 1024) + " MB)";
    }

    /**
     * 12. 切换显示格式
     */
    public static String switchDisplayFormat(int port, String format) {
        String portError = checkPortParam(port);
        if (portError != null) {
            return portError;
        }

        if (!isInstanceRunning(port)) {
            return "错误：端口 " + port + " 的BLE工具实例未启动";
        }

        BleTools tools = getInstance(port);

        if (format == null || format.trim().isEmpty()) {
            return "错误：请指定显示格式，'text' 表示文本，'hex' 表示HEX";
        }

        String fmt = format.trim().toLowerCase();
        if (fmt.equals("text") || fmt.equals("txt") || fmt.equals("文本")) {
            tools.displayAsHex = false;
            tools.refreshLogDisplay();
            return "显示格式已切换为：文本（Text）";
        } else if (fmt.equals("hex") || fmt.equals("十六进制")) {
            tools.displayAsHex = true;
            tools.refreshLogDisplay();
            return "显示格式已切换为：HEX（十六进制）";
        } else {
            return "错误：不支持的显示格式 '" + format + "'，请使用 'text' 或 'hex'";
        }
    }

    /**
     * 13. 退出BLE工具
     */
    public static String exitBle(int port) {
        String portError = checkPortParam(port);
        if (portError != null) {
            return portError;
        }

        BleTools tools = instances.get(port);
        if (tools == null) {
            return "错误：端口 " + port + " 没有正在运行的BLE工具实例";
        }

        // 关闭GUI窗口
        if (tools.guiWindow != null) {
            SwingUtilities.invokeLater(() -> {
                tools.guiWindow.dispose();
                tools.guiWindow = null;
            });
        }

        // 断开连接
        if (tools.isConnected) {
            tools.isConnected = false;
        }

        // 关闭服务器
        tools.isRunning = false;
        if (tools.serverSocket != null && !tools.serverSocket.isClosed()) {
            try {
                tools.serverSocket.close();
            } catch (IOException e) {
                return "关闭服务器时出错: " + e.getMessage();
            }
        }

        // 停止Python BLE服务（每个实例独立的Python进程）
        if (tools.bleManager != null) {
            tools.bleManager.stopService();
        }

        // 关闭线程池
        tools.executor.shutdown();
        try {
            if (!tools.executor.awaitTermination(2, TimeUnit.SECONDS)) {
                tools.executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            tools.executor.shutdownNow();
        }

        // 清空缓冲区
        synchronized (tools.dataBuffer) {
            tools.dataBuffer.clear();
            tools.currentBufferSize = 0;
            tools.totalReceivedBytes = 0;
            tools.totalSentBytes = 0;
            tools.lineCounter = 0;
        }

        // 清空设备列表和服务
        tools.scannedDevices.clear();
        tools.discoveredServices.clear();

        // 从实例映射中移除
        instances.remove(port);

        return "BLE工具实例端口 " + port + " 已完全关闭";
    }

    // ==================== 内部实现 ====================

    private void startServer(int port) throws IOException {
        this.serverPort = port;
        // 使用SO_REUSEADDR选项，允许端口在TIME_WAIT状态下被重用
        this.serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new java.net.InetSocketAddress(port));
        this.isRunning = true;

        this.serverThread = new Thread(() -> {
            while (isRunning && !serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    executor.submit(() -> handleClient(clientSocket));
                } catch (IOException e) {
                    if (isRunning) {
                        System.err.println("BLE服务器 accept 错误: " + e.getMessage());
                    }
                }
            }
        });
        this.serverThread.setDaemon(true);
        this.serverThread.start();
    }

    private void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                String response = processCommand(line);
                out.println(response);
            }
        } catch (IOException e) {
            System.err.println("BLE客户端处理错误: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private String processCommand(String command) {
        String[] parts = command.trim().split("\\s+", 2);
        String cmd = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";

        switch (cmd) {
            case "scan":
                int timeout = args.isEmpty() ? 10000 : Integer.parseInt(args);
                return scanBleDevices(serverPort, timeout);
            case "devices":
                return getBleDevices(serverPort);
            case "connect":
                return connectBleDevice(serverPort, args);
            case "disconnect":
                return disconnectBleDevice(serverPort);
            case "discover":
                return discoverBleServices(serverPort);
            case "get":
                String[] getArgs = args.split("\\s+", 2);
                int start = getArgs.length > 0 ? Integer.parseInt(getArgs[0]) : 1;
                int waitMs = getArgs.length > 1 ? Integer.parseInt(getArgs[1]) : 1000;
                return getBleLog(serverPort, start, null, null, waitMs);
            case "clear":
                return clearBleLog(serverPort);
            case "size":
                int newSize = args.isEmpty() ? 5 * 1024 * 1024 : Integer.parseInt(args);
                return setBleBufferSize(serverPort, newSize);
            default:
                return "未知命令: " + cmd;
        }
    }

    // ==================== 模拟方法（TODO: 替换为真实BLE实现） ====================

    private static void simulateBleScan(BleTools tools, int timeoutMs) {
        // 模拟扫描延迟
        try {
            Thread.sleep(Math.min(timeoutMs, 2000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 模拟发现的设备
        tools.scannedDevices.put("AA:BB:CC:DD:EE:01", "BLE Sensor 1");
        tools.scannedDevices.put("AA:BB:CC:DD:EE:02", "BLE Heart Rate Monitor");
        tools.scannedDevices.put("AA:BB:CC:DD:EE:03", "BLE Temperature Sensor");
        tools.scannedDevices.put("AA:BB:CC:DD:EE:04", "BLE Smart Device");
    }

    private static void simulateServiceDiscovery(BleTools tools) {
        // 模拟发现的服务和特征
        BleService batteryService = new BleService("0000180f-0000-1000-8000-00805f9b34fb", "Battery Service");
        batteryService.characteristics.put("00002a19-0000-1000-8000-00805f9b34fb",
            new BleCharacteristic("00002a19-0000-1000-8000-00805f9b34fb", "Battery Level",
                Arrays.asList("read", "notify")));
        tools.discoveredServices.put(batteryService.uuid, batteryService);

        BleService deviceInfoService = new BleService("0000180a-0000-1000-8000-00805f9b34fb", "Device Information");
        deviceInfoService.characteristics.put("00002a29-0000-1000-8000-00805f9b34fb",
            new BleCharacteristic("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name",
                Arrays.asList("read")));
        deviceInfoService.characteristics.put("00002a24-0000-1000-8000-00805f9b34fb",
            new BleCharacteristic("00002a24-0000-1000-8000-00805f9b34fb", "Model Number",
                Arrays.asList("read")));
        tools.discoveredServices.put(deviceInfoService.uuid, deviceInfoService);

        BleService customService = new BleService("0000fff0-0000-1000-8000-00805f9b34fb", "Custom Service");
        customService.characteristics.put("0000fff1-0000-1000-8000-00805f9b34fb",
            new BleCharacteristic("0000fff1-0000-1000-8000-00805f9b34fb", "Read Characteristic",
                Arrays.asList("read")));
        customService.characteristics.put("0000fff2-0000-1000-8000-00805f9b34fb",
            new BleCharacteristic("0000fff2-0000-1000-8000-00805f9b34fb", "Write Characteristic",
                Arrays.asList("write")));
        customService.characteristics.put("0000fff3-0000-1000-8000-00805f9b34fb",
            new BleCharacteristic("0000fff3-0000-1000-8000-00805f9b34fb", "Notify Characteristic",
                Arrays.asList("read", "notify")));
        tools.discoveredServices.put(customService.uuid, customService);
    }

    // ==================== 缓冲区管理 ====================

    private void addToBuffer(String direction, String data) {
        synchronized (dataBuffer) {
            lineCounter++;
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH.mm.ss.SSS"));
            String formattedData = String.format("[%05d] [%s] %s: %s", lineCounter, timestamp, direction, data);
            dataBuffer.add(formattedData);
            int dataSize = data.getBytes(StandardCharsets.UTF_8).length;
            currentBufferSize += formattedData.getBytes(StandardCharsets.UTF_8).length;

            if (direction.equals("RX")) {
                totalReceivedBytes += dataSize;
            } else if (direction.equals("TX")) {
                totalSentBytes += dataSize;
            }

            // 超出缓冲区大小时，删除旧数据
            while (currentBufferSize > maxBufferSize && !dataBuffer.isEmpty()) {
                String removed = dataBuffer.remove(0);
                currentBufferSize -= removed.getBytes(StandardCharsets.UTF_8).length;
            }

            updateStatusLabel();

            // 更新GUI
            SwingUtilities.invokeLater(() -> {
                if (logArea != null) {
                    logArea.append(convertLineForDisplay(formattedData) + "\n");
                }
            });
        }
    }

    private String getLogCore(Integer startLine, Integer endLine, Integer lineCount, int timeoutMs) {
        boolean isWaitMode = timeoutMs > 0;

        if (isWaitMode) {
            int initialLines;
            synchronized (dataBuffer) {
                initialLines = dataBuffer.size();
            }

            int startFrom = (startLine != null && startLine > 0) ? startLine : initialLines + 1;
            int targetLines = (lineCount != null && lineCount > 0) ? lineCount : 1;

            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                synchronized (dataBuffer) {
                    if (dataBuffer.size() >= startFrom + targetLines - 1) {
                        break;
                    }
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    break;
                }
            }

            synchronized (dataBuffer) {
                int totalLines = dataBuffer.size();

                if (startFrom > totalLines || totalLines == 0) {
                    return "等待超时，当前缓冲区有 " + totalLines + " 行数据";
                }

                int endAt = Math.min(startFrom + targetLines - 1, totalLines);

                StringBuilder sb = new StringBuilder();
                for (int i = startFrom - 1; i < endAt; i++) {
                    sb.append(dataBuffer.get(i));
                    if (i < endAt - 1) {
                        sb.append("\n");
                    }
                }

                return sb.toString();
            }
        } else {
            synchronized (dataBuffer) {
                if (dataBuffer.isEmpty()) {
                    return "";
                }

                int totalLines = dataBuffer.size();

                int start = (startLine != null && startLine > 0) ? startLine : 1;
                if (start > totalLines) {
                    return "";
                }

                int end;
                if (endLine != null && endLine > 0) {
                    end = Math.min(endLine, totalLines);
                } else if (lineCount != null && lineCount > 0) {
                    end = Math.min(start + lineCount - 1, totalLines);
                } else {
                    end = totalLines;
                }

                if (end < start) {
                    end = start;
                }

                StringBuilder sb = new StringBuilder();
                for (int i = start - 1; i < end && i < totalLines; i++) {
                    sb.append(dataBuffer.get(i));
                    if (i < end - 1) {
                        sb.append("\n");
                    }
                }

                return sb.toString();
            }
        }
    }

    // ==================== GUI窗口 ====================

    private void createGuiWindow() {
        if (guiWindow != null && guiWindow.isVisible()) {
            guiWindow.toFront();
            return;
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        guiWindow = new JFrame("BLE 蓝牙工具 - Java端口: " + serverPort + " | Python端口: " + pythonServicePort);
        guiWindow.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        guiWindow.setSize(900, 700);
        guiWindow.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 顶部控制面板
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        controlPanel.setBorder(BorderFactory.createTitledBorder("BLE设备"));

        controlPanel.add(new JLabel("设备:"));
        deviceCombo = new JComboBox<>();
        deviceCombo.setPreferredSize(new Dimension(350, 25));
        deviceCombo.setEditable(true);
        controlPanel.add(deviceCombo);

        scanButton = new JButton("扫描");
        scanButton.addActionListener(e -> {
            String result = scanBleDevices(serverPort, 10000);
            JOptionPane.showMessageDialog(guiWindow, result);
        });
        controlPanel.add(scanButton);

        statusIcon = new StatusIcon(false);

        connectButton = new JButton("连接设备", statusIcon);
        connectButton.setHorizontalTextPosition(SwingConstants.RIGHT);
        connectButton.setHorizontalAlignment(SwingConstants.CENTER);

        connectButton.addActionListener(e -> {
            if (isConnected) {
                String result = disconnectBleDevice(serverPort);
                JOptionPane.showMessageDialog(guiWindow, result);
            } else {
                String selected = (String) deviceCombo.getSelectedItem();
                if (selected != null && !selected.isEmpty()) {
                    // 提取地址（格式: "AA:BB:CC:DD:EE:01 - Device Name" 或纯地址）
                    String address = selected.split(" - ")[0].trim();
                    String result = connectBleDevice(serverPort, address);
                    JOptionPane.showMessageDialog(guiWindow, result);
                }
            }
        });
        controlPanel.add(connectButton);

        // 设备列表面板
        JPanel deviceListPanel = new JPanel(new BorderLayout());
        deviceListPanel.setBorder(BorderFactory.createTitledBorder("扫描到的设备列表 (0)"));
        deviceListModel = new DefaultListModel<>();
        deviceListModel.clear(); // 清空之前的设备列表
        deviceList = new JList<>(deviceListModel);
        deviceList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        deviceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        deviceList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = deviceList.getSelectedValue();
                if (selected != null) {
                    deviceCombo.setSelectedItem(selected);
                }
            }
        });
        JScrollPane deviceListScroll = new JScrollPane(deviceList);
        deviceListScroll.setPreferredSize(new Dimension(300, 200));
        deviceListPanel.add(deviceListScroll, BorderLayout.CENTER);

        // 中间日志区域
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("BLE日志"));

        // 使用SplitPane分割设备列表和日志区域
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, deviceListPanel, scrollPane);
        splitPane.setResizeWeight(0.3); // 设备列表占30%
        splitPane.setOneTouchExpandable(true);

        // 底部状态栏
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusLabel = new JLabel("行数: 0 | 缓冲区: 0/0 bytes (0.0%) | 发送: 0 bytes | 接收: 0 bytes");
        statusPanel.add(statusLabel, BorderLayout.WEST);

        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(splitPane, BorderLayout.CENTER);
        mainPanel.add(statusPanel, BorderLayout.SOUTH);

        guiWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int result = JOptionPane.showConfirmDialog(guiWindow,
                    "确定要关闭BLE工具吗？这将断开当前连接。",
                    "确认关闭",
                    JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    exitBle(serverPort);
                }
            }
        });

        guiWindow.setContentPane(mainPanel);
        guiWindow.setVisible(true);
    }

    private void updateStatusLabel() {
        if (statusLabel == null) return;
        SwingUtilities.invokeLater(() -> {
            double usagePercent = (double) currentBufferSize / maxBufferSize * 100;
            int lineCount = dataBuffer.size();
            statusLabel.setText(String.format("行数: %d | 缓冲区: %d/%d bytes (%.1f%%) | 发送: %d bytes | 接收: %d bytes",
                lineCount, currentBufferSize, maxBufferSize, usagePercent, totalSentBytes, totalReceivedBytes));
        });
    }

    private void refreshLogDisplay() {
        if (logArea == null) return;
        SwingUtilities.invokeLater(() -> {
            logArea.setText("");
            synchronized (dataBuffer) {
                for (String line : dataBuffer) {
                    logArea.append(convertLineForDisplay(line) + "\n");
                }
            }
        });
    }

    private String convertLineForDisplay(String line) {
        if (!displayAsHex) {
            return line;
        }
        return convertDataPartToHex(line);
    }

    private String convertLineToHex(String line) {
        return convertDataPartToHex(line);
    }

    private String convertDataPartToHex(String line) {
        // 找到 RX:/TX:/SYS: 后的数据部分并转换为HEX
        if (line.contains("RX: ")) {
            int idx = line.indexOf("RX: ");
            String prefix = line.substring(0, idx + 4);
            String data = line.substring(idx + 4);
            String hexData = bytesToHex(data.getBytes(StandardCharsets.UTF_8));
            return prefix + hexData;
        } else if (line.contains("TX: ")) {
            int idx = line.indexOf("TX: ");
            String prefix = line.substring(0, idx + 4);
            String data = line.substring(idx + 4);
            String hexData = bytesToHex(data.getBytes(StandardCharsets.UTF_8));
            return prefix + hexData;
        } else if (line.contains("SYS: ")) {
            return line;
        }
        return line;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b & 0xFF));
        }
        return sb.toString().trim();
    }

    // ==================== 状态图标类（复用UartTools的） ====================

    private static class StatusIcon implements Icon {
        private boolean isOpen = false;
        private static final int SIZE = 16;

        public StatusIcon(boolean isOpen) {
            this.isOpen = isOpen;
        }

        public void setOpen(boolean open) {
            this.isOpen = open;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (isOpen) {
                g2d.setColor(new Color(0, 200, 0));
            } else {
                g2d.setColor(new Color(200, 0, 0));
            }

            g2d.fillOval(x + 2, y + 2, SIZE - 4, SIZE - 4);

            g2d.setColor(Color.BLACK);
            g2d.drawOval(x + 2, y + 2, SIZE - 4, SIZE - 4);

            g2d.dispose();
        }

        @Override
        public int getIconWidth() {
            return SIZE;
        }

        @Override
        public int getIconHeight() {
            return SIZE;
        }
    }
}
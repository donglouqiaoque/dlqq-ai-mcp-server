package com.donglouqiaoque.aitools.mcp.tools;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * BLE管理器
 * 通过Socket与Python BLE服务通信
 * 支持跨平台（Windows/Linux/macOS）
 */
public class BleManager {

    // Python服务配置
    private static final String SCRIPT_RESOURCE_PATH = "/py/ble_service.py";
    private static final String[] REQUIRED_PACKAGES = {"bleak"};

    // 实例配置 - 每个实例有独立的端口号
    private final int servicePort;
    private final int notificationPort;  // 通知接收端口
    private Process pythonProcess;
    private boolean pythonServiceStarted = false;
    private File pythonScriptFile;

    // 状态
    private boolean isInitialized = false;
    private boolean isScanning = false;
    private Map<String, BleDevice> discoveredDevices = new ConcurrentHashMap<>();
    private BleDevice connectedDevice = null;

    // 扫描回调
    private ScanCallback scanCallback;
    private Thread scanThread;

    // 设备数据缓存
    private final Map<String, BleService> servicesCache = new ConcurrentHashMap<>();

    // 通知回调
    private final Map<String, NotificationCallback> notificationCallbacks = new ConcurrentHashMap<>();

    // 通知接收服务器
    private ServerSocket notificationServerSocket;
    private Thread notificationServerThread;
    private volatile boolean notificationServerRunning = false;

    // JSON解析器
    private final Gson gson = new Gson();

    // Python命令缓存
    private String pythonCommand = null;

    // 心跳检测
    private Thread heartbeatThread;
    private volatile boolean heartbeatRunning = false;
    private static final int HEARTBEAT_INTERVAL_MS = 10000;  // 心跳间隔10秒

    /**
     * 创建BLE管理器实例
     * @param port Python服务端口号（与MCP工具实例端口一致）
     */
    public BleManager(int port) {
        this.servicePort = port;
        this.notificationPort = port + 1000;  // 通知端口 = 服务端口 + 1000
        
        // 注册Shutdown Hook，确保Java进程退出时销毁Python进程
        registerShutdownHook();
    }

    /**
     * 获取实例标识（用于日志）
     */
    private String getInstanceId() {
        return "[BleManager-" + servicePort + "]";
    }

    // ==================== Shutdown Hook ====================

    // 静态变量，用于跟踪所有BleManager实例
    private static final List<BleManager> instances = new CopyOnWriteArrayList<>();

    /**
     * 注册Shutdown Hook，确保Java进程退出时销毁所有Python进程
     */
    private void registerShutdownHook() {
        // 添加当前实例到全局列表
        instances.add(this);
        
        // 只注册一次Shutdown Hook
        if (!shutdownHookRegistered) {
            synchronized (BleManager.class) {
                if (!shutdownHookRegistered) {
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        System.err.println("[BleManager] Java进程退出，开始清理所有Python进程...");
                        for (BleManager manager : instances) {
                            try {
                                manager.cleanupPythonProcess();
                            } catch (Exception e) {
                                System.err.println(manager.getInstanceId() + " 清理Python进程失败: " + e.getMessage());
                            }
                        }
                        System.err.println("[BleManager] 所有Python进程清理完成");
                    }, "BLE-Python-Cleanup"));
                    shutdownHookRegistered = true;
                }
            }
        }
    }

    private static volatile boolean shutdownHookRegistered = false;

    /**
     * 清理Python进程（供Shutdown Hook调用）
     */
    private void cleanupPythonProcess() {
        if (pythonProcess != null && pythonProcess.isAlive()) {
            System.err.println(getInstanceId() + " 正在销毁Python进程 (PID: " + pythonProcess.pid() + ")...");
            pythonProcess.destroy();
            try {
                // 等待进程结束，最多等待3秒
                if (!pythonProcess.waitFor(3, TimeUnit.SECONDS)) {
                    System.err.println(getInstanceId() + " Python进程未正常退出，强制终止...");
                    pythonProcess.destroyForcibly();
                    pythonProcess.waitFor(2, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                System.err.println(getInstanceId() + " 等待Python进程退出被中断: " + e.getMessage());
                pythonProcess.destroyForcibly();
                Thread.currentThread().interrupt();
            }
            pythonProcess = null;
            pythonServiceStarted = false;
            System.err.println(getInstanceId() + " Python进程已销毁");
        }
    }

    // ==================== Python环境检测与管理 ====================

    /**
     * 检查Python是否已安装
     */
    public static boolean hasPython() {
        return findPythonCommand() != null;
    }

    /**
     * 获取Python命令
     */
    public static String getPythonCommand() {
        return findPythonCommand();
    }

    /**
     * 查找Python可执行文件
     */
    private static String findPythonCommand() {
        String[] commands = {"python3", "python", "py"};
        for (String cmd : commands) {
            try {
                ProcessBuilder pb = new ProcessBuilder(cmd, "--version");
                pb.redirectErrorStream(true);
                Process p = pb.start();
                if (p.waitFor() == 0) {
                    return cmd;
                }
            } catch (Exception e) {
                // 继续尝试下一个
            }
        }
        return null;
    }

    /**
     * 获取Python版本
     */
    public static String getPythonVersion() {
        String python = findPythonCommand();
        if (python == null) return null;

        try {
            ProcessBuilder pb = new ProcessBuilder(python, "--version");
            pb.redirectErrorStream(true);
            Process p = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            p.waitFor();

            return line != null ? line.trim() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 检查库是否已安装
     */
    public static Map<String, LibraryInfo> checkLibraries() {
        Map<String, LibraryInfo> result = new LinkedHashMap<>();
        String python = findPythonCommand();

        if (python == null) {
            for (String pkg : REQUIRED_PACKAGES) {
                result.put(pkg, new LibraryInfo(false, null, null));
            }
            return result;
        }

        for (String pkg : REQUIRED_PACKAGES) {
            LibraryInfo info = checkLibrary(python, pkg);
            result.put(pkg, info);
        }

        return result;
    }

    /**
     * 检查单个库
     */
    private static LibraryInfo checkLibrary(String python, String packageName) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                python, "-c",
                "import " + packageName + "; print('INSTALLED'); print('VERSION:', getattr(" + packageName + ", '__version__', 'unknown'))"
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line1 = reader.readLine();
            String line2 = reader.readLine();
            p.waitFor();

            boolean installed = line1 != null && line1.contains("INSTALLED");
            String version = null;
            if (line2 != null && line2.startsWith("VERSION:")) {
                version = line2.substring(8).trim();
            }

            String latestVersion = checkLatestVersion(packageName);

            return new LibraryInfo(installed, version, latestVersion);

        } catch (Exception e) {
            return new LibraryInfo(false, null, null);
        }
    }

    /**
     * 检查库的最新版本
     */
    private static String checkLatestVersion(String packageName) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                findPythonCommand(), "-m", "pip", "index", "versions", packageName
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Available versions:")) {
                    String versions = line.substring(line.indexOf(":") + 1).trim();
                    String[] parts = versions.split(",");
                    if (parts.length > 0) {
                        p.waitFor();
                        return parts[0].trim();
                    }
                }
            }
            p.waitFor();
        } catch (Exception e) {
            // pip index 可能不可用
        }
        return null;
    }

    /**
     * 安装Python库
     */
    public static InstallResult installLibraries() {
        return installLibraries(null);
    }

    /**
     * 安装Python库（带进度回调）
     */
    public static InstallResult installLibraries(InstallProgressCallback callback) {
        String python = findPythonCommand();
        if (python == null) {
            return new InstallResult(false, "未找到Python，请先安装Python 3.8+", null);
        }

        StringBuilder output = new StringBuilder();
        Map<String, Boolean> installedPackages = new HashMap<>();

        for (String pkg : REQUIRED_PACKAGES) {
            if (callback != null) {
                callback.onProgress("正在安装 " + pkg + "...");
            }

            try {
                ProcessBuilder pb = new ProcessBuilder(python, "-m", "pip", "install", "-U", pkg);
                pb.redirectErrorStream(true);
                Process p = pb.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }

                int exitCode = p.waitFor();
                installedPackages.put(pkg, exitCode == 0);

                if (callback != null) {
                    callback.onProgress(pkg + " " + (exitCode == 0 ? "安装成功" : "安装失败"));
                }

            } catch (Exception e) {
                installedPackages.put(pkg, false);
                output.append("安装 ").append(pkg).append(" 失败: ").append(e.getMessage()).append("\n");
            }
        }

        boolean allSuccess = installedPackages.values().stream().allMatch(Boolean::booleanValue);
        String message = allSuccess ? "所有库安装成功" : "部分库安装失败";

        return new InstallResult(allSuccess, message, installedPackages);
    }

    /**
     * 检查是否有库需要更新
     */
    public static Map<String, Boolean> checkUpdates() {
        Map<String, Boolean> updates = new HashMap<>();
        Map<String, LibraryInfo> libs = checkLibraries();

        for (Map.Entry<String, LibraryInfo> entry : libs.entrySet()) {
            LibraryInfo info = entry.getValue();
            boolean needsUpdate = info.installed && info.latestVersion != null &&
                    !info.latestVersion.equals(info.currentVersion);
            updates.put(entry.getKey(), needsUpdate);
        }

        return updates;
    }

    // ==================== 服务启动与管理 ====================

    /**
     * 启动Python BLE服务
     */
    public boolean startService() {
        if (pythonServiceStarted) {
            return true;
        }

        try {
            pythonCommand = findPythonCommand();
            if (pythonCommand == null) {
                System.err.println(getInstanceId() + " 未找到Python，BLE服务无法启动");
                return false;
            }

            pythonScriptFile = extractPythonScript();
            if (pythonScriptFile == null) {
                System.err.println(getInstanceId() + " 无法提取BLE服务脚本");
                return false;
            }

            ProcessBuilder pb = new ProcessBuilder(pythonCommand, pythonScriptFile.getAbsolutePath(), String.valueOf(servicePort));
            pb.redirectErrorStream(true);
            pb.directory(pythonScriptFile.getParentFile());

            pythonProcess = pb.start();

            Thread.sleep(2000);

            // 临时设置为true以便sendCommand可以执行
            pythonServiceStarted = true;
            JsonObject response = sendCommand("ping", null);
            if (response != null && response.has("success") && response.get("success").getAsBoolean()) {
                // 启动通知接收服务器
                startNotificationServer();
                // 启动心跳线程
                startHeartbeat();
                // 使用stderr输出日志，避免污染MCP通信
                System.err.println(getInstanceId() + " Python BLE服务启动成功，端口: " + servicePort);
                System.err.println(getInstanceId() + " 通知接收服务器启动，端口: " + notificationPort);
                System.err.println(getInstanceId() + " 心跳检测已启动，间隔: " + HEARTBEAT_INTERVAL_MS + "ms");
                isInitialized = true;
                return true;
            } else {
                System.err.println(getInstanceId() + " Python BLE服务连接测试失败");
                // 测试失败，重置状态
                pythonServiceStarted = false;
                return false;
            }

        } catch (Exception e) {
            System.err.println(getInstanceId() + " 启动Python服务失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 从JAR提取Python脚本到.dlqq/script/python目录
     * 如果.dlqq/script/python目录中已有脚本，优先使用已存在的脚本
     */
    private File extractPythonScript() {
        try {
            // 获取工作目录
            Path workDir = Paths.get(System.getProperty("user.dir"));

            // 优先使用 .dlqq/script/python 目录
            Path dlqqPythonDir = workDir.resolve(".dlqq").resolve("script").resolve("python");
            File scriptFile = dlqqPythonDir.resolve("ble_service.py").toFile();

            // 检查 .dlqq/script/python/ble_service.py 是否已存在
            if (Files.exists(scriptFile.toPath())) {
                System.err.println(getInstanceId() + " 使用已存在的Python脚本: " + scriptFile.getAbsolutePath());
                return scriptFile;
            }

            // 不存在则提取到 .dlqq/script/python 目录
            Path pythonDir = dlqqPythonDir;

            // 确保目录存在
            if (!Files.exists(pythonDir)) {
                Files.createDirectories(pythonDir);
            }

            // 创建 .dlqq/.gitignore 文件，忽略该目录下的所有内容
            Path dlqqDir = workDir.resolve(".dlqq");
            Path gitignorePath = dlqqDir.resolve(".gitignore");
            if (!Files.exists(gitignorePath)) {
                Files.write(gitignorePath, "*\n".getBytes(StandardCharsets.UTF_8));
            }

            // 尝试将 .dlqq 目录设置为隐藏（Windows），失败则忽略
            try {
                java.nio.file.attribute.DosFileAttributeView dosView = Files.getFileAttributeView(
                        dlqqDir, java.nio.file.attribute.DosFileAttributeView.class);
                if (dosView != null) {
                    dosView.setHidden(true);
                }
            } catch (Exception ignored) {
                // 设置隐藏失败，忽略
            }

            try (InputStream is = getClass().getResourceAsStream(SCRIPT_RESOURCE_PATH)) {
                if (is == null) {
                    System.err.println(getInstanceId() + " 找不到资源: " + SCRIPT_RESOURCE_PATH);
                    return null;
                }
                Files.copy(is, scriptFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            // 使用stderr输出日志，避免污染MCP通信
            System.err.println(getInstanceId() + " Python脚本已提取到: " + scriptFile.getAbsolutePath());
            return scriptFile;

        } catch (Exception e) {
            System.err.println(getInstanceId() + " 提取脚本失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 发送命令到Python服务
     */
    private JsonObject sendCommand(String type, Map<String, Object> params) {
        if (!pythonServiceStarted) {
            return null;
        }

        try (Socket socket = new Socket()) {
            // 设置连接超时和读取超时（10秒）
            var socketAddress = new java.net.InetSocketAddress("127.0.0.1", servicePort);
            socket.connect(socketAddress, 10000);  // 10秒连接超时
            socket.setSoTimeout(30000);  // 30秒读取超时（BLE操作可能需要较长时间）
            
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            Map<String, Object> command = new HashMap<>();
            command.put("type", type);
            if (params != null) {
                command.putAll(params);
            }

            String jsonCmd = gson.toJson(command);
            out.println(jsonCmd);

            // 读取响应
            String response = in.readLine();
            if (response != null && !response.isEmpty()) {
                try {
                    return gson.fromJson(response, JsonObject.class);
                } catch (Exception e) {
                    System.err.println(getInstanceId() + " JSON解析失败: " + e.getMessage());
                }
            }

        } catch (java.net.SocketTimeoutException e) {
            System.err.println(getInstanceId() + " 发送命令超时 [" + type + "]: " + e.getMessage());
        } catch (Exception e) {
            System.err.println(getInstanceId() + " 发送命令失败 [" + type + "]: " + e.getMessage());
        }

        return null;
    }

    // ==================== 通知接收服务器 ====================

    /**
     * 启动通知接收服务器
     * Java开启监听端口，等待Python推送通知数据
     */
    private void startNotificationServer() {
        if (notificationServerRunning) {
            return;
        }

        notificationServerRunning = true;
        notificationServerThread = new Thread(() -> {
            try {
                notificationServerSocket = new ServerSocket(notificationPort);
                System.err.println(getInstanceId() + " 通知服务器监听端口: " + notificationPort);

                while (notificationServerRunning) {
                    try {
                        Socket clientSocket = notificationServerSocket.accept();
                        // 处理通知数据
                        handleNotification(clientSocket);
                    } catch (IOException e) {
                        if (notificationServerRunning) {
                            System.err.println(getInstanceId() + " 通知连接处理失败: " + e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println(getInstanceId() + " 通知服务器启动失败: " + e.getMessage());
            } finally {
                notificationServerRunning = false;
            }
        });
        notificationServerThread.setDaemon(true);
        notificationServerThread.start();

        // 等待服务器启动
        int retries = 0;
        while (!notificationServerRunning && retries < 10) {
            try {
                Thread.sleep(100);
                retries++;
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    /**
     * 处理接收到的通知数据
     */
    private void handleNotification(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8))) {

            String line = in.readLine();
            if (line != null && !line.isEmpty()) {
                JsonObject notification = gson.fromJson(line, JsonObject.class);
                if (notification != null) {
                    String charUuid = notification.has("char_uuid") ? notification.get("char_uuid").getAsString() : "";
                    String serviceUuid = notification.has("service_uuid") ? notification.get("service_uuid").getAsString() : "";

                    JsonArray dataArray = notification.getAsJsonArray("data");
                    byte[] data = null;
                    if (dataArray != null) {
                        data = new byte[dataArray.size()];
                        for (int i = 0; i < dataArray.size(); i++) {
                            data[i] = (byte) dataArray.get(i).getAsInt();
                        }
                    }

                    // 查找并调用回调
                    String key = serviceUuid + "/" + charUuid;
                    NotificationCallback callback = notificationCallbacks.get(key);
                    if (callback != null && data != null) {
                        callback.onDataReceived(serviceUuid, charUuid, data);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(getInstanceId() + " 处理通知失败: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    /**
     * 停止通知接收服务器
     */
    private void stopNotificationServer() {
        notificationServerRunning = false;
        if (notificationServerSocket != null) {
            try {
                notificationServerSocket.close();
            } catch (IOException e) {
                // ignore
            }
            notificationServerSocket = null;
        }
        if (notificationServerThread != null) {
            notificationServerThread.interrupt();
            notificationServerThread = null;
        }
    }

    /**
     * 停止服务
     */
    public void stopService() {
        stopNotificationServer();
        stopHeartbeat();
        cleanupPythonProcess();
        // 从全局实例列表中移除
        instances.remove(this);
        pythonServiceStarted = false;
        isInitialized = false;
    }

    // ==================== 心跳检测 ====================

    /**
     * 启动心跳线程，定期向Python发送ping命令
     */
    private void startHeartbeat() {
        heartbeatRunning = true;
        heartbeatThread = new Thread(() -> {
            System.err.println(getInstanceId() + " 心跳线程启动");
            while (heartbeatRunning && pythonServiceStarted) {
                try {
                    Thread.sleep(HEARTBEAT_INTERVAL_MS);
                    if (heartbeatRunning && pythonServiceStarted) {
                        JsonObject response = sendCommand("ping", null);
                        if (response == null || !response.has("success") || !response.get("success").getAsBoolean()) {
                            System.err.println(getInstanceId() + " 心跳检测失败，Python服务可能已停止");
                            // Python服务不可用，清理资源
                            stopService();
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println(getInstanceId() + " 心跳发送异常: " + e.getMessage());
                }
            }
            System.err.println(getInstanceId() + " 心跳线程退出");
        }, "BLE-Heartbeat-" + servicePort);
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();
    }

    /**
     * 停止心跳线程
     */
    private void stopHeartbeat() {
        heartbeatRunning = false;
        if (heartbeatThread != null) {
            heartbeatThread.interrupt();
            try {
                heartbeatThread.join(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            heartbeatThread = null;
        }
    }

    // ==================== BLE操作 ====================

    public boolean isNativeAvailable() {
        return pythonServiceStarted;
    }

    public boolean initialize() {
        if (isInitialized) {
            return true;
        }
        return startService();
    }

    public boolean startScan(ScanCallback callback, int timeoutMs) {
        if (!pythonServiceStarted && !startService()) {
            return false;
        }

        if (isScanning) {
            return true;
        }

        this.scanCallback = callback;
        isScanning = true;
        discoveredDevices.clear();

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("timeout", timeoutMs / 1000.0);

            JsonObject response = sendCommand("scan", params);

            if (response != null && response.has("success") && response.get("success").getAsBoolean()) {
                JsonArray devices = response.getAsJsonArray("devices");
                if (devices != null) {
                    for (JsonElement deviceElem : devices) {
                        JsonObject deviceObj = deviceElem.getAsJsonObject();
                        String address = deviceObj.get("address").getAsString();
                        String name = deviceObj.get("name").getAsString();
                        int rssi = deviceObj.get("rssi").getAsInt();

                        BleDevice device = new BleDevice(address, name, rssi);
                        discoveredDevices.put(address, device);

                        if (scanCallback != null) {
                            scanCallback.onDeviceFound(device);
                        }
                    }
                }

                if (scanCallback != null) {
                    scanCallback.onScanComplete();
                }
            } else {
                String error = response != null && response.has("error")
                        ? response.get("error").getAsString()
                        : "Unknown error";
                if (scanCallback != null) {
                    scanCallback.onScanFailed(error);
                }
            }
        } catch (Exception e) {
            if (scanCallback != null) {
                scanCallback.onScanFailed(e.getMessage());
            }
        } finally {
            isScanning = false;
        }

        return true;
    }

    public void stopScan() {
        isScanning = false;
        if (scanThread != null) {
            scanThread.interrupt();
        }
    }

    public boolean isScanning() {
        if (!pythonServiceStarted) return false;

        JsonObject response = sendCommand("is_scanning", null);
        if (response != null && response.has("success") && response.get("success").getAsBoolean()) {
            return response.has("scanning") && response.get("scanning").getAsBoolean();
        }
        return isScanning;
    }

    public boolean connect(String address, ConnectionCallback callback) {
        if (!pythonServiceStarted) return false;

        Map<String, Object> params = new HashMap<>();
        params.put("address", address);

        JsonObject response = sendCommand("connect", params);

        if (response != null && response.has("success") && response.get("success").getAsBoolean()) {
            String deviceName = discoveredDevices.containsKey(address)
                    ? discoveredDevices.get(address).getName() : "Unknown Device";

            BleDevice device = new BleDevice(address, deviceName, -50);
            device.setConnected(true);
            connectedDevice = device;

            if (callback != null) callback.onConnected(device);
            return true;
        } else {
            String error = response != null && response.has("error")
                    ? response.get("error").getAsString() : "Connection failed";
            if (callback != null) callback.onConnectionFailed(address, error);
            return false;
        }
    }

    public void disconnect() {
        if (!pythonServiceStarted) return;

        sendCommand("disconnect", null);

        if (connectedDevice != null) {
            connectedDevice.setConnected(false);
            connectedDevice = null;
        }
        servicesCache.clear();
        notificationCallbacks.clear();
    }

    public boolean isConnected() {
        if (!pythonServiceStarted) return false;

        JsonObject response = sendCommand("is_connected", null);
        if (response != null && response.has("success")) {
            return response.has("connected") && response.get("connected").getAsBoolean();
        }
        return false;
    }

    public List<BleService> discoverServices() {
        List<BleService> services = new ArrayList<>();

        if (!pythonServiceStarted) return services;

        JsonObject response = sendCommand("discover_services", null);

        if (response != null && response.has("success") && response.get("success").getAsBoolean()) {
            JsonArray servicesJson = response.getAsJsonArray("services");
            if (servicesJson != null) {
                for (JsonElement serviceElem : servicesJson) {
                    JsonObject serviceObj = serviceElem.getAsJsonObject();
                    String uuid = serviceObj.get("uuid").getAsString();
                    String name = serviceObj.get("name").getAsString();

                    BleService service = new BleService(uuid, name);
                    servicesCache.put(uuid, service);

                    JsonArray charsJson = serviceObj.getAsJsonArray("characteristics");
                    if (charsJson != null) {
                        for (JsonElement charElem : charsJson) {
                            JsonObject charObj = charElem.getAsJsonObject();
                            String charUuid = charObj.get("uuid").getAsString();
                            String charName = charObj.get("name").getAsString();

                            List<String> properties = new ArrayList<>();
                            JsonArray propsJson = charObj.getAsJsonArray("properties");
                            if (propsJson != null) {
                                for (JsonElement prop : propsJson) {
                                    properties.add(prop.getAsString());
                                }
                            }

                            BleCharacteristic characteristic = new BleCharacteristic(charUuid, charName, properties);
                            service.addCharacteristic(characteristic);
                        }
                    }

                    services.add(service);
                }
            }
        }

        return services;
    }

    public byte[] readCharacteristic(String serviceUuid, String charUuid) {
        if (!pythonServiceStarted) return null;

        Map<String, Object> params = new HashMap<>();
        params.put("char_uuid", charUuid);

        JsonObject response = sendCommand("read", params);

        if (response != null && response.has("success") && response.get("success").getAsBoolean()) {
            JsonArray dataArray = response.getAsJsonArray("data");
            if (dataArray != null) {
                byte[] data = new byte[dataArray.size()];
                for (int i = 0; i < dataArray.size(); i++) {
                    data[i] = (byte) dataArray.get(i).getAsInt();
                }
                return data;
            }
        }

        return null;
    }

    public boolean writeCharacteristic(String serviceUuid, String charUuid, byte[] data, boolean withResponse) {
        if (!pythonServiceStarted) return false;

        List<Integer> dataList = new ArrayList<>();
        for (byte b : data) {
            dataList.add(b & 0xFF);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("char_uuid", charUuid);
        params.put("data", dataList);
        params.put("response", withResponse);

        JsonObject response = sendCommand("write", params);
        return response != null && response.has("success") && response.get("success").getAsBoolean();
    }

    public boolean subscribeNotification(String serviceUuid, String charUuid, NotificationCallback callback) {
        if (!pythonServiceStarted) return false;

        Map<String, Object> params = new HashMap<>();
        params.put("char_uuid", charUuid);
        params.put("service_uuid", serviceUuid);
        params.put("notification_port", notificationPort);  // 传递通知端口给Python

        JsonObject response = sendCommand("subscribe", params);

        if (response != null && response.has("success") && response.get("success").getAsBoolean()) {
            String key = serviceUuid + "/" + charUuid;
            notificationCallbacks.put(key, callback);
            return true;
        }

        return false;
    }

    public boolean unsubscribeNotification(String serviceUuid, String charUuid) {
        if (!pythonServiceStarted) return false;

        Map<String, Object> params = new HashMap<>();
        params.put("char_uuid", charUuid);

        JsonObject response = sendCommand("unsubscribe", params);

        if (response != null && response.has("success") && response.get("success").getAsBoolean()) {
            String key = serviceUuid + "/" + charUuid;
            notificationCallbacks.remove(key);
            return true;
        }

        return false;
    }

    public Map<String, BleDevice> getDiscoveredDevices() {
        return new HashMap<>(discoveredDevices);
    }

    public BleDevice getConnectedDevice() {
        return connectedDevice;
    }

    public void cleanup() {
        disconnect();
        stopService();
    }

    // ==================== 回调接口 ====================

    public interface ScanCallback {
        void onDeviceFound(BleDevice device);
        void onScanComplete();
        void onScanFailed(String error);
    }

    public interface ConnectionCallback {
        void onConnected(BleDevice device);
        void onDisconnected(BleDevice device);
        void onConnectionFailed(String address, String error);
    }

    public interface NotificationCallback {
        void onDataReceived(String serviceUuid, String charUuid, byte[] data);
    }

    public interface InstallProgressCallback {
        void onProgress(String message);
    }

    // ==================== 数据类 ====================

    public static class BleDevice {
        private final String address;
        private String name;
        private int rssi;
        private boolean connected;

        public BleDevice(String address, String name, int rssi) {
            this.address = address;
            this.name = name;
            this.rssi = rssi;
            this.connected = false;
        }

        public String getAddress() { return address; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getRssi() { return rssi; }
        public void setRssi(int rssi) { this.rssi = rssi; }
        public boolean isConnected() { return connected; }
        public void setConnected(boolean connected) { this.connected = connected; }

        @Override
        public String toString() {
            return String.format("%s - %s (%d dBm)", address, name, rssi);
        }
    }

    public static class BleService {
        private final String uuid;
        private final String name;
        private final List<BleCharacteristic> characteristics = new ArrayList<>();

        public BleService(String uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }

        public String getUuid() { return uuid; }
        public String getName() { return name; }
        public List<BleCharacteristic> getCharacteristics() { return characteristics; }

        public void addCharacteristic(BleCharacteristic characteristic) {
            characteristics.add(characteristic);
        }
    }

    public static class BleCharacteristic {
        private final String uuid;
        private final String name;
        private final List<String> properties;

        public BleCharacteristic(String uuid, String name, List<String> properties) {
            this.uuid = uuid;
            this.name = name;
            this.properties = properties;
        }

        public String getUuid() { return uuid; }
        public String getName() { return name; }
        public List<String> getProperties() { return properties; }

        public boolean supportsRead() { return properties.contains("read"); }
        public boolean supportsWrite() { return properties.contains("write"); }
        public boolean supportsNotify() { return properties.contains("notify"); }
    }

    public static class LibraryInfo {
        public final boolean installed;
        public final String currentVersion;
        public final String latestVersion;

        public LibraryInfo(boolean installed, String currentVersion, String latestVersion) {
            this.installed = installed;
            this.currentVersion = currentVersion;
            this.latestVersion = latestVersion;
        }

        public boolean needsUpdate() {
            return installed && latestVersion != null && !latestVersion.equals(currentVersion);
        }
    }

    public static class InstallResult {
        public final boolean success;
        public final String message;
        public final Map<String, Boolean> packageStatus;

        public InstallResult(boolean success, String message, Map<String, Boolean> packageStatus) {
            this.success = success;
            this.message = message;
            this.packageStatus = packageStatus;
        }
    }
}

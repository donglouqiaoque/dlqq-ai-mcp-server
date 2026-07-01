package com.donglouqiaoque.aitools.mcp.tools;

import java.io.*;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * BLE工具 - 简化版
 * Java只负责启动Python GUI进程，AI通过Socket直接与Python通信
 */
public class BleTools {

    // 多实例管理 - key是Python端口号
    private static final Map<Integer, BleTools> instances = new ConcurrentHashMap<>();

    // Python进程
    private Process pythonProcess;
    private int pythonPort;  // Python端口号（直接）
    private volatile boolean isRunning = false;

    // JSON解析器
    private final Gson gson = new Gson();

    /**
     * 获取或创建实例
     */
    public static BleTools getInstance(int pythonPort) {
        return instances.computeIfAbsent(pythonPort, p -> new BleTools(p));
    }

    /**
     * 检查实例是否存在
     */
    public static boolean isInstanceRunning(int pythonPort) {
        BleTools tools = instances.get(pythonPort);
        return tools != null && tools.isRunning;
    }

    /**
     * 移除实例
     */
    public static void removeInstance(int pythonPort) {
        BleTools tools = instances.remove(pythonPort);
        if (tools != null) {
            tools.stop();
        }
    }

    private BleTools(int pythonPort) {
        this.pythonPort = pythonPort;
    }

    /**
     * 启动BLE工具（启动Python GUI进程）
     */
    public String start(int pythonPort) {
        if (isRunning) {
            return "BLE工具已在运行中，端口: " + pythonPort;
        }

        try {
            // 释放Python脚本
            String pythonDir = releasePythonScript();
            if (pythonDir == null) {
                return "错误：无法释放Python脚本";
            }

            // 检查Python
            String pythonCmd = findPythonCommand();
            if (pythonCmd == null) {
                return "错误：未找到Python 3，请先安装Python 3.8+";
            }

            // 检查bleak
            if (!checkBleak(pythonCmd, pythonDir)) {
                return "错误：bleak库未安装，请先运行 pip install bleak";
            }

            // 启动Python GUI进程
            String scriptPath = pythonDir + File.separator + "ble_tools.py";
            String version = getAppVersion();
            // 分离版本号、日期和时间，避免特殊字符问题
            String versionOnly;
            String buildDate;
            String buildTimeStr;
            if (version.contains("（")) {
                String[] parts = version.split("（");
                versionOnly = parts[0].replace("V", "");
                String timePart = parts[1].replace("）", "");
                String[] timeParts = timePart.split(" ");
                buildDate = timeParts[0].replace("-", ".");
                buildTimeStr = timeParts[1].replace(":", ".");
            } else {
                versionOnly = version.replace("V", "");
                buildDate = "unknown";
                buildTimeStr = "unknown";
            }
            ProcessBuilder pb = new ProcessBuilder(pythonCmd, scriptPath, 
                "--port", String.valueOf(pythonPort), 
                "--version", versionOnly,
                "--build-date", buildDate,
                "--build-time", buildTimeStr);
            pb.directory(new File(pythonDir));
            pb.redirectErrorStream(true);

            // 设置环境变量
            Map<String, String> env = pb.environment();
            env.put("PYTHONIOENCODING", "utf-8");

            pythonProcess = pb.start();
            isRunning = true;

            // 启动进程输出监控线程
            startProcessMonitor();

            // 等待Python启动（最多5秒）
            if (!waitForPythonStart(5)) {
                stop();
                return "错误：Python进程启动失败或超时";
            }

            return "BLE工具已启动，Python GUI窗口已打开，端口: " + pythonPort;

        } catch (Exception e) {
            stop();
            return "启动BLE工具失败: " + e.getMessage();
        }
    }

    /**
     * 停止BLE工具
     */
    public String stop() {
        if (!isRunning) {
            return "BLE工具未运行";
        }

        isRunning = false;

        // 发送退出命令
        try {
            sendCommand("exit", null);
        } catch (Exception e) {
            // 忽略
        }

        // 等待进程结束
        if (pythonProcess != null) {
            try {
                if (!pythonProcess.waitFor(3, TimeUnit.SECONDS)) {
                    pythonProcess.destroyForcibly();
                }
            } catch (Exception e) {
                pythonProcess.destroyForcibly();
            }
            pythonProcess = null;
        }

        instances.remove(pythonPort);
        return "BLE工具已停止";
    }

    /**
     * 发送命令到Python并返回结果
     */
    public JsonObject sendCommand(String type, Map<String, Object> params) {
        if (!isRunning) {
            JsonObject error = new JsonObject();
            error.addProperty("success", false);
            error.addProperty("error", "BLE工具未运行");
            return error;
        }

        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress("127.0.0.1", pythonPort), 5000);
            socket.setSoTimeout(30000);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            // 构建命令
            JsonObject command = new JsonObject();
            command.addProperty("type", type);
            if (params != null) {
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    Object value = entry.getValue();
                    String key = entry.getKey();
                    // 根据类型添加，保持数字、布尔、数组类型不被转为字符串
                    if (value instanceof Number) {
                        command.addProperty(key, (Number) value);
                    } else if (value instanceof Boolean) {
                        command.addProperty(key, (Boolean) value);
                    } else if (value instanceof java.util.List) {
                        // 处理List类型（如List<Integer>字节数组）
                        command.add(key, gson.toJsonTree(value));
                    } else {
                        command.addProperty(key, value.toString());
                    }
                }
            }

            // 发送命令
            out.println(command.toString());

            // 读取响应
            String response = in.readLine();
            if (response != null && !response.isEmpty()) {
                return gson.fromJson(response, JsonObject.class);
            }

        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("success", false);
            error.addProperty("error", "通信失败: " + e.getMessage());
            return error;
        }

        JsonObject error = new JsonObject();
        error.addProperty("success", false);
        error.addProperty("error", "无响应");
        return error;
    }

    /**
     * 等待Python启动
     */
    private boolean waitForPythonStart(int timeoutSeconds) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutSeconds * 1000) {
            try {
                // 尝试ping
                JsonObject response = sendCommand("ping", null);
                if (response != null && response.has("success") && response.get("success").getAsBoolean()) {
                    return true;
                }
            } catch (Exception e) {
                // 忽略，继续等待
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }
        }
        return false;
    }

    /**
     * 监控Python进程输出
     */
    private void startProcessMonitor() {
        Thread monitorThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(pythonProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.err.println("[Python] " + line);
                }
            } catch (Exception e) {
                System.err.println("[Python] 进程监控异常: " + e.getMessage());
            } finally {
                isRunning = false;
                System.err.println("[Python] 进程已结束");
            }
        }, "Python-Monitor-" + pythonPort);
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    /**
     * 释放Python脚本到项目目录（带文件锁，支持多进程并发）
     */
    private String releasePythonScript() {
        try {
            String projectDir = System.getProperty("user.dir");
            String pythonDir = projectDir + File.separator + ".dlqq" + File.separator + "script" + File.separator + "python";
            File dir = new File(pythonDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 创建 .dlqq/.gitignore 文件，忽略该目录下的所有内容
            File dlqqDir = new File(projectDir + File.separator + ".dlqq");
            File gitignoreFile = new File(dlqqDir, ".gitignore");
            if (!gitignoreFile.exists()) {
                try (FileWriter writer = new FileWriter(gitignoreFile)) {
                    writer.write("*\n");
                }
            }

            // 尝试将 .dlqq 目录设置为隐藏（Windows），失败则忽略
            try {
                java.nio.file.attribute.DosFileAttributeView dosView = java.nio.file.Files.getFileAttributeView(
                        dlqqDir.toPath(), java.nio.file.attribute.DosFileAttributeView.class);
                if (dosView != null) {
                    dosView.setHidden(true);
                }
            } catch (Exception ignored) {
                // 设置隐藏失败，忽略
            }

            // 从资源释放ble_tools.py（每次启动都强制覆盖，确保使用最新版本）
            String scriptPath = pythonDir + File.separator + "ble_tools.py";
            File scriptFile = new File(scriptPath);
            String lockFilePath = pythonDir + File.separator + ".ble_tools.lock";
            File lockFile = new File(lockFilePath);

            // 使用文件锁防止多进程竞争
            try (FileChannel lockChannel = FileChannel.open(lockFile.toPath(),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 FileLock lock = lockChannel.lock()) {

                // 持有锁后释放脚本（强制覆盖旧版本）
                try (InputStream is = getClass().getResourceAsStream("/py/ble_tools.py");
                     FileOutputStream fos = new FileOutputStream(scriptFile)) {
                    if (is == null) {
                        System.err.println("错误：找不到 ble_tools.py 资源文件");
                        return null;
                    }
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = is.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
                System.err.println("已释放 ble_tools.py 到: " + scriptPath);

                // 释放图标文件到 .dlqq/image 目录
                String imageDir = projectDir + File.separator + ".dlqq" + File.separator + "image";
                File imgDir = new File(imageDir);
                if (!imgDir.exists()) {
                    imgDir.mkdirs();
                }
                String iconPath = imageDir + File.separator + "logo.ico";
                File iconFile = new File(iconPath);
                try (InputStream is = getClass().getResourceAsStream("/image/logo.ico");
                     FileOutputStream fos = new FileOutputStream(iconFile)) {
                    if (is != null) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = is.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                        System.err.println("已释放 logo.ico 到: " + iconPath);
                    }
                } catch (Exception e) {
                    System.err.println("释放图标文件失败: " + e.getMessage());
                }
            }

            return pythonDir;
        } catch (Exception e) {
            System.err.println("释放Python脚本失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 查找Python命令
     */
    private String findPythonCommand() {
        String[] commands = {"python3", "python", "py"};
        for (String cmd : commands) {
            try {
                ProcessBuilder pb = new ProcessBuilder(cmd, "--version");
                pb.redirectErrorStream(true);
                Process process = pb.start();
                boolean finished = process.waitFor(3, TimeUnit.SECONDS);
                if (finished && process.exitValue() == 0) {
                    return cmd;
                }
            } catch (Exception e) {
                // 尝试下一个
            }
        }
        return null;
    }

    /**
     * 检查bleak库
     */
    private boolean checkBleak(String pythonCmd, String pythonDir) {
        try {
            ProcessBuilder pb = new ProcessBuilder(pythonCmd, "-c", "import bleak; print('ok')");
            pb.directory(new File(pythonDir));
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== 静态工具方法 ====================

    public static String startBle(int port) {
        return getInstance(port).start(port);
    }

    public static String stopBle(int port) {
        BleTools tools = instances.get(port);
        if (tools == null) {
            return "BLE工具未运行";
        }
        return tools.stop();
    }

    public static boolean isRunning(int pythonPort) {
        return isInstanceRunning(pythonPort);
    }

    // ==================== 静态工具方法（供AITools调用） ====================

    public static String runBle(int pythonPort) {
        return getInstance(pythonPort).start(pythonPort);
    }

    public static String scanBleDevices(int port, int timeoutSec) {
        if (!isInstanceRunning(port)) {
            return "错误：BLE工具未启动，请先使用 dlqq_run_ble 启动";
        }
        BleTools tools = getInstance(port);
        // 直接传递秒
        JsonObject result = tools.sendCommand("scan", Map.of("timeout", timeoutSec));
        if (result != null && result.has("success") && result.get("success").getAsBoolean()) {
            try { Thread.sleep(timeoutSec * 1000 + 500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return getBleDevices(port);
        }
        return "扫描失败";
    }

    public static String getBleDevices(int port) {
        if (!isInstanceRunning(port)) return "错误：BLE工具未启动";
        BleTools tools = getInstance(port);
        JsonObject result = tools.sendCommand("get_devices", null);
        if (result != null && result.has("success") && result.get("success").getAsBoolean() && result.has("devices")) {
            var devices = result.getAsJsonArray("devices");
            if (devices.size() == 0) return "扫描到的BLE设备:\n  未发现BLE设备\n";
            StringBuilder sb = new StringBuilder("扫描到的BLE设备:\n");
            for (int i = 0; i < devices.size(); i++) {
                var dev = devices.get(i).getAsJsonObject();
                sb.append(String.format("  %d. %s - %s (%ddBm)%n", i + 1,
                    dev.get("address").getAsString(),
                    dev.get("name").getAsString(),
                    dev.get("rssi").getAsInt()));
            }
            return sb.toString();
        }
        return "获取设备列表失败";
    }

    public static String connectBleDevice(int port, String address) {
        if (!isInstanceRunning(port)) return "错误：BLE工具未启动";
        BleTools tools = getInstance(port);
        JsonObject result = tools.sendCommand("connect", Map.of("address", address));
        return (result != null && result.get("success").getAsBoolean()) ? "正在连接设备..." : "连接失败";
    }

    public static String disconnectBleDevice(int port) {
        if (!isInstanceRunning(port)) return "错误：BLE工具未启动";
        BleTools tools = getInstance(port);
        JsonObject result = tools.sendCommand("disconnect", null);
        return (result != null && result.get("success").getAsBoolean()) ? "已断开连接" : "断开失败";
    }

    public static String discoverBleServices(int port) {
        if (!isInstanceRunning(port)) return "错误：BLE工具未启动";
        BleTools tools = getInstance(port);
        JsonObject result = tools.sendCommand("discover", null);
        if (result != null && result.get("success").getAsBoolean() && result.has("services")) {
            StringBuilder sb = new StringBuilder("BLE服务和特征:\n");
            var services = result.getAsJsonArray("services");
            for (var svc : services) {
                var s = svc.getAsJsonObject();
                sb.append("\n服务: ").append(s.get("uuid").getAsString()).append("\n");
                if (s.has("characteristics")) {
                    for (var c : s.getAsJsonArray("characteristics")) {
                        var ch = c.getAsJsonObject();
                        sb.append(String.format("  特征: %s [%s]%n",
                            ch.get("uuid").getAsString(),
                            ch.getAsJsonArray("properties").toString()));
                    }
                }
            }
            return sb.toString();
        }
        return "发现服务失败";
    }

    public static String readBleCharacteristic(int port, String serviceUuid, String charUuid) {
        if (!isInstanceRunning(port)) return "错误：BLE工具未启动";
        BleTools tools = getInstance(port);
        JsonObject result = tools.sendCommand("read", Map.of("char_uuid", charUuid));
        if (result != null && result.get("success").getAsBoolean()) {
            return String.format("读取结果:\n  HEX: %s\n  Text: %s",
                result.get("hex").getAsString(), result.get("text").getAsString());
        }
        return "读取失败";
    }

    public static String writeBleCharacteristicText(int port, String serviceUuid, String charUuid, String text) {
        if (!isInstanceRunning(port)) return "错误：BLE工具未启动";
        BleTools tools = getInstance(port);
        JsonObject result = tools.sendCommand("write", Map.of("char_uuid", charUuid, "data", text));
        return (result != null && result.get("success").getAsBoolean()) ? "写入成功" : "写入失败";
    }

    public static String writeBleCharacteristicHex(int port, String serviceUuid, String charUuid, java.util.List<String> hexArray) {
        if (!isInstanceRunning(port)) return "错误：BLE工具未启动";
        StringBuilder hex = new StringBuilder();
        for (String h : hexArray) hex.append(h);
        java.util.List<Integer> data = new java.util.ArrayList<>();
        for (int i = 0; i < hex.length(); i += 2) {
            data.add(Integer.parseInt(hex.substring(i, i + 2), 16));
        }
        BleTools tools = getInstance(port);
        JsonObject result = tools.sendCommand("write", Map.of("char_uuid", charUuid, "data", data));
        return (result != null && result.get("success").getAsBoolean()) ? "写入成功" : "写入失败";
    }

    public static String subscribeBleCharacteristic(int port, String serviceUuid, String charUuid, boolean subscribe) {
        if (!isInstanceRunning(port)) return "错误：BLE工具未启动";
        BleTools tools = getInstance(port);
        JsonObject result = tools.sendCommand("subscribe", Map.of("char_uuid", charUuid, "subscribe", subscribe));
        return (result != null && result.get("success").getAsBoolean()) ? (subscribe ? "订阅成功" : "取消订阅成功") : "操作失败";
    }

    public static String exitBle(int port) {
        BleTools tools = instances.get(port);
        return tools != null ? tools.stop() : "BLE工具未运行";
    }

    // 兼容方法（简化版）
    public static String bleLibHas() {
        return "Python BLE库检查已集成到启动流程中";
    }

    public static String bleLibInstall() {
        return "请在命令行运行: pip install bleak";
    }

    public static String bleLibUpdate() {
        return "请在命令行运行: pip install --upgrade bleak";
    }

    public static String bleReleaseScript() {
        return "脚本已自动释放";
    }

    public static String subscribeBleNotifications(Integer port, String serviceUuid, String charUuid, Boolean subscribe) {
        return subscribeBleCharacteristic(port, serviceUuid, charUuid, subscribe);
    }

    public static String getBleLog(Integer port, Integer startLine, Integer endLine, Integer lineCount, Integer timeoutSec) {
        if (!isInstanceRunning(port)) return "错误：BLE工具未启动";
        BleTools tools = getInstance(port);
        JsonObject result = tools.sendCommand("get_log", Map.of("lines", lineCount != null ? lineCount : 50));
        if (result != null && result.has("success") && result.get("success").getAsBoolean() && result.has("logs")) {
            var logs = result.getAsJsonArray("logs");
            StringBuilder sb = new StringBuilder();
            for (var log : logs) sb.append(log.getAsString()).append("\n");
            return sb.toString();
        }
        return "获取日志失败";
    }

    public static String getBleLogText(Integer port, Integer startLine, Integer endLine, Integer lineCount, Integer timeoutSec) {
        return getBleLog(port, startLine, endLine, lineCount, timeoutSec);
    }

    public static String getBleLogHex(Integer port, Integer startLine, Integer endLine, Integer lineCount, Integer timeoutSec) {
        return getBleLog(port, startLine, endLine, lineCount, timeoutSec);
    }

    public static String clearBleLog(Integer port) {
        if (!isInstanceRunning(port)) return "错误：BLE工具未启动";
        BleTools tools = getInstance(port);
        JsonObject result = tools.sendCommand("clear", null);
        return (result != null && result.get("success").getAsBoolean()) ? "已清空" : "清空失败";
    }

    public static String getLineCount(Integer port) {
        if (!isInstanceRunning(port)) return "错误：BLE工具未启动";
        return "请使用 get_log 获取日志";
    }

    public static String setBleBufferSize(Integer port, Integer size) {
        return "缓冲区大小设置暂不支持";
    }

    public static String switchDisplayFormat(Integer port, String format) {
        if (!isInstanceRunning(port)) return "错误：BLE工具未启动";
        BleTools tools = getInstance(port);
        JsonObject result = tools.sendCommand("switch_format", Map.of("format", format));
        return (result != null && result.get("success").getAsBoolean()) ? "已切换格式" : "切换失败";
    }

    /**
     * 获取应用版本号和构建时间
     * 格式：x.x.x（xxx.xx.xx xx.xx.xx）
     */
    private static String getAppVersion() {
        try {
            // 获取当前类的JAR文件路径
            String classPath = BleTools.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            // URL解码处理中文路径
            classPath = java.net.URLDecoder.decode(classPath, "UTF-8");
            File jarFile = new File(classPath);

            if (jarFile.exists()) {
                // 读取MANIFEST.MF
                try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile)) {
                    java.util.jar.Manifest manifest = jar.getManifest();
                    if (manifest != null) {
                        java.util.jar.Attributes mainAttrs = manifest.getMainAttributes();
                        String version = mainAttrs.getValue("Implementation-Version");

                        // 获取构建时间
                        String buildTime = null;
                        String buildTimeAttr = mainAttrs.getValue("Build-Time");
                        if (buildTimeAttr != null) {
                            try {
                                // 将UTC时间转换为本地时间
                                java.time.LocalDateTime utcTime = java.time.LocalDateTime.parse(buildTimeAttr,
                                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                                java.time.ZonedDateTime utcZoned = utcTime.atZone(java.time.ZoneId.of("UTC"));
                                java.time.ZonedDateTime localZoned = utcZoned.withZoneSameInstant(java.time.ZoneId.systemDefault());
                                buildTime = localZoned.format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd HH.mm.ss"));
                            } catch (Exception e) {
                                // 如果解析失败，使用JAR文件修改时间
                            }
                        }

                        // 如果没有从MANIFEST获取到构建时间，使用JAR文件修改时间
                        if (buildTime == null) {
                            long lastModified = jarFile.lastModified();
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy.MM.dd HH.mm.ss");
                            buildTime = sdf.format(new java.util.Date(lastModified));
                        }

                        if (version != null) {
                            return version + "（" + buildTime + "）";
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 出错时返回无法获取
        }
        return "无法获取";
    }

    /**
     * 检查Windows电脑的蓝牙适配器是否已开启
     */
    public static String checkBluetoothOpen() {
        StringBuilder result = new StringBuilder();
        result.append("=== 蓝牙状态检查 ===\n\n");

        try {
            // 方法1: 检查蓝牙设备状态
            result.append("[1/3] 检查蓝牙设备状态...\n");
            ProcessBuilder checkPb = new ProcessBuilder(
                "powershell.exe",
                "-Command",
                "Get-PnpDevice -Class Bluetooth | Select-Object Name, Status, InstanceId | Format-Table -AutoSize"
            );
            checkPb.redirectErrorStream(true);
            Process checkProcess = checkPb.start();

            StringBuilder checkOutput = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(checkProcess.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    checkOutput.append(line).append("\n");
                }
            }
            checkProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            result.append(checkOutput.toString()).append("\n");

            // 方法2: 检查蓝牙服务状态
            result.append("[2/3] 检查蓝牙服务状态...\n");
            ProcessBuilder pb2 = new ProcessBuilder(
                "powershell.exe",
                "-Command",
                "$service = Get-Service -Name bthserv -ErrorAction SilentlyContinue; " +
                "if ($service) { " +
                "    Write-Host ('蓝牙服务状态: ' + $service.Status) " +
                "} else { " +
                "    Write-Host '未找到蓝牙服务' " +
                "}"
            );
            pb2.redirectErrorStream(true);
            Process process2 = pb2.start();

            StringBuilder output2 = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process2.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output2.append(line).append("\n");
                }
            }
            process2.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            result.append(output2.toString()).append("\n");

            // 方法3: 检查蓝牙网络适配器
            result.append("[3/3] 检查蓝牙网络适配器...\n");
            ProcessBuilder pb3 = new ProcessBuilder(
                "powershell.exe",
                "-Command",
                "$adapter = Get-NetAdapter | Where-Object {$_.InterfaceDescription -like '*Bluetooth*'}; " +
                "if ($adapter) { " +
                "    Write-Host ('蓝牙适配器: ' + $adapter.Name + ' - 状态: ' + $adapter.Status) " +
                "} else { " +
                "    Write-Host '未找到蓝牙网络适配器' " +
                "}"
            );
            pb3.redirectErrorStream(true);
            Process process3 = pb3.start();

            StringBuilder output3 = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process3.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output3.append(line).append("\n");
                }
            }
            process3.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            result.append(output3.toString()).append("\n");

            result.append("\n=== 检查完成 ===\n");
            result.append("如需开启蓝牙，请手动操作:\n");
            result.append("1. 点击任务栏右下角的通知图标 → 展开快速设置 → 点击蓝牙图标开启\n");
            result.append("2. 或按 Win+I → 蓝牙和设备 → 开启蓝牙开关\n");
            result.append("3. 部分电脑需要按 Fn+功能键 开启蓝牙硬件开关\n");

            return result.toString();
        } catch (Exception e) {
            result.append("\n错误: ").append(e.getMessage()).append("\n");
            result.append(e.toString()).append("\n");
            return result.toString();
        }
    }
}

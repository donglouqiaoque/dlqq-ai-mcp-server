package com.donglouqiaoque.dlqqaimcpserver.mcp.tools;

import com.donglouqiaoque.dlqqaimcpserver.mcp.protocol.McpSchema.CallToolResult;
import com.donglouqiaoque.dlqqaimcpserver.mcp.protocol.McpSchema.ToolContent;
import com.donglouqiaoque.dlqqaimcpserver.mcp.protocol.McpSchema.ToolInputSchema;
import com.donglouqiaoque.dlqqaimcpserver.mcp.server.McpServer;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WiFi工具 - 扫描/连接WiFi网络（Java原生实现，通过系统命令操作）
 * Windows: netsh wlan; Linux: nmcli
 * 优先Java原生实现，不依赖Python
 */
public class WifiTools {

    // 实例管理（key为端口号，与USB工具修复后的逻辑一致）
    private static final Map<Integer, WifiTools> instances = new ConcurrentHashMap<>();

    // 实例状态
    private final int port;
    private volatile boolean isRunning = false;

    // 扫描结果缓存
    private volatile List<Map<String, String>> networkCache = new ArrayList<>();
    private volatile long lastScanTime = 0;

    // 日志缓冲区
    private final List<String> logBuffer = Collections.synchronizedList(new ArrayList<>());

    // GUI窗口
    private JFrame guiWindow = null;
    private DefaultTableModel tableModel = null;
    private JTextArea logArea = null;
    private JLabel statusLabel = null;

    private WifiTools(int port) {
        this.port = port;
    }

    // ==================== 实例管理 ====================

    public static WifiTools createInstance(int port) {
        return instances.computeIfAbsent(port, WifiTools::new);
    }

    public static WifiTools getInstance(int port) {
        return instances.get(port);
    }

    public static boolean isInstanceRunning(int port) {
        WifiTools tools = instances.get(port);
        return tools != null && tools.isRunning;
    }

    public static boolean hasInstance(int port) {
        return instances.containsKey(port);
    }

    public static void removeInstance(int port) {
        WifiTools tools = instances.remove(port);
        if (tools != null) {
            tools.stop();
        }
    }

    // ==================== 启动/停止 ====================

    public String start() {
        if (isRunning) {
            return "WiFi工具已在运行中，端口: " + port;
        }
        try {
            SwingUtilities.invokeLater(this::createGUI);
            isRunning = true;
            log("WiFi工具实例已启动，端口: " + port);
            return "WiFi工具已启动，端口: " + port;
        } catch (Exception e) {
            return "错误：启动WiFi工具失败: " + e.getMessage();
        }
    }

    public String stop() {
        if (!isRunning) {
            return "WiFi工具实例未在运行，端口: " + port;
        }
        if (guiWindow != null) {
            SwingUtilities.invokeLater(() -> {
                guiWindow.dispose();
                guiWindow = null;
            });
        }
        isRunning = false;
        log("WiFi工具实例已停止");
        return "WiFi工具已停止";
    }

    public String restart() {
        stop();
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return start();
    }

    // ==================== GUI ====================

    private void createGUI() {
        if (guiWindow != null) {
            guiWindow.toFront();
            return;
        }

        guiWindow = new JFrame("WiFi 工具 (端口 " + port + ")");
        guiWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        guiWindow.setSize(720, 520);
        guiWindow.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // 顶部：状态栏 + 按钮
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        statusLabel = new JLabel("状态: 未连接");
        topPanel.add(statusLabel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        JButton scanButton = new JButton("扫描网络");
        scanButton.addActionListener(e -> {
            String result = scanNetworks(10);
            updateStatus("扫描完成");
            if (result.startsWith("错误")) {
                appendLog(result);
            }
        });
        JButton refreshStatusButton = new JButton("刷新状态");
        refreshStatusButton.addActionListener(e -> {
            String status = getStatus();
            updateStatus(parseStatusSummary(status));
        });
        buttonPanel.add(scanButton);
        buttonPanel.add(refreshStatusButton);
        topPanel.add(buttonPanel, BorderLayout.EAST);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // 中间：网络列表表格
        tableModel = new DefaultTableModel(new String[]{"SSID", "信号", "认证", "频道"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(tableModel);
        mainPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        // 底部：日志
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setPreferredSize(new Dimension(0, 140));
        mainPanel.add(logScroll, BorderLayout.SOUTH);

        guiWindow.setContentPane(mainPanel);
        guiWindow.setVisible(true);
    }

    private void updateStatus(String status) {
        if (statusLabel != null) {
            SwingUtilities.invokeLater(() -> statusLabel.setText("状态: " + status));
        }
    }

    private void updateNetworkTable(List<Map<String, String>> networks) {
        if (tableModel == null) return;
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);
            for (Map<String, String> net : networks) {
                tableModel.addRow(new Object[]{
                    net.getOrDefault("ssid", ""),
                    net.getOrDefault("signal", ""),
                    net.getOrDefault("auth", ""),
                    net.getOrDefault("channel", "")
                });
            }
        });
    }

    private void appendLog(String message) {
        if (logArea != null) {
            SwingUtilities.invokeLater(() -> {
                logArea.append(message + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            });
        }
    }

    // ==================== 日志 ====================

    private void log(String message) {
        String timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        String line = "[" + timestamp + "] " + message;
        logBuffer.add(line);
        appendLog(line);
    }

    public List<String> getLogBuffer() {
        return new ArrayList<>(logBuffer);
    }

    public void clearLog() {
        logBuffer.clear();
    }

    // ==================== 核心功能 ====================

    /**
     * 扫描WiFi网络
     */
    public String scanNetworks(int timeoutSec) {
        try {
            List<Map<String, String>> networks = doScan(timeoutSec);
            networkCache = networks;
            lastScanTime = System.currentTimeMillis();
            log("扫描完成，发现 " + networks.size() + " 个网络");
            return formatNetworks(networks);
        } catch (IOException e) {
            return "错误：扫描失败: " + e.getMessage();
        }
    }

    /**
     * 执行扫描
     */
    private List<Map<String, String>> doScan(int timeoutSec) throws IOException {
        List<Map<String, String>> networks = new ArrayList<>();
        String os = getOsName();

        if (os.contains("win")) {
            String output = executeCommand(timeoutSec, "netsh", "wlan", "show", "networks", "mode=bssid");
            networks = parseWindowsNetworks(output);
        } else {
            String output = executeCommand(timeoutSec, "nmcli", "-t", "-f", "SSID,SIGNAL,SECURITY,CHAN", "dev", "wifi", "list");
            networks = parseLinuxNetworks(output);
        }
        return networks;
    }

    /**
     * 解析Windows netsh输出
     */
    private List<Map<String, String>> parseWindowsNetworks(String output) {
        List<Map<String, String>> networks = new ArrayList<>();
        // 按 "SSID n : xxx" 分段
        Pattern ssidPattern = Pattern.compile("SSID\\s+\\d+\\s*:\\s*(.*)", Pattern.CASE_INSENSITIVE);
        Matcher ssidMatcher = ssidPattern.matcher(output);

        List<int[]> segments = new ArrayList<>();
        while (ssidMatcher.find()) {
            segments.add(new int[]{ssidMatcher.start(), ssidMatcher.end()});
        }

        String[] lines = output.split("\r?\n");
        Map<String, String> current = null;
        for (String line : lines) {
            String trimmed = line.trim();
            Matcher m = ssidPattern.matcher(trimmed);
            if (m.matches()) {
                if (current != null) networks.add(current);
                current = new LinkedHashMap<>();
                current.put("ssid", m.group(1).trim());
                continue;
            }
            if (current == null) continue;

            // 身份验证 / Authentication
            Matcher authM = Pattern.compile("(身份验证|Authentication)\\s*:\\s*(.+)").matcher(trimmed);
            if (authM.matches()) {
                current.put("auth", authM.group(2).trim());
            }
            // 加密 / Encryption
            Matcher encM = Pattern.compile("(加密|Encryption)\\s*:\\s*(.+)").matcher(trimmed);
            if (encM.matches()) {
                current.putIfAbsent("encryption", encM.group(2).trim());
            }
            // 信号（第一个BSSID的信号）/ Signal
            Matcher sigM = Pattern.compile("(信号|Signal)\\s*:\\s*(.+)").matcher(trimmed);
            if (sigM.matches()) {
                current.putIfAbsent("signal", sigM.group(2).trim());
            }
            // 频道 / Channel
            Matcher chM = Pattern.compile("(频道|Channel)\\s*:\\s*(.+)").matcher(trimmed);
            if (chM.matches()) {
                current.putIfAbsent("channel", chM.group(2).trim());
            }
        }
        if (current != null) networks.add(current);
        return networks;
    }

    /**
     * 解析Linux nmcli输出（-t 分号分隔）
     */
    private List<Map<String, String>> parseLinuxNetworks(String output) {
        List<Map<String, String>> networks = new ArrayList<>();
        String[] lines = output.split("\r?\n");
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            String[] parts = line.split(":");
            if (parts.length >= 3) {
                Map<String, String> net = new LinkedHashMap<>();
                net.put("ssid", parts[0]);
                net.put("signal", parts[1] + "%");
                net.put("auth", parts[2]);
                net.put("channel", parts.length > 3 ? parts[3] : "");
                networks.add(net);
            }
        }
        return networks;
    }

    private String formatNetworks(List<Map<String, String>> networks) {
        StringBuilder sb = new StringBuilder();
        sb.append("【WiFi网络扫描结果】\n");
        sb.append("共找到 ").append(networks.size()).append(" 个网络\n\n");
        for (int i = 0; i < networks.size(); i++) {
            Map<String, String> net = networks.get(i);
            sb.append(i + 1).append(". SSID: ").append(net.getOrDefault("ssid", "?"))
              .append(" | 信号: ").append(net.getOrDefault("signal", "?"))
              .append(" | 认证: ").append(net.getOrDefault("auth", "?"))
              .append(" | 频道: ").append(net.getOrDefault("channel", "?"))
              .append("\n");
        }
        return sb.toString();
    }

    /**
     * 获取当前WiFi状态
     */
    public String getStatus() {
        try {
            String os = getOsName();
            String output;
            if (os.contains("win")) {
                output = executeCommand(10, "netsh", "wlan", "show", "interfaces");
            } else {
                output = executeCommand(10, "nmcli", "dev", "status");
            }
            return output;
        } catch (IOException e) {
            return "错误：获取状态失败: " + e.getMessage();
        }
    }

    /**
     * 从状态输出中提取摘要
     */
    private String parseStatusSummary(String statusOutput) {
        for (String line : statusOutput.split("\r?\n")) {
            String trimmed = line.trim();
            if (trimmed.contains("SSID") && trimmed.contains(":")) {
                int idx = trimmed.indexOf(':');
                String ssid = trimmed.substring(idx + 1).trim();
                if (!ssid.isEmpty() && !"无".equals(ssid)) {
                    return "已连接: " + ssid;
                }
            }
            if (trimmed.contains("状态") && trimmed.contains(":")) {
                int idx = trimmed.indexOf(':');
                String state = trimmed.substring(idx + 1).trim();
                if (!state.isEmpty()) {
                    return state;
                }
            }
        }
        return "未知";
    }

    /**
     * 列出已保存的WiFi配置
     */
    public String getProfiles() {
        try {
            String os = getOsName();
            if (os.contains("win")) {
                return executeCommand(10, "netsh", "wlan", "show", "profiles");
            } else {
                return executeCommand(10, "nmcli", "-t", "-f", "NAME,TYPE", "connection", "show");
            }
        } catch (IOException e) {
            return "错误：获取配置列表失败: " + e.getMessage();
        }
    }

    /**
     * 连接WiFi
     */
    public String connect(String ssid, String password, int timeoutSec) {
        if (ssid == null || ssid.trim().isEmpty()) {
            return "错误：SSID不能为空";
        }
        ssid = ssid.trim();
        try {
            String os = getOsName();
            if (os.contains("win")) {
                return connectWindows(ssid, password, timeoutSec);
            } else {
                return connectLinux(ssid, password, timeoutSec);
            }
        } catch (IOException e) {
            return "错误：连接失败: " + e.getMessage();
        }
    }

    private String connectWindows(String ssid, String password, int timeoutSec) throws IOException {
        StringBuilder result = new StringBuilder();
        boolean needProfile = password != null && !password.trim().isEmpty();

        if (needProfile) {
            // 生成并添加WLAN配置文件
            Path profileFile = Files.createTempFile("wifi_", ".xml");
            try {
                String xml = buildWlanProfileXml(ssid, password);
                Files.writeString(profileFile, xml, StandardCharsets.UTF_8);
                String addResult = executeCommand(10, "netsh", "wlan", "add", "profile", "filename=" + profileFile.toAbsolutePath());
                result.append("添加配置文件: ").append(addResult.trim()).append("\n");
            } finally {
                Files.deleteIfExists(profileFile);
            }
        }

        String connectResult = executeCommand(timeoutSec, "netsh", "wlan", "connect", "name=" + ssid);
        result.append("连接命令输出: ").append(connectResult.trim()).append("\n");
        log("尝试连接WiFi: " + ssid);
        return result.toString();
    }

    private String connectLinux(String ssid, String password, int timeoutSec) throws IOException {
        List<String> cmd = new ArrayList<>();
        cmd.add("nmcli");
        cmd.add("dev");
        cmd.add("wifi");
        cmd.add("connect");
        cmd.add(ssid);
        if (password != null && !password.trim().isEmpty()) {
            cmd.add("password");
            cmd.add(password);
        }
        return executeCommand(timeoutSec, cmd.toArray(new String[0]));
    }

    /**
     * 生成Windows WLAN配置文件XML（支持WPA/WPA2-PSK）
     */
    private String buildWlanProfileXml(String ssid, String password) {
        return "<?xml version=\"1.0\"?>\n" +
            "<WLANProfile xmlns=\"http://www.microsoft.com/networking/WLAN/profile/v1\">\n" +
            "    <name>" + xmlEscape(ssid) + "</name>\n" +
            "    <SSIDConfig>\n" +
            "        <SSID>\n" +
            "            <name>" + xmlEscape(ssid) + "</name>\n" +
            "        </SSID>\n" +
            "    </SSIDConfig>\n" +
            "    <connectionType>ESS</connectionType>\n" +
            "    <connectionMode>auto</connectionMode>\n" +
            "    <MSM>\n" +
            "        <security>\n" +
            "            <authEncryption>\n" +
            "                <authentication>WPA2PSK</authentication>\n" +
            "                <encryption>AES</encryption>\n" +
            "                <useOneX>false</useOneX>\n" +
            "            </authEncryption>\n" +
            "            <sharedKey>\n" +
            "                <keyType>passPhrase</keyType>\n" +
            "                <protected>false</protected>\n" +
            "                <keyMaterial>" + xmlEscape(password) + "</keyMaterial>\n" +
            "            </sharedKey>\n" +
            "        </security>\n" +
            "    </MSM>\n" +
            "</WLANProfile>\n";
    }

    private static String xmlEscape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    /**
     * 断开WiFi
     */
    public String disconnect() {
        try {
            String os = getOsName();
            if (os.contains("win")) {
                return executeCommand(10, "netsh", "wlan", "disconnect");
            } else {
                return executeCommand(10, "nmcli", "dev", "wifi", "disconnect");
            }
        } catch (IOException e) {
            return "错误：断开失败: " + e.getMessage();
        }
    }

    // ==================== 辅助 ====================

    private String getOsName() {
        return System.getProperty("os.name", "").toLowerCase();
    }

    /**
     * 执行系统命令并返回输出（UTF-8/GBK自动检测）
     */
    private String executeCommand(int timeoutSec, String... cmd) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // 读取原始字节，再按编码解码（Windows中文系统netsh输出为GBK）
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        try (InputStream is = process.getInputStream()) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = is.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
        }
        String output = decodeBytes(bos.toByteArray());

        boolean finished;
        try {
            finished = process.waitFor(timeoutSec, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IOException("命令执行被中断");
        }
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("命令执行超时（" + timeoutSec + "秒）");
        }
        return output;
    }

    /**
     * 解码命令输出字节：优先UTF-8严格解码，失败则回退GBK（Windows中文系统）
     */
    private String decodeBytes(byte[] raw) {
        try {
            java.nio.charset.CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT);
            return decoder.decode(java.nio.ByteBuffer.wrap(raw)).toString();
        } catch (java.nio.charset.CharacterCodingException e) {
            // UTF-8解码失败，回退到系统代码页（中文系统为GBK）
            try {
                return new String(raw, "GBK");
            } catch (Exception e2) {
                return new String(raw, StandardCharsets.ISO_8859_1);
            }
        }
    }

    // ==================== MCP工具注册 ====================

    public static void registerTools(McpServer server) {
        registerRunWifiTool(server);
        registerWifiScanTool(server);
        registerWifiGetNetworksTool(server);
        registerWifiStatusTool(server);
        registerWifiProfilesTool(server);
        registerWifiConnectTool(server);
        registerWifiDisconnectTool(server);
        registerWifiGetLogTool(server);
        registerWifiClearLogTool(server);
        registerWifiGetLineCountTool(server);
        registerExitWifiTool(server);
        registerRerunWifiTool(server);
    }

    private static CallToolResult ok(String text) {
        return new CallToolResult(Collections.singletonList(ToolContent.text(text)), false);
    }

    private static CallToolResult err(String text) {
        return new CallToolResult(Collections.singletonList(ToolContent.text(text)), true);
    }

    private static int parsePort(Object obj) {
        try {
            return Integer.parseInt(String.valueOf(obj).trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static void registerRunWifiTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "string");
        portProp.put("description", "实例端口号，用于区分不同的WiFi工具实例");
        properties.put("port", portProp);
        ToolInputSchema schema = new ToolInputSchema(properties, Collections.singletonList("port"));

        server.addTool("dlqq_run_wifi",
            "启动WiFi工具（打开图形界面窗口）。端口号用于区分不同的WiFi工具实例。",
            schema,
            args -> {
                int port = parsePort(args.get("port"));
                if (port <= 0) return err("无效的端口号");
                if (hasInstance(port)) return err("WiFi工具实例已在运行中，端口: " + port);
                WifiTools instance = createInstance(port);
                return ok(instance.start());
            }
        );
    }

    private static void registerWifiScanTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "string");
        portProp.put("description", "WiFi工具实例端口号");
        properties.put("port", portProp);
        Map<String, Object> timeoutProp = new HashMap<>();
        timeoutProp.put("type", "integer");
        timeoutProp.put("description", "扫描超时时间（秒，默认10秒）");
        properties.put("timeout", timeoutProp);
        ToolInputSchema schema = new ToolInputSchema(properties, Collections.singletonList("port"));

        server.addTool("dlqq_wifi_scan",
            "扫描周围的WiFi网络。扫描完成后使用 dlqq_wifi_get_networks 查看结果，或直接返回扫描结果。",
            schema,
            args -> {
                int port = parsePort(args.get("port"));
                if (port <= 0) return err("无效的端口号");
                WifiTools instance = getInstance(port);
                if (instance == null || !isInstanceRunning(port)) return err("WiFi工具未启动，请先使用 dlqq_run_wifi");
                int timeout = 10;
                if (args.get("timeout") != null) {
                    try { timeout = Integer.parseInt(String.valueOf(args.get("timeout"))); } catch (NumberFormatException ignored) {}
                }
                String result = instance.scanNetworks(timeout);
                return result.startsWith("错误") ? err(result) : ok(result);
            }
        );
    }

    private static void registerWifiGetNetworksTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "string");
        portProp.put("description", "WiFi工具实例端口号");
        properties.put("port", portProp);
        ToolInputSchema schema = new ToolInputSchema(properties, Collections.singletonList("port"));

        server.addTool("dlqq_wifi_get_networks",
            "获取最近一次扫描的WiFi网络列表（不需要重新扫描）。",
            schema,
            args -> {
                int port = parsePort(args.get("port"));
                if (port <= 0) return err("无效的端口号");
                WifiTools instance = getInstance(port);
                if (instance == null || !isInstanceRunning(port)) return err("WiFi工具未启动，请先使用 dlqq_run_wifi");
                if (instance.networkCache.isEmpty()) return ok("尚未扫描，请先使用 dlqq_wifi_scan 扫描");
                return ok(instance.formatNetworks(instance.networkCache));
            }
        );
    }

    private static void registerWifiStatusTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "string");
        portProp.put("description", "WiFi工具实例端口号");
        properties.put("port", portProp);
        ToolInputSchema schema = new ToolInputSchema(properties, Collections.singletonList("port"));

        server.addTool("dlqq_wifi_status",
            "获取当前WiFi连接状态（是否已连接、连接的SSID、信号强度等）。",
            schema,
            args -> {
                int port = parsePort(args.get("port"));
                if (port <= 0) return err("无效的端口号");
                WifiTools instance = getInstance(port);
                if (instance == null || !isInstanceRunning(port)) return err("WiFi工具未启动，请先使用 dlqq_run_wifi");
                return ok(instance.getStatus());
            }
        );
    }

    private static void registerWifiProfilesTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "string");
        portProp.put("description", "WiFi工具实例端口号");
        properties.put("port", portProp);
        ToolInputSchema schema = new ToolInputSchema(properties, Collections.singletonList("port"));

        server.addTool("dlqq_wifi_profiles",
            "列出系统已保存的WiFi配置文件列表。",
            schema,
            args -> {
                int port = parsePort(args.get("port"));
                if (port <= 0) return err("无效的端口号");
                WifiTools instance = getInstance(port);
                if (instance == null || !isInstanceRunning(port)) return err("WiFi工具未启动，请先使用 dlqq_run_wifi");
                return ok(instance.getProfiles());
            }
        );
    }

    private static void registerWifiConnectTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "string");
        portProp.put("description", "WiFi工具实例端口号");
        properties.put("port", portProp);
        Map<String, Object> ssidProp = new HashMap<>();
        ssidProp.put("type", "string");
        ssidProp.put("description", "要连接的WiFi名称（SSID）");
        properties.put("ssid", ssidProp);
        Map<String, Object> pwdProp = new HashMap<>();
        pwdProp.put("type", "string");
        pwdProp.put("description", "WiFi密码（可选，开放网络不需要）");
        properties.put("password", pwdProp);
        Map<String, Object> timeoutProp = new HashMap<>();
        timeoutProp.put("type", "integer");
        timeoutProp.put("description", "连接超时时间（秒，默认15秒）");
        properties.put("timeout", timeoutProp);
        ToolInputSchema schema = new ToolInputSchema(properties, Arrays.asList("port", "ssid"));

        server.addTool("dlqq_wifi_connect",
            "连接指定的WiFi网络。需要密码时请提供password参数（支持WPA/WPA2-PSK和开放网络）。",
            schema,
            args -> {
                int port = parsePort(args.get("port"));
                if (port <= 0) return err("无效的端口号");
                WifiTools instance = getInstance(port);
                if (instance == null || !isInstanceRunning(port)) return err("WiFi工具未启动，请先使用 dlqq_run_wifi");
                String ssid = (String) args.get("ssid");
                String password = (String) args.get("password");
                int timeout = 15;
                if (args.get("timeout") != null) {
                    try { timeout = Integer.parseInt(String.valueOf(args.get("timeout"))); } catch (NumberFormatException ignored) {}
                }
                String result = instance.connect(ssid, password, timeout);
                return result.startsWith("错误") ? err(result) : ok(result);
            }
        );
    }

    private static void registerWifiDisconnectTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "string");
        portProp.put("description", "WiFi工具实例端口号");
        properties.put("port", portProp);
        ToolInputSchema schema = new ToolInputSchema(properties, Collections.singletonList("port"));

        server.addTool("dlqq_wifi_disconnect",
            "断开当前WiFi连接。",
            schema,
            args -> {
                int port = parsePort(args.get("port"));
                if (port <= 0) return err("无效的端口号");
                WifiTools instance = getInstance(port);
                if (instance == null || !isInstanceRunning(port)) return err("WiFi工具未启动，请先使用 dlqq_run_wifi");
                return ok(instance.disconnect());
            }
        );
    }

    private static void registerWifiGetLogTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "string");
        portProp.put("description", "WiFi工具实例端口号");
        properties.put("port", portProp);
        Map<String, Object> startProp = new HashMap<>();
        startProp.put("type", "integer");
        startProp.put("description", "起始行（1-based，默认1）");
        properties.put("start_line", startProp);
        Map<String, Object> endProp = new HashMap<>();
        endProp.put("type", "integer");
        endProp.put("description", "结束行（-1表示到末尾）");
        properties.put("end_line", endProp);
        ToolInputSchema schema = new ToolInputSchema(properties, Collections.singletonList("port"));

        server.addTool("dlqq_wifi_get_log",
            "获取WiFi工具日志数据。",
            schema,
            args -> {
                int port = parsePort(args.get("port"));
                if (port <= 0) return err("无效的端口号");
                WifiTools instance = getInstance(port);
                if (instance == null || !isInstanceRunning(port)) return err("WiFi工具未启动，请先使用 dlqq_run_wifi");
                List<String> logs = instance.getLogBuffer();
                int startLine = 1;
                int endLine = logs.size();
                if (args.get("start_line") != null) {
                    try { startLine = Integer.parseInt(String.valueOf(args.get("start_line"))); } catch (NumberFormatException ignored) {}
                }
                if (args.get("end_line") != null) {
                    try {
                        int e = Integer.parseInt(String.valueOf(args.get("end_line")));
                        endLine = e == -1 ? logs.size() : Math.min(e, logs.size());
                    } catch (NumberFormatException ignored) {}
                }
                startLine = Math.max(1, Math.min(startLine, logs.size()));
                if (logs.isEmpty()) return ok("(暂无日志)");
                StringBuilder sb = new StringBuilder();
                for (int i = startLine; i <= endLine; i++) {
                    sb.append(i).append(" | ").append(logs.get(i - 1)).append("\n");
                }
                return ok(sb.toString());
            }
        );
    }

    private static void registerWifiClearLogTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "string");
        portProp.put("description", "WiFi工具实例端口号");
        properties.put("port", portProp);
        ToolInputSchema schema = new ToolInputSchema(properties, Collections.singletonList("port"));

        server.addTool("dlqq_wifi_clear_log",
            "清除WiFi工具日志缓冲区。",
            schema,
            args -> {
                int port = parsePort(args.get("port"));
                if (port <= 0) return err("无效的端口号");
                WifiTools instance = getInstance(port);
                if (instance == null || !isInstanceRunning(port)) return err("WiFi工具未启动，请先使用 dlqq_run_wifi");
                instance.clearLog();
                return ok("日志已清空");
            }
        );
    }

    private static void registerWifiGetLineCountTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "string");
        portProp.put("description", "WiFi工具实例端口号");
        properties.put("port", portProp);
        ToolInputSchema schema = new ToolInputSchema(properties, Collections.singletonList("port"));

        server.addTool("dlqq_wifi_get_line_count",
            "获取WiFi工具日志缓冲区的当前行数。",
            schema,
            args -> {
                int port = parsePort(args.get("port"));
                if (port <= 0) return err("无效的端口号");
                WifiTools instance = getInstance(port);
                if (instance == null || !isInstanceRunning(port)) return err("WiFi工具未启动，请先使用 dlqq_run_wifi");
                return ok("当前日志行数: " + instance.getLogBuffer().size());
            }
        );
    }

    private static void registerExitWifiTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "string");
        portProp.put("description", "WiFi工具实例端口号");
        properties.put("port", portProp);
        ToolInputSchema schema = new ToolInputSchema(properties, Collections.singletonList("port"));

        server.addTool("dlqq_exit_wifi",
            "关闭WiFi工具（关闭窗口并释放资源）。",
            schema,
            args -> {
                int port = parsePort(args.get("port"));
                if (port <= 0) return err("无效的端口号");
                if (!hasInstance(port)) return err("WiFi工具实例不存在");
                String result = removeInstanceAndGet(port);
                return ok(result);
            }
        );
    }

    private static void registerRerunWifiTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "string");
        portProp.put("description", "WiFi工具实例端口号");
        properties.put("port", portProp);
        ToolInputSchema schema = new ToolInputSchema(properties, Collections.singletonList("port"));

        server.addTool("dlqq_rerun_wifi",
            "重新启动WiFi工具（先关闭再启动，用于刷新窗口）。",
            schema,
            args -> {
                int port = parsePort(args.get("port"));
                if (port <= 0) return err("无效的端口号");
                if (!hasInstance(port)) return err("WiFi工具实例不存在");
                WifiTools instance = getInstance(port);
                return ok(instance.restart());
            }
        );
    }

    private static String removeInstanceAndGet(int port) {
        WifiTools tools = instances.remove(port);
        if (tools != null) {
            return tools.stop();
        }
        return "WiFi工具实例不存在";
    }
}

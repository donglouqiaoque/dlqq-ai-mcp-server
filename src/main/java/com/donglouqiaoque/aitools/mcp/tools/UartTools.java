package com.donglouqiaoque.aitools.mcp.tools;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * UART串口工具 - 支持CH340等USB转串口设备
 * 提供图形化串口通信界面
 */
public class UartTools {

    // 存储所有打开的串口连接
    private static final Map<String, SerialPortConnection> activeConnections = new HashMap<>();
    // 全局日志记录
    private static final List<String> globalLogs = new CopyOnWriteArrayList<>();
    private static final int MAX_GLOBAL_LOG_SIZE = 5000;

    // GUI窗口实例（单例）
    private static JFrame guiWindow = null;
    private static JTextArea txArea;
    private static JTextArea rxArea;
    private static JTextArea logArea;
    private static JComboBox<String> portCombo;
    private static JComboBox<Integer> baudCombo;
    private static JButton openButton;
    private static JButton closeButton;
    private static JButton sendButton;
    private static JCheckBox newlineCheckBox;
    private static SerialPort currentPort = null;
    private static boolean isPortOpen = false;

    /**
     * 串口连接包装类
     */
    public static class SerialPortConnection {
        private final SerialPort serialPort;
        private final String portName;
        private final java.util.Queue<String> receiveBuffer;
        private final List<String> logBuffer;
        private final int maxLogSize;

        public SerialPortConnection(SerialPort port, int maxLogSize) {
            this.serialPort = port;
            this.portName = port.getSystemPortName();
            this.receiveBuffer = new ConcurrentLinkedQueue<>();
            this.logBuffer = Collections.synchronizedList(new ArrayList<>());
            this.maxLogSize = maxLogSize;
        }

        public SerialPort getSerialPort() { return serialPort; }
        public String getPortName() { return portName; }
        public java.util.Queue<String> getReceiveBuffer() { return receiveBuffer; }
        public List<String> getLogBuffer() { return logBuffer; }

        public void addToLog(String direction, String data) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
            String logEntry = String.format("[%s] %s: %s", timestamp, direction, data.trim());
            logBuffer.add(logEntry);
            globalLogs.add("[" + portName + "] " + logEntry);

            // 限制日志大小
            while (logBuffer.size() > maxLogSize) {
                logBuffer.remove(0);
            }
            while (globalLogs.size() > MAX_GLOBAL_LOG_SIZE) {
                globalLogs.remove(0);
            }
        }

        public String getLogs(int count) {
            if (count <= 0 || count > logBuffer.size()) {
                count = logBuffer.size();
            }
            int start = Math.max(0, logBuffer.size() - count);
            StringBuilder sb = new StringBuilder();
            for (int i = start; i < logBuffer.size(); i++) {
                sb.append(logBuffer.get(i)).append("\n");
            }
            return sb.toString();
        }
    }

    /**
     * 打开图形化串口工具窗口
     */
    public static void openGuiWindow() {
        if (guiWindow != null && guiWindow.isVisible()) {
            guiWindow.toFront();
            guiWindow.requestFocus();
            return;
        }

        SwingUtilities.invokeLater(() -> {
            createGuiWindow();
        });
    }

    /**
     * 创建GUI窗口
     */
    private static void createGuiWindow() {
        // 设置中文字体
        setupChineseFont();
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        guiWindow = new JFrame("CH340 串口工具");
        guiWindow.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        guiWindow.setSize(900, 700);
        guiWindow.setLocationRelativeTo(null);

        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ========== 顶部控制面板 ==========
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        controlPanel.setBorder(BorderFactory.createTitledBorder("串口设置"));

        // 串口选择
        controlPanel.add(new JLabel("串口:"));
        portCombo = new JComboBox<>();
        refreshPortList();
        portCombo.setPreferredSize(new Dimension(120, 25));
        controlPanel.add(portCombo);

        // 刷新按钮
        JButton refreshButton = new JButton("刷新");
        refreshButton.addActionListener(e -> refreshPortList());
        controlPanel.add(refreshButton);

        // 波特率选择
        controlPanel.add(new JLabel("波特率:"));
        baudCombo = new JComboBox<>(new Integer[]{9600, 19200, 38400, 57600, 115200, 230400, 460800, 921600});
        baudCombo.setSelectedItem(115200);
        baudCombo.setPreferredSize(new Dimension(100, 25));
        controlPanel.add(baudCombo);

        // 打开/关闭按钮
        openButton = new JButton("打开串口");
        closeButton = new JButton("关闭串口");
        closeButton.setEnabled(false);

        openButton.addActionListener(e -> openPort());
        closeButton.addActionListener(e -> closePort());

        controlPanel.add(openButton);
        controlPanel.add(closeButton);

        // CH340查找按钮
        JButton ch340Button = new JButton("查找CH340");
        ch340Button.addActionListener(e -> findCH340());
        controlPanel.add(ch340Button);

        mainPanel.add(controlPanel, BorderLayout.NORTH);

        // ========== 中部数据面板 ==========
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.5);

        // 发送区域
        JPanel txPanel = new JPanel(new BorderLayout(5, 5));
        txPanel.setBorder(BorderFactory.createTitledBorder("发送数据 (TX)"));
        txArea = new JTextArea(8, 60);
        txArea.setLineWrap(true);
        txArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        JScrollPane txScroll = new JScrollPane(txArea);
        txPanel.add(txScroll, BorderLayout.CENTER);

        JPanel txButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        sendButton = new JButton("发送");
        sendButton.setEnabled(false);
        sendButton.addActionListener(e -> sendData());
        txButtonPanel.add(sendButton);

        newlineCheckBox = new JCheckBox("添加换行符", true);
        txButtonPanel.add(newlineCheckBox);

        txPanel.add(txButtonPanel, BorderLayout.SOUTH);

        // 接收区域
        JPanel rxPanel = new JPanel(new BorderLayout(5, 5));
        rxPanel.setBorder(BorderFactory.createTitledBorder("接收数据 (RX)"));
        rxArea = new JTextArea(8, 60);
        rxArea.setEditable(false);
        rxArea.setLineWrap(true);
        rxArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        JScrollPane rxScroll = new JScrollPane(rxArea);
        rxPanel.add(rxScroll, BorderLayout.CENTER);

        JPanel rxButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton clearRxButton = new JButton("清空接收区");
        clearRxButton.addActionListener(e -> rxArea.setText(""));
        rxButtonPanel.add(clearRxButton);
        rxPanel.add(rxButtonPanel, BorderLayout.SOUTH);

        splitPane.setTopComponent(txPanel);
        splitPane.setBottomComponent(rxPanel);

        mainPanel.add(splitPane, BorderLayout.CENTER);

        // ========== 底部日志面板 ==========
        JPanel logPanel = new JPanel(new BorderLayout(5, 5));
        logPanel.setBorder(BorderFactory.createTitledBorder("通信日志"));
        logPanel.setPreferredSize(new Dimension(0, 200));

        logArea = new JTextArea(8, 60);
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        JScrollPane logScroll = new JScrollPane(logArea);
        logPanel.add(logScroll, BorderLayout.CENTER);

        JPanel logButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton clearLogButton = new JButton("清空日志");
        clearLogButton.addActionListener(e -> {
            logArea.setText("");
            globalLogs.clear();
        });
        logButtonPanel.add(clearLogButton);
        logPanel.add(logButtonPanel, BorderLayout.SOUTH);

        mainPanel.add(logPanel, BorderLayout.SOUTH);

        guiWindow.add(mainPanel);

        // 关闭窗口处理
        guiWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closePort();
                guiWindow.dispose();
                guiWindow = null;
            }
        });

        // 设置默认按钮
        guiWindow.getRootPane().setDefaultButton(sendButton);

        guiWindow.setVisible(true);

        // 设置字体
        Font chineseFont = getChineseFont();
        setFontRecursively(guiWindow, chineseFont);
    }

    /**
     * 刷新串口列表
     */
    private static void refreshPortList() {
        portCombo.removeAllItems();
        SerialPort[] ports = SerialPort.getCommPorts();
        for (SerialPort port : ports) {
            String display = port.getSystemPortName() + " - " + port.getDescriptivePortName();
            portCombo.addItem(display);
        }
        if (portCombo.getItemCount() == 0) {
            portCombo.addItem("未找到串口");
        }
    }

    /**
     * 查找CH340设备
     */
    private static void findCH340() {
        StringBuilder sb = new StringBuilder();
        sb.append("CH340设备查找结果:\n");

        SerialPort[] ports = SerialPort.getCommPorts();
        boolean found = false;

        for (SerialPort port : ports) {
            // CH340的VID是0x1A86，PID是0x7523
            if (port.getVendorID() == 0x1A86 && port.getProductID() == 0x7523) {
                found = true;
                sb.append("✓ ").append(port.getSystemPortName())
                  .append(" - ").append(port.getDescriptivePortName()).append("\n");
            }
        }

        if (!found) {
            sb.append("未找到CH340设备\n");
        }

        JOptionPane.showMessageDialog(guiWindow, sb.toString(), "CH340查找结果", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 打开串口
     */
    private static void openPort() {
        if (portCombo.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(guiWindow, "请先选择串口", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String selected = (String) portCombo.getSelectedItem();
        String portName = selected.split(" - ")[0];
        int baudRate = (Integer) baudCombo.getSelectedItem();

        SerialPort port = findPortByName(portName);
        if (port == null) {
            JOptionPane.showMessageDialog(guiWindow, "找不到指定的串口", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 配置串口
        port.setBaudRate(baudRate);
        port.setNumDataBits(8);
        port.setNumStopBits(SerialPort.ONE_STOP_BIT);
        port.setParity(SerialPort.NO_PARITY);
        port.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        port.setComPortTimeouts(
            SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING,
            1000, 1000
        );

        if (!port.openPort()) {
            JOptionPane.showMessageDialog(guiWindow, "无法打开串口，可能已被占用", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        currentPort = port;
        isPortOpen = true;

        // 添加数据监听器
        port.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
            }

            @Override
            public void serialEvent(SerialPortEvent event) {
                if (event.getEventType() == SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
                    try {
                        byte[] newData = new byte[port.bytesAvailable()];
                        int numRead = port.readBytes(newData, newData.length);
                        if (numRead > 0) {
                            String received = new String(newData, 0, numRead, StandardCharsets.UTF_8);
                            SwingUtilities.invokeLater(() -> {
                                rxArea.append(received);
                                addToLog("RX", received);
                            });
                        }
                    } catch (Exception e) {
                        System.err.println("串口读取错误: " + e.getMessage());
                    }
                }
            }
        });

        // 更新UI
        openButton.setEnabled(false);
        closeButton.setEnabled(true);
        sendButton.setEnabled(true);
        portCombo.setEnabled(false);
        baudCombo.setEnabled(false);

        addToLog("INFO", "串口 " + portName + " 已打开，波特率 " + baudRate);
    }

    /**
     * 关闭串口
     */
    private static void closePort() {
        if (currentPort != null && currentPort.isOpen()) {
            currentPort.removeDataListener();
            currentPort.closePort();
            addToLog("INFO", "串口 " + currentPort.getSystemPortName() + " 已关闭");
        }

        currentPort = null;
        isPortOpen = false;

        // 更新UI
        if (openButton != null) {
            openButton.setEnabled(true);
            closeButton.setEnabled(false);
            sendButton.setEnabled(false);
            portCombo.setEnabled(true);
            baudCombo.setEnabled(true);
        }
    }

    /**
     * 发送数据
     */
    private static void sendData() {
        if (currentPort == null || !currentPort.isOpen()) {
            return;
        }

        String data = txArea.getText();
        if (data.isEmpty()) {
            return;
        }

        try {
            // 检查是否需要添加换行
            boolean addNewline = newlineCheckBox != null && newlineCheckBox.isSelected();

            String sendData = addNewline ? data + "\r\n" : data;
            byte[] bytes = sendData.getBytes(StandardCharsets.UTF_8);
            currentPort.writeBytes(bytes, bytes.length);

            addToLog("TX", data);
            txArea.setText("");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(guiWindow, "发送失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 添加日志
     */
    private static void addToLog(String direction, String data) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
        String logEntry = String.format("[%s] %s: %s", timestamp, direction, data.trim());

        globalLogs.add(logEntry);
        while (globalLogs.size() > MAX_GLOBAL_LOG_SIZE) {
            globalLogs.remove(0);
        }

        if (logArea != null) {
            logArea.append(logEntry + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }
    }

    /**
     * 获取最近日志
     */
    public static String getRecentLogs(int lines) {
        if (lines <= 0 || lines > globalLogs.size()) {
            lines = globalLogs.size();
        }
        int start = Math.max(0, globalLogs.size() - lines);
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < globalLogs.size(); i++) {
            sb.append(globalLogs.get(i)).append("\n");
        }
        return sb.toString();
    }

    /**
     * 清除日志
     */
    public static void clearLogs() {
        globalLogs.clear();
        if (logArea != null) {
            logArea.setText("");
        }
    }

    /**
     * 根据名称查找串口
     */
    private static SerialPort findPortByName(String portName) {
        SerialPort[] ports = SerialPort.getCommPorts();
        for (SerialPort port : ports) {
            if (port.getSystemPortName().equalsIgnoreCase(portName) ||
                port.getDescriptivePortName().contains(portName)) {
                return port;
            }
        }
        return null;
    }

    // ==================== 字体设置 ====================

    private static boolean isFontAvailable(String fontName) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (String name : ge.getAvailableFontFamilyNames()) {
            if (name.equalsIgnoreCase(fontName)) {
                return true;
            }
        }
        return false;
    }

    private static Font getChineseFont() {
        if (isFontAvailable("Microsoft YaHei")) {
            return new Font("Microsoft YaHei", Font.PLAIN, 12);
        }
        if (isFontAvailable("SimSun")) {
            return new Font("SimSun", Font.PLAIN, 12);
        }
        return new Font("Dialog", Font.PLAIN, 12);
    }

    private static void setupChineseFont() {
        Font uiFont = getChineseFont();
        UIManager.put("CheckBox.font", uiFont);
        UIManager.put("ComboBox.font", uiFont);
        UIManager.put("Label.font", uiFont);
        UIManager.put("Panel.font", uiFont);
        UIManager.put("Button.font", uiFont);
        UIManager.put("TextArea.font", new Font("Monospaced", Font.PLAIN, 12));
        UIManager.put("TextField.font", uiFont);
        UIManager.put("TitledBorder.font", uiFont);
    }

    private static void setFontRecursively(Component comp, Font font) {
        if (comp == null) return;
        comp.setFont(font);
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                setFontRecursively(child, font);
            }
        }
    }
}

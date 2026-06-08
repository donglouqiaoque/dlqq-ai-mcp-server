package com.donglouqiaoque.aitools.mcp.tools;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

/**
 * UART串口工具 - 支持CH340等USB转串口设备
 * 提供网络接口供AI连接控制
 */
public class UartTools {

    // 单例实例
    private static UartTools instance;

    // 串口服务器
    private ServerSocket serverSocket;
    private Thread serverThread;
    private boolean isRunning = false;
    private int serverPort = 0;

    // 串口连接状态
    private SerialPort currentPort = null;
    private boolean isPortOpen = false;
    private int currentBaudRate = 1000000;

    // 数据缓冲区
    private final List<String> receiveBuffer;
    private int maxBufferSize = 5 * 1024 * 1024; // 默认5MB
    private int currentBufferSize = 0;

    // GUI窗口
    private JFrame guiWindow = null;
    private JTextArea rxArea;
    private JTextArea logArea;
    private JComboBox<String> portCombo;
    private JComboBox<Integer> baudCombo;
    private JButton openCloseButton;
    private StatusIcon statusIcon;

    // 线程池
    private final ExecutorService executor;

    private UartTools() {
        this.receiveBuffer = Collections.synchronizedList(new ArrayList<>());
        this.executor = Executors.newCachedThreadPool();
    }

    /**
     * 获取单例实例
     */
    public static synchronized UartTools getInstance() {
        if (instance == null) {
            instance = new UartTools();
        }
        return instance;
    }

    // ==================== 工具方法（供MCP调用） ====================

    /**
     * 1. 运行串口服务器 - 打开CH340串口工具
     *
     * @param port 服务器端口号
     * @return 服务器启动结果
     */
    public static String runUart(int port) {
        UartTools tools = getInstance();
        if (tools.isRunning) {
            return "串口工具已在运行中，端口: " + tools.serverPort;
        }

        try {
            tools.startServer(port);
            // 同时打开GUI窗口
            SwingUtilities.invokeLater(() -> tools.createGuiWindow());
            return "串口工具已启动，监听端口: " + port + "，请使用 localhost:" + port + " 连接";
        } catch (Exception e) {
            return "启动串口工具失败: " + e.getMessage();
        }
    }

    /**
     * 2. 获取所有串口通道
     *
     * @return 串口列表信息
     */
    public static String getUartComList() {
        UartTools tools = getInstance();
        StringBuilder sb = new StringBuilder();
        sb.append("可用串口通道:\n");

        SerialPort[] ports = SerialPort.getCommPorts();
        if (ports.length == 0) {
            sb.append("  未找到串口设备\n");
        } else {
            for (int i = 0; i < ports.length; i++) {
                SerialPort port = ports[i];
                sb.append(String.format("  %d. %s - %s", i + 1, port.getSystemPortName(), port.getDescriptivePortName()));

                // 标记CH340设备
                if (port.getVendorID() == 0x1A86 && port.getProductID() == 0x7523) {
                    sb.append(" [CH340]");
                }
                sb.append("\n");
            }
        }

        // 显示当前状态
        if (tools.isPortOpen && tools.currentPort != null) {
            sb.append("\n当前已打开串口: ").append(tools.currentPort.getSystemPortName());
            sb.append("，波特率: ").append(tools.currentBaudRate);
        } else {
            sb.append("\n当前未打开任何串口");
        }

        return sb.toString();
    }

    /**
     * 3. 打开串口通道
     *
     * @param portName 串口名称（如 COM3、ttyUSB0）
     * @param baudRate 波特率（0~2000000，默认1000000）
     * @return 打开结果
     */
    public static String openUartCom(String portName, int baudRate) {
        UartTools tools = getInstance();

        // 如果已有打开的串口，先关闭
        if (tools.isPortOpen && tools.currentPort != null) {
            closeUartCom();
        }

        // 查找串口
        SerialPort port = findPortByName(portName);
        if (port == null) {
            return "未找到串口: " + portName;
        }

        // 验证波特率范围
        if (baudRate < 0 || baudRate > 2000000) {
            return "波特率超出范围，必须是 0~2000000";
        }
        if (baudRate == 0) {
            baudRate = 1000000; // 默认波特率
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
            return "无法打开串口 " + portName + "，可能已被占用";
        }

        tools.currentPort = port;
        tools.isPortOpen = true;
        tools.currentBaudRate = baudRate;

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
                            tools.addToBuffer(received);

                            // 更新GUI
                            SwingUtilities.invokeLater(() -> {
                                if (tools.rxArea != null) {
                                    tools.rxArea.append(received);
                                    tools.rxArea.setCaretPosition(tools.rxArea.getDocument().getLength());
                                }
                                if (tools.logArea != null) {
                                    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
                                    tools.logArea.append("[" + timestamp + "] RX: " + received.trim() + "\n");
                                }
                            });
                        }
                    } catch (Exception e) {
                        System.err.println("串口读取错误: " + e.getMessage());
                    }
                }
            }
        });

        // 更新GUI
        SwingUtilities.invokeLater(() -> {
            if (tools.openCloseButton != null) {
                tools.openCloseButton.setText("关闭串口");
            }
            if (tools.statusIcon != null) {
                tools.statusIcon.setOpen(true);
            }
        });

        return "串口 " + portName + " 已打开，波特率: " + baudRate;
    }

    /**
     * 4. 关闭串口通道
     *
     * @return 关闭结果
     */
    public static String closeUartCom() {
        UartTools tools = getInstance();

        if (tools.currentPort != null && tools.currentPort.isOpen()) {
            String portName = tools.currentPort.getSystemPortName();
            tools.currentPort.removeDataListener();
            tools.currentPort.closePort();
            tools.isPortOpen = false;
            tools.currentPort = null;

            // 更新GUI
            SwingUtilities.invokeLater(() -> {
                if (tools.openCloseButton != null) {
                    tools.openCloseButton.setText("打开串口");
                }
                if (tools.statusIcon != null) {
                    tools.statusIcon.setOpen(false);
                }
            });

            return "串口 " + portName + " 已关闭";
        }

        return "当前没有打开的串口";
    }

    /**
     * 5. 获取串口接收的文本数据
     *
     * @param startLine 起始行（1-based）
     * @param endLine   结束行（1-based，-1表示到末尾）
     * @param readLines 读取行数（与endLine互斥）
     * @return 文本数据
     */
    public static String getUartText(int startLine, int endLine, int readLines) {
        UartTools tools = getInstance();

        synchronized (tools.receiveBuffer) {
            if (tools.receiveBuffer.isEmpty()) {
                return "";
            }

            int totalLines = tools.receiveBuffer.size();

            // 验证起始行
            if (startLine < 1) {
                startLine = 1;
            }
            if (startLine > totalLines) {
                return "";
            }

            // 计算结束行
            if (endLine < 0 && readLines > 0) {
                endLine = startLine + readLines - 1;
            }
            if (endLine < 0 || endLine > totalLines) {
                endLine = totalLines;
            }
            if (endLine < startLine) {
                endLine = startLine;
            }

            StringBuilder sb = new StringBuilder();
            for (int i = startLine - 1; i < endLine && i < totalLines; i++) {
                sb.append(tools.receiveBuffer.get(i));
                if (i < endLine - 1) {
                    sb.append("\n");
                }
            }

            return sb.toString();
        }
    }

    /**
     * 6. 清除串口接收的数据
     *
     * @return 清除结果
     */
    public static String clearUartText() {
        UartTools tools = getInstance();

        synchronized (tools.receiveBuffer) {
            int clearedLines = tools.receiveBuffer.size();
            tools.receiveBuffer.clear();
            tools.currentBufferSize = 0;
            return "已清除 " + clearedLines + " 行数据";
        }
    }

    /**
     * 7. 设置串口数据缓冲区大小
     *
     * @param sizeBytes 缓冲区大小（字节）
     * @return 设置结果
     */
    public static String setUartBufferSize(int sizeBytes) {
        UartTools tools = getInstance();

        if (sizeBytes <= 0) {
            return "缓冲区大小必须大于0";
        }

        tools.maxBufferSize = sizeBytes;

        // 如果当前数据超出新大小，删除旧数据
        synchronized (tools.receiveBuffer) {
            while (tools.currentBufferSize > tools.maxBufferSize && !tools.receiveBuffer.isEmpty()) {
                String removed = tools.receiveBuffer.remove(0);
                tools.currentBufferSize -= removed.getBytes(StandardCharsets.UTF_8).length;
            }
        }

        return "缓冲区大小已设置为 " + sizeBytes + " 字节 (" + (sizeBytes / 1024 / 1024) + " MB)";
    }

    /**
     * 8. 退出串口工具 - 关闭窗口及进程
     *
     * @return 退出结果
     */
    public static String exitUart() {
        UartTools tools = getInstance();

        // 关闭GUI窗口
        if (tools.guiWindow != null) {
            SwingUtilities.invokeLater(() -> {
                tools.guiWindow.dispose();
                tools.guiWindow = null;
            });
        }

        // 关闭串口
        if (tools.currentPort != null && tools.currentPort.isOpen()) {
            tools.currentPort.removeDataListener();
            tools.currentPort.closePort();
            tools.isPortOpen = false;
            tools.currentPort = null;
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
        synchronized (tools.receiveBuffer) {
            tools.receiveBuffer.clear();
            tools.currentBufferSize = 0;
        }

        // 重置实例
        instance = null;

        return "串口工具已完全关闭";
    }

    // ==================== 内部实现 ====================

    /**
     * 启动网络服务器
     */
    private void startServer(int port) throws IOException {
        this.serverPort = port;
        this.serverSocket = new ServerSocket(port);
        this.isRunning = true;

        this.serverThread = new Thread(() -> {
            while (isRunning && !serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    executor.submit(() -> handleClient(clientSocket));
                } catch (IOException e) {
                    if (isRunning) {
                        System.err.println("服务器 accept 错误: " + e.getMessage());
                    }
                }
            }
        });
        this.serverThread.setDaemon(true);
        this.serverThread.start();
    }

    /**
     * 处理客户端连接
     */
    private void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                String response = processCommand(line);
                out.println(response);
            }
        } catch (IOException e) {
            System.err.println("客户端处理错误: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    /**
     * 处理命令
     */
    private String processCommand(String command) {
        String[] parts = command.trim().split("\\s+", 2);
        String cmd = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";

        switch (cmd) {
            case "list":
                return getUartComList();
            case "open":
                String[] openArgs = args.split("\\s+", 2);
                String portName = openArgs[0];
                int baudRate = openArgs.length > 1 ? Integer.parseInt(openArgs[1]) : 1000000;
                return openUartCom(portName, baudRate);
            case "close":
                return closeUartCom();
            case "get":
                String[] getArgs = args.split("\\s+", 3);
                int start = getArgs.length > 0 ? Integer.parseInt(getArgs[0]) : 1;
                int end = getArgs.length > 1 ? Integer.parseInt(getArgs[1]) : -1;
                int lines = getArgs.length > 2 ? Integer.parseInt(getArgs[2]) : 0;
                return getUartText(start, end, lines);
            case "clear":
                return clearUartText();
            case "size":
                int newSize = args.isEmpty() ? 5 * 1024 * 1024 : Integer.parseInt(args);
                return setUartBufferSize(newSize);
            default:
                return "未知命令: " + cmd;
        }
    }

    /**
     * 添加数据到缓冲区
     */
    private void addToBuffer(String data) {
        synchronized (receiveBuffer) {
            receiveBuffer.add(data);
            currentBufferSize += data.getBytes(StandardCharsets.UTF_8).length;

            // 超出缓冲区大小时，删除旧数据
            while (currentBufferSize > maxBufferSize && !receiveBuffer.isEmpty()) {
                String removed = receiveBuffer.remove(0);
                currentBufferSize -= removed.getBytes(StandardCharsets.UTF_8).length;
            }
        }
    }

    /**
     * 根据名称查找串口
     */
    private static SerialPort findPortByName(String portName) {
        SerialPort[] ports = SerialPort.getCommPorts();
        for (SerialPort port : ports) {
            if (port.getSystemPortName().equalsIgnoreCase(portName) ||
                port.getDescriptivePortName().toLowerCase().contains(portName.toLowerCase())) {
                return port;
            }
        }
        return null;
    }

    // ==================== GUI窗口 ====================

    /**
     * 创建GUI窗口
     */
    private void createGuiWindow() {
        if (guiWindow != null && guiWindow.isVisible()) {
            guiWindow.toFront();
            return;
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        guiWindow = new JFrame("CH340 串口工具 - 端口: " + serverPort);
        guiWindow.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        guiWindow.setSize(800, 600);
        guiWindow.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 顶部控制面板
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        controlPanel.setBorder(BorderFactory.createTitledBorder("串口设置"));

        controlPanel.add(new JLabel("串口:"));
        portCombo = new JComboBox<>();
        refreshPortList();
        portCombo.setPreferredSize(new Dimension(150, 25));
        controlPanel.add(portCombo);

        JButton refreshButton = new JButton("刷新");
        refreshButton.addActionListener(e -> refreshPortList());
        controlPanel.add(refreshButton);

        controlPanel.add(new JLabel("波特率:"));
        baudCombo = new JComboBox<>(new Integer[]{9600, 19200, 38400, 57600, 115200, 230400, 460800, 921600, 1000000});
        baudCombo.setSelectedItem(1000000);
        baudCombo.setPreferredSize(new Dimension(100, 25));
        controlPanel.add(baudCombo);

        // 状态指示圆圈（作为按钮图标）
        statusIcon = new StatusIcon(false);

        // 打开/关闭串口按钮（合并为一个）
        openCloseButton = new JButton("打开串口", statusIcon);
        openCloseButton.setHorizontalTextPosition(SwingConstants.RIGHT);
        openCloseButton.setHorizontalAlignment(SwingConstants.CENTER);

        openCloseButton.addActionListener(e -> {
            if (isPortOpen) {
                // 当前已打开，执行关闭
                String result = closeUartCom();
                JOptionPane.showMessageDialog(guiWindow, result);
            } else {
                // 当前未打开，执行打开
                String selected = (String) portCombo.getSelectedItem();
                if (selected != null && !selected.equals("未找到串口")) {
                    String portName = selected.split(" - ")[0].trim();
                    int baud = (Integer) baudCombo.getSelectedItem();
                    String result = openUartCom(portName, baud);
                    JOptionPane.showMessageDialog(guiWindow, result);
                }
            }
        });

        controlPanel.add(openCloseButton);

        mainPanel.add(controlPanel, BorderLayout.NORTH);

        // 接收区域
        JPanel rxPanel = new JPanel(new BorderLayout(5, 5));
        rxPanel.setBorder(BorderFactory.createTitledBorder("接收数据 (RX)"));
        rxArea = new JTextArea(15, 60);
        rxArea.setEditable(false);
        rxArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        JScrollPane rxScroll = new JScrollPane(rxArea);
        rxPanel.add(rxScroll, BorderLayout.CENTER);

        JPanel rxButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton clearRxButton = new JButton("清空接收区");
        clearRxButton.addActionListener(e -> {
            rxArea.setText("");
            clearUartText();
        });
        rxButtonPanel.add(clearRxButton);
        rxPanel.add(rxButtonPanel, BorderLayout.SOUTH);

        // 日志区域
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
        clearLogButton.addActionListener(e -> logArea.setText(""));
        logButtonPanel.add(clearLogButton);
        logPanel.add(logButtonPanel, BorderLayout.SOUTH);

        // 分割面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, rxPanel, logPanel);
        splitPane.setResizeWeight(0.7);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        guiWindow.add(mainPanel);

        // 关闭处理
        guiWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeUartCom();
                guiWindow.dispose();
                guiWindow = null;
            }
        });

        guiWindow.setVisible(true);

        // 设置中文字体
        Font chineseFont = getChineseFont();
        setFontRecursively(guiWindow, chineseFont);
    }

    /**
     * 刷新串口列表
     */
    private void refreshPortList() {
        if (portCombo == null) return;

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

    // ==================== 字体工具 ====================

    private static Font getChineseFont() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] availableFonts = ge.getAvailableFontFamilyNames();

        for (String name : availableFonts) {
            if (name.equalsIgnoreCase("Microsoft YaHei")) {
                return new Font("Microsoft YaHei", Font.PLAIN, 12);
            }
        }
        for (String name : availableFonts) {
            if (name.equalsIgnoreCase("SimSun")) {
                return new Font("SimSun", Font.PLAIN, 12);
            }
        }
        return new Font("Dialog", Font.PLAIN, 12);
    }

    private static void setFontRecursively(Component comp, Font font) {
        if (comp == null) return;
        comp.setFont(font);
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                if (!(child instanceof JTextArea)) {
                    setFontRecursively(child, font);
                }
            }
        }
    }

    /**
     * 状态指示圆圈图标组件
     * 红色表示关闭状态，绿色表示开启状态
     */
    private static class StatusIcon implements Icon {
        private boolean isOpen = false;
        private static final int SIZE = 16;
        private static final Color COLOR_CLOSED = new Color(220, 53, 69);  // 红色
        private static final Color COLOR_OPEN = new Color(40, 167, 69);   // 绿色

        public StatusIcon(boolean open) {
            this.isOpen = open;
        }

        public void setOpen(boolean open) {
            this.isOpen = open;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 绘制圆点
            int diameter = SIZE - 4;

            // 绘制阴影
            g2d.setColor(new Color(0, 0, 0, 50));
            g2d.fillOval(x + 1, y + 1, diameter, diameter);

            // 绘制实心圆
            g2d.setColor(isOpen ? COLOR_OPEN : COLOR_CLOSED);
            g2d.fillOval(x, y, diameter, diameter);

            // 绘制高光效果
            g2d.setColor(new Color(255, 255, 255, 100));
            int highlightSize = diameter / 2;
            g2d.fillOval(x + 2, y + 2, highlightSize, highlightSize);

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

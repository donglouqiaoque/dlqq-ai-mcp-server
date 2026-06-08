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

    // 多实例管理 - 每个端口对应一个实例
    private static final Map<Integer, UartTools> instances = new ConcurrentHashMap<>();

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
    private final List<String> dataBuffer;
    private int maxBufferSize = 5 * 1024 * 1024; // 默认5MB
    private int currentBufferSize = 0;
    private long totalSentBytes = 0;  // 统计发送字节数
    private long totalReceivedBytes = 0;  // 统计接收字节数
    private int lineCounter = 0;  // 行号计数器

    // GUI窗口
    private JFrame guiWindow = null;
    private JTextArea logArea;
    private JLabel statusLabel;  // 状态栏显示缓冲区占用率
    private JComboBox<String> portCombo;
    private JComboBox<Integer> baudCombo;
    private JButton openCloseButton;
    private StatusIcon statusIcon;

    // 线程池
    private final ExecutorService executor;

    private UartTools() {
        this.dataBuffer = Collections.synchronizedList(new ArrayList<>());
        this.executor = Executors.newCachedThreadPool();
    }

    /**
     * 获取指定端口的实例
     */
    public static synchronized UartTools getInstance(int port) {
        return instances.computeIfAbsent(port, k -> new UartTools());
    }

    /**
     * 检查指定端口的实例是否正在运行
     */
    public static boolean isInstanceRunning(int port) {
        UartTools tools = instances.get(port);
        return tools != null && tools.isRunning;
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
            return "错误：缺少 port 参数，请指定串口工具实例端口号。使用 dlqq_run_uart 启动串口工具";
        }
        return validatePort(port);
    }

    // ==================== 工具方法（供MCP调用） ====================

    /**
     * 1. 运行串口服务器 - 打开CH340串口工具
     *
     * @param port 服务器端口号
     * @return 服务器启动结果
     */
    public static String runUart(int port) {
        String portError = validatePort(port);
        if (portError != null) {
            return portError;
        }

        if (isInstanceRunning(port)) {
            return "串口工具已在运行中，端口: " + port;
        }

        // 检查是否已有其他端口的实例在运行，如果是则提示错误
        for (Map.Entry<Integer, UartTools> entry : instances.entrySet()) {
            if (entry.getValue().isRunning && entry.getKey() != port) {
                return "错误：端口 " + entry.getKey() + " 的串口工具实例正在运行，请先关闭该实例后再启动新实例";
            }
        }

        UartTools tools = getInstance(port);

        try {
            tools.startServer(port);
            // 同时打开GUI窗口
            SwingUtilities.invokeLater(() -> tools.createGuiWindow());
            return "串口工具已启动，监听端口: " + port + "，请使用 localhost:" + port + " 连接";
        } catch (Exception e) {
            // 启动失败，移除实例
            instances.remove(port);
            return "启动串口工具失败: " + e.getMessage();
        }
    }

    /**
     * 2. 获取所有串口通道
     *
     * @param port 服务器端口号
     * @return 串口列表信息
     */
    public static String getUartComList(int port) {
        String portError = checkPortParam(port);
        if (portError != null) {
            return portError;
        }

        if (!isInstanceRunning(port)) {
            return "错误：端口 " + port + " 的串口工具实例未启动，请先使用 dlqq_run_uart 启动";
        }

        UartTools tools = getInstance(port);
        StringBuilder sb = new StringBuilder();
        sb.append("可用串口通道:\n");

        SerialPort[] ports = SerialPort.getCommPorts();
        if (ports.length == 0) {
            sb.append("  未找到串口设备\n");
        } else {
            for (int i = 0; i < ports.length; i++) {
                SerialPort serialPort = ports[i];
                sb.append(String.format("  %d. %s - %s", i + 1, serialPort.getSystemPortName(), serialPort.getDescriptivePortName()));

                // 标记CH340设备
                if (serialPort.getVendorID() == 0x1A86 && serialPort.getProductID() == 0x7523) {
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
     * 2a. 刷新并获取所有串口通道
     *
     * @param port 服务器端口号
     * @return 刷新后的串口列表
     */
    public static String refreshComList(int port) {
        String portError = checkPortParam(port);
        if (portError != null) {
            return portError;
        }

        if (!isInstanceRunning(port)) {
            return "错误：端口 " + port + " 的串口工具实例未启动，请先使用 dlqq_run_uart 启动";
        }

        // 刷新串口列表，重新扫描系统串口
        SerialPort[] ports = SerialPort.getCommPorts();

        StringBuilder sb = new StringBuilder();
        sb.append("刷新后可用串口通道:\n");

        if (ports.length == 0) {
            sb.append("  未找到串口设备\n");
        } else {
            for (int i = 0; i < ports.length; i++) {
                SerialPort serialPort = ports[i];
                sb.append(String.format("  %d. %s - %s", i + 1, serialPort.getSystemPortName(), serialPort.getDescriptivePortName()));

                // 标记CH340设备
                if (serialPort.getVendorID() == 0x1A86 && serialPort.getProductID() == 0x7523) {
                    sb.append(" [CH340]");
                }
                sb.append("\n");
            }
        }

        sb.append("\n共找到 ").append(ports.length).append(" 个串口设备");
        return sb.toString();
    }

    /**
     * 3. 打开串口通道
     *
     * @param port 服务器端口号
     * @param com 串口名称（如 COM3、ttyUSB0）
     * @param baudRate 波特率（0~2000000，默认1000000）
     * @return 打开结果
     */
    public static String openUartCom(int port, String com, int baudRate) {
        String portError = checkPortParam(port);
        if (portError != null) {
            return portError;
        }

        if (!isInstanceRunning(port)) {
            return "错误：端口 " + port + " 的串口工具实例未启动，请先使用 dlqq_run_uart 启动";
        }

        // 验证串口名称参数
        if (com == null || com.trim().isEmpty()) {
            return "错误：缺少串口名称参数，请指定要打开的串口名称，如 COM3、ttyUSB0 或 USB Serial等关键词";
        }

        UartTools tools = getInstance(port);

        // 如果已有打开的串口，先关闭
        if (tools.isPortOpen && tools.currentPort != null) {
            closeUartCom(port);
        }

        // 查找串口
        SerialPort serialPort = findPortByName(com);
        if (serialPort == null) {
            return "未找到串口: " + com;
        }

        // 验证波特率范围
        if (baudRate < 0 || baudRate > 2000000) {
            return "波特率超出范围，必须是 0~2000000";
        }
        if (baudRate == 0) {
            baudRate = 1000000; // 默认波特率
        }

        // 配置串口
        serialPort.setBaudRate(baudRate);
        serialPort.setNumDataBits(8);
        serialPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
        serialPort.setParity(SerialPort.NO_PARITY);
        serialPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        serialPort.setComPortTimeouts(
            SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING,
            1000, 1000
        );

        if (!serialPort.openPort()) {
            return "无法打开串口 " + com + "，可能已被占用";
        }

        tools.currentPort = serialPort;
        tools.isPortOpen = true;
        tools.currentBaudRate = baudRate;

        // 添加数据监听器
        serialPort.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
            }

            @Override
            public void serialEvent(SerialPortEvent event) {
                if (event.getEventType() == SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
                    try {
                        byte[] newData = new byte[serialPort.bytesAvailable()];
                        int numRead = serialPort.readBytes(newData, newData.length);
                        if (numRead > 0) {
                            String received = new String(newData, 0, numRead, StandardCharsets.UTF_8);
                            tools.addToBuffer(received);

                            // 更新GUI - 直接显示缓冲区中的格式化内容
                            SwingUtilities.invokeLater(() -> {
                                if (tools.logArea != null) {
                                    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH.mm.ss.SSS"));
                                    String logLine = String.format("[%05d] [%s] RX: %s", tools.lineCounter, timestamp, received.trim());
                                    tools.logArea.append(logLine + "\n");
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

        return "串口 " + com + " 已打开，波特率: " + baudRate;
    }

    /**
     * 4. 关闭串口通道
     *
     * @param port 服务器端口号
     * @return 关闭结果
     */
    public static String closeUartCom(int port) {
        String portError = checkPortParam(port);
        if (portError != null) {
            return portError;
        }

        if (!isInstanceRunning(port)) {
            return "错误：端口 " + port + " 的串口工具实例未启动";
        }

        UartTools tools = getInstance(port);

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
     * 4a. 重新打开串口，先关闭再打开，可以更换串口或波特率
     *
     * @param port 服务器端口号
     * @param com 串口名称（如 COM3、ttyUSB0）
     * @param baudRate 新的波特率（0~2000000，默认1000000）
     * @return 重新打开结果
     */
    public static String reopenUartCom(int port, String com, int baudRate) {
        String portError = checkPortParam(port);
        if (portError != null) {
            return portError;
        }

        if (!isInstanceRunning(port)) {
            return "错误：端口 " + port + " 的串口工具实例未启动，请先使用 dlqq_run_uart 启动";
        }

        UartTools tools = getInstance(port);
        String currentCom = null;

        // 如果已有打开的串口，先关闭并记录当前串口名
        if (tools.isPortOpen && tools.currentPort != null) {
            currentCom = tools.currentPort.getSystemPortName();
            closeUartCom(port);
        }

        // 使用指定的串口名称，如果没有指定则使用当前串口名
        String targetCom = (com != null && !com.trim().isEmpty()) ? com.trim() : currentCom;
        if (targetCom == null) {
            return "错误：缺少串口名称参数，请指定要打开的串口名称，或先打开一个串口";
        }

        // 重新打开串口，使用新的波特率
        return openUartCom(port, targetCom, baudRate);
    }

    /**
     * 5. 获取串口接收的文本数据 - 支持3种读取模式
     *
     * 模式1（直接范围读取）: port + start_line + end_line
     *   直接读取指定行范围的数据
     *
     * 模式2（直接计数读取）: port + start_line + line_count
     *   从start_line开始读取指定行数
     *
     * 模式3（等待读取）: port + timeout_ms + line_count
     *   等待新数据到达，最多等待timeout_ms毫秒，读取至少line_count行
     *
     * @param port 服务器端口号（必需）
     * @param start_line 起始行号（1-based，模式1和2使用）
     * @param end_line 结束行号（模式1使用，-1表示到末尾）
     * @param line_count 读取行数（模式2和3使用）
     * @param timeout_ms 等待超时毫秒（模式3使用，0表示不等待）
     * @return 文本数据
     */
    public static String getUartText(Integer port, Integer start_line, Integer end_line, Integer line_count, int timeout_ms) {
        String portError = checkPortParam(port);
        if (portError != null) {
            return portError;
        }

        if (!isInstanceRunning(port)) {
            return "错误：端口 " + port + " 的串口工具实例未启动，请先使用 dlqq_run_uart 启动";
        }

        UartTools tools = getInstance(port);

        // 判断读取模式
        boolean isWaitMode = timeout_ms > 0;

        if (isWaitMode) {
            // ========== 模式3：等待读取模式 ==========
            // 记录等待前的行数
            int initialLines;
            synchronized (tools.dataBuffer) {
                initialLines = tools.dataBuffer.size();
            }

            // 默认从最后一行开始读取（如果没有指定start_line）
            int startFrom = (start_line != null && start_line > 0) ? start_line : initialLines + 1;
            int targetLines = (line_count != null && line_count > 0) ? line_count : 1;

            // 等待新数据到达
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < timeout_ms) {
                synchronized (tools.dataBuffer) {
                    if (tools.dataBuffer.size() >= startFrom + targetLines - 1) {
                        // 已有足够的新数据，跳出等待
                        break;
                    }
                }
                try {
                    Thread.sleep(10); // 短暂休眠避免CPU占用过高
                } catch (InterruptedException e) {
                    break;
                }
            }

            // 读取数据
            synchronized (tools.dataBuffer) {
                int totalLines = tools.dataBuffer.size();

                if (startFrom > totalLines || totalLines == 0) {
                    return "等待超时，当前缓冲区有 " + totalLines + " 行数据";
                }

                // 计算要读取的行数
                int endAt = Math.min(startFrom + targetLines - 1, totalLines);

                StringBuilder sb = new StringBuilder();
                for (int i = startFrom - 1; i < endAt; i++) {
                    sb.append(tools.dataBuffer.get(i));
                    if (i < endAt - 1) {
                        sb.append("\n");
                    }
                }

                return sb.toString();
            }
        } else {
            // ========== 模式1和2：直接读取模式 ==========
            synchronized (tools.dataBuffer) {
                if (tools.dataBuffer.isEmpty()) {
                    return "";
                }

                int totalLines = tools.dataBuffer.size();

                // 确定起始行
                int start = (start_line != null && start_line > 0) ? start_line : 1;
                if (start > totalLines) {
                    return "";
                }

                // 确定结束行
                int end;
                if (end_line != null && end_line > 0) {
                    // 模式1：使用end_line
                    end = Math.min(end_line, totalLines);
                } else if (line_count != null && line_count > 0) {
                    // 模式2：使用line_count
                    end = Math.min(start + line_count - 1, totalLines);
                } else {
                    // 默认读到末尾
                    end = totalLines;
                }

                if (end < start) {
                    end = start;
                }

                StringBuilder sb = new StringBuilder();
                for (int i = start - 1; i < end && i < totalLines; i++) {
                    sb.append(tools.dataBuffer.get(i));
                    if (i < end - 1) {
                        sb.append("\n");
                    }
                }

                return sb.toString();
            }
        }
    }

    /**
     * 6. 清除串口接收的数据
     *
     * @param port 服务器端口号
     * @return 清除结果
     */
    public static String clearUartText(int port) {
        String portError = checkPortParam(port);
        if (portError != null) {
            return portError;
        }

        if (!isInstanceRunning(port)) {
            return "错误：端口 " + port + " 的串口工具实例未启动，请先使用 dlqq_run_uart 启动";
        }

        UartTools tools = getInstance(port);

        synchronized (tools.dataBuffer) {
            int clearedLines = tools.dataBuffer.size();
            tools.dataBuffer.clear();
            tools.currentBufferSize = 0;
            tools.totalSentBytes = 0;
            tools.totalReceivedBytes = 0;
            tools.lineCounter = 0;

            // 同时清除GUI显示
            SwingUtilities.invokeLater(() -> {
                if (tools.logArea != null) {
                    tools.logArea.setText("");
                }
            });

            return "已清除 " + clearedLines + " 行数据及所有统计数据";
        }
    }

    /**
     * 6b. 获取当前缓冲区行数
     *
     * @param port 服务器端口号
     * @return 当前缓冲区行数
     */
    public static String getLineCount(int port) {
        String portError = checkPortParam(port);
        if (portError != null) {
            return portError;
        }

        if (!isInstanceRunning(port)) {
            return "错误：端口 " + port + " 的串口工具实例未启动，请先使用 dlqq_run_uart 启动";
        }

        UartTools tools = getInstance(port);

        synchronized (tools.dataBuffer) {
            int lineCount = tools.dataBuffer.size();
            return String.valueOf(lineCount);
        }
    }

    /**
     * 6c. 发送文本数据到串口
     *
     * @param port 服务器端口号
     * @param text 要发送的文本数据
     * @return 发送结果
     */
    public static String sendUartText(int port, String text) {
        String portError = checkPortParam(port);
        if (portError != null) {
            return portError;
        }

        if (!isInstanceRunning(port)) {
            return "错误：端口 " + port + " 的串口工具实例未启动，请先使用 dlqq_run_uart 启动";
        }

        UartTools tools = getInstance(port);

        if (tools.currentPort == null || !tools.isPortOpen) {
            return "错误：串口未打开，请先使用 dlqq_uart_open_com 打开串口";
        }

        if (text == null || text.isEmpty()) {
            return "错误：发送内容不能为空";
        }

        try {
            // 将文本转换为字节发送
            byte[] data = text.getBytes(StandardCharsets.UTF_8);
            int bytesWritten = tools.currentPort.writeBytes(data, data.length);

            if (bytesWritten < 0) {
                return "发送失败";
            }

            // 将发送的数据也存入缓冲区，用于调试追踪
            tools.addSentDataToBuffer(text, false);

            // 更新GUI - 显示与缓冲区一致的格式
            SwingUtilities.invokeLater(() -> {
                if (tools.logArea != null) {
                    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH.mm.ss.SSS"));
                    String logLine = String.format("[%05d] [%s] TX: %s", tools.lineCounter, timestamp, text.trim());
                    tools.logArea.append(logLine + "\n");
                }
            });

            return "已发送 " + bytesWritten + " 字节";
        } catch (Exception e) {
            return "发送出错: " + e.getMessage();
        }
    }

    /**
     * 6d. 发送16进制数据到串口
     *
     * @param port 服务器端口号
     * @param hexArray 16进制字符串数组，如 ["FF", "01", "02"]
     * @return 发送结果
     */
    public static String sendUartHex(int port, List<String> hexArray) {
        String portError = checkPortParam(port);
        if (portError != null) {
            return portError;
        }

        if (!isInstanceRunning(port)) {
            return "错误：端口 " + port + " 的串口工具实例未启动，请先使用 dlqq_run_uart 启动";
        }

        UartTools tools = getInstance(port);

        if (tools.currentPort == null || !tools.isPortOpen) {
            return "错误：串口未打开，请先使用 dlqq_uart_open_com 打开串口";
        }

        if (hexArray == null || hexArray.isEmpty()) {
            return "错误：16进制数据数组不能为空";
        }

        try {
            // 将16进制字符串数组转换为字节数组发送
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            StringBuilder hexString = new StringBuilder();

            for (String hex : hexArray) {
                if (hex == null || hex.trim().isEmpty()) {
                    continue;
                }
                String cleanHex = hex.trim().replaceAll("\\s", "");
                if (cleanHex.length() % 2 != 0) {
                    return "错误：16进制字符串长度必须是偶数，错误数据: " + hex;
                }
                for (int i = 0; i < cleanHex.length(); i += 2) {
                    try {
                        int byteValue = Integer.parseInt(cleanHex.substring(i, i + 2), 16);
                        baos.write(byteValue);
                        hexString.append(String.format("%02X ", byteValue));
                    } catch (NumberFormatException e) {
                        return "错误：包含无效的16进制字符: " + cleanHex.substring(i, i + 2);
                    }
                }
            }

            byte[] data = baos.toByteArray();
            if (data.length == 0) {
                return "错误：没有有效的16进制数据";
            }

            int bytesWritten = tools.currentPort.writeBytes(data, data.length);

            if (bytesWritten < 0) {
                return "发送失败";
            }

            // 将发送的16进制数据也存入缓冲区
            tools.addSentDataToBuffer(hexString.toString().trim(), true);

            // 更新GUI - 显示与缓冲区一致的格式
            SwingUtilities.invokeLater(() -> {
                if (tools.logArea != null) {
                    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH.mm.ss.SSS"));
                    String logLine = String.format("[%05d] [%s] TX[HEX]: %s", tools.lineCounter, timestamp, hexString.toString().trim());
                    tools.logArea.append(logLine + "\n");
                }
            });

            return "已发送 " + bytesWritten + " 字节 (HEX: " + hexString.toString().trim() + ")";
        } catch (Exception e) {
            return "发送出错: " + e.getMessage();
        }
    }

    /**
     * 7. 设置串口数据缓冲区大小
     *
     * @param port 服务器端口号
     * @param sizeBytes 缓冲区大小（字节）
     * @return 设置结果
     */
    public static String setUartBufferSize(int port, int sizeBytes) {
        String portError = checkPortParam(port);
        if (portError != null) {
            return portError;
        }

        if (!isInstanceRunning(port)) {
            return "错误：端口 " + port + " 的串口工具实例未启动，请先使用 dlqq_run_uart 启动";
        }

        UartTools tools = getInstance(port);

        if (sizeBytes <= 0) {
            return "错误：缓冲区大小必须大于0";
        }

        tools.maxBufferSize = sizeBytes;

        // 如果当前数据超出新大小，删除旧数据
        synchronized (tools.dataBuffer) {
            while (tools.currentBufferSize > tools.maxBufferSize && !tools.dataBuffer.isEmpty()) {
                String removed = tools.dataBuffer.remove(0);
                tools.currentBufferSize -= removed.getBytes(StandardCharsets.UTF_8).length;
            }
        }

        return "缓冲区大小已设置为 " + sizeBytes + " 字节 (" + (sizeBytes / 1024 / 1024) + " MB)";
    }

    /**
     * 8. 退出串口工具 - 关闭窗口及进程
     *
     * @param port 服务器端口号
     * @return 退出结果
     */
    public static String exitUart(int port) {
        String portError = checkPortParam(port);
        if (portError != null) {
            return portError;
        }

        UartTools tools = instances.get(port);
        if (tools == null) {
            return "错误：端口 " + port + " 没有正在运行的串口工具实例";
        }

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
        synchronized (tools.dataBuffer) {
            tools.dataBuffer.clear();
            tools.currentBufferSize = 0;
            tools.totalSentBytes = 0;
            tools.totalReceivedBytes = 0;
            tools.lineCounter = 0;
        }

        // 从实例映射中移除
        instances.remove(port);

        return "串口工具实例端口 " + port + " 已完全关闭";
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
                return getUartComList(serverPort);
            case "open":
                String[] openArgs = args.split("\\s+", 2);
                String portName = openArgs[0];
                int baudRate = openArgs.length > 1 ? Integer.parseInt(openArgs[1]) : 1000000;
                return openUartCom(serverPort, portName, baudRate);
            case "close":
                return closeUartCom(serverPort);
            case "get":
                String[] getArgs = args.split("\\s+", 2);
                int start = getArgs.length > 0 ? Integer.parseInt(getArgs[0]) : 1;
                int waitMs = getArgs.length > 1 ? Integer.parseInt(getArgs[1]) : 1000;
                return getUartText(serverPort, start, null, null, waitMs);
            case "clear":
                return clearUartText(serverPort);
            case "size":
                int newSize = args.isEmpty() ? 5 * 1024 * 1024 : Integer.parseInt(args);
                return setUartBufferSize(serverPort, newSize);
            default:
                return "未知命令: " + cmd;
        }
    }

    /**
     * 添加接收数据到缓冲区（带行号和时间戳）
     */
    private void addToBuffer(String data) {
        synchronized (dataBuffer) {
            lineCounter++;
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH.mm.ss.SSS"));
            String formattedData = String.format("[%05d] [%s] RX: %s", lineCounter, timestamp, data);
            dataBuffer.add(formattedData);
            int dataSize = data.getBytes(StandardCharsets.UTF_8).length;
            currentBufferSize += formattedData.getBytes(StandardCharsets.UTF_8).length;
            totalReceivedBytes += dataSize;

            // 超出缓冲区大小时，删除旧数据
            while (currentBufferSize > maxBufferSize && !dataBuffer.isEmpty()) {
                String removed = dataBuffer.remove(0);
                currentBufferSize -= removed.getBytes(StandardCharsets.UTF_8).length;
            }
            
            // 更新状态栏
            updateStatusLabel();
        }
    }

    /**
     * 添加发送数据到缓冲区（带行号和时间戳）
     */
    private void addSentDataToBuffer(String data, boolean isHex) {
        synchronized (dataBuffer) {
            lineCounter++;
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH.mm.ss.SSS"));
            String direction = isHex ? "TX[HEX]" : "TX";
            String formattedData = String.format("[%05d] [%s] %s: %s", lineCounter, timestamp, direction, data);
            dataBuffer.add(formattedData);
            int dataSize = data.getBytes(StandardCharsets.UTF_8).length;
            currentBufferSize += formattedData.getBytes(StandardCharsets.UTF_8).length;
            totalSentBytes += dataSize;

            // 超出缓冲区大小时，删除旧数据
            while (currentBufferSize > maxBufferSize && !dataBuffer.isEmpty()) {
                String removed = dataBuffer.remove(0);
                currentBufferSize -= removed.getBytes(StandardCharsets.UTF_8).length;
            }
            
            // 更新状态栏
            updateStatusLabel();
        }
    }

    /**
     * 获取缓冲区使用情况信息
     */
    private String getBufferUsageInfo() {
        double usagePercent = (double) currentBufferSize / maxBufferSize * 100;
        return String.format("[缓冲区: %d/%d bytes (%.1f%%) | 发送: %d bytes | 接收: %d bytes]",
            currentBufferSize, maxBufferSize, usagePercent, totalSentBytes, totalReceivedBytes);
    }

    /**
     * 更新状态栏显示
     */
    private void updateStatusLabel() {
        if (statusLabel == null) return;
        SwingUtilities.invokeLater(() -> {
            double usagePercent = (double) currentBufferSize / maxBufferSize * 100;
            int lineCount = dataBuffer.size();
            statusLabel.setText(String.format("行数: %d | 缓冲区: %d/%d bytes (%.1f%%) | 发送: %d bytes | 接收: %d bytes",
                lineCount, currentBufferSize, maxBufferSize, usagePercent, totalSentBytes, totalReceivedBytes));
        });
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
                String result = closeUartCom(serverPort);
                JOptionPane.showMessageDialog(guiWindow, result);
            } else {
                // 当前未打开，执行打开
                String selected = (String) portCombo.getSelectedItem();
                if (selected != null && !selected.equals("未找到串口")) {
                    String portName = selected.split(" - ")[0].trim();
                    int baud = (Integer) baudCombo.getSelectedItem();
                    String result = openUartCom(serverPort, portName, baud);
                    JOptionPane.showMessageDialog(guiWindow, result);
                }
            }
        });

        controlPanel.add(openCloseButton);

        mainPanel.add(controlPanel, BorderLayout.NORTH);

        // 通信日志区域（合并接收数据和日志）
        JPanel logPanel = new JPanel(new BorderLayout(5, 5));
        logPanel.setBorder(BorderFactory.createTitledBorder("通信日志"));

        logArea = new JTextArea(25, 60);
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        logPanel.add(logScroll, BorderLayout.CENTER);

        JPanel logButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton clearLogButton = new JButton("清空日志");
        clearLogButton.addActionListener(e -> {
            logArea.setText("");
            clearUartText(serverPort);
        });
        logButtonPanel.add(clearLogButton);
        logPanel.add(logButtonPanel, BorderLayout.SOUTH);

        mainPanel.add(logPanel, BorderLayout.CENTER);

        // 底部状态栏（显示缓冲区占用率）
        statusLabel = new JLabel("行数: 0 | 缓冲区: 0/0 bytes (0.0%) | 发送: 0 bytes | 接收: 0 bytes");
        statusLabel.setBorder(BorderFactory.createEtchedBorder());
        statusLabel.setFont(new Font("Consolas", Font.PLAIN, 11));
        mainPanel.add(statusLabel, BorderLayout.SOUTH);

        guiWindow.add(mainPanel);

        // 关闭处理
        guiWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeUartCom(serverPort);
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

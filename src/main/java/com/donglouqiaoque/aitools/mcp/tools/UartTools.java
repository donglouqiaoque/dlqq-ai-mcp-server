package com.donglouqiaoque.aitools.mcp.tools;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * UART串口工具 - 支持CH340等USB转串口设备
 * 提供串口连接、发送接收数据、日志记录等功能
 */
public class UartTools {

    // 存储所有打开的串口连接
    private static final Map<String, SerialPortConnection> activeConnections = new HashMap<>();

    /**
     * 串口连接包装类
     */
    public static class SerialPortConnection {
        private final SerialPort serialPort;
        private final String portName;
        private final AtomicBoolean isOpen;
        private final Queue<String> receiveBuffer;
        private final List<String> logBuffer;
        private final int maxLogSize;

        public SerialPortConnection(SerialPort port, int maxLogSize) {
            this.serialPort = port;
            this.portName = port.getSystemPortName();
            this.isOpen = new AtomicBoolean(false);
            this.receiveBuffer = new ConcurrentLinkedQueue<>();
            this.logBuffer = Collections.synchronizedList(new ArrayList<>());
            this.maxLogSize = maxLogSize;
        }

        public SerialPort getSerialPort() { return serialPort; }
        public String getPortName() { return portName; }
        public boolean isOpen() { return isOpen.get(); }
        public void setOpen(boolean open) { isOpen.set(open); }
        public Queue<String> getReceiveBuffer() { return receiveBuffer; }
        public List<String> getLogBuffer() { return logBuffer; }

        public void addToLog(String direction, String data) {
            String timestamp = java.time.LocalDateTime.now().toString();
            String logEntry = String.format("[%s] %s: %s", timestamp, direction, data);
            logBuffer.add(logEntry);

            // 限制日志大小
            while (logBuffer.size() > maxLogSize) {
                logBuffer.remove(0);
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
     * 获取所有可用串口列表
     *
     * @return 串口信息列表
     */
    public static List<PortInfo> getAvailablePorts() {
        List<PortInfo> ports = new ArrayList<>();
        SerialPort[] systemPorts = SerialPort.getCommPorts();

        for (SerialPort port : systemPorts) {
            PortInfo info = new PortInfo();
            info.setPortName(port.getSystemPortName());
            info.setDescriptivePortName(port.getDescriptivePortName());
            info.setPortLocation(port.getPortLocation());
            info.setVendorId(String.format("0x%04X", port.getVendorID()));
            info.setProductId(String.format("0x%04X", port.getProductID()));
            info.setOpen(port.isOpen());
            ports.add(info);
        }

        return ports;
    }

    /**
     * 串口信息
     */
    public static class PortInfo {
        private String portName;
        private String descriptivePortName;
        private String portLocation;
        private String vendorId;
        private String productId;
        private boolean isOpen;

        public String getPortName() { return portName; }
        public void setPortName(String portName) { this.portName = portName; }
        public String getDescriptivePortName() { return descriptivePortName; }
        public void setDescriptivePortName(String descriptivePortName) { this.descriptivePortName = descriptivePortName; }
        public String getPortLocation() { return portLocation; }
        public void setPortLocation(String portLocation) { this.portLocation = portLocation; }
        public String getVendorId() { return vendorId; }
        public void setVendorId(String vendorId) { this.vendorId = vendorId; }
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public boolean isOpen() { return isOpen; }
        public void setOpen(boolean open) { isOpen = open; }

        @Override
        public String toString() {
            return String.format("%s (%s, VID:%s PID:%s)",
                    portName, descriptivePortName, vendorId, productId);
        }
    }

    /**
     * 打开串口连接（支持CH340等设备）
     *
     * @param portName     串口名称（如 COM3, /dev/ttyUSB0）
     * @param baudRate     波特率（默认 115200）
     * @param dataBits     数据位（5,6,7,8，默认8）
     * @param stopBits     停止位（1,1.5,2，默认1）
     * @param parity       校验位（NONE, EVEN, ODD, MARK, SPACE，默认NONE）
     * @param maxLogSize   最大日志条数
     * @return 连接是否成功
     */
    public static boolean openPort(String portName, int baudRate, int dataBits,
                                    double stopBits, String parity, int maxLogSize) {
        // 先检查是否已连接
        if (activeConnections.containsKey(portName)) {
            SerialPortConnection existing = activeConnections.get(portName);
            if (existing.isOpen()) {
                return true; // 已经连接
            } else {
                activeConnections.remove(portName);
            }
        }

        // 查找串口
        SerialPort port = findPortByName(portName);
        if (port == null) {
            return false;
        }

        // 配置串口参数
        port.setBaudRate(baudRate);
        port.setNumDataBits(dataBits);

        // 设置停止位
        if (stopBits == 1.5) {
            port.setNumStopBits(SerialPort.ONE_POINT_FIVE_STOP_BITS);
        } else if (stopBits == 2) {
            port.setNumStopBits(SerialPort.TWO_STOP_BITS);
        } else {
            port.setNumStopBits(SerialPort.ONE_STOP_BIT);
        }

        // 设置校验位
        int parityMode;
        switch (parity.toUpperCase()) {
            case "EVEN":
                parityMode = SerialPort.EVEN_PARITY;
                break;
            case "ODD":
                parityMode = SerialPort.ODD_PARITY;
                break;
            case "MARK":
                parityMode = SerialPort.MARK_PARITY;
                break;
            case "SPACE":
                parityMode = SerialPort.SPACE_PARITY;
                break;
            default:
                parityMode = SerialPort.NO_PARITY;
        }
        port.setParity(parityMode);

        // 设置流控制（默认无）
        port.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);

        // 设置超时
        port.setComPortTimeouts(
                SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING,
                1000, 1000
        );

        // 打开串口
        if (!port.openPort()) {
            return false;
        }

        // 创建连接对象
        SerialPortConnection connection = new SerialPortConnection(port, maxLogSize);
        connection.setOpen(true);

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
                            connection.getReceiveBuffer().offer(received);
                            connection.addToLog("RX", received.trim());
                        }
                    } catch (Exception e) {
                        System.err.println("串口读取错误: " + e.getMessage());
                    }
                }
            }
        });

        activeConnections.put(portName, connection);
        return true;
    }

    /**
     * 简化的串口打开方法（使用默认参数）
     */
    public static boolean openPort(String portName) {
        return openPort(portName, 115200, 8, 1, "NONE", 1000);
    }

    /**
     * 关闭串口连接
     *
     * @param portName 串口名称
     * @return 是否成功关闭
     */
    public static boolean closePort(String portName) {
        SerialPortConnection connection = activeConnections.get(portName);
        if (connection == null) {
            return false;
        }

        SerialPort port = connection.getSerialPort();
        if (port.isOpen()) {
            port.removeDataListener();
            port.closePort();
        }
        connection.setOpen(false);
        activeConnections.remove(portName);
        return true;
    }

    /**
     * 发送数据到串口
     *
     * @param portName 串口名称
     * @param data     要发送的数据
     * @param addNewline 是否添加换行符
     * @return 是否发送成功
     */
    public static boolean sendData(String portName, String data, boolean addNewline) {
        SerialPortConnection connection = activeConnections.get(portName);
        if (connection == null || !connection.isOpen()) {
            return false;
        }

        try {
            String sendData = addNewline ? data + "\r\n" : data;
            byte[] bytes = sendData.getBytes(StandardCharsets.UTF_8);

            int bytesWritten = connection.getSerialPort().writeBytes(bytes, bytes.length);
            boolean success = bytesWritten == bytes.length;

            if (success) {
                connection.addToLog("TX", data);
            }

            return success;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从串口接收数据（非阻塞）
     *
     * @param portName 串口名称
     * @return 接收到的数据，如果没有数据则返回空字符串
     */
    public static String receiveData(String portName) {
        SerialPortConnection connection = activeConnections.get(portName);
        if (connection == null || !connection.isOpen()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        String data;
        while ((data = connection.getReceiveBuffer().poll()) != null) {
            sb.append(data);
        }
        return sb.toString();
    }

    /**
     * 等待并接收数据（带超时）
     *
     * @param portName      串口名称
     * @param timeoutMillis 超时时间（毫秒）
     * @return 接收到的数据
     */
    public static String receiveDataWithTimeout(String portName, int timeoutMillis) {
        SerialPortConnection connection = activeConnections.get(portName);
        if (connection == null || !connection.isOpen()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            String data = connection.getReceiveBuffer().poll();
            if (data != null) {
                sb.append(data);
            } else {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        return sb.toString();
    }

    /**
     * 获取串口日志
     *
     * @param portName 串口名称
     * @param count    获取最近的日志条数（0表示全部）
     * @return 日志内容
     */
    public static String getLogs(String portName, int count) {
        SerialPortConnection connection = activeConnections.get(portName);
        if (connection == null) {
            return "串口未连接: " + portName;
        }
        return connection.getLogs(count);
    }

    /**
     * 清除日志缓冲区
     *
     * @param portName 串口名称
     * @return 是否成功
     */
    public static boolean clearLogs(String portName) {
        SerialPortConnection connection = activeConnections.get(portName);
        if (connection == null) {
            return false;
        }
        connection.getLogBuffer().clear();
        return true;
    }

    /**
     * 检查串口是否已连接
     *
     * @param portName 串口名称
     * @return 是否已连接
     */
    public static boolean isConnected(String portName) {
        SerialPortConnection connection = activeConnections.get(portName);
        return connection != null && connection.isOpen();
    }

    /**
     * 获取已连接的串口列表
     *
     * @return 已连接的串口名称列表
     */
    public static List<String> getConnectedPorts() {
        List<String> ports = new ArrayList<>();
        for (Map.Entry<String, SerialPortConnection> entry : activeConnections.entrySet()) {
            if (entry.getValue().isOpen()) {
                ports.add(entry.getKey());
            }
        }
        return ports;
    }

    /**
     * 关闭所有串口连接
     */
    public static void closeAllPorts() {
        for (String portName : new ArrayList<>(activeConnections.keySet())) {
            closePort(portName);
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

    /**
     * 检测CH340设备（Vendor ID: 0x1A86, Product ID: 0x7523）
     *
     * @return CH340设备列表
     */
    public static List<PortInfo> findCH340Devices() {
        List<PortInfo> ch340Ports = new ArrayList<>();
        SerialPort[] ports = SerialPort.getCommPorts();

        for (SerialPort port : ports) {
            // CH340的VID是0x1A86，PID是0x7523
            if (port.getVendorID() == 0x1A86 && port.getProductID() == 0x7523) {
                PortInfo info = new PortInfo();
                info.setPortName(port.getSystemPortName());
                info.setDescriptivePortName(port.getDescriptivePortName());
                info.setPortLocation(port.getPortLocation());
                info.setVendorId(String.format("0x%04X", port.getVendorID()));
                info.setProductId(String.format("0x%04X", port.getProductID()));
                info.setOpen(port.isOpen());
                ch340Ports.add(info);
            }
        }

        return ch340Ports;
    }

    /**
     * 发送AT指令并等待响应
     *
     * @param portName 串口名称
     * @param command  AT指令
     * @param timeout  等待超时（毫秒）
     * @return 响应内容
     */
    public static String sendATCommand(String portName, String command, int timeout) {
        // 清空接收缓冲区
        SerialPortConnection connection = activeConnections.get(portName);
        if (connection == null || !connection.isOpen()) {
            return "错误：串口未连接";
        }
        connection.getReceiveBuffer().clear();

        // 发送指令
        boolean sent = sendData(portName, command, true);
        if (!sent) {
            return "错误：发送失败";
        }

        // 等待响应
        StringBuilder response = new StringBuilder();
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeout) {
            String data = connection.getReceiveBuffer().poll();
            if (data != null) {
                response.append(data);
                // 检查是否收到OK或ERROR
                if (response.toString().contains("OK") ||
                    response.toString().contains("ERROR")) {
                    break;
                }
            } else {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        return response.toString();
    }
}

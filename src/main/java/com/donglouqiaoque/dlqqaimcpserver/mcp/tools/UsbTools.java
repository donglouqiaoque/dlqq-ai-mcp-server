package com.donglouqiaoque.dlqqaimcpserver.mcp.tools;

import org.usb4java.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * USB工具 - 支持USB HID和CDC设备通信
 * 使用usb4java库实现跨平台USB通信
 */
public class UsbTools {

    // 多实例管理 - 每个实例ID对应一个实例
    private static final Map<Integer, UsbTools> instances = new ConcurrentHashMap<>();
    private static int nextInstanceId = 1;

    // 实例ID和配置
    private final int instanceId;
    private int serverPort = 0;
    private boolean isRunning = false;

    // USB设备状态
    private Device currentDevice = null;
    private DeviceHandle deviceHandle = null;
    private boolean isDeviceOpen = false;
    private int vendorId = 0;
    private int productId = 0;
    private String deviceType = "HID"; // HID或CDC

    // 端点配置
    private byte inEndpoint = (byte) 0x81;   // 默认输入端点
    private byte outEndpoint = (byte) 0x01;  // 默认输出端点
    private int interfaceNumber = 0;

    // 数据缓冲区
    private final List<String> dataBuffer;
    private int maxBufferSize = 5 * 1024 * 1024; // 默认5MB
    private int currentBufferSize = 0;
    private long totalSentBytes = 0;
    private long totalReceivedBytes = 0;
    private int lineCounter = 0;

    // 显示格式
    private boolean displayAsHex = false;

    // 线程池
    private final ExecutorService executor;
    private Future<?> readTask = null;
    private final AtomicBoolean shouldRead = new AtomicBoolean(false);

    // GUI窗口
    private JFrame guiWindow = null;
    private JTextArea logArea;
    private JLabel statusLabel;
    private JComboBox<String> deviceCombo;
    private JLabel connectStatusLabel;
    private JButton openCloseButton;

    // 时间格式化
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");

    // 设备列表缓存
    private final Map<String, Device> deviceCache = new ConcurrentHashMap<>();

    private UsbTools(int instanceId) {
        this.instanceId = instanceId;
        this.dataBuffer = Collections.synchronizedList(new ArrayList<>());
        this.executor = Executors.newCachedThreadPool();
    }

    /**
     * 创建新实例
     * 注意：实例以 serverPort 为key存储，与外部工具的port参数保持一致
     */
    public static synchronized UsbTools createInstance(int serverPort) {
        // 如果该端口已有实例，直接返回现有实例
        UsbTools existing = instances.get(serverPort);
        if (existing != null) {
            return existing;
        }
        int id = nextInstanceId++;
        UsbTools instance = new UsbTools(id);
        instance.serverPort = serverPort;
        instances.put(serverPort, instance);
        return instance;
    }

    /**
     * 获取指定ID的实例
     */
    public static UsbTools getInstance(int instanceId) {
        return instances.get(instanceId);
    }

    /**
     * 检查实例是否存在且运行中
     */
    public static boolean isInstanceRunning(int instanceId) {
        UsbTools tools = instances.get(instanceId);
        return tools != null && tools.isRunning;
    }

    /**
     * 检查实例是否存在
     */
    public static boolean hasInstance(int instanceId) {
        return instances.containsKey(instanceId);
    }

    /**
     * 获取所有实例ID
     */
    public static Set<Integer> getAllInstanceIds() {
        return new HashSet<>(instances.keySet());
    }

    /**
     * 移除并停止实例
     */
    public static void removeInstance(int instanceId) {
        UsbTools tools = instances.remove(instanceId);
        if (tools != null) {
            tools.stop();
        }
    }

    /**
     * 启动USB工具（打开GUI窗口）
     */
    public String start() {
        if (isRunning) {
            return "USB工具实例 " + instanceId + " 已在运行中";
        }

        try {
            // 初始化USB库
            Context context = new Context();
            int result = LibUsb.init(context);
            if (result != LibUsb.SUCCESS) {
                return "错误：无法初始化USB库: " + LibUsb.strError(result);
            }

            // 创建GUI窗口
            SwingUtilities.invokeLater(() -> createGUI());

            isRunning = true;
            log("USB工具实例 " + instanceId + " 已启动");
            return "USB工具实例 " + instanceId + " 已启动，端口: " + serverPort;

        } catch (Exception e) {
            return "错误：启动USB工具失败: " + e.getMessage();
        }
    }

    /**
     * 停止USB工具
     */
    public String stop() {
        if (!isRunning) {
            return "USB工具实例 " + instanceId + " 未在运行";
        }

        // 关闭USB设备
        closeDevice();

        // 停止读取线程
        shouldRead.set(false);
        if (readTask != null) {
            readTask.cancel(true);
            readTask = null;
        }

        // 关闭GUI窗口
        if (guiWindow != null) {
            SwingUtilities.invokeLater(() -> {
                guiWindow.dispose();
                guiWindow = null;
            });
        }

        // 关闭线程池
        executor.shutdown();

        isRunning = false;
        log("USB工具实例 " + instanceId + " 已停止");
        return "USB工具实例 " + instanceId + " 已停止";
    }

    /**
     * 重新启动USB工具
     */
    public String restart() {
        stop();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // ignore
        }
        return start();
    }

    /**
     * 扫描USB设备
     */
    public List<Map<String, Object>> scanDevices() {
        List<Map<String, Object>> deviceList = new ArrayList<>();
        deviceCache.clear();

        try {
            DeviceList list = new DeviceList();
            int result = LibUsb.getDeviceList(null, list);
            if (result < 0) {
                log("扫描USB设备失败: " + LibUsb.strError(result));
                return deviceList;
            }

            for (Device device : list) {
                DeviceDescriptor descriptor = new DeviceDescriptor();
                result = LibUsb.getDeviceDescriptor(device, descriptor);
                if (result != LibUsb.SUCCESS) {
                    continue;
                }

                int vid = descriptor.idVendor() & 0xFFFF;
                int pid = descriptor.idProduct() & 0xFFFF;

                // 过滤掉系统设备（只显示有名称的设备或指定类设备）
                String deviceName = getDeviceName(device, descriptor);
                String deviceClass = getDeviceClassName(descriptor.bDeviceClass());

                // 只显示HID、CDC或其他常见通信类设备
                if (isCommunicationDevice(device, descriptor)) {
                    String deviceId = String.format("%04X:%04X", vid, pid);
                    String displayName = String.format("%s [%s] - %s", deviceName, deviceId, deviceClass);

                    Map<String, Object> deviceInfo = new HashMap<>();
                    deviceInfo.put("id", deviceId);
                    deviceInfo.put("name", deviceName);
                    deviceInfo.put("vendorId", vid);
                    deviceInfo.put("productId", pid);
                    deviceInfo.put("vendorIdHex", String.format("0x%04X", vid));
                    deviceInfo.put("productIdHex", String.format("0x%04X", pid));
                    deviceInfo.put("deviceClass", deviceClass);
                    deviceInfo.put("displayName", displayName);

                    deviceList.add(deviceInfo);
                    deviceCache.put(deviceId, device);
                }
            }

            LibUsb.freeDeviceList(list, true);
            log("扫描完成，找到 " + deviceList.size() + " 个USB设备");

        } catch (Exception e) {
            log("扫描USB设备异常: " + e.getMessage());
        }

        // 更新GUI下拉框
        updateDeviceCombo(deviceList);
        return deviceList;
    }

    /**
     * 打开USB设备
     */
    public String openDevice(int vendorId, int productId, String type) {
        if (isDeviceOpen) {
            return "错误：设备已打开，请先关闭当前设备";
        }

        this.vendorId = vendorId;
        this.productId = productId;
        this.deviceType = (type != null && !type.isEmpty()) ? type.toUpperCase() : "HID";

        try {
            // 查找设备
            Device device = findDevice(vendorId, productId);
            if (device == null) {
                return "错误：未找到指定USB设备 (VID: " + String.format("0x%04X", vendorId) +
                       ", PID: " + String.format("0x%04X", productId) + ")";
            }

            // 打开设备
            DeviceHandle handle = new DeviceHandle();
            int result = LibUsb.open(device, handle);
            if (result != LibUsb.SUCCESS) {
                return "错误：无法打开USB设备: " + LibUsb.strError(result);
            }

            // 查找端点
            boolean endpointsFound = findEndpoints(device, handle);
            if (!endpointsFound) {
                LibUsb.close(handle);
                return "错误：无法找到合适的USB端点";
            }

            // 声明接口
            result = LibUsb.claimInterface(handle, interfaceNumber);
            if (result != LibUsb.SUCCESS) {
                LibUsb.close(handle);
                return "错误：无法声明USB接口: " + LibUsb.strError(result);
            }

            this.currentDevice = device;
            this.deviceHandle = handle;
            this.isDeviceOpen = true;

            // 启动读取线程
            shouldRead.set(true);
            readTask = executor.submit(this::readLoop);

            log("USB设备已打开: " + String.format("VID: 0x%04X, PID: 0x%04X", vendorId, productId));
            updateConnectStatus(true);
            return "USB设备已打开 (VID: " + String.format("0x%04X", vendorId) +
                   ", PID: " + String.format("0x%04X", productId) + ", 类型: " + deviceType + ")";

        } catch (Exception e) {
            return "错误：打开USB设备失败: " + e.getMessage();
        }
    }

    /**
     * 关闭USB设备
     */
    public String closeDevice() {
        if (!isDeviceOpen) {
            return "错误：设备未打开";
        }

        shouldRead.set(false);
        if (readTask != null) {
            readTask.cancel(true);
            readTask = null;
        }

        try {
            if (deviceHandle != null) {
                LibUsb.releaseInterface(deviceHandle, interfaceNumber);
                LibUsb.close(deviceHandle);
                deviceHandle = null;
            }
        } catch (Exception e) {
            log("关闭设备异常: " + e.getMessage());
        }

        currentDevice = null;
        isDeviceOpen = false;
        log("USB设备已关闭");
        updateConnectStatus(false);
        return "USB设备已关闭";
    }

    /**
     * 发送数据（十六进制）
     */
    public String sendHex(String[] hexArray) {
        if (!isDeviceOpen) {
            return "错误：设备未打开";
        }

        if (hexArray == null || hexArray.length == 0) {
            return "错误：数据不能为空";
        }

        try {
            // 合并十六进制字符串
            StringBuilder hexBuilder = new StringBuilder();
            for (String hex : hexArray) {
                hexBuilder.append(hex.replaceAll("\\s", ""));
            }
            String hexString = hexBuilder.toString();

            // 转换为字节数组
            int len = hexString.length();
            byte[] data = new byte[len / 2];
            for (int i = 0; i < len; i += 2) {
                data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                                     + Character.digit(hexString.charAt(i + 1), 16));
            }

            return sendData(data);

        } catch (Exception e) {
            return "错误：发送数据失败: " + e.getMessage();
        }
    }

    /**
     * 发送数据（文本）
     */
    public String sendText(String text) {
        if (!isDeviceOpen) {
            return "错误：设备未打开";
        }

        if (text == null || text.isEmpty()) {
            return "错误：数据不能为空";
        }

        try {
            byte[] data = text.getBytes("UTF-8");
            return sendData(data);
        } catch (UnsupportedEncodingException e) {
            return "错误：编码失败: " + e.getMessage();
        }
    }

    /**
     * 发送原始数据
     */
    private String sendData(byte[] data) {
        if (deviceHandle == null) {
            return "错误：设备句柄为空";
        }

        try {
            ByteBuffer buffer = ByteBuffer.allocateDirect(data.length);
            buffer.put(data);
            buffer.flip();

            IntBuffer transferred = IntBuffer.allocate(1);
            int result = LibUsb.bulkTransfer(deviceHandle, outEndpoint, buffer, transferred, 5000);

            if (result != LibUsb.SUCCESS) {
                return "错误：发送数据失败: " + LibUsb.strError(result);
            }

            int sent = transferred.get();
            totalSentBytes += sent;

            // 记录到缓冲区
            String logEntry = formatLogEntry("发送", data, sent, true);
            addToBuffer(logEntry, sent);

            return "发送成功 (" + sent + " 字节)";

        } catch (Exception e) {
            return "错误：发送数据异常: " + e.getMessage();
        }
    }

    /**
     * 读取数据循环
     */
    private void readLoop() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(1024);

        while (shouldRead.get() && isDeviceOpen && deviceHandle != null) {
            try {
                IntBuffer transferred = IntBuffer.allocate(1);
                buffer.clear();

                int result = LibUsb.bulkTransfer(deviceHandle, inEndpoint, buffer, transferred, 100);

                if (result == LibUsb.SUCCESS) {
                    int received = transferred.get();
                    if (received > 0) {
                        byte[] data = new byte[received];
                        buffer.get(data);

                        totalReceivedBytes += received;

                        // 记录到缓冲区
                        String logEntry = formatLogEntry("接收", data, received, false);
                        addToBuffer(logEntry, received);
                    }
                } else if (result != LibUsb.ERROR_TIMEOUT) {
                    log("读取数据错误: " + LibUsb.strError(result));
                }

            } catch (Exception e) {
                if (shouldRead.get()) {
                    log("读取数据异常: " + e.getMessage());
                }
            }

            // 短暂休眠，避免CPU占用过高
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    /**
     * 获取缓冲区数据
     */
    public List<String> getLog(int startLine, int endLine, int lineCount, int timeout) {
        return getLogInternal(startLine, endLine, lineCount);
    }

    /**
     * 获取缓冲区数据（强制文本格式）
     */
    public List<String> getLogText(int startLine, int endLine, int lineCount) {
        return getLogInternal(startLine, endLine, lineCount);
    }

    /**
     * 获取缓冲区数据（强制HEX格式）
     */
    public List<String> getLogHex(int startLine, int endLine, int lineCount) {
        // 临时切换为HEX格式
        boolean oldFormat = displayAsHex;
        displayAsHex = true;
        List<String> result = getLogInternal(startLine, endLine, lineCount);
        displayAsHex = oldFormat;
        return result;
    }

    /**
     * 内部获取缓冲区数据
     */
    private List<String> getLogInternal(int startLine, int endLine, int lineCount) {
        synchronized (dataBuffer) {
            int totalLines = dataBuffer.size();
            if (totalLines == 0) {
                return new ArrayList<>();
            }

            // 计算实际的起始和结束行
            int start = (startLine > 0) ? startLine - 1 : 0;
            int end;

            if (endLine > 0) {
                end = Math.min(endLine, totalLines);
            } else if (lineCount > 0) {
                end = Math.min(start + lineCount, totalLines);
            } else {
                end = totalLines;
            }

            if (start < 0) start = 0;
            if (start >= totalLines) start = totalLines - 1;
            if (end > totalLines) end = totalLines;

            List<String> result = new ArrayList<>();
            for (int i = start; i < end; i++) {
                if (i >= 0 && i < totalLines) {
                    result.add(dataBuffer.get(i));
                }
            }
            return result;
        }
    }

    /**
     * 获取缓冲区行数
     */
    public int getLineCount() {
        return dataBuffer.size();
    }

    /**
     * 清除缓冲区
     */
    public String clearBuffer() {
        synchronized (dataBuffer) {
            int count = dataBuffer.size();
            dataBuffer.clear();
            currentBufferSize = 0;
            lineCounter = 0;
            return "已清除 " + count + " 行数据";
        }
    }

    /**
     * 设置缓冲区大小
     */
    public String setBufferSize(int sizeBytes) {
        if (sizeBytes < 1024) {
            return "错误：缓冲区大小不能小于1024字节";
        }
        if (sizeBytes > 100 * 1024 * 1024) {
            return "错误：缓冲区大小不能超过100MB";
        }
        this.maxBufferSize = sizeBytes;
        return "缓冲区大小已设置为 " + sizeBytes + " 字节";
    }

    /**
     * 切换显示格式
     */
    public String switchTextHex() {
        displayAsHex = !displayAsHex;
        return "显示格式已切换为: " + (displayAsHex ? "HEX" : "文本");
    }

    // ===================== 辅助方法 =====================

    /**
     * 查找设备
     */
    private Device findDevice(int vid, int pid) {
        // 先在缓存中查找
        String deviceId = String.format("%04X:%04X", vid, pid);
        Device cached = deviceCache.get(deviceId);
        if (cached != null) {
            return cached;
        }

        // 重新扫描
        DeviceList list = new DeviceList();
        int result = LibUsb.getDeviceList(null, list);
        if (result < 0) {
            return null;
        }

        for (Device device : list) {
            DeviceDescriptor descriptor = new DeviceDescriptor();
            result = LibUsb.getDeviceDescriptor(device, descriptor);
            if (result == LibUsb.SUCCESS) {
                if ((descriptor.idVendor() & 0xFFFF) == vid &&
                    (descriptor.idProduct() & 0xFFFF) == pid) {
                    // 复制设备引用，释放列表
                    Device found = device;
                    LibUsb.refDevice(found);
                    LibUsb.freeDeviceList(list, true);
                    return found;
                }
            }
        }

        LibUsb.freeDeviceList(list, true);
        return null;
    }

    /**
     * 查找端点
     */
    private boolean findEndpoints(Device device, DeviceHandle handle) {
        try {
            ConfigDescriptor config = new ConfigDescriptor();
            int result = LibUsb.getActiveConfigDescriptor(device, config);
            if (result != LibUsb.SUCCESS) {
                return false;
            }

            // 遍历接口和端点
            // 使用直接字段访问 - usb4java 1.3.0使用结构体映射
            Interface[] interfaces = config.iface();
            if (interfaces != null) {
                for (int i = 0; i < interfaces.length && i < config.bNumInterfaces(); i++) {
                    Interface iface = interfaces[i];
                    if (iface == null) continue;

                    InterfaceDescriptor[] altsettings = iface.altsetting();
                    if (altsettings == null) continue;

                    for (int j = 0; j < altsettings.length && j < iface.numAltsetting(); j++) {
                        InterfaceDescriptor altsetting = altsettings[j];
                        if (altsetting == null) continue;

                        EndpointDescriptor[] endpoints = altsetting.endpoint();
                        if (endpoints == null) continue;

                        for (int k = 0; k < endpoints.length && k < altsetting.bNumEndpoints(); k++) {
                            EndpointDescriptor endpoint = endpoints[k];
                            if (endpoint == null) continue;

                            byte addr = endpoint.bEndpointAddress();
                            byte attr = endpoint.bmAttributes();

                            // 查找批量传输端点 (传输类型在低2位)
                            int transferType = attr & 0x03;
                            if (transferType == 2) { // BULK = 2
                                if ((addr & LibUsb.ENDPOINT_DIR_MASK) == LibUsb.ENDPOINT_IN) {
                                    inEndpoint = addr;
                                } else {
                                    outEndpoint = addr;
                                }
                            }

                            // 查找中断传输端点（HID常用） (transferType = 3)
                            if (transferType == 3) { // INTERRUPT = 3
                                if ((addr & LibUsb.ENDPOINT_DIR_MASK) == LibUsb.ENDPOINT_IN) {
                                    inEndpoint = addr;
                                } else {
                                    outEndpoint = addr;
                                }
                            }
                        }

                        interfaceNumber = altsetting.bInterfaceNumber();
                    }
                }
            }

            LibUsb.freeConfigDescriptor(config);
            return true;

        } catch (Exception e) {
            log("查找端点异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 判断是否为通信设备
     */
    private boolean isCommunicationDevice(Device device, DeviceDescriptor descriptor) {
        int deviceClass = descriptor.bDeviceClass() & 0xFF;
        int vid = descriptor.idVendor() & 0xFFFF;
        int pid = descriptor.idProduct() & 0xFFFF;

        // HID设备类
        if (deviceClass == LibUsb.CLASS_HID) {
            return true;
        }

        // CDC设备类 (0x0A = 10)
        if (deviceClass == 0x0A) {
            return true;
        }

        // 供应商特定类（可能是HID）
        if (deviceClass == LibUsb.CLASS_VENDOR_SPEC) {
            return true;
        }

        // 常见USB厂商ID
        // 0x0483 = STMicroelectronics
        // 0x0D28 = NXP
        // 0x04D8 = Microchip
        // 0x10C4 = Silicon Labs
        // 0x1A86 = QinHeng (CH340等)
        // 0x067B = Prolific
        // 0x0403 = FTDI
        // 0x2341 = Arduino
        // 0x2A03 = Arduino

        int[] knownVendors = {0x0483, 0x0D28, 0x04D8, 0x10C4, 0x1A86, 0x067B, 0x0403, 0x2341, 0x2A03};
        for (int knownVid : knownVendors) {
            if (vid == knownVid) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取设备名称
     */
    private String getDeviceName(Device device, DeviceDescriptor descriptor) {
        // 从设备描述符获取字符串描述
        String manufacturer = getStringDescriptor(device, descriptor.iManufacturer());
        String product = getStringDescriptor(device, descriptor.iProduct());

        if (product != null && !product.isEmpty()) {
            return product;
        }
        if (manufacturer != null && !manufacturer.isEmpty()) {
            return manufacturer + " Device";
        }

        return "USB Device";
    }

    /**
     * 获取字符串描述符
     */
    private String getStringDescriptor(Device device, byte index) {
        if (index == 0) {
            return null;
        }

        DeviceHandle handle = new DeviceHandle();
        int result = LibUsb.open(device, handle);
        if (result != LibUsb.SUCCESS) {
            return null;
        }

        try {
            // 使用ByteBuffer分配内存
            ByteBuffer buffer = ByteBuffer.allocateDirect(256);
            result = LibUsb.getStringDescriptor(handle, index, (short) 0x0409, buffer);
            if (result >= 0) {
                // 将ByteBuffer转换为String
                byte[] bytes = new byte[result];
                buffer.get(bytes, 0, result);
                // USB字符串描述符是UTF-16LE格式，跳过前2个字节(header)
                if (bytes.length > 2) {
                    return new String(bytes, 2, bytes.length - 2, "UTF-16LE");
                }
            }
        } catch (Exception e) {
            // ignore
        } finally {
            LibUsb.close(handle);
        }

        return null;
    }

    /**
     * 获取设备类名称
     */
    private String getDeviceClassName(int deviceClass) {
        switch (deviceClass) {
            case LibUsb.CLASS_PER_INTERFACE:
                return "Per Interface";
            case LibUsb.CLASS_AUDIO:
                return "Audio";
            case LibUsb.CLASS_COMM:
                return "Communication";
            case LibUsb.CLASS_HID:
                return "HID";
            case LibUsb.CLASS_PHYSICAL:
                return "Physical";
            case LibUsb.CLASS_IMAGE:
                return "Image";
            case LibUsb.CLASS_PRINTER:
                return "Printer";
            case LibUsb.CLASS_MASS_STORAGE:
                return "Mass Storage";
            case LibUsb.CLASS_HUB:
                return "Hub";
            case 0x0A: // CDC
                return "CDC";
            case LibUsb.CLASS_SMART_CARD:
                return "Smart Card";
            case LibUsb.CLASS_CONTENT_SECURITY:
                return "Security";
            case LibUsb.CLASS_VIDEO:
                return "Video";
            case LibUsb.CLASS_PERSONAL_HEALTHCARE:
                return "Healthcare";
            case LibUsb.CLASS_DIAGNOSTIC_DEVICE:
                return "Diagnostic";
            case LibUsb.CLASS_WIRELESS:
                return "Wireless";
            case LibUsb.CLASS_APPLICATION:
                return "Application";
            case LibUsb.CLASS_VENDOR_SPEC:
                return "Vendor Specific";
            default:
                return "Unknown (" + deviceClass + ")";
        }
    }

    /**
     * 格式化日志条目
     */
    private String formatLogEntry(String direction, byte[] data, int length, boolean isSend) {
        String timestamp = timeFormat.format(new Date());
        String prefix = isSend ? "[TX]" : "[RX]";

        if (displayAsHex) {
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < length; i++) {
                hex.append(String.format("%02X", data[i] & 0xFF));
                if (i < length - 1) {
                    hex.append(" ");
                }
            }
            return String.format("%s %s %s (%d bytes): %s", timestamp, prefix, direction, length, hex.toString());
        } else {
            // 尝试作为文本解码
            String text;
            try {
                text = new String(data, 0, length, "UTF-8");
                // 替换不可打印字符
                text = text.replaceAll("[^\\x20-\\x7E]", ".");
            } catch (Exception e) {
                text = "<binary data>";
            }
            return String.format("%s %s %s (%d bytes): %s", timestamp, prefix, direction, length, text);
        }
    }

    /**
     * 添加数据到缓冲区
     */
    private void addToBuffer(String logEntry, int dataSize) {
        synchronized (dataBuffer) {
            lineCounter++;
            String numberedEntry = String.format("[%d] %s", lineCounter, logEntry);
            dataBuffer.add(numberedEntry);
            currentBufferSize += dataSize + logEntry.length();

            // 限制缓冲区大小
            while (currentBufferSize > maxBufferSize && !dataBuffer.isEmpty()) {
                String removed = dataBuffer.remove(0);
                currentBufferSize -= removed.length();
            }

            // 限制行数
            while (dataBuffer.size() > 100000) {
                String removed = dataBuffer.remove(0);
                currentBufferSize -= removed.length();
            }
        }

        // 更新GUI
        SwingUtilities.invokeLater(() -> {
            if (logArea != null) {
                logArea.append(logEntry + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            }
            updateStatusLabel();
        });
    }

    /**
     * 创建GUI窗口
     */
    private void createGUI() {
        guiWindow = new JFrame("USB工具 - 实例 " + instanceId);
        guiWindow.setSize(900, 600);
        guiWindow.setLocationRelativeTo(null);
        guiWindow.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        // Set window icon
        LogoLoader.applyTo(guiWindow, "USB工具");

        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 顶部控制面板
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        controlPanel.setBorder(BorderFactory.createTitledBorder("设备控制"));

        // 设备选择
        JLabel deviceLabel = new JLabel("设备:");
        deviceCombo = new JComboBox<>();
        deviceCombo.setPreferredSize(new Dimension(300, 25));
        deviceCombo.setEditable(false);

        JButton scanButton = new JButton("扫描设备");
        scanButton.addActionListener(e -> scanDevices());

        // 连接状态
        connectStatusLabel = new JLabel("● 未连接");
        connectStatusLabel.setForeground(Color.RED);

        openCloseButton = new JButton("打开设备");
        openCloseButton.setEnabled(false);
        openCloseButton.addActionListener(e -> {
            if (isDeviceOpen) {
                closeDevice();
            } else {
                // 从下拉框获取选中的设备
                String selected = (String) deviceCombo.getSelectedItem();
                if (selected != null && !selected.isEmpty()) {
                    // 解析VID和PID
                    // 格式: "设备名 [VID:PID] - 类型"
                    int startIdx = selected.indexOf('[');
                    int endIdx = selected.indexOf(']');
                    if (startIdx >= 0 && endIdx > startIdx) {
                        String vidPid = selected.substring(startIdx + 1, endIdx);
                        String[] parts = vidPid.split(":");
                        if (parts.length == 2) {
                            int vid = Integer.parseInt(parts[0], 16);
                            int pid = Integer.parseInt(parts[1], 16);
                            openDevice(vid, pid, "HID");
                        }
                    }
                }
            }
        });

        controlPanel.add(deviceLabel);
        controlPanel.add(deviceCombo);
        controlPanel.add(scanButton);
        controlPanel.add(openCloseButton);
        controlPanel.add(connectStatusLabel);

        // 日志区域
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("通信日志"));

        // 底部状态栏
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusLabel = new JLabel("就绪 | 缓冲区: 0 / " + (maxBufferSize / 1024) + " KB | 发送: 0 B | 接收: 0 B");
        statusPanel.add(statusLabel, BorderLayout.WEST);

        // 组装面板
        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(statusPanel, BorderLayout.SOUTH);

        // 作者信息栏（作者 + 版本号 + 联系作者链接）
        mainPanel.add(AboutPanel.create(guiWindow), BorderLayout.PAGE_END);

        guiWindow.add(mainPanel);

        // 关闭事件
        guiWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stop();
            }
        });

        guiWindow.setVisible(true);
        log("USB工具GUI已启动");
    }

    /**
     * 更新设备下拉框
     */
    private void updateDeviceCombo(List<Map<String, Object>> devices) {
        SwingUtilities.invokeLater(() -> {
            if (deviceCombo != null) {
                deviceCombo.removeAllItems();
                for (Map<String, Object> device : devices) {
                    deviceCombo.addItem((String) device.get("displayName"));
                }
                openCloseButton.setEnabled(!devices.isEmpty() && !isDeviceOpen);
            }
        });
    }

    /**
     * 更新连接状态
     */
    private void updateConnectStatus(boolean connected) {
        SwingUtilities.invokeLater(() -> {
            if (connectStatusLabel != null) {
                if (connected) {
                    connectStatusLabel.setText("● 已连接");
                    connectStatusLabel.setForeground(new Color(0, 150, 0));
                    openCloseButton.setText("关闭设备");
                } else {
                    connectStatusLabel.setText("● 未连接");
                    connectStatusLabel.setForeground(Color.RED);
                    openCloseButton.setText("打开设备");
                }
            }
            if (deviceCombo != null) {
                deviceCombo.setEnabled(!connected);
            }
        });
    }

    /**
     * 更新状态标签
     */
    private void updateStatusLabel() {
        SwingUtilities.invokeLater(() -> {
            if (statusLabel != null) {
                statusLabel.setText(String.format("就绪 | 缓冲区: %d / %d KB | 发送: %s | 接收: %s",
                    currentBufferSize / 1024,
                    maxBufferSize / 1024,
                    formatBytes(totalSentBytes),
                    formatBytes(totalReceivedBytes)));
            }
        });
    }

    /**
     * 格式化字节大小
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        }
    }

    /**
     * 记录日志
     */
    private void log(String message) {
        String timestamp = timeFormat.format(new Date());
        String logLine = "[" + timestamp + "] [INFO] " + message;
        System.out.println(logLine);

        if (logArea != null) {
            SwingUtilities.invokeLater(() -> {
                logArea.append(logLine + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            });
        }
    }
}

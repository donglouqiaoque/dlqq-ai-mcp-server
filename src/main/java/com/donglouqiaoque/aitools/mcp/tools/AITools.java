package com.donglouqiaoque.aitools.mcp.tools;

import com.donglouqiaoque.aitools.ai.AIService;
import com.donglouqiaoque.aitools.config.ConfigManager;
import com.donglouqiaoque.aitools.mcp.protocol.McpSchema;
import com.donglouqiaoque.aitools.mcp.protocol.McpSchema.*;
import com.donglouqiaoque.aitools.mcp.server.McpServer;
import com.donglouqiaoque.aitools.utils.FileUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * AI工具集 - 注册到MCP服务器的工具
 */
public class AITools {

    private final AIService aiService;
    private final ConfigManager configManager;

    public AITools() {
        this.aiService = new AIService();
        this.configManager = ConfigManager.getInstance();
    }

    /**
     * 注册所有AI工具到MCP服务器
     */
    public void registerTools(McpServer server) {
        registerChatTool(server);
        registerFileReadTool(server);
        registerFileWriteTool(server);
        registerFileSummarizeTool(server);
        registerConfigGetTool(server);
        registerConfigSetTool(server);
        // 注册GUI对话框工具
        registerDialogInputTool(server);
        registerDialogFileSelectTool(server);
        registerDialogConfirmTool(server);
        registerDialogMessageTool(server);
        // 注册UART串口工具
        registerUartListPortsTool(server);
        registerUartOpenPortTool(server);
        registerUartClosePortTool(server);
        registerUartSendDataTool(server);
        registerUartReceiveDataTool(server);
        registerUartGetLogsTool(server);
        registerUartClearLogsTool(server);
        registerUartSendATTool(server);
        registerUartFindCH340Tool(server);
    }

    /**
     * 注册AI聊天工具
     */
    private void registerChatTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        
        Map<String, Object> messageProp = new HashMap<>();
        messageProp.put("type", "string");
        messageProp.put("description", "要发送给AI的消息");
        properties.put("message", messageProp);
        
        Map<String, Object> modelProp = new HashMap<>();
        modelProp.put("type", "string");
        modelProp.put("description", "使用的AI模型（可选，默认使用配置中的模型）");
        properties.put("model", modelProp);
        
        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("message"));
        
        server.addTool("ai_chat", 
            "与AI进行对话，获取AI的回复", 
            inputSchema,
            arguments -> {
                String message = (String) arguments.get("message");
                String model = (String) arguments.get("model");
                
                if (message == null || message.trim().isEmpty()) {
                    return errorResult("消息不能为空");
                }
                
                try {
                    if (!configManager.hasApiKey()) {
                        return errorResult("未配置API密钥，请先使用 config_set 工具设置API密钥");
                    }
                    
                    String response;
                    if (model != null && !model.isEmpty()) {
                        List<com.donglouqiaoque.aitools.ai.ChatMessage> messages = new ArrayList<>();
                        messages.add(com.donglouqiaoque.aitools.ai.ChatMessage.user(message));
                        response = aiService.chat(messages, model);
                    } else {
                        response = aiService.chat(message);
                    }
                    
                    return textResult(response);
                } catch (IOException e) {
                    return errorResult("请求失败: " + e.getMessage());
                } catch (Exception e) {
                    return errorResult("错误: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 注册文件读取工具
     */
    private void registerFileReadTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        
        Map<String, Object> pathProp = new HashMap<>();
        pathProp.put("type", "string");
        pathProp.put("description", "文件路径");
        properties.put("path", pathProp);
        
        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("path"));
        
        server.addTool("file_read",
            "读取指定路径的文件内容",
            inputSchema,
            arguments -> {
                String path = (String) arguments.get("path");

                if (path == null || path.trim().isEmpty()) {
                    return errorResult("文件路径不能为空");
                }

                try {
                    String content = FileUtils.readFile(path);
                    return textResult(content);
                } catch (IOException e) {
                    return errorResult("读取文件失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 注册文件写入工具
     */
    private void registerFileWriteTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> pathProp = new HashMap<>();
        pathProp.put("type", "string");
        pathProp.put("description", "文件路径（相对或绝对路径）");
        properties.put("path", pathProp);

        Map<String, Object> contentProp = new HashMap<>();
        contentProp.put("type", "string");
        contentProp.put("description", "要写入的文件内容");
        properties.put("content", contentProp);

        Map<String, Object> appendProp = new HashMap<>();
        appendProp.put("type", "boolean");
        appendProp.put("description", "是否追加写入（默认false，即覆盖写入）");
        properties.put("append", appendProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("path", "content"));

        server.addTool("file_write",
            "写入内容到指定文件，自动创建目录",
            inputSchema,
            arguments -> {
                String path = (String) arguments.get("path");
                String content = (String) arguments.get("content");
                Boolean append = (Boolean) arguments.get("append");

                if (path == null || path.trim().isEmpty()) {
                    return errorResult("文件路径不能为空");
                }
                if (content == null) {
                    return errorResult("文件内容不能为null");
                }

                try {
                    // 解析相对路径
                    java.nio.file.Path filePath = java.nio.file.Paths.get(path);
                    if (!filePath.isAbsolute()) {
                        // 如果是相对路径，基于当前工作目录
                        filePath = java.nio.file.Paths.get(System.getProperty("user.dir"), path);
                    }

                    // 确保父目录存在
                    java.nio.file.Path parent = filePath.getParent();
                    if (parent != null && !java.nio.file.Files.exists(parent)) {
                        java.nio.file.Files.createDirectories(parent);
                    }

                    // 写入文件
                    if (Boolean.TRUE.equals(append)) {
                        FileUtils.appendFile(filePath.toString(), content);
                    } else {
                        FileUtils.writeFile(filePath.toString(), content);
                    }

                    return textResult("文件写入成功: " + filePath);
                } catch (IOException e) {
                    return errorResult("写入文件失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 注册文件总结工具
     */
    private void registerFileSummarizeTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        
        Map<String, Object> pathProp = new HashMap<>();
        pathProp.put("type", "string");
        pathProp.put("description", "文件路径");
        properties.put("path", pathProp);
        
        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("path"));
        
        server.addTool("file_summarize", 
            "读取文件并使用AI生成内容摘要", 
            inputSchema,
            arguments -> {
                String path = (String) arguments.get("path");
                
                if (path == null || path.trim().isEmpty()) {
                    return errorResult("文件路径不能为空");
                }
                
                try {
                    if (!configManager.hasApiKey()) {
                        return errorResult("未配置API密钥，请先使用 config_set 工具设置API密钥");
                    }
                    
                    String content = FileUtils.readFile(path);
                    // 限制内容长度
                    content = FileUtils.truncate(content, 8000);
                    
                    String prompt = "请总结以下文件的主要内容，用简洁的语言描述:\n\n" + content;
                    String response = aiService.chat(prompt);
                    
                    return textResult(response);
                } catch (IOException e) {
                    return errorResult("处理失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 注册配置获取工具
     */
    private void registerConfigGetTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        
        Map<String, Object> keyProp = new HashMap<>();
        keyProp.put("type", "string");
        keyProp.put("description", "配置项名称（api.key, api.url, api.model）");
        keyProp.put("enum", Arrays.asList("api.key", "api.url", "api.model", "all"));
        properties.put("key", keyProp);
        
        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("key"));
        
        server.addTool("config_get", 
            "获取AI工具配置信息", 
            inputSchema,
            arguments -> {
                String key = (String) arguments.get("key");
                
                if (key == null || key.trim().isEmpty()) {
                    return errorResult("配置项名称不能为空");
                }
                
                StringBuilder result = new StringBuilder();
                
                switch (key) {
                    case "api.key":
                        result.append("API密钥: ").append(configManager.hasApiKey() ? "已设置" : "未设置");
                        break;
                    case "api.url":
                        result.append("API地址: ").append(configManager.getApiUrl());
                        break;
                    case "api.model":
                        result.append("默认模型: ").append(configManager.getModel());
                        break;
                    case "all":
                        result.append("当前配置:\n");
                        result.append("API密钥: ").append(configManager.hasApiKey() ? "已设置" : "未设置").append("\n");
                        result.append("API地址: ").append(configManager.getApiUrl()).append("\n");
                        result.append("默认模型: ").append(configManager.getModel());
                        break;
                    default:
                        String value = configManager.getProperty(key, "");
                        result.append(key).append(" = ").append(value);
                }
                
                return textResult(result.toString());
            }
        );
    }

    /**
     * 注册配置设置工具
     */
    private void registerConfigSetTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        
        Map<String, Object> keyProp = new HashMap<>();
        keyProp.put("type", "string");
        keyProp.put("description", "配置项名称");
        keyProp.put("enum", Arrays.asList("api.key", "api.url", "api.model"));
        properties.put("key", keyProp);
        
        Map<String, Object> valueProp = new HashMap<>();
        valueProp.put("type", "string");
        valueProp.put("description", "配置项值");
        properties.put("value", valueProp);
        
        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("key", "value"));
        
        server.addTool("config_set", 
            "设置AI工具配置", 
            inputSchema,
            arguments -> {
                String key = (String) arguments.get("key");
                String value = (String) arguments.get("value");
                
                if (key == null || key.trim().isEmpty()) {
                    return errorResult("配置项名称不能为空");
                }
                if (value == null) {
                    return errorResult("配置项值不能为null");
                }
                
                switch (key) {
                    case "api.key":
                        configManager.setApiKey(value);
                        return textResult("API密钥已设置");
                    case "api.url":
                        configManager.setApiUrl(value);
                        return textResult("API地址已设置为: " + value);
                    case "api.model":
                        configManager.setModel(value);
                        return textResult("默认模型已设置为: " + value);
                    default:
                        configManager.setProperty(key, value);
                        return textResult("配置已设置: " + key + " = " + value);
                }
            }
        );
    }

    /**
     * 创建文本结果
     */
    private CallToolResult textResult(String text) {
        CallToolResult result = new CallToolResult();
        result.setError(false);
        List<ToolContent> content = new ArrayList<>();
        content.add(ToolContent.text(text));
        result.setContent(content);
        return result;
    }

    /**
     * 创建错误结果
     */
    private CallToolResult errorResult(String error) {
        CallToolResult result = new CallToolResult();
        result.setError(true);
        List<ToolContent> content = new ArrayList<>();
        content.add(ToolContent.error(error));
        result.setContent(content);
        return result;
    }

    // ==================== GUI对话框工具 ====================

    /**
     * 注册对话框输入工具
     */
    private void registerDialogInputTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> titleProp = new HashMap<>();
        titleProp.put("type", "string");
        titleProp.put("description", "对话框标题");
        properties.put("title", titleProp);

        Map<String, Object> messageProp = new HashMap<>();
        messageProp.put("type", "string");
        messageProp.put("description", "提示消息");
        properties.put("message", messageProp);

        Map<String, Object> showFileProp = new HashMap<>();
        showFileProp.put("type", "boolean");
        showFileProp.put("description", "是否显示文件选择按钮");
        properties.put("show_file_select", showFileProp);

        Map<String, Object> extensionsProp = new HashMap<>();
        extensionsProp.put("type", "array");
        extensionsProp.put("description", "允许的文件扩展名列表（如 [\"txt\", \"java\"]）");
        properties.put("file_extensions", extensionsProp);

        Map<String, Object> timeoutProp = new HashMap<>();
        timeoutProp.put("type", "integer");
        timeoutProp.put("description", "超时时间（秒），0表示无超时");
        properties.put("timeout", timeoutProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("title", "message"));

        server.addTool("dialog_input",
            "弹出一个对话框，允许用户输入文本并可选择文件。返回用户输入的文本和选中的文件内容。",
            inputSchema,
            arguments -> {
                String title = (String) arguments.get("title");
                String message = (String) arguments.get("message");
                Boolean showFileSelect = (Boolean) arguments.getOrDefault("show_file_select", false);
                @SuppressWarnings("unchecked")
                List<String> extensions = (List<String>) arguments.get("file_extensions");
                Integer timeout = (Integer) arguments.getOrDefault("timeout", 0);

                if (title == null || title.trim().isEmpty()) {
                    return errorResult("对话框标题不能为空");
                }
                if (message == null) {
                    message = "";
                }

                try {
                    String[] extArray = extensions != null ?
                            extensions.toArray(new String[0]) : null;

                    DialogTools.DialogResult result = DialogTools.showInputDialog(
                            title, message, showFileSelect, extArray, timeout);

                    if (result == null) {
                        return errorResult("对话框超时或用户未响应");
                    }

                    if (!result.isConfirmed()) {
                        return textResult("用户取消了对话框");
                    }

                    StringBuilder sb = new StringBuilder();
                    sb.append("用户输入的文本:\n");
                    sb.append(result.getTextInput() != null ? result.getTextInput() : "");

                    if (result.getFilePath() != null && !result.getFilePath().isEmpty()) {
                        sb.append("\n\n选中的文件路径: ").append(result.getFilePath());
                        if (result.getFileContent() != null && !result.getFileContent().isEmpty()) {
                            sb.append("\n文件内容:\n");
                            sb.append(FileUtils.truncate(result.getFileContent(), 5000));
                        }
                    }

                    return textResult(sb.toString());
                } catch (Exception e) {
                    return errorResult("对话框执行失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 注册文件选择对话框工具
     */
    private void registerDialogFileSelectTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> titleProp = new HashMap<>();
        titleProp.put("type", "string");
        titleProp.put("description", "对话框标题");
        properties.put("title", titleProp);

        Map<String, Object> extensionsProp = new HashMap<>();
        extensionsProp.put("type", "array");
        extensionsProp.put("description", "允许的文件扩展名列表");
        properties.put("extensions", extensionsProp);

        Map<String, Object> multipleProp = new HashMap<>();
        multipleProp.put("type", "boolean");
        multipleProp.put("description", "是否允许多选");
        properties.put("allow_multiple", multipleProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("title"));

        server.addTool("dialog_file_select",
            "弹出文件选择对话框，返回选中的文件路径列表",
            inputSchema,
            arguments -> {
                String title = (String) arguments.get("title");
                @SuppressWarnings("unchecked")
                List<String> extensions = (List<String>) arguments.get("extensions");
                Boolean multiple = (Boolean) arguments.getOrDefault("allow_multiple", false);

                if (title == null || title.trim().isEmpty()) {
                    title = "选择文件";
                }

                try {
                    String[] extArray = extensions != null ?
                            extensions.toArray(new String[0]) : null;

                    String[] paths = DialogTools.showFileChooser(title, extArray, multiple);

                    if (paths.length == 0) {
                        return textResult("用户未选择任何文件");
                    }

                    StringBuilder sb = new StringBuilder();
                    sb.append("选中的文件:\n");
                    for (int i = 0; i < paths.length; i++) {
                        sb.append(i + 1).append(". ").append(paths[i]).append("\n");
                    }
                    return textResult(sb.toString());
                } catch (Exception e) {
                    return errorResult("文件选择失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 注册确认对话框工具
     */
    private void registerDialogConfirmTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> titleProp = new HashMap<>();
        titleProp.put("type", "string");
        titleProp.put("description", "对话框标题");
        properties.put("title", titleProp);

        Map<String, Object> messageProp = new HashMap<>();
        messageProp.put("type", "string");
        messageProp.put("description", "确认消息内容");
        properties.put("message", messageProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("title", "message"));

        server.addTool("dialog_confirm",
            "弹出确认对话框，返回用户是否点击确定",
            inputSchema,
            arguments -> {
                String title = (String) arguments.get("title");
                String message = (String) arguments.get("message");

                if (title == null || title.trim().isEmpty()) {
                    return errorResult("标题不能为空");
                }
                if (message == null) {
                    message = "";
                }

                try {
                    boolean confirmed = DialogTools.showConfirmDialog(title, message);
                    return textResult(confirmed ? "用户点击了确定" : "用户点击了取消");
                } catch (Exception e) {
                    return errorResult("确认对话框执行失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 注册消息提示对话框工具
     */
    private void registerDialogMessageTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> titleProp = new HashMap<>();
        titleProp.put("type", "string");
        titleProp.put("description", "对话框标题");
        properties.put("title", titleProp);

        Map<String, Object> messageProp = new HashMap<>();
        messageProp.put("type", "string");
        messageProp.put("description", "消息内容");
        properties.put("message", messageProp);

        Map<String, Object> typeProp = new HashMap<>();
        typeProp.put("type", "string");
        typeProp.put("description", "消息类型");
        typeProp.put("enum", Arrays.asList("info", "warning", "error"));
        properties.put("type", typeProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("title", "message"));

        server.addTool("dialog_message",
            "弹出消息提示对话框（非阻塞，异步显示）",
            inputSchema,
            arguments -> {
                String title = (String) arguments.get("title");
                String message = (String) arguments.get("message");
                String type = (String) arguments.getOrDefault("type", "info");

                if (title == null || title.trim().isEmpty()) {
                    return errorResult("标题不能为空");
                }
                if (message == null) {
                    message = "";
                }

                try {
                    DialogTools.showMessageDialog(title, message, type);
                    return textResult("消息对话框已显示");
                } catch (Exception e) {
                    return errorResult("消息对话框显示失败: " + e.getMessage());
                }
            }
        );
    }

    // ==================== UART串口工具 ====================

    /**
     * 注册列出可用串口工具
     */
    private void registerUartListPortsTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        ToolInputSchema inputSchema = new ToolInputSchema(properties, new ArrayList<>());

        server.addTool("uart_list_ports",
            "列出系统所有可用串口，包括CH340等USB转串口设备",
            inputSchema,
            arguments -> {
                try {
                    List<UartTools.PortInfo> ports = UartTools.getAvailablePorts();
                    if (ports.isEmpty()) {
                        return textResult("未找到可用串口");
                    }

                    StringBuilder sb = new StringBuilder();
                    sb.append("可用串口列表:\n\n");
                    for (int i = 0; i < ports.size(); i++) {
                        UartTools.PortInfo port = ports.get(i);
                        sb.append(i + 1).append(". ").append(port.getPortName()).append("\n");
                        sb.append("   描述: ").append(port.getDescriptivePortName()).append("\n");
                        sb.append("   位置: ").append(port.getPortLocation()).append("\n");
                        sb.append("   VID: ").append(port.getVendorId()).append(", PID: ").append(port.getProductId()).append("\n");
                        sb.append("   状态: ").append(port.isOpen() ? "已打开" : "可用").append("\n\n");
                    }
                    return textResult(sb.toString());
                } catch (Exception e) {
                    return errorResult("获取串口列表失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 注册打开串口工具
     */
    private void registerUartOpenPortTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "string");
        portProp.put("description", "串口名称（如 COM3, /dev/ttyUSB0）");
        properties.put("port", portProp);

        Map<String, Object> baudProp = new HashMap<>();
        baudProp.put("type", "integer");
        baudProp.put("description", "波特率（默认 115200）");
        properties.put("baud_rate", baudProp);

        Map<String, Object> dataBitsProp = new HashMap<>();
        dataBitsProp.put("type", "integer");
        dataBitsProp.put("description", "数据位（5,6,7,8，默认8）");
        properties.put("data_bits", dataBitsProp);

        Map<String, Object> stopBitsProp = new HashMap<>();
        stopBitsProp.put("type", "number");
        stopBitsProp.put("description", "停止位（1, 1.5, 2，默认1）");
        properties.put("stop_bits", stopBitsProp);

        Map<String, Object> parityProp = new HashMap<>();
        parityProp.put("type", "string");
        parityProp.put("description", "校验位（NONE, EVEN, ODD, MARK, SPACE，默认NONE）");
        parityProp.put("enum", Arrays.asList("NONE", "EVEN", "ODD", "MARK", "SPACE"));
        properties.put("parity", parityProp);

        Map<String, Object> maxLogProp = new HashMap<>();
        maxLogProp.put("type", "integer");
        maxLogProp.put("description", "最大日志条数（默认1000）");
        properties.put("max_log_size", maxLogProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port"));

        server.addTool("uart_open_port",
            "打开指定串口连接（支持CH340等USB转串口设备）",
            inputSchema,
            arguments -> {
                String portName = (String) arguments.get("port");
                Integer baudRate = (Integer) arguments.getOrDefault("baud_rate", 115200);
                Integer dataBits = (Integer) arguments.getOrDefault("data_bits", 8);
                Number stopBitsNum = (Number) arguments.getOrDefault("stop_bits", 1.0);
                double stopBits = stopBitsNum.doubleValue();
                String parity = (String) arguments.getOrDefault("parity", "NONE");
                Integer maxLogSize = (Integer) arguments.getOrDefault("max_log_size", 1000);

                if (portName == null || portName.trim().isEmpty()) {
                    return errorResult("串口名称不能为空");
                }

                try {
                    boolean success = UartTools.openPort(portName, baudRate, dataBits,
                            stopBits, parity, maxLogSize);
                    if (success) {
                        return textResult("串口 " + portName + " 已成功打开\n" +
                                "波特率: " + baudRate + ", 数据位: " + dataBits +
                                ", 停止位: " + stopBits + ", 校验位: " + parity);
                    } else {
                        return errorResult("无法打开串口 " + portName + "，请检查设备是否连接");
                    }
                } catch (Exception e) {
                    return errorResult("打开串口失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 注册关闭串口工具
     */
    private void registerUartClosePortTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "string");
        portProp.put("description", "串口名称");
        properties.put("port", portProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port"));

        server.addTool("uart_close_port",
            "关闭指定串口连接",
            inputSchema,
            arguments -> {
                String portName = (String) arguments.get("port");

                if (portName == null || portName.trim().isEmpty()) {
                    return errorResult("串口名称不能为空");
                }

                try {
                    boolean success = UartTools.closePort(portName);
                    if (success) {
                        return textResult("串口 " + portName + " 已关闭");
                    } else {
                        return errorResult("串口 " + portName + " 未打开或关闭失败");
                    }
                } catch (Exception e) {
                    return errorResult("关闭串口失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 注册发送数据到串口工具
     */
    private void registerUartSendDataTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "string");
        portProp.put("description", "串口名称");
        properties.put("port", portProp);

        Map<String, Object> dataProp = new HashMap<>();
        dataProp.put("type", "string");
        dataProp.put("description", "要发送的数据");
        properties.put("data", dataProp);

        Map<String, Object> newlineProp = new HashMap<>();
        newlineProp.put("type", "boolean");
        newlineProp.put("description", "是否添加换行符（默认true）");
        properties.put("add_newline", newlineProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port", "data"));

        server.addTool("uart_send_data",
            "向串口发送数据",
            inputSchema,
            arguments -> {
                String portName = (String) arguments.get("port");
                String data = (String) arguments.get("data");
                Boolean addNewline = (Boolean) arguments.getOrDefault("add_newline", true);

                if (portName == null || portName.trim().isEmpty()) {
                    return errorResult("串口名称不能为空");
                }
                if (data == null) {
                    return errorResult("发送数据不能为null");
                }

                try {
                    boolean success = UartTools.sendData(portName, data, addNewline);
                    if (success) {
                        return textResult("数据已发送到 " + portName + ": " + data);
                    } else {
                        return errorResult("发送失败，串口 " + portName + " 未打开");
                    }
                } catch (Exception e) {
                    return errorResult("发送数据失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 注册接收串口数据工具
     */
    private void registerUartReceiveDataTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "string");
        portProp.put("description", "串口名称");
        properties.put("port", portProp);

        Map<String, Object> timeoutProp = new HashMap<>();
        timeoutProp.put("type", "integer");
        timeoutProp.put("description", "等待超时时间（毫秒，默认0表示立即返回）");
        properties.put("timeout", timeoutProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port"));

        server.addTool("uart_receive_data",
            "从串口接收数据（非阻塞）",
            inputSchema,
            arguments -> {
                String portName = (String) arguments.get("port");
                Integer timeout = (Integer) arguments.getOrDefault("timeout", 0);

                if (portName == null || portName.trim().isEmpty()) {
                    return errorResult("串口名称不能为空");
                }

                try {
                    String data;
                    if (timeout > 0) {
                        data = UartTools.receiveDataWithTimeout(portName, timeout);
                    } else {
                        data = UartTools.receiveData(portName);
                    }

                    if (data == null) {
                        return errorResult("串口 " + portName + " 未打开");
                    }
                    if (data.isEmpty()) {
                        return textResult("未接收到新数据");
                    }
                    return textResult("接收到的数据:\n" + data);
                } catch (Exception e) {
                    return errorResult("接收数据失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 注册获取串口日志工具
     */
    private void registerUartGetLogsTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "string");
        portProp.put("description", "串口名称");
        properties.put("port", portProp);

        Map<String, Object> countProp = new HashMap<>();
        countProp.put("type", "integer");
        countProp.put("description", "获取最近的日志条数（0表示全部）");
        properties.put("count", countProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port"));

        server.addTool("uart_get_logs",
            "获取串口通信日志（TX/RX记录），用于AI分析串口通信情况",
            inputSchema,
            arguments -> {
                String portName = (String) arguments.get("port");
                Integer count = (Integer) arguments.getOrDefault("count", 100);

                if (portName == null || portName.trim().isEmpty()) {
                    return errorResult("串口名称不能为空");
                }

                try {
                    String logs = UartTools.getLogs(portName, count);
                    return textResult(logs);
                } catch (Exception e) {
                    return errorResult("获取日志失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 注册清除串口日志工具
     */
    private void registerUartClearLogsTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "string");
        portProp.put("description", "串口名称");
        properties.put("port", portProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port"));

        server.addTool("uart_clear_logs",
            "清除串口通信日志",
            inputSchema,
            arguments -> {
                String portName = (String) arguments.get("port");

                if (portName == null || portName.trim().isEmpty()) {
                    return errorResult("串口名称不能为空");
                }

                try {
                    boolean success = UartTools.clearLogs(portName);
                    if (success) {
                        return textResult("串口 " + portName + " 的日志已清除");
                    } else {
                        return errorResult("串口 " + portName + " 未打开");
                    }
                } catch (Exception e) {
                    return errorResult("清除日志失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 注册发送AT指令工具
     */
    private void registerUartSendATTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "string");
        portProp.put("description", "串口名称");
        properties.put("port", portProp);

        Map<String, Object> commandProp = new HashMap<>();
        commandProp.put("type", "string");
        commandProp.put("description", "AT指令（如 AT, AT+VERSION）");
        properties.put("command", commandProp);

        Map<String, Object> timeoutProp = new HashMap<>();
        timeoutProp.put("type", "integer");
        timeoutProp.put("description", "等待响应超时（毫秒，默认5000）");
        properties.put("timeout", timeoutProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port", "command"));

        server.addTool("uart_send_at",
            "发送AT指令并等待响应（适用于WiFi模块、蓝牙模块等）",
            inputSchema,
            arguments -> {
                String portName = (String) arguments.get("port");
                String command = (String) arguments.get("command");
                Integer timeout = (Integer) arguments.getOrDefault("timeout", 5000);

                if (portName == null || portName.trim().isEmpty()) {
                    return errorResult("串口名称不能为空");
                }
                if (command == null || command.trim().isEmpty()) {
                    return errorResult("AT指令不能为空");
                }

                try {
                    String response = UartTools.sendATCommand(portName, command, timeout);
                    return textResult("AT指令响应:\n" + response);
                } catch (Exception e) {
                    return errorResult("发送AT指令失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 注册查找CH340设备工具
     */
    private void registerUartFindCH340Tool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        ToolInputSchema inputSchema = new ToolInputSchema(properties, new ArrayList<>());

        server.addTool("uart_find_ch340",
            "查找系统中的CH340 USB转串口设备",
            inputSchema,
            arguments -> {
                try {
                    List<UartTools.PortInfo> ports = UartTools.findCH340Devices();
                    if (ports.isEmpty()) {
                        return textResult("未找到CH340设备");
                    }

                    StringBuilder sb = new StringBuilder();
                    sb.append("找到的CH340设备:\n\n");
                    for (int i = 0; i < ports.size(); i++) {
                        UartTools.PortInfo port = ports.get(i);
                        sb.append(i + 1).append(". ").append(port.getPortName()).append("\n");
                        sb.append("   描述: ").append(port.getDescriptivePortName()).append("\n");
                        sb.append("   位置: ").append(port.getPortLocation()).append("\n");
                        sb.append("   VID: ").append(port.getVendorId()).append(", PID: ").append(port.getProductId()).append("\n");
                        sb.append("   状态: ").append(port.isOpen() ? "已打开" : "可用").append("\n\n");
                    }
                    return textResult(sb.toString());
                } catch (Exception e) {
                    return errorResult("查找CH340设备失败: " + e.getMessage());
                }
            }
        );
    }
}


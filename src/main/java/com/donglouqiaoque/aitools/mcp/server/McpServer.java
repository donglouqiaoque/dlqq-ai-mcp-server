package com.donglouqiaoque.aitools.mcp.server;

import com.donglouqiaoque.aitools.mcp.protocol.JsonRpcMessage;
import com.donglouqiaoque.aitools.mcp.protocol.McpSchema;
import com.donglouqiaoque.aitools.mcp.protocol.McpSchema.*;
import com.donglouqiaoque.aitools.mcp.transport.McpTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * MCP服务器核心类
 */
public class McpServer {

    private static final Logger logger = LoggerFactory.getLogger(McpServer.class);

    private final String serverName;
    private final String serverVersion;
    private final McpTransport transport;
    private final Map<String, ToolHandler> toolHandlers;
    private final Map<String, ResourceHandler> resourceHandlers;
    private final Map<String, PromptHandler> promptHandlers;
    private final Capabilities capabilities;

    private boolean initialized = false;
    private String clientProtocolVersion;
    private int toolsListRequestCount = 0;  // 记录tools/list请求次数

    public McpServer(String serverName, String serverVersion, McpTransport transport) {
        this.serverName = serverName;
        this.serverVersion = serverVersion;
        this.transport = transport;
        this.toolHandlers = new ConcurrentHashMap<>();
        this.resourceHandlers = new ConcurrentHashMap<>();
        this.promptHandlers = new ConcurrentHashMap<>();
        this.capabilities = new Capabilities();

        // 注册默认的处理程序
        registerDefaultHandlers();
    }

    /**
     * 注册默认的处理程序
     */
    private void registerDefaultHandlers() {
        // 初始化
        this.transport.setMessageHandler(this::handleMessage);
    }

    /**
     * 启动服务器
     */
    public void start() throws IOException {
        this.transport.start();
    }

    /**
     * 停止服务器
     */
    public void stop() throws IOException {
        this.transport.stop();
    }

    /**
     * 处理传入的消息
     */
    private void handleMessage(JsonRpcMessage.Request request) {
        String method = request.getMethod();
        logger.info("[McpServer.handleMessage] 收到请求: method={}, id={}, isNotification={}",
                method, request.getId(), request.isNotification());

        try {
            // 检查是否是通知（notification）- id为null
            boolean isNotification = request.isNotification();

            switch (method) {
                case "initialize":
                    logger.info("[McpServer.handleMessage] 处理 initialize 请求");
                    handleInitialize(request);
                    break;
                case "initialized":
                case "notifications/initialized":
                    // 客户端确认初始化完成 - 这是通知，不返回响应
                    logger.info("[McpServer.handleMessage] 处理 initialized 通知");
                    handleInitialized(request);
                    break;
                case "tools/list":
                    logger.info("[McpServer.handleMessage] 处理 tools/list 请求");
                    handleToolsList(request);
                    break;
                case "tools/call":
                    logger.info("[McpServer.handleMessage] 处理 tools/call 请求");
                    handleToolsCall(request);
                    break;
                case "resources/list":
                    logger.info("[McpServer.handleMessage] 处理 resources/list 请求");
                    handleResourcesList(request);
                    break;
                case "prompts/list":
                    logger.info("[McpServer.handleMessage] 处理 prompts/list 请求");
                    handlePromptsList(request);
                    break;
                case "prompts/get":
                    logger.info("[McpServer.handleMessage] 处理 prompts/get 请求");
                    handlePromptsGet(request);
                    break;
                default:
                    // 对于通知，不发送错误响应
                    if (!isNotification) {
                        logger.warn("[McpServer.handleMessage] 未知方法: {}", method);
                        sendErrorResponse(request.getId(), JsonRpcMessage.ERROR_METHOD_NOT_FOUND,
                                "Unknown method: " + method);
                    } else {
                        logger.warn("[McpServer.handleMessage] 忽略未知通知: {}", method);
                    }
            }
        } catch (Exception e) {
            logger.error("[McpServer.handleMessage] 处理请求时出错: method={}, error={}", method, e.getMessage(), e);
            try {
                // 对于通知，不发送错误响应
                if (request.getId() != null) {
                    sendErrorResponse(request.getId(), JsonRpcMessage.ERROR_INTERNAL_ERROR,
                            "Internal error: " + e.getMessage());
                } else {
                    logger.error("[McpServer.handleMessage] 处理通知时出错: {}", e.getMessage(), e);
                }
            } catch (IOException ioException) {
                logger.error("[McpServer.handleMessage] 发送错误响应失败: {}", ioException.getMessage(), ioException);
            }
        }
    }

    /**
     * 处理初始化请求
     */
    @SuppressWarnings("unchecked")
    private void handleInitialize(JsonRpcMessage.Request request) throws IOException {
        Map<String, Object> params = request.getParams();
        logger.info("[McpServer.handleInitialize] 开始处理，params={}", params);

        if (params == null) {
            logger.error("[McpServer.handleInitialize] 缺少params参数");
            sendErrorResponse(request.getId(), JsonRpcMessage.ERROR_INVALID_PARAMS, "Missing params");
            return;
        }

        this.clientProtocolVersion = (String) params.get("protocolVersion");
        this.initialized = true;

        logger.info("[McpServer.handleInitialize] 客户端协议版本: {}", this.clientProtocolVersion);
        logger.info("[McpServer.handleInitialize] 注册的工具数量: {}", toolHandlers.size());
        logger.info("[McpServer.handleInitialize] 注册的资源数量: {}", resourceHandlers.size());
        logger.info("[McpServer.handleInitialize] 注册的提示数量: {}", promptHandlers.size());

        // 构建初始化结果 - 使用客户端请求的协议版本
        InitializeResult result = new InitializeResult();
        result.setProtocolVersion(this.clientProtocolVersion);
        result.setCapabilities(this.capabilities);
        result.setServerInfo(new ServerInfo(serverName, serverVersion));

        logger.info("[McpServer.handleInitialize] 发送初始化响应...");
        logger.info("[McpServer.handleInitialize] 响应内容: protocolVersion={}, serverName={}, serverVersion={}",
                this.clientProtocolVersion, serverName, serverVersion);
        sendResponse(request.getId(), result);
        logger.info("[McpServer.handleInitialize] 初始化响应已发送完成");
    }

    /**
     * 处理初始化确认
     */
    private void handleInitialized(JsonRpcMessage.Request request) {
        // 初始化确认是通知，不需要响应
        // 但需要确认服务器已正确初始化
        logger.info("MCP服务器初始化完成，准备处理请求");
    }

    /**
     * 处理工具列表请求
     */
    private void handleToolsList(JsonRpcMessage.Request request) throws IOException {
        logger.info("[McpServer.handleToolsList] ========== 开始处理tools/list请求 ==========");
        long startTime = System.currentTimeMillis();

        List<Tool> tools = new ArrayList<>();
        logger.info("[McpServer.handleToolsList] 正在构建工具列表，handler数量: {}", toolHandlers.size());

        for (Map.Entry<String, ToolHandler> entry : toolHandlers.entrySet()) {
            ToolHandler handler = entry.getValue();
            Tool tool = new Tool();
            tool.setName(entry.getKey());
            tool.setDescription(handler.getDescription());
            tool.setInputSchema(handler.getInputSchema());
            tools.add(tool);
        }

        logger.info("[McpServer.handleToolsList] 工具列表构建完成，共 {} 个工具", tools.size());

        ListToolsResult result = new ListToolsResult(tools);
        logger.info("[McpServer.handleToolsList] ListToolsResult对象已创建");

        long buildTime = System.currentTimeMillis() - startTime;
        logger.info("[McpServer.handleToolsList] 构建耗时 {}ms，准备发送响应...", buildTime);

        sendResponse(request.getId(), result);
        logger.info("[McpServer.handleToolsList] ========== tools/list响应发送完成，总耗时 {}ms ==========", System.currentTimeMillis() - startTime);
    }

    /**
     * 处理工具调用请求
     */
    @SuppressWarnings("unchecked")
    private void handleToolsCall(JsonRpcMessage.Request request) throws IOException {
        Map<String, Object> params = request.getParams();
        if (params == null) {
            sendErrorResponse(request.getId(), JsonRpcMessage.ERROR_INVALID_PARAMS, "Missing params");
            return;
        }

        String toolName = (String) params.get("name");
        Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");

        ToolHandler handler = toolHandlers.get(toolName);
        if (handler == null) {
            sendErrorResponse(request.getId(), JsonRpcMessage.ERROR_INVALID_PARAMS, 
                    "Tool not found: " + toolName);
            return;
        }

        try {
            CallToolResult result = handler.call(arguments);
            sendResponse(request.getId(), result);
        } catch (Exception e) {
            CallToolResult errorResult = new CallToolResult();
            errorResult.setError(true);
            List<ToolContent> content = new ArrayList<>();
            content.add(ToolContent.error("Error: " + e.getMessage()));
            errorResult.setContent(content);
            sendResponse(request.getId(), errorResult);
        }
    }

    /**
     * 处理资源列表请求
     */
    private void handleResourcesList(JsonRpcMessage.Request request) throws IOException {
        List<Resource> resources = new ArrayList<>();
        
        for (Map.Entry<String, ResourceHandler> entry : resourceHandlers.entrySet()) {
            resources.add(entry.getValue().getResource());
        }

        ListResourcesResult result = new ListResourcesResult(resources);
        sendResponse(request.getId(), result);
    }

    /**
     * 处理提示列表请求
     */
    private void handlePromptsList(JsonRpcMessage.Request request) throws IOException {
        List<Prompt> prompts = new ArrayList<>();
        
        for (Map.Entry<String, PromptHandler> entry : promptHandlers.entrySet()) {
            prompts.add(entry.getValue().getPrompt());
        }

        ListPromptsResult result = new ListPromptsResult(prompts);
        sendResponse(request.getId(), result);
    }

    /**
     * 处理获取提示请求
     */
    @SuppressWarnings("unchecked")
    private void handlePromptsGet(JsonRpcMessage.Request request) throws IOException {
        Map<String, Object> params = request.getParams();
        if (params == null) {
            sendErrorResponse(request.getId(), JsonRpcMessage.ERROR_INVALID_PARAMS, "Missing params");
            return;
        }

        String promptName = (String) params.get("name");
        Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");

        PromptHandler handler = promptHandlers.get(promptName);
        if (handler == null) {
            sendErrorResponse(request.getId(), JsonRpcMessage.ERROR_INVALID_PARAMS, 
                    "Prompt not found: " + promptName);
            return;
        }

        try {
            GetPromptResult result = handler.get(arguments);
            sendResponse(request.getId(), result);
        } catch (Exception e) {
            sendErrorResponse(request.getId(), JsonRpcMessage.ERROR_INTERNAL_ERROR, 
                    "Error getting prompt: " + e.getMessage());
        }
    }

    /**
     * 发送响应
     */
    private void sendResponse(Object id, Object result) throws IOException {
        logger.info("[McpServer.sendResponse] ========== 准备发送响应 ==========");
        logger.info("[McpServer.sendResponse] 响应ID: {}", id);
        logger.info("[McpServer.sendResponse] 结果类型: {}", result != null ? result.getClass().getSimpleName() : "null");

        // 记录结果内容摘要
        if (result != null) {
            String resultStr = result.toString();
            String preview = resultStr.length() > 500 ? resultStr.substring(0, 500) + "..." : resultStr;
            logger.info("[McpServer.sendResponse] 结果内容摘要: {}", preview);
        }

        try {
            JsonRpcMessage.Response response = JsonRpcMessage.Response.success(id, result);
            logger.info("[McpServer.sendResponse] Response对象已创建, isError: {}", response.isError());

            transport.sendResponse(response);
            logger.info("[McpServer.sendResponse] ========== 响应发送成功 ==========");
        } catch (IOException e) {
            logger.error("[McpServer.sendResponse] 发送响应时发生IO异常: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("[McpServer.sendResponse] 发送响应时发生异常: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(Object id, int code, String message) throws IOException {
        logger.error("[McpServer.sendErrorResponse] ========== 准备发送错误响应 ==========");
        logger.error("[McpServer.sendErrorResponse] 响应ID: {}, 错误码: {}, 错误消息: {}", id, code, message);

        try {
            JsonRpcMessage.Response response = JsonRpcMessage.Response.error(id, code, message);
            transport.sendResponse(response);
            logger.info("[McpServer.sendErrorResponse] ========== 错误响应发送成功 ==========");
        } catch (IOException e) {
            logger.error("[McpServer.sendErrorResponse] 发送错误响应时发生IO异常: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("[McpServer.sendErrorResponse] 发送错误响应时发生异常: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 注册工具
     */
    public void addTool(String name, String description, ToolInputSchema inputSchema, 
                        Function<Map<String, Object>, CallToolResult> handler) {
        toolHandlers.put(name, new ToolHandler(description, inputSchema, handler));
        capabilities.setTools(true);
    }

    /**
     * 获取所有已注册工具的信息列表
     * @param filter 名称过滤关键字（可为null，返回全部工具）
     * @return 工具列表文本
     */
    public String getToolsInfo(String filter) {
        StringBuilder sb = new StringBuilder();
        List<String> names = new ArrayList<>(toolHandlers.keySet());
        Collections.sort(names);

        String keyword = (filter == null || filter.trim().isEmpty()) ? null : filter.trim().toLowerCase();
        int count = 0;
        for (String name : names) {
            if (keyword != null && !name.toLowerCase().contains(keyword)) {
                continue;
            }
            ToolHandler handler = toolHandlers.get(name);
            sb.append("- `").append(name).append("`\n  ").append(handler.getDescription()).append("\n");
            count++;
        }

        StringBuilder result = new StringBuilder();
        result.append("MCP服务器已注册工具");
        if (keyword != null) {
            result.append("（过滤: ").append(filter.trim()).append("）");
        }
        result.append(": ").append(count).append(" 个");
        if (keyword == null) {
            result.append(" / 总计 ").append(toolHandlers.size()).append(" 个");
        }
        result.append("\n");
        result.append("===================\n\n");
        result.append(sb);
        return result.toString();
    }

    /**
     * 注册资源
     */
    public void addResource(Resource resource, ResourceHandler handler) {
        resourceHandlers.put(resource.getUri(), handler);
        capabilities.setResources(true);
    }

    /**
     * 注册提示
     */
    public void addPrompt(Prompt prompt, PromptHandler handler) {
        promptHandlers.put(prompt.getName(), handler);
        capabilities.setPrompts(true);
    }

    /**
     * 工具处理器
     */
    private static class ToolHandler {
        private final String description;
        private final ToolInputSchema inputSchema;
        private final Function<Map<String, Object>, CallToolResult> handler;

        ToolHandler(String description, ToolInputSchema inputSchema, 
                   Function<Map<String, Object>, CallToolResult> handler) {
            this.description = description;
            this.inputSchema = inputSchema;
            this.handler = handler;
        }

        String getDescription() {
            return description;
        }

        ToolInputSchema getInputSchema() {
            return inputSchema;
        }

        CallToolResult call(Map<String, Object> arguments) {
            // 校验参数：检测未知参数
            if (inputSchema != null && inputSchema.getProperties() != null) {
                java.util.Set<String> validParams = inputSchema.getProperties().keySet();
                for (String key : arguments.keySet()) {
                    if (!validParams.contains(key)) {
                        CallToolResult result = new CallToolResult();
                        result.setError(true);
                        java.util.List<ToolContent> content = new java.util.ArrayList<>();
                        content.add(ToolContent.error("未知参数: " + key + "，有效参数为: " + String.join(", ", validParams)));
                        result.setContent(content);
                        return result;
                    }
                }
            }
            return handler.apply(arguments);
        }
    }

    /**
     * 资源处理器接口
     */
    public interface ResourceHandler {
        Resource getResource();
        String getContent();
    }

    /**
     * 提示处理器接口
     */
    public interface PromptHandler {
        Prompt getPrompt();
        GetPromptResult get(Map<String, Object> arguments);
    }
}

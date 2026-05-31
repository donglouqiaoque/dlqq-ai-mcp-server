package com.donglouqiaoque.aitools.mcp.server;

import com.donglouqiaoque.aitools.mcp.protocol.JsonRpcMessage;
import com.donglouqiaoque.aitools.mcp.protocol.McpSchema;
import com.donglouqiaoque.aitools.mcp.protocol.McpSchema.*;
import com.donglouqiaoque.aitools.mcp.transport.McpTransport;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * MCP服务器核心类
 */
public class McpServer {

    private final String serverName;
    private final String serverVersion;
    private final McpTransport transport;
    private final Map<String, ToolHandler> toolHandlers;
    private final Map<String, ResourceHandler> resourceHandlers;
    private final Map<String, PromptHandler> promptHandlers;
    private final Capabilities capabilities;

    private boolean initialized = false;
    private String clientProtocolVersion;

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
        try {
            String method = request.getMethod();
            
            // 检查是否是通知（notification）- id为null
            boolean isNotification = request.isNotification();
            
            switch (method) {
                case "initialize":
                    handleInitialize(request);
                    break;
                case "initialized":
                case "notifications/initialized":
                    // 客户端确认初始化完成 - 这是通知，不返回响应
                    handleInitialized(request);
                    break;
                case "tools/list":
                    handleToolsList(request);
                    break;
                case "tools/call":
                    handleToolsCall(request);
                    break;
                case "resources/list":
                    handleResourcesList(request);
                    break;
                case "prompts/list":
                    handlePromptsList(request);
                    break;
                case "prompts/get":
                    handlePromptsGet(request);
                    break;
                default:
                    // 对于通知，不发送错误响应
                    if (!isNotification) {
                        sendErrorResponse(request.getId(), JsonRpcMessage.ERROR_METHOD_NOT_FOUND,
                                "Unknown method: " + method);
                    } else {
                        System.err.println("忽略未知通知: " + method);
                    }
            }
        } catch (Exception e) {
            try {
                // 对于通知，不发送错误响应
                if (request.getId() != null) {
                    sendErrorResponse(request.getId(), JsonRpcMessage.ERROR_INTERNAL_ERROR,
                            "Internal error: " + e.getMessage());
                } else {
                    System.err.println("处理通知时出错: " + e.getMessage());
                }
            } catch (IOException ioException) {
                System.err.println("Failed to send error response: " + ioException.getMessage());
            }
        }
    }

    /**
     * 处理初始化请求
     */
    @SuppressWarnings("unchecked")
    private void handleInitialize(JsonRpcMessage.Request request) throws IOException {
        Map<String, Object> params = request.getParams();
        if (params == null) {
            sendErrorResponse(request.getId(), JsonRpcMessage.ERROR_INVALID_PARAMS, "Missing params");
            return;
        }

        this.clientProtocolVersion = (String) params.get("protocolVersion");
        this.initialized = true;

        System.err.println("收到初始化请求，protocolVersion=" + this.clientProtocolVersion);

        // 构建初始化结果
        InitializeResult result = new InitializeResult();
        result.setProtocolVersion(McpSchema.LATEST_PROTOCOL_VERSION);
        result.setCapabilities(this.capabilities);
        result.setServerInfo(new ServerInfo(serverName, serverVersion));

        System.err.println("发送初始化响应...");
        sendResponse(request.getId(), result);
        System.err.println("初始化响应已发送");
    }

    /**
     * 处理初始化确认
     */
    private void handleInitialized(JsonRpcMessage.Request request) {
        // 初始化确认是通知，不需要响应
        // 但需要确认服务器已正确初始化
        System.err.println("MCP服务器初始化完成，准备处理请求");
    }

    /**
     * 处理工具列表请求
     */
    private void handleToolsList(JsonRpcMessage.Request request) throws IOException {
        List<Tool> tools = new ArrayList<>();
        
        for (Map.Entry<String, ToolHandler> entry : toolHandlers.entrySet()) {
            ToolHandler handler = entry.getValue();
            Tool tool = new Tool();
            tool.setName(entry.getKey());
            tool.setDescription(handler.getDescription());
            tool.setInputSchema(handler.getInputSchema());
            tools.add(tool);
        }

        ListToolsResult result = new ListToolsResult(tools);
        sendResponse(request.getId(), result);
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
        JsonRpcMessage.Response response = JsonRpcMessage.Response.success(id, result);
        transport.sendResponse(response);
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(Object id, int code, String message) throws IOException {
        JsonRpcMessage.Response response = JsonRpcMessage.Response.error(id, code, message);
        transport.sendResponse(response);
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

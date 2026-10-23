package com.donglouqiaoque.dlqqaimcpserver.mcp.server;

import com.donglouqiaoque.dlqqaimcpserver.mcp.protocol.JsonRpcMessage;
import com.donglouqiaoque.dlqqaimcpserver.mcp.protocol.McpSchema.*;
import com.donglouqiaoque.dlqqaimcpserver.mcp.transport.McpTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    /** 兜底通知默认延时（毫秒），可用 -Dmcp.listChanged.delay.ms 覆盖 */
    private static final long DEFAULT_TOOLS_NOTIFY_DELAY_MS = 10_000L;
    /** 兜底通知调度器（守护线程，避免阻止进程退出） */
    private final ScheduledExecutorService notificationScheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "tools-list-changed-notifier");
                thread.setDaemon(true);
                return thread;
            });
    /** 当前待执行的兜底通知任务 */
    private volatile ScheduledFuture<?> pendingToolsNotification;
    /** 客户端是否已请求过 tools/list（请求过后不再推送兜底通知） */
    private volatile boolean toolsListRequested = false;
    /** 兜底通知是否已经发送过（只发一次） */
    private volatile boolean fallbackToolsNotificationSent = false;

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
     * 通知客户端工具列表已变化（notifications/tools/list_changed）。
     *
     * @return true 表示通知已通过至少一个通道发送
     */
    public boolean notifyToolsListChanged() {
        boolean sent = transport.notifyToolsListChanged();
        logger.info("[McpServer] Sent notifications/tools/list_changed, result={}", sent);
        return sent;
    }

    /**
     * 动态工具管理：被移除（禁用）的工具集合。
     * 工具本身仍保留在 toolHandlers 中，便于 reset 时恢复。
     */
    private final Set<String> disabledTools = ConcurrentHashMap.newKeySet();

    /** 不允许被移除的管理工具（否则无法恢复） */
    private static final Set<String> PROTECTED_TOOLS = Set.of(
            "dlqq_mcp_remove_tools",
            "dlqq_mcp_refresh_tools",
            "dlqq_mcp_reset_tools",
            "dlqq_mcp_run_tools");

    /**
     * 动态工具变更结果。
     */
    public static class ToolChangeResult {
        /** 本次实际移除/恢复的工具 */
        public final List<String> applied = new ArrayList<>();
        /** 不存在或未启用的工具 */
        public final List<String> unknown = new ArrayList<>();
        /** 受保护、不允许移除的管理工具 */
        public final List<String> protectedNames = new ArrayList<>();
        /** 变更后处于启用状态的工具数量 */
        public int enabledCount;
        /** 变更后处于移除状态的工具数量 */
        public int disabledCount;
    }

    /**
     * 是否存在指定工具（含已被移除的工具）。
     */
    public boolean hasTool(String name) {
        return toolHandlers.containsKey(name);
    }

    /**
     * 当前被移除（禁用）的工具列表。
     */
    public Set<String> getDisabledTools() {
        return new LinkedHashSet<>(disabledTools);
    }

    /**
     * 当前启用（对客户端可见）的工具数量。
     */
    public int getEnabledToolCount() {
        return toolHandlers.size() - disabledTools.size();
    }

    /**
     * 启动时应用持久化的排除列表（.dlqq/settings/settings.properties 的 mcp.tools.excluded）。
     *
     * @param names 要移除的工具名
     * @return 实际生效的数量
     */
    public int applyExcludedTools(Collection<String> names) {
        int applied = 0;
        if (names == null) {
            return 0;
        }
        for (String name : names) {
            if (name == null || name.trim().isEmpty()) {
                continue;
            }
            String toolName = name.trim();
            if (PROTECTED_TOOLS.contains(toolName) || !toolHandlers.containsKey(toolName)) {
                logger.warn("[McpServer] Ignored invalid excluded tool at startup: {}", toolName);
                continue;
            }
            if (disabledTools.add(toolName)) {
                applied++;
            }
        }
        logger.info("[McpServer] Applied {} excluded tools at startup, enabled tools={}", applied, getEnabledToolCount());
        return applied;
    }

    /**
     * 运行时移除工具（仅禁用，不删除注册信息，便于 reset 恢复）。
     */
    public ToolChangeResult disableTools(Collection<String> names) {
        ToolChangeResult result = new ToolChangeResult();
        if (names != null) {
            for (String name : names) {
                if (name == null || name.trim().isEmpty()) {
                    continue;
                }
                String toolName = name.trim();
                if (PROTECTED_TOOLS.contains(toolName)) {
                    result.protectedNames.add(toolName);
                } else if (!toolHandlers.containsKey(toolName)) {
                    result.unknown.add(toolName);
                } else if (disabledTools.add(toolName)) {
                    result.applied.add(toolName);
                }
            }
        }
        result.enabledCount = getEnabledToolCount();
        result.disabledCount = disabledTools.size();
        logger.info("[McpServer] Removed tools: applied={}, unknown={}, protected={}, enabled={}",
                result.applied, result.unknown, result.protectedNames, result.enabledCount);
        return result;
    }

    /**
     * 恢复所有被移除的工具。
     *
     * @return 实际恢复的工具名
     */
    public List<String> enableAllTools() {
        List<String> restored = new ArrayList<>(disabledTools);
        disabledTools.clear();
        logger.info("[McpServer] Restored all removed tools: {}", restored);
        return restored;
    }

    /**
     * 指定工具当前是否被移除。
     */
    public boolean isToolDisabled(String name) {
        return disabledTools.contains(name);
    }

    /**
     * 直接调用已注册工具，忽略“运行时移除/客户端可见性”限制。
     *
     * <p>供 {@code dlqq_mcp_run_tools} 使用：即使工具已被 {@code dlqq_mcp_remove_tools}
     * 移除、或客户端尚未加载，也按名称直接执行服务端已注册的处理器。</p>
     *
     * @param name      工具名
     * @param arguments 工具参数（可为 null）
     * @return 工具执行结果；工具未注册时返回 null
     * @throws Exception 工具执行过程中的异常
     */
    public CallToolResult invokeToolDirect(String name, Map<String, Object> arguments) throws Exception {
        ToolHandler handler = toolHandlers.get(name);
        if (handler == null) {
            return null;
        }
        logger.info("[McpServer] dlqq_mcp_run_tools direct call: {} (disabled={})", name, disabledTools.contains(name));
        return handler.call(arguments);
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
     * 客户端发来任意消息时调用：取消兜底通知；收到 tools/list 后永久取消。
     *
     * @param method JSON-RPC 方法名
     */
    private void onClientActivity(String method) {
        cancelPendingToolsNotification();
        if ("tools/list".equals(method)) {
            toolsListRequested = true;
            return;
        }
        if ("initialize".equals(method) || "initialized".equals(method)
                || "notifications/initialized".equals(method)) {
            scheduleFallbackToolsNotification();
        }
    }

    /**
     * 初始化后启动兜底计时：若客户端在延时内没有请求 tools/list，则推送一次列表变更通知。
     */
    private void scheduleFallbackToolsNotification() {
        if (toolsListRequested || fallbackToolsNotificationSent) {
            return;
        }
        cancelPendingToolsNotification();
        long delayMs = Long.getLong("mcp.listChanged.delay.ms", DEFAULT_TOOLS_NOTIFY_DELAY_MS);
        pendingToolsNotification = notificationScheduler.schedule(() -> {
            if (!toolsListRequested && !fallbackToolsNotificationSent) {
                fallbackToolsNotificationSent = true;
                logger.info("[McpServer] No tools/list within {}ms after initialize; sending notifications/tools/list_changed", delayMs);
                notifyToolsListChanged();
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    /** 取消尚未触发的兜底通知。 */
    private void cancelPendingToolsNotification() {
        ScheduledFuture<?> pending = pendingToolsNotification;
        if (pending != null) {
            pending.cancel(false);
            pendingToolsNotification = null;
        }
    }

    /**
     * 停止服务器
     */
    public void stop() throws IOException {
        cancelPendingToolsNotification();
        notificationScheduler.shutdownNow();
        this.transport.stop();
    }

    /**
     * 处理传入的消息
     */
    private void handleMessage(JsonRpcMessage.Request request) {
        String method = request.getMethod();
        onClientActivity(method);
        logger.info("[McpServer.handleMessage] Received request: method={}, id={}, isNotification={}",
                method, request.getId(), request.isNotification());

        try {
            // 检查是否是通知（notification）- id为null
            boolean isNotification = request.isNotification();

            switch (method) {
                case "initialize":
                    logger.info("[McpServer.handleMessage] Handling initialize request");
                    handleInitialize(request);
                    break;
                case "initialized":
                case "notifications/initialized":
                    // 客户端确认初始化完成 - 这是通知，不返回响应
                    logger.info("[McpServer.handleMessage] Handling initialized notification");
                    handleInitialized(request);
                    break;
                case "tools/list":
                    logger.info("[McpServer.handleMessage] Handling tools/list request");
                    handleToolsList(request);
                    break;
                case "tools/call":
                    logger.info("[McpServer.handleMessage] Handling tools/call request");
                    handleToolsCall(request);
                    break;
                case "resources/list":
                    logger.info("[McpServer.handleMessage] Handling resources/list request");
                    handleResourcesList(request);
                    break;
                case "prompts/list":
                    logger.info("[McpServer.handleMessage] Handling prompts/list request");
                    handlePromptsList(request);
                    break;
                case "prompts/get":
                    logger.info("[McpServer.handleMessage] Handling prompts/get request");
                    handlePromptsGet(request);
                    break;
                default:
                    // 对于通知，不发送错误响应
                    if (!isNotification) {
                        logger.warn("[McpServer.handleMessage] Unknown method: {}", method);
                        sendErrorResponse(request.getId(), JsonRpcMessage.ERROR_METHOD_NOT_FOUND,
                                "Unknown method: " + method);
                    } else {
                        logger.warn("[McpServer.handleMessage] Ignoring unknown notification: {}", method);
                    }
            }
        } catch (Exception e) {
            logger.error("[McpServer.handleMessage] Error handling request: method={}, error={}", method, e.getMessage(), e);
            try {
                // 对于通知，不发送错误响应
                if (request.getId() != null) {
                    sendErrorResponse(request.getId(), JsonRpcMessage.ERROR_INTERNAL_ERROR,
                            "Internal error: " + e.getMessage());
                } else {
                    logger.error("[McpServer.handleMessage] Error handling notification: {}", e.getMessage(), e);
                }
            } catch (IOException ioException) {
                logger.error("[McpServer.handleMessage] Failed to send error response: {}", ioException.getMessage(), ioException);
            }
        }
    }

    /**
     * 处理初始化请求
     */
    @SuppressWarnings("unchecked")
    private void handleInitialize(JsonRpcMessage.Request request) throws IOException {
        Map<String, Object> params = request.getParams();
        logger.info("[McpServer.handleInitialize] Starting, params={}", params);

        if (params == null) {
            logger.error("[McpServer.handleInitialize] Missing params");
            sendErrorResponse(request.getId(), JsonRpcMessage.ERROR_INVALID_PARAMS, "Missing params");
            return;
        }

        this.clientProtocolVersion = (String) params.get("protocolVersion");
        this.initialized = true;

        logger.info("[McpServer.handleInitialize] Client protocol version: {}", this.clientProtocolVersion);
        logger.info("[McpServer.handleInitialize] Registered tools: {}", getEnabledToolCount());
        logger.info("[McpServer.handleInitialize] Registered resources: {}", resourceHandlers.size());
        logger.info("[McpServer.handleInitialize] Registered prompts: {}", promptHandlers.size());

        // 构建初始化结果 - 使用客户端请求的协议版本
        InitializeResult result = new InitializeResult();
        result.setProtocolVersion(this.clientProtocolVersion);
        result.setCapabilities(this.capabilities);
        result.setServerInfo(new ServerInfo(serverName, serverVersion));

        logger.info("[McpServer.handleInitialize] Sending initialize response...");
        logger.info("[McpServer.handleInitialize] Response: protocolVersion={}, serverName={}, serverVersion={}",
                this.clientProtocolVersion, serverName, serverVersion);
        sendResponse(request.getId(), result);
        logger.info("[McpServer.handleInitialize] Initialize response sent");
    }

    /**
     * 处理初始化确认
     */
    private void handleInitialized(JsonRpcMessage.Request request) {
        // 初始化确认是通知，不需要响应
        // 但需要确认服务器已正确初始化
        logger.info("MCP server initialized, ready for requests");
    }

    /**
     * 处理工具列表请求
     */
    private void handleToolsList(JsonRpcMessage.Request request) throws IOException {
        logger.info("[McpServer.handleToolsList] ========== Handling tools/list request ==========");
        long startTime = System.currentTimeMillis();

        List<Tool> tools = new ArrayList<>();
        logger.info("[McpServer.handleToolsList] Building tool list, handler count: {}", toolHandlers.size());

        for (Map.Entry<String, ToolHandler> entry : toolHandlers.entrySet()) {
            if (disabledTools.contains(entry.getKey())) {
                continue;
            }
            ToolHandler handler = entry.getValue();
            Tool tool = new Tool();
            tool.setName(entry.getKey());
            tool.setDescription(handler.getDescription());
            tool.setInputSchema(handler.getInputSchema());
            tools.add(tool);
        }

        logger.info("[McpServer.handleToolsList] Tool list built: {} tools ({} removed)",
                tools.size(), disabledTools.size());

        ListToolsResult result = new ListToolsResult(tools);
        logger.info("[McpServer.handleToolsList] ListToolsResult created");

        long buildTime = System.currentTimeMillis() - startTime;
        logger.info("[McpServer.handleToolsList] Build took {}ms, sending response...", buildTime);

        sendResponse(request.getId(), result);
        logger.info("[McpServer.handleToolsList] ========== tools/list response sent, total {}ms ==========", System.currentTimeMillis() - startTime);
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
        if (disabledTools.contains(toolName)) {
            sendErrorResponse(request.getId(), JsonRpcMessage.ERROR_INVALID_PARAMS,
                    "工具已被移除: " + toolName + "（可用 dlqq_mcp_reset_tools 恢复）");
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
        logger.info("[McpServer.sendResponse] ========== Sending response ==========");
        logger.info("[McpServer.sendResponse] Response ID: {}", id);
        logger.info("[McpServer.sendResponse] Result type: {}", result != null ? result.getClass().getSimpleName() : "null");

        // 记录结果内容摘要
        if (result != null) {
            String resultStr = result.toString();
            String preview = resultStr.length() > 500 ? resultStr.substring(0, 500) + "..." : resultStr;
            logger.info("[McpServer.sendResponse] Result summary: {}", preview);
        }

        try {
            JsonRpcMessage.Response response = JsonRpcMessage.Response.success(id, result);
            logger.info("[McpServer.sendResponse] Response created, isError: {}", response.isError());

            transport.sendResponse(response);
            logger.info("[McpServer.sendResponse] ========== Response sent ==========");
        } catch (IOException e) {
            logger.error("[McpServer.sendResponse] IO error while sending response: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("[McpServer.sendResponse] Error while sending response: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(Object id, int code, String message) throws IOException {
        logger.error("[McpServer.sendErrorResponse] ========== Sending error response ==========");
        logger.error("[McpServer.sendErrorResponse] Response ID: {}, code: {}, message: {}", id, code, message);

        try {
            JsonRpcMessage.Response response = JsonRpcMessage.Response.error(id, code, message);
            transport.sendResponse(response);
            logger.info("[McpServer.sendErrorResponse] ========== Error response sent ==========");
        } catch (IOException e) {
            logger.error("[McpServer.sendErrorResponse] IO error while sending error response: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("[McpServer.sendErrorResponse] Error while sending error response: {}", e.getMessage(), e);
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
            if (disabledTools.contains(name)) {
                continue;
            }
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
            result.append(" / 总计 ").append(getEnabledToolCount()).append(" 个");
        }
        if (!disabledTools.isEmpty()) {
            List<String> disabled = new ArrayList<>(disabledTools);
            Collections.sort(disabled);
            result.append("（已移除 ").append(disabled.size()).append(" 个: ")
                  .append(String.join(", ", disabled)).append("）");
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
                Set<String> validParams = inputSchema.getProperties().keySet();
                for (String key : arguments.keySet()) {
                    if (!validParams.contains(key)) {
                        CallToolResult result = new CallToolResult();
                        result.setError(true);
                        List<ToolContent> content = new ArrayList<>();
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

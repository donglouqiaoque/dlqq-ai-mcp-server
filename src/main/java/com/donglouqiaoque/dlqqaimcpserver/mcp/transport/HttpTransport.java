package com.donglouqiaoque.dlqqaimcpserver.mcp.transport;

import com.donglouqiaoque.dlqqaimcpserver.mcp.protocol.JsonRpcMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.io.OutputStream;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * MCP Streamable HTTP 传输层（基于 JDK 内置 HttpServer，无需额外依赖）。
 *
 * <p>协议说明（MCP Streamable HTTP Transport）：</p>
 * <ul>
 *   <li>客户端向 <code>POST {path}</code>（默认 /mcp）发送 JSON-RPC 请求；</li>
 *   <li>请求（带 id）返回 JSON-RPC 响应（application/json）；</li>
 *   <li>通知（无 id）返回 202 Accepted，无响应体；</li>
 *   <li>GET {path} 返回服务健康信息，用于探测服务器状态；</li>
 *   <li>DELETE {path} 用于客户端请求终止会话（无状态模式直接返回 200）。</li>
 * </ul>
 */
public class HttpTransport implements McpTransport {

    private static final Logger logger = LoggerFactory.getLogger(HttpTransport.class);

    /** HTTP 监听端口（端口被占用、由用户重新指定时更新为最终生效的端口） */
    private volatile int port;
    /** MCP 端点路径 */
    private final String path;
    /** HTTP 服务器 */
    private HttpServer httpServer;
    /** 请求处理线程池 */
    private ExecutorService executor;
    /** 运行状态 */
    private final AtomicBoolean running = new AtomicBoolean(false);
    /** 消息处理器（由 McpServer 注入） */
    private volatile Consumer<JsonRpcMessage.Request> messageHandler;
    /** Gson JSON 解析器（整数 id 保留为 Long，避免序列化后变成 1.0） */
    private final Gson gson = new GsonBuilder()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .setLenient()
            .create();

    /**
     * 当前请求线程的响应暂存区。
     * McpServer 处理请求时会在同一线程内同步调用 sendResponse，
     * 因此通过 ThreadLocal 把响应关联回当前 HTTP 请求。
     */
    private final ThreadLocal<JsonRpcMessage.Response> currentResponse = new ThreadLocal<>();

    /** 已建立的 SSE 长连接（GET + Accept: text/event-stream），用于服务端主动推送通知 */
    private final List<SseClient> sseClients = new CopyOnWriteArrayList<>();

    /** 一个 SSE 客户端连接的输出流（写操作需持有 lock） */
    private static final class SseClient {
        final OutputStream out;
        final Object lock = new Object();

        SseClient(OutputStream out) {
            this.out = out;
        }
    }

    public HttpTransport(int port, String path) {
        this.port = port;
        this.path = (path == null || path.trim().isEmpty()) ? "/mcp" : path;
    }

    public HttpTransport(int port) {
        this(port, "/mcp");
    }

    @Override
    public void start() throws IOException {
        if (running.get()) {
            logger.warn("[HttpTransport] Transport already running, ignoring duplicate start");
            return;
        }
        try {
            httpServer = bindServer();
            httpServer.createContext(this.path, this::handleExchange);
            executor = Executors.newFixedThreadPool(8, r -> {
                Thread t = new Thread(r, "MCP-HTTP-Worker");
                t.setDaemon(true);
                return t;
            });
            httpServer.setExecutor(executor);
            httpServer.start();
            running.set(true);
            logger.info("[HttpTransport] Streamable HTTP transport started: http://localhost:{}{} (POST JSON-RPC)",
                    port, this.path);
        } catch (IOException e) {
            logger.error("[HttpTransport] HTTP server failed to start (port {}): {}", port, e.getMessage());
            throw e;
        }
    }

    /** promptForPort 返回值：重新尝试默认端口（用户直接回车） */
    private static final int RETRY_DEFAULT_PORT = -1;
    /** promptForPort 返回值：无法询问或输入结束，放弃启动 */
    private static final int ABORT = -2;

    /** 控制台输入读取器（复用同一个实例，避免 BufferedReader 预读导致后续输入丢失） */
    private BufferedReader consoleReader;

    /**
     * 绑定监听端口；端口被占用时提示用户输入新端口：
     * <ul>
     *   <li>输入新端口 → 用它重试；若仍被占用，继续提示；</li>
     *   <li>直接回车 → 重新尝试默认端口；仍被占用则继续提示；</li>
     *   <li>无法交互/输入结束 → 抛出原始异常（进程退出）。</li>
     * </ul>
     */
    private HttpServer bindServer() throws IOException {
        final int defaultPort = this.port;
        int candidate = defaultPort;
        while (true) {
            try {
                HttpServer server = HttpServer.create(new InetSocketAddress(candidate), 0);
                this.port = candidate;
                return server;
            } catch (BindException e) {
                logger.warn("[HttpTransport] Port {} already in use", candidate);
                int next = promptForPort(candidate, defaultPort);
                if (next == ABORT) {
                    throw e;
                }
                candidate = (next == RETRY_DEFAULT_PORT) ? defaultPort : next;
            }
        }
    }

    /**
     * 询问用户输入新端口。
     *
     * @return 新端口；{@link #RETRY_DEFAULT_PORT} 表示重试默认端口；{@link #ABORT} 表示放弃
     */
    private int promptForPort(int busyPort, int defaultPort) {
        if (!canPrompt()) {
            System.out.println("[HTTP] Port " + busyPort + " is already in use.");
            System.out.println("[HTTP] Use --port <number> to choose another port, or free the port and retry.");
            System.out.flush();
            return ABORT;
        }
        BufferedReader reader = consoleReader();
        while (true) {
            System.out.println("[HTTP] Port " + busyPort + " is already in use.");
            System.out.print("[HTTP] Enter a new port (1-65535), or press Enter to retry port "
                    + defaultPort + " (Ctrl+C to exit): ");
            System.out.flush();
            String line;
            try {
                line = reader.readLine();
            } catch (IOException e) {
                return ABORT;
            }
            if (line == null) {
                return ABORT;
            }
            line = line.trim();
            if (line.isEmpty()) {
                return RETRY_DEFAULT_PORT;
            }
            try {
                int p = Integer.parseInt(line);
                if (p >= 1 && p <= 65535) {
                    if (p == busyPort) {
                        System.out.println("[HTTP] Port " + p + " is still in use, please enter another one.");
                        continue;
                    }
                    return p;
                }
                System.out.println("[HTTP] Port must be between 1 and 65535.");
            } catch (NumberFormatException e) {
                System.out.println("[HTTP] Invalid input, please enter a number.");
            }
        }
    }

    /**
     * 复用同一个控制台读取器：BufferedReader 会预读，反复 new 会丢掉已缓冲的后续输入。
     */
    private BufferedReader consoleReader() {
        if (consoleReader == null) {
            consoleReader = new BufferedReader(new InputStreamReader(System.in));
        }
        return consoleReader;
    }

    /**
     * 是否允许从控制台读取端口：默认要求存在交互式控制台，
     * 可用 -Ddlqq.http.askPort=true/false 强制开启或关闭（便于脚本/测试）。
     */
    private boolean canPrompt() {
        String override = System.getProperty("dlqq.http.askPort");
        if (override != null && !override.trim().isEmpty()) {
            return Boolean.parseBoolean(override.trim());
        }
        return System.console() != null;
    }

    /**
     * 实际监听端口（端口冲突并被用户改端口后，返回最终生效的端口）。
     */
    public int getPort() {
        return port;
    }

    @Override
    public void stop() {
        running.set(false);
        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        logger.info("[HttpTransport] HTTP transport stopped");
    }

    @Override
    public boolean isRunning() {
        return running.get() && httpServer != null;
    }

    @Override
    public void sendResponse(JsonRpcMessage.Response response) throws IOException {
        JsonRpcMessage.Response prev = currentResponse.get();
        if (prev != null) {
            // 同一请求发出多个响应（当前实现不产生），保留最后一个并记录警告
            logger.warn("[HttpTransport] sendResponse called multiple times for the same request; old response will be overwritten");
        }
        currentResponse.set(response);
    }

    @Override
    public void setMessageHandler(Consumer<JsonRpcMessage.Request> handler) {
        this.messageHandler = handler;
    }

    // ==================== HTTP 处理 ====================

    private void handleExchange(HttpExchange exchange) {
        try {
            setCommonHeaders(exchange);
            String method = exchange.getRequestMethod();
            switch (method) {
                case "POST":
                    handlePost(exchange);
                    break;
                case "GET":
                    handleGet(exchange);
                    break;
                case "DELETE":
                    handleDelete(exchange);
                    break;
                case "OPTIONS":
                    // CORS 预检
                    sendEmpty(exchange, 204);
                    break;
                default:
                    sendJson(exchange, 405, jsonRpcError(null, JsonRpcMessage.ERROR_INVALID_REQUEST,
                            "Method Not Allowed: " + method));
                    break;
            }
        } catch (Exception e) {
            logger.error("[HttpTransport] Error handling HTTP request: {}", e.getMessage(), e);
            try {
                sendJson(exchange, 500, jsonRpcError(null, JsonRpcMessage.ERROR_INTERNAL_ERROR,
                        "Internal error: " + e.getMessage()));
            } catch (IOException ignored) {
                // 响应已发送或连接已断开
            }
        } finally {
            currentResponse.remove();
            exchange.close();
        }
    }

    private void setCommonHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, GET, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Accept, Mcp-Session-Id");
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        if (body == null || body.trim().isEmpty()) {
            sendJson(exchange, 400, jsonRpcError(null, JsonRpcMessage.ERROR_INVALID_REQUEST, "Empty request body"));
            return;
        }
        logger.debug("[HttpTransport] Received POST request: {} bytes", body.length());

        JsonRpcMessage.Request request = parseRequest(body);
        if (request == null) {
            sendJson(exchange, 400, jsonRpcError(null, JsonRpcMessage.ERROR_INVALID_REQUEST,
                    "Invalid JSON-RPC request"));
            return;
        }

        logger.info("[HttpTransport] Received JSON-RPC request: method={}, id={}, isNotification={}",
                request.getMethod(), request.getId(), request.isNotification());

        Consumer<JsonRpcMessage.Request> handler = messageHandler;
        if (handler == null) {
            sendJson(exchange, 500, jsonRpcError(request.getId(), JsonRpcMessage.ERROR_INTERNAL_ERROR,
                    "Message handler not set"));
            return;
        }

        currentResponse.set(null);
        try {
            handler.accept(request);
        } catch (Exception e) {
            logger.error("[HttpTransport] Message handler threw an exception: {}", e.getMessage(), e);
        }

        JsonRpcMessage.Response response = currentResponse.get();
        if (response == null) {
            if (request.isNotification()) {
                // 通知：202 Accepted，无响应体
                sendEmpty(exchange, 202);
                logger.info("[HttpTransport] Notification handled, returning 202 Accepted");
            } else {
                sendJson(exchange, 500, jsonRpcError(request.getId(), JsonRpcMessage.ERROR_INTERNAL_ERROR,
                        "No response produced"));
            }
            return;
        }

        String json = gson.toJson(response);
        logger.debug("[HttpTransport] Response JSON: {} bytes", json.length());
        sendJson(exchange, 200, json);
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        String accept = exchange.getRequestHeaders().getFirst("Accept");
        logger.info("[HttpTransport] Received GET request, Accept={}", accept);
        if (accept != null && accept.toLowerCase().contains("text/event-stream")) {
            openSseStream(exchange);
            return;
        }
        String body = "{\n" +
                "  \"status\": \"ok\",\n" +
                "  \"name\": \"dlqq-ai-mcp-server\",\n" +
                "  \"transport\": \"streamable-http\",\n" +
                "  \"endpoint\": \"" + this.path + "\",\n" +
                "  \"hint\": \"Send JSON-RPC requests via POST " + this.path + "\"\n" +
                "}";
        sendJson(exchange, 200, body);
    }

    private void handleDelete(HttpExchange exchange) throws IOException {
        // 无状态模式：直接确认会话终止
        sendEmpty(exchange, 200);
    }

    /**
     * 打开 SSE 长连接（GET {path}，Accept 包含 text/event-stream）。
     * 依据 MCP Streamable HTTP 规范，服务端可通过该通道主动推送通知。
     */
    private void openSseStream(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.getResponseHeaders().set("Connection", "keep-alive");
        exchange.sendResponseHeaders(200, 0);
        OutputStream out = exchange.getResponseBody();
        SseClient client = new SseClient(out);
        sseClients.add(client);
        logger.info("[HttpTransport] SSE client connected, connections={}", sseClients.size());
        try {
            synchronized (client.lock) {
                out.write(": connected\n\n".getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
            while (running.get()) {
                Thread.sleep(15000);
                synchronized (client.lock) {
                    out.write(": ping\n\n".getBytes(StandardCharsets.UTF_8));
                    out.flush();
                }
            }
        } catch (IOException | InterruptedException e) {
            logger.info("[HttpTransport] SSE connection ended: {}", e.getMessage());
        } finally {
            sseClients.remove(client);
            try {
                out.close();
            } catch (IOException ignored) {
            }
            logger.info("[HttpTransport] SSE client disconnected, remaining={}", sseClients.size());
        }
    }

    /**
     * 向所有 SSE 连接广播 notifications/tools/list_changed。
     */
    @Override
    public boolean notifyToolsListChanged() {
        if (sseClients.isEmpty()) {
            logger.info("[HttpTransport] No SSE connections, skipping notifications/tools/list_changed");
            return false;
        }
        String message = "event: message\ndata: {\"jsonrpc\":\"2.0\",\"method\":\"notifications/tools/list_changed\"}\n\n";
        int sent = 0;
        for (SseClient client : sseClients) {
            try {
                synchronized (client.lock) {
                    client.out.write(message.getBytes(StandardCharsets.UTF_8));
                    client.out.flush();
                }
                sent++;
            } catch (IOException e) {
                sseClients.remove(client);
                logger.warn("[HttpTransport] Failed to push SSE, removed connection: {}", e.getMessage());
            }
        }
        logger.info("[HttpTransport] Pushed notifications/tools/list_changed to {} SSE connection(s)", sent);
        return sent > 0;
    }

    // ==================== 工具方法 ====================

    /**
     * 解析 JSON-RPC 请求（与 StdioTransport 的解析逻辑一致）
     */
    @SuppressWarnings("unchecked")
    private JsonRpcMessage.Request parseRequest(String json) {
        try {
            Map<String, Object> map = gson.fromJson(json, new TypeToken<Map<String, Object>>() {}.getType());
            if (map == null) {
                return null;
            }
            Object id = map.get("id");
            String method = (String) map.get("method");
            if (method == null) {
                return null;
            }
            Map<String, Object> params = (Map<String, Object>) map.get("params");
            if (params == null) {
                params = new HashMap<>();
            }
            JsonRpcMessage.Request request = new JsonRpcMessage.Request();
            request.setId(id);
            request.setMethod(method);
            request.setParams(params);
            return request;
        } catch (Exception e) {
            logger.error("[HttpTransport] JSON parse failed: {}, JSON: {}", e.getMessage(), json);
            return null;
        }
    }

    private void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    private void sendEmpty(HttpExchange exchange, int status) throws IOException {
        exchange.sendResponseHeaders(status, -1);
    }

    private String jsonRpcError(Object id, int code, String message) {
        return "{\"jsonrpc\":\"2.0\",\"id\":" + gson.toJson(id) + ",\"error\":{\"code\":" + code
                + ",\"message\":" + gson.toJson(message) + "}}";
    }
}

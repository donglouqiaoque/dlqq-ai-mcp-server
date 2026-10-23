package com.donglouqiaoque.dlqqaimcpserver.mcp.transport;

import com.donglouqiaoque.dlqqaimcpserver.mcp.protocol.JsonRpcMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import com.donglouqiaoque.dlqqaimcpserver.mcp.protocol.McpSchema.CallToolResult;
import com.donglouqiaoque.dlqqaimcpserver.mcp.protocol.McpSchema.Capabilities;
import com.donglouqiaoque.dlqqaimcpserver.mcp.protocol.McpSchema.InitializeResult;
import com.donglouqiaoque.dlqqaimcpserver.mcp.protocol.McpSchema.ListPromptsResult;
import com.donglouqiaoque.dlqqaimcpserver.mcp.protocol.McpSchema.ListResourcesResult;
import com.donglouqiaoque.dlqqaimcpserver.mcp.protocol.McpSchema.ListToolsResult;
import com.donglouqiaoque.dlqqaimcpserver.mcp.protocol.McpSchema.Prompt;
import com.donglouqiaoque.dlqqaimcpserver.mcp.protocol.McpSchema.PromptArgument;
import com.donglouqiaoque.dlqqaimcpserver.mcp.protocol.McpSchema.Resource;
import com.donglouqiaoque.dlqqaimcpserver.mcp.protocol.McpSchema.ServerInfo;
import com.donglouqiaoque.dlqqaimcpserver.mcp.protocol.McpSchema.Tool;
import com.donglouqiaoque.dlqqaimcpserver.mcp.protocol.McpSchema.ToolContent;
import com.donglouqiaoque.dlqqaimcpserver.mcp.protocol.McpSchema.ToolInputSchema;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * 标准输入输出传输层实现
 * 遵循MCP协议规范，使用Content-Length头分隔消息
 */
public class StdioTransport implements McpTransport {

    private static final Logger logger = LoggerFactory.getLogger(StdioTransport.class);

    private final BufferedInputStream inputStream;
    private final BufferedOutputStream outputStream;
    private Consumer<JsonRpcMessage.Request> messageHandler;
    private final ExecutorService executor;
    private final AtomicBoolean running;
    private final BlockingQueue<String> messageQueue;
    private volatile boolean useContentLength = true;  // 是否使用Content-Length协议
    private final Gson gson;  // Gson JSON解析器

    public StdioTransport() {
        // 初始化Gson解析器
        this.gson = new GsonBuilder()
            .setLenient()  // 宽松模式，允许不规范的JSON
            .create();
        // 强制使用UTF-8编码的输入输出流
        this.inputStream = new BufferedInputStream(System.in);
        this.outputStream = new BufferedOutputStream(System.out);
        this.executor = Executors.newFixedThreadPool(2);
        this.running = new AtomicBoolean(false);
        this.messageQueue = new LinkedBlockingQueue<>();

        // 设置系统属性确保UTF-8编码
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("sun.stdout.encoding", "UTF-8");
        System.setProperty("sun.stderr.encoding", "UTF-8");

        // 设置默认字符集
        try {
            Charset.defaultCharset();
        } catch (Exception ignored) {
        }
    }
    
    private void debug(String msg) {
        // 使用日志框架输出调试信息，避免干扰MCP协议通信
        logger.debug("[MCP] {}", msg);
    }

    @Override
    public void start() throws IOException {
        running.set(true);
        logger.info("[StdioTransport] Transport starting...");
        logger.info("[StdioTransport] System.in available: {}", System.in.available());
        logger.info("[StdioTransport] Encoding: file.encoding={}, sun.stdout.encoding={}",
                System.getProperty("file.encoding"), System.getProperty("sun.stdout.encoding"));

        // 启动读取线程
        executor.submit(this::readLoop);
        logger.info("[StdioTransport] Reader thread started");

        // 启动处理线程
        executor.submit(this::processLoop);
        logger.info("[StdioTransport] Processing thread started");
    }
    
    /**
     * 检查传输层是否正在运行
     * 用于健康检查
     */
    public boolean isRunning() {
        return running.get() && !executor.isShutdown() && !executor.isTerminated();
    }

    /**
     * 通过 stdout 发送 notifications/tools/list_changed 通知。
     */
    @Override
    public boolean notifyToolsListChanged() {
        String notification = "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/tools/list_changed\"}";
        try {
            synchronized (outputStream) {
                outputStream.write(notification.getBytes(StandardCharsets.UTF_8));
                outputStream.write('\n');
                outputStream.flush();
            }
            logger.info("[StdioTransport] Sent notifications/tools/list_changed");
            return true;
        } catch (IOException e) {
            logger.error("[StdioTransport] Failed to send notifications/tools/list_changed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void stop() throws IOException {
        running.set(false);
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 读取循环 - 按照MCP协议规范读取Content-Length分隔的消息
     * 修复：增加重试机制，防止因临时EOF或IOException导致server断开
     */
    private void readLoop() {
        logger.info("[StdioTransport.readLoop] ========== Read loop started ==========");
        int errorCount = 0;
        final int MAX_ERROR_COUNT = 5;  // 最大连续错误次数
        int messageCount = 0;
        long lastStatusLog = System.currentTimeMillis();
        int bytesReadTotal = 0;

        while (running.get()) {
            try {
                // 每30秒输出一次状态
                if (System.currentTimeMillis() - lastStatusLog > 30000) {
                    logger.info("[StdioTransport.readLoop] Status - messages read: {}, total bytes: {}, available input: {}, running: {}",
                            messageCount, bytesReadTotal, inputStream.available(), running.get());
                    lastStatusLog = System.currentTimeMillis();
                }

                String message = readMessage();
                if (message != null) {
                    boolean offered = messageQueue.offer(message);
                    errorCount = 0;  // 重置错误计数
                    messageCount++;
                    bytesReadTotal += message.getBytes(StandardCharsets.UTF_8).length;

                    if (offered) {
                        logger.info("[StdioTransport.readLoop] Message queued, queue size: {}, total messages: {}",
                                messageQueue.size(), messageCount);
                    } else {
                        logger.error("[StdioTransport.readLoop] Failed to queue message! Queue may be full");
                    }

                    if (messageCount % 10 == 0) {
                        logger.info("[StdioTransport.readLoop] Processed {} messages, total bytes received: {}", messageCount, bytesReadTotal);
                    }
                } else {
                    // EOF 或读取失败 - 可能是父进程暂时断开
                    errorCount++;
                    logger.warn("[StdioTransport.readLoop] Read null (EOF?), error count {}/{}, available input: {}",
                            errorCount, MAX_ERROR_COUNT, inputStream.available());
                    if (errorCount >= MAX_ERROR_COUNT) {
                        logger.error("[StdioTransport.readLoop] {} consecutive read failures, exiting loop", MAX_ERROR_COUNT);
                        break;
                    }
                    try {
                        Thread.sleep(100);  // 短暂等待后重试
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (IOException e) {
                if (running.get()) {
                    logger.error("[StdioTransport.readLoop] Error reading stdin: {}", e.getMessage(), e);
                }
                errorCount++;
                if (errorCount >= MAX_ERROR_COUNT) {
                    logger.error("[StdioTransport.readLoop] {} consecutive IO errors, exiting loop", MAX_ERROR_COUNT);
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        logger.info("[StdioTransport.readLoop] ========== Read loop exited, {} messages processed, {} bytes total ==========", messageCount, bytesReadTotal);
    }

    /**
     * 读取一条完整的消息（根据Content-Length头）
     * 注意：Content-Length是字节长度，不是字符长度
     * 使用同一个InputStream读取头部和正文，避免BufferedReader预读问题
     *
     * 支持两种模式：
     * 1. 标准MCP模式：Content-Length头部 + 空行 + JSON正文
     * 2. 兼容模式：直接发送JSON（某些客户端如Qwen）
     */
    private String readMessage() throws IOException {
        debug("readMessage: 开始读取消息");
        // 读取第一行（使用UTF-8编码的字节缓冲区）
        ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();
        int b;

        while (true) {
            b = inputStream.read();
            if (b == -1) {
                debug("readMessage: 输入结束 (EOF)");
                return null;
            }

            if (b == '\n') {
                // 行结束
                break;
            } else if (b != '\r') {
                lineBuffer.write(b);
            }
        }

        String firstLine = new String(lineBuffer.toByteArray(), StandardCharsets.UTF_8).trim();
        debug("readMessage: 读取到第一行: [" + firstLine + "]");

        // 检查是否是直接发送的JSON（以{开头）
        if (firstLine.startsWith("{")) {
            debug("readMessage: 检测到直接JSON消息（无Content-Length），尝试读取完整JSON");
            useContentLength = false;  // 客户端不使用Content-Length，服务器也不使用
            // 尝试读取完整JSON（处理字符串值中包含换行符的情况）
            return readCompleteJson(firstLine);
        }

        // 标准MCP模式：解析Content-Length头部
        int contentLength = -1;

        // 第一行可能是Content-Length头
        if (firstLine.toLowerCase().startsWith("content-length:")) {
            try {
                contentLength = Integer.parseInt(firstLine.substring(firstLine.indexOf(':') + 1).trim());
                debug("readMessage: Content-Length=" + contentLength);
            } catch (NumberFormatException e) {
                debug("readMessage: Invalid Content-Length header: " + firstLine);
            }
        }

        // 继续读取头部直到空行
        while (true) {
            ByteArrayOutputStream headerBuffer = new ByteArrayOutputStream();
            while (true) {
                b = inputStream.read();
                if (b == -1) {
                    debug("readMessage: 读取头部时EOF");
                    return null;
                }
                if (b == '\n') break;
                if (b != '\r') headerBuffer.write(b);
            }
            String line = new String(headerBuffer.toByteArray(), StandardCharsets.UTF_8).trim();
            debug("readMessage: 读取到头部行: [" + line + "]");
            if (line.isEmpty()) {
                debug("readMessage: 遇到空行，头部结束");
                break;
            }
            if (line.toLowerCase().startsWith("content-length:")) {
                try {
                    contentLength = Integer.parseInt(line.substring(line.indexOf(':') + 1).trim());
                    debug("readMessage: Content-Length=" + contentLength);
                } catch (NumberFormatException e) {
                    debug("readMessage: Invalid Content-Length header: " + line);
                }
            }
        }

        if (contentLength < 0) {
            debug("readMessage: 未找到Content-Length，返回null");
            return null;
        }

        if (contentLength == 0) {
            debug("readMessage: Content-Length为0，返回空字符串");
            return "";
        }

        // 读取指定字节长度的内容
        byte[] buffer = new byte[contentLength];
        int totalRead = 0;
        while (totalRead < contentLength) {
            int read = inputStream.read(buffer, totalRead, contentLength - totalRead);
            if (read == -1) {
                debug("readMessage: 读取内容时遇到EOF，已读取 " + totalRead + "/" + contentLength + " 字节");
                break;
            }
            totalRead += read;
        }

        if (totalRead == 0) {
            debug("readMessage: 未读取到任何内容");
            return null;
        }

        String result = new String(buffer, 0, totalRead, StandardCharsets.UTF_8);
        debug("readMessage: 读取到消息内容 (" + totalRead + "字节): " + result);
        return result;
    }

    /**
     * 读取完整JSON消息（处理字符串值中包含换行符的情况）
     */
    private String readCompleteJson(String firstLine) throws IOException {
        StringBuilder jsonBuilder = new StringBuilder(firstLine);
        int braceCount = countBraces(firstLine);

        debug("readCompleteJson: 初始大括号计数=" + braceCount);

        // 如果第一行JSON已经完整，直接返回
        if (braceCount == 0 && firstLine.trim().endsWith("}")) {
            debug("readCompleteJson: JSON已完整");
            return firstLine;
        }

        // 继续读取直到JSON完整（大括号平衡）
        int timeout = 100; // 最大读取行数，防止无限循环
        int linesRead = 1;

        while (braceCount > 0 && timeout-- > 0) {
            // 读取下一行
            ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();
            int b;
            while (true) {
                b = inputStream.read();
                if (b == -1) {
                    debug("readCompleteJson: EOF，已读取 " + linesRead + " 行");
                    break;
                }
                if (b == '\n') break;
                if (b != '\r') lineBuffer.write(b);
            }

            if (b == -1) break;

            String line = new String(lineBuffer.toByteArray(), StandardCharsets.UTF_8);
            jsonBuilder.append('\n').append(line);
            linesRead++;

            braceCount += countBraces(line);
            debug("readCompleteJson: 读取第 " + linesRead + " 行，当前大括号计数=" + braceCount);

            // 尝试验证JSON是否完整
            if (braceCount == 0) {
                String candidate = jsonBuilder.toString().trim();
                if (candidate.endsWith("}")) {
                    try {
                        // 尝试解析验证
                        gson.fromJson(candidate, Object.class);
                        debug("readCompleteJson: JSON验证成功，总长度=" + candidate.length());
                        return candidate;
                    } catch (Exception e) {
                        // JSON仍不合法，继续读取
                        debug("readCompleteJson: JSON验证失败，继续读取: " + e.getMessage());
                    }
                }
            }
        }

        debug("readCompleteJson: 返回已读取内容（可能不完整），共 " + linesRead + " 行");
        return jsonBuilder.toString().trim();
    }

    /**
     * 计算字符串中的大括号平衡值
     * { 返回 +1, } 返回 -1
     */
    private int countBraces(String str) {
        int count = 0;
        boolean inString = false;
        boolean escape = false;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            if (escape) {
                escape = false;
                continue;
            }

            if (c == '\\') {
                escape = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (!inString) {
                if (c == '{') count++;
                else if (c == '}') count--;
            }
        }

        return count;
    }

    /**
     * 处理循环
     */
    private void processLoop() {
        logger.info("[StdioTransport.processLoop] Processing loop started");
        int processedCount = 0;
        int emptyPollCount = 0;
        long lastStatusLog = System.currentTimeMillis();

        while (running.get()) {
            try {
                String message = messageQueue.poll(100, TimeUnit.MILLISECONDS);
                if (message != null) {
                    emptyPollCount = 0; // 重置空轮询计数
                    logger.info("[StdioTransport.processLoop] Took message from queue, remaining: {}, processed: {}",
                            messageQueue.size(), processedCount + 1);
                    processMessage(message);
                    processedCount++;
                } else {
                    emptyPollCount++;
                    // 每10秒输出一次状态（如果队列为空）
                    if (System.currentTimeMillis() - lastStatusLog > 10000) {
                        logger.info("[StdioTransport.processLoop] Status - processed: {}, empty polls: {}, queue size: {}, running: {}",
                                processedCount, emptyPollCount, messageQueue.size(), running.get());
                        lastStatusLog = System.currentTimeMillis();
                        emptyPollCount = 0;
                    }
                }
            } catch (InterruptedException e) {
                logger.warn("[StdioTransport.processLoop] Processing thread interrupted");
                Thread.currentThread().interrupt();
                break;
            }
        }
        logger.info("[StdioTransport.processLoop] Processing loop exited, {} messages processed", processedCount);
    }

    /**
     * 预处理JSON消息，将字符串值内的真实换行符转为\\n转义
     * 用于兼容AI直接传入换行符而不是\\n的情况
     */
    private String preprocessJsonNewlines(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }

        // 如果JSON中不包含真实换行符，直接返回
        if (!json.contains("\n") && !json.contains("\r")) {
            return json;
        }

        // 如果JSON已经合法（可以正常解析），不需要处理
        try {
            gson.fromJson(json, Object.class);
            return json; // JSON合法，直接返回
        } catch (Exception e) {
            // JSON不合法，需要尝试修复
            debug("preprocessJsonNewlines: JSON解析失败，尝试修复换行符: " + e.getMessage());
        }

        // 逐字符处理，只替换字符串值内的换行符
        StringBuilder result = new StringBuilder();
        boolean inString = false;
        boolean escape = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escape) {
                // 当前字符被转义，直接添加
                result.append(c);
                escape = false;
                continue;
            }

            if (c == '\\') {
                // 转义字符开始
                result.append(c);
                escape = true;
                continue;
            }

            if (c == '"') {
                // 字符串边界
                inString = !inString;
                result.append(c);
                continue;
            }

            if (inString) {
                // 在字符串值内，替换换行符
                if (c == '\r') {
                    // 检查是否是\r\n组合
                    if (i + 1 < json.length() && json.charAt(i + 1) == '\n') {
                        // \\r\\n -> \\n
                        result.append('\\').append('n');
                        i++; // 跳过下一个\n
                    } else {
                        // \\r -> \\n
                        result.append('\\').append('n');
                    }
                } else if (c == '\n') {
                    // \\n -> \\n
                    result.append('\\').append('n');
                } else {
                    result.append(c);
                }
            } else {
                // 在JSON结构外（非字符串值），保留原样
                result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * 处理消息
     */
    private void processMessage(String message) {
        logger.info("[StdioTransport.processMessage] ========== Processing message ==========");
        logger.info("[StdioTransport.processMessage] Received message: {}", message);
        long startTime = System.currentTimeMillis();

        try {
            // 预处理：修复AI直接传入的真实换行符（将字符串值内的\n和\r\n转为\\n转义）
            String preprocessedMessage = preprocessJsonNewlines(message);
            if (!preprocessedMessage.equals(message)) {
                logger.info("[StdioTransport.processMessage] Preprocessed newlines, original length={}, processed length={}", 
                    message.length(), preprocessedMessage.length());
            }

            // 简化版JSON解析
            JsonRpcMessage.Request request = parseRequest(preprocessedMessage);
            if (request == null) {
                logger.error("[StdioTransport.processMessage] Message parsing returned null");
                sendErrorResponse(null, JsonRpcMessage.ERROR_PARSE_ERROR, "Failed to parse request");
                return;
            }
            logger.info("[StdioTransport.processMessage] Parsed: id={}, method={}, isNotification={}",
                    request.getId(), request.getMethod(), request.isNotification());

            if (messageHandler != null) {
                logger.info("[StdioTransport.processMessage] Calling messageHandler...");
                try {
                    messageHandler.accept(request);
                    long elapsed = System.currentTimeMillis() - startTime;
                    logger.info("[StdioTransport.processMessage] messageHandler completed, took {}ms", elapsed);
                } catch (Exception handlerEx) {
                    logger.error("[StdioTransport.processMessage] Exception in messageHandler: {}", handlerEx.getMessage(), handlerEx);
                    throw handlerEx;
                }
            } else {
                logger.warn("[StdioTransport.processMessage] messageHandler not set, cannot process request");
            }
        } catch (Exception e) {
            logger.error("[StdioTransport.processMessage] Error processing message: {}", e.getMessage(), e);
            try {
                sendErrorResponse(null, JsonRpcMessage.ERROR_PARSE_ERROR,
                        "Parse error: " + e.getMessage());
            } catch (IOException ioException) {
                logger.error("[StdioTransport.processMessage] Failed to send error response: {}", ioException.getMessage());
            }
        } finally {
            long elapsed = System.currentTimeMillis() - startTime;
            logger.info("[StdioTransport.processMessage] ========== Message processed, total {}ms ==========", elapsed);
        }
    }

    /**
     * 解析JSON-RPC请求（使用Gson标准库）
     */
    @SuppressWarnings("unchecked")
    private JsonRpcMessage.Request parseRequest(String json) {
        debug("parseRequest: 解析 JSON: " + json);
        try {
            // 使用Gson解析为Map
            Map<String, Object> map = gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());
            if (map == null) {
                debug("parseRequest: Gson解析返回null");
                return null;
            }

            // 提取 id（支持数字、字符串和null）
            Object id = map.get("id");
            debug("parseRequest: id=" + id);

            // 提取 method
            String method = (String) map.get("method");
            debug("parseRequest: method=" + method);
            if (method == null) {
                debug("parseRequest: method 为 null，返回 null");
                return null;
            }

            // 提取 params
            Map<String, Object> params = (Map<String, Object>) map.get("params");
            if (params == null) {
                params = new HashMap<>();
            }
            debug("parseRequest: params=" + params);

            JsonRpcMessage.Request request = new JsonRpcMessage.Request();
            request.setId(id);
            request.setMethod(method);
            request.setParams(params);
            return request;
        } catch (Exception e) {
            logger.error("[StdioTransport] Gson JSON parse failed: " + e.getMessage() + ", JSON: " + json);
            return null;
        }
    }

    /**
     * 从JSON中提取id（支持数字和字符串）
     */
    private Object extractJsonId(String json) {
        String quotedKey = "\"id\"";
        int keyIndex = json.indexOf(quotedKey);
        if (keyIndex == -1) {
            return null;
        }

        int pos = keyIndex + quotedKey.length();

        // 跳过空白字符
        while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
            pos++;
        }

        // 检查冒号
        if (pos >= json.length() || json.charAt(pos) != ':') {
            return null;
        }
        pos++;

        // 跳过空白字符
        while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
            pos++;
        }

        if (pos >= json.length()) {
            return null;
        }

        char firstChar = json.charAt(pos);
        if (firstChar == '"') {
            // 字符串id
            pos++;
            int valueEnd = json.indexOf('"', pos);
            while (valueEnd > pos && json.charAt(valueEnd - 1) == '\\') {
                valueEnd = json.indexOf('"', valueEnd + 1);
            }
            if (valueEnd == -1) {
                return null;
            }
            return unescapeJson(json.substring(pos, valueEnd));
        } else if (firstChar == 'n' && json.startsWith("null", pos)) {
            return null;
        } else if (firstChar == '-' || Character.isDigit(firstChar)) {
            // 数字id
            int valueEnd = pos;
            while (valueEnd < json.length() &&
                   (Character.isDigit(json.charAt(valueEnd)) ||
                    json.charAt(valueEnd) == '-' ||
                    json.charAt(valueEnd) == '+' ||
                    json.charAt(valueEnd) == '.' ||
                    json.charAt(valueEnd) == 'e' ||
                    json.charAt(valueEnd) == 'E')) {
                valueEnd++;
            }
            String numStr = json.substring(pos, valueEnd);
            try {
                return Long.parseLong(numStr);
            } catch (NumberFormatException e) {
                return numStr;
            }
        } else {
            return null;
        }
    }

    /**
     * 从JSON中提取字符串值 - 修复版：正确处理键值对
     */
    private String extractJsonValue(String json, String key) {
        // 找到 key 的位置 - key 必须用引号包围
        String quotedKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(quotedKey);
        if (keyIndex == -1) {
            return null;
        }

        int pos = keyIndex + quotedKey.length();
        
        // 跳过空白字符
        while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
            pos++;
        }
        
        // 检查冒号
        if (pos >= json.length() || json.charAt(pos) != ':') {
            return null;
        }
        pos++;
        
        // 跳过空白字符
        while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
            pos++;
        }

        if (pos >= json.length()) {
            return null;
        }

        char firstChar = json.charAt(pos);
        if (firstChar == '"') {
            // 字符串值
            pos++;
            int valueEnd = json.indexOf('"', pos);
            while (valueEnd > pos && json.charAt(valueEnd - 1) == '\\') {
                valueEnd = json.indexOf('"', valueEnd + 1);
            }
            if (valueEnd == -1) {
                return null;
            }
            return unescapeJson(json.substring(pos, valueEnd));
        } else if (firstChar == 'n' && json.startsWith("null", pos)) {
            return null;
        } else {
            // 其他值（数字等）
            int valueEnd = pos;
            while (valueEnd < json.length() &&
                   (Character.isDigit(json.charAt(valueEnd)) ||
                    json.charAt(valueEnd) == '.' ||
                    json.charAt(valueEnd) == '-' ||
                    json.charAt(valueEnd) == 'e' ||
                    json.charAt(valueEnd) == 'E')) {
                valueEnd++;
            }
            if (valueEnd > pos) {
                return json.substring(pos, valueEnd);
            }
            return null;
        }
    }

    /**
     * 从JSON中提取对象
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractJsonObject(String json, String key) {
        String quotedKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(quotedKey);
        if (keyIndex == -1) {
            return null;
        }

        int pos = keyIndex + quotedKey.length();
        
        // 跳过空白字符
        while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
            pos++;
        }
        
        // 检查冒号
        if (pos >= json.length() || json.charAt(pos) != ':') {
            return null;
        }
        pos++;
        
        // 跳过空白字符
        while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
            pos++;
        }

        if (pos >= json.length() || json.charAt(pos) != '{') {
            return null;
        }

        // 找到匹配的右括号
        int braceCount = 0;
        int valueEnd = pos;
        boolean inString = false;

        for (int i = pos; i < json.length(); i++) {
            char c = json.charAt(i);

            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                inString = !inString;
            } else if (!inString) {
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        valueEnd = i + 1;
                        break;
                    }
                }
            }
        }

        if (valueEnd <= pos) {
            return null;
        }

        String objectJson = json.substring(pos, valueEnd);
        return parseJsonObject(objectJson);
    }

    /**
     * 解析简单JSON对象
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonObject(String json) {
        Map<String, Object> result = new HashMap<>();

        if (!json.startsWith("{") || !json.endsWith("}")) {
            return result;
        }

        String content = json.substring(1, json.length() - 1).trim();
        if (content.isEmpty()) {
            return result;
        }

        // 简单的键值对解析
        int i = 0;
        while (i < content.length()) {
            // 跳过空白
            while (i < content.length() && Character.isWhitespace(content.charAt(i))) {
                i++;
            }

            if (i >= content.length()) break;

            // 解析键
            if (content.charAt(i) != '"') {
                // 跳过直到找到逗号或结束
                while (i < content.length() && content.charAt(i) != ',') {
                    i++;
                }
                if (i < content.length()) i++;
                continue;
            }

            int keyStart = i + 1;
            int keyEnd = content.indexOf('"', keyStart);
            while (keyEnd > keyStart && content.charAt(keyEnd - 1) == '\\') {
                keyEnd = content.indexOf('"', keyEnd + 1);
            }
            if (keyEnd == -1) break;

            String key = unescapeJson(content.substring(keyStart, keyEnd));
            i = keyEnd + 1;

            // 跳过空白和冒号
            while (i < content.length() && (Character.isWhitespace(content.charAt(i)) || content.charAt(i) == ':')) {
                i++;
            }

            if (i >= content.length()) break;

            // 解析值
            Object value = parseJsonValue(content, i);
            result.put(key, value);

            // 跳过已解析的部分（正确追踪括号深度）
            int depth = 0;
            boolean inStr = false;
            while (i < content.length()) {
                char c = content.charAt(i);
                if (c == '"' && (i == 0 || content.charAt(i - 1) != '\\')) {
                    inStr = !inStr;
                } else if (!inStr) {
                    if (c == '{' || c == '[') depth++;
                    else if (c == '}' || c == ']') depth--;
                    else if (c == ',' && depth == 0) {
                        i++;  // 跳过逗号
                        break;
                    }
                }
                i++;
            }
        }

        return result;
    }

    /**
     * 解析JSON数组
     */
    @SuppressWarnings("unchecked")
    private List<Object> parseJsonArray(String json) {
        List<Object> result = new ArrayList<>();

        if (!json.startsWith("[") || !json.endsWith("]")) {
            return result;
        }

        String content = json.substring(1, json.length() - 1).trim();
        if (content.isEmpty()) {
            return result;
        }

        int i = 0;
        while (i < content.length()) {
            // 跳过空白
            while (i < content.length() && Character.isWhitespace(content.charAt(i))) {
                i++;
            }
            if (i >= content.length()) break;

            // 解析值
            Object value = parseJsonValue(content, i);
            result.add(value);

            // 跳过已解析的部分，跳到下一个逗号或结束
            while (i < content.length() && content.charAt(i) != ',') {
                i++;
            }
            if (i < content.length() && content.charAt(i) == ',') {
                i++;
            }
        }

        return result;
    }

    /**
     * 解析JSON值
     */
    private Object parseJsonValue(String json, int start) {
        if (start >= json.length()) {
            return null;
        }

        char firstChar = json.charAt(start);

        if (firstChar == '"') {
            // 字符串
            int end = json.indexOf('"', start + 1);
            while (end > start + 1 && json.charAt(end - 1) == '\\') {
                end = json.indexOf('"', end + 1);
            }
            if (end == -1) return null;
            return unescapeJson(json.substring(start + 1, end));
        } else if (firstChar == '{') {
            // 对象 - 简化处理
            int braceCount = 1;
            int end = start + 1;
            while (end < json.length() && braceCount > 0) {
                if (json.charAt(end) == '{') braceCount++;
                else if (json.charAt(end) == '}') braceCount--;
                end++;
            }
            return parseJsonObject(json.substring(start, end));
        } else if (firstChar == '[') {
            // 数组 - 解析为List
            int bracketCount = 1;
            int end = start + 1;
            while (end < json.length() && bracketCount > 0) {
                if (json.charAt(end) == '[') bracketCount++;
                else if (json.charAt(end) == ']') bracketCount--;
                end++;
            }
            return parseJsonArray(json.substring(start, end));
        } else if (json.startsWith("null", start)) {
            return null;
        } else if (json.startsWith("true", start)) {
            return Boolean.TRUE;
        } else if (json.startsWith("false", start)) {
            return Boolean.FALSE;
        } else {
            // 数字
            int end = start;
            while (end < json.length() &&
                   (Character.isDigit(json.charAt(end)) ||
                    json.charAt(end) == '.' ||
                    json.charAt(end) == '-' ||
                    json.charAt(end) == 'e' ||
                    json.charAt(end) == 'E' ||
                    json.charAt(end) == '+')) {
                end++;
            }
            String numStr = json.substring(start, end);
            try {
                if (numStr.contains(".") || numStr.contains("e") || numStr.contains("E")) {
                    return Double.parseDouble(numStr);
                } else {
                    return Long.parseLong(numStr);
                }
            } catch (NumberFormatException e) {
                return numStr;
            }
        }
    }

    /**
     * 反转义JSON字符串
     * 注意：不处理 \b 和 \f，避免Windows路径中的\b被解析为退格符
     * 使用逐字符处理方式，避免replace全局替换导致的二次转义问题
     */
    private String unescapeJson(String s) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case '"':
                        result.append('"');
                        i += 2;
                        break;
                    case '\\':
                        result.append('\\');
                        i += 2;
                        break;
                    case 'n':
                        result.append('\n');
                        i += 2;
                        break;
                    case 'r':
                        result.append('\r');
                        i += 2;
                        break;
                    case 't':
                        result.append('\t');
                        i += 2;
                        break;
                    default:
                        // 未知的转义序列，保留原样
                        result.append(c);
                        i++;
                        break;
                }
            } else {
                result.append(c);
                i++;
            }
        }
        return result.toString();
    }

    @Override
    public void sendResponse(JsonRpcMessage.Response response) throws IOException {
        String json = serializeResponse(response);
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        int totalBytesToSend = jsonBytes.length + (useContentLength ? 0 : 1); // +1 for newline in non-ContentLength mode

        logger.info("[StdioTransport.sendResponse] ========== Sending response ==========");
        logger.info("[StdioTransport.sendResponse] Response ID: {}, isError: {}", response.getId(), response.isError());
        logger.info("[StdioTransport.sendResponse] JSON length: {} bytes", jsonBytes.length);
        logger.info("[StdioTransport.sendResponse] Using ContentLength mode: {}", useContentLength);

        // 记录响应内容摘要（前200字符）
        String jsonPreview = json.length() > 200 ? json.substring(0, 200) + "..." : json;
        logger.info("[StdioTransport.sendResponse] Response summary: {}", jsonPreview);

        synchronized (outputStream) {
            try {
                if (useContentLength) {
                    // 标准MCP协议：使用Content-Length头部
                    byte[] header = ("Content-Length: " + jsonBytes.length + "\n\n").getBytes(StandardCharsets.UTF_8);
                    outputStream.write(header);
                    logger.info("[StdioTransport.sendResponse] Content-Length header written: {} bytes", header.length);
                } else {
                    // 兼容模式：直接发送JSON（Qwen等客户端）
                    logger.info("[StdioTransport.sendResponse] Using plain JSON mode (no Content-Length)");
                }

                // 写入JSON内容
                outputStream.write(jsonBytes);
                outputStream.write('\n');  // 每个消息后加换行符
                logger.info("[StdioTransport.sendResponse] JSON content and newline written");

                // 强制刷新输出流
                outputStream.flush();
                logger.info("[StdioTransport.sendResponse] Output stream flushed");

                // 检查System.out是否出错
                if (System.out.checkError()) {
                    logger.error("[StdioTransport.sendResponse] System.out error detected!");
                } else {
                    logger.info("[StdioTransport.sendResponse] System.out status OK");
                }

                logger.info("[StdioTransport.sendResponse] ========== Response sent ==========");
                logger.info("[StdioTransport.sendResponse] Total bytes sent (estimated): {} bytes", totalBytesToSend);
            } catch (IOException e) {
                logger.error("[StdioTransport.sendResponse] IO error while sending response: {}", e.getMessage(), e);
                throw e;
            }
        }
    }

    /**
     * 序列化响应
     */
    private String serializeResponse(JsonRpcMessage.Response response) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"jsonrpc\":\"").append(response.getJsonrpc()).append("\",");

        // id可以是数字或字符串，需要根据类型决定是否加引号
        Object id = response.getId();
        if (id != null) {
            if (id instanceof Number) {
                sb.append("\"id\":").append(id).append(",");
            } else {
                sb.append("\"id\":\"").append(escapeJson(id.toString())).append("\",");
            }
        } else {
            sb.append("\"id\":null,");
        }

        if (response.isError()) {
            JsonRpcMessage.JsonRpcError error = response.getError();
            sb.append("\"error\":{");
            sb.append("\"code\":").append(error.getCode()).append(",");
            sb.append("\"message\":\"").append(escapeJson(error.getMessage())).append("\"");
            if (error.getData() != null) {
                sb.append(",\"data\":").append(serializeValue(error.getData()));
            }
            sb.append("}");
        } else {
            sb.append("\"result\":").append(serializeValue(response.getResult()));
        }

        sb.append("}");
        return sb.toString();
    }

    /**
     * 序列化值为JSON
     */
    private String serializeValue(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            return "\"" + escapeJson((String) value) + "\"";
        } else if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof Map) {
            return serializeMap((Map<String, Object>) value);
        } else if (value instanceof List) {
            return serializeList((List<?>) value);
        } else if (value instanceof InitializeResult) {
            return serializeInitializeResult((InitializeResult) value);
        } else if (value instanceof ServerInfo) {
            return serializeServerInfo((ServerInfo) value);
        } else if (value instanceof Capabilities) {
            return serializeCapabilities((Capabilities) value);
        } else if (value instanceof ListToolsResult) {
            return serializeListToolsResult((ListToolsResult) value);
        } else if (value instanceof ListPromptsResult) {
            return serializeListPromptsResult((ListPromptsResult) value);
        } else if (value instanceof ListResourcesResult) {
            return serializeListResourcesResult((ListResourcesResult) value);
        } else if (value instanceof Tool) {
            return serializeTool((Tool) value);
        } else if (value instanceof CallToolResult) {
            return serializeCallToolResult((CallToolResult) value);
        } else if (value instanceof ToolContent) {
            return serializeToolContent((ToolContent) value);
        } else if (value instanceof ToolInputSchema) {
            return serializeToolInputSchema((ToolInputSchema) value);
        } else {
            return "\"" + escapeJson(value.toString()) + "\"";
        }
    }

    /**
     * 序列化 InitializeResult
     */
    private String serializeInitializeResult(InitializeResult result) {
        if (result == null) return "null";
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"protocolVersion\":\"").append(escapeJson(result.getProtocolVersion())).append("\",");
        sb.append("\"capabilities\":").append(serializeCapabilities(result.getCapabilities())).append(",");
        sb.append("\"serverInfo\":").append(serializeServerInfo(result.getServerInfo()));
        sb.append("}");
        return sb.toString();
    }

    /**
     * 序列化 ServerInfo
     */
    private String serializeServerInfo(ServerInfo info) {
        if (info == null) return "null";
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"name\":\"").append(escapeJson(info.getName())).append("\",");
        sb.append("\"version\":\"").append(escapeJson(info.getVersion())).append("\"");
        sb.append("}");
        return sb.toString();
    }

    /**
     * 序列化 Capabilities
     * MCP协议要求capabilities中的字段是对象格式，包含特定的子字段
     */
    private String serializeCapabilities(Capabilities caps) {
        if (caps == null) return "null";
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        // MCP协议要求：tools能力对象包含listChanged字段
        if (caps.isTools()) {
            if (!first) sb.append(",");
            sb.append("\"tools\":{\"listChanged\":true}");
            first = false;
        }
        // resources能力对象包含subscribe和listChanged字段
        if (caps.isResources()) {
            if (!first) sb.append(",");
            sb.append("\"resources\":{\"subscribe\":true,\"listChanged\":true}");
            first = false;
        }
        // prompts能力对象包含listChanged字段
        if (caps.isPrompts()) {
            if (!first) sb.append(",");
            sb.append("\"prompts\":{\"listChanged\":true}");
            first = false;
        }
        // logging能力对象为空对象
        if (caps.isLogging()) {
            if (!first) sb.append(",");
            sb.append("\"logging\":{}");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * 序列化 ListToolsResult
     */
    private String serializeListToolsResult(ListToolsResult result) {
        if (result == null || result.getTools() == null) return "{\"tools\":[]}";
        StringBuilder sb = new StringBuilder();
        sb.append("{\"tools\":[");
        List<Tool> tools = result.getTools();
        for (int i = 0; i < tools.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(serializeTool(tools.get(i)));
        }
        sb.append("]}");
        return sb.toString();
    }

    /**
     * 序列化 ListPromptsResult
     */
    private String serializeListPromptsResult(ListPromptsResult result) {
        if (result == null || result.getPrompts() == null) return "{\"prompts\":[]}";
        StringBuilder sb = new StringBuilder();
        sb.append("{\"prompts\":[");
        List<Prompt> prompts = result.getPrompts();
        for (int i = 0; i < prompts.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(serializePrompt(prompts.get(i)));
        }
        sb.append("]}");
        return sb.toString();
    }

    /**
     * 序列化 Prompt
     */
    private String serializePrompt(Prompt prompt) {
        if (prompt == null) return "null";
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"name\":\"").append(escapeJson(prompt.getName())).append("\",");
        sb.append("\"description\":\"").append(escapeJson(prompt.getDescription())).append("\"");
        if (prompt.getArguments() != null) {
            sb.append(",\"arguments\":").append(serializePromptArguments(prompt.getArguments()));
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * 序列化 PromptArgument列表
     */
    private String serializePromptArguments(List<PromptArgument> arguments) {
        if (arguments == null || arguments.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < arguments.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(serializePromptArgument(arguments.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 序列化 PromptArgument
     */
    private String serializePromptArgument(PromptArgument arg) {
        if (arg == null) return "null";
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"name\":\"").append(escapeJson(arg.getName())).append("\",");
        sb.append("\"description\":\"").append(escapeJson(arg.getDescription())).append("\",");
        sb.append("\"required\":").append(arg.isRequired());
        sb.append("}");
        return sb.toString();
    }

    /**
     * 序列化 ListResourcesResult
     */
    private String serializeListResourcesResult(ListResourcesResult result) {
        if (result == null || result.getResources() == null) return "{\"resources\":[]}";
        StringBuilder sb = new StringBuilder();
        sb.append("{\"resources\":[");
        List<Resource> resources = result.getResources();
        for (int i = 0; i < resources.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(serializeResource(resources.get(i)));
        }
        sb.append("]}");
        return sb.toString();
    }

    /**
     * 序列化 Resource
     */
    private String serializeResource(Resource resource) {
        if (resource == null) return "null";
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"uri\":\"").append(escapeJson(resource.getUri())).append("\",");
        sb.append("\"name\":\"").append(escapeJson(resource.getName())).append("\"");
        if (resource.getDescription() != null) {
            sb.append(",\"description\":\"").append(escapeJson(resource.getDescription())).append("\"");
        }
        if (resource.getMimeType() != null) {
            sb.append(",\"mimeType\":\"").append(escapeJson(resource.getMimeType())).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * 序列化 Tool
     */
    private String serializeTool(Tool tool) {
        if (tool == null) return "null";
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"name\":\"").append(escapeJson(tool.getName())).append("\",");
        sb.append("\"description\":\"").append(escapeJson(tool.getDescription())).append("\",");
        sb.append("\"inputSchema\":").append(serializeToolInputSchema(tool.getInputSchema()));
        sb.append("}");
        return sb.toString();
    }

    /**
     * 序列化 ToolInputSchema
     */
    private String serializeToolInputSchema(ToolInputSchema schema) {
        if (schema == null) return "null";
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"type\":\"").append(escapeJson(schema.getType())).append("\"");
        if (schema.getProperties() != null) {
            sb.append(",\"properties\":").append(serializeMap(schema.getProperties()));
        }
        if (schema.getRequired() != null) {
            sb.append(",\"required\":").append(serializeList(schema.getRequired()));
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * 序列化 CallToolResult
     */
    private String serializeCallToolResult(CallToolResult result) {
        if (result == null) return "null";
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"content\":[");
        if (result.getContent() != null) {
            List<ToolContent> content = result.getContent();
            for (int i = 0; i < content.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(serializeToolContent(content.get(i)));
            }
        }
        sb.append("],");
        sb.append("\"isError\":").append(result.isError());
        sb.append("}");
        return sb.toString();
    }

    /**
     * 序列化 ToolContent
     */
    private String serializeToolContent(ToolContent content) {
        if (content == null) return "null";
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"type\":\"").append(escapeJson(content.getType())).append("\",");
        sb.append("\"text\":\"").append(escapeJson(content.getText())).append("\"");
        sb.append("}");
        return sb.toString();
    }

    /**
     * 序列化Map
     */
    private String serializeMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{");

        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("\"").append(entry.getKey()).append("\":");
            sb.append(serializeValue(entry.getValue()));
        }

        sb.append("}");
        return sb.toString();
    }

    /**
     * 序列化List
     */
    private String serializeList(List<?> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[");

        boolean first = true;
        for (Object item : list) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append(serializeValue(item));
        }

        sb.append("]");
        return sb.toString();
    }

    /**
     * 转义JSON字符串
     */
    private String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            // 处理需要转义的字符
            if (c == '"') {
                sb.append("\\\"");
            } else if (c == '\\') {
                sb.append("\\\\");
            } else if (c == '\b') {
                sb.append("\\b");
            } else if (c == '\f') {
                sb.append("\\f");
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c == '\t') {
                sb.append("\\t");
            } else if (c < 0x20) {
                // 其他控制字符使用 \\uXXXX 格式
                sb.append('\\').append('u').append(String.format("%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(String id, int code, String message) throws IOException {
        JsonRpcMessage.Response response = JsonRpcMessage.Response.error(id, code, message);
        sendResponse(response);
    }

    @Override
    public void setMessageHandler(Consumer<JsonRpcMessage.Request> handler) {
        this.messageHandler = handler;
    }
}

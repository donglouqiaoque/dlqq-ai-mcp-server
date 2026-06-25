package com.donglouqiaoque.aitools.mcp.transport;

import com.donglouqiaoque.aitools.mcp.protocol.JsonRpcMessage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 标准输入输出传输层实现
 * 遵循MCP协议规范，使用Content-Length头分隔消息
 */
public class StdioTransport implements McpTransport {

    private final BufferedInputStream inputStream;
    private final BufferedOutputStream outputStream;
    private Consumer<JsonRpcMessage.Request> messageHandler;
    private final ExecutorService executor;
    private final AtomicBoolean running;
    private final BlockingQueue<String> messageQueue;
    private volatile boolean useContentLength = true;  // 是否使用Content-Length协议

    public StdioTransport() {
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
            java.nio.charset.Charset.defaultCharset();
        } catch (Exception ignored) {
        }
    }
    
    private void debug(String msg) {
        // 启用调试输出到stderr
        System.err.println("[MCP] " + msg);
    }

    @Override
    public void start() throws IOException {
        running.set(true);

        // 启动读取线程
        executor.submit(this::readLoop);

        // 启动处理线程
        executor.submit(this::processLoop);
    }
    
    /**
     * 检查传输层是否正在运行
     * 用于健康检查
     */
    public boolean isRunning() {
        return running.get() && !executor.isShutdown() && !executor.isTerminated();
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
        int errorCount = 0;
        final int MAX_ERROR_COUNT = 5;  // 最大连续错误次数
        
        while (running.get()) {
            try {
                String message = readMessage();
                if (message != null) {
                    messageQueue.offer(message);
                    errorCount = 0;  // 重置错误计数
                } else {
                    // EOF 或读取失败 - 可能是父进程暂时断开
                    errorCount++;
                    if (errorCount >= MAX_ERROR_COUNT) {
                        debug("readLoop: 连续 " + MAX_ERROR_COUNT + " 次读取失败，退出循环");
                        break;
                    }
                    debug("readLoop: 读取到null (EOF?)，重试第 " + errorCount + "/" + MAX_ERROR_COUNT + " 次");
                    try {
                        Thread.sleep(100);  // 短暂等待后重试
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (IOException e) {
                if (running.get()) {
                    System.err.println("Error reading from stdin: " + e.getMessage());
                }
                errorCount++;
                if (errorCount >= MAX_ERROR_COUNT) {
                    debug("readLoop: 连续 " + MAX_ERROR_COUNT + " 次IO错误，退出循环");
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
        debug("readLoop: 读取循环已退出");
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
            debug("readMessage: 检测到直接JSON消息（无Content-Length），切换到纯JSON模式");
            useContentLength = false;  // 客户端不使用Content-Length，服务器也不使用
            return firstLine;
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
     * 处理循环
     */
    private void processLoop() {
        while (running.get()) {
            try {
                String message = messageQueue.poll(100, TimeUnit.MILLISECONDS);
                if (message != null) {
                    processMessage(message);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * 处理消息
     */
    private void processMessage(String message) {
        debug("processMessage: 收到消息: " + message);
        try {
            // 简化版JSON解析
            JsonRpcMessage.Request request = parseRequest(message);
            if (request == null) {
                debug("processMessage: 解析返回 null");
                sendErrorResponse(null, JsonRpcMessage.ERROR_PARSE_ERROR, "Failed to parse request");
                return;
            }
            debug("processMessage: 解析后的请求: id=" + request.getId() + ", method=" + request.getMethod());
            if (messageHandler != null) {
                messageHandler.accept(request);
            }
        } catch (Exception e) {
            debug("processMessage: 错误: " + e.getMessage());
            try {
                sendErrorResponse(null, JsonRpcMessage.ERROR_PARSE_ERROR,
                        "Parse error: " + e.getMessage());
            } catch (IOException ioException) {
                debug("Failed to send error response: " + ioException.getMessage());
            }
        }
    }

    /**
     * 解析JSON-RPC请求（简化实现）
     */
    @SuppressWarnings("unchecked")
    private JsonRpcMessage.Request parseRequest(String json) {
        debug("parseRequest: 解析 JSON: " + json);
        // 提取 id（支持数字和字符串）
        Object id = extractJsonId(json);
        debug("parseRequest: id=" + id);

        // 提取 method
        String method = extractJsonValue(json, "method");
        debug("parseRequest: method=" + method);
        if (method == null) {
            debug("parseRequest: method 为 null，返回 null");
            return null;
        }

        // 提取 params
        Map<String, Object> params = extractJsonObject(json, "params");
        debug("parseRequest: params=" + params);

        JsonRpcMessage.Request request = new JsonRpcMessage.Request();
        request.setId(id);
        request.setMethod(method);
        request.setParams(params);
        return request;
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
        Map<String, Object> result = new java.util.HashMap<>();

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
        List<Object> result = new java.util.ArrayList<>();

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
        debug("sendResponse: 发送响应: " + json);
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        synchronized (outputStream) {
            if (useContentLength) {
                // 标准MCP协议：使用Content-Length头部
                outputStream.write("Content-Length: ".getBytes(StandardCharsets.UTF_8));
                outputStream.write(String.valueOf(jsonBytes.length).getBytes(StandardCharsets.UTF_8));
                outputStream.write('\n');
                outputStream.write('\n');
                debug("sendResponse: 使用Content-Length协议发送");
            } else {
                // 兼容模式：直接发送JSON（Qwen等客户端）
                debug("sendResponse: 使用纯JSON模式发送");
            }
            outputStream.write(jsonBytes);
            outputStream.write('\n');  // 每个消息后加换行符
            outputStream.flush();
        }
        debug("sendResponse: 响应已发送");
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
        } else if (value instanceof java.util.List) {
            return serializeList((java.util.List<?>) value);
        } else if (value instanceof com.donglouqiaoque.aitools.mcp.protocol.McpSchema.InitializeResult) {
            return serializeInitializeResult((com.donglouqiaoque.aitools.mcp.protocol.McpSchema.InitializeResult) value);
        } else if (value instanceof com.donglouqiaoque.aitools.mcp.protocol.McpSchema.ServerInfo) {
            return serializeServerInfo((com.donglouqiaoque.aitools.mcp.protocol.McpSchema.ServerInfo) value);
        } else if (value instanceof com.donglouqiaoque.aitools.mcp.protocol.McpSchema.Capabilities) {
            return serializeCapabilities((com.donglouqiaoque.aitools.mcp.protocol.McpSchema.Capabilities) value);
        } else if (value instanceof com.donglouqiaoque.aitools.mcp.protocol.McpSchema.ListToolsResult) {
            return serializeListToolsResult((com.donglouqiaoque.aitools.mcp.protocol.McpSchema.ListToolsResult) value);
        } else if (value instanceof com.donglouqiaoque.aitools.mcp.protocol.McpSchema.Tool) {
            return serializeTool((com.donglouqiaoque.aitools.mcp.protocol.McpSchema.Tool) value);
        } else if (value instanceof com.donglouqiaoque.aitools.mcp.protocol.McpSchema.CallToolResult) {
            return serializeCallToolResult((com.donglouqiaoque.aitools.mcp.protocol.McpSchema.CallToolResult) value);
        } else if (value instanceof com.donglouqiaoque.aitools.mcp.protocol.McpSchema.ToolContent) {
            return serializeToolContent((com.donglouqiaoque.aitools.mcp.protocol.McpSchema.ToolContent) value);
        } else if (value instanceof com.donglouqiaoque.aitools.mcp.protocol.McpSchema.ToolInputSchema) {
            return serializeToolInputSchema((com.donglouqiaoque.aitools.mcp.protocol.McpSchema.ToolInputSchema) value);
        } else {
            return "\"" + escapeJson(value.toString()) + "\"";
        }
    }

    /**
     * 序列化 InitializeResult
     */
    private String serializeInitializeResult(com.donglouqiaoque.aitools.mcp.protocol.McpSchema.InitializeResult result) {
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
    private String serializeServerInfo(com.donglouqiaoque.aitools.mcp.protocol.McpSchema.ServerInfo info) {
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
    private String serializeCapabilities(com.donglouqiaoque.aitools.mcp.protocol.McpSchema.Capabilities caps) {
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
    private String serializeListToolsResult(com.donglouqiaoque.aitools.mcp.protocol.McpSchema.ListToolsResult result) {
        if (result == null || result.getTools() == null) return "{\"tools\":[]}";
        StringBuilder sb = new StringBuilder();
        sb.append("{\"tools\":[");
        List<com.donglouqiaoque.aitools.mcp.protocol.McpSchema.Tool> tools = result.getTools();
        for (int i = 0; i < tools.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(serializeTool(tools.get(i)));
        }
        sb.append("]}");
        return sb.toString();
    }

    /**
     * 序列化 Tool
     */
    private String serializeTool(com.donglouqiaoque.aitools.mcp.protocol.McpSchema.Tool tool) {
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
    private String serializeToolInputSchema(com.donglouqiaoque.aitools.mcp.protocol.McpSchema.ToolInputSchema schema) {
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
    private String serializeCallToolResult(com.donglouqiaoque.aitools.mcp.protocol.McpSchema.CallToolResult result) {
        if (result == null) return "null";
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"content\":[");
        if (result.getContent() != null) {
            List<com.donglouqiaoque.aitools.mcp.protocol.McpSchema.ToolContent> content = result.getContent();
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
    private String serializeToolContent(com.donglouqiaoque.aitools.mcp.protocol.McpSchema.ToolContent content) {
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
    private String serializeList(java.util.List<?> list) {
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
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\b", "\\b")
                .replace("\f", "\\f");
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

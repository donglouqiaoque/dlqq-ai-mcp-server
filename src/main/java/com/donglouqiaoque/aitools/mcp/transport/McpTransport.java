package com.donglouqiaoque.aitools.mcp.transport;

import com.donglouqiaoque.aitools.mcp.protocol.JsonRpcMessage;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * MCP传输层接口
 */
public interface McpTransport {

    /**
     * 启动传输层
     */
    void start() throws IOException;

    /**
     * 停止传输层
     */
    void stop() throws IOException;

    /**
     * 发送响应
     *
     * @param response JSON-RPC响应
     * @throws IOException IO异常
     */
    void sendResponse(JsonRpcMessage.Response response) throws IOException;

    /**
     * 设置消息处理器
     *
     * @param handler 消息处理器
     */
    void setMessageHandler(Consumer<JsonRpcMessage.Request> handler);
}

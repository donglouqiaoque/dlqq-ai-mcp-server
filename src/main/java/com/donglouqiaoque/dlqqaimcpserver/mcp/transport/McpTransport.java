package com.donglouqiaoque.dlqqaimcpserver.mcp.transport;

import com.donglouqiaoque.dlqqaimcpserver.mcp.protocol.JsonRpcMessage;

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

    /**
     * 检查传输层是否正在运行（用于健康检查）
     *
     * @return true 表示运行中
     */
    default boolean isRunning() {
        return true;
    }

    /**
     * 发送 notifications/tools/list_changed 通知，让客户端重新拉取工具列表。
     *
     * <p>HTTP 传输会在已打开的 SSE 长连接上广播；stdio 传输直接写 stdout。
     * 默认实现不做任何事（返回 false）。</p>
     *
     * @return true 表示至少向一个客户端通道发送成功
     */
    default boolean notifyToolsListChanged() {
        return false;
    }
}

package com.donglouqiaoque.aitools.mcp;

import com.donglouqiaoque.aitools.mcp.server.McpServer;
import com.donglouqiaoque.aitools.mcp.tools.AITools;
import com.donglouqiaoque.aitools.mcp.transport.StdioTransport;

import java.io.IOException;

/**
 * MCP服务器入口
 */
public class MCPServerMain {

    public static void main(String[] args) {
        // 在最早阶段设置UTF-8编码
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("sun.stdout.encoding", "UTF-8");
        System.setProperty("sun.stderr.encoding", "UTF-8");

        // 重新初始化标准输入输出流的编码
        try {
            System.setOut(new java.io.PrintStream(System.out, true, "UTF-8"));
            System.setErr(new java.io.PrintStream(System.err, true, "UTF-8"));
        } catch (Exception ignored) {
        }

        System.err.println("MCP Server starting...");
        System.err.println("Working directory: " + System.getProperty("user.dir"));
        System.err.println("Default charset: " + java.nio.charset.Charset.defaultCharset().name());
        try {
            // 创建传输层（stdio）
            StdioTransport transport = new StdioTransport();
            System.err.println("Transport created");

            // 创建MCP服务器
            McpServer server = new McpServer(
                "donglouqiaoque-ai-tools",
                "1.0.0",
                transport
            );
            System.err.println("Server created");

            // 注册AI工具
            AITools aiTools = new AITools();
            aiTools.registerTools(server);
            System.err.println("Tools registered");

            // 启动服务器
            server.start();
            System.err.println("Server started, waiting for requests...");
            
            // 阻塞主线程，保持进程运行
            Thread.currentThread().join();

        } catch (IOException e) {
            System.err.println("MCP服务器启动失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (InterruptedException e) {
            System.err.println("MCP服务器被中断");
            System.exit(0);
        }
    }
}

package com.donglouqiaoque.aitools.mcp;

import com.donglouqiaoque.aitools.mcp.server.McpServer;
import com.donglouqiaoque.aitools.mcp.tools.AITools;
import com.donglouqiaoque.aitools.mcp.transport.StdioTransport;

import java.io.IOException;

/**
 * MCP服务器入口
 */
public class MCPServerMain {

    // 全局变量用于健康检查
    private static volatile boolean isRunning = true;
    private static StdioTransport transport;
    private static McpServer server;

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
            startServer();
            
            // 启动健康检查线程
            startHealthCheckThread();
            
            // 主线程保持运行
            while (isRunning) {
                Thread.sleep(1000);
            }
            
        } catch (IOException e) {
            System.err.println("MCP服务器启动失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (InterruptedException e) {
            System.err.println("MCP服务器被中断");
            isRunning = false;
        } finally {
            cleanup();
        }
    }
    
    /**
     * 启动MCP服务器
     */
    private static void startServer() throws IOException {
        // 创建传输层（stdio）
        transport = new StdioTransport();
        System.err.println("Transport created");

        // 创建MCP服务器
        server = new McpServer(
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
    }
    
    /**
     * 清理资源
     */
    private static void cleanup() {
        System.err.println("MCP Server cleaning up...");
        isRunning = false;
        if (server != null) {
            try {
                server.stop();
            } catch (IOException e) {
                System.err.println("Error stopping server: " + e.getMessage());
            }
        }
        System.err.println("MCP Server exited");
    }
    
    /**
     * 启动健康检查线程
     * 监控服务器状态，如果传输层断开则尝试恢复
     */
    private static void startHealthCheckThread() {
        Thread healthCheck = new Thread(() -> {
            int checkInterval = 5000;  // 每5秒检查一次
            int errorCount = 0;
            final int MAX_ERROR_COUNT = 3;
            
            while (isRunning) {
                try {
                    Thread.sleep(checkInterval);
                    
                    // 检查传输层是否还在运行
                    if (transport != null && !transport.isRunning()) {
                        System.err.println("[HealthCheck] 传输层已停止运行!");
                        errorCount++;
                        
                        if (errorCount >= MAX_ERROR_COUNT) {
                            System.err.println("[HealthCheck] 连续 " + MAX_ERROR_COUNT + " 次检查失败，准备重启...");
                            try {
                                // 停止旧服务器
                                if (server != null) {
                                    server.stop();
                                }
                                // 重新启动
                                startServer();
                                errorCount = 0;
                                System.err.println("[HealthCheck] 服务器重启成功");
                            } catch (IOException e) {
                                System.err.println("[HealthCheck] 重启失败: " + e.getMessage());
                            }
                        }
                    } else {
                        errorCount = 0;  // 重置错误计数
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            System.err.println("[HealthCheck] 健康检查线程已退出");
        });
        healthCheck.setDaemon(true);
        healthCheck.setName("MCP-HealthCheck");
        healthCheck.start();
        System.err.println("Health check thread started");
    }
}

package com.donglouqiaoque.aitools.mcp;

import com.donglouqiaoque.aitools.mcp.server.McpServer;
import com.donglouqiaoque.aitools.mcp.tools.AITools;
import com.donglouqiaoque.aitools.mcp.transport.StdioTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * MCP服务器入口
 */
public class MCPServerMain {

    // 在静态初始化块中先创建目录和设置 stderr 重定向（在 Logger 初始化之前）
    static {
        try {
            // 生成当前时间字符串（格式：yyyy-MM-dd_HH-mm-ss），每次启动都创建新文件
            String dateStr = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

            // 创建日志目录结构（必须在 Logger 初始化之前完成）
            java.io.File logDir = new java.io.File(".dlqq/log");
            java.io.File loggerDir = new java.io.File(logDir, "logger");
            java.io.File stderrDir = new java.io.File(logDir, "stderr");

            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            if (!loggerDir.exists()) {
                loggerDir.mkdirs();
            }
            if (!stderrDir.exists()) {
                stderrDir.mkdirs();
            }

            // 创建 settings 目录
            java.io.File settingsDir = new java.io.File(logDir.getParentFile(), "settings");
            if (!settingsDir.exists()) {
                settingsDir.mkdirs();
            }

            // 创建默认的 settings.properties 文件
            java.io.File settingsFile = new java.io.File(settingsDir, "settings.properties");
            if (!settingsFile.exists()) {
                try (java.io.FileWriter writer = new java.io.FileWriter(settingsFile)) {
                    writer.write("# MCP Server Settings\n");
                    writer.write("# ==================\n\n");
                    writer.write("# Response size limits (bytes)\n");
                    writer.write("response.max.size=32768\n\n");
                    writer.write("# Project build scripts\n");
                    writer.write("# Scripts for build/rebuild/erase/download/run/debug operations\n");
                    writer.write("# 脚本路径为空表示未配置，执行时会提示需要先设置\n");
                    writer.write("script.build=\n");
                    writer.write("script.rebuild=\n");
                    writer.write("script.chip.erase=\n");
                    writer.write("script.chip.download=\n");
                    writer.write("script.erase.download=\n");
                    writer.write("script.build.erase.download=\n");
                    writer.write("script.rebuild.erase.download=\n");
                    writer.write("script.run=\n");
                    writer.write("script.debug=\n");
                    writer.write("script.build.run=\n");
                    writer.write("script.build.debug=\n");
                    writer.write("script.rebuild.run=\n");
                    writer.write("script.rebuild.debug=\n");
                }
            }

            // 创建 .dlqq/.gitignore 文件，忽略该目录下的所有内容
            java.io.File gitignoreFile = new java.io.File(logDir.getParentFile(), ".gitignore");
            if (!gitignoreFile.exists()) {
                try (java.io.FileWriter writer = new java.io.FileWriter(gitignoreFile)) {
                    writer.write("*\n");
                }
            }

            // 尝试将 .dlqq 目录设置为隐藏（Windows），失败则忽略
            try {
                java.nio.file.Path dlqqPath = logDir.getParentFile().toPath();
                java.nio.file.attribute.DosFileAttributeView dosView = java.nio.file.Files.getFileAttributeView(
                        dlqqPath, java.nio.file.attribute.DosFileAttributeView.class);
                if (dosView != null) {
                    dosView.setHidden(true);
                }
            } catch (Exception ignored) {
                // 设置隐藏失败，忽略
            }

            // 设置 SimpleLogger 的日志文件路径（带日期）
            String loggerFilePath = "./.dlqq/log/logger/" + dateStr + ".log";
            System.setProperty("org.slf4j.simpleLogger.logFile", loggerFilePath);
            // 设置为追加模式
            System.setProperty("org.slf4j.simpleLogger.append", "true");

            // 重定向 stderr 到带日期的文件
            java.io.File stderrFile = new java.io.File(stderrDir, dateStr + ".log");
            java.io.PrintStream stderrStream = new java.io.PrintStream(
                new java.io.FileOutputStream(stderrFile, true), true, "UTF-8");
            System.setErr(stderrStream);
        } catch (Exception e) {
            // 如果重定向失败，保持默认的 stderr
        }
    }

    // Logger 必须在目录创建之后初始化
    private static final Logger logger = LoggerFactory.getLogger(MCPServerMain.class);

    // 全局变量用于健康检查
    private static volatile boolean isRunning = true;
    private static StdioTransport transport;
    private static McpServer server;

    public static void main(String[] args) {
        // 设置UTF-8编码
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("sun.stdout.encoding", "UTF-8");
        System.setProperty("sun.stderr.encoding", "UTF-8");

        // 确保 SimpleLogger 追加模式设置（必须在 LoggerFactory 首次使用前）
        System.setProperty("org.slf4j.simpleLogger.append", "true");

        // 重新初始化 stdout 的编码
        try {
            System.setOut(new java.io.PrintStream(System.out, true, "UTF-8"));
        } catch (Exception ignored) {
        }

        // 日志目录已在静态块中创建
        // 注意： SimpleLogger 的日志文件路径在静态块中已设置，包含日期

        logger.info("MCP Server starting...");
        logger.info("Working directory: {}", System.getProperty("user.dir"));
        logger.info("Default charset: {}", java.nio.charset.Charset.defaultCharset().name());
        logger.info("Log directory: {}", new java.io.File(".dlqq/log").getAbsolutePath());
        
        try {
            startServer();
            
            // 启动健康检查线程
            startHealthCheckThread();
            
            // 主线程保持运行
            while (isRunning) {
                Thread.sleep(1000);
            }
            
        } catch (IOException e) {
            logger.error("MCP服务器启动失败: {}", e.getMessage(), e);
            System.exit(1);
        } catch (InterruptedException e) {
            logger.warn("MCP服务器被中断");
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
        logger.debug("Transport created");

        // 创建MCP服务器
        String version = MCPServerMain.class.getPackage().getImplementationVersion();
        if (version == null || version.isEmpty()) {
            version = "255.0.0";
        }
        server = new McpServer(
            "donglouqiaoque-ai-tools",
            version,
            transport
        );
        logger.debug("Server created");

        // 注册AI工具
        AITools aiTools = new AITools();
        aiTools.registerTools(server);
        logger.debug("Tools registered");

        // 启动服务器
        server.start();
        logger.info("========================================");
        logger.info("MCP Server started successfully!");
        logger.info("Server name: donglouqiaoque-ai-tools");
        logger.info("Server version: {}", version);
        logger.info("Waiting for requests...");
        logger.info("========================================");
    }
    
    /**
     * 清理资源
     */
    private static void cleanup() {
        logger.info("MCP Server cleaning up...");
        isRunning = false;
        if (server != null) {
            try {
                server.stop();
            } catch (IOException e) {
                logger.error("Error stopping server: {}", e.getMessage(), e);
            }
        }
        logger.info("MCP Server exited");
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
            int checkCount = 0;
            long lastMemoryLog = System.currentTimeMillis();

            logger.info("[HealthCheck] 健康检查线程启动，检查间隔={}ms", checkInterval);

            while (isRunning) {
                try {
                    Thread.sleep(checkInterval);
                    checkCount++;

                    // 每30秒输出一次内存状态
                    if (System.currentTimeMillis() - lastMemoryLog > 30000) {
                        Runtime runtime = Runtime.getRuntime();
                        long totalMemory = runtime.totalMemory() / 1024 / 1024;
                        long freeMemory = runtime.freeMemory() / 1024 / 1024;
                        long maxMemory = runtime.maxMemory() / 1024 / 1024;
                        logger.info("[HealthCheck] JVM内存状态 - 总内存:{}MB, 可用:{}MB, 最大:{}MB, 已用:{}MB",
                                totalMemory, freeMemory, maxMemory, totalMemory - freeMemory);
                        lastMemoryLog = System.currentTimeMillis();
                    }

                    // 检查传输层是否还在运行
                    if (transport != null) {
                        boolean transportRunning = transport.isRunning();
                        if (checkCount % 12 == 0) {  // 每分钟输出一次状态
                            logger.info("[HealthCheck] 状态正常 - 运行检查次数={}, 传输层状态={}", checkCount, transportRunning ? "运行中" : "已停止");
                        }

                        if (!transportRunning) {
                            logger.warn("[HealthCheck] 传输层已停止运行!");
                            errorCount++;

                            if (errorCount >= MAX_ERROR_COUNT) {
                                logger.error("[HealthCheck] 连续 {} 次检查失败，准备重启...", MAX_ERROR_COUNT);
                                try {
                                    // 停止旧服务器
                                    if (server != null) {
                                        logger.info("[HealthCheck] 停止旧服务器...");
                                        server.stop();
                                    }
                                    // 重新启动
                                    logger.info("[HealthCheck] 正在重新启动服务器...");
                                    startServer();
                                    errorCount = 0;
                                    logger.info("[HealthCheck] 服务器重启成功");
                                } catch (IOException e) {
                                    logger.error("[HealthCheck] 重启失败: {}", e.getMessage(), e);
                                }
                            }
                        } else {
                            if (errorCount > 0) {
                                logger.info("[HealthCheck] 传输层恢复正常，重置错误计数");
                            }
                            errorCount = 0;  // 重置错误计数
                        }
                    } else {
                        logger.warn("[HealthCheck] transport为null");
                    }
                } catch (InterruptedException e) {
                    logger.warn("[HealthCheck] 健康检查线程被中断");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            logger.info("[HealthCheck] 健康检查线程已退出，总检查次数={}", checkCount);
        });
        healthCheck.setDaemon(true);
        healthCheck.setName("MCP-HealthCheck");
        healthCheck.start();
        logger.info("[HealthCheck] 健康检查线程已启动");
    }
}

package com.donglouqiaoque.dlqqaimcpserver.mcp;

import com.donglouqiaoque.dlqqaimcpserver.mcp.server.McpServer;
import com.donglouqiaoque.dlqqaimcpserver.mcp.tools.AITools;
import com.donglouqiaoque.dlqqaimcpserver.mcp.transport.HttpTransport;
import com.donglouqiaoque.dlqqaimcpserver.mcp.transport.McpTransport;
import com.donglouqiaoque.dlqqaimcpserver.mcp.transport.StdioTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import com.donglouqiaoque.dlqqaimcpserver.mcp.tools.McpTools;
import com.donglouqiaoque.dlqqaimcpserver.utils.Workspace;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

/**
 * MCP服务器入口
 */
public class MCPServerMain {

    // Logger 在工作区确定后、日志目录创建完成后初始化
    private static Logger logger;

    /**
     * 初始化日志目录与 stderr 重定向（`.dlqq` 跟随工作区）。
     */
    private static void initLogging() {
        try {
            // 生成当前时间字符串（格式：yyyy-MM-dd_HH-mm-ss），每次启动都创建新文件
            String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

            Path dlqqDir = Workspace.getRoot().resolve(".dlqq");
            File logDir = dlqqDir.resolve("log").toFile();
            File loggerDir = new File(logDir, "logger");
            File stderrDir = new File(logDir, "stderr");

            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            if (!loggerDir.exists()) {
                loggerDir.mkdirs();
            }
            if (!stderrDir.exists()) {
                stderrDir.mkdirs();
            }

            // 创建 .dlqq/.gitignore 文件，忽略该目录下的所有内容
            File gitignoreFile = new File(dlqqDir.toFile(), ".gitignore");
            if (!gitignoreFile.exists()) {
                try (FileWriter writer = new FileWriter(gitignoreFile)) {
                    writer.write("*\n");
                }
            }

            // 尝试将 .dlqq 目录设置为隐藏（Windows），失败则忽略
            try {
                DosFileAttributeView dosView = Files.getFileAttributeView(
                        dlqqDir, DosFileAttributeView.class);
                if (dosView != null) {
                    dosView.setHidden(true);
                }
            } catch (Exception ignored) {
                // 设置隐藏失败，忽略
            }

            // 设置 SimpleLogger 的日志文件路径（带日期，绝对路径，位于工作区）
            File loggerFile = new File(loggerDir, dateStr + ".log");
            System.setProperty("org.slf4j.simpleLogger.logFile", loggerFile.getAbsolutePath());
            // 设置为追加模式
            System.setProperty("org.slf4j.simpleLogger.append", "true");

            // 重定向 stderr 到带日期的文件
            File stderrFile = new File(stderrDir, dateStr + ".log");
            PrintStream stderrStream = new PrintStream(
                new FileOutputStream(stderrFile, true), true, "UTF-8");
            System.setErr(stderrStream);
        } catch (Exception e) {
            // 如果重定向失败，保持默认的 stderr
        }
    }

    /** 当前工作区的日志目录（工作区/.dlqq/log）。 */
    private static Path getLogDir() {
        return Workspace.getRoot().resolve(".dlqq").resolve("log");
    }

    // 全局变量用于健康检查
    private static volatile boolean isRunning = true;
    private static McpTransport transport;
    private static McpServer server;

    // 传输方式配置（默认 stdio）
    private static boolean httpMode = false;
    private static int httpPort = 49129;
    private static String httpPath = "/mcp";
    /** 启动参数指定的工作区目录（--workspace） */
    private static String workspacePath = null;

    public static void main(String[] args) {
        // 解析命令行参数（--transport http|stdio, --port N, --path /mcp, --workspace <dir>）
        parseArgs(args);

        // 初始化工作区：--workspace > 进程 CWD（工作区路径不持久化）
        Workspace.init(workspacePath);

        // 设置UTF-8编码
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("sun.stdout.encoding", "UTF-8");
        System.setProperty("sun.stderr.encoding", "UTF-8");

        // 初始化工作区下的日志目录（.dlqq 跟随工作区），并创建 Logger
        initLogging();
        logger = LoggerFactory.getLogger(MCPServerMain.class);

        // 确保当前工作区的 settings.properties 存在（配置随工作区变化）
        McpTools.ensureSettingsFile();

        // 重新初始化 stdout 的编码
        try {
            System.setOut(new PrintStream(System.out, true, "UTF-8"));
        } catch (Exception ignored) {
        }

        logger.info("MCP Server starting...");
        logger.info("Working directory: {}", System.getProperty("user.dir"));
        logger.info("Workspace: {} (source: {})", Workspace.getRoot(), Workspace.getSource());
        logger.info("Default charset: {}", Charset.defaultCharset().name());
        logger.info("Log directory: {}", getLogDir());
        
        try {
            startServer();
            
            // 启动健康检查线程
            startHealthCheckThread();
            
            // 主线程保持运行
            while (isRunning) {
                Thread.sleep(1000);
            }
            
        } catch (IOException e) {
            logger.error("MCP server failed to start: {}", e.getMessage(), e);
            System.exit(1);
        } catch (InterruptedException e) {
            logger.warn("MCP server interrupted");
            isRunning = false;
        } finally {
            cleanup();
        }
    }
    
    /**
     * 解析命令行参数
     * 支持: --transport http|stdio, --port N, --path /mcp, --workspace <dir>
     */
    private static void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--transport":
                    if (i + 1 < args.length) {
                        String t = args[++i].toLowerCase();
                        httpMode = "http".equals(t);
                        if (!"http".equals(t) && !"stdio".equals(t)) {
                            logger.warn("[MCPServerMain] Unknown transport: {}, using default stdio", t);
                            httpMode = false;
                        }
                    }
                    break;
                case "--port":
                    if (i + 1 < args.length) {
                        try {
                            httpPort = Integer.parseInt(args[++i]);
                        } catch (NumberFormatException e) {
                            logger.warn("[MCPServerMain] Invalid port: {}, using default 49129", args[i]);
                        }
                    }
                    break;
                case "--path":
                    if (i + 1 < args.length) {
                        httpPath = args[++i];
                        if (!httpPath.startsWith("/")) {
                            httpPath = "/" + httpPath;
                        }
                    }
                    break;
                case "--workspace":
                    if (i + 1 < args.length) {
                        workspacePath = args[++i];
                    }
                    break;
                default:
                    // 忽略未知参数
                    break;
            }
        }
    }

    /**
     * 启动MCP服务器
     */
    private static void startServer() throws IOException {
        // 创建传输层（stdio 或 http）
        HttpTransport httpTransport = null;
        if (httpMode) {
            httpTransport = new HttpTransport(httpPort, httpPath);
            transport = httpTransport;
            logger.info("Transport created: HTTP (port={}, path={})", httpPort, httpPath);
        } else {
            transport = new StdioTransport();
            logger.info("Transport created: STDIO");
        }

        // 创建MCP服务器
        String version = MCPServerMain.class.getPackage().getImplementationVersion();
        if (version == null || version.isEmpty()) {
            version = "255.0.0";
        }
        server = new McpServer(
            "dlqq-ai-mcp-server",
            version,
            transport
        );
        logger.debug("Server created");

        // 注册工具
        AITools aiTools = new AITools();
        aiTools.registerTools(server);
        logger.debug("Tools registered");

        // 应用持久化的“被移除工具”列表（.dlqq/settings/settings.properties 的 mcp.tools.excluded）
        int excluded = server.applyExcludedTools(
                McpTools.getExcludedToolNames());
        logger.info("Persisted removed tools: {}, enabled tools: {}", excluded, server.getEnabledToolCount());

        // 启动服务器
        server.start();
        logger.info("========================================");
        logger.info("MCP Server started successfully!");
        logger.info("Server name: dlqq-ai-mcp-server");
        logger.info("Server version: {}", version);
        if (httpMode) {
            int actualPort = httpTransport != null ? httpTransport.getPort() : httpPort;
            logger.info("Transport: HTTP (Streamable HTTP)");
            logger.info("Endpoint: http://localhost:{}{}", actualPort, httpPath);
            // 双击 bat / 控制台启动时给出可见提示（端口可能因冲突被用户改过）
            System.out.println("[HTTP] MCP server is running: http://localhost:" + actualPort + httpPath);
            System.out.println("[HTTP] Press Ctrl+C to stop.");
            System.out.flush();
        } else {
            logger.info("Transport: STDIO");
        }
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

            logger.info("[HealthCheck] Health check thread started, interval={}ms", checkInterval);

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
                        logger.info("[HealthCheck] JVM memory - total:{}MB, free:{}MB, max:{}MB, used:{}MB",
                                totalMemory, freeMemory, maxMemory, totalMemory - freeMemory);
                        lastMemoryLog = System.currentTimeMillis();
                    }

                    // 检查传输层是否还在运行
                    if (transport != null) {
                        boolean transportRunning = transport.isRunning();
                        if (checkCount % 12 == 0) {  // 每分钟输出一次状态
                            logger.info("[HealthCheck] Status OK - checks={}, transport={}", checkCount, transportRunning ? "running" : "stopped");
                        }

                        if (!transportRunning) {
                            logger.warn("[HealthCheck] Transport stopped!");
                            errorCount++;

                            if (errorCount >= MAX_ERROR_COUNT) {
                                logger.error("[HealthCheck] {} consecutive check failures, restarting...", MAX_ERROR_COUNT);
                                try {
                                    // 停止旧服务器
                                    if (server != null) {
                                        logger.info("[HealthCheck] Stopping old server...");
                                        server.stop();
                                    }
                                    // 重新启动
                                    logger.info("[HealthCheck] Restarting server...");
                                    startServer();
                                    errorCount = 0;
                                    logger.info("[HealthCheck] Server restarted successfully");
                                } catch (IOException e) {
                                    logger.error("[HealthCheck] Restart failed: {}", e.getMessage(), e);
                                }
                            }
                        } else {
                            if (errorCount > 0) {
                                logger.info("[HealthCheck] Transport recovered, resetting error count");
                            }
                            errorCount = 0;  // 重置错误计数
                        }
                    } else {
                        logger.warn("[HealthCheck] transport is null");
                    }
                } catch (InterruptedException e) {
                    logger.warn("[HealthCheck] Health check thread interrupted");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            logger.info("[HealthCheck] Health check thread exited, total checks={}", checkCount);
        });
        healthCheck.setDaemon(true);
        healthCheck.setName("MCP-HealthCheck");
        healthCheck.start();
        logger.info("[HealthCheck] Health check thread started");
    }
}

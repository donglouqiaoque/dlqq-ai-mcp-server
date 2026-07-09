package com.donglouqiaoque.aitools.mcp.tools;

import com.donglouqiaoque.aitools.mcp.protocol.McpSchema.CallToolResult;
import com.donglouqiaoque.aitools.mcp.protocol.McpSchema.ToolContent;
import com.donglouqiaoque.aitools.mcp.protocol.McpSchema.ToolInputSchema;
import com.donglouqiaoque.aitools.mcp.server.McpServer;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.*;

/**
 * 命令执行工具 - 执行命令行指令（需要用户审批）
 */
public class RunCommandTools {

    // 默认超时时间（秒）
    private static final int DEFAULT_TIMEOUT = 60;
    // 最大超时时间（秒）
    private static final int MAX_TIMEOUT = 600;

    /**
     * 注册命令执行工具
     */
    public static void registerTools(McpServer server) {
        registerRunCommandTool(server);
    }

    /**
     * 注册 dlqq_run_command 工具
     */
    private static void registerRunCommandTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> commandProp = new HashMap<>();
        commandProp.put("type", "string");
        commandProp.put("description", "要执行的命令行指令，如: \"echo Hello World\", \"dir\", \"java -version\"");
        properties.put("command", commandProp);

        Map<String, Object> workingDirProp = new HashMap<>();
        workingDirProp.put("type", "string");
        workingDirProp.put("description", "工作目录（可选），命令将在该目录下执行");
        properties.put("working_dir", workingDirProp);

        Map<String, Object> timeoutProp = new HashMap<>();
        timeoutProp.put("type", "integer");
        timeoutProp.put("description", "超时时间（秒），默认60秒，最大600秒");
        properties.put("timeout", timeoutProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.singletonList("command"));

        server.addTool("dlqq_run_command",
            "执行命令行指令（需要用户审批）。执行前会弹出确认对话框，用户可以选择执行或取消。支持设置工作目录和超时时间。",
            inputSchema,
            params -> {
                Object commandObj = params.get("command");
                if (commandObj == null) {
                    return createErrorResult("缺少必需参数: command");
                }
                String command = String.valueOf(commandObj).trim();
                if (command.isEmpty()) {
                    return createErrorResult("命令不能为空");
                }

                String workingDir = null;
                Object workingDirObj = params.get("working_dir");
                if (workingDirObj != null) {
                    workingDir = String.valueOf(workingDirObj).trim();
                    if (workingDir.isEmpty()) {
                        workingDir = null;
                    }
                }

                int timeout = DEFAULT_TIMEOUT;
                Object timeoutObj = params.get("timeout");
                if (timeoutObj != null) {
                    try {
                        timeout = Integer.parseInt(String.valueOf(timeoutObj));
                        if (timeout < 1) {
                            timeout = DEFAULT_TIMEOUT;
                        } else if (timeout > MAX_TIMEOUT) {
                            timeout = MAX_TIMEOUT;
                        }
                    } catch (NumberFormatException e) {
                        // 使用默认值
                    }
                }

                // 显示确认对话框
                boolean approved = showApprovalDialog(command, workingDir, timeout);

                if (!approved) {
                    return createErrorResult("用户取消了命令执行");
                }

                // 执行命令
                return executeCommand(command, workingDir, timeout);
            }
        );
    }

    /**
     * 显示用户确认对话框
     */
    private static boolean showApprovalDialog(String command, String workingDir, int timeout) {
        // 使用 SwingUtilities.invokeAndWait 确保在 EDT 线程中显示对话框
        final boolean[] result = { false };

        try {
            SwingUtilities.invokeAndWait(() -> {
                // 创建对话框
                JDialog dialog = new JDialog((Frame) null, "命令执行确认", true);
                dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                dialog.setSize(600, 300);
                dialog.setLocationRelativeTo(null);
                dialog.setResizable(true);

                // 主面板
                JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
                mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

                // 警告图标和标题
                JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                JLabel warningLabel = new JLabel("⚠️ AI 请求执行系统命令");
                warningLabel.setFont(new Font("Dialog", Font.BOLD, 16));
                warningLabel.setForeground(new Color(220, 140, 0));
                headerPanel.add(warningLabel);
                mainPanel.add(headerPanel, BorderLayout.NORTH);

                // 命令信息面板
                JPanel infoPanel = new JPanel(new GridLayout(0, 1, 5, 5));
                infoPanel.setBorder(BorderFactory.createTitledBorder("命令信息"));

                // 命令
                JPanel commandPanel = new JPanel(new BorderLayout());
                commandPanel.add(new JLabel("命令: "), BorderLayout.WEST);
                JTextArea commandArea = new JTextArea(command);
                commandArea.setEditable(false);
                commandArea.setLineWrap(true);
                commandArea.setWrapStyleWord(true);
                commandArea.setBackground(new Color(245, 245, 245));
                commandArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
                JScrollPane commandScroll = new JScrollPane(commandArea);
                commandScroll.setPreferredSize(new Dimension(0, 60));
                commandPanel.add(commandScroll, BorderLayout.CENTER);
                infoPanel.add(commandPanel);

                // 工作目录
                if (workingDir != null && !workingDir.isEmpty()) {
                    JPanel dirPanel = new JPanel(new BorderLayout());
                    dirPanel.add(new JLabel("工作目录: "), BorderLayout.WEST);
                    JTextField dirField = new JTextField(workingDir);
                    dirField.setEditable(false);
                    dirField.setBackground(new Color(245, 245, 245));
                    dirPanel.add(dirField, BorderLayout.CENTER);
                    infoPanel.add(dirPanel);
                }

                // 超时时间
                JPanel timeoutPanel = new JPanel(new BorderLayout());
                timeoutPanel.add(new JLabel("超时时间: "), BorderLayout.WEST);
                timeoutPanel.add(new JLabel(timeout + " 秒"), BorderLayout.CENTER);
                infoPanel.add(timeoutPanel);

                mainPanel.add(infoPanel, BorderLayout.CENTER);

                // 警告信息
                JPanel warningPanel = new JPanel(new BorderLayout());
                warningPanel.setBackground(new Color(255, 248, 220));
                warningPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(255, 200, 100)),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)
                ));
                JLabel warningText = new JLabel("⚠️ 警告：执行未知命令可能对系统造成风险，请确认该命令是安全的。");
                warningText.setForeground(new Color(180, 100, 0));
                warningPanel.add(warningText, BorderLayout.CENTER);
                mainPanel.add(warningPanel, BorderLayout.SOUTH);

                // 按钮面板
                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
                JButton cancelButton = new JButton("取消");
                cancelButton.setPreferredSize(new Dimension(100, 30));
                JButton executeButton = new JButton("执行命令");
                executeButton.setPreferredSize(new Dimension(100, 30));
                executeButton.setBackground(new Color(0, 120, 215));
                executeButton.setForeground(Color.WHITE);
                executeButton.setFocusPainted(false);

                cancelButton.addActionListener(e -> {
                    result[0] = false;
                    dialog.dispose();
                });

                executeButton.addActionListener(e -> {
                    result[0] = true;
                    dialog.dispose();
                });

                buttonPanel.add(cancelButton);
                buttonPanel.add(executeButton);
                mainPanel.add(buttonPanel, BorderLayout.SOUTH);

                dialog.getContentPane().add(mainPanel);

                // 设置默认按钮
                dialog.getRootPane().setDefaultButton(cancelButton);

                // 显示对话框
                dialog.setVisible(true);
            });
        } catch (Exception e) {
            // 如果对话框显示失败，默认拒绝执行
            return false;
        }

        return result[0];
    }

    /**
     * 执行命令
     */
    private static CallToolResult executeCommand(String command, String workingDir, int timeout) {
        try {
            // 构建 ProcessBuilder
            ProcessBuilder pb;

            // 检测操作系统
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                // Windows 使用 cmd /c
                pb = new ProcessBuilder("cmd", "/c", command);
            } else {
                // Linux/Mac 使用 sh -c
                pb = new ProcessBuilder("sh", "-c", command);
            }

            // 设置工作目录
            if (workingDir != null && !workingDir.isEmpty()) {
                java.io.File dir = new java.io.File(workingDir);
                if (dir.exists() && dir.isDirectory()) {
                    pb.directory(dir);
                } else {
                    return createErrorResult("工作目录不存在: " + workingDir);
                }
            }

            // 合并错误流
            pb.redirectErrorStream(true);

            // 启动进程
            Process process = pb.start();

            // 使用 Future 来超时控制
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<StringBuilder> future = executor.submit(() -> {
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }
                return output;
            });

            StringBuilder output;
            boolean timedOut = false;

            try {
                output = future.get(timeout, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                process.destroyForcibly();
                timedOut = true;
                output = new StringBuilder("命令执行超时（" + timeout + "秒）\n");
                // 尝试获取已输出的内容
                try {
                    output.append(future.get(2, TimeUnit.SECONDS));
                } catch (Exception ex) {
                    // 忽略
                }
            } finally {
                executor.shutdown();
            }

            // 等待进程结束（如果还未结束）
            int exitCode = -1;
            try {
                if (!timedOut) {
                    exitCode = process.waitFor();
                }
            } catch (InterruptedException e) {
                // 忽略
            }

            // 构建结果
            StringBuilder result = new StringBuilder();
            result.append("【命令执行结果】\n");
            result.append("命令: ").append(command).append("\n");
            if (workingDir != null && !workingDir.isEmpty()) {
                result.append("工作目录: ").append(workingDir).append("\n");
            }
            result.append("退出码: ").append(exitCode).append("\n");
            result.append(timedOut ? "状态: 超时\n" : "状态: 完成\n");
            result.append("\n===== 输出 =====\n");
            result.append(output.length() > 0 ? output.toString() : "(无输出)");

            return createSuccessResult(result.toString());

        } catch (IOException e) {
            return createErrorResult("执行命令失败: " + e.getMessage());
        } catch (Exception e) {
            return createErrorResult("未知错误: " + e.getMessage());
        }
    }

    /**
     * 创建成功结果
     */
    private static CallToolResult createSuccessResult(String content) {
        return new CallToolResult(
            Collections.singletonList(ToolContent.text(content)),
            false
        );
    }

    /**
     * 创建错误结果
     */
    private static CallToolResult createErrorResult(String message) {
        return new CallToolResult(
            Collections.singletonList(ToolContent.text(message)),
            true
        );
    }
}

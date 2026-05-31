package com.donglouqiaoque.aitools.mcp.tools;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * GUI对话框工具 - 提供图形界面交互功能
 * 支持文本输入、文件选择等操作
 */
public class DialogTools {

    /**
     * 对话框结果
     */
    public static class DialogResult {
        private final boolean confirmed;
        private final String textInput;
        private final String filePath;
        private final String fileContent;

        public DialogResult(boolean confirmed, String textInput, String filePath, String fileContent) {
            this.confirmed = confirmed;
            this.textInput = textInput;
            this.filePath = filePath;
            this.fileContent = fileContent;
        }

        public boolean isConfirmed() { return confirmed; }
        public String getTextInput() { return textInput; }
        public String getFilePath() { return filePath; }
        public String getFileContent() { return fileContent; }
    }

    /**
     * 显示输入对话框（支持文本输入和文件选择）
     *
     * @param title       窗口标题
     * @param message     提示消息
     * @param showFileSelect 是否显示文件选择按钮
     * @param fileExtensions 允许的文件扩展名（如 {"txt", "java"}）
     * @param timeoutSeconds 超时时间（秒），0表示无超时
     * @return 用户输入结果，如果超时则返回null
     */
    public static DialogResult showInputDialog(String title, String message,
                                               boolean showFileSelect, String[] fileExtensions,
                                               int timeoutSeconds) {
        // 使用CompletableFuture在Swing线程中执行
        CompletableFuture<DialogResult> future = new CompletableFuture<>();

        SwingUtilities.invokeLater(() -> {
            try {
                // 设置外观为系统默认
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                }

                // 创建对话框
                JDialog dialog = new JDialog((Frame) null, title, true);
                dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                dialog.setSize(600, 350);
                dialog.setLocationRelativeTo(null);

                // 主面板
                JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
                mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

                // 消息标签
                JLabel messageLabel = new JLabel(message);
                messageLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
                mainPanel.add(messageLabel, BorderLayout.NORTH);

                // 文本输入区域
                JTextArea textArea = new JTextArea(8, 50);
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);
                textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
                JScrollPane scrollPane = new JScrollPane(textArea);
                mainPanel.add(scrollPane, BorderLayout.CENTER);

                // 底部面板（文件选择 + 按钮）
                JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));

                // 文件选择区域
                JPanel filePanel = new JPanel(new BorderLayout(5, 0));
                JTextField filePathField = new JTextField();
                filePathField.setEditable(false);
                filePathField.setVisible(showFileSelect);

                if (showFileSelect) {
                    JButton fileButton = new JButton("选择文件");
                    fileButton.addActionListener(e -> {
                        JFileChooser fileChooser = new JFileChooser();
                        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

                        // 设置文件过滤器
                        if (fileExtensions != null && fileExtensions.length > 0) {
                            StringBuilder desc = new StringBuilder("支持的文件 (");
                            for (int i = 0; i < fileExtensions.length; i++) {
                                if (i > 0) desc.append(", ");
                                desc.append("*.").append(fileExtensions[i]);
                            }
                            desc.append(")");
                            FileNameExtensionFilter filter = new FileNameExtensionFilter(
                                    desc.toString(), fileExtensions);
                            fileChooser.setFileFilter(filter);
                        }

                        int result = fileChooser.showOpenDialog(dialog);
                        if (result == JFileChooser.APPROVE_OPTION) {
                            File selectedFile = fileChooser.getSelectedFile();
                            filePathField.setText(selectedFile.getAbsolutePath());
                        }
                    });

                    JPanel fileSelectPanel = new JPanel(new BorderLayout(5, 0));
                    fileSelectPanel.add(fileButton, BorderLayout.WEST);
                    fileSelectPanel.add(filePathField, BorderLayout.CENTER);
                    filePanel.add(fileSelectPanel, BorderLayout.CENTER);
                    bottomPanel.add(filePanel, BorderLayout.NORTH);
                }

                // 按钮面板
                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                JButton okButton = new JButton("确定");
                JButton cancelButton = new JButton("取消");

                // 存储读取的文件内容
                final StringBuilder fileContent = new StringBuilder();

                okButton.addActionListener(e -> {
                    // 读取选中的文件内容
                    String filePath = filePathField.getText();
                    if (filePath != null && !filePath.isEmpty()) {
                        try {
                            fileContent.append(new String(java.nio.file.Files.readAllBytes(
                                    java.nio.file.Paths.get(filePath)), java.nio.charset.StandardCharsets.UTF_8));
                        } catch (Exception ex) {
                            fileContent.append("[读取文件失败: ").append(ex.getMessage()).append("]");
                        }
                    }
                    dialog.dispose();
                    future.complete(new DialogResult(true, textArea.getText(), filePath, fileContent.toString()));
                });

                cancelButton.addActionListener(e -> {
                    dialog.dispose();
                    future.complete(new DialogResult(false, null, null, null));
                });

                buttonPanel.add(okButton);
                buttonPanel.add(cancelButton);
                bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

                mainPanel.add(bottomPanel, BorderLayout.SOUTH);
                dialog.add(mainPanel);

                // ESC键关闭对话框
                dialog.getRootPane().registerKeyboardAction(
                        e -> cancelButton.doClick(),
                        KeyStroke.getKeyStroke("ESCAPE"),
                        JComponent.WHEN_IN_FOCUSED_WINDOW
                );

                // Enter键确认
                dialog.getRootPane().registerKeyboardAction(
                        e -> okButton.doClick(),
                        KeyStroke.getKeyStroke("ENTER"),
                        JComponent.WHEN_IN_FOCUSED_WINDOW
                );

                // 设置默认按钮
                dialog.getRootPane().setDefaultButton(okButton);

                dialog.setVisible(true);

            } catch (Exception ex) {
                future.completeExceptionally(ex);
            }
        });

        try {
            if (timeoutSeconds > 0) {
                return future.get(timeoutSeconds, TimeUnit.SECONDS);
            } else {
                return future.get();
            }
        } catch (TimeoutException e) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException("对话框执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 简化的输入对话框（无超时，支持文件选择）
     */
    public static DialogResult showInputDialog(String title, String message,
                                               boolean showFileSelect, String[] fileExtensions) {
        return showInputDialog(title, message, showFileSelect, fileExtensions, 0);
    }

    /**
     * 纯文本输入对话框
     */
    public static DialogResult showTextInputDialog(String title, String message) {
        return showInputDialog(title, message, false, null, 0);
    }

    /**
     * 文件选择对话框
     *
     * @param title          窗口标题
     * @param extensions     允许的文件扩展名
     * @param allowMultiple  是否允许多选
     * @return 选中的文件路径数组，如果取消则返回空数组
     */
    public static String[] showFileChooser(String title, String[] extensions, boolean allowMultiple) {
        CompletableFuture<String[]> future = new CompletableFuture<>();

        SwingUtilities.invokeLater(() -> {
            try {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                }

                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle(title);
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fileChooser.setMultiSelectionEnabled(allowMultiple);

                // 设置文件过滤器
                if (extensions != null && extensions.length > 0) {
                    StringBuilder desc = new StringBuilder("支持的文件 (");
                    for (int i = 0; i < extensions.length; i++) {
                        if (i > 0) desc.append(", ");
                        desc.append("*.").append(extensions[i]);
                    }
                    desc.append(")");
                    FileNameExtensionFilter filter = new FileNameExtensionFilter(
                            desc.toString(), extensions);
                    fileChooser.setFileFilter(filter);
                }

                int result = fileChooser.showOpenDialog(null);

                if (result == JFileChooser.APPROVE_OPTION) {
                    if (allowMultiple) {
                        File[] files = fileChooser.getSelectedFiles();
                        String[] paths = new String[files.length];
                        for (int i = 0; i < files.length; i++) {
                            paths[i] = files[i].getAbsolutePath();
                        }
                        future.complete(paths);
                    } else {
                        future.complete(new String[]{fileChooser.getSelectedFile().getAbsolutePath()});
                    }
                } else {
                    future.complete(new String[0]);
                }
            } catch (Exception ex) {
                future.completeExceptionally(ex);
            }
        });

        try {
            return future.get();
        } catch (Exception e) {
            throw new RuntimeException("文件选择对话框执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 确认对话框
     *
     * @param title   窗口标题
     * @param message 提示消息
     * @return 用户是否点击确定
     */
    public static boolean showConfirmDialog(String title, String message) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        SwingUtilities.invokeLater(() -> {
            try {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                }

                int result = JOptionPane.showConfirmDialog(
                        null,
                        message,
                        title,
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );
                future.complete(result == JOptionPane.YES_OPTION);
            } catch (Exception ex) {
                future.completeExceptionally(ex);
            }
        });

        try {
            return future.get();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 消息提示对话框
     *
     * @param title   窗口标题
     * @param message 消息内容
     * @param type    消息类型："info", "warning", "error"
     */
    public static void showMessageDialog(String title, String message, String type) {
        SwingUtilities.invokeLater(() -> {
            try {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                }

                int messageType;
                switch (type.toLowerCase()) {
                    case "warning":
                        messageType = JOptionPane.WARNING_MESSAGE;
                        break;
                    case "error":
                        messageType = JOptionPane.ERROR_MESSAGE;
                        break;
                    default:
                        messageType = JOptionPane.INFORMATION_MESSAGE;
                }

                JOptionPane.showMessageDialog(null, message, title, messageType);
            } catch (Exception ignored) {
            }
        });
    }
}

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
     * 检查字体是否可用
     */
    private static boolean isFontAvailable(String fontName) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (String name : ge.getAvailableFontFamilyNames()) {
            if (name.equalsIgnoreCase(fontName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取支持中文的UI字体
     */
    private static Font getChineseFont() {
        // 尝试使用微软雅黑（Windows系统自带）
        if (isFontAvailable("Microsoft YaHei")) {
            return new Font("Microsoft YaHei", Font.PLAIN, 12);
        }
        // 尝试使用宋体
        if (isFontAvailable("SimSun")) {
            return new Font("SimSun", Font.PLAIN, 12);
        }
        // 尝试使用Dialog
        return new Font("Dialog", Font.PLAIN, 12);
    }

    /**
     * 获取支持中文的等宽字体
     */
    private static Font getMonospacedChineseFont() {
        // 尝试使用 Consolas（Windows等宽字体）
        if (isFontAvailable("Consolas")) {
            return new Font("Consolas", Font.PLAIN, 12);
        }
        // 尝试使用 Courier New
        if (isFontAvailable("Courier New")) {
            return new Font("Courier New", Font.PLAIN, 12);
        }
        // 回退到 Dialog 字体
        return new Font("Dialog", Font.PLAIN, 12);
    }

    /**
     * 设置全局中文字体 - 必须在设置LookAndFeel之前调用
     */
    private static void setupChineseFont() {
        Font uiFont = getChineseFont();

        // 设置所有Swing组件的字体
        UIManager.put("CheckBox.font", uiFont);
        UIManager.put("CheckBoxMenuItem.font", uiFont);
        UIManager.put("ColorChooser.font", uiFont);
        UIManager.put("ComboBox.font", uiFont);
        UIManager.put("DesktopIcon.font", uiFont);
        UIManager.put("EditorPane.font", uiFont);
        UIManager.put("FormattedTextField.font", uiFont);
        UIManager.put("InternalFrame.titleFont", uiFont);
        UIManager.put("Label.font", uiFont);
        UIManager.put("List.font", uiFont);
        UIManager.put("Menu.font", uiFont);
        UIManager.put("MenuBar.font", uiFont);
        UIManager.put("MenuItem.font", uiFont);
        UIManager.put("OptionPane.buttonFont", uiFont);
        UIManager.put("OptionPane.font", uiFont);
        UIManager.put("OptionPane.messageFont", uiFont);
        UIManager.put("Panel.font", uiFont);
        UIManager.put("PasswordField.font", uiFont);
        UIManager.put("PopupMenu.font", uiFont);
        UIManager.put("ProgressBar.font", uiFont);
        UIManager.put("RadioButton.font", uiFont);
        UIManager.put("RadioButtonMenuItem.font", uiFont);
        UIManager.put("ScrollPane.font", uiFont);
        UIManager.put("Slider.font", uiFont);
        UIManager.put("Spinner.font", uiFont);
        UIManager.put("TabbedPane.font", uiFont);
        UIManager.put("Table.font", uiFont);
        UIManager.put("TableHeader.font", uiFont);
        UIManager.put("TextArea.font", new Font("Monospaced", Font.PLAIN, 12));
        UIManager.put("TextField.font", uiFont);
        UIManager.put("TextPane.font", uiFont);
        UIManager.put("TitledBorder.font", uiFont);
        UIManager.put("ToggleButton.font", uiFont);
        UIManager.put("ToolBar.font", uiFont);
        UIManager.put("ToolTip.font", uiFont);
        UIManager.put("Tree.font", uiFont);
        UIManager.put("Viewport.font", uiFont);
        UIManager.put("Button.font", uiFont);
    }

    /**
     * 为组件及其子组件递归设置字体
     */
    private static void setFontRecursively(Component comp, Font font) {
        if (comp == null) return;
        comp.setFont(font);
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                setFontRecursively(child, font);
            }
        }
    }

    /**
     * 显示输入窗口（支持文本输入和文件选择）
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
                // 先设置全局字体，支持中文显示（必须在setLookAndFeel之前）
                setupChineseFont();

                // 设置外观为系统默认
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                }

                // 创建独立窗口（使用JFrame而不是JDialog）
                JFrame frame = new JFrame(title);
                frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                frame.setSize(600, 350);
                frame.setLocationRelativeTo(null);

                // 主面板
                JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
                mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

                // 消息标签 - 支持多行显示
                JTextArea messageArea = new JTextArea(message);
                messageArea.setEditable(false);
                messageArea.setLineWrap(true);
                messageArea.setWrapStyleWord(true);
                messageArea.setOpaque(false);
                messageArea.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
                // 使用支持中文的字体
                Font chineseFont = getChineseFont();
                messageArea.setFont(chineseFont);
                JScrollPane messageScroll = new JScrollPane(messageArea);
                messageScroll.setBorder(null);
                messageScroll.setPreferredSize(new Dimension(550, 60));
                mainPanel.add(messageScroll, BorderLayout.NORTH);

                // 文本输入区域 - 使用支持中文的等宽字体
                JTextArea textArea = new JTextArea(8, 50);
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);
                // 使用支持中文的等宽字体
                Font monoFont = getMonospacedChineseFont();
                textArea.setFont(monoFont);
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

                        int result = fileChooser.showOpenDialog(frame);
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

                // 关闭窗口的通用方法
                Runnable closeWindow = () -> {
                    frame.dispose();
                };

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
                    closeWindow.run();
                    if (!future.isDone()) {
                        future.complete(new DialogResult(true, textArea.getText(), filePath, fileContent.toString()));
                    }
                });

                cancelButton.addActionListener(e -> {
                    closeWindow.run();
                    if (!future.isDone()) {
                        future.complete(new DialogResult(false, null, null, null));
                    }
                });

                buttonPanel.add(okButton);
                buttonPanel.add(cancelButton);
                bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

                mainPanel.add(bottomPanel, BorderLayout.SOUTH);
                frame.add(mainPanel);

                // ESC键关闭窗口
                frame.getRootPane().registerKeyboardAction(
                        e -> cancelButton.doClick(),
                        KeyStroke.getKeyStroke("ESCAPE"),
                        JComponent.WHEN_IN_FOCUSED_WINDOW
                );

                // Enter键确认
                frame.getRootPane().registerKeyboardAction(
                        e -> okButton.doClick(),
                        KeyStroke.getKeyStroke("ENTER"),
                        JComponent.WHEN_IN_FOCUSED_WINDOW
                );

                // 设置默认按钮
                frame.getRootPane().setDefaultButton(okButton);

                // 所有组件添加完成后，递归设置中文字体
                setFontRecursively(frame, chineseFont);

                // 添加窗口关闭监听器 - 关闭窗口等于取消
                frame.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        if (!future.isDone()) {
                            future.complete(new DialogResult(false, null, null, null));
                        }
                        frame.dispose();
                    }
                });

                frame.setVisible(true);

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
                // 先设置全局字体，支持中文显示（必须在setLookAndFeel之前）
                setupChineseFont();

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
     * 确认对话框（独立窗口）
     *
     * @param title   窗口标题
     * @param message 提示消息
     * @return 用户是否点击确定
     */
    public static boolean showConfirmDialog(String title, String message) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        SwingUtilities.invokeLater(() -> {
            try {
                // 先设置全局字体，支持中文显示（必须在setLookAndFeel之前）
                setupChineseFont();

                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                }

                // 创建独立窗口
                JFrame frame = new JFrame(title);
                frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                frame.setSize(400, 200);
                frame.setLocationRelativeTo(null);
                frame.setResizable(false);

                // 主面板
                JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
                mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

                // 消息区域 - 支持多行显示
                JTextArea messageArea = new JTextArea(message);
                messageArea.setEditable(false);
                messageArea.setLineWrap(true);
                messageArea.setWrapStyleWord(true);
                messageArea.setOpaque(false);
                Font chineseFont = getChineseFont();
                messageArea.setFont(chineseFont);
                JScrollPane messageScroll = new JScrollPane(messageArea);
                messageScroll.setBorder(null);
                mainPanel.add(messageScroll, BorderLayout.CENTER);

                // 按钮面板
                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
                JButton yesButton = new JButton("是");
                JButton noButton = new JButton("否");

                yesButton.addActionListener(e -> {
                    frame.dispose();
                    if (!future.isDone()) {
                        future.complete(true);
                    }
                });

                noButton.addActionListener(e -> {
                    frame.dispose();
                    if (!future.isDone()) {
                        future.complete(false);
                    }
                });

                buttonPanel.add(yesButton);
                buttonPanel.add(noButton);
                mainPanel.add(buttonPanel, BorderLayout.SOUTH);

                frame.add(mainPanel);

                // ESC键选择"否"
                frame.getRootPane().registerKeyboardAction(
                        e -> noButton.doClick(),
                        KeyStroke.getKeyStroke("ESCAPE"),
                        JComponent.WHEN_IN_FOCUSED_WINDOW
                );

                // Enter键选择"是"
                frame.getRootPane().registerKeyboardAction(
                        e -> yesButton.doClick(),
                        KeyStroke.getKeyStroke("ENTER"),
                        JComponent.WHEN_IN_FOCUSED_WINDOW
                );

                frame.getRootPane().setDefaultButton(yesButton);

                // 设置字体
                setFontRecursively(frame, chineseFont);

                // 关闭窗口等于选择"否"
                frame.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        if (!future.isDone()) {
                            future.complete(false);
                        }
                        frame.dispose();
                    }
                });

                frame.setVisible(true);

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
     * 消息提示对话框（独立窗口）
     *
     * @param title   窗口标题
     * @param message 消息内容
     * @param type    消息类型："info", "warning", "error"
     */
    public static void showMessageDialog(String title, String message, String type) {
        SwingUtilities.invokeLater(() -> {
            try {
                // 先设置全局字体，支持中文显示（必须在setLookAndFeel之前）
                setupChineseFont();

                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                }

                // 创建独立窗口
                JFrame frame = new JFrame(title);
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.setSize(400, 200);
                frame.setLocationRelativeTo(null);

                // 主面板
                JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
                mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

                // 消息区域 - 支持多行显示
                JTextArea messageArea = new JTextArea(message);
                messageArea.setEditable(false);
                messageArea.setLineWrap(true);
                messageArea.setWrapStyleWord(true);
                messageArea.setOpaque(false);
                Font chineseFont = getChineseFont();
                messageArea.setFont(chineseFont);
                JScrollPane messageScroll = new JScrollPane(messageArea);
                messageScroll.setBorder(null);
                mainPanel.add(messageScroll, BorderLayout.CENTER);

                // 按钮面板
                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
                JButton okButton = new JButton("确定");
                okButton.addActionListener(e -> frame.dispose());
                buttonPanel.add(okButton);
                mainPanel.add(buttonPanel, BorderLayout.SOUTH);

                frame.add(mainPanel);

                // ESC键和Enter键都关闭窗口
                frame.getRootPane().registerKeyboardAction(
                        e -> okButton.doClick(),
                        KeyStroke.getKeyStroke("ESCAPE"),
                        JComponent.WHEN_IN_FOCUSED_WINDOW
                );
                frame.getRootPane().registerKeyboardAction(
                        e -> okButton.doClick(),
                        KeyStroke.getKeyStroke("ENTER"),
                        JComponent.WHEN_IN_FOCUSED_WINDOW
                );

                frame.getRootPane().setDefaultButton(okButton);

                // 设置字体
                setFontRecursively(frame, chineseFont);

                frame.setVisible(true);

            } catch (Exception ignored) {
            }
        });
    }
}

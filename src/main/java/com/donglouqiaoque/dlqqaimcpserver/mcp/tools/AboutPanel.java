package com.donglouqiaoque.dlqqaimcpserver.mcp.tools;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URI;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * 图形化工具的"关于信息栏"：
 * 作者名称（鸫楼桥鹊）+ MCP 工具版本号 + 联系作者链接（点击打开默认浏览器）。
 *
 * <p>供 UART / WiFi / USB 等 Swing 图形化工具共用。</p>
 */
final class AboutPanel {

    /** 作者名称 */
    static final String AUTHOR = "鸫楼桥鹊";
    /** 联系作者网址 */
    static final String WEBSITE = "http://www.donglouqiaoque.com";

    private AboutPanel() {
    }

    /**
     * 创建作者信息栏面板（FlowLayout 水平排列：作者 | 版本 | 联系作者链接）。
     *
     * @param owner 所属窗口（用于链接打开失败时弹提示）
     * @return 信息栏面板
     */
    static JPanel create(Frame owner) {
        JPanel authorPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 2));
        authorPanel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        authorPanel.setBackground(new Color(240, 240, 240));

        // 作者
        JLabel authorLabel = new JLabel("作者：" + AUTHOR);
        authorLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        authorLabel.setForeground(Color.DARK_GRAY);
        authorPanel.add(authorLabel);

        // 版本号（从 jar MANIFEST 读取）
        String version = getAppVersion();
        String versionText = "无法获取".equals(version) ? "无法获取" : "V" + version;
        JLabel versionLabel = new JLabel("版本：" + versionText);
        versionLabel.setFont(new Font("Consolas", Font.PLAIN, 11));
        versionLabel.setForeground(Color.DARK_GRAY);
        authorPanel.add(versionLabel);

        // 联系作者（可点击链接，打开默认浏览器）
        JLabel linkLabel = new JLabel("<html><u>联系作者：www.donglouqiaoque.com</u></html>");
        linkLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        linkLabel.setForeground(new Color(0, 102, 204));
        linkLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        linkLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().browse(new URI(WEBSITE));
                    }
                } catch (Exception ex) {
                    if (owner != null) {
                        JOptionPane.showMessageDialog(owner, "无法打开浏览器：" + ex.getMessage(),
                                "错误", JOptionPane.ERROR_MESSAGE);
                    } else {
                        System.err.println("无法打开浏览器: " + ex.getMessage());
                    }
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                linkLabel.setForeground(new Color(0, 51, 153));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                linkLabel.setForeground(new Color(0, 102, 204));
            }
        });
        authorPanel.add(linkLabel);

        return authorPanel;
    }

    /**
     * 获取应用版本号和构建时间（从 jar MANIFEST 读取）
     * 格式：Vx.x.x（yyyy.MM.dd HH.mm.ss）
     */
    static String getAppVersion() {
        try {
            // 获取当前类的JAR文件路径
            String classPath = AboutPanel.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            // URL解码处理中文路径
            classPath = java.net.URLDecoder.decode(classPath, "UTF-8");
            File jarFile = new File(classPath);

            if (jarFile.exists()) {
                // 读取MANIFEST.MF
                try (JarFile jar = new JarFile(jarFile)) {
                    Manifest manifest = jar.getManifest();
                    if (manifest != null) {
                        Attributes mainAttrs = manifest.getMainAttributes();
                        String version = mainAttrs.getValue("Implementation-Version");
                        if (version == null || version.isEmpty()) {
                            return "无法获取";
                        }

                        // 获取构建时间
                        String buildTime = null;
                        String buildTimeAttr = mainAttrs.getValue("Build-Time");
                        if (buildTimeAttr != null) {
                            try {
                                // 将UTC时间转换为本地时间
                                java.time.LocalDateTime utcTime = java.time.LocalDateTime.parse(buildTimeAttr,
                                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                                java.time.ZonedDateTime utcZoned = utcTime.atZone(java.time.ZoneId.of("UTC"));
                                java.time.ZonedDateTime localZoned = utcZoned.withZoneSameInstant(java.time.ZoneId.systemDefault());
                                buildTime = localZoned.format(
                                    java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd HH.mm.ss"));
                            } catch (Exception e) {
                                // 构建时间解析失败，忽略
                            }
                        }

                        if (buildTime != null) {
                            return "V" + version + "（" + buildTime + "）";
                        }
                        return "V" + version;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[AboutPanel] 读取版本号失败: " + e.getMessage());
        }
        return "无法获取";
    }
}

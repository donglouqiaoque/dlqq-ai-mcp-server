package com.donglouqiaoque.dlqqaimcpserver.mcp.tools;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.InputStream;

/**
 * 工具窗口 LOGO 加载器。
 *
 * <p>统一从 jar 资源 <code>/image/logo.png</code> 加载窗口图标。
 * 使用 PNG 而非 ICO 的原因：JDK 内置 ImageIO 不支持 ICO 格式，
 * 直接读取会导致图标加载失败（返回 null）。</p>
 */
final class LogoLoader {

    private LogoLoader() {
    }

    /**
     * 加载窗口 LOGO 图片。
     *
     * @return Logo 图片；资源不存在或加载失败时返回 null（调用方应静默降级，不影响窗口功能）
     */
    static Image load() {
        try (InputStream is = LogoLoader.class.getResourceAsStream("/image/logo.png")) {
            if (is == null) {
                return null;
            }
            return ImageIO.read(is);
        } catch (Exception e) {
            System.err.println("[LogoLoader] 加载 logo.png 失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 为窗口设置 LOGO 图标（失败时静默降级）。
     */
    static void applyTo(java.awt.Window window, String toolName) {
        try {
            Image logo = load();
            if (logo != null) {
                window.setIconImage(logo);
                System.err.println("[" + toolName + "] 窗口图标已设置 (logo.png)");
            } else {
                System.err.println("[" + toolName + "] 警告: 未找到图标资源 /image/logo.png");
            }
        } catch (Exception e) {
            System.err.println("[" + toolName + "] 设置窗口图标失败: " + e.getMessage());
        }
    }
}

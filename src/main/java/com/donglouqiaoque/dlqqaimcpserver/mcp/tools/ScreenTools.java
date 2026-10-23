package com.donglouqiaoque.dlqqaimcpserver.mcp.tools;

import com.donglouqiaoque.dlqqaimcpserver.mcp.protocol.McpSchema.CallToolResult;
import com.donglouqiaoque.dlqqaimcpserver.mcp.protocol.McpSchema.ToolContent;
import com.donglouqiaoque.dlqqaimcpserver.mcp.protocol.McpSchema.ToolInputSchema;
import com.donglouqiaoque.dlqqaimcpserver.mcp.server.McpServer;
import com.donglouqiaoque.dlqqaimcpserver.utils.Workspace;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

/**
 * 屏幕截图工具 - 获取桌面图像（Java原生实现，使用 java.awt.Robot）
 */
public class ScreenTools {

    public static void registerTools(McpServer server) {
        registerCaptureScreenTool(server);
    }

    /**
     * dlqq_capture_screen - 获取桌面图像（截图）
     */
    private static void registerCaptureScreenTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> outputProp = new HashMap<>();
        outputProp.put("type", "string");
        outputProp.put("description", "输出文件路径（可选，支持png/jpg格式，默认保存到.dlqq/cache目录下带时间戳的文件）");
        properties.put("output_path", outputProp);

        Map<String, Object> xProp = new HashMap<>();
        xProp.put("type", "integer");
        xProp.put("description", "截取区域左上角X坐标（可选，默认整个屏幕）");
        properties.put("x", xProp);

        Map<String, Object> yProp = new HashMap<>();
        yProp.put("type", "integer");
        yProp.put("description", "截取区域左上角Y坐标（可选，默认整个屏幕）");
        properties.put("y", yProp);

        Map<String, Object> widthProp = new HashMap<>();
        widthProp.put("type", "integer");
        widthProp.put("description", "截取区域宽度（可选，默认整个屏幕宽度）");
        properties.put("width", widthProp);

        Map<String, Object> heightProp = new HashMap<>();
        heightProp.put("type", "integer");
        heightProp.put("description", "截取区域高度（可选，默认整个屏幕高度）");
        properties.put("height", heightProp);

        Map<String, Object> formatProp = new HashMap<>();
        formatProp.put("type", "string");
        formatProp.put("description", "图片格式（可选，png或jpg，默认png）");
        formatProp.put("enum", Arrays.asList("png", "jpg"));
        properties.put("format", formatProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.emptyList());

        server.addTool("dlqq_capture_screen",
            "获取桌面图像（屏幕截图）。默认截取整个屏幕，也可通过x/y/width/height截取指定区域。" +
            "图片保存到文件后返回文件路径，可用于查看当前屏幕内容。",
            inputSchema,
            args -> {
                try {
                    // 确定输出路径
                    String outputPath = (String) args.get("output_path");
                    String format = args.get("format") != null ? String.valueOf(args.get("format")).toLowerCase() : "png";
                    if (!"png".equals(format) && !"jpg".equals(format)) {
                        format = "png";
                    }

                    Path target;
                    if (outputPath != null && !outputPath.trim().isEmpty()) {
                        Path requested = Workspace.resolve(outputPath.trim());
                        String permissionError = FileTools.checkWritePermission(requested.toString(), "屏幕截图输出");
                        if (permissionError != null) {
                            return error(permissionError);
                        }
                        target = requested;
                    } else {
                        // 默认保存到 .dlqq/cache 目录
                        Path cacheDir = Workspace.getRoot().resolve(".dlqq/cache");
                        Files.createDirectories(cacheDir);
                        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                        target = cacheDir.resolve("screenshot_" + timestamp + "." + format);
                    }

                    // 确保输出目录存在
                    if (target.getParent() != null) {
                        Files.createDirectories(target.getParent());
                    }

                    // 计算截取区域
                    Rectangle captureRect = getScreenBounds();
                    int x = args.get("x") != null ? toInt(args.get("x")) : captureRect.x;
                    int y = args.get("y") != null ? toInt(args.get("y")) : captureRect.y;
                    int width = args.get("width") != null ? toInt(args.get("width")) : captureRect.width;
                    int height = args.get("height") != null ? toInt(args.get("height")) : captureRect.height;

                    // 限制在屏幕范围内
                    Rectangle rect = new Rectangle(x, y, width, height);
                    rect = rect.intersection(captureRect);
                    if (rect.isEmpty()) {
                        return error("截取区域超出屏幕范围: x=" + x + ", y=" + y + ", w=" + width + ", h=" + height +
                            "（屏幕范围: " + captureRect + "）");
                    }

                    // 截图（Robot 截取的图像不包含鼠标光标）
                    Robot robot = new Robot();
                    BufferedImage image = robot.createScreenCapture(rect);

                    // 将当前鼠标光标绘制到截图上（Windows 平台通过 JNA 获取真实光标位图）
                    boolean cursorDrawn = WindowsNativeUtils.drawCursor(image, rect);

                    // 保存
                    boolean written = ImageIO.write(image, format, target.toFile());
                    if (!written) {
                        return error("图片保存失败（不支持的格式: " + format + "）");
                    }

                    long fileSize = Files.size(target);

                    StringBuilder sb = new StringBuilder();
                    sb.append("【截图成功】\n");
                    sb.append("===================\n\n");
                    sb.append("文件路径: ").append(target.toAbsolutePath()).append("\n");
                    sb.append("图片格式: ").append(format).append("\n");
                    sb.append("尺寸: ").append(rect.width).append(" x ").append(rect.height).append(" 像素\n");
                    sb.append("截取区域: x=").append(rect.x).append(", y=").append(rect.y)
                      .append(", w=").append(rect.width).append(", h=").append(rect.height).append("\n");
                    sb.append("文件大小: ").append(fileSize).append(" 字节 (")
                      .append(String.format("%.1f", fileSize / 1024.0)).append(" KB)\n");
                    sb.append("屏幕总尺寸: ").append(captureRect.width).append(" x ").append(captureRect.height).append("\n");
                    sb.append("鼠标光标: ").append(cursorDrawn ? "已绘制到截图中" : "未绘制（光标不在截取区域内、被隐藏或平台不支持）").append("\n");
                    sb.append("\n提示: 可使用 dlqq_read_bin_file 读取图片内容，或告知用户查看该文件。");

                    return ok(sb.toString());
                } catch (Exception e) {
                    return error("截图失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 获取所有屏幕合并后的边界（支持多显示器）
     */
    private static Rectangle getScreenBounds() {
        Rectangle all = new Rectangle();
        for (GraphicsDevice gd : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
            all = all.union(gd.getDefaultConfiguration().getBounds());
        }
        return all;
    }

    private static CallToolResult ok(String text) {
        return new CallToolResult(Collections.singletonList(ToolContent.text(text)), false);
    }

    private static CallToolResult error(String text) {
        return new CallToolResult(Collections.singletonList(ToolContent.text(text)), true);
    }

    private static int toInt(Object obj) throws NumberFormatException {
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        return Integer.parseInt(String.valueOf(obj).trim());
    }
}

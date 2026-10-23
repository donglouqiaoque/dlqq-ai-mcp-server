package com.donglouqiaoque.dlqqaimcpserver.mcp.tools;

import com.donglouqiaoque.dlqqaimcpserver.mcp.protocol.McpSchema.CallToolResult;
import com.donglouqiaoque.dlqqaimcpserver.mcp.protocol.McpSchema.ToolContent;
import com.donglouqiaoque.dlqqaimcpserver.mcp.protocol.McpSchema.ToolInputSchema;
import com.donglouqiaoque.dlqqaimcpserver.mcp.server.McpServer;
import com.donglouqiaoque.dlqqaimcpserver.utils.Workspace;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 图片工具 - 图片格式互转（SVG、PNG、JPG、GIF、BMP）
 *
 * <p>转换规则：</p>
 * <ul>
 *   <li>SVG → PNG/JPG/GIF/BMP：通过 Apache Batik 渲染为位图后写出；</li>
 *   <li>光栅格式（PNG/JPG/GIF/BMP）互转：通过 ImageIO 直接转换（JPG 可指定质量）；</li>
 *   <li>光栅 → SVG：以 data URI 嵌入位图的方式生成 SVG（非矢量化，内容为嵌入图片）。</li>
 * </ul>
 */
public class ImageTools {

    /** 支持的目标格式集合 */
    private static final Set<String> SUPPORTED_FORMATS =
            new HashSet<>(Arrays.asList("svg", "png", "jpg", "jpeg", "gif", "bmp"));

    public static void registerTools(McpServer server) {
        registerConvertImageTool(server);
    }

    /**
     * dlqq_convert_image - 图片格式转换
     */
    private static void registerConvertImageTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> srcProp = new HashMap<>();
        srcProp.put("type", "string");
        srcProp.put("description", "源图片文件路径（支持 svg/png/jpg/jpeg/gif/bmp 格式）");
        properties.put("source_path", srcProp);

        Map<String, Object> fmtProp = new HashMap<>();
        fmtProp.put("type", "string");
        fmtProp.put("description", "目标格式: svg/png/jpg/gif/bmp");
        fmtProp.put("enum", Arrays.asList("svg", "png", "jpg", "gif", "bmp"));
        properties.put("target_format", fmtProp);

        Map<String, Object> outProp = new HashMap<>();
        outProp.put("type", "string");
        outProp.put("description", "输出文件路径（可选，默认保存到 .dlqq/cache 目录下带时间戳的文件）");
        properties.put("output_path", outProp);

        Map<String, Object> qualityProp = new HashMap<>();
        qualityProp.put("type", "integer");
        qualityProp.put("description", "JPG 输出质量（1-100，默认 90，仅对 jpg 格式有效）");
        properties.put("quality", qualityProp);

        ToolInputSchema schema = new ToolInputSchema(properties, Arrays.asList("source_path", "target_format"));

        server.addTool("dlqq_convert_image",
            "图片格式转换工具。支持 SVG、PNG、JPG、GIF、BMP 等图片格式互转：" +
            "SVG 可转换为任意光栅格式；光栅格式（png/jpg/gif/bmp）之间可互转；" +
            "光栅转 SVG 时以位图嵌入方式生成 SVG（非矢量化）。",
            schema,
            args -> {
                try {
                    String srcStr = String.valueOf(args.get("source_path")).trim();
                    String fmt = String.valueOf(args.get("target_format")).toLowerCase(Locale.ROOT);
                    if ("jpeg".equals(fmt)) {
                        fmt = "jpg";
                    }
                    if (!SUPPORTED_FORMATS.contains(fmt) || "jpeg".equals(fmt)) {
                        return error("不支持的目标格式: " + fmt + "（支持 svg/png/jpg/gif/bmp）");
                    }

                    Path src = Workspace.resolve(srcStr);
                    if (!Files.exists(src) || !Files.isRegularFile(src)) {
                        return error("源文件不存在: " + src);
                    }

                    String srcExt = extension(src).toLowerCase(Locale.ROOT);
                    BufferedImage img;
                    if ("svg".equals(srcExt)) {
                        if ("svg".equals(fmt)) {
                            return error("源文件已经是 SVG 格式，无需转换");
                        }
                        img = renderSvg(src);
                    } else {
                        img = ImageIO.read(src.toFile());
                        if (img == null) {
                            return error("无法识别的源图片格式: " + srcExt + "（支持 svg/png/jpg/jpeg/gif/bmp）");
                        }
                    }

                    Path target = resolveTarget(args, src, fmt);
                    if ("svg".equals(fmt)) {
                        writeSvgEmbedded(img, target);
                    } else {
                        writeRaster(img, fmt, target, quality(args));
                    }

                    return ok(resultText(src, target, fmt, img.getWidth(), img.getHeight()));
                } catch (Exception e) {
                    return error("图片转换失败: " + e.getMessage());
                }
            }
        );
    }

    // ==================== 核心转换逻辑 ====================

    /**
     * SVG → 位图（通过 Batik PNGTranscoder 渲染）
     */
    private static BufferedImage renderSvg(Path svg) throws IOException {
        try {
            PNGTranscoder transcoder = new PNGTranscoder();
            TranscoderInput input = new TranscoderInput(svg.toUri().toString());
            ByteArrayOutputStream ostream = new ByteArrayOutputStream();
            TranscoderOutput output = new TranscoderOutput(ostream);
            transcoder.transcode(input, output);
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(ostream.toByteArray()));
            if (img == null) {
                throw new IOException("SVG 渲染结果为空");
            }
            return img;
        } catch (TranscoderException e) {
            throw new IOException("SVG 渲染失败: " + e.getMessage(), e);
        }
    }

    /**
     * 位图 → 光栅格式（png/jpg/gif/bmp）
     */
    private static void writeRaster(BufferedImage img, String fmt, Path target, float quality) throws IOException {
        if ("jpg".equals(fmt)) {
            // JPG 不支持透明通道，白底填充
            BufferedImage rgb = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgb.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
            g.drawImage(img, 0, 0, null);
            g.dispose();

            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(target.toFile())) {
                writer.setOutput(ios);
                ImageWriteParam param = writer.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
                writer.write(null, new IIOImage(rgb, null, null), param);
            } finally {
                writer.dispose();
            }
            return;
        }
        boolean written = ImageIO.write(img, fmt, target.toFile());
        if (!written) {
            throw new IOException("不支持的输出格式: " + fmt);
        }
    }

    /**
     * 位图 → SVG（以 PNG data URI 嵌入，非矢量化）
     */
    private static void writeSvgEmbedded(BufferedImage img, Path target) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        if (!ImageIO.write(img, "png", bos)) {
            throw new IOException("位图编码为 PNG 失败");
        }
        String b64 = Base64.getEncoder().encodeToString(bos.toByteArray());
        int w = img.getWidth();
        int h = img.getHeight();
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" " +
                "xmlns:xlink=\"http://www.w3.org/1999/xlink\" " +
                "width=\"" + w + "\" height=\"" + h + "\" viewBox=\"0 0 " + w + " " + h + "\">\n" +
                "  <image width=\"" + w + "\" height=\"" + h + "\" " +
                "xlink:href=\"data:image/png;base64," + b64 + "\"/>\n" +
                "</svg>\n";
        Files.writeString(target, svg, StandardCharsets.UTF_8);
    }

    // ==================== 辅助方法 ====================

    /**
     * 解析输出路径；未指定时默认输出到 .dlqq/cache 下带时间戳的文件
     */
    private static Path resolveTarget(Map<String, Object> args, Path src, String fmt) throws IOException {
        Object outObj = args.get("output_path");
        if (outObj != null && !String.valueOf(outObj).trim().isEmpty()) {
            Path p = Workspace.resolve(String.valueOf(outObj).trim());
            String permissionError = FileTools.checkWritePermission(p.toString(), "图片转换输出");
            if (permissionError != null) {
                throw new IOException(permissionError);
            }
            if (p.getParent() != null) {
                Files.createDirectories(p.getParent());
            }
            return p;
        }
        Path cache = Workspace.getRoot().resolve(".dlqq/cache");
        Files.createDirectories(cache);
        String base = src.getFileName().toString();
        int dot = base.lastIndexOf('.');
        String stem = dot > 0 ? base.substring(0, dot) : base;
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return cache.resolve(stem + "_" + ts + "." + fmt);
    }

    /** 解析 JPG 质量（1-100 → 0.0-1.0），默认 90 */
    private static float quality(Map<String, Object> args) {
        int q = 90;
        Object qObj = args.get("quality");
        if (qObj != null) {
            try {
                q = Integer.parseInt(String.valueOf(qObj).trim());
            } catch (NumberFormatException ignored) {
                // 非法值用默认
            }
        }
        q = Math.max(1, Math.min(100, q));
        return q / 100f;
    }

    private static String extension(Path p) {
        String name = p.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1) : "";
    }

    private static String resultText(Path src, Path target, String fmt, int w, int h) throws IOException {
        long size = Files.size(target);
        StringBuilder sb = new StringBuilder();
        sb.append("【图片转换成功】\n");
        sb.append("===================\n\n");
        sb.append("源文件: ").append(src.toAbsolutePath()).append("\n");
        sb.append("输出文件: ").append(target.toAbsolutePath()).append("\n");
        sb.append("目标格式: ").append(fmt).append("\n");
        sb.append("图片尺寸: ").append(w).append(" x ").append(h).append(" 像素\n");
        sb.append("文件大小: ").append(size).append(" 字节 (")
          .append(String.format("%.1f", size / 1024.0)).append(" KB)\n");
        if ("svg".equals(fmt)) {
            sb.append("\n说明: 光栅转 SVG 为位图嵌入方式（非矢量化），图片内容保持不变。");
        }
        return sb.toString();
    }

    private static CallToolResult ok(String text) {
        return new CallToolResult(Collections.singletonList(ToolContent.text(text)), false);
    }

    private static CallToolResult error(String text) {
        return new CallToolResult(Collections.singletonList(ToolContent.text(text)), true);
    }
}

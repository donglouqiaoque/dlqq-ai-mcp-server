package com.donglouqiaoque.aitools.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 文件工具类
 */
public class FileUtils {

    /**
     * 规范化文件路径，统一处理正斜杠和反斜杠
     * 先将所有反斜杠替换为正斜杠，再根据系统转换为默认分隔符
     *
     * @param filePath 原始文件路径
     * @return 规范化后的路径
     */
    public static String normalizePath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return filePath;
        }
        // 先将反斜杠替换为正斜杠（处理JSON转义问题）
        String normalized = filePath.replace("\\", "/");
        // 再将正斜杠替换为系统默认分隔符
        return normalized.replace("/", java.io.File.separator);
    }

    /**
     * 读取文件内容为字符串
     *
     * @param filePath 文件路径
     * @return 文件内容
     * @throws IOException 读取异常
     */
    public static String readFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("文件不存在: " + filePath);
        }
        if (!Files.isReadable(path)) {
            throw new IOException("文件无法读取: " + filePath);
        }
        return Files.readString(path);
    }

    /**
     * 读取文件行为列表
     *
     * @param filePath 文件路径
     * @return 行列表
     * @throws IOException 读取异常
     */
    public static List<String> readFileLines(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("文件不存在: " + filePath);
        }
        return Files.readAllLines(path);
    }

    /**
     * 写入文件
     *
     * @param filePath 文件路径
     * @param content  内容
     * @throws IOException 写入异常
     */
    public static void writeFile(String filePath, String content) throws IOException {
        Path path = Paths.get(filePath);
        // 确保父目录存在
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, content);
    }

    /**
     * 追加写入文件
     *
     * @param filePath 文件路径
     * @param content  内容
     * @throws IOException 写入异常
     */
    public static void appendFile(String filePath, String content) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            writeFile(filePath, content);
        } else {
            String existing = readFile(filePath);
            writeFile(filePath, existing + content);
        }
    }

    /**
     * 检查文件是否存在
     *
     * @param filePath 文件路径
     * @return 是否存在
     */
    public static boolean exists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }

    /**
     * 获取文件大小
     *
     * @param filePath 文件路径
     * @return 文件大小（字节）
     * @throws IOException 读取异常
     */
    public static long getFileSize(String filePath) throws IOException {
        return Files.size(Paths.get(filePath));
    }

    /**
     * 获取文件扩展名
     *
     * @param filePath 文件路径
     * @return 扩展名（不含点）
     */
    public static String getExtension(String filePath) {
        int lastDot = filePath.lastIndexOf('.');
        if (lastDot == -1 || lastDot == filePath.length() - 1) {
            return "";
        }
        return filePath.substring(lastDot + 1).toLowerCase();
    }

    /**
     * 获取文件名（不含扩展名）
     *
     * @param filePath 文件路径
     * @return 文件名
     */
    public static String getFileNameWithoutExtension(String filePath) {
        String fileName = Paths.get(filePath).getFileName().toString();
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1) {
            return fileName;
        }
        return fileName.substring(0, lastDot);
    }

    /**
     * 检测文件编码（简化版，假设UTF-8）
     *
     * @param filePath 文件路径
     * @return 编码名称
     */
    public static String detectEncoding(String filePath) {
        // 简化实现，实际项目中可以使用 juniversalchardet 等库
        return "UTF-8";
    }

    /**
     * 限制文本长度
     *
     * @param text   原始文本
     * @param maxLen 最大长度
     * @return 截断后的文本
     */
    public static String truncate(String text, int maxLen) {
        if (text == null || text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen) + "... (已截断)";
    }
}

package com.donglouqiaoque.aitools.mcp.tools;

import com.donglouqiaoque.aitools.utils.FileUtils;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 文件操作工具 - 提供文件读写、修改、复制移动等功能
 */
public class FileTools {

    // 文件读取跟踪：记录每个文件被读取的行范围
    // key: 文件绝对路径, value: 读取的行号集合（使用TreeSet便于范围查询）
    private static final Map<String, Set<Integer>> fileReadTracker = new ConcurrentHashMap<>();

    // 文件修改标记：记录文件是否被修改过
    // key: 文件绝对路径, value: 是否已修改
    private static final Map<String, Boolean> fileModifiedFlag = new ConcurrentHashMap<>();

    /**
     * 记录文件读取的行范围
     */
    private static void recordFileRead(String filePath, int startLine, int endLine) {
        String normalizedPath = FileUtils.normalizePath(filePath);
        Set<Integer> readLines = fileReadTracker.computeIfAbsent(normalizedPath, k -> new TreeSet<>());
        for (int i = startLine; i <= endLine; i++) {
            readLines.add(i);
        }
    }

    /**
     * 检查是否已读取指定行范围
     * @return 未读取的行号列表，如果都已读取则返回空列表
     */
    private static List<Integer> checkUnreadLines(String filePath, int startLine, int endLine) {
        String normalizedPath = FileUtils.normalizePath(filePath);
        Set<Integer> readLines = fileReadTracker.get(normalizedPath);
        List<Integer> unreadLines = new ArrayList<>();

        if (readLines == null || readLines.isEmpty()) {
            // 没有读取过该文件
            for (int i = startLine; i <= endLine; i++) {
                unreadLines.add(i);
            }
            return unreadLines;
        }

        // 检查每个要修改的行是否已读取
        for (int i = startLine; i <= endLine; i++) {
            if (!readLines.contains(i)) {
                unreadLines.add(i);
            }
        }
        return unreadLines;
    }

    /**
     * 标记文件已被修改（会清空读取记录）
     */
    private static void markFileModified(String filePath) {
        String normalizedPath = FileUtils.normalizePath(filePath);
        fileModifiedFlag.put(normalizedPath, true);
        // 清空读取记录，要求重新读取
        fileReadTracker.remove(normalizedPath);
    }

    /**
     * 检查文件是否被修改过
     */
    private static boolean isFileModified(String filePath) {
        String normalizedPath = FileUtils.normalizePath(filePath);
        return fileModifiedFlag.getOrDefault(normalizedPath, false);
    }

    /**
     * 重置文件的修改标记和读取记录
     */
    public static void resetFileTracking(String filePath) {
        String normalizedPath = FileUtils.normalizePath(filePath);
        fileModifiedFlag.remove(normalizedPath);
        fileReadTracker.remove(normalizedPath);
    }

    /**
     * 重置文件的修改标记（不清除读取记录）
     * 当AI重新读取文件后调用，允许基于新内容继续修改
     */
    private static void resetFileModified(String filePath) {
        String normalizedPath = FileUtils.normalizePath(filePath);
        fileModifiedFlag.remove(normalizedPath);
    }

    /**
     * 读取文件内容（支持行范围和字符集）
     *
     * @param filePath   文件路径
     * @param startLine  起始行（1-based，包含）
     * @param endLine    结束行（1-based，包含），-1表示到文件末尾
     * @param readLines  读取行数（与endLine互斥，优先使用endLine）
     * @param charset    字符集（如 UTF-8、GBK、ISO-8859-1 等，默认为 UTF-8）
     * @return 指定范围的文件内容
     * @throws IOException 读取异常
     */
    public static String readFile(String filePath, int startLine, int endLine, int readLines, String charset) throws IOException {
        filePath = FileUtils.normalizePath(filePath);
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("文件不存在: " + filePath);
        }
        if (!Files.isReadable(path)) {
            throw new IOException("文件无法读取: " + filePath);
        }

        // 解析字符集，默认为 UTF-8
        Charset cs = StandardCharsets.UTF_8;
        if (charset != null && !charset.trim().isEmpty()) {
            try {
                cs = Charset.forName(charset.trim());
            } catch (Exception e) {
                throw new IOException("不支持的字符集: " + charset + "，请使用 UTF-8、GBK、ISO-8859-1 等标准字符集");
            }
        }

        // 使用 FileChannel 以共享模式读取文件，避免多进程竞争
        List<String> lines = readAllLinesWithSharedLock(path, cs);
        if (lines.isEmpty()) {
            return "";
        }

        // 验证起始行
        if (startLine < 1) {
            startLine = 1;
        }
        if (startLine > lines.size()) {
            throw new IOException("起始行超出文件范围，文件共 " + lines.size() + " 行");
        }

        // 计算结束行
        if (endLine < 0 && readLines > 0) {
            // 使用readLines计算endLine
            endLine = startLine + readLines - 1;
        }

        // 默认到文件末尾
        if (endLine < 0 || endLine > lines.size()) {
            endLine = lines.size();
        }

        if (endLine < startLine) {
            throw new IOException("结束行必须大于等于起始行");
        }

        StringBuilder result = new StringBuilder();
        for (int i = startLine - 1; i < endLine && i < lines.size(); i++) {
            // 添加行号（i+1是因为行号从1开始）
            result.append(String.format("%-4d | %s", i + 1, lines.get(i)));
            if (i < endLine - 1) {
                result.append(System.lineSeparator());
            }
        }

        // 记录本次读取的行范围
        recordFileRead(filePath, startLine, endLine);

        // 如果文件之前被修改过，现在AI重新读取了最新内容
        // 重置修改标记，允许基于新读取的内容继续修改
        resetFileModified(filePath);

        return result.toString();
    }

    /**
     * 使用共享读锁读取文件所有行，避免多进程并发访问时的文件竞争
     */
    private static List<String> readAllLinesWithSharedLock(Path path, Charset charset) throws IOException {
        List<String> lines = new ArrayList<>();

        // 使用 FileChannel 以只读模式打开文件，允许其他进程同时读取
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ);
             InputStream is = Channels.newInputStream(channel);
             Reader reader = new InputStreamReader(is, charset);
             BufferedReader br = new BufferedReader(reader)) {

            // 在 Windows 上尝试获取共享锁（读锁），非阻塞
            // 在 Linux/Mac 上，这个锁是建议性的
            FileLock lock = null;
            try {
                lock = channel.tryLock(0, Long.MAX_VALUE, true); // true = 共享锁
            } catch (Exception e) {
                // 某些系统不支持共享锁，继续无锁读取
            }

            try {
                String line;
                while ((line = br.readLine()) != null) {
                    lines.add(line);
                }
            } finally {
                if (lock != null) {
                    try {
                        lock.release();
                    } catch (Exception e) {
                        // 忽略释放锁的错误
                    }
                }
            }
        }

        return lines;
    }

    /**
     * 检测文件是否以换行符结尾
     * @param path 文件路径
     * @return 如果文件以换行符结尾返回true，否则返回false
     */
    private static boolean fileEndsWithNewline(Path path) throws IOException {
        if (!Files.exists(path) || Files.size(path) == 0) {
            return false;
        }
        // 读取文件最后几个字节检查是否以换行符结尾
        try (InputStream is = Files.newInputStream(path)) {
            long fileSize = Files.size(path);
            int checkSize = (int) Math.min(fileSize, 10); // 检查最后10个字节
            byte[] buffer = new byte[checkSize];
            is.skip(fileSize - checkSize);
            int read = is.read(buffer);
            if (read > 0) {
                // 检查最后一个有效字节是否是换行符
                for (int i = read - 1; i >= 0; i--) {
                    byte b = buffer[i];
                    if (b == '\n' || b == '\r') {
                        return true;
                    }
                    // 如果遇到非空白字符，说明文件不以换行符结尾
                    if (b != ' ' && b != '\t') {
                        return false;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 将行列表写回文件，保留文件原始的换行符结尾特性
     * @param path 文件路径
     * @param lines 行列表
     * @param endsWithNewline 是否以换行符结尾
     * @param charset 字符集
     * @throws IOException 写入异常
     */
    private static void writeLinesToFile(Path path, List<String> lines, boolean endsWithNewline, Charset charset) throws IOException {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            result.append(lines.get(i));
            if (i < lines.size() - 1) {
                result.append(System.lineSeparator());
            }
        }
        // 如果原文件以换行符结尾，在最后一行后也添加换行符
        if (endsWithNewline && !lines.isEmpty()) {
            result.append(System.lineSeparator());
        }
        Files.writeString(path, result.toString(), charset);
    }

    /**
     * 创建文件
     *
     * @param filePath 文件路径
     * @param content  文件内容（null或空字符串则创建空文件）
     * @param charset  字符集（如 UTF-8、GBK、ISO-8859-1 等，默认为 UTF-8）
     * @throws IOException 创建异常
     */
    public static void createFile(String filePath, String content, String charset) throws IOException {
        filePath = FileUtils.normalizePath(filePath);
        Path path = Paths.get(filePath);

        // 解析字符集，默认为 UTF-8
        Charset cs = StandardCharsets.UTF_8;
        if (charset != null && !charset.trim().isEmpty()) {
            try {
                cs = Charset.forName(charset.trim());
            } catch (Exception e) {
                throw new IOException("不支持的字符集: " + charset + "，请使用 UTF-8、GBK、ISO-8859-1 等标准字符集");
            }
        }

        // 确保父目录存在
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        // 创建文件（如果不存在）
        if (!Files.exists(path)) {
            Files.createFile(path);
        }

        // 写入内容
        if (content != null && !content.isEmpty()) {
            Files.writeString(path, content, cs);
        }
    }

    /**
     * 覆盖文件内容（不管文件是否存在都直接写入）
     *
     * @param filePath 文件路径
     * @param content  文件内容
     * @param charset  字符集（如 UTF-8、GBK、ISO-8859-1 等，默认为 UTF-8）
     * @throws IOException 写入异常
     */
    public static void overwriteFile(String filePath, String content, String charset) throws IOException {
        filePath = FileUtils.normalizePath(filePath);
        Path path = Paths.get(filePath);

        // 解析字符集，默认为 UTF-8
        Charset cs = StandardCharsets.UTF_8;
        if (charset != null && !charset.trim().isEmpty()) {
            try {
                cs = Charset.forName(charset.trim());
            } catch (Exception e) {
                throw new IOException("不支持的字符集: " + charset + "，请使用 UTF-8、GBK、ISO-8859-1 等标准字符集");
            }
        }

        // 确保父目录存在
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        // 直接写入内容（覆盖原有内容或创建新文件）
        Files.writeString(path, content != null ? content : "", cs);
    }

    /**
     * 在文件末尾追加内容
     *
     * @param filePath 文件路径
     * @param content  追加的内容
     * @param charset  字符集（如 UTF-8、GBK、ISO-8859-1 等，默认为 UTF-8）
     * @throws IOException 写入异常
     */
    public static void appendFile(String filePath, String content, String charset) throws IOException {
        filePath = FileUtils.normalizePath(filePath);
        Path path = Paths.get(filePath);

        if (content == null || content.isEmpty()) {
            return;
        }

        // 解析字符集，默认为 UTF-8
        Charset cs = StandardCharsets.UTF_8;
        if (charset != null && !charset.trim().isEmpty()) {
            try {
                cs = Charset.forName(charset.trim());
            } catch (Exception e) {
                throw new IOException("不支持的字符集: " + charset + "，请使用 UTF-8、GBK、ISO-8859-1 等标准字符集");
            }
        }

        // 确保父目录存在
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        // 如果文件不存在，创建新文件；否则追加内容
        if (!Files.exists(path)) {
            Files.writeString(path, content, cs);
        } else {
            Files.writeString(path, content, cs, java.nio.file.StandardOpenOption.APPEND);
        }
    }

    /**
     * 删除文件（带确认对话框）
     *
     * @param filePath 文件路径
     * @return 是否成功删除
     * @throws IOException 删除异常
     */
    public static boolean deleteFile(String filePath) throws IOException {
        filePath = FileUtils.normalizePath(filePath);
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("文件不存在: " + filePath);
        }

        // 弹出确认对话框
        String fileName = path.getFileName().toString();
        boolean confirmed = DialogTools.showConfirmDialog(
                "确认删除",
                "确定要删除文件 \"" + fileName + "\" 吗？\n路径: " + filePath + "\n\n此操作不可恢复！"
        );

        if (!confirmed) {
            return false;
        }

        Files.delete(path);
        return true;
    }

    /**
     * 复制文件
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     * @throws IOException 复制异常
     */
    public static void copyFile(String sourcePath, String targetPath) throws IOException {
        sourcePath = FileUtils.normalizePath(sourcePath);
        targetPath = FileUtils.normalizePath(targetPath);
        Path source = Paths.get(sourcePath);
        if (!Files.exists(source)) {
            throw new IOException("源文件不存在: " + sourcePath);
        }
        if (!Files.isRegularFile(source)) {
            throw new IOException("源路径不是文件: " + sourcePath);
        }

        Path target = Paths.get(targetPath);
        // 确保目标目录存在
        Path parent = target.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * 移动文件
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     * @throws IOException 移动异常
     */
    public static void moveFile(String sourcePath, String targetPath) throws IOException {
        sourcePath = FileUtils.normalizePath(sourcePath);
        targetPath = FileUtils.normalizePath(targetPath);
        Path source = Paths.get(sourcePath);
        if (!Files.exists(source)) {
            throw new IOException("源文件不存在: " + sourcePath);
        }
        if (!Files.isRegularFile(source)) {
            throw new IOException("源路径不是文件: " + sourcePath);
        }

        Path target = Paths.get(targetPath);
        // 确保目标目录存在
        Path parent = target.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * 重命名文件
     *
     * @param sourcePath 源文件路径
     * @param newName    新文件名（仅文件名，不包含路径）
     * @throws IOException 重命名异常
     */
    public static void renameFile(String sourcePath, String newName) throws IOException {
        sourcePath = FileUtils.normalizePath(sourcePath);
        Path source = Paths.get(sourcePath);
        if (!Files.exists(source)) {
            throw new IOException("源文件不存在: " + sourcePath);
        }
        if (!Files.isRegularFile(source)) {
            throw new IOException("源路径不是文件: " + sourcePath);
        }

        // 构建目标路径（同目录，新文件名）
        Path target = source.getParent().resolve(newName);

        // 如果目标已存在，抛出异常
        if (Files.exists(target)) {
            throw new IOException("目标文件已存在: " + target);
        }

        Files.move(source, target);
    }

    /**
     * 重命名文件夹
     *
     * @param sourcePath 源文件夹路径
     * @param newName    新文件夹名（仅文件夹名，不包含路径）
     * @throws IOException 重命名异常
     */
    public static void renameDirectory(String sourcePath, String newName) throws IOException {
        sourcePath = FileUtils.normalizePath(sourcePath);
        Path source = Paths.get(sourcePath);
        if (!Files.exists(source)) {
            throw new IOException("源文件夹不存在: " + sourcePath);
        }
        if (!Files.isDirectory(source)) {
            throw new IOException("源路径不是文件夹: " + sourcePath);
        }

        // 构建目标路径（同目录，新文件夹名）
        Path target = source.getParent().resolve(newName);

        // 如果目标已存在，抛出异常
        if (Files.exists(target)) {
            throw new IOException("目标文件夹已存在: " + target);
        }

        Files.move(source, target);
    }

    /**
     * 删除指定行范围的内容
     *
     * @param filePath  文件路径
     * @param startLine 起始行（1-based，包含）
     * @param endLine   结束行（1-based，包含）
     * @param charset   字符集（如 UTF-8、GBK、ISO-8859-1 等，默认为 UTF-8）
     * @return 删除结果报告，包含修改前后的对比
     * @throws IOException 操作异常
     */
    public static String removeLinesInFile(String filePath, int startLine, int endLine, String charset) throws IOException {
        filePath = FileUtils.normalizePath(filePath);
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("文件不存在: " + filePath);
        }

        // 解析字符集，默认为 UTF-8
        Charset cs = StandardCharsets.UTF_8;
        if (charset != null && !charset.trim().isEmpty()) {
            try {
                cs = Charset.forName(charset.trim());
            } catch (Exception e) {
                throw new IOException("不支持的字符集: " + charset + "，请使用 UTF-8、GBK、ISO-8859-1 等标准字符集");
            }
        }

        List<String> lines = Files.readAllLines(path, cs);
        if (lines.isEmpty()) {
            return "错误：文件为空";
        }

        // 验证行号
        if (endLine < 0) {
            endLine = lines.size();
        }
        if (startLine < 1) {
            startLine = 1;
        }
        if (endLine < 1) {
            endLine = 1;
        }
        if (endLine > lines.size()) {
            endLine = lines.size();
        }
        if (startLine > endLine) {
            throw new IOException("起始行不能大于结束行");
        }

        // ========== 读取验证 ==========
        // 检查文件是否被修改过
        if (isFileModified(filePath)) {
            return "【拒绝修改】文件已被修改过，读取记录已重置。请先重新读取文件内容，确认最新状态后再修改。";
        }

        // 检查是否已读取要修改的所有行
        List<Integer> unreadLines = checkUnreadLines(filePath, startLine, endLine);
        if (!unreadLines.isEmpty()) {
            StringBuilder errorMsg = new StringBuilder();
            errorMsg.append("【拒绝修改】您尚未读取以下行，请先使用 dlqq_read_file 读取后再修改：\n");
            int showCount = Math.min(unreadLines.size(), 10);
            for (int i = 0; i < showCount; i++) {
                errorMsg.append("  - 第 ").append(unreadLines.get(i)).append(" 行\n");
            }
            if (unreadLines.size() > 10) {
                errorMsg.append("  ... 还有 ").append(unreadLines.size() - 10).append(" 行未读取\n");
            }
            errorMsg.append("\n建议：使用 dlqq_read_file 读取第 ").append(startLine).append(" 行到第 ").append(endLine).append(" 行的内容。");
            return errorMsg.toString();
        }

        // 保存被删除的内容（用于对比）
        StringBuilder removedContent = new StringBuilder();
        for (int i = startLine - 1; i < endLine && i < lines.size(); i++) {
            if (i > startLine - 1) {
                removedContent.append("\n");
            }
            removedContent.append(lines.get(i));
        }

        // 删除指定行
        List<String> newLines = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            int lineNum = i + 1;
            if (lineNum < startLine || lineNum > endLine) {
                newLines.add(lines.get(i));
            }
        }

        // 检测原文件是否以换行符结尾，保留这个特性
        boolean endsWithNewline = fileEndsWithNewline(path);
        writeLinesToFile(path, newLines, endsWithNewline, cs);

        // 标记文件已被修改
        markFileModified(filePath);

        // 生成删除报告（包含上下文）
        return generateDeleteReportWithContext(lines, newLines, startLine, endLine, removedContent.toString());
    }

    /**
     * 生成删除操作对比报告（包含上下文）
     */
    private static String generateDeleteReportWithContext(List<String> oldLines, List<String> newLines,
                                                           int startLine, int endLine, String removedContent) {
        StringBuilder report = new StringBuilder();
        report.append("╔════════════════════════════════════════════════════════════════╗\n");
        report.append("║  删除完成 - 修改前后对比报告                                    ║\n");
        report.append("╚════════════════════════════════════════════════════════════════╝\n\n");

        report.append("【位置】第 ").append(startLine).append(" 行 至 第 ").append(endLine).append(" 行\n\n");

        int oldTotalLines = oldLines.size();
        int newTotalLines = newLines.size();
        int deletedCount = endLine - startLine + 1;

        // ========== 展示删除前的上下文 ==========
        report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        report.append("【删除前的文件状态】\n");
        report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        // 上文（前5行）
        int contextBefore = 5;
        int contextStart = Math.max(1, startLine - contextBefore);
        if (contextStart < startLine) {
            report.append("... 上文（第 ").append(contextStart).append("-").append(startLine - 1).append(" 行）...\n");
            for (int i = contextStart - 1; i < startLine - 1 && i < oldLines.size(); i++) {
                report.append(String.format("  %-4d | %s\n", i + 1, oldLines.get(i)));
            }
            report.append("\n");
        }

        // 被删除的内容
        report.append("【已删除的内容】\n");
        if (removedContent != null && !removedContent.isEmpty()) {
            String[] removedContentLines = removedContent.split("\n");
            for (int i = 0; i < removedContentLines.length; i++) {
                report.append(String.format("- %-4d | %s\n", startLine + i, removedContentLines[i]));
            }
        } else {
            report.append("(空)\n");
        }

        // 下文（后5行）
        int contextAfter = 5;
        int contextEnd = Math.min(oldTotalLines, endLine + contextAfter);
        if (endLine < contextEnd) {
            report.append("\n");
            report.append("... 下文（第 ").append(endLine + 1).append("-").append(contextEnd).append(" 行）...\n");
            for (int i = endLine; i < contextEnd && i < oldLines.size(); i++) {
                report.append(String.format("  %-4d | %s\n", i + 1, oldLines.get(i)));
            }
        }

        report.append("\n\n");

        // ========== 展示删除后的上下文 ==========
        report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        report.append("【删除后的文件状态】\n");
        report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        // 上文（前5行）- 在新文件中（位置不变）
        if (contextStart < startLine) {
            report.append("... 上文（第 ").append(contextStart).append("-").append(startLine - 1).append(" 行）...\n");
            for (int i = contextStart - 1; i < startLine - 1 && i < newLines.size(); i++) {
                report.append(String.format("  %-4d | %s\n", i + 1, newLines.get(i)));
            }
            report.append("\n");
        }

        // 删除位置的标记
        report.append("【删除位置】第 ").append(startLine).append("-").append(endLine).append(" 行 (已删除)\n");

        // 下文（后5行）- 在新文件中（行号已变化）
        int newContextStart = startLine; // 删除后，原来的下文从startLine开始
        int newContextEnd = Math.min(newTotalLines, startLine - 1 + contextAfter);
        if (newContextStart <= newTotalLines && newContextStart <= newContextEnd) {
            report.append("\n");
            report.append("... 下文（原第 ").append(endLine + 1).append(" 行起，现第 ").append(newContextStart).append(" 行）...\n");
            for (int i = newContextStart - 1; i < newContextEnd && i < newLines.size(); i++) {
                report.append(String.format("  %-4d | %s\n", i + 1, newLines.get(i)));
            }
        }

        report.append("\n\n");

        // 统计信息
        int diff = oldTotalLines - newTotalLines;

        report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        report.append("【统计信息】\n");
        report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        report.append("  原文件总行数：").append(oldTotalLines).append(" 行\n");
        report.append("  新文件总行数：").append(newTotalLines).append(" 行\n");
        report.append("  删除行数：   -").append(deletedCount).append(" 行\n");
        report.append("  实际减少：   -").append(diff).append(" 行\n");
        report.append("\n");
        report.append("【状态】✓ 删除成功\n");

        // 结构检查提示
        report.append("\n");
        report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        report.append("【结构检查】\n");
        report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        report.append("  请检查删除后的上下文连接是否正确。\n");
        report.append("  上文和下文应该连续，不应该出现跳跃或重复。\n");

        return report.toString();
    }

    /**
     * 替换指定行范围的内容
     *
     * @param filePath    文件路径
     * @param startLine   起始行（1-based，包含）
     * @param endLine     结束行（1-based，包含）
     * @param newContent  新的内容（支持多行）
     * @param charset     字符集（如 UTF-8、GBK、ISO-8859-1 等，默认为 UTF-8）
     * @return 替换结果报告，包含修改前后的对比
     * @throws IOException 操作异常
     */
    public static String replaceLinesInFile(String filePath, int startLine, int endLine, String newContent, String charset) throws IOException {
        filePath = FileUtils.normalizePath(filePath);
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("文件不存在: " + filePath);
        }

        // 解析字符集，默认为 UTF-8
        Charset cs = StandardCharsets.UTF_8;
        if (charset != null && !charset.trim().isEmpty()) {
            try {
                cs = Charset.forName(charset.trim());
            } catch (Exception e) {
                throw new IOException("不支持的字符集: " + charset + "，请使用 UTF-8、GBK、ISO-8859-1 等标准字符集");
            }
        }

        List<String> lines = Files.readAllLines(path, cs);
        if (lines.isEmpty()) {
            return "错误：文件为空";
        }

        // 验证行号
        if (endLine < 0) {
            endLine = lines.size();
        }
        if (startLine < 1) {
            startLine = 1;
        }
        if (endLine < 1) {
            endLine = 1;
        }
        if (endLine > lines.size()) {
            endLine = lines.size();
        }
        if (startLine > endLine) {
            throw new IOException("起始行不能大于结束行");
        }

        // ========== 读取验证 ==========
        // 检查文件是否被修改过
        if (isFileModified(filePath)) {
            return "【拒绝修改】文件已被修改过，读取记录已重置。请先重新读取文件内容，确认最新状态后再修改。";
        }

        // 检查是否已读取要修改的所有行
        List<Integer> unreadLines = checkUnreadLines(filePath, startLine, endLine);
        if (!unreadLines.isEmpty()) {
            StringBuilder errorMsg = new StringBuilder();
            errorMsg.append("【拒绝修改】您尚未读取以下行，请先使用 dlqq_read_file 读取后再修改：\n");
            // 显示前10个未读取的行号
            int showCount = Math.min(unreadLines.size(), 10);
            for (int i = 0; i < showCount; i++) {
                errorMsg.append("  - 第 ").append(unreadLines.get(i)).append(" 行\n");
            }
            if (unreadLines.size() > 10) {
                errorMsg.append("  ... 还有 ").append(unreadLines.size() - 10).append(" 行未读取\n");
            }
            errorMsg.append("\n建议：使用 dlqq_read_file 读取第 ").append(startLine).append(" 行到第 ").append(endLine).append(" 行的内容。");
            return errorMsg.toString();
        }

        // 保存原始内容（用于对比）
        StringBuilder oldContent = new StringBuilder();
        for (int i = startLine - 1; i < endLine && i < lines.size(); i++) {
            if (i > startLine - 1) {
                oldContent.append("\n");
            }
            oldContent.append(lines.get(i));
        }

        // 准备新内容行
        String[] newLines = newContent != null ? newContent.split("\\r?\\n") : new String[0];

        // 构建新文件内容
        List<String> resultLines = new ArrayList<>();

        // 添加替换范围之前的行
        for (int i = 0; i < startLine - 1; i++) {
            resultLines.add(lines.get(i));
        }

        // 添加新内容行
        for (String newLine : newLines) {
            resultLines.add(newLine);
        }

        // 添加替换范围之后的行
        for (int i = endLine; i < lines.size(); i++) {
            resultLines.add(lines.get(i));
        }

        // 检测原文件是否以换行符结尾，保留这个特性
        boolean endsWithNewline = fileEndsWithNewline(path);
        writeLinesToFile(path, resultLines, endsWithNewline, cs);

        // 标记文件已被修改
        markFileModified(filePath);

        // 生成对比报告（包含上下文）
        return generateDiffReportWithContext(lines, resultLines, startLine, endLine, oldContent.toString(), newContent);
    }

    /**
     * 生成修改前后对比报告（包含上下文）
     */
    private static String generateDiffReportWithContext(List<String> oldLines, List<String> newLines,
                                                         int startLine, int endLine, String oldContent, String newContent) {
        StringBuilder report = new StringBuilder();
        report.append("╔════════════════════════════════════════════════════════════════╗\n");
        report.append("║  替换完成 - 修改前后对比报告                                    ║\n");
        report.append("╚════════════════════════════════════════════════════════════════╝\n\n");

        report.append("【位置】第 ").append(startLine).append(" 行 至 第 ").append(endLine).append(" 行\n\n");

        int oldTotalLines = oldLines.size();
        int newTotalLines = newLines.size();
        int oldLineCount = (oldContent != null && !oldContent.isEmpty()) ? oldContent.split("\n").length : 0;
        int newLineCount = (newContent != null && !newContent.isEmpty()) ? newContent.split("\r?\n").length : 0;
        int newEndLine = startLine + newLineCount - 1;

        // ========== 展示修改前的上下文 ==========
        report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        report.append("【修改前的文件状态】\n");
        report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        // 上文（前5行）
        int contextBefore = 5;
        int contextStart = Math.max(1, startLine - contextBefore);
        if (contextStart < startLine) {
            report.append("... 上文（第 ").append(contextStart).append("-").append(startLine - 1).append(" 行）...\n");
            for (int i = contextStart - 1; i < startLine - 1 && i < oldLines.size(); i++) {
                report.append(String.format("  %-4d | %s\n", i + 1, oldLines.get(i)));
            }
            report.append("\n");
        }

        // 原始内容（被替换的部分）
        report.append("【原始内容 - 将被替换】\n");
        if (oldContent != null && !oldContent.isEmpty()) {
            String[] oldContentLines = oldContent.split("\n");
            for (int i = 0; i < oldContentLines.length; i++) {
                report.append(String.format("- %-4d | %s\n", startLine + i, oldContentLines[i]));
            }
        } else {
            report.append("(空)\n");
        }

        // 下文（后5行）
        int contextAfter = 5;
        int contextEnd = Math.min(oldTotalLines, endLine + contextAfter);
        if (endLine < contextEnd) {
            report.append("\n");
            report.append("... 下文（第 ").append(endLine + 1).append("-").append(contextEnd).append(" 行）...\n");
            for (int i = endLine; i < contextEnd && i < oldLines.size(); i++) {
                report.append(String.format("  %-4d | %s\n", i + 1, oldLines.get(i)));
            }
        }

        report.append("\n\n");

        // ========== 展示修改后的上下文 ==========
        report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        report.append("【修改后的文件状态】\n");
        report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        // 计算新文件中的对应位置
        int lineDiff = newLineCount - oldLineCount;
        int newContextEnd = Math.min(newTotalLines, newEndLine + contextAfter);

        // 上文（前5行）- 在新文件中
        int newContextStart = Math.max(1, startLine - contextBefore);
        if (newContextStart < startLine) {
            report.append("... 上文（第 ").append(newContextStart).append("-").append(startLine - 1).append(" 行）...\n");
            for (int i = newContextStart - 1; i < startLine - 1 && i < newLines.size(); i++) {
                report.append(String.format("  %-4d | %s\n", i + 1, newLines.get(i)));
            }
            report.append("\n");
        }

        // 新内容
        report.append("【新内容 - 已替换】\n");
        if (newContent != null && !newContent.isEmpty()) {
            String[] newContentLines = newContent.split("\r?\n");
            for (int i = 0; i < newContentLines.length; i++) {
                report.append(String.format("+ %-4d | %s\n", startLine + i, newContentLines[i]));
            }
        } else {
            report.append("(已删除)\n");
        }

        // 下文（后5行）- 在新文件中
        if (newEndLine < newContextEnd) {
            report.append("\n");
            report.append("... 下文（第 ").append(newEndLine + 1).append("-").append(newContextEnd).append(" 行）...\n");
            for (int i = newEndLine; i < newContextEnd && i < newLines.size(); i++) {
                report.append(String.format("  %-4d | %s\n", i + 1, newLines.get(i)));
            }
        }

        report.append("\n\n");

        // 统计信息
        report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        report.append("【统计信息】\n");
        report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        report.append("  原文件总行数：").append(oldTotalLines).append(" 行\n");
        report.append("  新文件总行数：").append(newTotalLines).append(" 行\n");
        report.append("  替换位置：    第 ").append(startLine).append("-").append(endLine).append(" 行\n");
        report.append("  新内容位置：  第 ").append(startLine).append("-").append(newEndLine).append(" 行\n");
        report.append("  原始行数：    ").append(oldLineCount).append(" 行\n");
        report.append("  新行数：      ").append(newLineCount).append(" 行\n");
        int diff = newLineCount - oldLineCount;
        if (diff > 0) {
            report.append("  变化：        +").append(diff).append(" 行 (新增)\n");
        } else if (diff < 0) {
            report.append("  变化：        ").append(diff).append(" 行 (删除)\n");
        } else {
            report.append("  变化：        0 行 (行数相同)\n");
        }
        report.append("\n");
        report.append("【状态】✓ 替换成功\n");

        // 结构检查提示
        report.append("\n");
        report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        report.append("【结构检查】\n");
        report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        report.append("  请检查上文和下文的连接是否正确，确认没有破坏文件结构。\n");
        report.append("  如果上下文不连续，可能需要重新调整修改范围。\n");

        return report.toString();
    }

    /**
     * 在指定行插入内容
     *
     * @param filePath 文件路径
     * @param lineNum  插入位置（1-based，插入到该行之前）
     * @param content  要插入的内容
     * @param charset  字符集（如 UTF-8、GBK、ISO-8859-1 等，默认为 UTF-8）
     * @return 插入结果报告，包含修改前后的对比
     * @throws IOException 操作异常
     */
    public static String insertTextInFile(String filePath, int lineNum, String content, String charset) throws IOException {
        filePath = FileUtils.normalizePath(filePath);
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("文件不存在: " + filePath);
        }

        // 解析字符集，默认为 UTF-8
        Charset cs = StandardCharsets.UTF_8;
        if (charset != null && !charset.trim().isEmpty()) {
            try {
                cs = Charset.forName(charset.trim());
            } catch (Exception e) {
                throw new IOException("不支持的字符集: " + charset + "，请使用 UTF-8、GBK、ISO-8859-1 等标准字符集");
            }
        }

        List<String> lines = Files.readAllLines(path, cs);
        int oldTotalLines = lines.size();

        // 验证行号
        if (lineNum < 1) {
            lineNum = 1;
        }
        if (lineNum > lines.size() + 1) {
            lineNum = lines.size() + 1;
        }

        // ========== 读取验证 ==========
        // 检查文件是否被修改过
        if (isFileModified(filePath)) {
            return "【拒绝修改】文件已被修改过，读取记录已重置。请先重新读取文件内容，确认最新状态后再修改。";
        }

        // 插入操作需要检查插入位置的上下文是否已读取
        // 检查插入点附近是否已读取（前后2行）
        int checkStart = Math.max(1, lineNum - 2);
        int checkEnd = Math.min(oldTotalLines, lineNum + 2);
        List<Integer> unreadLines = checkUnreadLines(filePath, checkStart, checkEnd);
        if (!unreadLines.isEmpty()) {
            StringBuilder errorMsg = new StringBuilder();
            errorMsg.append("【拒绝修改】您尚未读取插入位置附近的行，请先使用 dlqq_read_file 读取后再修改：\n");
            for (Integer unreadLine : unreadLines) {
                errorMsg.append("  - 第 ").append(unreadLine).append(" 行\n");
            }
            errorMsg.append("\n建议：使用 dlqq_read_file 读取第 ").append(checkStart).append(" 行到第 ").append(checkEnd).append(" 行的内容，了解插入位置的上下文。");
            return errorMsg.toString();
        }

        // 准备插入内容（支持多行）
        String[] insertLines = content.split("\\r?\\n");

        // 在指定位置插入
        int insertIndex = lineNum - 1;
        for (int i = insertLines.length - 1; i >= 0; i--) {
            lines.add(insertIndex, insertLines[i]);
        }

        // 检测原文件是否以换行符结尾，保留这个特性
        boolean endsWithNewline = fileEndsWithNewline(path);
        writeLinesToFile(path, lines, endsWithNewline, cs);

        // 标记文件已被修改
        markFileModified(filePath);

        // 生成插入报告（包含上下文）
        return generateInsertReportWithContext(lines, lineNum, content, oldTotalLines, lines.size());
    }

    /**
     * 生成插入操作对比报告（包含上下文）
     */
    private static String generateInsertReportWithContext(List<String> newLines, int lineNum, String insertedContent,
                                                         int oldTotalLines, int newTotalLines) {
        StringBuilder report = new StringBuilder();
        report.append("╔════════════════════════════════════════════════════════════════╗\n");
        report.append("║  插入完成 - 修改前后对比报告                                    ║\n");
        report.append("╚════════════════════════════════════════════════════════════════╝\n\n");

        report.append("【位置】在第 ").append(lineNum).append(" 行之前插入\n\n");

        int insertLineCount = (insertedContent != null && !insertedContent.isEmpty()) ? insertedContent.split("\\r?\\n").length : 0;
        int newStartLine = lineNum;
        int newEndLine = lineNum + insertLineCount - 1;

        // ========== 展示插入后的上下文 ==========
        report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        report.append("【插入后的文件状态】\n");
        report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        // 上文（前5行）
        int contextBefore = 5;
        int contextStart = Math.max(1, newStartLine - contextBefore);
        if (contextStart < newStartLine) {
            report.append("... 上文（第 ").append(contextStart).append("-").append(newStartLine - 1).append(" 行）...\n");
            for (int i = contextStart - 1; i < newStartLine - 1 && i < newLines.size(); i++) {
                report.append(String.format("  %-4d | %s\n", i + 1, newLines.get(i)));
            }
            report.append("\n");
        }

        // 插入的内容
        report.append("【插入的内容】\n");
        if (insertedContent != null && !insertedContent.isEmpty()) {
            String[] insertContentLines = insertedContent.split("\\r?\\n");
            for (int i = 0; i < insertContentLines.length; i++) {
                report.append(String.format("+ %-4d | %s\n", newStartLine + i, insertContentLines[i]));
            }
        } else {
            report.append("(空内容)\n");
        }

        // 下文（后5行）
        int contextAfter = 5;
        int contextEnd = Math.min(newTotalLines, newEndLine + contextAfter);
        if (newEndLine < contextEnd) {
            report.append("\n");
            report.append("... 下文（第 ").append(newEndLine + 1).append("-").append(contextEnd).append(" 行）...\n");
            for (int i = newEndLine; i < contextEnd && i < newLines.size(); i++) {
                report.append(String.format("  %-4d | %s\n", i + 1, newLines.get(i)));
            }
        }

        report.append("\n\n");

        // 统计信息
        int insertedCount = newTotalLines - oldTotalLines;

        report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        report.append("【统计信息】\n");
        report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        report.append("  原文件总行数：").append(oldTotalLines).append(" 行\n");
        report.append("  新文件总行数：").append(newTotalLines).append(" 行\n");
        report.append("  插入位置：    第 ").append(lineNum).append(" 行之前\n");
        report.append("  新插入行号：  第 ").append(newStartLine).append("-").append(newEndLine).append(" 行\n");
        report.append("  插入行数：    +").append(insertedCount).append(" 行\n");
        report.append("\n");
        report.append("【状态】✓ 插入成功\n");

        // 结构检查提示
        report.append("\n");
        report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        report.append("【结构检查】\n");
        report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        report.append("  请检查插入的内容与上下文的连接是否正确。\n");
        report.append("  上文、插入内容和下文应该构成完整的逻辑结构。\n");

        return report.toString();
    }

    /**
     * 替换文件中的文本
     *
     * @param filePath    文件路径
     * @param searchText  搜索文本
     * @param replaceText 替换文本
     * @param useRegex    是否使用正则表达式
     * @param charset     字符集（如 UTF-8、GBK、ISO-8859-1 等，默认为 UTF-8）
     * @return 替换结果报告，包含修改前后对比
     * @throws IOException 操作异常
     */
    public static String replaceTextInFile(String filePath, String searchText, String replaceText, boolean useRegex, String charset) throws IOException {
        filePath = FileUtils.normalizePath(filePath);
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("文件不存在: " + filePath);
        }

        // 解析字符集，默认为 UTF-8
        Charset cs = StandardCharsets.UTF_8;
        if (charset != null && !charset.trim().isEmpty()) {
            try {
                cs = Charset.forName(charset.trim());
            } catch (Exception e) {
                throw new IOException("不支持的字符集: " + charset + "，请使用 UTF-8、GBK、ISO-8859-1 等标准字符集");
            }
        }

        // ========== 读取验证 ==========
        // 检查文件是否被修改过
        if (isFileModified(filePath)) {
            return "【拒绝修改】文件已被修改过，读取记录已重置。请先重新读取文件内容，确认最新状态后再修改。";
        }

        // 读取文件所有行用于验证
        List<String> lines = Files.readAllLines(path, cs);
        int totalLines = lines.size();

        // 检查是否已读取文件全部内容
        List<Integer> unreadLines = checkUnreadLines(filePath, 1, totalLines);
        if (!unreadLines.isEmpty()) {
            StringBuilder errorMsg = new StringBuilder();
            errorMsg.append("【拒绝修改】文本替换需要了解文件全部内容。您尚未读取以下行：\n");
            int showCount = Math.min(unreadLines.size(), 10);
            for (int i = 0; i < showCount; i++) {
                errorMsg.append("  - 第 ").append(unreadLines.get(i)).append(" 行\n");
            }
            if (unreadLines.size() > 10) {
                errorMsg.append("  ... 还有 ").append(unreadLines.size() - 10).append(" 行未读取\n");
            }
            errorMsg.append("\n建议：使用 dlqq_read_file 读取整个文件内容，或至少读取包含搜索文本的区域。");
            return errorMsg.toString();
        }

        String content = Files.readString(path, cs);
        String newContent;
        int replaceCount = 0;
        List<int[]> matchPositions = new ArrayList<>(); // 记录匹配位置 [start, end]

        if (useRegex) {
            try {
                // 预编译正则表达式，使用高效模式
                Pattern pattern = Pattern.compile(searchText);
                java.util.regex.Matcher matcher = pattern.matcher(content);

                // 快速检查是否有匹配
                if (!matcher.find()) {
                    return "【未替换】未找到匹配的正则表达式: " + searchText;
                }

                // 记录匹配位置
                matcher.reset();
                while (matcher.find()) {
                    replaceCount++;
                    matchPositions.add(new int[]{matcher.start(), matcher.end()});
                }

                // 执行替换
                matcher.reset();
                StringBuffer sb = new StringBuffer(content.length());
                while (matcher.find()) {
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(replaceText));
                }
                matcher.appendTail(sb);
                newContent = sb.toString();
            } catch (PatternSyntaxException e) {
                throw new IOException("正则表达式语法错误: " + e.getMessage());
            }
        } else {
            // 普通文本替换
            if (!content.contains(searchText)) {
                return "【未替换】未找到文本: " + searchText;
            }

            // 记录匹配位置
            int index = 0;
            while ((index = content.indexOf(searchText, index)) != -1) {
                replaceCount++;
                matchPositions.add(new int[]{index, index + searchText.length()});
                index += searchText.length();
            }

            // 执行替换
            newContent = content.replace(searchText, replaceText);
        }

        // 如果有替换，写回文件
        if (replaceCount > 0) {
            Files.writeString(path, newContent, cs);
            // 标记文件已被修改
            markFileModified(filePath);
        }

        // 生成对比报告
        return generateTextReplaceReport(lines, content, newContent, searchText, replaceText, useRegex, replaceCount, matchPositions);
    }

    /**
     * 生成文本替换对比报告
     */
    private static String generateTextReplaceReport(List<String> lines, String oldContent, String newContent,
                                                     String searchText, String replaceText, boolean useRegex,
                                                     int replaceCount, List<int[]> matchPositions) {
        StringBuilder report = new StringBuilder();
        report.append("╔════════════════════════════════════════════════════════════════╗\n");
        report.append("║  文本替换完成 - 修改前后对比报告                                ║\n");
        report.append("╚════════════════════════════════════════════════════════════════╝\n\n");

        report.append("【搜索模式】").append(useRegex ? "正则表达式" : "普通文本").append("\n");
        report.append("【搜索内容】").append(searchText).append("\n");
        report.append("【替换为】  ").append(replaceText).append("\n");
        report.append("【替换次数】").append(replaceCount).append(" 处\n\n");

        // 显示匹配位置
        if (!matchPositions.isEmpty()) {
            report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            report.append("【匹配位置】\n");
            report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            for (int i = 0; i < matchPositions.size() && i < 10; i++) {
                int[] pos = matchPositions.get(i);
                // 计算行号
                int lineNum = 1;
                int charCount = 0;
                for (String line : lines) {
                    if (charCount + line.length() + 1 > pos[0]) {
                        break;
                    }
                    charCount += line.length() + 1; // +1 for newline
                    lineNum++;
                }
                // 提取上下文
                int contextStart = Math.max(0, pos[0] - 30);
                int contextEnd = Math.min(oldContent.length(), pos[1] + 30);
                String context = oldContent.substring(contextStart, contextEnd);
                context = context.replace("\n", "\\n");

                report.append(String.format("  第%d处: 第%d行, 字符位置 %d-%d\n", i + 1, lineNum, pos[0], pos[1]));
                report.append(String.format("  上下文: ...%s...\n", context));
                report.append("\n");
            }
            if (matchPositions.size() > 10) {
                report.append("  ... 还有 ").append(matchPositions.size() - 10).append(" 处匹配 ...\n");
            }
        }

        // 统计信息
        int oldLineCount = oldContent.split("\n").length;
        int newLineCount = newContent.split("\n").length;

        report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        report.append("【统计信息】\n");
        report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        report.append("  原文件行数：").append(oldLineCount).append(" 行\n");
        report.append("  新文件行数：").append(newLineCount).append(" 行\n");
        report.append("  行数变化：  ").append(newLineCount - oldLineCount).append(" 行\n");
        report.append("\n");
        report.append("【状态】✓ 替换成功 (共 ").append(replaceCount).append(" 处)\n");

        // 结构检查提示
        report.append("\n");
        report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        report.append("【结构检查】\n");
        report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        report.append("  文本替换可能影响文件中的多处位置。\n");
        report.append("  请检查替换后的内容是否正确，特别是关键代码区域。\n");

        return report.toString();
    }

    // ==================== 目录操作工具 ====================

    /**
     * 创建文件夹（只创建最后一级）
     *
     * @param dirPath 文件夹路径
     * @throws IOException 创建异常
     */
    public static void createDirectory(String dirPath) throws IOException {
        dirPath = FileUtils.normalizePath(dirPath);
        Path path = Paths.get(dirPath);
        if (Files.exists(path)) {
            if (!Files.isDirectory(path)) {
                throw new IOException("路径已存在但不是目录: " + dirPath);
            }
            return; // 目录已存在
        }
        Files.createDirectory(path);
    }

    /**
     * 创建文件夹（包括所有父文件夹）
     *
     * @param dirPath 文件夹路径
     * @throws IOException 创建异常
     */
    public static void createDirectories(String dirPath) throws IOException {
        dirPath = FileUtils.normalizePath(dirPath);
        Path path = Paths.get(dirPath);
        if (Files.exists(path)) {
            if (!Files.isDirectory(path)) {
                throw new IOException("路径已存在但不是目录: " + dirPath);
            }
            return; // 目录已存在
        }
        Files.createDirectories(path);
    }

    /**
     * 删除文件夹（带确认对话框，递归删除）
     *
     * @param dirPath 文件夹路径
     * @return 是否成功删除
     * @throws IOException 删除异常
     */
    public static boolean deleteDirectory(String dirPath) throws IOException {
        dirPath = FileUtils.normalizePath(dirPath);
        Path path = Paths.get(dirPath);
        if (!Files.exists(path)) {
            throw new IOException("目录不存在: " + dirPath);
        }
        if (!Files.isDirectory(path)) {
            throw new IOException("路径不是目录: " + dirPath);
        }

        // 计算目录中的文件和子目录数量
        int fileCount = 0;
        int dirCount = 0;
        try (java.util.stream.Stream<Path> stream = Files.walk(path)) {
            for (Path p : stream.toList()) {
                if (Files.isRegularFile(p)) {
                    fileCount++;
                } else if (Files.isDirectory(p) && !p.equals(path)) {
                    dirCount++;
                }
            }
        }

        // 弹出确认对话框
        String dirName = path.getFileName().toString();
        StringBuilder message = new StringBuilder();
        message.append("确定要删除目录 \"").append(dirName).append("\" 吗？\n");
        message.append("路径: ").append(dirPath).append("\n");
        if (fileCount > 0 || dirCount > 0) {
            message.append("包含: ").append(fileCount).append(" 个文件, ").append(dirCount).append(" 个子目录\n");
        }
        message.append("\n此操作不可恢复！");

        boolean confirmed = DialogTools.showConfirmDialog(
                "确认删除目录",
                message.toString()
        );

        if (!confirmed) {
            return false;
        }

        // 递归删除目录
        deleteDirectoryRecursive(path);
        return true;
    }

    /**
     * 递归删除目录
     */
    private static void deleteDirectoryRecursive(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (java.util.stream.Stream<Path> stream = Files.list(path)) {
                for (Path child : stream.toList()) {
                    deleteDirectoryRecursive(child);
                }
            }
        }
        Files.delete(path);
    }

    /**
     * 复制文件夹（递归复制）
     *
     * @param sourcePath 源文件夹路径
     * @param targetPath 目标文件夹路径
     * @throws IOException 复制异常
     */
    public static void copyDirectory(String sourcePath, String targetPath) throws IOException {
        sourcePath = FileUtils.normalizePath(sourcePath);
        targetPath = FileUtils.normalizePath(targetPath);
        Path source = Paths.get(sourcePath);
        if (!Files.exists(source)) {
            throw new IOException("源目录不存在: " + sourcePath);
        }
        if (!Files.isDirectory(source)) {
            throw new IOException("源路径不是目录: " + sourcePath);
        }

        Path target = Paths.get(targetPath);

        // 遍历源目录并复制
        Files.walk(source).forEach(sourceFile -> {
            try {
                Path relativePath = source.relativize(sourceFile);
                Path targetFile = target.resolve(relativePath);

                if (Files.isDirectory(sourceFile)) {
                    if (!Files.exists(targetFile)) {
                        Files.createDirectories(targetFile);
                    }
                } else {
                    // 确保父目录存在
                    Path parent = targetFile.getParent();
                    if (parent != null && !Files.exists(parent)) {
                        Files.createDirectories(parent);
                    }
                    Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new RuntimeException("复制失败: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 移动文件夹
     *
     * @param sourcePath 源文件夹路径
     * @param targetPath 目标文件夹路径
     * @throws IOException 移动异常
     */
    public static void moveDirectory(String sourcePath, String targetPath) throws IOException {
        sourcePath = FileUtils.normalizePath(sourcePath);
        targetPath = FileUtils.normalizePath(targetPath);
        Path source = Paths.get(sourcePath);
        if (!Files.exists(source)) {
            throw new IOException("源目录不存在: " + sourcePath);
        }
        if (!Files.isDirectory(source)) {
            throw new IOException("源路径不是目录: " + sourcePath);
        }

        Path target = Paths.get(targetPath);

        // 如果目标已存在，先删除
        if (Files.exists(target)) {
            deleteDirectoryRecursive(target);
        }

        // 确保目标父目录存在
        Path parent = target.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        // 移动目录
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    // ==================== 二进制文件操作工具 ====================

    /**
     * 读取二进制文件并转换为十六进制文本（支持字节范围）
     *
     * @param filePath   文件路径
     * @param startByte  起始字节（0-based，包含）
     * @param endByte    结束字节（0-based，包含），-1表示到文件末尾
     * @param readBytes  读取字节数（与endByte互斥，endByte优先）
     * @return 十六进制表示的文本
     * @throws IOException 读取异常
     */
    public static String readBinaryFileAsHex(String filePath, long startByte, long endByte, long readBytes) throws IOException {
        filePath = FileUtils.normalizePath(filePath);
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("文件不存在: " + filePath);
        }
        if (!Files.isRegularFile(path)) {
            throw new IOException("路径不是文件: " + filePath);
        }

        long fileSize = Files.size(path);

        // 验证起始字节
        if (startByte < 0) {
            startByte = 0;
        }
        if (startByte >= fileSize) {
            throw new IOException("起始字节超出文件范围，文件共 " + fileSize + " 字节");
        }

        // 计算结束字节
        if (endByte < 0 && readBytes > 0) {
            // 使用readBytes计算endByte
            endByte = startByte + readBytes - 1;
        }

        // 默认到文件末尾
        if (endByte < 0 || endByte >= fileSize) {
            endByte = fileSize - 1;
        }

        if (endByte < startByte) {
            throw new IOException("结束字节必须大于等于起始字节");
        }

        // 读取指定范围的字节
        long bytesToRead = endByte - startByte + 1;
        byte[] buffer = new byte[(int) bytesToRead];

        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(filePath, "r")) {
            raf.seek(startByte);
            raf.readFully(buffer);
        }

        // 转换为十六进制
        StringBuilder hex = new StringBuilder(buffer.length * 2);
        for (byte b : buffer) {
            hex.append(String.format("%02X", b & 0xFF));
        }
        return hex.toString();
    }

    /**
     * 将十六进制文本写入为二进制文件
     *
     * @param filePath 文件路径
     * @param hexText  十六进制文本
     * @param append   是否追加写入
     * @throws IOException 写入异常
     */
    public static void writeBinaryFileFromHex(String filePath, String hexText, boolean append) throws IOException {
        filePath = FileUtils.normalizePath(filePath);
        Path path = Paths.get(filePath);

        // 确保父目录存在
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        // 清理十六进制文本（去除空格、换行等）
        String cleanHex = hexText.replaceAll("\\s", "");

        // 验证十六进制格式
        if (!cleanHex.matches("^[0-9A-Fa-f]*$")) {
            throw new IOException("无效的十六进制文本，包含非十六进制字符");
        }
        if (cleanHex.length() % 2 != 0) {
            throw new IOException("无效的十六进制文本，字符数必须是偶数");
        }

        // 转换为字节数组
        int len = cleanHex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(cleanHex.charAt(i), 16) << 4)
                                 + Character.digit(cleanHex.charAt(i + 1), 16));
        }

        // 根据append模式选择写入方式
        if (append && Files.exists(path)) {
            // 追加写入
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(filePath, true)) {
                fos.write(data);
            }
        } else {
            // 覆盖写入
            Files.write(path, data);
        }
    }

    // ==================== 搜索工具 ====================

    /**
     * 在指定目录下查找文件
     *
     * @param searchPath 搜索目录路径
     * @param pattern    搜索模式
     * @param useRegex   是否使用正则表达式
     * @return 匹配的文件路径列表
     * @throws IOException 搜索异常
     */
    public static List<String> searchFiles(String searchPath, String pattern, boolean useRegex) throws IOException {
        searchPath = FileUtils.normalizePath(searchPath);
        Path path = Paths.get(searchPath);
        if (!Files.exists(path)) {
            throw new IOException("目录不存在: " + searchPath);
        }
        if (!Files.isDirectory(path)) {
            throw new IOException("路径不是目录: " + searchPath);
        }

        List<String> results = new ArrayList<>();

        if (useRegex) {
            try {
                Pattern regex = Pattern.compile(pattern);
                Files.walk(path)
                    .filter(Files::isRegularFile)
                    .filter(p -> regex.matcher(p.getFileName().toString()).find())
                    .forEach(p -> results.add(p.toString()));
            } catch (PatternSyntaxException e) {
                throw new IOException("正则表达式语法错误: " + e.getMessage());
            }
        } else {
            // 部分匹配
            String lowerPattern = pattern.toLowerCase();
            Files.walk(path)
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().toLowerCase().contains(lowerPattern))
                .forEach(p -> results.add(p.toString()));
        }

        return results;
    }

    /**
     * 在指定目录下查找文件夹
     *
     * @param searchPath 搜索目录路径
     * @param pattern    搜索模式
     * @param useRegex   是否使用正则表达式
     * @return 匹配的文件夹路径列表
     * @throws IOException 搜索异常
     */
    public static List<String> searchDirectories(String searchPath, String pattern, boolean useRegex) throws IOException {
        searchPath = FileUtils.normalizePath(searchPath);
        Path path = Paths.get(searchPath);
        if (!Files.exists(path)) {
            throw new IOException("目录不存在: " + searchPath);
        }
        if (!Files.isDirectory(path)) {
            throw new IOException("路径不是目录: " + searchPath);
        }

        List<String> results = new ArrayList<>();

        if (useRegex) {
            try {
                Pattern regex = Pattern.compile(pattern);
                Files.walk(path)
                    .filter(Files::isDirectory)
                    .filter(p -> !p.equals(path)) // 排除搜索目录本身
                    .filter(p -> regex.matcher(p.getFileName().toString()).find())
                    .forEach(p -> results.add(p.toString()));
            } catch (PatternSyntaxException e) {
                throw new IOException("正则表达式语法错误: " + e.getMessage());
            }
        } else {
            // 部分匹配
            String lowerPattern = pattern.toLowerCase();
            Files.walk(path)
                .filter(Files::isDirectory)
                .filter(p -> !p.equals(path)) // 排除搜索目录本身
                .filter(p -> p.getFileName().toString().toLowerCase().contains(lowerPattern))
                .forEach(p -> results.add(p.toString()));
        }

        return results;
    }

    // ==================== 目录列表工具 ====================

    /**
     * 列出目录树（递归列出所有内容）
     *
     * @param dirPath      目录路径
     * @param maxDepth     最大递归深度（-1表示无限制）
     * @param maxEntries   最大返回条目数（-1表示无限制）
     * @param showHidden   是否显示隐藏文件/目录
     * @return 格式化的目录树字符串
     * @throws IOException 操作异常
     */
    public static String listDirectoryTree(String dirPath, int maxDepth, int maxEntries, boolean showHidden) throws IOException {
        dirPath = FileUtils.normalizePath(dirPath);
        Path path = Paths.get(dirPath);
        if (!Files.exists(path)) {
            throw new IOException("目录不存在: " + dirPath);
        }
        if (!Files.isDirectory(path)) {
            throw new IOException("路径不是目录: " + dirPath);
        }

        StringBuilder result = new StringBuilder();
        result.append("目录结构: ").append(dirPath).append("\n");
        result.append("=" .repeat(Math.min(60, dirPath.length() + 10))).append("\n\n");

        int[] stats = new int[3]; // [files, dirs, total]
        listDirectoryTreeRecursive(path, "", 0, maxDepth, maxEntries, showHidden, result, stats);

        result.append("\n");
        result.append("-".repeat(40)).append("\n");
        result.append("统计: ").append(stats[1]).append(" 个目录, ").append(stats[0]).append(" 个文件");
        if (maxEntries > 0 && stats[2] >= maxEntries) {
            result.append(" (已限制，最多显示 ").append(maxEntries).append(" 个条目)");
        }
        result.append("\n");

        return result.toString();
    }

    private static boolean listDirectoryTreeRecursive(Path path, String prefix, int depth, int maxDepth,
                                                      int maxEntries, boolean showHidden, StringBuilder result, int[] stats) {
        // 检查深度限制
        if (maxDepth >= 0 && depth > maxDepth) {
            return true;
        }

        // 检查条目限制
        if (maxEntries > 0 && stats[2] >= maxEntries) {
            return false;
        }

        try (java.util.stream.Stream<Path> stream = Files.list(path)) {
            List<Path> entries = stream
                .filter(p -> showHidden || !isHidden(p))
                .sorted((a, b) -> {
                    boolean aIsDir = Files.isDirectory(a);
                    boolean bIsDir = Files.isDirectory(b);
                    if (aIsDir != bIsDir) {
                        return aIsDir ? -1 : 1; // 目录在前
                    }
                    return a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString());
                })
                .toList();

            for (int i = 0; i < entries.size(); i++) {
                Path entry = entries.get(i);
                boolean isLast = (i == entries.size() - 1);

                // 检查条目限制
                if (maxEntries > 0 && stats[2] >= maxEntries) {
                    return false;
                }

                String entryPrefix = prefix + (isLast ? "└── " : "├── ");
                String name = entry.getFileName().toString();

                if (Files.isDirectory(entry)) {
                    result.append(entryPrefix).append("[").append(name).append("]/\n");
                    stats[1]++;
                    stats[2]++;

                    // 递归处理子目录
                    String childPrefix = prefix + (isLast ? "    " : "│   ");
                    boolean continued = listDirectoryTreeRecursive(entry, childPrefix, depth + 1, maxDepth,
                                                                     maxEntries, showHidden, result, stats);
                    if (!continued) {
                        return false;
                    }
                } else {
                    String sizeStr = formatFileSize(Files.size(entry));
                    result.append(entryPrefix).append(name);
                    if (sizeStr != null) {
                        result.append(" (").append(sizeStr).append(")");
                    }
                    result.append("\n");
                    stats[0]++;
                    stats[2]++;
                }
            }
        } catch (IOException e) {
            result.append(prefix).append("[读取失败: ").append(e.getMessage()).append("]\n");
        }

        return true;
    }

    /**
     * 列出单层目录内容
     *
     * @param dirPath    目录路径
     * @param showHidden 是否显示隐藏文件/目录
     * @return 格式化的目录内容字符串
     * @throws IOException 操作异常
     */
    public static String listDirectoryFiles(String dirPath, boolean showHidden) throws IOException {
        dirPath = FileUtils.normalizePath(dirPath);
        Path path = Paths.get(dirPath);
        if (!Files.exists(path)) {
            throw new IOException("目录不存在: " + dirPath);
        }
        if (!Files.isDirectory(path)) {
            throw new IOException("路径不是目录: " + dirPath);
        }

        StringBuilder result = new StringBuilder();
        result.append("目录: ").append(dirPath).append("\n");
        result.append("=".repeat(Math.min(60, dirPath.length() + 8))).append("\n\n");

        try (java.util.stream.Stream<Path> stream = Files.list(path)) {
            List<Path> entries = stream
                .filter(p -> showHidden || !isHidden(p))
                .sorted((a, b) -> {
                    boolean aIsDir = Files.isDirectory(a);
                    boolean bIsDir = Files.isDirectory(b);
                    if (aIsDir != bIsDir) {
                        return aIsDir ? -1 : 1; // 目录在前
                    }
                    return a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString());
                })
                .toList();

            if (entries.isEmpty()) {
                result.append("(空目录)\n");
            } else {
                int fileCount = 0;
                int dirCount = 0;

                for (Path entry : entries) {
                    String name = entry.getFileName().toString();
                    if (Files.isDirectory(entry)) {
                        result.append("[DIR]  ").append(name).append("/\n");
                        dirCount++;
                    } else {
                        String sizeStr = formatFileSize(Files.size(entry));
                        result.append("[FILE] ").append(name);
                        if (sizeStr != null) {
                            result.append(" (").append(sizeStr).append(")");
                        }
                        result.append("\n");
                        fileCount++;
                    }
                }

                result.append("\n");
                result.append("-".repeat(40)).append("\n");
                result.append("总计: ").append(dirCount).append(" 个目录, ").append(fileCount).append(" 个文件\n");
            }
        }

        return result.toString();
    }

    /**
     * 检查文件/目录是否为隐藏文件
     */
    private static boolean isHidden(Path path) {
        try {
            return Files.isHidden(path);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 格式化文件大小
     */
    private static String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
        }
    }
}

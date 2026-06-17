package com.donglouqiaoque.aitools.mcp.tools;

import com.donglouqiaoque.aitools.utils.FileUtils;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 文件操作工具 - 提供文件读写、修改、复制移动等功能
 */
public class FileTools {

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
            result.append(lines.get(i));
            if (i < endLine - 1) {
                result.append(System.lineSeparator());
            }
        }

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
     * @return 是否成功删除
     * @throws IOException 操作异常
     */
    public static boolean removeLinesInFile(String filePath, int startLine, int endLine, String charset) throws IOException {
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
            return false;
        }

        // 验证行号
        if (startLine < 1) {
            startLine = 1;
        }
        if (endLine > lines.size()) {
            endLine = lines.size();
        }
        if (startLine > endLine) {
            throw new IOException("起始行不能大于结束行");
        }

        // 删除指定行
        List<String> newLines = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            int lineNum = i + 1;
            if (lineNum < startLine || lineNum > endLine) {
                newLines.add(lines.get(i));
            }
        }

        // 写回文件
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < newLines.size(); i++) {
            result.append(newLines.get(i));
            if (i < newLines.size() - 1) {
                result.append(System.lineSeparator());
            }
        }

        Files.writeString(path, result.toString(), cs);
        return true;
    }

    /**
     * 替换指定行范围的内容
     *
     * @param filePath    文件路径
     * @param startLine   起始行（1-based，包含）
     * @param endLine     结束行（1-based，包含）
     * @param newContent  新的内容（支持多行）
     * @param charset     字符集（如 UTF-8、GBK、ISO-8859-1 等，默认为 UTF-8）
     * @return 是否成功替换
     * @throws IOException 操作异常
     */
    public static boolean replaceLinesInFile(String filePath, int startLine, int endLine, String newContent, String charset) throws IOException {
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
            return false;
        }

        // 验证行号
        if (startLine < 1) {
            startLine = 1;
        }
        if (endLine > lines.size()) {
            endLine = lines.size();
        }
        if (startLine > endLine) {
            throw new IOException("起始行不能大于结束行");
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

        // 写回文件
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < resultLines.size(); i++) {
            result.append(resultLines.get(i));
            if (i < resultLines.size() - 1) {
                result.append(System.lineSeparator());
            }
        }

        Files.writeString(path, result.toString(), cs);
        return true;
    }

    /**
     * 在指定行插入内容
     *
     * @param filePath 文件路径
     * @param lineNum  插入位置（1-based，插入到该行之前）
     * @param content  要插入的内容
     * @param charset  字符集（如 UTF-8、GBK、ISO-8859-1 等，默认为 UTF-8）
     * @return 是否成功插入
     * @throws IOException 操作异常
     */
    public static boolean insertTextInFile(String filePath, int lineNum, String content, String charset) throws IOException {
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

        // 验证行号
        if (lineNum < 1) {
            lineNum = 1;
        }
        if (lineNum > lines.size() + 1) {
            lineNum = lines.size() + 1;
        }

        // 准备插入内容（支持多行）
        String[] insertLines = content.split("\\r?\\n");

        // 在指定位置插入
        int insertIndex = lineNum - 1;
        for (int i = insertLines.length - 1; i >= 0; i--) {
            lines.add(insertIndex, insertLines[i]);
        }

        // 写回文件
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            result.append(lines.get(i));
            if (i < lines.size() - 1) {
                result.append(System.lineSeparator());
            }
        }

        Files.writeString(path, result.toString(), cs);
        return true;
    }

    /**
     * 替换文件中的文本
     *
     * @param filePath    文件路径
     * @param searchText  搜索文本
     * @param replaceText 替换文本
     * @param useRegex    是否使用正则表达式
     * @param charset     字符集（如 UTF-8、GBK、ISO-8859-1 等，默认为 UTF-8）
     * @return 替换次数
     * @throws IOException 操作异常
     */
    public static int replaceTextInFile(String filePath, String searchText, String replaceText, boolean useRegex, String charset) throws IOException {
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

        String content = Files.readString(path, cs);
        String newContent;
        int replaceCount = 0;

        if (useRegex) {
            try {
                // 预编译正则表达式，使用高效模式
                Pattern pattern = Pattern.compile(searchText);
                java.util.regex.Matcher matcher = pattern.matcher(content);

                // 快速检查是否有匹配
                if (!matcher.find()) {
                    return 0;
                }

                // 重置matcher并执行替换
                matcher.reset();
                StringBuffer sb = new StringBuffer(content.length());
                while (matcher.find()) {
                    replaceCount++;
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(replaceText));
                }
                matcher.appendTail(sb);
                newContent = sb.toString();
            } catch (PatternSyntaxException e) {
                throw new IOException("正则表达式语法错误: " + e.getMessage());
            }
        } else {
            // 普通文本替换 - 使用String.replace()，内部优化更高效
            if (!content.contains(searchText)) {
                return 0;
            }

            // 对于多次替换，使用String.replace()比手动循环更高效
            // Java内部使用KMP或类似算法优化字符串查找
            newContent = content.replace(searchText, replaceText);

            // 计算替换次数
            int index = 0;
            while ((index = content.indexOf(searchText, index)) != -1) {
                replaceCount++;
                index += searchText.length();
            }
        }

        // 如果有替换，写回文件
        if (replaceCount > 0) {
            Files.writeString(path, newContent, cs);
        }

        return replaceCount;
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
}

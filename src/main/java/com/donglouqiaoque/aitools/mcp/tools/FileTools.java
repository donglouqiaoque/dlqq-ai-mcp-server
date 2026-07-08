package com.donglouqiaoque.aitools.mcp.tools;

import com.donglouqiaoque.aitools.utils.AIRulesUtils;
import com.donglouqiaoque.aitools.utils.FileUtils;

import javax.swing.*;
import java.awt.Component;
import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

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

    // 目录搜索记录：记录目录搜索时使用的文件扩展名
    // key: "目录路径|搜索模式|是否正则", value: 搜索时使用的文件扩展名列表
    private static final Map<String, List<String>> dirSearchTracker = new ConcurrentHashMap<>();

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
     * 获取当前工作目录（项目目录）
     */
    private static String getWorkingDirectory() {
        return FileUtils.normalizePath(System.getProperty("user.dir"));
    }

    /**
     * 检查文件是否在项目目录内
     * @param filePath 文件路径
     * @return true 表示在项目目录内，false 表示在项目目录外
     */
    private static boolean isInsideProject(String filePath) {
        String normalizedFilePath = FileUtils.normalizePath(filePath);
        String workingDir = getWorkingDirectory();
        // 添加路径分隔符确保准确匹配
        String fileWithSep = normalizedFilePath + File.separator;
        String dirWithSep = workingDir + File.separator;
        return fileWithSep.startsWith(dirWithSep) || normalizedFilePath.equals(workingDir);
    }

    /**
     * 显示跨目录写入确认对话框
     * @param filePath 要操作的文件路径
     * @param operation 操作描述（如"写入"、"修改"、"删除"等）
     * @return true 表示用户确认，false 表示用户取消
     */
    private static boolean showOutsideProjectConfirmDialog(String filePath, String operation) {
        String workingDir = getWorkingDirectory();
        String normalizedPath = FileUtils.normalizePath(filePath);

        // 创建一个 CountDownLatch 来同步 GUI 线程和调用线程
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};

        // 在 Swing 事件调度线程中显示对话框
        SwingUtilities.invokeLater(() -> {
            try {
                // 使用自定义样式的确认对话框
                String title = "跨目录操作确认 - " + operation;
                String message = "检测到您即将在【项目目录外】执行 " + operation + " 操作！\n\n"
                    + "操作目标:\n" + normalizedPath + "\n\n"
                    + "当前项目目录:\n" + workingDir + "\n\n"
                    + "注意：此操作可能会修改系统文件或其他项目文件。\n"
                    + "请确认您是否真的要继续执行此操作?";

                // 创建自定义的选项数组
                String[] options = {"确认执行", "取消操作"};

                // 显示确认对话框
                int choice = JOptionPane.showOptionDialog(
                    null,                                   // 父组件
                    message,                                // 消息内容
                    title,                                  // 标题
                    JOptionPane.YES_NO_OPTION,             // 选项类型
                    JOptionPane.WARNING_MESSAGE,           // 消息类型
                    null,                                   // 图标
                    options,                                // 自定义选项
                    options[1]                             // 默认选中"取消"
                );

                result[0] = (choice == JOptionPane.YES_OPTION);
            } catch (Exception e) {
                // 对话框显示失败，安全起见返回 false
                System.err.println("显示确认对话框失败: " + e.getMessage());
                result[0] = false;
            } finally {
                latch.countDown();
            }
        });

        // 等待用户选择，最多等待 5 分钟
        try {
            boolean completed = latch.await(5, TimeUnit.MINUTES);
            if (!completed) {
                System.err.println("等待用户确认超时，取消操作");
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        return result[0];
    }

    /**
     * 检查并确认跨目录写入操作
     * @param filePath 文件路径
     * @param operation 操作描述
     * @return 操作结果字符串，null 表示允许执行
     */
    private static String checkCrossDirectoryWrite(String filePath, String operation) {
        if (!isInsideProject(filePath)) {
            boolean confirmed = showOutsideProjectConfirmDialog(filePath, operation);
            if (!confirmed) {
                return "【操作已取消】用户拒绝了在【项目目录外】执行 " + operation + " 操作\n"
                    + "目标路径: " + filePath + "\n"
                    + "项目路径: " + getWorkingDirectory();
            }
        }
        return null; // 允许执行
    }

    /**
     * 检查目录操作（会影响目录外文件）
     * 对于移动、复制、删除目录等操作，如果源或目标在项目外，需要确认
     * @param sourcePath 源路径（可为null）
     * @param targetPath 目标路径
     * @param operation 操作描述
     * @return 操作结果字符串，null 表示允许执行
     */
    private static String checkCrossDirectoryOperation(String sourcePath, String targetPath, String operation) {
        boolean sourceOutside = sourcePath != null && !isInsideProject(sourcePath);
        boolean targetOutside = !isInsideProject(targetPath);

        if (sourceOutside || targetOutside) {
            StringBuilder message = new StringBuilder("检测到您即将在【项目目录外】执行 " + operation + " 操作！\n\n");
            if (sourceOutside) {
                message.append("源路径 (项目外):\n").append(FileUtils.normalizePath(sourcePath)).append("\n\n");
            } else if (sourcePath != null) {
                message.append("源路径:\n").append(FileUtils.normalizePath(sourcePath)).append("\n\n");
            }
            if (targetOutside) {
                message.append("目标路径 (项目外):\n").append(FileUtils.normalizePath(targetPath)).append("\n\n");
            } else {
                message.append("目标路径:\n").append(FileUtils.normalizePath(targetPath)).append("\n\n");
            }
            message.append("当前项目目录:\n").append(getWorkingDirectory()).append("\n\n");
            message.append("注意：此操作可能会影响系统文件或其他项目文件。\n");
            message.append("请确认您是否真的要继续执行此操作?");

            CountDownLatch latch = new CountDownLatch(1);
            final boolean[] result = {false};

            SwingUtilities.invokeLater(() -> {
                try {
                    String[] options = {"确认执行", "取消操作"};
                    int choice = JOptionPane.showOptionDialog(
                        null,
                        message.toString(),
                        "跨目录操作确认 - " + operation,
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        options,
                        options[1]
                    );
                    result[0] = (choice == JOptionPane.YES_OPTION);
                } catch (Exception e) {
                    System.err.println("显示确认对话框失败: " + e.getMessage());
                    result[0] = false;
                } finally {
                    latch.countDown();
                }
            });

            try {
                boolean completed = latch.await(5, TimeUnit.MINUTES);
                if (!completed) {
                    return "【操作已取消】等待用户确认超时";
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "【操作已取消】等待被中断";
            }

            if (!result[0]) {
                return "【操作已取消】用户拒绝了在【项目目录外】执行 " + operation + " 操作";
            }
        }
        return null; // 允许执行
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

        // 检查文件是否被读保护（只在项目内检查）
        boolean insideProject = isInsideProject(filePath);
        if (AIRulesUtils.isReadProtected(filePath, insideProject)) {
            throw new IOException("文件被读保护规则禁止读取（.aireadrules）: " + filePath);
        }

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

    // ========== ANSI 颜色代码 ==========
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";    // 删除 - 红色
    private static final String ANSI_GREEN = "\u001B[32m";  // 新增 + 绿色
    private static final String ANSI_YELLOW = "\u001B[33m"; // 警告/标题 - 黄色
    private static final String ANSI_BLUE = "\u001B[34m";   // 信息 - 蓝色
    private static final String ANSI_CYAN = "\u001B[36m";   // 行号 - 青色
    private static final String ANSI_GRAY = "\u001B[90m";   // 上下文 - 灰色

    // ========== 返回内容大小限制 ==========
    // 目标最大返回大小从settings.properties读取，默认8KB
    private static int getTargetMaxResponseSize() {
        return McpTools.getResponseMaxSize();
    }

    /**
     * 智能截断内容 - 根据目标大小动态计算显示行数
     *
     * @param reportBuilder 报告构建器
     * @param headerInfo 头部信息（用于计算已用空间）
     * @param footerInfo 尾部信息（用于计算已用空间）
     * @param lines 内容行数组
     * @param startLineNum 起始行号
     * @param prefix 行前缀（如 "- " 表示删除，"+ " 表示新增，"  "表示普通）
     * @return 截断后的报告
     */
    private static StringBuilder appendContentWithSmartTruncation(
            StringBuilder reportBuilder,
            String headerInfo,
            String footerInfo,
            String[] lines,
            int startLineNum,
            String prefix) {

        int targetMaxSize = getTargetMaxResponseSize();

        // 计算头部和尾部已占用的空间
        int headerLen = headerInfo != null ? headerInfo.length() : 0;
        int footerLen = footerInfo != null ? footerInfo.length() : 0;
        int availableSize = targetMaxSize - headerLen - footerLen - 100; // 100为缓冲

        if (availableSize < 500) {
            availableSize = 500; // 至少保留500字符
        }

        // 如果内容很少，直接显示全部
        StringBuilder fullContent = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            fullContent.append(String.format("%s%4d | %s\n", prefix, startLineNum + i, lines[i]));
        }

        if (headerLen + fullContent.length() + footerLen <= targetMaxSize) {
            // 内容在限制范围内
            reportBuilder.append(fullContent);
        } else {
            // 需要截断 - 显示开头和结尾
            int halfAvailable = availableSize / 2;
            StringBuilder headBuilder = new StringBuilder();
            int headLines = 0;
            
            // 填充开头部分
            for (int i = 0; i < lines.length && headBuilder.length() < halfAvailable; i++) {
                headBuilder.append(String.format("%s%4d | %s\n", prefix, startLineNum + i, lines[i]));
                headLines++;
            }
            
            // 填充结尾部分（至少保留10行或剩余行）
            int tailLines = Math.min(10, lines.length - headLines);
            StringBuilder tailBuilder = new StringBuilder();
            for (int i = lines.length - tailLines; i < lines.length; i++) {
                tailBuilder.append(String.format("%s%4d | %s\n", prefix, startLineNum + i, lines[i]));
            }
            
            // 如果开头+结尾超过可用空间，调整开头部分
            if (headBuilder.length() + tailBuilder.length() > availableSize) {
                int excess = headBuilder.length() + tailBuilder.length() - availableSize;
                String headStr = headBuilder.toString();
                // 估算每行平均长度，截去开头的多余行
                int avgLineLen = headLines > 0 ? headStr.length() / headLines : 50;
                int linesToRemove = excess / avgLineLen + 1;
                String[] headLineArray = headStr.split("\n");
                headBuilder = new StringBuilder();
                for (int i = linesToRemove; i < headLineArray.length && i < headLineArray.length - tailLines + 3; i++) {
                    if (i > linesToRemove) headBuilder.append("\n");
                    headBuilder.append(headLineArray[i]);
                }
                headLines = Math.max(0, headLines - linesToRemove);
            }
            
            reportBuilder.append(headBuilder);
            int skippedLines = lines.length - headLines - tailLines;
            if (skippedLines > 0) {
                int maxSize = getTargetMaxResponseSize();
                reportBuilder.append(String.format("%s ... (%d 行已省略，共 %d 行) ...\n",
                    prefix.isEmpty() ? " " : prefix, skippedLines, lines.length));
                reportBuilder.append(String.format("\n【提示】当前返回内容大小限制为 %d 字节，内容已截断。\n", maxSize));
                reportBuilder.append("如需查看完整内容，可使用 dlqq_set_mcp_properties 工具设置 response.max.size 增大限制（最大258048字节）。\n");
            }
            reportBuilder.append(tailBuilder);
        }

        return reportBuilder;
    }

    /**
     * 创建文件（文件已存在时拒绝）
     *
     * @param filePath 文件路径
     * @param content  文件内容（null或空字符串则创建空文件）
     * @param charset  字符集（如 UTF-8、GBK、ISO-8859-1 等，默认为 UTF-8）
     * @return 创建结果报告
     * @throws IOException 创建异常
     */
    public static String createFile(String filePath, String content, String charset) throws IOException {
        filePath = FileUtils.normalizePath(filePath);

        // 检查文件是否被写保护（只在项目内检查）
        boolean insideProject = isInsideProject(filePath);
        if (AIRulesUtils.isWriteProtected(filePath, insideProject)) {
            throw new IOException("文件被写保护规则禁止修改（.aiwriterules）: " + filePath);
        }

        // 跨目录写入保护检查
        String checkResult = checkCrossDirectoryWrite(filePath, "创建文件");
        if (checkResult != null) {
            return checkResult;
        }

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

        // 检查文件是否已存在
        if (Files.exists(path)) {
            throw new IOException("【拒绝创建】文件已存在: " + filePath + "\n如需覆盖文件内容，请使用 dlqq_overwrite_file；如需清空文件内容，请使用 dlqq_clear_file。");
        }

        // 创建文件
        Files.createFile(path);

        // 写入内容（支持空内容创建空文件）
        int lineCount = 0;
        if (content != null && !content.isEmpty()) {
            Files.writeString(path, content, cs);
            lineCount = content.split("\r?\n", -1).length;
        }

        // 生成创建报告（带+/-差异）
        StringBuilder report = new StringBuilder();

        StringBuilder headerInfo = new StringBuilder();
        headerInfo.append("创建文件：").append(filePath).append("\n");
        headerInfo.append("新增 ").append(lineCount).append(" 行\n");
        headerInfo.append("原文件行数：0 行 → 新文件行数：").append(lineCount).append(" 行 (+").append(lineCount).append(" 行)\n\n");

        String footerInfo = "\n【状态】✓ 创建成功\n";

        report.append(headerInfo);

        // 显示新增内容（+）
        if (content != null && !content.isEmpty()) {
            String[] lines = content.split("\r?\n", -1);
            for (int i = 0; i < lines.length; i++) {
                report.append(String.format("+ %4d | %s\n", i + 1, lines[i]));
            }
        }

        report.append(footerInfo);
        return report.toString();
    }

    /**
     * 覆盖文件内容（不管文件是否存在都直接写入）
     *
     * @param filePath 文件路径
     * @param content  文件内容
     * @param charset  字符集（如 UTF-8、GBK、ISO-8859-1 等，默认为 UTF-8）
     * @return 覆盖结果报告
     * @throws IOException 写入异常
     */
    public static String overwriteFile(String filePath, String content, String charset) throws IOException {
        filePath = FileUtils.normalizePath(filePath);

        // 检查文件是否被写保护（只在项目内检查）
        boolean insideProject = isInsideProject(filePath);
        if (AIRulesUtils.isWriteProtected(filePath, insideProject)) {
            throw new IOException("文件被写保护规则禁止修改（.aiwriterules）: " + filePath);
        }

        // 跨目录写入保护检查
        String checkResult = checkCrossDirectoryWrite(filePath, "覆盖文件");
        if (checkResult != null) {
            return checkResult;
        }

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

        // 检查内容是否为空，拒绝写入空内容
        if (content == null || content.isEmpty()) {
            throw new IOException("【拒绝写入】不能使用空文本覆盖文件。如需清空文件内容，请使用专门的 dlqq_clear_file 工具。");
        }

        // 读取原文件内容（用于对比）
        List<String> oldLines = new ArrayList<>();
        int oldLineCount = 0;
        if (Files.exists(path)) {
            oldLines = Files.readAllLines(path, cs);
            oldLineCount = oldLines.size();
        }

        // 直接写入内容（覆盖原有内容或创建新文件）
        Files.writeString(path, content, cs);

        // 计算新文件行数
        int newLineCount = content.split("\r?\n", -1).length;

        // 生成覆盖报告（带+/-差异）
        StringBuilder report = new StringBuilder();

        int diff = newLineCount - oldLineCount;
        StringBuilder headerInfo = new StringBuilder();
        headerInfo.append("覆盖文件：").append(filePath).append("\n");
        if (oldLineCount > 0) {
            headerInfo.append("修改详情：删除 ").append(oldLineCount).append(" 行，新增 ").append(newLineCount).append(" 行");
            if (diff > 0) {
                headerInfo.append(String.format(" (+%d 行)\n", diff));
            } else if (diff < 0) {
                headerInfo.append(String.format(" (%d 行)\n", diff));
            } else {
                headerInfo.append(" (行数不变)\n");
            }
        } else {
            headerInfo.append("新增 ").append(newLineCount).append(" 行\n");
        }
        headerInfo.append("原文件行数：").append(oldLineCount).append(" 行 → 新文件行数：").append(newLineCount).append(" 行\n\n");

        String footerInfo = "\n【状态】✓ 覆盖成功\n";

        report.append(headerInfo);

        // 显示旧内容（-）和新内容（+）
        if (oldLineCount > 0) {
            // 显示旧内容（被删除）
            for (int i = 0; i < oldLines.size(); i++) {
                report.append(String.format("- %4d | %s\n", i + 1, oldLines.get(i)));
            }
            report.append("\n");
        }

        // 显示新内容（新增）
        String[] newLines = content.split("\r?\n", -1);
        for (int i = 0; i < newLines.length; i++) {
            report.append(String.format("+ %4d | %s\n", i + 1, newLines[i]));
        }

        report.append(footerInfo);
        return report.toString();
    }

    /**
     * 在文件末尾追加内容
     *
     * @param filePath 文件路径
     * @param content  追加的内容
     * @param charset  字符集（如 UTF-8、GBK、ISO-8859-1 等，默认为 UTF-8）
     * @return 追加结果报告，包含文件末尾上下文
     * @throws IOException 写入异常
     */
    public static String appendFile(String filePath, String content, String charset) throws IOException {
        filePath = FileUtils.normalizePath(filePath);

        // 检查文件是否被写保护（只在项目内检查）
        boolean insideProject = isInsideProject(filePath);
        if (AIRulesUtils.isWriteProtected(filePath, insideProject)) {
            throw new IOException("文件被写保护规则禁止修改（.aiwriterules）: " + filePath);
        }

        // 跨目录写入保护检查
        String checkResult = checkCrossDirectoryWrite(filePath, "追加文件");
        if (checkResult != null) {
            return checkResult;
        }

        Path path = Paths.get(filePath);

        // 检查内容是否为空，拒绝追加空内容
        if (content == null || content.isEmpty()) {
            throw new IOException("【拒绝追加】不能使用空文本追加文件。如需清空文件内容，请使用 dlqq_clear_file 工具。");
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

        // 读取原文件末尾内容（用于显示上下文）
        List<String> oldLines = new ArrayList<>();
        int oldLineCount = 0;
        if (Files.exists(path)) {
            oldLines = Files.readAllLines(path, cs);
            oldLineCount = oldLines.size();
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

        // 生成追加报告
        return generateAppendReport(filePath, content, oldLines, oldLineCount, cs);
    }

    /**
     * 生成文件追加报告 - 带+/-差异
     */
    private static String generateAppendReport(String filePath, String content, List<String> oldLines, int oldLineCount, Charset cs) throws IOException {
        StringBuilder report = new StringBuilder();

        // 计算追加内容的行数
        List<String> newContentLines = Arrays.asList(content.split("\n", -1));
        if (newContentLines.size() == 1 && content.contains("\r\n")) {
            newContentLines = Arrays.asList(content.split("\r\n", -1));
        }

        int newTotalLines = oldLineCount + newContentLines.size();

        StringBuilder headerInfo = new StringBuilder();
        headerInfo.append("追加内容到文件：").append(filePath).append("\n");
        headerInfo.append("新增 ").append(newContentLines.size()).append(" 行 (+").append(newContentLines.size()).append(" 行)\n");
        headerInfo.append("原文件行数：").append(oldLineCount).append(" 行 → 新文件行数：").append(newTotalLines).append(" 行\n\n");

        String footerInfo = "\n【状态】✓ 内容追加成功\n";

        report.append(headerInfo);

        // 显示上文（最后5行）- 保持不变
        if (oldLineCount > 0) {
            int displayStart = Math.max(1, oldLineCount - 4);
            for (int i = displayStart - 1; i < oldLineCount; i++) {
                report.append(String.format("  %4d | %s\n", i + 1, oldLines.get(i)));
            }
        }

        // 显示追加内容（+）
        for (int i = 0; i < newContentLines.size(); i++) {
            int lineNum = oldLineCount + i + 1;
            report.append(String.format("+ %4d | %s\n", lineNum, newContentLines.get(i)));
        }

        report.append(footerInfo);
        return report.toString();
    }

    /**
     * 清空文件内容（保留文件本身）- 专门用于清空文件
     *
     * @param filePath 文件路径
     * @param charset  字符集（如 UTF-8、GBK、ISO-8859-1 等，默认为 UTF-8）
     * @return 清空结果报告
     * @throws IOException 操作异常
     */
    public static String clearFile(String filePath, String charset) throws IOException {
        filePath = FileUtils.normalizePath(filePath);

        // 检查文件是否被写保护（只在项目内检查）
        boolean insideProject = isInsideProject(filePath);
        if (AIRulesUtils.isWriteProtected(filePath, insideProject)) {
            throw new IOException("文件被写保护规则禁止修改（.aiwriterules）: " + filePath);
        }

        // 跨目录写入保护检查
        String checkResult = checkCrossDirectoryWrite(filePath, "清空文件");
        if (checkResult != null) {
            return checkResult;
        }

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

        // 读取原文件内容
        List<String> oldLines = new ArrayList<>();
        int oldLineCount = 0;
        if (Files.exists(path)) {
            oldLines = Files.readAllLines(path, cs);
            oldLineCount = oldLines.size();
        }

        // 确保父目录存在
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        // 写入空内容，清空文件
        Files.writeString(path, "", cs);

        // 生成清空报告（带+/-差异）
        StringBuilder report = new StringBuilder();

        StringBuilder headerInfo = new StringBuilder();
        headerInfo.append("清空文件：").append(filePath).append("\n");
        headerInfo.append("删除 ").append(oldLineCount).append(" 行 (-").append(oldLineCount).append(" 行)\n");
        headerInfo.append("原文件行数：").append(oldLineCount).append(" 行 → 新文件行数：0 行 (-").append(oldLineCount).append(" 行)\n\n");

        String footerInfo = "\n【状态】✓ 清空成功\n";

        report.append(headerInfo);

        // 显示被删除的内容（-）
        for (int i = 0; i < oldLines.size(); i++) {
            report.append(String.format("- %4d | %s\n", i + 1, oldLines.get(i)));
        }

        report.append(footerInfo);
        return report.toString();
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

        // 检查文件是否被写保护（只在项目内检查）
        boolean insideProject = isInsideProject(filePath);
        if (AIRulesUtils.isWriteProtected(filePath, insideProject)) {
            throw new IOException("文件被写保护规则禁止修改（.aiwriterules）: " + filePath);
        }

        // 跨目录写入保护检查
        String checkResult = checkCrossDirectoryWrite(filePath, "删除文件");
        if (checkResult != null) {
            throw new IOException(checkResult);
        }

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

        // 跨目录写入保护检查
        String checkResult = checkCrossDirectoryOperation(sourcePath, targetPath, "复制文件");
        if (checkResult != null) {
            throw new IOException(checkResult);
        }

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

        // 跨目录写入保护检查
        String checkResult = checkCrossDirectoryOperation(sourcePath, targetPath, "移动文件");
        if (checkResult != null) {
            throw new IOException(checkResult);
        }

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

        // 跨目录写入保护检查
        String checkResult = checkCrossDirectoryWrite(sourcePath, "重命名文件");
        if (checkResult != null) {
            throw new IOException(checkResult);
        }

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

        // 跨目录写入保护检查
        String checkResult = checkCrossDirectoryWrite(sourcePath, "重命名目录");
        if (checkResult != null) {
            throw new IOException(checkResult);
        }

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

        // 检查文件是否被写保护（只在项目内检查）
        boolean insideProject = isInsideProject(filePath);
        if (AIRulesUtils.isWriteProtected(filePath, insideProject)) {
            throw new IOException("文件被写保护规则禁止修改（.aiwriterules）: " + filePath);
        }

        // 跨目录写入保护检查
        String checkResult = checkCrossDirectoryWrite(filePath, "删除行");
        if (checkResult != null) {
            return checkResult;
        }

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
     * 生成删除操作对比报告 - 使用标准diff格式（Markdown兼容）
     */
    private static String generateDeleteReportWithContext(List<String> oldLines, List<String> newLines,
                                                           int startLine, int endLine, String removedContent) {
        StringBuilder report = new StringBuilder();

        int oldTotalLines = oldLines.size();
        int newTotalLines = newLines.size();
        int deletedCount = endLine - startLine + 1;
        
        String headerInfo = "删除第 " + startLine + "-" + endLine + " 行\n" +
            "原文件行数：" + oldTotalLines + " 行 → " +
            "新文件行数：" + newTotalLines + " 行 (-" + deletedCount + " 行)\n\n";
        String footerInfo = "\n【状态】✓ 删除成功\n";
        
        report.append(headerInfo);

        // 计算显示范围
        int displayStart = Math.max(1, startLine - 5);
        int displayOldEnd = Math.min(oldTotalLines, endLine + 5);

        // 上文（前5行）
        for (int i = displayStart - 1; i < startLine - 1 && i < oldLines.size(); i++) {
            report.append(String.format("  %4d | %s\n", i + 1, oldLines.get(i)));
        }

        // 被删除的内容 - 使用智能截断
        if (removedContent != null && !removedContent.isEmpty()) {
            String[] removedContentLines = removedContent.split("\n");
            appendContentWithSmartTruncation(report, headerInfo, footerInfo, removedContentLines, startLine, "- ");
        }

        // 下文（后5行）- 从新文件中获取，行号需要重新计算
        int newStartLine = startLine; // 新文件中对应删除位置的起始行
        int contextAfter = 5; // 下文显示5行
        for (int i = 0; i < contextAfter && (newStartLine - 1 + i) < newLines.size(); i++) {
            int newLineNum = newStartLine + i;
            report.append(String.format("  %4d | %s\n", newLineNum, newLines.get(newStartLine - 1 + i)));
        }

        report.append(footerInfo);
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

        // 检查文件是否被写保护（只在项目内检查）
        boolean insideProject = isInsideProject(filePath);
        if (AIRulesUtils.isWriteProtected(filePath, insideProject)) {
            throw new IOException("文件被写保护规则禁止修改（.aiwriterules）: " + filePath);
        }

        // 跨目录写入保护检查
        String checkResult = checkCrossDirectoryWrite(filePath, "替换行");
        if (checkResult != null) {
            return checkResult;
        }

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

        // 检查新内容是否为空，拒绝写入空内容
        if (newContent == null || newContent.isEmpty()) {
            return "【拒绝修改】不能使用空文本替换文件内容。如需删除这些行，请使用 dlqq_remove_lines_in_file 工具；如需清空文件，请使用 dlqq_clear_file 工具。";
        }

        // 准备新内容行
        String[] newLines = newContent.split("\\r?\\n");

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
     * 生成修改前后对比报告 - 使用标准diff格式（Markdown兼容）
     */
    private static String generateDiffReportWithContext(List<String> oldLines, List<String> newLines,
                                                         int startLine, int endLine, String oldContent, String newContent) {
        StringBuilder report = new StringBuilder();

        int oldTotalLines = oldLines.size();
        int newTotalLines = newLines.size();
        int oldLineCount = (oldContent != null && !oldContent.isEmpty()) ? oldContent.split("\n").length : 0;
        int newLineCount = (newContent != null && !newContent.isEmpty()) ? newContent.split("\r?\n").length : 0;
        int newEndLine = startLine + newLineCount - 1;
        int diff = newLineCount - oldLineCount;

        // 构建修改统计信息
        StringBuilder modifyInfo = new StringBuilder();
        modifyInfo.append("替换第 ").append(startLine).append("-").append(endLine).append(" 行\n");
        modifyInfo.append("修改详情：删除 ").append(oldLineCount).append(" 行，新增 ").append(newLineCount).append(" 行");
        if (diff > 0) {
            modifyInfo.append(String.format(" (+%d 行)\n", diff));
        } else if (diff < 0) {
            modifyInfo.append(String.format(" (%d 行)\n", diff));
        } else {
            modifyInfo.append(" (行数不变)\n");
        }
        modifyInfo.append("原文件行数：").append(oldTotalLines).append(" 行 → 新文件行数：").append(newTotalLines).append(" 行\n\n");

        String headerInfo = modifyInfo.toString();
        String footerInfo = "\n【状态】✓ 替换成功\n";
        
        report.append(headerInfo);

        // 计算显示范围
        int displayStart = Math.max(1, startLine - 5);
        int displayOldEnd = Math.min(oldTotalLines, endLine + 5);

        // 上文（前5行）
        for (int i = displayStart - 1; i < startLine - 1 && i < oldLines.size(); i++) {
            report.append(String.format("  %4d | %s\n", i + 1, oldLines.get(i)));
        }

        // 原始内容（标记为-）
        if (oldContent != null && !oldContent.isEmpty()) {
            String[] oldContentLines = oldContent.split("\n");
            for (int i = 0; i < oldContentLines.length; i++) {
                report.append(String.format("- %4d | %s\n", startLine + i, oldContentLines[i]));
            }
        }

        // 新内容 - 使用智能截断
        if (newContent != null && !newContent.isEmpty()) {
            String[] newContentLines = newContent.split("\r?\n");
            appendContentWithSmartTruncation(report, headerInfo, footerInfo, newContentLines, startLine, "+ ");
        }

        // 下文（后5行）
        int newLineOffset = newLineCount - oldLineCount;
        for (int i = endLine + 1; i <= displayOldEnd && i <= oldLines.size(); i++) {
            int newLineNum = i + newLineOffset;
            if (newLineNum <= newLines.size()) {
                report.append(String.format("  %4d | %s\n", newLineNum, newLines.get(newLineNum - 1)));
            }
        }

        report.append(footerInfo);
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

        // 检查文件是否被写保护（只在项目内检查）
        boolean insideProject = isInsideProject(filePath);
        if (AIRulesUtils.isWriteProtected(filePath, insideProject)) {
            throw new IOException("文件被写保护规则禁止修改（.aiwriterules）: " + filePath);
        }

        // 跨目录写入保护检查
        String checkResult = checkCrossDirectoryWrite(filePath, "插入文本");
        if (checkResult != null) {
            return checkResult;
        }

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

        // 检查插入内容是否为空，拒绝插入空内容
        if (content == null || content.isEmpty()) {
            return "【拒绝修改】不能使用空文本插入文件。如需清空文件，请使用 dlqq_clear_file 工具。";
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
     * 生成插入操作对比报告 - 普通文本格式
     */
    private static String generateInsertReportWithContext(List<String> newLines, int lineNum, String insertedContent,
                                                         int oldTotalLines, int newTotalLines) {
        StringBuilder report = new StringBuilder();

        int insertLineCount = (insertedContent != null && !insertedContent.isEmpty()) ? insertedContent.split("\\r?\\n").length : 0;
        int newStartLine = lineNum;
        int newEndLine = lineNum + insertLineCount - 1;
        int insertedCount = newTotalLines - oldTotalLines;
        
        String headerInfo = "在第 " + lineNum + " 行之前插入\n" +
            "原文件行数：" + oldTotalLines + " 行 → " +
            "新文件行数：" + newTotalLines + " 行 (+" + insertedCount + " 行)\n\n";
        String footerInfo = "\n【状态】✓ 插入成功\n";
        
        report.append(headerInfo);

        // 计算显示范围
        int displayStart = Math.max(1, newStartLine - 5);
        int displayEnd = Math.min(newTotalLines, newEndLine + 5);

        // 上文（前5行）
        for (int i = displayStart - 1; i < newStartLine - 1 && i < newLines.size(); i++) {
            report.append(String.format("  %4d | %s\n", i + 1, newLines.get(i)));
        }

        // 插入的内容 - 使用智能截断
        if (insertedContent != null && !insertedContent.isEmpty()) {
            String[] insertContentLines = insertedContent.split("\\r?\\n");
            appendContentWithSmartTruncation(report, headerInfo, footerInfo, insertContentLines, newStartLine, "+ ");
        }

        // 下文（后5行）
        for (int i = newEndLine; i < displayEnd && i < newLines.size(); i++) {
            report.append(String.format("  %4d | %s\n", i + 1, newLines.get(i)));
        }

        report.append(footerInfo);
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
     * @param startLine   起始行号（1-based，可选，null表示读取全部）
     * @param endLine     结束行号（1-based，可选，-1表示到末尾，null表示读取全部）
     * @return 替换结果报告，包含修改前后对比
     * @throws IOException 操作异常
     */
    public static String replaceTextInFile(String filePath, String searchText, String replaceText, boolean useRegex, String charset, Integer startLine, Integer endLine) throws IOException {
        filePath = FileUtils.normalizePath(filePath);

        // 检查文件是否被写保护（只在项目内检查）
        boolean insideProject = isInsideProject(filePath);
        if (AIRulesUtils.isWriteProtected(filePath, insideProject)) {
            throw new IOException("文件被写保护规则禁止修改（.aiwriterules）: " + filePath);
        }

        // 跨目录写入保护检查
        String checkResult = checkCrossDirectoryWrite(filePath, "替换文本");
        if (checkResult != null) {
            return checkResult;
        }

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

        // 处理行号范围
        int checkStartLine = 1;
        int checkEndLine = totalLines;
        boolean isRangeMode = (startLine != null && endLine != null);

        if (isRangeMode) {
            // 使用传入的行范围
            checkStartLine = Math.max(1, startLine);
            checkEndLine = endLine == -1 ? totalLines : Math.min(endLine, totalLines);
            if (checkStartLine > totalLines) {
                checkStartLine = totalLines;
            }
            if (checkEndLine < checkStartLine) {
                checkEndLine = checkStartLine;
            }
        }

        // 检查是否已读取指定行范围
        List<Integer> unreadLines = checkUnreadLines(filePath, checkStartLine, checkEndLine);
        if (!unreadLines.isEmpty()) {
            StringBuilder errorMsg = new StringBuilder();
            if (isRangeMode) {
                errorMsg.append("【拒绝修改】您尚未读取指定行范围的内容。您尚未读取以下行：\n");
            } else {
                errorMsg.append("【拒绝修改】文本替换需要了解文件全部内容。您尚未读取以下行：\n");
            }
            int showCount = Math.min(unreadLines.size(), 10);
            for (int i = 0; i < showCount; i++) {
                errorMsg.append("  - 第 ").append(unreadLines.get(i)).append(" 行\n");
            }
            if (unreadLines.size() > 10) {
                errorMsg.append("  ... 还有 ").append(unreadLines.size() - 10).append(" 行未读取\n");
            }
            if (isRangeMode) {
                errorMsg.append("\n建议：请先使用 dlqq_read_file 读取第 ").append(checkStartLine).append("-").append(checkEndLine).append(" 行的内容。");
            } else {
                errorMsg.append("\n建议：使用 dlqq_read_file 读取整个文件内容，或传入start_line/end_line参数指定修改范围。");
            }
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
            // 检查替换后的内容是否为空，拒绝写入空内容
            if (newContent == null || newContent.isEmpty()) {
                return "【拒绝修改】替换后的内容为空。文本替换操作不能导致文件内容被完全删除。如需清空文件，请使用 dlqq_clear_file 工具。";
            }
            Files.writeString(path, newContent, cs);
            // 标记文件已被修改
            markFileModified(filePath);
        }

        // 生成对比报告
        return generateTextReplaceReport(lines, content, newContent, searchText, replaceText, useRegex, replaceCount, matchPositions);
    }

    /**
     * 将匹配的文本替换为空（删除匹配的文本）- 专门用于删除文本
     *
     * @param filePath    文件路径
     * @param searchText  搜索文本
     * @param useRegex    是否使用正则表达式
     * @param charset     字符集（如 UTF-8、GBK、ISO-8859-1 等，默认为 UTF-8）
     * @param startLine   起始行号（1-based，可选，null表示读取全部）
     * @param endLine     结束行号（1-based，可选，-1表示到末尾，null表示读取全部）
     * @return 删除结果报告，包含修改前后对比
     * @throws IOException 操作异常
     */
    public static String removeTextInFile(String filePath, String searchText, boolean useRegex, String charset, Integer startLine, Integer endLine) throws IOException {
        // 调用 replaceTextInFile，将替换文本设为空字符串
        return replaceTextInFile(filePath, searchText, "", useRegex, charset, startLine, endLine);
    }

    /**
     * 生成文本替换对比报告 - 使用diff风格（带颜色）
     */
    private static String generateTextReplaceReport(List<String> oldLines, String oldContent, String newContent,
                                                     String searchText, String replaceText, boolean useRegex,
                                                     int replaceCount, List<int[]> matchPositions) {
        StringBuilder report = new StringBuilder();

        // 分割新内容为行列表
        List<String> newLines = Arrays.asList(newContent.split("\n", -1));
        if (newLines.size() == 1 && newContent.contains("\r\n")) {
            newLines = Arrays.asList(newContent.split("\r\n", -1));
        }

        // 计算每处修改的行号
        List<Integer> modifiedLineNumbers = new ArrayList<>();
        for (int[] pos : matchPositions) {
            int lineNum = 1;
            int charCount = 0;
            for (int i = 0; i < oldLines.size(); i++) {
                String line = oldLines.get(i);
                if (charCount + line.length() + 1 > pos[0]) {
                    lineNum = i + 1;
                    break;
                }
                charCount += line.length() + 1;
            }
            if (!modifiedLineNumbers.contains(lineNum)) {
                modifiedLineNumbers.add(lineNum);
            }
        }

        // 判断是否为删除操作（replaceText为空）
        boolean isDeleteOperation = (replaceText == null || replaceText.isEmpty());

        // 计算被替换内容的行数（通过searchText和replaceText）
        int oldContentLines = (searchText != null && !searchText.isEmpty()) ? searchText.split("\r?\n").length : 0;
        int newContentLines = (replaceText != null && !replaceText.isEmpty()) ? replaceText.split("\r?\n").length : 0;
        int contentDiff = newContentLines - oldContentLines;
        int totalDiff = newLines.size() - oldLines.size();

        StringBuilder headerInfo = new StringBuilder();
        if (isDeleteOperation) {
            headerInfo.append("文本删除：").append(useRegex ? "正则" : "普通文本").append(" | ");
            headerInfo.append("删除次数：").append(replaceCount).append(" 处\n");
            headerInfo.append("修改详情：删除 ").append(oldContentLines).append(" 行");
            headerInfo.append(String.format(" (%d 行)\n", totalDiff));
        } else {
            headerInfo.append("文本替换：").append(useRegex ? "正则" : "普通文本").append(" | ");
            headerInfo.append("替换次数：").append(replaceCount).append(" 处\n");
            headerInfo.append("修改详情：删除 ").append(oldContentLines).append(" 行，新增 ").append(newContentLines).append(" 行");
            if (contentDiff > 0) {
                headerInfo.append(String.format(" (+%d 行)\n", contentDiff));
            } else if (contentDiff < 0) {
                headerInfo.append(String.format(" (%d 行)\n", contentDiff));
            } else {
                headerInfo.append(" (行数不变)\n");
            }
        }
        headerInfo.append("原文件行数：").append(oldLines.size()).append(" 行 → 新文件行数：").append(newLines.size()).append(" 行");
        if (totalDiff != 0) {
            headerInfo.append(String.format(" (%+d 行)\n\n", totalDiff));
        } else {
            headerInfo.append("\n\n");
        }
        String footerInfo = isDeleteOperation ? "\n【状态】✓ 删除成功\n" : "\n【状态】✓ 替换成功\n";

        report.append(headerInfo);

        // 为每处修改生成diff
        int maxModifications = 20; // 最多显示20处修改，避免报告过长
        for (int i = 0; i < modifiedLineNumbers.size() && i < maxModifications; i++) {
            int modLine = modifiedLineNumbers.get(i);
            int displayStart = Math.max(1, modLine - 3);
            int displayOldEnd = Math.min(oldLines.size(), modLine + 3);
            int displayNewEnd = Math.min(newLines.size(), modLine + 3);

            // 上文（从旧文件获取）
            for (int lineNum = displayStart; lineNum < modLine && lineNum <= oldLines.size(); lineNum++) {
                report.append(String.format("  %4d | %s\n", lineNum, oldLines.get(lineNum - 1)));
            }

            // 修改行：原行（-）
            if (modLine <= oldLines.size()) {
                report.append(String.format("- %4d | %s\n", modLine, oldLines.get(modLine - 1)));
            }
            
            // 新行（+）- 只有替换操作才显示
            if (!isDeleteOperation && modLine <= newLines.size()) {
                report.append(String.format("+ %4d | %s\n", modLine, newLines.get(modLine - 1)));
            }

            // 下文（删除操作从新文件获取，替换操作从旧文件获取）
            if (isDeleteOperation) {
                // 删除操作：下文从新文件获取，行号需要调整
                int linesAfter = Math.min(3, newLines.size() - (modLine - 1));
                for (int j = 0; j < linesAfter; j++) {
                    int newLineNum = modLine + j;
                    if ((modLine - 1 + j) < newLines.size()) {
                        report.append(String.format("  %4d | %s\n", newLineNum, newLines.get(modLine - 1 + j)));
                    }
                }
            } else {
                // 替换操作：下文从旧文件获取
                for (int lineNum = modLine + 1; lineNum <= displayOldEnd && lineNum <= oldLines.size(); lineNum++) {
                    report.append(String.format("  %4d | %s\n", lineNum, oldLines.get(lineNum - 1)));
                }
            }

            if (i < modifiedLineNumbers.size() - 1 && i < maxModifications - 1) {
                report.append("\n");
            }
        }
        
        // 如果有更多修改，显示省略信息
        if (modifiedLineNumbers.size() > maxModifications) {
            report.append("\n  ... (还有 ").append(modifiedLineNumbers.size() - maxModifications).append(" 处修改未显示) ...\n");
        }

        report.append(footerInfo);
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

        // 跨目录写入保护检查
        String checkResult = checkCrossDirectoryWrite(dirPath, "创建目录");
        if (checkResult != null) {
            throw new IOException(checkResult);
        }

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

        // 跨目录写入保护检查
        String checkResult = checkCrossDirectoryWrite(dirPath, "创建多级目录");
        if (checkResult != null) {
            throw new IOException(checkResult);
        }

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

        // 跨目录写入保护检查
        String checkResult = checkCrossDirectoryWrite(dirPath, "删除目录");
        if (checkResult != null) {
            throw new IOException(checkResult);
        }

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

        // 跨目录写入保护检查
        String checkResult = checkCrossDirectoryOperation(sourcePath, targetPath, "复制目录");
        if (checkResult != null) {
            throw new IOException(checkResult);
        }

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

        // 跨目录写入保护检查
        String checkResult = checkCrossDirectoryOperation(sourcePath, targetPath, "移动目录");
        if (checkResult != null) {
            throw new IOException(checkResult);
        }

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

    // ==================== 文本搜索工具 ====================

    /**
     * 搜索结果类
     */
    public static class TextSearchResult {
        public final String filePath;
        public final int lineNumber;
        public final String lineContent;
        public final List<String> contextLines;

        public TextSearchResult(String filePath, int lineNumber, String lineContent, List<String> contextLines) {
            this.filePath = filePath;
            this.lineNumber = lineNumber;
            this.lineContent = lineContent;
            this.contextLines = contextLines;
        }
    }

    /**
     * 在指定目录下搜索文本内容
     *
     * @param searchPath      搜索目录路径
     * @param pattern         搜索模式
     * @param useRegex        是否使用正则表达式
     * @param ignoreCase      是否忽略大小写
     * @param textExtensions  文本文件后缀名列表（如 [".java", ".txt", ".c"]）
     * @param maxFiles        最大搜索文件数（-1表示无限制）
     * @param maxFileSize     最大文件大小（字节，-1表示无限制）
     * @return 搜索结果列表
     * @throws IOException 搜索异常
     */
    public static List<TextSearchResult> searchTextInFiles(
            String searchPath,
            String pattern,
            boolean useRegex,
            boolean ignoreCase,
            List<String> textExtensions,
            int maxFiles,
            long maxFileSize) throws IOException {

        searchPath = FileUtils.normalizePath(searchPath);
        Path path = Paths.get(searchPath);
        if (!Files.exists(path)) {
            throw new IOException("目录不存在: " + searchPath);
        }
        if (!Files.isDirectory(path)) {
            throw new IOException("路径不是目录: " + searchPath);
        }

        List<TextSearchResult> results = new ArrayList<>();

        // 准备匹配器
        Pattern regex = null;
        String searchPattern = ignoreCase ? pattern.toLowerCase() : pattern;

        if (useRegex) {
            try {
                int flags = ignoreCase ? Pattern.CASE_INSENSITIVE : 0;
                regex = Pattern.compile(pattern, flags);
            } catch (PatternSyntaxException e) {
                throw new IOException("正则表达式语法错误: " + e.getMessage());
            }
        }

        // 准备文件扩展名过滤器
        Set<String> extensionSet = textExtensions.stream()
            .map(ext -> ext.toLowerCase().startsWith(".") ? ext.toLowerCase() : "." + ext.toLowerCase())
            .collect(Collectors.toSet());

        // 检查是否在项目内（用于.aiignore规则）
        boolean insideProject = isInsideProject(searchPath);

        // 遍历文件
        final List<TextSearchResult> finalResults = results;
        final Pattern finalRegex = regex;
        final String finalSearchPattern = searchPattern;
        final boolean finalInsideProject = insideProject;

        try (var paths = Files.walk(path)) {
            paths.filter(Files::isRegularFile)
                .filter(p -> isTextFile(p, extensionSet))
                .filter(p -> !AIRulesUtils.isIgnored(p.toString(), finalInsideProject)) // 过滤被忽略的文件
                .filter(p -> {
                    try {
                        return maxFileSize <= 0 || Files.size(p) <= maxFileSize;
                    } catch (IOException e) {
                        return false;
                    }
                })
                .limit(maxFiles > 0 ? maxFiles : Long.MAX_VALUE)
                .forEach(p -> {
                    try {
                        searchInFile(p, finalRegex, finalSearchPattern, useRegex, ignoreCase, finalResults);
                    } catch (IOException e) {
                        // 跳过读取失败的文件
                    }
                });
        }

        // 记录搜索使用的文件扩展名，用于后续替换验证
        recordDirSearch(searchPath, pattern, useRegex, textExtensions);

        return finalResults;
    }

    /**
     * 记录目录搜索使用的文件扩展名
     * @param dirPath 搜索目录
     * @param pattern 搜索模式
     * @param useRegex 是否使用正则
     * @param extensions 文件扩展名列表
     */
    private static void recordDirSearch(String dirPath, String pattern, boolean useRegex, List<String> extensions) {
        String key = FileUtils.normalizePath(dirPath) + "|" + pattern + "|" + useRegex;
        // 保存扩展名（统一小写，不带点）
        List<String> normalizedExts = extensions.stream()
            .map(ext -> ext.toLowerCase().startsWith(".") ? ext.toLowerCase().substring(1) : ext.toLowerCase())
            .collect(Collectors.toList());
        dirSearchTracker.put(key, normalizedExts);
    }

    /**
     * 检查替换时的文件扩展名是否在搜索记录的范围内
     * @param dirPath 目录路径
     * @param searchPattern 搜索模式
     * @param useRegex 是否使用正则
     * @param replaceExtensions 替换时的文件扩展名列表
     * @return 如果允许替换返回null，否则返回错误信息
     */
    private static String checkReplaceExtensions(String dirPath, String searchPattern, boolean useRegex, List<String> replaceExtensions) {
        String key = FileUtils.normalizePath(dirPath) + "|" + searchPattern + "|" + useRegex;
        List<String> searchExts = dirSearchTracker.get(key);

        if (searchExts == null || searchExts.isEmpty()) {
            return "【错误】未找到对应的搜索记录。请先使用 dlqq_search_text_in_dir 进行搜索，然后再执行替换操作。";
        }

        // 标准化替换扩展名
        List<String> normalizedReplaceExts = replaceExtensions.stream()
            .map(ext -> ext.toLowerCase().startsWith(".") ? ext.toLowerCase().substring(1) : ext.toLowerCase())
            .distinct()
            .collect(Collectors.toList());

        // 检查替换扩展名是否都是搜索扩展名的子集
        List<String> invalidExts = new ArrayList<>();
        for (String ext : normalizedReplaceExts) {
            if (!searchExts.contains(ext)) {
                invalidExts.add(ext);
            }
        }

        if (!invalidExts.isEmpty()) {
            return "【错误】替换时的文件类型超出了搜索时的范围。\n" +
                   "搜索时允许的文件类型: " + String.join(", ", searchExts) + "\n" +
                   "替换时尝试使用的文件类型: " + String.join(", ", normalizedReplaceExts) + "\n" +
                   "不允许的文件类型: " + String.join(", ", invalidExts);
        }

        return null; // 验证通过
    }

    /**
     * 在单个文件中搜索文本
     *
     * @param filePath   文件路径
     * @param pattern    搜索模式
     * @param useRegex   是否使用正则表达式
     * @param ignoreCase 是否忽略大小写
     * @return 搜索结果列表
     * @throws IOException 搜索异常
     */
    public static List<TextSearchResult> searchTextInSingleFile(
            String filePath,
            String pattern,
            boolean useRegex,
            boolean ignoreCase) throws IOException {

        filePath = FileUtils.normalizePath(filePath);
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("文件不存在: " + filePath);
        }
        if (!Files.isRegularFile(path)) {
            throw new IOException("路径不是文件: " + filePath);
        }

        List<TextSearchResult> results = new ArrayList<>();

        // 准备匹配器
        Pattern regex = null;
        String searchPattern = ignoreCase ? pattern.toLowerCase() : pattern;

        if (useRegex) {
            try {
                int flags = ignoreCase ? Pattern.CASE_INSENSITIVE : 0;
                regex = Pattern.compile(pattern, flags);
            } catch (PatternSyntaxException e) {
                throw new IOException("正则表达式语法错误: " + e.getMessage());
            }
        }

        // 搜索文件
        searchInFile(path, regex, searchPattern, useRegex, ignoreCase, results);

        return results;
    }

    /**
     * 检查是否为文本文件
     */
    private static boolean isTextFile(Path path, Set<String> extensionSet) {
        String fileName = path.getFileName().toString().toLowerCase();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            String ext = fileName.substring(dotIndex);
            return extensionSet.contains(ext);
        }
        // 如果没有扩展名但包含 "*"（表示所有文件）
        return extensionSet.contains("*") || extensionSet.contains(".*");
    }

    /**
     * 在单个文件中搜索文本 - 使用流式读取，支持大文件
     */
    private static void searchInFile(Path file, Pattern regex, String searchPattern,
                                     boolean useRegex, boolean ignoreCase,
                                     List<TextSearchResult> results) throws IOException {
        // 使用流式读取，避免大文件导致内存溢出
        List<String> contextBuffer = new ArrayList<>(); // 保存前后3行上下文
        int lineNumber = 0;

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;

                boolean matched;
                if (useRegex) {
                    matched = regex.matcher(line).find();
                } else {
                    String checkLine = ignoreCase ? line.toLowerCase() : line;
                    matched = checkLine.contains(searchPattern);
                }

                if (matched) {
                    // 构建上下文：需要前3行，当前行，后3行
                    List<String> context = new ArrayList<>();

                    // 添加前3行（从contextBuffer中）
                    int startIdx = Math.max(0, contextBuffer.size() - 3);
                    for (int i = startIdx; i < contextBuffer.size(); i++) {
                        String[] parts = contextBuffer.get(i).split("\n", 2);
                        int ctxLineNum = Integer.parseInt(parts[0]);
                        context.add("    " + String.format("%4d", ctxLineNum) + " | " + parts[1]);
                    }

                    // 添加当前行（标记为匹配行）
                    context.add(">>> " + String.format("%4d", lineNumber) + " | " + line);

                    // 读取后3行
                    int linesAfter = 0;
                    StringBuilder afterBuffer = new StringBuilder();
                    String afterLine;
                    while (linesAfter < 3 && (afterLine = reader.readLine()) != null) {
                        lineNumber++;
                        linesAfter++;
                        context.add("    " + String.format("%4d", lineNumber) + " | " + afterLine);
                        afterBuffer.append(lineNumber).append("\n").append(afterLine).append("\n");
                    }

                    results.add(new TextSearchResult(
                        file.toString(),
                        lineNumber - linesAfter, // 当前匹配行的行号
                        line,
                        context
                    ));

                    // 将后读取的行添加到contextBuffer
                    if (afterBuffer.length() > 0) {
                        String[] afterLines = afterBuffer.toString().split("\n");
                        for (int i = 0; i < afterLines.length - 1; i += 2) {
                            if (i + 1 < afterLines.length) {
                                contextBuffer.add(afterLines[i] + "\n" + afterLines[i + 1]);
                            }
                        }
                    }
                }

                // 维护contextBuffer（只保留最近3行）
                contextBuffer.add(lineNumber + "\n" + line);
                if (contextBuffer.size() > 3) {
                    contextBuffer.remove(0);
                }
            }
        }
    }

    /**
     * 格式化搜索结果为字符串
     */
    public static String formatSearchResults(List<TextSearchResult> results, int maxResults) {
        if (results.isEmpty()) {
            return "未找到匹配的文本";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("找到 ").append(results.size()).append(" 处匹配\n");
        sb.append("=" .repeat(60)).append("\n\n");

        int displayCount = maxResults > 0 ? Math.min(results.size(), maxResults) : results.size();

        for (int i = 0; i < displayCount; i++) {
            TextSearchResult r = results.get(i);
            sb.append("[").append(i + 1).append("] ").append(r.filePath).append("\n");
            sb.append("    第 ").append(r.lineNumber).append(" 行: ").append(truncate(r.lineContent, 80)).append("\n");
            sb.append("    上下文:\n");
            for (String ctx : r.contextLines) {
                sb.append("    ").append(ctx).append("\n");
            }
            sb.append("\n");
        }

        if (results.size() > displayCount) {
            sb.append("... 还有 ").append(results.size() - displayCount).append(" 处匹配未显示\n");
        }

        return sb.toString();
    }

    /**
     * 在目录下所有文本文件中替换文本
     *
     * @param dirPath     目录路径
     * @param searchText  搜索文本
     * @param replaceText 替换文本
     * @param useRegex    是否使用正则表达式
     * @param charset     字符集（如 UTF-8、GBK、ISO-8859-1 等，默认为 UTF-8）
     * @param fileExtensions 文件扩展名列表（如 ["java", "txt"]），为空则使用默认文本文件类型
     * @param maxFiles    最大处理文件数（默认100）
     * @return 替换结果报告
     * @throws IOException 操作异常
     */
    public static String replaceTextInDir(String dirPath, String searchText, String replaceText,
                                           boolean useRegex, String charset, List<String> fileExtensions,
                                           Integer maxFiles) throws IOException {
        dirPath = FileUtils.normalizePath(dirPath);

        // 跨目录写入保护检查
        String checkResult = checkCrossDirectoryWrite(dirPath, "目录批量替换");
        if (checkResult != null) {
            return checkResult;
        }

        Path path = Paths.get(dirPath);

        if (!Files.exists(path)) {
            throw new IOException("目录不存在: " + dirPath);
        }
        if (!Files.isDirectory(path)) {
            throw new IOException("路径不是目录: " + dirPath);
        }

        // 参数校验
        if (searchText == null || searchText.isEmpty()) {
            throw new IOException("搜索文本不能为空");
        }
        if (replaceText == null) {
            replaceText = "";
        }

        // 默认文本文件扩展名
        List<String> defaultExtensions = Arrays.asList(
            "java", "txt", "md", "xml", "json", "properties", "yml", "yaml",
            "html", "htm", "js", "css", "py", "c", "cpp", "h", "hpp",
            "bat", "cmd", "sh", "ini", "conf", "cfg"
        );

        List<String> extensions = (fileExtensions != null && !fileExtensions.isEmpty())
            ? fileExtensions.stream().map(String::toLowerCase).collect(Collectors.toList())
            : defaultExtensions;

        // 验证替换时的文件扩展名是否在搜索记录的范围内（搜索时使用相同的搜索文本和正则设置）
        String extCheckResult = checkReplaceExtensions(dirPath, searchText, useRegex, extensions);
        if (extCheckResult != null) {
            return extCheckResult;
        }

        int max = (maxFiles != null && maxFiles > 0) ? maxFiles : 100;

        // 查找所有匹配的文件
        List<Path> matchedFiles = new ArrayList<>();
        try (var stream = Files.walk(path)) {
            matchedFiles = stream
                .filter(Files::isRegularFile)
                .filter(p -> {
                    String fileName = p.getFileName().toString().toLowerCase();
                    int dotIndex = fileName.lastIndexOf('.');
                    if (dotIndex > 0) {
                        String ext = fileName.substring(dotIndex + 1);
                        return extensions.contains(ext);
                    }
                    return false;
                })
                .limit(max + 1) // 多取一个用于判断是否超过限制
                .collect(Collectors.toList());
        }

        boolean limitExceeded = matchedFiles.size() > max;
        if (limitExceeded) {
            matchedFiles = matchedFiles.subList(0, max);
        }

        if (matchedFiles.isEmpty()) {
            return "【未找到】目录下没有匹配的文件类型: " + String.join(", ", extensions);
        }

        // 逐个文件执行替换
        StringBuilder report = new StringBuilder();
        report.append("========================================\n");
        report.append("目录文本替换报告\n");
        report.append("========================================\n\n");
        report.append("目录: ").append(dirPath).append("\n");
        report.append("搜索文本: ").append(searchText).append("\n");
        report.append("替换文本: ").append(replaceText.isEmpty() ? "(空/删除)" : replaceText).append("\n");
        report.append("模式: ").append(useRegex ? "正则表达式" : "普通文本").append("\n");
        report.append("字符集: ").append(charset != null ? charset : "UTF-8").append("\n");
        report.append("文件类型: ").append(String.join(", ", extensions)).append("\n");
        report.append("扫描文件数: ").append(matchedFiles.size()).append(limitExceeded ? " (已达到最大限制 " + max + ")" : "").append("\n\n");

        int totalFilesWithMatches = 0;
        int totalReplacements = 0;
        List<String> modifiedFiles = new ArrayList<>();
        List<String> skippedFiles = new ArrayList<>();

        for (int i = 0; i < matchedFiles.size(); i++) {
            Path file = matchedFiles.get(i);
            String filePath = file.toString();

            report.append("[").append(i + 1).append("/").append(matchedFiles.size()).append("] ");
            report.append(file.getFileName()).append(" ... ");

            try {
                // 调用单文件替换（不检查读取记录，因为目录替换是批量操作）
                String result = replaceTextInFileInternal(filePath, searchText, replaceText, useRegex, charset);

                if (result.contains("替换成功")) {
                    totalFilesWithMatches++;
                    modifiedFiles.add(filePath);
                    // 提取替换次数
                    int count = extractReplaceCount(result);
                    totalReplacements += count;
                    report.append("✓ 替换 ").append(count).append(" 处\n");
                } else if (result.contains("未找到")) {
                    report.append("- 无匹配\n");
                } else {
                    skippedFiles.add(filePath + ": " + result);
                    report.append("✗ 跳过 (").append(result.replace("\n", " ")).append(")\n");
                }
            } catch (Exception e) {
                skippedFiles.add(filePath + ": " + e.getMessage());
                report.append("✗ 错误 (").append(e.getMessage()).append(")\n");
            }
        }

        report.append("\n========================================\n");
        report.append("替换汇总\n");
        report.append("========================================\n");
        report.append("处理文件数: ").append(matchedFiles.size()).append("\n");
        report.append("成功替换文件数: ").append(totalFilesWithMatches).append("\n");
        report.append("总替换次数: ").append(totalReplacements).append("\n");
        report.append("跳过/失败文件数: ").append(skippedFiles.size()).append("\n");

        if (!modifiedFiles.isEmpty()) {
            report.append("\n已修改的文件:\n");
            for (String fp : modifiedFiles) {
                report.append("  ✓ ").append(fp).append("\n");
            }
        }

        if (!skippedFiles.isEmpty()) {
            report.append("\n跳过的文件:\n");
            for (String msg : skippedFiles.subList(0, Math.min(skippedFiles.size(), 10))) {
                report.append("  ✗ ").append(msg).append("\n");
            }
            if (skippedFiles.size() > 10) {
                report.append("  ... 还有 ").append(skippedFiles.size() - 10).append(" 个\n");
            }
        }

        return report.toString();
    }

    /**
     * 内部替换方法（不检查读取记录，用于目录批量替换）
     */
    private static String replaceTextInFileInternal(String filePath, String searchText, String replaceText,
                                                     boolean useRegex, String charset) throws IOException {
        filePath = FileUtils.normalizePath(filePath);
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            throw new IOException("文件不存在: " + filePath);
        }

        Charset cs = StandardCharsets.UTF_8;
        if (charset != null && !charset.trim().isEmpty()) {
            try {
                cs = Charset.forName(charset.trim());
            } catch (Exception e) {
                throw new IOException("不支持的字符集: " + charset);
            }
        }

        String content = Files.readString(path, cs);
        String newContent;
        int replaceCount = 0;

        if (useRegex) {
            try {
                Pattern pattern = Pattern.compile(searchText);
                Matcher matcher = pattern.matcher(content);

                if (!matcher.find()) {
                    return "【未替换】未找到匹配";
                }

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
            if (!content.contains(searchText)) {
                return "【未替换】未找到文本";
            }

            int index = 0;
            while ((index = content.indexOf(searchText, index)) != -1) {
                replaceCount++;
                index += searchText.length();
            }

            newContent = content.replace(searchText, replaceText);
        }

        if (replaceCount > 0) {
            if (newContent == null || newContent.isEmpty()) {
                return "【拒绝修改】替换后的内容为空";
            }
            Files.writeString(path, newContent, cs);
            markFileModified(filePath);
            return "【替换成功】共替换 " + replaceCount + " 处";
        }

        return "【未替换】";
    }

    /**
     * 从替换结果中提取替换次数
     */
    private static int extractReplaceCount(String result) {
        try {
            if (result.contains("共替换")) {
                int start = result.indexOf("共替换") + 3;
                int end = result.indexOf(" 处");
                if (end > start) {
                    return Integer.parseInt(result.substring(start, end).trim());
                }
            }
        } catch (Exception e) {
            // 解析失败返回1
        }
        return 1;
    }

    /**
     * 截断长文本
     */
    private static String truncate(String s, int maxLength) {
        if (s.length() <= maxLength) {
            return s;
        }
        return s.substring(0, maxLength - 3) + "...";
    }
}

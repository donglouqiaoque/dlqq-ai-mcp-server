package com.donglouqiaoque.aitools.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * AI规则工具 - 解析和应用.aiignore/.aiwriterules/.aireadrules规则
 * 类似于.gitignore，用于指定哪些文件/目录应该被AI工具忽略或保护
 *
 * 规则说明：
 * 1. 当前项目内：遵守规则文件
 * 2. 非当前项目：弹JFrame由用户确定，不遵守规则
 * 3. 全局目录修改才需要遵守，单独的文件不需要遵守
 */
public class AIRulesUtils {

    // 缓存解析后的忽略规则 (.aiignore)
    private static final List<IgnoreRule> ignoreRules = new ArrayList<>();
    private static long ignoreLastModified = 0;
    private static String ignoreLoadedPath = null;

    // 缓存解析后的写保护规则 (.aiwriterules)
    private static final List<IgnoreRule> writeRules = new ArrayList<>();
    private static long writeRulesLastModified = 0;
    private static String writeRulesLoadedPath = null;

    // 缓存解析后的读保护规则 (.aireadrules)
    private static final List<IgnoreRule> readRules = new ArrayList<>();
    private static long readRulesLastModified = 0;
    private static String readRulesLoadedPath = null;

    // 规则文件类型枚举
    public enum RuleType {
        IGNORE,   // .aiignore
        WRITE,    // .aiwriterules
        READ      // .aireadrules
    }

    /**
     * 检查文件是否被忽略
     * @param filePath 要检查的文件路径（绝对路径）
     * @param isInsideProject 是否在项目内
     * @return true 表示被忽略，false 表示未忽略
     */
    public static boolean isIgnored(String filePath, boolean isInsideProject) {
        // 非项目内不遵守规则
        if (!isInsideProject) {
            return false;
        }
        return isIgnored(filePath);
    }

    /**
     * 检查文件是否被忽略（默认在项目内调用）
     * @param filePath 要检查的文件路径（绝对路径）
     * @return true 表示被忽略，false 表示未忽略
     */
    public static boolean isIgnored(String filePath) {
        String normalizedPath = FileUtils.normalizePath(filePath);
        String workingDir = FileUtils.normalizePath(System.getProperty("user.dir"));

        // 只在当前工程目录内才应用.aiignore规则
        if (!normalizedPath.startsWith(workingDir + "/") && !normalizedPath.equals(workingDir)) {
            return false; // 工程外的文件不应用.aiignore
        }

        // 获取相对路径用于匹配
        String relativePath = normalizedPath.substring(workingDir.length());
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }

        // 加载并检查.aiignore
        List<IgnoreRule> rules = loadRules(workingDir, RuleType.IGNORE);

        // 从后向前匹配（后定义的规则优先级高）
        boolean ignored = false;
        for (IgnoreRule rule : rules) {
            if (rule.matches(relativePath)) {
                ignored = !rule.negation; // 取反规则会取消忽略
            }
        }

        return ignored;
    }

    /**
     * 过滤文件列表，移除被忽略的文件
     */
    public static List<String> filterIgnoredFiles(List<String> filePaths, boolean isInsideProject) {
        if (!isInsideProject) {
            return filePaths; // 非项目内不过滤
        }
        List<String> filtered = new ArrayList<>();
        for (String path : filePaths) {
            if (!isIgnored(path)) {
                filtered.add(path);
            }
        }
        return filtered;
    }

    /**
     * 检查文件是否被写保护（禁止修改）
     * @param filePath 要检查的文件路径（绝对路径）
     * @param isInsideProject 是否在项目内
     * @return true 表示被写保护，false 表示未保护
     */
    public static boolean isWriteProtected(String filePath, boolean isInsideProject) {
        // 非项目内不遵守规则
        if (!isInsideProject) {
            return false;
        }
        return isProtectedByRules(filePath, RuleType.WRITE);
    }

    /**
     * 检查文件是否被读保护（禁止读取）
     * @param filePath 要检查的文件路径（绝对路径）
     * @param isInsideProject 是否在项目内
     * @return true 表示被读保护，false 表示未保护
     */
    public static boolean isReadProtected(String filePath, boolean isInsideProject) {
        // 非项目内不遵守规则
        if (!isInsideProject) {
            return false;
        }
        return isProtectedByRules(filePath, RuleType.READ);
    }

    /**
     * 通用保护检查方法
     */
    private static boolean isProtectedByRules(String filePath, RuleType ruleType) {
        String normalizedPath = FileUtils.normalizePath(filePath);
        String workingDir = FileUtils.normalizePath(System.getProperty("user.dir"));

        // 只在当前工程目录内才应用规则
        if (!normalizedPath.startsWith(workingDir + "/") && !normalizedPath.equals(workingDir)) {
            return false; // 工程外的文件不应用规则
        }

        // 获取相对路径用于匹配
        String relativePath = normalizedPath.substring(workingDir.length());
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }

        // 加载并检查规则
        List<IgnoreRule> rules = loadRules(workingDir, ruleType);

        // 从后向前匹配（后定义的规则优先级高）
        boolean protectedFlag = false;
        for (IgnoreRule rule : rules) {
            if (rule.matches(relativePath)) {
                protectedFlag = !rule.negation; // 取反规则会取消保护
            }
        }

        return protectedFlag;
    }

    /**
     * 获取规则文件名
     */
    private static String getRulesFileName(RuleType type) {
        switch (type) {
            case WRITE:
                return ".aiwriterules";
            case READ:
                return ".aireadrules";
            case IGNORE:
            default:
                return ".aiignore";
        }
    }

    /**
     * 加载规则
     */
    private static List<IgnoreRule> loadRules(String workingDir, RuleType ruleType) {
        String fileName = getRulesFileName(ruleType);
        Path rulesFile = Paths.get(workingDir, fileName);

        if (!Files.exists(rulesFile)) {
            return new ArrayList<>(); // 没有规则文件
        }

        // 获取对应的缓存
        List<IgnoreRule> targetRules;
        long targetLastModified;
        String targetLoadedPath;

        switch (ruleType) {
            case WRITE:
                targetRules = writeRules;
                targetLastModified = writeRulesLastModified;
                targetLoadedPath = writeRulesLoadedPath;
                break;
            case READ:
                targetRules = readRules;
                targetLastModified = readRulesLastModified;
                targetLoadedPath = readRulesLoadedPath;
                break;
            case IGNORE:
            default:
                targetRules = ignoreRules;
                targetLastModified = ignoreLastModified;
                targetLoadedPath = ignoreLoadedPath;
                break;
        }

        // 检查是否需要重新加载
        try {
            long currentModified = Files.getLastModifiedTime(rulesFile).toMillis();
            if (targetLoadedPath != null && targetLoadedPath.equals(workingDir)
                && currentModified == targetLastModified && !targetRules.isEmpty()) {
                return targetRules;
            }

            // 重新加载
            List<String> lines = Files.readAllLines(rulesFile);
            targetRules.clear();

            for (String line : lines) {
                IgnoreRule rule = parseRule(line);
                if (rule != null) {
                    targetRules.add(rule);
                }
            }

            // 更新缓存
            switch (ruleType) {
                case WRITE:
                    writeRulesLastModified = currentModified;
                    writeRulesLoadedPath = workingDir;
                    break;
                case READ:
                    readRulesLastModified = currentModified;
                    readRulesLoadedPath = workingDir;
                    break;
                case IGNORE:
                default:
                    ignoreLastModified = currentModified;
                    ignoreLoadedPath = workingDir;
                    break;
            }

        } catch (IOException e) {
            // 读取失败，返回空列表
            return new ArrayList<>();
        }

        return targetRules;
    }

    /**
     * 解析忽略规则
     */
    private static IgnoreRule parseRule(String line) {
        String trimmed = line.trim();

        // 空行或注释
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return null;
        }

        IgnoreRule rule = new IgnoreRule();

        // 检查是否是取反规则（以!开头）
        if (trimmed.startsWith("!")) {
            rule.negation = true;
            trimmed = trimmed.substring(1).trim();
        }

        // 检查是否以/开头（匹配项目根目录）
        if (trimmed.startsWith("/")) {
            rule.anchorToRoot = true;
            trimmed = trimmed.substring(1);
        }

        // 检查是否是目录规则（以/结尾）
        if (trimmed.endsWith("/")) {
            rule.directoryOnly = true;
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        rule.pattern = trimmed;

        // 转换glob模式为正则表达式
        try {
            rule.regex = globToRegex(trimmed);
        } catch (PatternSyntaxException e) {
            return null; // 无效模式
        }

        return rule;
    }

    /**
     * 将glob模式转换为正则表达式
     */
    private static Pattern globToRegex(String glob) {
        StringBuilder sb = new StringBuilder();
        sb.append("^");

        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            switch (c) {
                case '*':
                    if (i + 1 < glob.length() && glob.charAt(i + 1) == '*') {
                        // ** 匹配任意目录层级
                        sb.append(".*");
                        i++; // 跳过第二个*
                    } else {
                        // * 匹配任意字符（不包括/）
                        sb.append("[^/]*");
                    }
                    break;
                case '?':
                    sb.append("[^/]");
                    break;
                case '.':
                    sb.append("\\.");
                    break;
                case '/':
                    sb.append("/");
                    break;
                case '[':
                case ']':
                case '(':
                case ')':
                case '{':
                case '}':
                case '+':
                case '^':
                case '$':
                case '|':
                    sb.append("\\").append(c);
                    break;
                case '\\':
                    // 转义字符
                    if (i + 1 < glob.length()) {
                        sb.append(glob.charAt(++i));
                    }
                    break;
                default:
                    sb.append(c);
            }
        }

        sb.append("$");
        return Pattern.compile(sb.toString());
    }

    /**
     * 重置缓存（当需要强制重新加载时使用）
     */
    public static void resetCache() {
        ignoreRules.clear();
        ignoreLastModified = 0;
        ignoreLoadedPath = null;

        writeRules.clear();
        writeRulesLastModified = 0;
        writeRulesLoadedPath = null;

        readRules.clear();
        readRulesLastModified = 0;
        readRulesLoadedPath = null;
    }

    /**
     * 忽略规则内部类
     */
    private static class IgnoreRule {
        String pattern;
        Pattern regex;
        boolean negation = false;      // 是否取反（!）
        boolean anchorToRoot = false;  // 是否锚定到根目录（/开头）
        boolean directoryOnly = false; // 是否只匹配目录（/结尾）

        boolean matches(String path) {
            // 如果锚定到根，直接匹配
            if (anchorToRoot) {
                return regex.matcher(path).matches();
            }

            // 否则匹配路径的任何部分
            // 例如 pattern "test" 应该匹配 "test", "dir/test", "dir/test/file"
            if (regex.matcher(path).matches()) {
                return true;
            }

            // 检查路径的任意组成部分
            String[] parts = path.split("/");
            for (String part : parts) {
                // 对目录部分移除末尾的/
                String checkPart = part;
                if (checkPart.endsWith("/")) {
                    checkPart = checkPart.substring(0, checkPart.length() - 1);
                }
                if (regex.matcher(checkPart).matches()) {
                    return true;
                }
            }

            return false;
        }
    }
}

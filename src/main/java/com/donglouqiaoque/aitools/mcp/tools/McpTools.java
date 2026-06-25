package com.donglouqiaoque.aitools.mcp.tools;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.jar.*;
import java.util.stream.Stream;

/**
 * MCP工具类
 * 提供版本信息、构建时间等查询功能，以及MCP数据管理功能
 */
public class McpTools {

    /**
     * 获取MCP服务器版本和构建信息
     * @return 版本信息字符串
     */
    public static String getMcpVersion() {
        StringBuilder sb = new StringBuilder();
        sb.append("MCP服务器版本信息\n");
        sb.append("===================\n\n");

        try {
            // 获取当前类的JAR文件路径
            String classPath = McpTools.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            // URL解码处理中文路径
            classPath = java.net.URLDecoder.decode(classPath, "UTF-8");
            File jarFile = new File(classPath);

            sb.append("JAR文件路径: ").append(jarFile.getAbsolutePath()).append("\n");
            sb.append("JAR文件存在: ").append(jarFile.exists() ? "✓ 是" : "✗ 否").append("\n\n");

            if (jarFile.exists()) {
                // 获取JAR文件最后修改时间
                long lastModified = jarFile.lastModified();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String buildTime = sdf.format(new Date(lastModified));
                sb.append("JAR文件修改时间: ").append(buildTime).append("\n");
                sb.append("JAR文件大小: ").append(jarFile.length()).append(" bytes (").append(String.format("%.2f", jarFile.length() / 1024.0)).append(" KB)\n\n");

                // 读取MANIFEST.MF
                try (JarFile jar = new JarFile(jarFile)) {
                    Manifest manifest = jar.getManifest();
                    if (manifest != null) {
                        Attributes mainAttrs = manifest.getMainAttributes();

                        sb.append("MANIFEST信息:\n");
                        sb.append("---------------\n");

                        String version = mainAttrs.getValue("Implementation-Version");
                        if (version != null) {
                            sb.append("版本: ").append(version).append("\n");
                        } else {
                            sb.append("版本: (未在MANIFEST中指定)\n");
                        }

                        String title = mainAttrs.getValue("Implementation-Title");
                        if (title != null) {
                            sb.append("名称: ").append(title).append("\n");
                        }

                        String vendor = mainAttrs.getValue("Implementation-Vendor");
                        if (vendor != null) {
                            sb.append("供应商: ").append(vendor).append("\n");
                        }

                        String buildJdk = mainAttrs.getValue("Build-Jdk");
                        if (buildJdk != null) {
                            sb.append("构建JDK: ").append(buildJdk).append("\n");
                        }

                        String createdBy = mainAttrs.getValue("Created-By");
                        if (createdBy != null) {
                            sb.append("创建者: ").append(createdBy).append("\n");
                        }

                        String buildTimeAttr = mainAttrs.getValue("Build-Time");
                        if (buildTimeAttr != null) {
                            try {
                                // 将UTC时间转换为本地时间
                                java.time.LocalDateTime utcTime = java.time.LocalDateTime.parse(buildTimeAttr,
                                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                                java.time.ZonedDateTime utcZoned = utcTime.atZone(java.time.ZoneId.of("UTC"));
                                java.time.ZonedDateTime localZoned = utcZoned.withZoneSameInstant(java.time.ZoneId.systemDefault());
                                String localTimeStr = localZoned.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                                sb.append("构建时间: ").append(localTimeStr).append(" (UTC: ").append(buildTimeAttr).append(")\n");
                            } catch (Exception e) {
                                sb.append("构建时间: ").append(buildTimeAttr).append("\n");
                            }
                        }
                    } else {
                        sb.append("MANIFEST信息: (未找到)\n");
                    }
                }
            }

            // 获取Java运行时信息
            sb.append("\n运行时信息:\n");
            sb.append("---------------\n");
            sb.append("Java版本: ").append(System.getProperty("java.version")).append("\n");
            sb.append("Java厂商: ").append(System.getProperty("java.vendor")).append("\n");
            sb.append("Java路径: ").append(System.getProperty("java.home")).append("\n");
            sb.append("操作系统: ").append(System.getProperty("os.name")).append(" ").append(System.getProperty("os.version")).append("\n");
            sb.append("工作目录: ").append(System.getProperty("user.dir")).append("\n");

        } catch (Exception e) {
            sb.append("\n✗ 获取版本信息时出错: ").append(e.getMessage()).append("\n");
            e.printStackTrace();
        }

        return sb.toString();
    }

    /**
     * 清除.dlqq数据目录
     * 删除整个.dlqq目录及其所有内容
     * @return 删除结果描述（始终返回成功，即使部分文件被占用）
     */
    public static String clearDlqqData() {
        Path dlqqPath = Paths.get(".dlqq");

        if (!Files.exists(dlqqPath)) {
            return ".dlqq目录不存在，无需清除";
        }

        if (!Files.isDirectory(dlqqPath)) {
            return "✗ 清除失败：.dlqq路径不是目录";
        }

        StringBuilder result = new StringBuilder();
        java.util.List<String> deletedItems = new java.util.ArrayList<>();
        java.util.List<String> busyFiles = new java.util.ArrayList<>();

        // 先尝试删除日志目录（因为这是最可能被占用的）
        Path logPath = dlqqPath.resolve("log");
        if (Files.exists(logPath)) {
            // 尝试删除logger目录下的日志文件
            Path loggerPath = logPath.resolve("logger");
            if (Files.exists(loggerPath)) {
                try (Stream<Path> stream = Files.list(loggerPath)) {
                    for (Path file : stream.toList()) {
                        if (Files.isRegularFile(file)) {
                            try {
                                Files.delete(file);
                                deletedItems.add("log/logger/" + file.getFileName());
                            } catch (IOException e) {
                                busyFiles.add("log/logger/" + file.getFileName());
                            }
                        }
                    }
                } catch (IOException e) {
                    // 忽略读取目录失败
                }
                // 尝试删除logger目录
                try {
                    Files.delete(loggerPath);
                    deletedItems.add("log/logger");
                } catch (IOException e) {
                    // 目录可能不为空，继续
                }
            }

            // 尝试删除stderr目录下的日志文件
            Path stderrPath = logPath.resolve("stderr");
            if (Files.exists(stderrPath)) {
                try (Stream<Path> stream = Files.list(stderrPath)) {
                    for (Path file : stream.toList()) {
                        if (Files.isRegularFile(file)) {
                            try {
                                Files.delete(file);
                                deletedItems.add("log/stderr/" + file.getFileName());
                            } catch (IOException e) {
                                busyFiles.add("log/stderr/" + file.getFileName());
                            }
                        }
                    }
                } catch (IOException e) {
                    // 忽略读取目录失败
                }
                // 尝试删除stderr目录
                try {
                    Files.delete(stderrPath);
                    deletedItems.add("log/stderr");
                } catch (IOException e) {
                    // 目录可能不为空，继续
                }
            }

            // 尝试删除log目录
            try {
                Files.delete(logPath);
                deletedItems.add("log");
            } catch (IOException e) {
                // 目录可能不为空，继续
            }
        }

        // 尝试删除python目录
        Path pythonPath = dlqqPath.resolve("python");
        if (Files.exists(pythonPath)) {
            java.util.List<String> pythonDeleted = new java.util.ArrayList<>();
            java.util.List<String> pythonBusy = new java.util.ArrayList<>();
            deleteDirectoryRecursiveCollect(pythonPath, ".dlqq/python", pythonDeleted, pythonBusy);
            deletedItems.addAll(pythonDeleted);
            busyFiles.addAll(pythonBusy);
        }

        // 最后删除.dlqq目录本身
        try {
            Files.delete(dlqqPath);
            deletedItems.add(".dlqq");
        } catch (IOException e) {
            // 如果目录不为空，继续
        }

        // 构建返回结果
        result.append("✓ .dlqq数据目录清除完成\n");
        result.append("===================\n\n");

        if (!deletedItems.isEmpty()) {
            result.append("已删除的项目（").append(deletedItems.size()).append("个）：\n");
            for (String item : deletedItems) {
                result.append("  ✓ ").append(item).append("\n");
            }
            result.append("\n");
        }

        if (!busyFiles.isEmpty()) {
            result.append("以下文件当前被占用，无法删除（").append(busyFiles.size()).append("个）：\n");
            for (String file : busyFiles) {
                result.append("  ○ ").append(file).append("\n");
            }
            result.append("\n");
            result.append("注意：部分文件无法删除是正常现象，因为这些文件正在被MCP服务器使用。\n");
            result.append("已删除的项目不会受到影响，被占用的文件将在MCP服务器重启后可以被清除。\n");
        } else if (!deletedItems.isEmpty()) {
            result.append("所有项目已成功删除。\n");
        }

        return result.toString();
    }

    /**
     * 清除缓存日志文件
     * 只删除.dlqq/log目录下的日志文件，保留目录结构
     * @return 删除结果描述（始终返回成功，即使部分文件被占用）
     */
    public static String clearCache() {
        Path logPath = Paths.get(".dlqq/log");

        if (!Files.exists(logPath)) {
            return "日志目录不存在，无需清除";
        }

        if (!Files.isDirectory(logPath)) {
            return "✗ 清除失败：日志路径不是目录";
        }

        int deletedCount = 0;
        java.util.List<String> busyFiles = new java.util.ArrayList<>();

        // 遍历log目录下的所有子目录
        try (Stream<Path> subDirs = Files.list(logPath)) {
            for (Path subDir : subDirs.toList()) {
                if (Files.isDirectory(subDir)) {
                    // 遍历子目录中的日志文件
                    try (Stream<Path> files = Files.list(subDir)) {
                        for (Path file : files.toList()) {
                            if (Files.isRegularFile(file)) {
                                try {
                                    Files.delete(file);
                                    deletedCount++;
                                } catch (IOException e) {
                                    busyFiles.add(subDir.getFileName() + "/" + file.getFileName());
                                }
                            }
                        }
                    } catch (IOException e) {
                        // 忽略读取子目录失败
                    }
                }
            }
        } catch (IOException e) {
            return "✗ 清除失败：无法读取日志目录 - " + e.getMessage();
        }

        // 构建返回结果
        StringBuilder result = new StringBuilder();
        result.append("✓ 日志缓存清除完成\n");
        result.append("===================\n\n");

        if (deletedCount > 0) {
            result.append("已删除的日志文件：").append(deletedCount).append("个\n\n");
        }

        if (!busyFiles.isEmpty()) {
            result.append("以下日志文件当前被占用，无法删除（").append(busyFiles.size()).append("个）：\n");
            for (String file : busyFiles) {
                result.append("  ○ ").append(file).append("\n");
            }
            result.append("\n");
            result.append("注意：部分文件无法删除是正常现象，因为这些日志文件正在被MCP服务器使用。\n");
            result.append("已删除的日志文件不会受到影响，被占用的文件将在MCP服务器重启后可以被清除。\n");
        } else if (deletedCount == 0) {
            result.append("没有找到需要清除的日志文件。\n");
        } else {
            result.append("所有日志文件已成功删除。\n");
        }

        return result.toString();
    }

    /**
     * 递归删除目录并收集结果
     */
    private static void deleteDirectoryRecursiveCollect(Path path, String relativePath,
                                                        java.util.List<String> deletedItems,
                                                        java.util.List<String> busyFiles) {
        if (Files.isDirectory(path)) {
            try (Stream<Path> stream = Files.list(path)) {
                for (Path child : stream.toList()) {
                    String childRelative = relativePath + "/" + child.getFileName();
                    deleteDirectoryRecursiveCollect(child, childRelative, deletedItems, busyFiles);
                }
            } catch (IOException e) {
                // 忽略读取目录失败
            }
        }
        try {
            Files.delete(path);
            if (Files.isRegularFile(path)) {
                deletedItems.add(relativePath);
            } else {
                deletedItems.add(relativePath);
            }
        } catch (IOException e) {
            if (Files.exists(path)) {
                if (Files.isRegularFile(path)) {
                    busyFiles.add(relativePath);
                }
            }
        }
    }
}

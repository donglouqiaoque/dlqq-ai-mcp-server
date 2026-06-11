package com.donglouqiaoque.aitools.mcp.tools;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.jar.*;

/**
 * 信息工具类
 * 提供版本信息、构建时间等查询功能
 */
public class InfoTools {

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
            String classPath = InfoTools.class.getProtectionDomain().getCodeSource().getLocation().getPath();
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
}

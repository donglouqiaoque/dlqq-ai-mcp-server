package com.donglouqiaoque.aitools.mcp.tools;

import com.donglouqiaoque.aitools.utils.FileUtils;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 压缩/解压缩工具 - 支持ZIP格式及密码保护
 */
public class ZipTools {

    /**
     * 支持的压缩格式
     */
    public static final List<String> SUPPORTED_FORMATS = Arrays.asList("zip");

    /**
     * 压缩文件或目录
     * @param sourcePaths 源文件/目录路径列表（支持多个文件/目录）
     * @param outputPath 输出压缩文件路径（可选，默认为源目录下的压缩包）
     * @param format 压缩格式（可选，默认为zip）
     * @param password 压缩密码（可选，为空表示不加密）
     * @param compressionLevel 压缩级别（1-9，默认为5）
     * @return 包含操作结果的信息
     */
    public static Map<String, Object> zipFiles(List<String> sourcePaths, String outputPath, 
                                                String format, String password, Integer compressionLevel) throws IOException {
        if (sourcePaths == null || sourcePaths.isEmpty()) {
            throw new IOException("源文件路径不能为空");
        }

        // 验证并规范化源路径
        List<File> sourceFiles = new ArrayList<>();
        for (String sourcePath : sourcePaths) {
            String normalizedPath = FileUtils.normalizePath(sourcePath);
            File sourceFile = new File(normalizedPath);
            if (!sourceFile.exists()) {
                throw new IOException("源文件/目录不存在: " + sourcePath);
            }
            sourceFiles.add(sourceFile);
        }

        // 确定压缩格式
        if (format == null || format.trim().isEmpty()) {
            format = "zip";
        }
        format = format.toLowerCase().trim();
        if (!SUPPORTED_FORMATS.contains(format)) {
            throw new IOException("不支持的压缩格式: " + format + "，支持的格式: " + String.join(", ", SUPPORTED_FORMATS));
        }

        // 确定输出路径
        Path outputFilePath;
        if (outputPath != null && !outputPath.trim().isEmpty()) {
            String normalizedOutput = FileUtils.normalizePath(outputPath);
            outputFilePath = Paths.get(normalizedOutput);
        } else {
            // 默认输出到第一个源文件所在目录
            File firstSource = sourceFiles.get(0);
            String baseName = firstSource.getName();
            if (firstSource.isDirectory()) {
                baseName = firstSource.getName();
            } else {
                baseName = firstSource.getName();
                int lastDot = baseName.lastIndexOf('.');
                if (lastDot > 0) {
                    baseName = baseName.substring(0, lastDot);
                }
            }
            outputFilePath = Paths.get(firstSource.getParent(), baseName + ".zip");
        }

        // 确保输出目录存在
        Files.createDirectories(outputFilePath.getParent());

        // 如果输出文件已存在，删除它
        if (Files.exists(outputFilePath)) {
            Files.delete(outputFilePath);
        }

        // 设置压缩参数
        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setCompressionMethod(CompressionMethod.DEFLATE);
        
        // 设置压缩级别
        int level = (compressionLevel != null && compressionLevel >= 1 && compressionLevel <= 9) ? compressionLevel : 5;
        CompressionLevel[] levels = {
            CompressionLevel.FASTEST, CompressionLevel.FASTEST, CompressionLevel.FAST,
            CompressionLevel.FAST, CompressionLevel.NORMAL, CompressionLevel.NORMAL,
            CompressionLevel.HIGHER, CompressionLevel.HIGHER, CompressionLevel.MAXIMUM
        };
        zipParameters.setCompressionLevel(levels[level - 1]);

        // 设置加密
        boolean usePassword = password != null && !password.trim().isEmpty();
        if (usePassword) {
            zipParameters.setEncryptFiles(true);
            zipParameters.setEncryptionMethod(EncryptionMethod.AES);
            zipParameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
        }

        // 创建ZIP文件
        ZipFile zipFile = usePassword ? new ZipFile(outputFilePath.toFile(), password.toCharArray())
                                       : new ZipFile(outputFilePath.toFile());

        // 添加文件到压缩包
        for (File sourceFile : sourceFiles) {
            if (sourceFile.isDirectory()) {
                zipFile.addFolder(sourceFile, zipParameters);
            } else {
                zipFile.addFile(sourceFile, zipParameters);
            }
        }

        // 获取压缩结果信息
        long originalSize = calculateTotalSize(sourceFiles);
        long compressedSize = Files.size(outputFilePath);
        double compressionRatio = originalSize > 0 ? (1.0 - (double) compressedSize / originalSize) * 100 : 0;

        return Map.of(
            "outputPath", outputFilePath.toString(),
            "format", format,
            "originalSize", originalSize,
            "compressedSize", compressedSize,
            "compressionRatio", String.format("%.2f%%", compressionRatio),
            "fileCount", sourceFiles.size(),
            "passwordProtected", usePassword
        );
    }

    /**
     * 解压缩文件
     * @param zipPath ZIP文件路径
     * @param outputDir 输出目录（可选，默认为ZIP文件所在目录）
     * @param password 解压密码（可选，如果ZIP有密码则需要提供）
     * @return 包含操作结果的信息
     */
    public static Map<String, Object> unzipFile(String zipPath, String outputDir, String password) throws IOException {
        if (zipPath == null || zipPath.trim().isEmpty()) {
            throw new IOException("ZIP文件路径不能为空");
        }

        // 规范化路径
        String normalizedZipPath = FileUtils.normalizePath(zipPath);
        Path zipFilePath = Paths.get(normalizedZipPath);

        if (!Files.exists(zipFilePath)) {
            throw new IOException("ZIP文件不存在: " + zipPath);
        }

        if (!Files.isRegularFile(zipFilePath)) {
            throw new IOException("路径不是文件: " + zipPath);
        }

        // 检查文件扩展名
        String fileName = zipFilePath.getFileName().toString().toLowerCase();
        if (!fileName.endsWith(".zip")) {
            throw new IOException("不支持的文件格式，仅支持ZIP格式");
        }

        // 确定输出目录
        Path outputPath;
        if (outputDir != null && !outputDir.trim().isEmpty()) {
            String normalizedOutput = FileUtils.normalizePath(outputDir);
            outputPath = Paths.get(normalizedOutput);
        } else {
            // 默认输出到ZIP文件所在目录下的同名文件夹
            String baseName = fileName;
            if (baseName.endsWith(".zip")) {
                baseName = baseName.substring(0, baseName.length() - 4);
            }
            outputPath = zipFilePath.getParent().resolve(baseName);
        }

        // 确保输出目录存在
        Files.createDirectories(outputPath);

        // 创建ZIP文件对象
        boolean usePassword = password != null && !password.trim().isEmpty();
        ZipFile zipFile = usePassword ? new ZipFile(zipFilePath.toFile(), password.toCharArray())
                                       : new ZipFile(zipFilePath.toFile());

        // 检查是否是有效的ZIP文件
        if (!zipFile.isValidZipFile()) {
            throw new IOException("无效的ZIP文件: " + zipPath);
        }

        // 检查是否需要密码
        if (zipFile.isEncrypted() && !usePassword) {
            throw new IOException("ZIP文件需要密码，请提供password参数");
        }

        // 获取文件列表信息
        List<String> fileList = zipFile.getFileHeaders().stream()
            .map(header -> header.getFileName())
            .collect(Collectors.toList());

        // 解压
        zipFile.extractAll(outputPath.toString());

        // 计算解压后的大小
        long extractedSize = calculateDirectorySize(outputPath.toFile());

        return Map.of(
            "zipPath", zipPath,
            "outputDir", outputPath.toString(),
            "fileCount", fileList.size(),
            "extractedSize", extractedSize,
            "files", fileList.subList(0, Math.min(fileList.size(), 20)), // 最多显示20个文件
            "hasMoreFiles", fileList.size() > 20
        );
    }

    /**
     * 列出ZIP文件内容
     * @param zipPath ZIP文件路径
     * @param password 密码（可选）
     * @return ZIP文件内容列表
     */
    public static Map<String, Object> listZipContents(String zipPath, String password) throws IOException {
        if (zipPath == null || zipPath.trim().isEmpty()) {
            throw new IOException("ZIP文件路径不能为空");
        }

        String normalizedZipPath = FileUtils.normalizePath(zipPath);
        Path zipFilePath = Paths.get(normalizedZipPath);

        if (!Files.exists(zipFilePath)) {
            throw new IOException("ZIP文件不存在: " + zipPath);
        }

        boolean usePassword = password != null && !password.trim().isEmpty();
        ZipFile zipFile = usePassword ? new ZipFile(zipFilePath.toFile(), password.toCharArray())
                                       : new ZipFile(zipFilePath.toFile());

        if (!zipFile.isValidZipFile()) {
            throw new IOException("无效的ZIP文件: " + zipPath);
        }

        List<Map<String, Object>> fileList = zipFile.getFileHeaders().stream()
            .map(header -> Map.<String, Object>of(
                "name", header.getFileName(),
                "size", header.getUncompressedSize(),
                "compressedSize", header.getCompressedSize(),
                "isDirectory", header.isDirectory(),
                "crc", header.getCrc(),
                "lastModified", header.getLastModifiedTime()
            ))
            .collect(Collectors.toList());

        long totalSize = fileList.stream().mapToLong(f -> (Long) f.get("size")).sum();
        long totalCompressed = fileList.stream().mapToLong(f -> (Long) f.get("compressedSize")).sum();

        return Map.of(
            "zipPath", zipPath,
            "fileCount", fileList.size(),
            "totalSize", totalSize,
            "totalCompressedSize", totalCompressed,
            "encrypted", zipFile.isEncrypted(),
            "files", fileList
        );
    }

    /**
     * 计算多个文件/目录的总大小
     */
    private static long calculateTotalSize(List<File> files) {
        long total = 0;
        for (File file : files) {
            total += calculateDirectorySize(file);
        }
        return total;
    }

    /**
     * 计算文件或目录的大小
     */
    private static long calculateDirectorySize(File file) {
        if (!file.exists()) {
            return 0;
        }
        if (file.isFile()) {
            return file.length();
        }
        long size = 0;
        File[] files = file.listFiles();
        if (files != null) {
            for (File f : files) {
                size += calculateDirectorySize(f);
            }
        }
        return size;
    }

    /**
     * 检查是否是支持的压缩格式
     */
    public static boolean isSupportedFormat(String format) {
        if (format == null) {
            return false;
        }
        return SUPPORTED_FORMATS.contains(format.toLowerCase().trim());
    }

    /**
     * 获取支持的格式列表
     */
    public static String getSupportedFormats() {
        return String.join(", ", SUPPORTED_FORMATS);
    }
}

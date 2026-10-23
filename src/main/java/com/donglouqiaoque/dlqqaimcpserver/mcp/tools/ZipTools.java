package com.donglouqiaoque.dlqqaimcpserver.mcp.tools;

import com.donglouqiaoque.dlqqaimcpserver.utils.FileUtils;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 压缩/解压缩工具 - 支持ZIP(带密码)、7Z、TAR、GZ、BZ2、XZ、RAR、CPIO、AR等格式
 */
public class ZipTools {

    /**
     * 支持的压缩格式（解压）
     */
    public static final List<String> SUPPORTED_FORMATS = Arrays.asList(
        "zip", "jar", "war", "ear", "7z", "tar", "tar.gz", "tgz", "tar.bz2", "tbz2",
        "tar.xz", "txz", "gz", "bz2", "xz", "rar", "cpio", "ar"
    );

    /**
     * 压缩文件或目录（目前仅支持zip格式）
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
        if (!"zip".equals(format)) {
            throw new IOException("压缩功能目前仅支持 zip 格式（解压支持 " + getSupportedFormats() + "）");
        }

        // 确定输出路径
        Path outputFilePath;
        if (outputPath != null && !outputPath.trim().isEmpty()) {
            String normalizedOutput = FileUtils.normalizePath(outputPath);
            outputFilePath = Paths.get(normalizedOutput);
        } else {
            // 默认输出到 .dlqq/cache 目录
            File firstSource = sourceFiles.get(0);
            String baseName = firstSource.getName();
            if (!firstSource.isDirectory()) {
                int lastDot = baseName.lastIndexOf('.');
                if (lastDot > 0) {
                    baseName = baseName.substring(0, lastDot);
                }
            }
            Path cacheDir = McpTools.ensureCacheDir();
            outputFilePath = cacheDir.resolve(baseName + ".zip");
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
     * 解压缩文件（支持多种格式：zip/7z/tar/gz/bz2/xz/rar等）
     * @param zipPath 压缩文件路径
     * @param outputDir 输出目录（可选，默认为压缩文件所在目录下的同名文件夹）
     * @param password 解压密码（可选，仅zip格式支持）
     * @return 包含操作结果的信息
     */
    public static Map<String, Object> unzipFile(String zipPath, String outputDir, String password) throws IOException {
        if (zipPath == null || zipPath.trim().isEmpty()) {
            throw new IOException("压缩文件路径不能为空");
        }

        // 规范化路径
        String normalizedZipPath = FileUtils.normalizePath(zipPath);
        Path zipFilePath = Paths.get(normalizedZipPath);

        if (!Files.exists(zipFilePath)) {
            throw new IOException("压缩文件不存在: " + zipPath);
        }

        if (!Files.isRegularFile(zipFilePath)) {
            throw new IOException("路径不是文件: " + zipPath);
        }

        // 检查文件扩展名
        String fileName = zipFilePath.getFileName().toString().toLowerCase();
        String ext = getArchiveExt(fileName);
        if (ext == null) {
            throw new IOException("不支持的文件格式: " + fileName + "，支持的格式: " + getSupportedFormats());
        }

        // 确定输出目录
        Path outputPath;
        if (outputDir != null && !outputDir.trim().isEmpty()) {
            String normalizedOutput = FileUtils.normalizePath(outputDir);
            outputPath = Paths.get(normalizedOutput);
        } else {
            // 默认输出到 .dlqq/cache 目录下的同名文件夹
            String baseName = stripArchiveExt(fileName);
            Path cacheDir = McpTools.ensureCacheDir();
            outputPath = cacheDir.resolve(baseName);
        }

        // 确保输出目录存在
        Files.createDirectories(outputPath);

        List<String> fileList;

        if ("zip".equals(ext)) {
            // zip格式使用zip4j（支持密码）
            fileList = extractZip(zipFilePath, outputPath, password);
        } else {
            // 其他格式使用 commons-compress
            if (password != null && !password.trim().isEmpty()) {
                throw new IOException("当前格式 (" + ext + ") 不支持密码解压，密码仅适用于ZIP格式");
            }
            fileList = extractWithCommonsCompress(zipFilePath, outputPath, ext);
        }

        // 计算解压后的大小
        long extractedSize = calculateDirectorySize(outputPath.toFile());

        return Map.of(
            "zipPath", zipPath,
            "outputDir", outputPath.toString(),
            "format", ext,
            "fileCount", fileList.size(),
            "extractedSize", extractedSize,
            "files", fileList.subList(0, Math.min(fileList.size(), 20)), // 最多显示20个文件
            "hasMoreFiles", fileList.size() > 20
        );
    }

    /**
     * 列出压缩文件内容（支持多种格式）
     * @param zipPath 压缩文件路径
     * @param password 密码（可选，仅zip格式支持）
     * @return 文件内容列表
     */
    public static Map<String, Object> listZipContents(String zipPath, String password) throws IOException {
        if (zipPath == null || zipPath.trim().isEmpty()) {
            throw new IOException("压缩文件路径不能为空");
        }

        String normalizedZipPath = FileUtils.normalizePath(zipPath);
        Path zipFilePath = Paths.get(normalizedZipPath);

        if (!Files.exists(zipFilePath)) {
            throw new IOException("压缩文件不存在: " + zipPath);
        }

        String fileName = zipFilePath.getFileName().toString().toLowerCase();
        String ext = getArchiveExt(fileName);
        if (ext == null) {
            throw new IOException("不支持的文件格式: " + fileName + "，支持的格式: " + getSupportedFormats());
        }

        if ("zip".equals(ext)) {
            // zip格式使用zip4j（支持密码）
            boolean usePassword = password != null && !password.trim().isEmpty();
            ZipFile zipFile = usePassword ? new ZipFile(zipFilePath.toFile(), password.toCharArray())
                                           : new ZipFile(zipFilePath.toFile());

            if (!zipFile.isValidZipFile()) {
                throw new IOException("无效的ZIP文件: " + zipPath);
            }

            List<Map<String, Object>> fileList = zipFile.getFileHeaders().stream()
                .map(header -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", header.getFileName());
                    m.put("size", header.getUncompressedSize());
                    m.put("compressedSize", header.getCompressedSize());
                    m.put("isDirectory", header.isDirectory());
                    m.put("crc", header.getCrc());
                    m.put("lastModified", header.getLastModifiedTime());
                    return m;
                })
                .collect(Collectors.toList());

            long totalSize = fileList.stream().mapToLong(f -> ((Number) f.get("size")).longValue()).sum();
            long totalCompressed = fileList.stream().mapToLong(f -> ((Number) f.get("compressedSize")).longValue()).sum();

            return Map.of(
                "zipPath", zipPath,
                "format", ext,
                "fileCount", fileList.size(),
                "totalSize", totalSize,
                "totalCompressedSize", totalCompressed,
                "encrypted", zipFile.isEncrypted(),
                "files", fileList
            );
        } else {
            if (password != null && !password.trim().isEmpty()) {
                throw new IOException("当前格式 (" + ext + ") 不支持密码，密码仅适用于ZIP格式");
            }
            return listWithCommonsCompress(zipFilePath, ext);
        }
    }

    /**
     * 使用zip4j解压zip文件
     */
    private static List<String> extractZip(Path zipFilePath, Path outputPath, String password) throws IOException {
        boolean usePassword = password != null && !password.trim().isEmpty();
        ZipFile zipFile = usePassword ? new ZipFile(zipFilePath.toFile(), password.toCharArray())
                                       : new ZipFile(zipFilePath.toFile());

        // 检查是否是有效的ZIP文件
        if (!zipFile.isValidZipFile()) {
            throw new IOException("无效的ZIP文件: " + zipFilePath);
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

        return fileList;
    }

    /**
     * 使用 commons-compress 解压（支持 7z/tar/gz/bz2/xz/rar/cpio/ar 等）
     * 按扩展名明确分发，避免自动检测流的 mark/reset 问题
     */
    private static List<String> extractWithCommonsCompress(Path archivePath, Path outputPath, String ext) throws IOException {
        List<String> fileList = new ArrayList<>();
        try (InputStream rawIn = new BufferedInputStream(Files.newInputStream(archivePath))) {
            switch (ext) {
                case "gz":
                case "bz2":
                case "xz": {
                    // 单文件压缩（无归档结构）
                    InputStream comp = createCompressorStream(rawIn, ext);
                    String outName = stripLastExtension(archivePath.getFileName().toString());
                    Path target = outputPath.resolve(outName).normalize();
                    try (OutputStream out = Files.newOutputStream(target)) {
                        IOUtils.copy(comp, out);
                    }
                    fileList.add(outName);
                    break;
                }
                case "tar.gz":
                case "tgz":
                case "tar.bz2":
                case "tbz2":
                case "tbz":
                case "tar.xz":
                case "txz": {
                    // tar + 压缩
                    InputStream comp = createCompressorStream(rawIn, ext);
                    extractTarEntries(comp, outputPath, fileList);
                    break;
                }
                case "tar": {
                    extractTarEntries(rawIn, outputPath, fileList);
                    break;
                }
                case "7z": {
                    // 7z不支持流式读取，需使用 SevenZFile 随机访问文件
                    extractSevenZ(archivePath, outputPath, fileList);
                    break;
                }
                default: {
                    // 7z/rar/cpio/ar等：自动检测归档格式
                    try (ArchiveInputStream ais = new ArchiveStreamFactory().createArchiveInputStream(
                            new BufferedInputStream(rawIn, 8192))) {
                        extractArchiveEntries(ais, outputPath, fileList);
                    } catch (ArchiveException e) {
                        throw new IOException("无法识别的归档格式: " + archivePath.getFileName() + "（" + e.getMessage() + "）");
                    }
                }
            }
        }
        return fileList;
    }

    /**
     * 创建解压缩流（gzip/bzip2/xz）
     */
    private static InputStream createCompressorStream(InputStream in, String ext) throws IOException {
        switch (ext) {
            case "gz":
            case "tar.gz":
            case "tgz":
                return new GzipCompressorInputStream(in);
            case "bz2":
            case "tar.bz2":
            case "tbz2":
            case "tbz":
                return new BZip2CompressorInputStream(in);
            case "xz":
            case "tar.xz":
            case "txz":
                return new XZCompressorInputStream(in);
            default:
                return in;
        }
    }

    /**
     * 解压7z文件（SevenZFile 需要随机访问文件，不支持流式）
     */
    private static void extractSevenZ(Path archivePath, Path outputPath, List<String> fileList) throws IOException {
        try (SevenZFile sevenZFile = new SevenZFile(archivePath.toFile())) {
            SevenZArchiveEntry entry;
            while ((entry = sevenZFile.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                Path target = outputPath.resolve(entry.getName()).normalize();
                if (!target.startsWith(outputPath)) {
                    throw new IOException("检测到非法路径（路径穿越）: " + entry.getName());
                }
                Files.createDirectories(target.getParent());
                try (OutputStream out = Files.newOutputStream(target)) {
                    // SevenZFile 不是 InputStream，需手动循环读取
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = sevenZFile.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                }
                fileList.add(entry.getName());
            }
        }
    }

    /**
     * 解压tar条目
     */
    private static void extractTarEntries(InputStream in, Path outputPath, List<String> fileList) throws IOException {
        try (TarArchiveInputStream tis = new TarArchiveInputStream(in)) {
            TarArchiveEntry entry;
            while ((entry = tis.getNextTarEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                Path target = outputPath.resolve(entry.getName()).normalize();
                if (!target.startsWith(outputPath)) {
                    throw new IOException("检测到非法路径（路径穿越）: " + entry.getName());
                }
                Files.createDirectories(target.getParent());
                try (OutputStream out = Files.newOutputStream(target)) {
                    IOUtils.copy(tis, out);
                }
                fileList.add(entry.getName());
            }
        }
    }

    /**
     * 解压通用归档条目（7z/rar/cpio/ar/zip等）
     */
    private static void extractArchiveEntries(ArchiveInputStream ais, Path outputPath, List<String> fileList) throws IOException {
        ArchiveEntry entry;
        while ((entry = ais.getNextEntry()) != null) {
            if (entry.isDirectory()) {
                continue;
            }
            Path target = outputPath.resolve(entry.getName()).normalize();
            if (!target.startsWith(outputPath)) {
                throw new IOException("检测到非法路径（路径穿越）: " + entry.getName());
            }
            Files.createDirectories(target.getParent());
            try (OutputStream out = Files.newOutputStream(target)) {
                IOUtils.copy(ais, out);
            }
            fileList.add(entry.getName());
        }
    }

    /**
     * 使用 commons-compress 列出压缩文件内容
     */
    private static Map<String, Object> listWithCommonsCompress(Path archivePath, String ext) throws IOException {
        List<Map<String, Object>> fileList = new ArrayList<>();
        long totalSize = 0;
        long totalCompressed = 0;

        try (InputStream rawIn = new BufferedInputStream(Files.newInputStream(archivePath))) {
            switch (ext) {
                case "gz":
                case "bz2":
                case "xz": {
                    // 单文件压缩：无法列出内部条目，显示解压后的文件名
                    Map<String, Object> m = new LinkedHashMap<>();
                    String outName = stripLastExtension(archivePath.getFileName().toString());
                    m.put("name", outName);
                    m.put("size", -1L);
                    m.put("compressedSize", Files.size(archivePath));
                    m.put("isDirectory", false);
                    m.put("crc", 0L);
                    m.put("lastModified", Files.getLastModifiedTime(archivePath).toMillis());
                    fileList.add(m);
                    break;
                }
                case "tar.gz":
                case "tgz":
                case "tar.bz2":
                case "tbz2":
                case "tbz":
                case "tar.xz":
                case "txz": {
                    InputStream comp = createCompressorStream(rawIn, ext);
                    totalSize += listTarEntries(comp, fileList);
                    break;
                }
                case "tar": {
                    totalSize += listTarEntries(rawIn, fileList);
                    break;
                }
                case "7z": {
                    // 7z不支持流式读取，使用 SevenZFile 列出
                    try (SevenZFile sevenZFile = new SevenZFile(archivePath.toFile())) {
                        SevenZArchiveEntry entry;
                        while ((entry = sevenZFile.getNextEntry()) != null) {
                            Map<String, Object> m = new LinkedHashMap<>();
                            m.put("name", entry.getName());
                            m.put("size", entry.getSize());
                            m.put("compressedSize", entry.getSize());
                            m.put("isDirectory", entry.isDirectory());
                            m.put("crc", 0L);
                            m.put("lastModified", entry.getLastModifiedDate());
                            fileList.add(m);
                            if (!entry.isDirectory()) {
                                totalSize += entry.getSize();
                            }
                        }
                    }
                    break;
                }
                default: {
                    try (ArchiveInputStream ais = new ArchiveStreamFactory().createArchiveInputStream(
                            new BufferedInputStream(rawIn, 8192))) {
                        ArchiveEntry entry;
                        while ((entry = ais.getNextEntry()) != null) {
                            Map<String, Object> m = new LinkedHashMap<>();
                            m.put("name", entry.getName());
                            m.put("size", entry.getSize());
                            m.put("compressedSize", entry.getSize());
                            m.put("isDirectory", entry.isDirectory());
                            m.put("crc", 0L);
                            m.put("lastModified", entry.getLastModifiedDate());
                            fileList.add(m);
                            if (!entry.isDirectory()) {
                                totalSize += entry.getSize();
                                totalCompressed += entry.getSize();
                            }
                        }
                    } catch (ArchiveException e) {
                        throw new IOException("无法识别的归档格式: " + archivePath.getFileName() + "（" + e.getMessage() + "）");
                    }
                }
            }
        }

        totalCompressed = Math.max(totalCompressed, totalSize);
        return Map.of(
            "zipPath", archivePath.toString(),
            "format", ext,
            "fileCount", fileList.size(),
            "totalSize", totalSize,
            "totalCompressedSize", totalCompressed,
            "encrypted", false,
            "files", fileList
        );
    }

    /**
     * 列出tar条目，返回非目录条目的总大小
     */
    private static long listTarEntries(InputStream in, List<Map<String, Object>> fileList) throws IOException {
        long totalSize = 0;
        try (TarArchiveInputStream tis = new TarArchiveInputStream(in)) {
            TarArchiveEntry entry;
            while ((entry = tis.getNextTarEntry()) != null) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", entry.getName());
                m.put("size", entry.getSize());
                m.put("compressedSize", entry.getSize());
                m.put("isDirectory", entry.isDirectory());
                m.put("crc", 0L);
                m.put("lastModified", entry.getLastModifiedDate());
                fileList.add(m);
                if (!entry.isDirectory()) {
                    totalSize += entry.getSize();
                }
            }
        }
        return totalSize;
    }

    /**
     * 去掉最后一个扩展名（如 file1.txt.gz → file1.txt）
     */
    private static String stripLastExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName + ".out";
    }

    /**
     * 根据文件名判断归档格式
     */
    private static String getArchiveExt(String fileName) {
        if (fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz")) return "tar.gz";
        if (fileName.endsWith(".tar.bz2") || fileName.endsWith(".tbz2") || fileName.endsWith(".tbz")) return "tar.bz2";
        if (fileName.endsWith(".tar.xz") || fileName.endsWith(".txz")) return "tar.xz";
        if (fileName.endsWith(".tar")) return "tar";
        if (fileName.endsWith(".zip")) return "zip";
        if (fileName.endsWith(".jar") || fileName.endsWith(".war") || fileName.endsWith(".ear")) return "zip";
        if (fileName.endsWith(".7z")) return "7z";
        if (fileName.endsWith(".gz")) return "gz";
        if (fileName.endsWith(".bz2")) return "bz2";
        if (fileName.endsWith(".xz")) return "xz";
        if (fileName.endsWith(".rar")) return "rar";
        if (fileName.endsWith(".cpio")) return "cpio";
        if (fileName.endsWith(".ar")) return "ar";
        return null;
    }

    /**
     * 去掉归档扩展名得到默认输出目录名
     */
    private static String stripArchiveExt(String fileName) {
        String[] suffixes = {".tar.gz", ".tar.bz2", ".tar.xz", ".tar", ".zip", ".jar", ".war", ".ear",
                             ".7z", ".gz", ".bz2", ".xz", ".rar", ".cpio", ".ar", ".tgz", ".tbz2", ".tbz", ".txz"};
        for (String suffix : suffixes) {
            if (fileName.endsWith(suffix)) {
                return fileName.substring(0, fileName.length() - suffix.length());
            }
        }
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
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

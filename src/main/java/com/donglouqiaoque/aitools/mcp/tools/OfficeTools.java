package com.donglouqiaoque.aitools.mcp.tools;

import com.donglouqiaoque.aitools.utils.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.Loader;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFTextShape;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Office文件处理工具
 * 支持PDF、DOC、DOCX、XLS、XLSX、PPT、PPTX等格式的读取和转换为Markdown
 */
public class OfficeTools {

    /**
     * 读取Office文件并转换为Markdown
     * @param filePath 文件路径
     * @return 包含Markdown文件路径的结果
     */
    public static Map<String, Object> readOfficeFile(String filePath) throws IOException {
        filePath = FileUtils.normalizePath(filePath);
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            throw new IOException("文件不存在: " + filePath);
        }

        if (!Files.isRegularFile(path)) {
            throw new IOException("路径不是文件: " + filePath);
        }

        // 获取文件扩展名
        String fileName = path.getFileName().toString().toLowerCase();
        String ext = "";
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            ext = fileName.substring(lastDot + 1);
        }

        // 生成输出文件名
        String baseName = fileName.substring(0, lastDot > 0 ? lastDot : fileName.length());
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String outputFileName = baseName + "_" + timestamp + ".md";

        // 确保缓存目录存在
        Path cacheDir = McpTools.ensureCacheDir();
        Path outputPath = cacheDir.resolve(outputFileName);

        StringBuilder markdown = new StringBuilder();
        String title = baseName;

        switch (ext) {
            case "pdf":
                markdown.append(readPdfFile(path));
                break;
            case "doc":
                markdown.append(readDocFile(path));
                break;
            case "docx":
                markdown.append(readDocxFile(path));
                break;
            case "xls":
            case "xlsx":
                markdown.append(readExcelFile(path, ext));
                break;
            case "ppt":
            case "pptx":
                markdown.append(readPptFile(path, ext));
                break;
            default:
                throw new IOException("不支持的文件格式: " + ext + "。支持的格式: pdf, doc, docx, xls, xlsx, ppt, pptx");
        }

        // 添加文件信息头
        StringBuilder header = new StringBuilder();
        header.append("---\n");
        header.append("title: ").append(title).append("\n");
        header.append("source: ").append(filePath).append("\n");
        header.append("converted: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        header.append("---\n\n");
        header.append("# ").append(title).append("\n\n");

        // 写入文件
        String finalContent = header.toString() + markdown.toString();
        Files.writeString(outputPath, finalContent, StandardCharsets.UTF_8);

        Map<String, Object> result = new HashMap<>();
        result.put("markdownPath", outputPath.toString());
        result.put("originalPath", filePath);
        result.put("title", title);
        result.put("size", Files.size(outputPath));
        return result;
    }

    /**
     * 读取PDF文件
     */
    private static String readPdfFile(Path path) throws IOException {
        StringBuilder sb = new StringBuilder();

        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            int pageCount = document.getNumberOfPages();
            sb.append("## 文件信息\n\n");
            sb.append("- 格式: PDF\n");
            sb.append("- 页数: ").append(pageCount).append("\n\n");

            for (int i = 1; i <= pageCount; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String pageText = stripper.getText(document);

                if (pageText != null && !pageText.trim().isEmpty()) {
                    sb.append("## 第").append(i).append("页\n\n");
                    // 处理文本，转换为Markdown格式
                    sb.append(processTextToMarkdown(pageText));
                    sb.append("\n\n");
                }
            }
        }

        return sb.toString();
    }

    /**
     * 读取DOC文件（旧格式）
     */
    private static String readDocFile(Path path) throws IOException {
        StringBuilder sb = new StringBuilder();

        try (FileInputStream fis = new FileInputStream(path.toFile());
             HWPFDocument document = new HWPFDocument(fis)) {

            WordExtractor extractor = new WordExtractor(document);
            String text = extractor.getText();

            sb.append("## 文件信息\n\n");
            sb.append("- 格式: Word (DOC)\n\n");
            sb.append("## 内容\n\n");
            sb.append(processTextToMarkdown(text));
        }

        return sb.toString();
    }

    /**
     * 读取DOCX文件
     */
    private static String readDocxFile(Path path) throws IOException {
        StringBuilder sb = new StringBuilder();

        try (FileInputStream fis = new FileInputStream(path.toFile());
             XWPFDocument document = new XWPFDocument(fis)) {

            sb.append("## 文件信息\n\n");
            sb.append("- 格式: Word (DOCX)\n");
            sb.append("- 段落数: ").append(document.getParagraphs().size()).append("\n\n");
            sb.append("## 内容\n\n");

            // 读取所有段落
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            for (XWPFParagraph para : paragraphs) {
                String text = para.getText();
                if (text != null && !text.trim().isEmpty()) {
                    // 根据样式判断标题级别
                    String style = para.getStyle();
                    if (style != null) {
                        if (style.startsWith("Heading1")) {
                            sb.append("# ");
                        } else if (style.startsWith("Heading2")) {
                            sb.append("## ");
                        } else if (style.startsWith("Heading3")) {
                            sb.append("### ");
                        }
                    }
                    sb.append(text).append("\n\n");
                }
            }
        }

        return sb.toString();
    }

    /**
     * 读取Excel文件
     */
    private static String readExcelFile(Path path, String ext) throws IOException {
        StringBuilder sb = new StringBuilder();
        Workbook workbook = null;

        try (FileInputStream fis = new FileInputStream(path.toFile())) {
            if ("xlsx".equals(ext)) {
                workbook = new XSSFWorkbook(fis);
            } else {
                workbook = new HSSFWorkbook(fis);
            }

            int sheetCount = workbook.getNumberOfSheets();
            sb.append("## 文件信息\n\n");
            sb.append("- 格式: Excel (").append(ext.toUpperCase()).append(")\n");
            sb.append("- 工作表数: ").append(sheetCount).append("\n\n");

            // 遍历所有工作表
            for (int i = 0; i < sheetCount; i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName();

                sb.append("## 工作表: ").append(sheetName).append("\n\n");

                // 转换为Markdown表格
                sb.append(excelSheetToMarkdownTable(sheet));
                sb.append("\n\n");
            }
        } finally {
            if (workbook != null) {
                workbook.close();
            }
        }

        return sb.toString();
    }

    /**
     * 将Excel工作表转换为Markdown表格
     */
    private static String excelSheetToMarkdownTable(Sheet sheet) {
        StringBuilder sb = new StringBuilder();
        int rowCount = sheet.getLastRowNum() + 1;

        if (rowCount == 0) {
            return "*空工作表*\n";
        }

        // 获取最大列数
        int maxCols = 0;
        for (int i = 0; i < rowCount; i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                maxCols = Math.max(maxCols, row.getLastCellNum());
            }
        }

        // 遍历所有行
        for (int i = 0; i < rowCount; i++) {
            Row row = sheet.getRow(i);
            sb.append("| ");

            for (int j = 0; j < maxCols; j++) {
                String cellValue = "";
                if (row != null) {
                    Cell cell = row.getCell(j);
                    if (cell != null) {
                        cellValue = getCellValueAsString(cell);
                    }
                }
                sb.append(cellValue).append(" | ");
            }
            sb.append("\n");

            // 添加表头分隔符（第一行后）
            if (i == 0) {
                sb.append("|");
                for (int j = 0; j < maxCols; j++) {
                    sb.append(" --- |");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * 获取单元格值作为字符串
     */
    private static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    /**
     * 读取PPT文件
     */
    private static String readPptFile(Path path, String ext) throws IOException {
        StringBuilder sb = new StringBuilder();

        if ("pptx".equals(ext)) {
            // PPTX格式
            try (FileInputStream fis = new FileInputStream(path.toFile());
                 XMLSlideShow ppt = new XMLSlideShow(fis)) {

                int slideCount = ppt.getSlides().size();
                sb.append("## 文件信息\n\n");
                sb.append("- 格式: PowerPoint (PPTX)\n");
                sb.append("- 幻灯片数: ").append(slideCount).append("\n\n");

                int slideNum = 1;
                for (XSLFSlide slide : ppt.getSlides()) {
                    sb.append("## 幻灯片 ").append(slideNum++).append("\n\n");

                    // 提取文本
                    StringBuilder slideText = new StringBuilder();
                    for (XSLFShape shape : slide.getShapes()) {
                        if (shape instanceof XSLFTextShape) {
                            String text = ((XSLFTextShape) shape).getText();
                            if (text != null && !text.trim().isEmpty()) {
                                slideText.append(text).append("\n");
                            }
                        }
                    }

                    if (slideText.length() > 0) {
                        sb.append(processTextToMarkdown(slideText.toString()));
                    } else {
                        sb.append("*无文本内容*\n");
                    }
                    sb.append("\n");
                }
            }
        } else {
            // PPT格式（旧格式）
            try (FileInputStream fis = new FileInputStream(path.toFile());
                 HSLFSlideShow ppt = new HSLFSlideShow(fis)) {

                int slideCount = ppt.getSlides().size();
                sb.append("## 文件信息\n\n");
                sb.append("- 格式: PowerPoint (PPT)\n");
                sb.append("- 幻灯片数: ").append(slideCount).append("\n\n");

                int slideNum = 1;
                for (HSLFSlide slide : ppt.getSlides()) {
                    sb.append("## 幻灯片 ").append(slideNum++).append("\n\n");

                    // 提取文本
                    StringBuilder slideText = new StringBuilder();
                    for (org.apache.poi.hslf.usermodel.HSLFShape shape : slide.getShapes()) {
                        if (shape instanceof HSLFTextShape) {
                            String text = ((HSLFTextShape) shape).getText();
                            if (text != null && !text.trim().isEmpty()) {
                                slideText.append(text).append("\n");
                            }
                        }
                    }

                    if (slideText.length() > 0) {
                        sb.append(processTextToMarkdown(slideText.toString()));
                    } else {
                        sb.append("*无文本内容*\n");
                    }
                    sb.append("\n");
                }
            }
        }

        return sb.toString();
    }

    /**
     * 处理文本，转换为Markdown格式
     */
    private static String processTextToMarkdown(String text) {
        if (text == null) {
            return "";
        }

        // 转义特殊字符
        text = text.replace("\\", "\\\\")
                   .replace("|", "\\|")
                   .replace("*", "\\*")
                   .replace("_", "\\_")
                   .replace("#", "\\#")
                   .replace("[", "\\[")
                   .replace("]", "\\]");

        // 保留段落结构
        String[] paragraphs = text.split("\n");
        StringBuilder sb = new StringBuilder();
        for (String para : paragraphs) {
            para = para.trim();
            if (!para.isEmpty()) {
                sb.append(para).append("\n\n");
            }
        }

        return sb.toString().trim();
    }

    /**
     * 支持的Office文件格式
     */
    public static final Set<String> SUPPORTED_EXTENSIONS = new HashSet<>(Arrays.asList(
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "md", "txt"
    ));

    /**
     * 可转换的输出格式
     */
    public static final Set<String> OUTPUT_FORMATS = new HashSet<>(Arrays.asList(
        "docx", "xlsx", "pptx", "pdf", "md", "txt"
    ));

    /**
     * 检查文件是否为支持的Office格式
     */
    public static boolean isSupportedOfficeFile(String filePath) {
        String fileName = filePath.toLowerCase();
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            String ext = fileName.substring(lastDot + 1);
            return SUPPORTED_EXTENSIONS.contains(ext);
        }
        return false;
    }

    /**
     * 将Office文件从一种格式转换为另一种格式
     * @param sourcePath 源文件路径
     * @param targetFormat 目标格式（docx/xlsx/pptx/pdf/md/txt）
     * @param outputPath 输出路径（可选，默认为cache目录）
     * @return 包含输出文件路径的结果
     */
    public static Map<String, Object> convertOfficeFile(String sourcePath, String targetFormat, String outputPath) throws IOException {
        sourcePath = FileUtils.normalizePath(sourcePath);
        Path source = Paths.get(sourcePath);

        if (!Files.exists(source)) {
            throw new IOException("源文件不存在: " + sourcePath);
        }

        if (!Files.isRegularFile(source)) {
            throw new IOException("路径不是文件: " + sourcePath);
        }

        // 获取源文件扩展名
        String fileName = source.getFileName().toString().toLowerCase();
        String sourceExt = "";
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            sourceExt = fileName.substring(lastDot + 1).toLowerCase();
        }

        // 规范化目标格式
        targetFormat = targetFormat.toLowerCase().trim();
        if (targetFormat.startsWith(".")) {
            targetFormat = targetFormat.substring(1);
        }

        // 验证目标格式
        if (!OUTPUT_FORMATS.contains(targetFormat)) {
            throw new IOException("不支持的目标格式: " + targetFormat + "。支持的格式: " + String.join(", ", OUTPUT_FORMATS));
        }

        // 检查是否为相同格式
        if (sourceExt.equals(targetFormat)) {
            throw new IOException("源格式和目标格式相同，无需转换");
        }

        // 确定输出路径
        Path output;
        if (outputPath != null && !outputPath.trim().isEmpty()) {
            output = Paths.get(FileUtils.normalizePath(outputPath));
        } else {
            // 生成默认输出文件名
            String baseName = fileName.substring(0, lastDot > 0 ? lastDot : fileName.length());
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String outputFileName = baseName + "_converted_" + timestamp + "." + targetFormat;
            Path cacheDir = McpTools.ensureCacheDir();
            output = cacheDir.resolve(outputFileName);
        }

        // 确保输出目录存在
        Files.createDirectories(output.getParent());

        // 根据目标格式执行转换
        switch (targetFormat) {
            case "md":
            case "txt":
                // 转换为纯文本/Markdown - 提取内容并保存
                convertToText(source, sourceExt, output);
                break;
            case "docx":
                // 转换为Word文档
                convertToDocx(source, sourceExt, output);
                break;
            case "xlsx":
                // 转换为Excel
                convertToExcel(source, sourceExt, output);
                break;
            case "pptx":
                // 转换为PPT
                convertToPptx(source, sourceExt, output);
                break;
            case "pdf":
                // 转换为PDF
                convertToPdf(source, sourceExt, output);
                break;
            default:
                throw new IOException("暂不支持转换为格式: " + targetFormat);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("sourcePath", sourcePath);
        result.put("sourceFormat", sourceExt);
        result.put("targetFormat", targetFormat);
        result.put("outputPath", output.toString());
        result.put("size", Files.size(output));
        return result;
    }

    /**
     * 转换为纯文本/Markdown格式
     */
    private static void convertToText(Path source, String sourceExt, Path output) throws IOException {
        StringBuilder content = new StringBuilder();

        switch (sourceExt) {
            case "pdf":
                content.append(readPdfFile(source));
                break;
            case "doc":
                content.append(readDocFile(source));
                break;
            case "docx":
                content.append(readDocxFile(source));
                break;
            case "xls":
            case "xlsx":
                content.append(readExcelFile(source, sourceExt));
                break;
            case "ppt":
            case "pptx":
                content.append(readPptFile(source, sourceExt));
                break;
            case "md":
            case "txt":
                // 直接复制文本内容
                content.append(Files.readString(source, StandardCharsets.UTF_8));
                break;
            default:
                throw new IOException("无法从格式 " + sourceExt + " 提取文本");
        }

        // 如果是输出为txt，清理Markdown标记
        if (output.toString().toLowerCase().endsWith(".txt")) {
            String text = content.toString();
            // 移除Markdown标记
            text = text.replaceAll("^---[\\s\\S]*?---\\s*", "");  // 移除YAML frontmatter
            text = text.replaceAll("#+\\s+", "");  // 移除标题标记
            text = text.replaceAll("\\*\\*?|__?", "");  // 移除强调标记
            text = text.replaceAll("\\[([^\\]]+)\\]\\([^\\)]+\\)", "$1");  // 移除链接
            text = text.replaceAll("\\|", " ");  // 移除表格分隔符
            text = text.replaceAll("\\s+", " ");  // 规范化空格
            content = new StringBuilder(text);
        }

        Files.writeString(output, content.toString(), StandardCharsets.UTF_8);
    }

    /**
     * 转换为DOCX格式
     */
    private static void convertToDocx(Path source, String sourceExt, Path output) throws IOException {
        // 提取源文件内容
        StringBuilder markdownContent = new StringBuilder();

        switch (sourceExt) {
            case "pdf":
                markdownContent.append(readPdfFile(source));
                break;
            case "doc":
                markdownContent.append(readDocFile(source));
                break;
            case "md":
            case "txt":
                markdownContent.append(Files.readString(source, StandardCharsets.UTF_8));
                break;
            default:
                throw new IOException("暂不支持从 " + sourceExt + " 转换为DOCX");
        }

        // 创建新的DOCX文档
        try (XWPFDocument document = new XWPFDocument()) {
            String[] lines = markdownContent.toString().split("\\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                XWPFParagraph paragraph = document.createParagraph();
                if (trimmed.startsWith("# ")) {
                    paragraph.setStyle("Heading1");
                    paragraph.createRun().setText(trimmed.substring(2));
                } else if (trimmed.startsWith("## ")) {
                    paragraph.setStyle("Heading2");
                    paragraph.createRun().setText(trimmed.substring(3));
                } else if (trimmed.startsWith("### ")) {
                    paragraph.setStyle("Heading3");
                    paragraph.createRun().setText(trimmed.substring(4));
                } else {
                    paragraph.createRun().setText(trimmed);
                }
            }

            try (FileOutputStream fos = new FileOutputStream(output.toFile())) {
                document.write(fos);
            }
        }
    }

    /**
     * 转换为XLSX格式
     */
    private static void convertToExcel(Path source, String sourceExt, Path output) throws IOException {
        // 目前仅支持从CSV或文本表格转换
        if (!sourceExt.equals("md") && !sourceExt.equals("txt")) {
            throw new IOException("暂不支持从 " + sourceExt + " 转换为Excel，请先将源文件转换为Markdown表格格式");
        }

        String content = Files.readString(source, StandardCharsets.UTF_8);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Sheet1");

            // 解析Markdown表格
            String[] lines = content.split("\\n");
            int rowNum = 0;
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("|")) {
                    // 跳过表格分隔符行
                    if (line.contains("---")) continue;

                    String[] cells = line.split("\\|");
                    org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                    int colNum = 0;
                    for (String cell : cells) {
                        cell = cell.trim();
                        if (!cell.isEmpty()) {
                            row.createCell(colNum++).setCellValue(cell);
                        }
                    }
                }
            }

            try (FileOutputStream fos = new FileOutputStream(output.toFile())) {
                workbook.write(fos);
            }
        }
    }

    /**
     * 转换为PPTX格式
     */
    private static void convertToPptx(Path source, String sourceExt, Path output) throws IOException {
        // 提取内容并按段落/标题分割成幻灯片
        StringBuilder content = new StringBuilder();

        switch (sourceExt) {
            case "md":
            case "txt":
                content.append(Files.readString(source, StandardCharsets.UTF_8));
                break;
            default:
                throw new IOException("暂不支持从 " + sourceExt + " 转换为PPTX");
        }

        try (XMLSlideShow ppt = new XMLSlideShow()) {
            String[] sections = content.toString().split("(?m)^##+\\s+");
            for (String section : sections) {
                if (section.trim().isEmpty()) continue;

                XSLFSlide slide = ppt.createSlide();
                String[] lines = section.split("\\n");

                // 第一行作为标题
                if (lines.length > 0) {
                    String title = lines[0].trim();
                    if (!title.isEmpty()) {
                        XSLFTextShape titleShape = slide.createTextBox();
                        titleShape.setText(title);
                    }
                }

                // 剩余内容作为正文
                if (lines.length > 1) {
                    StringBuilder body = new StringBuilder();
                    for (int i = 1; i < lines.length; i++) {
                        body.append(lines[i]).append("\\n");
                    }
                    if (body.length() > 0) {
                        XSLFTextShape bodyShape = slide.createTextBox();
                        bodyShape.setText(body.toString().trim());
                    }
                }
            }

            try (FileOutputStream fos = new FileOutputStream(output.toFile())) {
                ppt.write(fos);
            }
        }
    }

    /**
     * 转换为PDF格式
     */
    private static void convertToPdf(Path source, String sourceExt, Path output) throws IOException {
        // PDF转换需要更复杂的库支持，这里先提取文本并保存
        // 注意：Apache PDFBox不支持PDF写入，需要额外依赖如iText或OpenPDF
        throw new IOException("PDF格式转换暂不支持，请先转换为DOCX格式后使用外部工具转换为PDF");
    }
}

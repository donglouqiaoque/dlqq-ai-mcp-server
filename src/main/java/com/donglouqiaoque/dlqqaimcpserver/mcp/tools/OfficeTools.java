package com.donglouqiaoque.dlqqaimcpserver.mcp.tools;

import com.donglouqiaoque.dlqqaimcpserver.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.concurrent.*;

/**
 * Office文件处理工具
 * 支持PDF、DOC、DOCX、XLS、XLSX、PPT、PPTX等格式的读取和转换为Markdown
 */
public class OfficeTools {
    private static final Logger logger = LoggerFactory.getLogger(OfficeTools.class);
    
    // PDF处理限制：最大页数和超时时间
    private static final int MAX_PDF_PAGES = 100;
    private static final int PDF_TIMEOUT_SECONDS = 90;

    /**
     * 读取Office文件并转换为Markdown
     * @param filePath 文件路径
     * @return 包含Markdown文件路径的结果
     */
    public static Map<String, Object> readOfficeFile(String filePath) throws IOException {
        return readOfficeFile(filePath, null, null);
    }

    /**
     * 读取Office文件并转换为Markdown（支持PDF页码范围）
     * @param filePath 文件路径
     * @param startPage 起始页码（仅PDF有效，可选，从1开始）
     * @param endPage 结束页码（仅PDF有效，可选，包含该页）
     * @return 包含Markdown文件路径的结果
     */
    public static Map<String, Object> readOfficeFile(String filePath, Integer startPage, Integer endPage) throws IOException {
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
                markdown.append(readPdfFile(path, startPage, endPage));
                break;
            case "doc":
                markdown.append(readDocFile(path, startPage, endPage));
                break;
            case "docx":
                markdown.append(readDocxFile(path, startPage, endPage));
                break;
            case "xls":
            case "xlsx":
                markdown.append(readExcelFile(path, ext, startPage, endPage));
                break;
            case "ppt":
            case "pptx":
                markdown.append(readPptFile(path, ext, startPage, endPage));
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
     * 读取PDF文件（带超时保护，支持页码范围）
     */
    private static String readPdfFile(Path path, Integer startPage, Integer endPage) throws IOException {
        logger.info("开始读取PDF文件: {}, 页码范围: {}-{}", path, 
            startPage != null ? startPage : "默认", 
            endPage != null ? endPage : "默认");
        
        // 使用线程池和Future实现超时控制
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(() -> extractPdfText(path, startPage, endPage));
        
        try {
            String result = future.get(PDF_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            logger.info("PDF文件读取完成: {}", path);
            return result;
        } catch (TimeoutException e) {
            future.cancel(true);
            logger.error("PDF处理超时（超过{}秒）: {}", PDF_TIMEOUT_SECONDS, path);
            throw new IOException("PDF 处理超时（超过" + PDF_TIMEOUT_SECONDS + "秒），该文件可能包含复杂图形或表格。建议：1) 使用其他工具查看；2) 转换为图片后再读取；3) 提取纯文本版本。");
        } catch (InterruptedException e) {
            future.cancel(true);
            logger.error("PDF处理被中断: {}", path, e);
            throw new IOException("PDF 处理被中断: " + e.getMessage());
        } catch (ExecutionException e) {
            logger.error("PDF处理异常: {}", path, e.getCause());
            throw new IOException("PDF 处理失败: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        } finally {
            executor.shutdownNow();
        }
    }
    
    /**
     * 实际提取PDF文本（在独立线程中执行）
     */
    private static String extractPdfText(Path path, Integer startPage, Integer endPage) throws IOException {
        StringBuilder sb = new StringBuilder();

        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            // 移除 setSortByPosition(true) - 这是导致性能问题的关键！
            // stripper.setSortByPosition(true);

            int pageCount = document.getNumberOfPages();
            logger.info("PDF总页数: {}", pageCount);
            
            // 计算实际处理范围
            int actualStartPage = (startPage != null && startPage > 0) ? startPage : 1;
            int actualEndPage = (endPage != null && endPage > 0) ? Math.min(endPage, pageCount) : pageCount;
            
            // 应用最大页数限制
            if (actualEndPage - actualStartPage + 1 > MAX_PDF_PAGES) {
                logger.warn("请求页数范围过大({}-{})，限制为{}页", actualStartPage, actualEndPage, MAX_PDF_PAGES);
                actualEndPage = actualStartPage + MAX_PDF_PAGES - 1;
            }
            
            // 确保范围有效
            actualStartPage = Math.max(1, Math.min(actualStartPage, pageCount));
            actualEndPage = Math.max(actualStartPage, Math.min(actualEndPage, pageCount));
            
            int pagesToProcess = actualEndPage - actualStartPage + 1;
            
            sb.append("## 文件信息\n\n");
            sb.append("- 格式: PDF\n");
            sb.append("- 总页数: ").append(pageCount).append("\n");
            sb.append("- 处理页码: ").append(actualStartPage).append("-").append(actualEndPage);
            if (startPage != null || endPage != null) {
                sb.append("（用户指定范围）");
            }
            sb.append("\n\n");

            // 设置提取范围
            stripper.setStartPage(actualStartPage);
            stripper.setEndPage(actualEndPage);
            
            logger.info("开始提取PDF文本（{}-{}页）...", actualStartPage, actualEndPage);
            String allText = stripper.getText(document);
            logger.info("PDF文本提取完成");
            
            // 按页分割并处理
            String[] pages = allText.split("\f"); // PDF分页符
            int currentPage = actualStartPage;
            for (int i = 0; i < pages.length && currentPage <= actualEndPage; i++) {
                String pageText = pages[i];
                if (pageText != null && !pageText.trim().isEmpty()) {
                    sb.append("## 第").append(currentPage).append("页\n\n");
                    sb.append(processTextToMarkdown(pageText));
                    sb.append("\n\n");
                }
                currentPage++;
            }
        }

        return sb.toString();
    }

    /**
     * 读取DOC文件（旧格式，支持段落范围）
     */
    private static String readDocFile(Path path, Integer startPage, Integer endPage) throws IOException {
        StringBuilder sb = new StringBuilder();

        try (FileInputStream fis = new FileInputStream(path.toFile());
             HWPFDocument document = new HWPFDocument(fis)) {

            WordExtractor extractor = new WordExtractor(document);
            String text = extractor.getText();
            
            // 按段落分割
            String[] paragraphs = text.split("\r?\n");
            int totalParagraphs = paragraphs.length;
            
            // 计算范围（Word按段落索引，startPage/endPage转为段落索引）
            int startIdx = (startPage != null && startPage > 0) ? (startPage - 1) : 0;
            int endIdx = (endPage != null && endPage > 0) ? Math.min(endPage - 1, totalParagraphs - 1) : (totalParagraphs - 1);
            
            // 限制最大段落数
            if (endIdx - startIdx + 1 > MAX_PDF_PAGES) {
                logger.warn("DOC段落范围过大({}-{})，限制为{}段", startIdx + 1, endIdx + 1, MAX_PDF_PAGES);
                endIdx = startIdx + MAX_PDF_PAGES - 1;
            }
            
            startIdx = Math.max(0, Math.min(startIdx, totalParagraphs - 1));
            endIdx = Math.max(startIdx, Math.min(endIdx, totalParagraphs - 1));

            sb.append("## 文件信息\n\n");
            sb.append("- 格式: Word (DOC)\n");
            sb.append("- 总段落数: ").append(totalParagraphs).append("\n");
            if (startPage != null || endPage != null) {
                sb.append("- 处理段落: ").append(startIdx + 1).append("-").append(endIdx + 1).append("\n");
            }
            sb.append("\n## 内容\n\n");
            
            // 只输出指定范围的段落
            for (int i = startIdx; i <= endIdx && i < totalParagraphs; i++) {
                if (paragraphs[i] != null && !paragraphs[i].trim().isEmpty()) {
                    sb.append(processTextToMarkdown(paragraphs[i])).append("\n\n");
                }
            }
        }

        return sb.toString();
    }

    /**
     * 读取DOCX文件（支持段落范围）
     */
    private static String readDocxFile(Path path, Integer startPage, Integer endPage) throws IOException {
        StringBuilder sb = new StringBuilder();

        try (FileInputStream fis = new FileInputStream(path.toFile());
             XWPFDocument document = new XWPFDocument(fis)) {

            List<XWPFParagraph> paragraphs = document.getParagraphs();
            int totalParagraphs = paragraphs.size();
            
            // 计算范围（按段落索引）
            int startIdx = (startPage != null && startPage > 0) ? (startPage - 1) : 0;
            int endIdx = (endPage != null && endPage > 0) ? Math.min(endPage - 1, totalParagraphs - 1) : (totalParagraphs - 1);
            
            // 限制最大段落数
            if (endIdx - startIdx + 1 > MAX_PDF_PAGES) {
                logger.warn("DOCX段落范围过大({}-{})，限制为{}段", startIdx + 1, endIdx + 1, MAX_PDF_PAGES);
                endIdx = startIdx + MAX_PDF_PAGES - 1;
            }
            
            startIdx = Math.max(0, Math.min(startIdx, totalParagraphs - 1));
            endIdx = Math.max(startIdx, Math.min(endIdx, totalParagraphs - 1));

            sb.append("## 文件信息\n\n");
            sb.append("- 格式: Word (DOCX)\n");
            sb.append("- 总段落数: ").append(totalParagraphs).append("\n");
            if (startPage != null || endPage != null) {
                sb.append("- 处理段落: ").append(startIdx + 1).append("-").append(endIdx + 1).append("\n");
            }
            sb.append("\n## 内容\n\n");

            // 只读取指定范围的段落
            for (int i = startIdx; i <= endIdx && i < totalParagraphs; i++) {
                XWPFParagraph para = paragraphs.get(i);
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
     * 读取Excel文件（支持Sheet范围，start_page/end_page 对应 Sheet 索引）
     */
    private static String readExcelFile(Path path, String ext, Integer startPage, Integer endPage) throws IOException {
        StringBuilder sb = new StringBuilder();
        Workbook workbook = null;

        try (FileInputStream fis = new FileInputStream(path.toFile())) {
            if ("xlsx".equals(ext)) {
                workbook = new XSSFWorkbook(fis);
            } else {
                workbook = new HSSFWorkbook(fis);
            }

            int sheetCount = workbook.getNumberOfSheets();
            
            // 计算Sheet范围（startPage/endPage作为Sheet索引，1-based）
            int startSheet = (startPage != null && startPage > 0) ? (startPage - 1) : 0;
            int endSheet = (endPage != null && endPage > 0) ? Math.min(endPage - 1, sheetCount - 1) : (sheetCount - 1);
            
            // 限制最大Sheet数
            if (endSheet - startSheet + 1 > MAX_PDF_PAGES) {
                logger.warn("Excel Sheet范围过大({}-{})，限制为{}个", startSheet + 1, endSheet + 1, MAX_PDF_PAGES);
                endSheet = startSheet + MAX_PDF_PAGES - 1;
            }
            
            startSheet = Math.max(0, Math.min(startSheet, sheetCount - 1));
            endSheet = Math.max(startSheet, Math.min(endSheet, sheetCount - 1));

            sb.append("## 文件信息\n\n");
            sb.append("- 格式: Excel (").append(ext.toUpperCase()).append(")\n");
            sb.append("- 总工作表数: ").append(sheetCount).append("\n");
            if (startPage != null || endPage != null) {
                sb.append("- 处理工作表: ").append(startSheet + 1).append("-").append(endSheet + 1).append("\n");
            }
            sb.append("\n");

            // 遍历指定范围的工作表
            for (int i = startSheet; i <= endSheet && i < sheetCount; i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName();

                sb.append("## 工作表 ").append(i + 1).append(": ").append(sheetName).append("\n\n");

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
     * 读取PPT文件（支持幻灯片范围）
     */
    private static String readPptFile(Path path, String ext, Integer startPage, Integer endPage) throws IOException {
        StringBuilder sb = new StringBuilder();

        if ("pptx".equals(ext)) {
            // PPTX格式
            try (FileInputStream fis = new FileInputStream(path.toFile());
                 XMLSlideShow ppt = new XMLSlideShow(fis)) {

                int slideCount = ppt.getSlides().size();
                
                // 计算幻灯片范围（1-based）
                int startSlide = (startPage != null && startPage > 0) ? (startPage - 1) : 0;
                int endSlide = (endPage != null && endPage > 0) ? Math.min(endPage - 1, slideCount - 1) : (slideCount - 1);
                
                // 限制最大幻灯片数
                if (endSlide - startSlide + 1 > MAX_PDF_PAGES) {
                    logger.warn("PPTX幻灯片范围过大({}-{})，限制为{}张", startSlide + 1, endSlide + 1, MAX_PDF_PAGES);
                    endSlide = startSlide + MAX_PDF_PAGES - 1;
                }
                
                startSlide = Math.max(0, Math.min(startSlide, slideCount - 1));
                endSlide = Math.max(startSlide, Math.min(endSlide, slideCount - 1));

                sb.append("## 文件信息\n\n");
                sb.append("- 格式: PowerPoint (PPTX)\n");
                sb.append("- 总幻灯片数: ").append(slideCount).append("\n");
                if (startPage != null || endPage != null) {
                    sb.append("- 处理幻灯片: ").append(startSlide + 1).append("-").append(endSlide + 1).append("\n");
                }
                sb.append("\n");

                List<XSLFSlide> slides = ppt.getSlides();
                for (int i = startSlide; i <= endSlide && i < slideCount; i++) {
                    XSLFSlide slide = slides.get(i);
                    sb.append("## 幻灯片 ").append(i + 1).append("\n\n");

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
                
                // 计算幻灯片范围（1-based）
                int startSlide = (startPage != null && startPage > 0) ? (startPage - 1) : 0;
                int endSlide = (endPage != null && endPage > 0) ? Math.min(endPage - 1, slideCount - 1) : (slideCount - 1);
                
                // 限制最大幻灯片数
                if (endSlide - startSlide + 1 > MAX_PDF_PAGES) {
                    logger.warn("PPT幻灯片范围过大({}-{})，限制为{}张", startSlide + 1, endSlide + 1, MAX_PDF_PAGES);
                    endSlide = startSlide + MAX_PDF_PAGES - 1;
                }
                
                startSlide = Math.max(0, Math.min(startSlide, slideCount - 1));
                endSlide = Math.max(startSlide, Math.min(endSlide, slideCount - 1));

                sb.append("## 文件信息\n\n");
                sb.append("- 格式: PowerPoint (PPT)\n");
                sb.append("- 总幻灯片数: ").append(slideCount).append("\n");
                if (startPage != null || endPage != null) {
                    sb.append("- 处理幻灯片: ").append(startSlide + 1).append("-").append(endSlide + 1).append("\n");
                }
                sb.append("\n");

                List<HSLFSlide> slides = ppt.getSlides();
                for (int i = startSlide; i <= endSlide && i < slideCount; i++) {
                    HSLFSlide slide = slides.get(i);
                    sb.append("## 幻灯片 ").append(i + 1).append("\n\n");

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
        return convertOfficeFile(sourcePath, targetFormat, outputPath, null, null);
    }

    /**
     * 将Office文件从一种格式转换为另一种格式（支持分页/分段）
     * @param sourcePath 源文件路径
     * @param targetFormat 目标格式（docx/xlsx/pptx/pdf/md/txt）
     * @param outputPath 输出路径（可选，默认为cache目录）
     * @param startPage 起始页码/段落/Sheet/幻灯片（可选）
     * @param endPage 结束页码/段落/Sheet/幻灯片（可选）
     * @return 包含输出文件路径的结果
     */
    public static Map<String, Object> convertOfficeFile(String sourcePath, String targetFormat, String outputPath, Integer startPage, Integer endPage) throws IOException {
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
            // 生成默认输出文件名（包含范围信息）
            String baseName = fileName.substring(0, lastDot > 0 ? lastDot : fileName.length());
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String rangeSuffix = "";
            if (startPage != null || endPage != null) {
                rangeSuffix = "_p" + (startPage != null ? startPage : "") + "-" + (endPage != null ? endPage : "");
            }
            String outputFileName = baseName + "_converted" + rangeSuffix + "_" + timestamp + "." + targetFormat;
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
                convertToText(source, sourceExt, output, startPage, endPage);
                break;
            case "docx":
                // 转换为Word文档
                convertToDocx(source, sourceExt, output, startPage, endPage);
                break;
            case "xlsx":
                // 转换为Excel
                convertToExcel(source, sourceExt, output, startPage, endPage);
                break;
            case "pptx":
                // 转换为PPT
                convertToPptx(source, sourceExt, output, startPage, endPage);
                break;
            case "pdf":
                // 转换为PDF
                convertToPdf(source, sourceExt, output, startPage, endPage);
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
     * 转换为纯文本/Markdown格式（支持分页/分段）
     */
    private static void convertToText(Path source, String sourceExt, Path output, Integer startPage, Integer endPage) throws IOException {
        StringBuilder content = new StringBuilder();

        switch (sourceExt) {
            case "pdf":
                content.append(readPdfFile(source, startPage, endPage));
                break;
            case "doc":
                content.append(readDocFile(source, startPage, endPage));
                break;
            case "docx":
                content.append(readDocxFile(source, startPage, endPage));
                break;
            case "xls":
            case "xlsx":
                content.append(readExcelFile(source, sourceExt, startPage, endPage));
                break;
            case "ppt":
            case "pptx":
                content.append(readPptFile(source, sourceExt, startPage, endPage));
                break;
            case "md":
            case "txt":
                // 直接复制文本内容（不支持分页）
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
     * 转换为DOCX格式（支持分页/分段）
     */
    private static void convertToDocx(Path source, String sourceExt, Path output, Integer startPage, Integer endPage) throws IOException {
        // 提取源文件内容
        StringBuilder markdownContent = new StringBuilder();

        switch (sourceExt) {
            case "pdf":
                markdownContent.append(readPdfFile(source, startPage, endPage));
                break;
            case "doc":
                markdownContent.append(readDocFile(source, startPage, endPage));
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
     * 转换为XLSX格式（startPage/endPage 参数暂不使用，保留兼容性）
     */
    private static void convertToExcel(Path source, String sourceExt, Path output, Integer startPage, Integer endPage) throws IOException {
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
     * 转换为PPTX格式（startPage/endPage 参数暂不使用，保留兼容性）
     */
    private static void convertToPptx(Path source, String sourceExt, Path output, Integer startPage, Integer endPage) throws IOException {
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
     * 转换为PDF格式（startPage/endPage 参数暂不使用，保留兼容性）
     */
    private static void convertToPdf(Path source, String sourceExt, Path output, Integer startPage, Integer endPage) throws IOException {
        // PDF转换需要更复杂的库支持，这里先提取文本并保存
        // 注意：Apache PDFBox不支持PDF写入，需要额外依赖如iText或OpenPDF
        throw new IOException("PDF格式转换暂不支持，请先转换为DOCX格式后使用外部工具转换为PDF");
    }
}

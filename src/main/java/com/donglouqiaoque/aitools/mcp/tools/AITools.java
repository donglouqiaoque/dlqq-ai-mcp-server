package com.donglouqiaoque.aitools.mcp.tools;

import com.donglouqiaoque.aitools.mcp.protocol.McpSchema.*;
import com.donglouqiaoque.aitools.mcp.server.McpServer;

import java.io.IOException;
import java.util.*;

/**
 * AI工具集 - 注册到MCP服务器的工具
 * 包含12个核心工具：文件操作、对话框、串口功能
 */
public class AITools {

    /**
     * 注册所有AI工具到MCP服务器
     */
    public void registerTools(McpServer server) {
        // ========== 文件操作工具 (1-11) ==========
        registerReadFileTool(server);           // dlqq_read_file
        registerCreateFileTool(server);           // dlqq_create_file
        registerOverwriteFileTool(server);        // dlqq_overwrite_file
        registerAppendFileTool(server);           // dlqq_append_file
        registerRemoveLinesTool(server);          // dlqq_remove_lines_in_file
        registerReplaceLinesTool(server);         // dlqq_replace_lines_in_file
        registerInsertTextTool(server);           // dlqq_insert_text_in_file
        registerReplaceTextTool(server);          // dlqq_replace_text_in_file
        registerDeleteFileTool(server);           // dlqq_delete_file
        registerCopyFileTool(server);             // dlqq_copy_file
        registerMoveFileTool(server);             // dlqq_move_file
        registerRenameFileTool(server);           // dlqq_rename_file

        // ========== 目录操作工具 (12-17) ==========
        registerCreateDirTool(server);            // dlqq_mdir
        registerCreateDirsTool(server);           // dlqq_mdirs
        registerDeleteDirTool(server);            // dlqq_delete_dir
        registerCopyDirTool(server);              // dlqq_copy_dir
        registerMoveDirTool(server);              // dlqq_move_dir
        registerRenameDirTool(server);              // dlqq_rename_dir

        // ========== 二进制文件操作工具 (18-19) ==========
        registerReadBinFileTool(server);          // dlqq_read_bin_file
        registerWriteBinFileTool(server);         // dlqq_write_bin_file

        // ========== 搜索工具 (20-21) ==========
        registerSearchFileTool(server);           // dlqq_search_file
        registerSearchDirTool(server);            // dlqq_search_dir

        // ========== 对话框工具 (22-24) ==========
        registerDialogTextTool(server);           // dlqq_dialog_text
        registerDialogCheckTool(server);          // dlqq_dialog_check
        registerDialogFilePathTool(server);       // dlqq_dialog_file_path

        // ========== 串口工具 (25-32) ==========
        registerRunUartTool(server);              // dlqq_run_uart
        registerUartGetComTool(server);         // dlqq_uart_get_com
        registerUartRefreshComTool(server);       // dlqq_uart_refresh_com
        registerUartOpenComTool(server);          // dlqq_uart_open_com
        registerUartReopenComTool(server);        // dlqq_uart_reopen_com
        registerUartCloseComTool(server);         // dlqq_uart_close_com
        registerUartSendTextTool(server);         // dlqq_uart_send_text
        registerUartSendHexTool(server);          // dlqq_uart_send_hex
        registerUartGetLogTool(server);           // dlqq_uart_get_log
        registerUartGetLogTextTool(server);       // dlqq_uart_get_log_text
        registerUartGetLogHexTool(server);        // dlqq_uart_get_log_hex
        registerUartClearTextTool(server);        // dlqq_uart_clear_text
        registerUartGetLineCountTool(server);     // dlqq_uart_get_line_count
        registerUartSetTextSizeTool(server);      // dlqq_uart_set_text_size
        registerUartSwitchTextHexTool(server);    // dlqq_uart_switch_text_hex
        registerExitUartTool(server);             // dlqq_exit_uart
    }

    // ==================== 文件操作工具 ====================

    /**
     * 1. dlqq_read_file - 读取文件（支持行范围）
     */
    private void registerReadFileTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> pathProp = new HashMap<>();
        pathProp.put("type", "string");
        pathProp.put("description", "文件路径（支持绝对路径和相对路径）");
        properties.put("file_path", pathProp);

        Map<String, Object> startProp = new HashMap<>();
        startProp.put("type", "integer");
        startProp.put("description", "起始行数（1-based，默认为1）");
        properties.put("start_line", startProp);

        Map<String, Object> endProp = new HashMap<>();
        endProp.put("type", "integer");
        endProp.put("description", "结束行数（1-based，包含，-1表示到文件末尾）");
        properties.put("end_line", endProp);

        Map<String, Object> linesProp = new HashMap<>();
        linesProp.put("type", "integer");
        linesProp.put("description", "读取行数（与end_line互斥，end_line优先）");
        properties.put("read_lines", linesProp);

        Map<String, Object> charsetProp = new HashMap<>();
        charsetProp.put("type", "string");
        charsetProp.put("description", "字符集编码（如 UTF-8、GBK、ISO-8859-1 等，默认为 UTF-8）");
        properties.put("charset", charsetProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("file_path"));

        server.addTool("dlqq_read_file",
            "读取指定文件的指定行范围的内容。支持通过start_line和end_line指定范围，或通过start_line和read_lines指定起始行和读取行数。支持指定字符集编码（默认UTF-8）。",
            inputSchema,
            arguments -> {
                String filePath = (String) arguments.get("file_path");
                Integer startLine = parseInteger(arguments.get("start_line"), 1);
                Integer endLine = parseInteger(arguments.get("end_line"), -1);
                Integer readLines = parseInteger(arguments.get("read_lines"), 0);
                String charset = (String) arguments.get("charset");

                if (filePath == null || filePath.trim().isEmpty()) {
                    return errorResult("文件路径不能为空");
                }

                try {
                    String content = FileTools.readFile(filePath, startLine, endLine, readLines, charset);
                    return textResult(content);
                } catch (IOException e) {
                    return errorResult("读取文件失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 2. dlqq_create_file - 创建文件
     */
    private void registerCreateFileTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> pathProp = new HashMap<>();
        pathProp.put("type", "string");
        pathProp.put("description", "文件路径（支持绝对路径和相对路径）");
        properties.put("file_path", pathProp);

        Map<String, Object> contentProp = new HashMap<>();
        contentProp.put("type", "string");
        contentProp.put("description", "文件内容（为空则创建空文件）");
        properties.put("content", contentProp);

        Map<String, Object> charsetProp = new HashMap<>();
        charsetProp.put("type", "string");
        charsetProp.put("description", "字符集编码（如 UTF-8、GBK、ISO-8859-1 等，默认为 UTF-8）");
        properties.put("charset", charsetProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("file_path"));

        server.addTool("dlqq_create_file",
            "创建新文件，可指定文件内容。如果文件已存在，会覆盖原有内容。自动创建父目录。支持指定字符集编码（默认UTF-8）。",
            inputSchema,
            arguments -> {
                String filePath = (String) arguments.get("file_path");
                String content = (String) arguments.get("content");
                String charset = (String) arguments.get("charset");

                if (filePath == null || filePath.trim().isEmpty()) {
                    return errorResult("文件路径不能为空");
                }

                try {
                    FileTools.createFile(filePath, content, charset);
                    return textResult("文件创建成功: " + filePath);
                } catch (IOException e) {
                    return errorResult("创建文件失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 2.5 dlqq_overwrite_file - 覆盖文件
     */
    private void registerOverwriteFileTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> pathProp = new HashMap<>();
        pathProp.put("type", "string");
        pathProp.put("description", "文件路径（支持绝对路径和相对路径）");
        properties.put("file_path", pathProp);

        Map<String, Object> contentProp = new HashMap<>();
        contentProp.put("type", "string");
        contentProp.put("description", "文件内容（会完全覆盖原文件内容）");
        properties.put("content", contentProp);

        Map<String, Object> charsetProp = new HashMap<>();
        charsetProp.put("type", "string");
        charsetProp.put("description", "字符集编码（如 UTF-8、GBK、ISO-8859-1 等，默认为 UTF-8）");
        properties.put("charset", charsetProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("file_path"));

        server.addTool("dlqq_overwrite_file",
            "覆盖指定文件的内容，不管文件是否存在都会直接写入。自动创建父目录。支持指定字符集编码（默认UTF-8）。",
            inputSchema,
            arguments -> {
                String filePath = (String) arguments.get("file_path");
                String content = (String) arguments.get("content");
                String charset = (String) arguments.get("charset");

                if (filePath == null || filePath.trim().isEmpty()) {
                    return errorResult("文件路径不能为空");
                }

                try {
                    FileTools.overwriteFile(filePath, content, charset);
                    return textResult("文件覆盖成功: " + filePath);
                } catch (IOException e) {
                    return errorResult("覆盖文件失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 2.6 dlqq_append_file - 追加文件内容
     */
    private void registerAppendFileTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> pathProp = new HashMap<>();
        pathProp.put("type", "string");
        pathProp.put("description", "文件路径（支持绝对路径和相对路径）");
        properties.put("file_path", pathProp);

        Map<String, Object> contentProp = new HashMap<>();
        contentProp.put("type", "string");
        contentProp.put("description", "要追加的内容");
        properties.put("content", contentProp);

        Map<String, Object> charsetProp = new HashMap<>();
        charsetProp.put("type", "string");
        charsetProp.put("description", "字符集编码（如 UTF-8、GBK、ISO-8859-1 等，默认为 UTF-8）");
        properties.put("charset", charsetProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("file_path", "content"));

        server.addTool("dlqq_append_file",
            "在文件末尾追加内容。如果文件不存在会自动创建。自动创建父目录。支持指定字符集编码（默认UTF-8）。",
            inputSchema,
            arguments -> {
                String filePath = (String) arguments.get("file_path");
                String content = (String) arguments.get("content");
                String charset = (String) arguments.get("charset");

                if (filePath == null || filePath.trim().isEmpty()) {
                    return errorResult("文件路径不能为空");
                }
                if (content == null || content.isEmpty()) {
                    return errorResult("追加内容不能为空");
                }

                try {
                    FileTools.appendFile(filePath, content, charset);
                    return textResult("内容追加成功: " + filePath);
                } catch (IOException e) {
                    return errorResult("追加内容失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 3. dlqq_remove_lines_in_file - 删除指定行
     */
    private void registerRemoveLinesTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> pathProp = new HashMap<>();
        pathProp.put("type", "string");
        pathProp.put("description", "文件路径");
        properties.put("file_path", pathProp);

        Map<String, Object> startProp = new HashMap<>();
        startProp.put("type", "integer");
        startProp.put("description", "起始行数（1-based，包含）");
        properties.put("start_line", startProp);

        Map<String, Object> endProp = new HashMap<>();
        endProp.put("type", "integer");
        endProp.put("description", "结束行数（1-based，包含）");
        properties.put("end_line", endProp);

        Map<String, Object> charsetProp = new HashMap<>();
        charsetProp.put("type", "string");
        charsetProp.put("description", "字符集编码（如 UTF-8、GBK、ISO-8859-1 等，默认为 UTF-8）");
        properties.put("charset", charsetProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("file_path", "start_line", "end_line"));

        server.addTool("dlqq_remove_lines_in_file",
            "删除文件中指定行范围的内容（包含起始行和结束行）。支持指定字符集编码（默认UTF-8）。",
            inputSchema,
            arguments -> {
                String filePath = (String) arguments.get("file_path");
                Integer startLine = parseInteger(arguments.get("start_line"), null);
                Integer endLine = parseInteger(arguments.get("end_line"), null);
                String charset = (String) arguments.get("charset");

                if (filePath == null || filePath.trim().isEmpty()) {
                    return errorResult("文件路径不能为空");
                }
                if (startLine == null || endLine == null) {
                    return errorResult("起始行和结束行必须指定");
                }

                try {
                    boolean success = FileTools.removeLinesInFile(filePath, startLine, endLine, charset);
                    if (success) {
                        return textResult("成功删除第 " + startLine + " 行至第 " + endLine + " 行");
                    } else {
                        return errorResult("删除失败");
                    }
                } catch (IOException e) {
                    return errorResult("操作失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 3.5 dlqq_replace_lines_in_file - 替换指定行内容
     */
    private void registerReplaceLinesTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> pathProp = new HashMap<>();
        pathProp.put("type", "string");
        pathProp.put("description", "文件路径");
        properties.put("file_path", pathProp);

        Map<String, Object> startProp = new HashMap<>();
        startProp.put("type", "integer");
        startProp.put("description", "起始行数（1-based，包含）");
        properties.put("start_line", startProp);

        Map<String, Object> endProp = new HashMap<>();
        endProp.put("type", "integer");
        endProp.put("description", "结束行数（1-based，包含）");
        properties.put("end_line", endProp);

        Map<String, Object> contentProp = new HashMap<>();
        contentProp.put("type", "string");
        contentProp.put("description", "替换的新内容（支持多行）");
        properties.put("content", contentProp);

        Map<String, Object> charsetProp = new HashMap<>();
        charsetProp.put("type", "string");
        charsetProp.put("description", "字符集编码（如 UTF-8、GBK、ISO-8859-1 等，默认为 UTF-8）");
        properties.put("charset", charsetProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("file_path", "start_line", "end_line", "content"));

        server.addTool("dlqq_replace_lines_in_file",
            "将文件中指定行范围的内容替换为新内容（包含起始行和结束行）。支持多行替换。支持指定字符集编码（默认UTF-8）。",
            inputSchema,
            arguments -> {
                String filePath = (String) arguments.get("file_path");
                Integer startLine = parseInteger(arguments.get("start_line"), null);
                Integer endLine = parseInteger(arguments.get("end_line"), null);
                String content = (String) arguments.get("content");
                String charset = (String) arguments.get("charset");

                if (filePath == null || filePath.trim().isEmpty()) {
                    return errorResult("文件路径不能为空");
                }
                if (startLine == null || endLine == null) {
                    return errorResult("起始行和结束行必须指定");
                }
                if (content == null) {
                    content = "";
                }

                try {
                    boolean success = FileTools.replaceLinesInFile(filePath, startLine, endLine, content, charset);
                    if (success) {
                        return textResult("成功替换第 " + startLine + " 行至第 " + endLine + " 行的内容");
                    } else {
                        return errorResult("替换失败");
                    }
                } catch (IOException e) {
                    return errorResult("操作失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 4. dlqq_insert_text_in_file - 在指定行插入内容
     */
    private void registerInsertTextTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> pathProp = new HashMap<>();
        pathProp.put("type", "string");
        pathProp.put("description", "文件路径");
        properties.put("file_path", pathProp);

        Map<String, Object> lineProp = new HashMap<>();
        lineProp.put("type", "integer");
        lineProp.put("description", "插入位置（1-based，插入到该行之前）");
        properties.put("line", lineProp);

        Map<String, Object> contentProp = new HashMap<>();
        contentProp.put("type", "string");
        contentProp.put("description", "要插入的内容（支持多行）");
        properties.put("content", contentProp);

        Map<String, Object> charsetProp = new HashMap<>();
        charsetProp.put("type", "string");
        charsetProp.put("description", "字符集编码（如 UTF-8、GBK、ISO-8859-1 等，默认为 UTF-8）");
        properties.put("charset", charsetProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("file_path", "line", "content"));

        server.addTool("dlqq_insert_text_in_file",
            "在文件的指定行之前插入内容。支持插入多行文本。支持指定字符集编码（默认UTF-8）。",
            inputSchema,
            arguments -> {
                String filePath = (String) arguments.get("file_path");
                Integer line = parseInteger(arguments.get("line"), null);
                String content = (String) arguments.get("content");
                String charset = (String) arguments.get("charset");

                if (filePath == null || filePath.trim().isEmpty()) {
                    return errorResult("文件路径不能为空");
                }
                if (line == null) {
                    return errorResult("插入行必须指定");
                }
                if (content == null) {
                    content = "";
                }

                try {
                    boolean success = FileTools.insertTextInFile(filePath, line, content, charset);
                    if (success) {
                        return textResult("成功在第 " + line + " 行前插入内容");
                    } else {
                        return errorResult("插入失败");
                    }
                } catch (IOException e) {
                    return errorResult("操作失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 5. dlqq_replace_text_in_file - 替换文本（支持正则）
     */
    private void registerReplaceTextTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> pathProp = new HashMap<>();
        pathProp.put("type", "string");
        pathProp.put("description", "文件路径");
        properties.put("file_path", pathProp);

        Map<String, Object> searchProp = new HashMap<>();
        searchProp.put("type", "string");
        searchProp.put("description", "要搜索的文本或正则表达式");
        properties.put("search_text", searchProp);

        Map<String, Object> replaceProp = new HashMap<>();
        replaceProp.put("type", "string");
        replaceProp.put("description", "替换后的文本");
        properties.put("replace_text", replaceProp);

        Map<String, Object> modeProp = new HashMap<>();
        modeProp.put("type", "string");
        modeProp.put("description", "模式：'text'（普通文本）或 'regex'（正则表达式）");
        modeProp.put("enum", Arrays.asList("text", "regex"));
        properties.put("mode", modeProp);

        Map<String, Object> charsetProp = new HashMap<>();
        charsetProp.put("type", "string");
        charsetProp.put("description", "字符集编码（如 UTF-8、GBK、ISO-8859-1 等，默认为 UTF-8）");
        properties.put("charset", charsetProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("file_path", "search_text", "replace_text"));

        server.addTool("dlqq_replace_text_in_file",
            "替换文件中的指定文本。支持普通文本模式和正则表达式模式。支持指定字符集编码（默认UTF-8）。",
            inputSchema,
            arguments -> {
                String filePath = (String) arguments.get("file_path");
                String searchText = (String) arguments.get("search_text");
                String replaceText = (String) arguments.get("replace_text");
                String mode = (String) arguments.getOrDefault("mode", "text");
                String charset = (String) arguments.get("charset");

                if (filePath == null || filePath.trim().isEmpty()) {
                    return errorResult("文件路径不能为空");
                }
                if (searchText == null) {
                    return errorResult("搜索文本不能为空");
                }
                if (replaceText == null) {
                    replaceText = "";
                }

                try {
                    boolean useRegex = "regex".equalsIgnoreCase(mode);
                    int count = FileTools.replaceTextInFile(filePath, searchText, replaceText, useRegex, charset);
                    if (count > 0) {
                        return textResult("成功替换 " + count + " 处内容");
                    } else {
                        return textResult("未找到匹配内容，未进行替换");
                    }
                } catch (IOException e) {
                    return errorResult("操作失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 6. dlqq_delete_file - 删除文件（带确认对话框）
     */
    private void registerDeleteFileTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> pathProp = new HashMap<>();
        pathProp.put("type", "string");
        pathProp.put("description", "要删除的文件路径");
        properties.put("file_path", pathProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("file_path"));

        server.addTool("dlqq_delete_file",
            "删除指定文件。敏感操作，会弹出确认对话框要求用户手动确认。",
            inputSchema,
            arguments -> {
                String filePath = (String) arguments.get("file_path");

                if (filePath == null || filePath.trim().isEmpty()) {
                    return errorResult("文件路径不能为空");
                }

                try {
                    boolean deleted = FileTools.deleteFile(filePath);
                    if (deleted) {
                        return textResult("文件已删除: " + filePath);
                    } else {
                        return textResult("用户取消了删除操作");
                    }
                } catch (IOException e) {
                    return errorResult("删除失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 7. dlqq_copy_file - 复制文件
     */
    private void registerCopyFileTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> sourceProp = new HashMap<>();
        sourceProp.put("type", "string");
        sourceProp.put("description", "源文件路径");
        properties.put("source_path", sourceProp);

        Map<String, Object> targetProp = new HashMap<>();
        targetProp.put("type", "string");
        targetProp.put("description", "目标文件路径");
        properties.put("target_path", targetProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("source_path", "target_path"));

        server.addTool("dlqq_copy_file",
            "复制文件到指定位置。如果目标文件已存在，会覆盖。自动创建目标目录。",
            inputSchema,
            arguments -> {
                String sourcePath = (String) arguments.get("source_path");
                String targetPath = (String) arguments.get("target_path");

                if (sourcePath == null || sourcePath.trim().isEmpty()) {
                    return errorResult("源文件路径不能为空");
                }
                if (targetPath == null || targetPath.trim().isEmpty()) {
                    return errorResult("目标文件路径不能为空");
                }

                try {
                    FileTools.copyFile(sourcePath, targetPath);
                    return textResult("文件复制成功");
                } catch (IOException e) {
                    return errorResult("复制失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 8. dlqq_move_file - 移动文件
     */
    private void registerMoveFileTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> sourceProp = new HashMap<>();
        sourceProp.put("type", "string");
        sourceProp.put("description", "源文件路径");
        properties.put("source_path", sourceProp);

        Map<String, Object> targetProp = new HashMap<>();
        targetProp.put("type", "string");
        targetProp.put("description", "目标文件路径");
        properties.put("target_path", targetProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("source_path", "target_path"));

        server.addTool("dlqq_move_file",
            "移动文件到指定位置。如果目标文件已存在，会覆盖。自动创建目标目录。",
            inputSchema,
            arguments -> {
                String sourcePath = (String) arguments.get("source_path");
                String targetPath = (String) arguments.get("target_path");

                if (sourcePath == null || sourcePath.trim().isEmpty()) {
                    return errorResult("源文件路径不能为空");
                }
                if (targetPath == null || targetPath.trim().isEmpty()) {
                    return errorResult("目标文件路径不能为空");
                }

                try {
                    FileTools.moveFile(sourcePath, targetPath);
                    return textResult("文件移动成功");
                } catch (IOException e) {
                    return errorResult("移动失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 8.5 dlqq_rename_file - 重命名文件
     */
    private void registerRenameFileTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> sourceProp = new HashMap<>();
        sourceProp.put("type", "string");
        sourceProp.put("description", "源文件路径");
        properties.put("source_path", sourceProp);

        Map<String, Object> nameProp = new HashMap<>();
        nameProp.put("type", "string");
        nameProp.put("description", "新文件名（仅文件名，不包含路径）");
        properties.put("new_name", nameProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("source_path", "new_name"));

        server.addTool("dlqq_rename_file",
            "重命名文件，在同一目录下修改文件名。",
            inputSchema,
            arguments -> {
                String sourcePath = (String) arguments.get("source_path");
                String newName = (String) arguments.get("new_name");

                if (sourcePath == null || sourcePath.trim().isEmpty()) {
                    return errorResult("源文件路径不能为空");
                }
                if (newName == null || newName.trim().isEmpty()) {
                    return errorResult("新文件名不能为空");
                }

                try {
                    FileTools.renameFile(sourcePath, newName);
                    return textResult("文件重命名成功");
                } catch (IOException e) {
                    return errorResult("重命名失败: " + e.getMessage());
                }
            }
        );
    }

    // ==================== 目录操作工具 ====================

    /**
     * 9. dlqq_mdir - 创建文件夹
     */
    private void registerCreateDirTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> pathProp = new HashMap<>();
        pathProp.put("type", "string");
        pathProp.put("description", "文件夹路径（只创建最后一级，父目录必须已存在）");
        properties.put("dir_path", pathProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("dir_path"));

        server.addTool("dlqq_mdir",
            "创建指定文件夹。如果父目录不存在会报错，请使用 dlqq_mdirs 创建多级目录。",
            inputSchema,
            arguments -> {
                String dirPath = (String) arguments.get("dir_path");

                if (dirPath == null || dirPath.trim().isEmpty()) {
                    return errorResult("文件夹路径不能为空");
                }

                try {
                    FileTools.createDirectory(dirPath);
                    return textResult("文件夹创建成功: " + dirPath);
                } catch (IOException e) {
                    return errorResult("创建文件夹失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 10. dlqq_mdirs - 创建文件夹（包括父文件夹）
     */
    private void registerCreateDirsTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> pathProp = new HashMap<>();
        pathProp.put("type", "string");
        pathProp.put("description", "文件夹路径（包括所有父文件夹一起创建）");
        properties.put("dir_path", pathProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("dir_path"));

        server.addTool("dlqq_mdirs",
            "创建指定文件夹，如果不存在会自动创建所有父文件夹。",
            inputSchema,
            arguments -> {
                String dirPath = (String) arguments.get("dir_path");

                if (dirPath == null || dirPath.trim().isEmpty()) {
                    return errorResult("文件夹路径不能为空");
                }

                try {
                    FileTools.createDirectories(dirPath);
                    return textResult("文件夹创建成功: " + dirPath);
                } catch (IOException e) {
                    return errorResult("创建文件夹失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 11. dlqq_delete_dir - 删除文件夹（带确认对话框）
     */
    private void registerDeleteDirTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> pathProp = new HashMap<>();
        pathProp.put("type", "string");
        pathProp.put("description", "要删除的文件夹路径（会递归删除所有内容）");
        properties.put("dir_path", pathProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("dir_path"));

        server.addTool("dlqq_delete_dir",
            "删除指定文件夹及其所有内容。敏感操作，会弹出确认对话框显示目录包含的文件和子目录数量。",
            inputSchema,
            arguments -> {
                String dirPath = (String) arguments.get("dir_path");

                if (dirPath == null || dirPath.trim().isEmpty()) {
                    return errorResult("文件夹路径不能为空");
                }

                try {
                    boolean deleted = FileTools.deleteDirectory(dirPath);
                    if (deleted) {
                        return textResult("文件夹已删除: " + dirPath);
                    } else {
                        return textResult("用户取消了删除操作");
                    }
                } catch (IOException e) {
                    return errorResult("删除失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 12. dlqq_copy_dir - 复制文件夹
     */
    private void registerCopyDirTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> sourceProp = new HashMap<>();
        sourceProp.put("type", "string");
        sourceProp.put("description", "源文件夹路径");
        properties.put("source_path", sourceProp);

        Map<String, Object> targetProp = new HashMap<>();
        targetProp.put("type", "string");
        targetProp.put("description", "目标文件夹路径");
        properties.put("target_path", targetProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("source_path", "target_path"));

        server.addTool("dlqq_copy_dir",
            "复制文件夹到指定位置，递归复制所有子目录和文件。",
            inputSchema,
            arguments -> {
                String sourcePath = (String) arguments.get("source_path");
                String targetPath = (String) arguments.get("target_path");

                if (sourcePath == null || sourcePath.trim().isEmpty()) {
                    return errorResult("源文件夹路径不能为空");
                }
                if (targetPath == null || targetPath.trim().isEmpty()) {
                    return errorResult("目标文件夹路径不能为空");
                }

                try {
                    FileTools.copyDirectory(sourcePath, targetPath);
                    return textResult("文件夹复制成功");
                } catch (IOException e) {
                    return errorResult("复制失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 13. dlqq_move_dir - 移动文件夹
     */
    private void registerMoveDirTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> sourceProp = new HashMap<>();
        sourceProp.put("type", "string");
        sourceProp.put("description", "源文件夹路径");
        properties.put("source_path", sourceProp);

        Map<String, Object> targetProp = new HashMap<>();
        targetProp.put("type", "string");
        targetProp.put("description", "目标文件夹路径");
        properties.put("target_path", targetProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("source_path", "target_path"));

        server.addTool("dlqq_move_dir",
            "移动文件夹到指定位置。如果目标文件夹已存在，会先删除再移动。",
            inputSchema,
            arguments -> {
                String sourcePath = (String) arguments.get("source_path");
                String targetPath = (String) arguments.get("target_path");

                if (sourcePath == null || sourcePath.trim().isEmpty()) {
                    return errorResult("源文件夹路径不能为空");
                }
                if (targetPath == null || targetPath.trim().isEmpty()) {
                    return errorResult("目标文件夹路径不能为空");
                }

                try {
                    FileTools.moveDirectory(sourcePath, targetPath);
                    return textResult("文件夹移动成功");
                } catch (IOException e) {
                    return errorResult("移动失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 13.5 dlqq_rename_dir - 重命名文件夹
     */
    private void registerRenameDirTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> sourceProp = new HashMap<>();
        sourceProp.put("type", "string");
        sourceProp.put("description", "源文件夹路径");
        properties.put("source_path", sourceProp);

        Map<String, Object> nameProp = new HashMap<>();
        nameProp.put("type", "string");
        nameProp.put("description", "新文件夹名（仅文件夹名，不包含路径）");
        properties.put("new_name", nameProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("source_path", "new_name"));

        server.addTool("dlqq_rename_dir",
            "重命名文件夹，在同一目录下修改文件夹名。",
            inputSchema,
            arguments -> {
                String sourcePath = (String) arguments.get("source_path");
                String newName = (String) arguments.get("new_name");

                if (sourcePath == null || sourcePath.trim().isEmpty()) {
                    return errorResult("源文件夹路径不能为空");
                }
                if (newName == null || newName.trim().isEmpty()) {
                    return errorResult("新文件夹名不能为空");
                }

                try {
                    FileTools.renameDirectory(sourcePath, newName);
                    return textResult("文件夹重命名成功");
                } catch (IOException e) {
                    return errorResult("重命名失败: " + e.getMessage());
                }
            }
        );
    }

    // ==================== 二进制文件操作工具 ====================

    /**
     * 14. dlqq_read_bin_file - 读取二进制文件为十六进制
     */
    private void registerReadBinFileTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> pathProp = new HashMap<>();
        pathProp.put("type", "string");
        pathProp.put("description", "二进制文件路径");
        properties.put("file_path", pathProp);

        Map<String, Object> startProp = new HashMap<>();
        startProp.put("type", "integer");
        startProp.put("description", "起始字节（0-based，默认为0）");
        properties.put("start_byte", startProp);

        Map<String, Object> endProp = new HashMap<>();
        endProp.put("type", "integer");
        endProp.put("description", "结束字节（0-based，包含，-1表示到文件末尾）");
        properties.put("end_byte", endProp);

        Map<String, Object> bytesProp = new HashMap<>();
        bytesProp.put("type", "integer");
        bytesProp.put("description", "读取字节数（与end_byte互斥，end_byte优先）");
        properties.put("read_bytes", bytesProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("file_path"));

        server.addTool("dlqq_read_bin_file",
            "读取二进制文件并将字节转换为十六进制文本返回。支持通过start_byte和end_byte指定范围，或通过start_byte和read_bytes指定起始字节和读取字节数。",
            inputSchema,
            arguments -> {
                String filePath = (String) arguments.get("file_path");
                Long startByte = parseLong(arguments.get("start_byte"), 0L);
                Long endByte = parseLong(arguments.get("end_byte"), -1L);
                Long readBytes = parseLong(arguments.get("read_bytes"), 0L);

                if (filePath == null || filePath.trim().isEmpty()) {
                    return errorResult("文件路径不能为空");
                }

                try {
                    String hexContent = FileTools.readBinaryFileAsHex(filePath, startByte, endByte, readBytes);
                    return textResult(hexContent);
                } catch (IOException e) {
                    return errorResult("读取文件失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 15. dlqq_write_bin_file - 将十六进制写入二进制文件
     */
    private void registerWriteBinFileTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> pathProp = new HashMap<>();
        pathProp.put("type", "string");
        pathProp.put("description", "目标文件路径");
        properties.put("file_path", pathProp);

        Map<String, Object> hexProp = new HashMap<>();
        hexProp.put("type", "string");
        hexProp.put("description", "十六进制文本（可包含空格、换行，会自动去除）");
        properties.put("hex_content", hexProp);

        Map<String, Object> appendProp = new HashMap<>();
        appendProp.put("type", "boolean");
        appendProp.put("description", "是否追加写入（默认false，即覆盖写入）");
        properties.put("append", appendProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("file_path", "hex_content"));

        server.addTool("dlqq_write_bin_file",
            "将十六进制文本转换为二进制并写入文件。支持包含空格和换行的十六进制文本。支持覆盖或追加写入。",
            inputSchema,
            arguments -> {
                String filePath = (String) arguments.get("file_path");
                String hexContent = (String) arguments.get("hex_content");
                Boolean append = (Boolean) arguments.getOrDefault("append", false);

                if (filePath == null || filePath.trim().isEmpty()) {
                    return errorResult("文件路径不能为空");
                }
                if (hexContent == null || hexContent.trim().isEmpty()) {
                    return errorResult("十六进制内容不能为空");
                }

                try {
                    FileTools.writeBinaryFileFromHex(filePath, hexContent, append);
                    String mode = append ? "追加" : "覆盖";
                    return textResult(mode + "写入二进制文件成功: " + filePath);
                } catch (IOException e) {
                    return errorResult("写入文件失败: " + e.getMessage());
                }
            }
        );
    }

    // ==================== 搜索工具 ====================

    /**
     * 16. dlqq_search_file - 搜索文件
     */
    private void registerSearchFileTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> pathProp = new HashMap<>();
        pathProp.put("type", "string");
        pathProp.put("description", "搜索目录路径");
        properties.put("search_path", pathProp);

        Map<String, Object> patternProp = new HashMap<>();
        patternProp.put("type", "string");
        patternProp.put("description", "搜索模式（文件名或正则表达式）");
        properties.put("pattern", patternProp);

        Map<String, Object> modeProp = new HashMap<>();
        modeProp.put("type", "string");
        modeProp.put("description", "模式：'text'（部分匹配）或 'regex'（正则表达式）");
        modeProp.put("enum", Arrays.asList("text", "regex"));
        properties.put("mode", modeProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("search_path", "pattern"));

        server.addTool("dlqq_search_file",
            "在指定目录下查找文件。支持部分匹配（文件名包含指定文本）或正则表达式匹配。",
            inputSchema,
            arguments -> {
                String searchPath = (String) arguments.get("search_path");
                String pattern = (String) arguments.get("pattern");
                String mode = (String) arguments.getOrDefault("mode", "text");

                if (searchPath == null || searchPath.trim().isEmpty()) {
                    return errorResult("搜索路径不能为空");
                }
                if (pattern == null || pattern.trim().isEmpty()) {
                    return errorResult("搜索模式不能为空");
                }

                try {
                    boolean useRegex = "regex".equalsIgnoreCase(mode);
                    List<String> results = FileTools.searchFiles(searchPath, pattern, useRegex);

                    if (results.isEmpty()) {
                        return textResult("未找到匹配的文件");
                    }

                    StringBuilder sb = new StringBuilder();
                    sb.append("找到 ").append(results.size()).append(" 个文件:\n");
                    for (int i = 0; i < results.size(); i++) {
                        sb.append(i + 1).append(". ").append(results.get(i)).append("\n");
                    }
                    return textResult(sb.toString());
                } catch (IOException e) {
                    return errorResult("搜索失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 17. dlqq_search_dir - 搜索文件夹
     */
    private void registerSearchDirTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> pathProp = new HashMap<>();
        pathProp.put("type", "string");
        pathProp.put("description", "搜索目录路径");
        properties.put("search_path", pathProp);

        Map<String, Object> patternProp = new HashMap<>();
        patternProp.put("type", "string");
        patternProp.put("description", "搜索模式（文件夹名或正则表达式）");
        properties.put("pattern", patternProp);

        Map<String, Object> modeProp = new HashMap<>();
        modeProp.put("type", "string");
        modeProp.put("description", "模式：'text'（部分匹配）或 'regex'（正则表达式）");
        modeProp.put("enum", Arrays.asList("text", "regex"));
        properties.put("mode", modeProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("search_path", "pattern"));

        server.addTool("dlqq_search_dir",
            "在指定目录下查找文件夹。支持部分匹配（文件夹名包含指定文本）或正则表达式匹配。",
            inputSchema,
            arguments -> {
                String searchPath = (String) arguments.get("search_path");
                String pattern = (String) arguments.get("pattern");
                String mode = (String) arguments.getOrDefault("mode", "text");

                if (searchPath == null || searchPath.trim().isEmpty()) {
                    return errorResult("搜索路径不能为空");
                }
                if (pattern == null || pattern.trim().isEmpty()) {
                    return errorResult("搜索模式不能为空");
                }

                try {
                    boolean useRegex = "regex".equalsIgnoreCase(mode);
                    List<String> results = FileTools.searchDirectories(searchPath, pattern, useRegex);

                    if (results.isEmpty()) {
                        return textResult("未找到匹配的文件夹");
                    }

                    StringBuilder sb = new StringBuilder();
                    sb.append("找到 ").append(results.size()).append(" 个文件夹:\n");
                    for (int i = 0; i < results.size(); i++) {
                        sb.append(i + 1).append(". ").append(results.get(i)).append("\n");
                    }
                    return textResult(sb.toString());
                } catch (IOException e) {
                    return errorResult("搜索失败: " + e.getMessage());
                }
            }
        );
    }

    // ==================== 对话框工具 ====================

    /**
     * 18. dlqq_dialog_text - 文本输入对话框
     */
    private void registerDialogTextTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> titleProp = new HashMap<>();
        titleProp.put("type", "string");
        titleProp.put("description", "对话框标题");
        properties.put("title", titleProp);

        Map<String, Object> messageProp = new HashMap<>();
        messageProp.put("type", "string");
        messageProp.put("description", "提示消息");
        properties.put("message", messageProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("title", "message"));

        server.addTool("dlqq_dialog_text",
            "向用户提问并等待用户输入文本。展示独立窗口供用户输入任意文本内容。",
            inputSchema,
            arguments -> {
                String title = (String) arguments.get("title");
                String message = (String) arguments.get("message");

                if (title == null || title.trim().isEmpty()) {
                    return errorResult("标题不能为空");
                }

                try {
                    DialogTools.DialogResult result = DialogTools.showTextInputDialog(title, message != null ? message : "");
                    if (result == null || !result.isConfirmed()) {
                        return textResult("用户取消了输入");
                    }
                    return textResult(result.getTextInput() != null ? result.getTextInput() : "");
                } catch (Exception e) {
                    return errorResult("对话框执行失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 19. dlqq_dialog_check - 确认对话框（是/否）
     */
    private void registerDialogCheckTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> titleProp = new HashMap<>();
        titleProp.put("type", "string");
        titleProp.put("description", "对话框标题");
        properties.put("title", titleProp);

        Map<String, Object> messageProp = new HashMap<>();
        messageProp.put("type", "string");
        messageProp.put("description", "确认消息内容");
        properties.put("message", messageProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("title", "message"));

        server.addTool("dlqq_dialog_check",
            "向用户提问并要求用户选择是或否。展示独立确认窗口。",
            inputSchema,
            arguments -> {
                String title = (String) arguments.get("title");
                String message = (String) arguments.get("message");

                if (title == null || title.trim().isEmpty()) {
                    return errorResult("标题不能为空");
                }

                try {
                    boolean confirmed = DialogTools.showConfirmDialog(title, message != null ? message : "");
                    return textResult(confirmed ? "是" : "否");
                } catch (Exception e) {
                    return errorResult("对话框执行失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 20. dlqq_dialog_file_path - 文件选择对话框
     */
    private void registerDialogFilePathTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> titleProp = new HashMap<>();
        titleProp.put("type", "string");
        titleProp.put("description", "对话框标题");
        properties.put("title", titleProp);

        Map<String, Object> extensionsProp = new HashMap<>();
        extensionsProp.put("type", "array");
        extensionsProp.put("description", "允许的文件扩展名列表（如 [\"txt\", \"java\"]）");
        properties.put("extensions", extensionsProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("title"));

        server.addTool("dlqq_dialog_file_path",
            "向用户提问并要求用户选择文件。展示独立文件选择窗口，返回选中的文件路径。",
            inputSchema,
            arguments -> {
                String title = (String) arguments.get("title");
                @SuppressWarnings("unchecked")
                List<String> extensions = (List<String>) arguments.get("extensions");

                if (title == null || title.trim().isEmpty()) {
                    title = "选择文件";
                }

                try {
                    String[] extArray = extensions != null ? extensions.toArray(new String[0]) : null;
                    String[] paths = DialogTools.showFileChooser(title, extArray, false);

                    if (paths.length == 0) {
                        return textResult("用户未选择文件");
                    }

                    return textResult(paths[0]);
                } catch (Exception e) {
                    return errorResult("文件选择失败: " + e.getMessage());
                }
            }
        );
    }

    // ==================== 串口工具 ====================

    /**
     * 22. dlqq_run_uart - 启动串口服务器
     */
    private void registerRunUartTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "integer");
        portProp.put("description", "服务器端口号（用于AI后续连接）");
        properties.put("port", portProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port"));

        server.addTool("dlqq_run_uart",
            "打开CH340串口工具，需要指定端口号，用于后续通讯，ip地址为localhost即可，打开后窗口保持运行，AI退出返回对话，继续与用户交互。后续交互使用localhost和指定端口与串口工具连接。",
            inputSchema,
            arguments -> {
                Integer port = parseInteger(arguments.get("port"), null);

                if (port == null) {
                    return errorResult("端口号必须指定");
                }
                if (port < 1 || port > 65535) {
                    return errorResult("端口号必须在 1~65535 范围内");
                }

                try {
                    String result = UartTools.runUart(port);
                    return textResult(result);
                } catch (Exception e) {
                    return errorResult("启动串口工具失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 23. dlqq_uart_get_com - 获取串口通道列表
     */
    private void registerUartGetComTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "integer");
        portProp.put("description", "串口工具的端口号（dlqq_run_uart 指定的端口号）");
        properties.put("port", portProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port"));

        server.addTool("dlqq_uart_get_com",
            "获取当前所有的串口通道。必须先指定要操作的串口工具的端口号。",
            inputSchema,
            arguments -> {
                Integer port = parseInteger(arguments.get("port"), null);

                if (port == null) {
                    return errorResult("必须指定端口号（port）");
                }
                if (port < 1 || port > 65535) {
                    return errorResult("端口号必须在 1~65535 范围内");
                }

                try {
                    String result = UartTools.getUartComList(port);
                    return textResult(result);
                } catch (Exception e) {
                    return errorResult("获取串口列表失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 23a. dlqq_uart_refresh_com - 刷新可用串口列表
     */
    private void registerUartRefreshComTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "integer");
        portProp.put("description", "串口工具的端口号（dlqq_run_uart 指定的端口号）");
        properties.put("port", portProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port"));

        server.addTool("dlqq_uart_refresh_com",
            "刷新可用串口列表。获取系统中当前可用的所有串口设备。",
            inputSchema,
            arguments -> {
                Integer port = parseInteger(arguments.get("port"), null);

                if (port == null) {
                    return errorResult("必须指定端口号（port）");
                }
                if (port < 1 || port > 65535) {
                    return errorResult("端口号必须在 1~65535 范围内");
                }

                try {
                    String result = UartTools.refreshComList(port);
                    return textResult(result);
                } catch (Exception e) {
                    return errorResult("刷新串口列表失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 24. dlqq_uart_open_com - 打开串口通道
     */
    private void registerUartOpenComTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "integer");
        portProp.put("description", "串口工具的端口号（dlqq_run_uart 指定的端口号）");
        properties.put("port", portProp);

        Map<String, Object> comProp = new HashMap<>();
        comProp.put("type", "string");
        comProp.put("description", "串口号（如 COM3、ttyUSB0、USB Serial等）");
        properties.put("com", comProp);

        Map<String, Object> baudRateProp = new HashMap<>();
        baudRateProp.put("type", "integer");
        baudRateProp.put("description", "波特率（0~2000000，默认1000000）");
        properties.put("baud_rate", baudRateProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port", "com"));

        server.addTool("dlqq_uart_open_com",
            "打开某个通道的串口，开始捕获串口日志数据。必须指定串口工具的端口号和串口号，波特率可选，默认为1000000。",
            inputSchema,
            arguments -> {
                Integer port = parseInteger(arguments.get("port"), null);
                String com = (String) arguments.get("com");
                Integer baudRate = parseInteger(arguments.get("baud_rate"), 1000000);

                if (port == null) {
                    return errorResult("必须指定端口号（port）");
                }
                if (port < 1 || port > 65535) {
                    return errorResult("端口号必须在 1~65535 范围内");
                }
                if (com == null || com.trim().isEmpty()) {
                    return errorResult("串口号必须指定");
                }
                if (baudRate < 0 || baudRate > 2000000) {
                    return errorResult("波特率必须在 0~2000000 范围内");
                }

                try {
                    String result = UartTools.openUartCom(port, com, baudRate);
                    return textResult(result);
                } catch (Exception e) {
                    return errorResult("打开串口失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 24a. dlqq_uart_reopen_com - 重新打开串口（用于重新设置波特率）
     */
    private void registerUartReopenComTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "integer");
        portProp.put("description", "串口工具的端口号（dlqq_run_uart 指定的端口号）");
        properties.put("port", portProp);

        Map<String, Object> comProp = new HashMap<>();
        comProp.put("type", "string");
        comProp.put("description", "串口号（如 COM3、ttyUSB0等）");
        properties.put("com", comProp);

        Map<String, Object> baudRateProp = new HashMap<>();
        baudRateProp.put("type", "integer");
        baudRateProp.put("description", "波特率（0~2000000，默认1000000）");
        properties.put("baud_rate", baudRateProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port", "com"));

        server.addTool("dlqq_uart_reopen_com",
            "重新打开串口，用于更改波特率。会先关闭当前串口，然后用新的波特率重新打开。",
            inputSchema,
            arguments -> {
                Integer port = parseInteger(arguments.get("port"), null);
                String com = (String) arguments.get("com");
                Integer baudRate = parseInteger(arguments.get("baud_rate"), 1000000);

                if (port == null) {
                    return errorResult("必须指定端口号（port）");
                }
                if (port < 1 || port > 65535) {
                    return errorResult("端口号必须在 1~65535 范围内");
                }
                if (com == null || com.trim().isEmpty()) {
                    return errorResult("串口号必须指定");
                }
                if (baudRate < 0 || baudRate > 2000000) {
                    return errorResult("波特率必须在 0~2000000 范围内");
                }

                try {
                    String result = UartTools.reopenUartCom(port, com, baudRate);
                    return textResult(result);
                } catch (Exception e) {
                    return errorResult("重新打开串口失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 25. dlqq_uart_close_com - 关闭串口通道
     */
    private void registerUartCloseComTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "integer");
        portProp.put("description", "串口工具的端口号（dlqq_run_uart 指定的端口号）");
        properties.put("port", portProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port"));

        server.addTool("dlqq_uart_close_com",
            "关闭指定串口工具的当前打开的串口通道。",
            inputSchema,
            arguments -> {
                Integer port = parseInteger(arguments.get("port"), null);

                if (port == null) {
                    return errorResult("必须指定端口号（port）");
                }
                if (port < 1 || port > 65535) {
                    return errorResult("端口号必须在 1~65535 范围内");
                }

                try {
                    String result = UartTools.closeUartCom(port);
                    return textResult(result);
                } catch (Exception e) {
                    return errorResult("关闭串口失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 25a. dlqq_uart_send_text - 发送文本数据到串口
     */
    private void registerUartSendTextTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "integer");
        portProp.put("description", "串口工具的端口号（dlqq_run_uart 指定的端口号）");
        properties.put("port", portProp);

        Map<String, Object> textProp = new HashMap<>();
        textProp.put("type", "string");
        textProp.put("description", "要发送的文本内容");
        properties.put("text", textProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port", "text"));

        server.addTool("dlqq_uart_send_text",
            "发送文本数据到指定串口。发送的内容会同时存入缓冲区，方便后续查看。",
            inputSchema,
            arguments -> {
                Integer port = parseInteger(arguments.get("port"), null);
                String text = (String) arguments.get("text");

                if (port == null) {
                    return errorResult("必须指定端口号（port）");
                }
                if (port < 1 || port > 65535) {
                    return errorResult("端口号必须在 1~65535 范围内");
                }
                if (text == null || text.trim().isEmpty()) {
                    return errorResult("发送内容不能为空");
                }

                try {
                    String result = UartTools.sendUartText(port, text);
                    return textResult(result);
                } catch (Exception e) {
                    return errorResult("发送文本失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 25b. dlqq_uart_send_hex - 发送16进制数据到串口
     */
    private void registerUartSendHexTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "integer");
        portProp.put("description", "串口工具的端口号（dlqq_run_uart 指定的端口号）");
        properties.put("port", portProp);

        Map<String, Object> hexArrayProp = new HashMap<>();
        hexArrayProp.put("type", "array");
        hexArrayProp.put("description", "16进制字符串数组（如 [\"FF\", \"01\", \"02\"]）");
        properties.put("hex_array", hexArrayProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port", "hex_array"));

        server.addTool("dlqq_uart_send_hex",
            "发送16进制数据到指定串口。每个字符串应为偶数长度的16进制字符（如 \"FF01\"）。发送的内容会同时存入缓冲区。",
            inputSchema,
            arguments -> {
                Integer port = parseInteger(arguments.get("port"), null);
                @SuppressWarnings("unchecked")
                List<String> hexArray = (List<String>) arguments.get("hex_array");

                if (port == null) {
                    return errorResult("必须指定端口号（port）");
                }
                if (port < 1 || port > 65535) {
                    return errorResult("端口号必须在 1~65535 范围内");
                }
                if (hexArray == null || hexArray.isEmpty()) {
                    return errorResult("16进制数据不能为空");
                }

                try {
                    String result = UartTools.sendUartHex(port, hexArray);
                    return textResult(result);
                } catch (Exception e) {
                    return errorResult("发送16进制数据失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 26. dlqq_uart_get_log - 获取串口日志数据
     * 根据当前显示格式设置（通过 dlqq_uart_switch_text_hex 切换）返回文本或HEX格式
     * 支持3种模式：
     * 模式1: start_line + end_line - 直接读取已有数据
     * 模式2: start_line + line_count - 直接读取已有数据
     * 模式3: start_line + timeout_ms + line_count - 等待模式读取新数据
     * 如需固定格式，请使用 dlqq_uart_get_log_text（强制文本）或 dlqq_uart_get_log_hex（强制HEX）
     */
    private void registerUartGetLogTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "integer");
        portProp.put("description", "串口工具的端口号（dlqq_run_uart 指定的端口号）");
        properties.put("port", portProp);

        Map<String, Object> startLineProp = new HashMap<>();
        startLineProp.put("type", "integer");
        startLineProp.put("description", "起始行（1-based，模式1/2必需，模式3默认为-1表示从最后一行开始读取新数据）");
        properties.put("start_line", startLineProp);

        Map<String, Object> endLineProp = new HashMap<>();
        endLineProp.put("type", "integer");
        endLineProp.put("description", "结束行（1-based，模式1使用）");
        properties.put("end_line", endLineProp);

        Map<String, Object> lineCountProp = new HashMap<>();
        lineCountProp.put("type", "integer");
        lineCountProp.put("description", "读取行数（模式2和模式3使用，模式3必需）");
        properties.put("line_count", lineCountProp);

        Map<String, Object> timeoutProp = new HashMap<>();
        timeoutProp.put("type", "integer");
        timeoutProp.put("description", "超时时间（毫秒，模式3使用，默认0表示不等待）");
        properties.put("timeout_ms", timeoutProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port"));

        server.addTool("dlqq_uart_get_log",
            "获取串口缓冲区日志数据。支持3种模式：\n" +
            "模式1: 指定start_line和end_line，直接读取已有数据\n" +
            "模式2: 指定start_line和line_count，直接读取已有数据\n" +
            "模式3: 指定timeout_ms和line_count（start_line默认为-1），等待读取新数据\n" +
            "返回格式根据 dlqq_uart_switch_text_hex 设置（文本/HEX），保证与UI显示一致\n" +
            "如需固定格式，请使用 dlqq_uart_get_log_text（强制文本）或 dlqq_uart_get_log_hex（强制HEX）",
            inputSchema,
            arguments -> {
                Integer port = parseInteger(arguments.get("port"), null);
                Integer startLine = parseInteger(arguments.get("start_line"), null);
                Integer endLine = parseInteger(arguments.get("end_line"), null);
                Integer lineCount = parseInteger(arguments.get("line_count"), null);
                Integer timeoutMs = parseInteger(arguments.get("timeout_ms"), 0);

                if (port == null) {
                    return errorResult("必须指定端口号（port）");
                }
                if (port < 1 || port > 65535) {
                    return errorResult("端口号必须在 1~65535 范围内");
                }

                try {
                    String result = UartTools.getUartLog(port, startLine, endLine, lineCount, timeoutMs);
                    return textResult(result);
                } catch (Exception e) {
                    return errorResult("获取串口数据失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 26a. dlqq_uart_get_log_text - 获取串口日志数据（强制文本格式）
     * 支持3种模式（同dlqq_uart_get_log）
     */
    private void registerUartGetLogTextTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "integer");
        portProp.put("description", "串口工具的端口号（dlqq_run_uart 指定的端口号）");
        properties.put("port", portProp);

        Map<String, Object> startLineProp = new HashMap<>();
        startLineProp.put("type", "integer");
        startLineProp.put("description", "起始行（1-based，模式1/2必需，模式3默认为-1表示从最后一行开始读取新数据）");
        properties.put("start_line", startLineProp);

        Map<String, Object> endLineProp = new HashMap<>();
        endLineProp.put("type", "integer");
        endLineProp.put("description", "结束行（1-based，模式1使用）");
        properties.put("end_line", endLineProp);

        Map<String, Object> lineCountProp = new HashMap<>();
        lineCountProp.put("type", "integer");
        lineCountProp.put("description", "读取行数（模式2和模式3使用，模式3必需）");
        properties.put("line_count", lineCountProp);

        Map<String, Object> timeoutProp = new HashMap<>();
        timeoutProp.put("type", "integer");
        timeoutProp.put("description", "超时时间（毫秒，模式3使用，默认0表示不等待）");
        properties.put("timeout_ms", timeoutProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port"));

        server.addTool("dlqq_uart_get_log_text",
            "获取串口缓冲区日志数据（强制文本格式）。支持3种模式：\n" +
            "模式1: 指定start_line和end_line，直接读取已有数据\n" +
            "模式2: 指定start_line和line_count，直接读取已有数据\n" +
            "模式3: 指定timeout_ms和line_count（start_line默认为-1），等待读取新数据\n" +
            "无论当前显示格式设置如何，始终返回文本格式",
            inputSchema,
            arguments -> {
                Integer port = parseInteger(arguments.get("port"), null);
                Integer startLine = parseInteger(arguments.get("start_line"), null);
                Integer endLine = parseInteger(arguments.get("end_line"), null);
                Integer lineCount = parseInteger(arguments.get("line_count"), null);
                Integer timeoutMs = parseInteger(arguments.get("timeout_ms"), 0);

                if (port == null) {
                    return errorResult("必须指定端口号（port）");
                }
                if (port < 1 || port > 65535) {
                    return errorResult("端口号必须在 1~65535 范围内");
                }

                try {
                    String result = UartTools.getUartLogText(port, startLine, endLine, lineCount, timeoutMs);
                    return textResult(result);
                } catch (Exception e) {
                    return errorResult("获取串口数据失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 26b. dlqq_uart_get_log_hex - 获取串口日志数据（强制HEX格式）
     * 支持3种模式（同dlqq_uart_get_log）
     */
    private void registerUartGetLogHexTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "integer");
        portProp.put("description", "串口工具的端口号（dlqq_run_uart 指定的端口号）");
        properties.put("port", portProp);

        Map<String, Object> startLineProp = new HashMap<>();
        startLineProp.put("type", "integer");
        startLineProp.put("description", "起始行（1-based，模式1/2必需，模式3默认为-1表示从最后一行开始读取新数据）");
        properties.put("start_line", startLineProp);

        Map<String, Object> endLineProp = new HashMap<>();
        endLineProp.put("type", "integer");
        endLineProp.put("description", "结束行（1-based，模式1使用）");
        properties.put("end_line", endLineProp);

        Map<String, Object> lineCountProp = new HashMap<>();
        lineCountProp.put("type", "integer");
        lineCountProp.put("description", "读取行数（模式2和模式3使用，模式3必需）");
        properties.put("line_count", lineCountProp);

        Map<String, Object> timeoutProp = new HashMap<>();
        timeoutProp.put("type", "integer");
        timeoutProp.put("description", "超时时间（毫秒，模式3使用，默认0表示不等待）");
        properties.put("timeout_ms", timeoutProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port"));

        server.addTool("dlqq_uart_get_log_hex",
            "获取串口缓冲区日志数据（强制HEX格式）。支持3种模式：\n" +
            "模式1: 指定start_line和end_line，直接读取已有数据\n" +
            "模式2: 指定start_line和line_count，直接读取已有数据\n" +
            "模式3: 指定timeout_ms和line_count（start_line默认为-1），等待读取新数据\n" +
            "无论当前显示格式设置如何，始终返回HEX格式",
            inputSchema,
            arguments -> {
                Integer port = parseInteger(arguments.get("port"), null);
                Integer startLine = parseInteger(arguments.get("start_line"), null);
                Integer endLine = parseInteger(arguments.get("end_line"), null);
                Integer lineCount = parseInteger(arguments.get("line_count"), null);
                Integer timeoutMs = parseInteger(arguments.get("timeout_ms"), 0);

                if (port == null) {
                    return errorResult("必须指定端口号（port）");
                }
                if (port < 1 || port > 65535) {
                    return errorResult("端口号必须在 1~65535 范围内");
                }

                try {
                    String result = UartTools.getUartLogHex(port, startLine, endLine, lineCount, timeoutMs);
                    return textResult(result);
                } catch (Exception e) {
                    return errorResult("获取串口数据失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 27. dlqq_uart_clear_text - 清除串口数据
     */
    private void registerUartClearTextTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "integer");
        portProp.put("description", "串口工具的端口号（dlqq_run_uart 指定的端口号）");
        properties.put("port", portProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port"));

        server.addTool("dlqq_uart_clear_text",
            "清除指定串口工具接收的数据。",
            inputSchema,
            arguments -> {
                Integer port = parseInteger(arguments.get("port"), null);

                if (port == null) {
                    return errorResult("必须指定端口号（port）");
                }
                if (port < 1 || port > 65535) {
                    return errorResult("端口号必须在 1~65535 范围内");
                }

                try {
                    String result = UartTools.clearUartText(port);
                    return textResult(result);
                } catch (Exception e) {
                    return errorResult("清除串口数据失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 27a. dlqq_uart_get_line_count - 获取缓冲区行数
     */
    private void registerUartGetLineCountTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "integer");
        portProp.put("description", "串口工具的端口号（dlqq_run_uart 指定的端口号）");
        properties.put("port", portProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port"));

        server.addTool("dlqq_uart_get_line_count",
            "获取指定串口工具缓冲区当前的日志行数。",
            inputSchema,
            arguments -> {
                Integer port = parseInteger(arguments.get("port"), null);

                if (port == null) {
                    return errorResult("必须指定端口号（port）");
                }
                if (port < 1 || port > 65535) {
                    return errorResult("端口号必须在 1~65535 范围内");
                }

                try {
                    String result = UartTools.getLineCount(port);
                    return textResult(result);
                } catch (Exception e) {
                    return errorResult("获取行数失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 28. dlqq_uart_set_text_size - 设置串口缓冲区大小
     */
    private void registerUartSetTextSizeTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "integer");
        portProp.put("description", "串口工具的端口号（dlqq_run_uart 指定的端口号）");
        properties.put("port", portProp);

        Map<String, Object> sizeProp = new HashMap<>();
        sizeProp.put("type", "integer");
        sizeProp.put("description", "缓冲区大小（字节），默认5MB（5242880字节）");
        properties.put("size_bytes", sizeProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port", "size_bytes"));

        server.addTool("dlqq_uart_set_text_size",
            "设置指定串口工具的数据缓冲区大小，单位字节。大小无限制，默认5M Byte大小。超出缓冲区，最早接收到的串口数据需要丢弃。",
            inputSchema,
            arguments -> {
                Integer port = parseInteger(arguments.get("port"), null);
                Integer sizeBytes = parseInteger(arguments.get("size_bytes"), null);

                if (port == null) {
                    return errorResult("必须指定端口号（port）");
                }
                if (port < 1 || port > 65535) {
                    return errorResult("端口号必须在 1~65535 范围内");
                }
                if (sizeBytes == null) {
                    return errorResult("缓冲区大小必须指定");
                }
                if (sizeBytes <= 0) {
                    return errorResult("缓冲区大小必须大于0");
                }

                try {
                    String result = UartTools.setUartBufferSize(port, sizeBytes);
                    return textResult(result);
                } catch (Exception e) {
                    return errorResult("设置缓冲区大小失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 32. dlqq_uart_switch_text_hex - 切换日志显示格式（文本/HEX）
     */
    private void registerUartSwitchTextHexTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "integer");
        portProp.put("description", "串口工具的端口号（dlqq_run_uart 指定的端口号）");
        properties.put("port", portProp);

        Map<String, Object> formatProp = new HashMap<>();
        formatProp.put("type", "string");
        formatProp.put("description", "显示格式：'text' 表示文本，'hex' 表示HEX");
        properties.put("format", formatProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port", "format"));

        server.addTool("dlqq_uart_switch_text_hex",
            "切换串口日志的显示格式。切换后，UI日志显示和 dlqq_uart_get_log 返回的数据格式都会相应改变。",
            inputSchema,
            arguments -> {
                Integer port = parseInteger(arguments.get("port"), null);
                String format = parseString(arguments.get("format"), null);

                if (port == null) {
                    return errorResult("必须指定端口号（port）");
                }
                if (port < 1 || port > 65535) {
                    return errorResult("端口号必须在 1~65535 范围内");
                }
                if (format == null || format.trim().isEmpty()) {
                    return errorResult("必须指定显示格式（format），'text' 或 'hex'");
                }

                try {
                    String result = UartTools.switchDisplayFormat(port, format);
                    return textResult(result);
                } catch (Exception e) {
                    return errorResult("切换显示格式失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 33. dlqq_exit_uart - 退出串口工具
     */
    private void registerExitUartTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "integer");
        portProp.put("description", "串口工具的端口号（dlqq_run_uart 指定的端口号）");
        properties.put("port", portProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port"));

        server.addTool("dlqq_exit_uart",
            "关闭指定端口号的串口工具窗口及其进程，释放所有资源。调用后该端口的串口工具将完全停止运行。",
            inputSchema,
            arguments -> {
                Integer port = parseInteger(arguments.get("port"), null);

                if (port == null) {
                    return errorResult("必须指定端口号（port）");
                }
                if (port < 1 || port > 65535) {
                    return errorResult("端口号必须在 1~65535 范围内");
                }

                try {
                    String result = UartTools.exitUart(port);
                    return textResult(result);
                } catch (Exception e) {
                    return errorResult("关闭串口工具失败: " + e.getMessage());
                }
            }
        );
    }

    // ==================== 工具方法 ====================

    /**
     * 创建文本结果
     */
    private CallToolResult textResult(String text) {
        CallToolResult result = new CallToolResult();
        result.setError(false);
        List<ToolContent> content = new ArrayList<>();
        content.add(ToolContent.text(text));
        result.setContent(content);
        return result;
    }

    /**
     * 创建错误结果
     */
    private CallToolResult errorResult(String error) {
        CallToolResult result = new CallToolResult();
        result.setError(true);
        List<ToolContent> content = new ArrayList<>();
        content.add(ToolContent.error(error));
        result.setContent(content);
        return result;
    }

    /**
     * 解析整数参数（兼容 Long 和 Integer）
     */
    private Integer parseInteger(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Long) {
            return ((Long) value).intValue();
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    /**
     * 解析长整数参数（兼容 Long 和 Integer）
     */
    private Long parseLong(Object value, Long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return defaultValue;
    }

    /**
     * 解析字符串参数
     */
    private String parseString(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof String) {
            return (String) value;
        }
        return value.toString();
    }
}

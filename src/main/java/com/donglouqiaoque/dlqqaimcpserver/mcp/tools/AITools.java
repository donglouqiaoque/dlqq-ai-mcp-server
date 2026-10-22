package com.donglouqiaoque.dlqqaimcpserver.mcp.tools;

import com.donglouqiaoque.dlqqaimcpserver.mcp.protocol.McpSchema.*;
import com.donglouqiaoque.dlqqaimcpserver.mcp.server.McpServer;

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
        registerClearFileTool(server); // dlqq_clear_file
        registerAppendFileTool(server);           // dlqq_append_file
        registerRemoveLinesTool(server);          // dlqq_remove_lines_in_file
        registerReplaceLinesTool(server);         // dlqq_replace_lines_in_file
        registerInsertTextTool(server);           // dlqq_insert_text_in_file
        registerReplaceTextTool(server);          // dlqq_replace_text_in_file
        registerRemoveTextInFileTool(server);   // dlqq_remove_text_in_file
        registerReplaceTextInDirTool(server);   // dlqq_replace_text_in_dir
        registerDeleteFileTool(server);           // dlqq_delete_file
        registerCopyFileTool(server);             // dlqq_copy_file
        registerMoveFileTool(server);             // dlqq_move_file
        registerRenameFileTool(server);           // dlqq_rename_file

        // ========== 目录操作工具 (12-19) ==========
        registerCreateDirTool(server);            // dlqq_mdir
        registerCreateDirsTool(server);           // dlqq_mdirs
        registerDeleteDirTool(server);            // dlqq_delete_dir
        registerCopyDirTool(server);              // dlqq_copy_dir
        registerMoveDirTool(server);              // dlqq_move_dir
        registerRenameDirTool(server);              // dlqq_rename_dir
        registerListDirTreeTool(server);            // dlqq_list_dir_tree
        registerListDirFilesTool(server);         // dlqq_list_dir_files

        // ========== 二进制文件操作工具 (20-21) ==========
        registerReadBinFileTool(server);          // dlqq_read_bin_file
        registerWriteBinFileTool(server);         // dlqq_write_bin_file

        // ========== BOM 控制工具 (20-21) ==========
        registerAddUnicodeBomTool(server);        // dlqq_add_unicode_bom
        registerRemoveUnicodeBomTool(server);     // dlqq_remove_unicode_bom

        // ========== 搜索工具 (25-28) ==========
        registerSearchFileTool(server);           // dlqq_search_file
        registerSearchDirTool(server);            // dlqq_search_dir
        registerSearchTextInDirTool(server);      // dlqq_search_text_in_dir
        registerSearchTextInFileTool(server);     // dlqq_search_text_in_file

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
        registerRerunUartTool(server);            // dlqq_rerun_uart

        // ========== BLE库管理工具 (30-33) ==========
        registerBleLibHasTool(server);          // dlqq_ble_lib_has
        registerBleLibInstallTool(server);      // dlqq_ble_lib_install
        registerBleLibUpdateTool(server);       // dlqq_ble_lib_update
        registerBleReleaseScriptTool(server);   // dlqq_ble_release_script

        // ========== BLE蓝牙工具 (34-45) ==========
        registerRunBleTool(server);               // dlqq_run_ble
        registerBleScanTool(server);              // dlqq_ble_scan
        registerBleGetDevicesTool(server);        // dlqq_ble_get_devices
        registerBleConnectTool(server);           // dlqq_ble_connect
        registerBleDisconnectTool(server);        // dlqq_ble_disconnect
        registerBleDiscoverTool(server);          // dlqq_ble_discover
        registerBleReadTool(server);              // dlqq_ble_read
        registerBleWriteTextTool(server);         // dlqq_ble_write_text
        registerBleWriteHexTool(server);          // dlqq_ble_write_hex
        registerBleSubscribeTool(server);         // dlqq_ble_subscribe
        registerBleGetLogTool(server);            // dlqq_ble_get_log
        registerBleGetLogTextTool(server);        // dlqq_ble_get_log_text
        registerBleGetLogHexTool(server);         // dlqq_ble_get_log_hex
        registerBleClearTool(server);             // dlqq_ble_clear
        registerBleGetLineCountTool(server);      // dlqq_ble_get_line_count
        registerBleSetBufferSizeTool(server);     // dlqq_ble_set_buffer_size
        registerBleSwitchTextHexTool(server);     // dlqq_ble_switch_text_hex
        registerExitBleTool(server);              // dlqq_exit_ble
        registerRerunBleTool(server);             // dlqq_rerun_ble

        // ========== 系统信息工具 ==========
        registerGetMcpVersionTool(server);          // dlqq_get_mcp_version
        registerCheckBluetoothTool(server);          // dlqq_check_bluetooth_open

        // ========== MCP数据管理工具 ==========
        registerClearMcpDataTool(server);           // dlqq_clear_mcp_data
        registerClearMcpCacheTool(server);          // dlqq_clear_mcp_cache
        registerClearMcpLogTool(server);            // dlqq_clear_mcp_log
        registerResetMcpPropertiesTool(server);       // dlqq_reset_mcp_properties
        registerGetMcpPropertiesTool(server);       // dlqq_get_mcp_properties
        registerSetMcpPropertiesTool(server);       // dlqq_set_mcp_properties

        // ========== Office文件工具 ==========
        registerReadOfficeFileTool(server);         // dlqq_read_office_file
        registerConvertOfficeFileTool(server);      // dlqq_convert_office_file

        // ========== 压缩/解压工具 ==========
        registerZipTool(server);                    // dlqq_zip
        registerUnzipTool(server);                  // dlqq_unzip

        // ========== 计算器工具 ==========
        CalculatorTools.registerTools(server);      // dlqq_calculator_math, dlqq_calculator_programmer, dlqq_calculator_function

        // ========== 命令执行工具 ==========
        RunCommandTools.registerTools(server);      // dlqq_run_command

        // ========== USB工具 (88-101) ==========
        registerRunUsbTool(server);                 // dlqq_run_usb
        registerUsbScanTool(server);                // dlqq_usb_scan
        registerUsbGetDevicesTool(server);            // dlqq_usb_get_devices
        registerUsbOpenTool(server);                // dlqq_usb_open
        registerUsbCloseTool(server);               // dlqq_usb_close
        registerUsbSendTextTool(server);            // dlqq_usb_send_text
        registerUsbSendHexTool(server);             // dlqq_usb_send_hex
        registerUsbGetLogTool(server);              // dlqq_usb_get_log
        registerUsbGetLogTextTool(server);          // dlqq_usb_get_log_text
        registerUsbGetLogHexTool(server);           // dlqq_usb_get_log_hex
        registerUsbClearTool(server);               // dlqq_usb_clear
        registerUsbGetLineCountTool(server);        // dlqq_usb_get_line_count
        registerUsbSwitchTextHexTool(server);       // dlqq_usb_switch_text_hex
        registerExitUsbTool(server);                  // dlqq_exit_usb
        registerRerunUsbTool(server);                 // dlqq_rerun_usb

        // ========== 项目构建和芯片操作工具 ==========
        registerBuildProjectTool(server);           // dlqq_build_project
        registerRebuildProjectTool(server);         // dlqq_rebuild_project
        registerChipEraseTool(server);              // dlqq_chip_erase
        registerChipDownloadTool(server);           // dlqq_chip_download

        // ========== 组合脚本执行工具 ==========
        registerEraseDownloadTool(server);          // dlqq_erase_download
        registerBuildEraseDownloadTool(server);   // dlqq_build_erase_download
        registerRebuildEraseDownloadTool(server); // dlqq_rebuild_erase_download

        // ========== 运行调试工具 ==========
        registerRunProjectTool(server);             // dlqq_run_project
        registerDebugProjectTool(server);           // dlqq_debug_project
        registerBuildRunProjectTool(server);        // dlqq_build_run_project
        registerBuildDebugProjectTool(server);      // dlqq_build_debug_project
        registerRebuildRunProjectTool(server);      // dlqq_rebuild_run_project
        registerRebuildDebugProjectTool(server);    // dlqq_rebuild_debug_project

        // ========== 脚本路径设置工具 ==========
        registerSetBuildScriptTool(server);         // dlqq_set_build_script
        registerSetRebuildScriptTool(server);       // dlqq_set_rebuild_script
        registerSetChipEraseScriptTool(server);     // dlqq_set_chip_erase_script
        registerSetChipDownloadScriptTool(server);  // dlqq_set_chip_download_script
        registerSetEraseDownloadScriptTool(server); // dlqq_set_erase_download_script
        registerSetBuildEraseDownloadScriptTool(server);  // dlqq_set_build_erase_download_script
        registerSetRebuildEraseDownloadScriptTool(server); // dlqq_set_rebuild_erase_download_script
        registerSetRunScriptTool(server);           // dlqq_set_run_script
        registerSetDebugScriptTool(server);         // dlqq_set_debug_script
        registerSetBuildRunScriptTool(server);      // dlqq_set_build_run_script
        registerSetBuildDebugScriptTool(server);    // dlqq_set_build_debug_script
        registerSetRebuildRunScriptTool(server);    // dlqq_set_rebuild_run_script
        registerSetRebuildDebugScriptTool(server);  // dlqq_set_rebuild_debug_script

        // ========== WiFi工具 ==========
        WifiTools.registerTools(server);            // dlqq_run_wifi 等12个

        // ========== 鼠标工具 ==========
        MouseTools.registerTools(server);           // dlqq_mouse_get_position 等7个

        // ========== 键盘工具 ==========
        KeyboardTools.registerTools(server);        // dlqq_keyboard_type 等6个

        // ========== 时间工具 ==========
        TimeTools.registerTools(server);            // dlqq_get_current_time

        // ========== 屏幕截图工具 ==========
        ScreenTools.registerTools(server);          // dlqq_capture_screen

        // ========== 工具列表查询工具 ==========
        registerGetToolListTool(server);            // dlqq_get_tool_list
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
        startProp.put("type", Arrays.asList("integer", "string"));
        startProp.put("description", "起始行数（1-based，默认为1）");
        properties.put("start_line", startProp);

        Map<String, Object> endProp = new HashMap<>();
        endProp.put("type", Arrays.asList("integer", "string"));
        endProp.put("description", "结束行数（1-based，包含，-1表示到文件末尾）");
        properties.put("end_line", endProp);

        Map<String, Object> linesProp = new HashMap<>();
        linesProp.put("type", Arrays.asList("integer", "string"));
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
                    String report = FileTools.createFile(filePath, content, charset);
                    return textResult(report);
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
                    String report = FileTools.overwriteFile(filePath, content, charset);
                    return textResult(report);
                } catch (IOException e) {
                    return errorResult("覆盖文件失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 2.5b dlqq_clear_file - 清空文件内容
     */
    private void registerClearFileTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> pathProp = new HashMap<>();
        pathProp.put("type", "string");
        pathProp.put("description", "文件路径（支持绝对路径和相对路径）");
        properties.put("file_path", pathProp);

        Map<String, Object> charsetProp = new HashMap<>();
        charsetProp.put("type", "string");
        charsetProp.put("description", "字符集编码（如 UTF-8、GBK、ISO-8859-1 等，默认为 UTF-8）");
        properties.put("charset", charsetProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("file_path"));

        server.addTool("dlqq_clear_file",
            "清空文件内容（保留文件本身）。将文件内容清空为空白，但保留文件。如需删除文件本身，请使用 dlqq_delete_file。",
            inputSchema,
            arguments -> {
                String filePath = (String) arguments.get("file_path");
                String charset = (String) arguments.get("charset");

                if (filePath == null || filePath.trim().isEmpty()) {
                    return errorResult("文件路径不能为空");
                }

                try {
                    String report = FileTools.clearFile(filePath, charset);
                    return textResult(report);
                } catch (IOException e) {
                    return errorResult("清空文件失败: " + e.getMessage());
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
                    String report = FileTools.appendFile(filePath, content, charset);
                    return textResult(report);
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
        startProp.put("type", Arrays.asList("integer", "string"));
        startProp.put("description", "起始行数（1-based，包含）");
        properties.put("start_line", startProp);

        Map<String, Object> endProp = new HashMap<>();
        endProp.put("type", Arrays.asList("integer", "string"));
        endProp.put("description", "结束行数（1-based，包含，-1表示到文件末尾）");
        properties.put("end_line", endProp);

        Map<String, Object> charsetProp = new HashMap<>();
        charsetProp.put("type", "string");
        charsetProp.put("description", "字符集编码（如 UTF-8、GBK、ISO-8859-1 等，默认为 UTF-8）");
        properties.put("charset", charsetProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("file_path", "start_line", "end_line"));

        server.addTool("dlqq_remove_lines_in_file",
            "删除文件中指定行范围的内容（包含起始行和结束行）。支持指定字符集编码（默认UTF-8）。注意：end_line为-1表示到文件末尾，行号超出范围会自动修正。",
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
                    String report = FileTools.removeLinesInFile(filePath, startLine, endLine, charset);
                    // 检查是否被拒绝修改
                    if (report.startsWith("【拒绝修改】")) {
                        return errorResult(report + "\n\n【读取示例】\n请使用以下命令读取目标内容：\n" +
                            "{\"file_path\": \"" + filePath + "\", \"start_line\": " + startLine + ", \"end_line\": " + endLine + "}");
                    }
                    return textResult(report);
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
        startProp.put("type", Arrays.asList("integer", "string"));
        startProp.put("description", "起始行数（1-based，包含）");
        properties.put("start_line", startProp);

        Map<String, Object> endProp = new HashMap<>();
        endProp.put("type", Arrays.asList("integer", "string"));
        endProp.put("description", "结束行数（1-based，包含，-1表示到文件末尾）");
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
            "将文件中指定行范围的内容替换为新内容（包含起始行和结束行）。支持多行替换。支持指定字符集编码（默认UTF-8）。注意：end_line为-1表示到文件末尾，行号超出范围会自动修正。",
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
                    return errorResult("错误：content 参数为空。可能原因：\n" +
                        "1. JSON 字符串中的换行符 \\n 被当作实际换行，导致解析失败\n" +
                        "2. content 内容过长或包含特殊字符\n" +
                        "建议：将 \\n 替换为 \\\n 或使用 dlqq_insert_text_in_file 工具逐行插入");
                }

                try {
                    String report = FileTools.replaceLinesInFile(filePath, startLine, endLine, content, charset);
                    // 检查是否被拒绝修改
                    if (report.startsWith("【拒绝修改】")) {
                        return errorResult(report + "\n\n【读取示例】\n请使用以下命令读取目标内容：\n" +
                            "{\"file_path\": \"" + filePath + "\", \"start_line\": " + startLine + ", \"end_line\": " + endLine + "}");
                    }
                    return textResult(report);
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
        lineProp.put("type", Arrays.asList("integer", "string"));
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
                    String report = FileTools.insertTextInFile(filePath, line, content, charset);
                    // 检查是否被拒绝修改
                    if (report.startsWith("【拒绝修改】")) {
                        // 计算建议读取的范围（插入点前后5行）
                        int readStart = Math.max(1, line - 5);
                        int readEnd = line + 5;
                        return errorResult(report + "\n\n【读取示例】\n请使用以下命令读取插入位置附近的上下文：\n" +
                            "{\"file_path\": \"" + filePath + "\", \"start_line\": " + readStart + ", \"end_line\": " + readEnd + "}");
                    }
                    return textResult(report);
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

        Map<String, Object> startLineProp = new HashMap<>();
        startLineProp.put("type", "integer");
        startLineProp.put("description", "起始行号（1-based，可选）。传入时可只读取指定行范围进行验证");
        properties.put("start_line", startLineProp);

        Map<String, Object> endLineProp = new HashMap<>();
        endLineProp.put("type", "integer");
        endLineProp.put("description", "结束行号（1-based，可选，-1表示到末尾）。传入时可只读取指定行范围进行验证");
        properties.put("end_line", endLineProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("file_path", "search_text", "replace_text"));

        server.addTool("dlqq_replace_text_in_file",
            "替换文件中的指定文本。支持普通文本模式和正则表达式模式。支持指定字符集编码（默认UTF-8）。" +
            "传入start_line和end_line参数时，只需读取指定行范围即可修改；未传入时需读取全部文件内容。" +
            "返回结果仅展示修改部分和上下5行代码的对比。",
            inputSchema,
            arguments -> {
                String filePath = (String) arguments.get("file_path");
                String searchText = (String) arguments.get("search_text");
                String replaceText = (String) arguments.get("replace_text");
                String mode = (String) arguments.getOrDefault("mode", "text");
                String charset = (String) arguments.get("charset");
                Integer startLine = (Integer) arguments.get("start_line");
                Integer endLine = (Integer) arguments.get("end_line");

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
                    String report = FileTools.replaceTextInFile(filePath, searchText, replaceText, useRegex, charset, startLine, endLine);
                    // 检查是否被拒绝修改或未找到匹配
                    if (report.startsWith("【拒绝修改】")) {
                        return errorResult(report + "\n\n【读取示例】\n请使用以下命令读取文件全部内容：\n" +
                            "{\"file_path\": \"" + filePath + "\", \"start_line\": 1, \"end_line\": -1}");
                    }
                    if (report.startsWith("【未替换】")) {
                        return errorResult(report);
                    }
                    return textResult(report);
                } catch (IOException e) {
                    return errorResult("操作失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 5.5 dlqq_remove_text_in_file - 删除匹配的文本
     */
    private void registerRemoveTextInFileTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> pathProp = new HashMap<>();
        pathProp.put("type", "string");
        pathProp.put("description", "文件路径");
        properties.put("file_path", pathProp);

        Map<String, Object> searchProp = new HashMap<>();
        searchProp.put("type", "string");
        searchProp.put("description", "要搜索并删除的文本或正则表达式");
        properties.put("search_text", searchProp);

        Map<String, Object> modeProp = new HashMap<>();
        modeProp.put("type", "string");
        modeProp.put("description", "模式：'text'（普通文本）或 'regex'（正则表达式）");
        modeProp.put("enum", Arrays.asList("text", "regex"));
        properties.put("mode", modeProp);

        Map<String, Object> charsetProp = new HashMap<>();
        charsetProp.put("type", "string");
        charsetProp.put("description", "字符集编码（如 UTF-8、GBK、ISO-8859-1 等，默认为 UTF-8）");
        properties.put("charset", charsetProp);

        Map<String, Object> startLineProp = new HashMap<>();
        startLineProp.put("type", "integer");
        startLineProp.put("description", "起始行号（1-based，可选）。传入时可只读取指定行范围进行验证");
        properties.put("start_line", startLineProp);

        Map<String, Object> endLineProp = new HashMap<>();
        endLineProp.put("type", "integer");
        endLineProp.put("description", "结束行号（1-based，可选，-1表示到末尾）。传入时可只读取指定行范围进行验证");
        properties.put("end_line", endLineProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("file_path", "search_text"));

        server.addTool("dlqq_remove_text_in_file",
            "删除文件中匹配的文本。相当于将匹配文本替换为空字符串。支持普通文本模式和正则表达式模式。" +
            "传入start_line和end_line参数时，只需读取指定行范围即可修改；未传入时需读取全部文件内容。" +
            "返回结果仅展示修改部分和上下5行代码的对比。",
            inputSchema,
            arguments -> {
                String filePath = (String) arguments.get("file_path");
                String searchText = (String) arguments.get("search_text");
                String mode = (String) arguments.getOrDefault("mode", "text");
                String charset = (String) arguments.get("charset");
                Integer startLine = (Integer) arguments.get("start_line");
                Integer endLine = (Integer) arguments.get("end_line");

                if (filePath == null || filePath.trim().isEmpty()) {
                    return errorResult("文件路径不能为空");
                }
                if (searchText == null || searchText.isEmpty()) {
                    return errorResult("搜索文本不能为空");
                }

                try {
                    boolean useRegex = "regex".equalsIgnoreCase(mode);
                    String report = FileTools.removeTextInFile(filePath, searchText, useRegex, charset, startLine, endLine);
                    // 检查是否被拒绝修改或未找到匹配
                    if (report.startsWith("【拒绝修改】")) {
                        return errorResult(report + "\n\n【读取示例】\n请使用以下命令读取文件全部内容：\n" +
                            "{\"file_path\": \"" + filePath + "\", \"start_line\": 1, \"end_line\": -1}");
                    }
                    if (report.startsWith("【未替换】")) {
                        return errorResult(report);
                    }
                    return textResult(report);
                } catch (IOException e) {
                    return errorResult("操作失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 5.6 dlqq_replace_text_in_dir - 在目录下批量替换文本
     */
    private void registerReplaceTextInDirTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> dirProp = new HashMap<>();
        dirProp.put("type", "string");
        dirProp.put("description", "目录路径，将在该目录及其子目录中查找文本文件进行替换");
        properties.put("dir_path", dirProp);

        Map<String, Object> searchProp = new HashMap<>();
        searchProp.put("type", "string");
        searchProp.put("description", "要搜索的文本或正则表达式");
        properties.put("search_text", searchProp);

        Map<String, Object> replaceProp = new HashMap<>();
        replaceProp.put("type", "string");
        replaceProp.put("description", "替换后的文本（为空则删除匹配内容）");
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

        Map<String, Object> fileExtProp = new HashMap<>();
        fileExtProp.put("type", "array");
        fileExtProp.put("description", "文件扩展名列表（如 [\"java\", \"txt\", \"md\"]），为空则使用默认文本文件类型");
        properties.put("file_extensions", fileExtProp);

        Map<String, Object> maxFilesProp = new HashMap<>();
        maxFilesProp.put("type", "integer");
        maxFilesProp.put("description", "最大处理文件数（默认100，防止处理过多文件）");
        properties.put("max_files", maxFilesProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("dir_path", "search_text", "replace_text"));

        server.addTool("dlqq_replace_text_in_dir",
            "在指定目录及其子目录下的所有文本文件中批量替换指定文本。支持普通文本模式和正则表达式模式。" +
            "支持指定文件类型过滤，支持设置最大处理文件数限制。返回详细的替换报告，包含每个文件的替换结果汇总。",
            inputSchema,
            arguments -> {
                String dirPath = (String) arguments.get("dir_path");
                String searchText = (String) arguments.get("search_text");
                String replaceText = (String) arguments.get("replace_text");
                String mode = (String) arguments.getOrDefault("mode", "text");
                String charset = (String) arguments.get("charset");
                Object fileExtensionsObj = arguments.get("file_extensions");
                Integer maxFiles = (Integer) arguments.get("max_files");

                if (dirPath == null || dirPath.trim().isEmpty()) {
                    return errorResult("目录路径不能为空");
                }
                if (searchText == null || searchText.isEmpty()) {
                    return errorResult("搜索文本不能为空");
                }
                if (replaceText == null) {
                    replaceText = "";
                }

                // 处理文件扩展名列表
                List<String> fileExtensions = null;
                if (fileExtensionsObj instanceof List) {
                    fileExtensions = new ArrayList<>();
                    for (Object ext : (List<?>) fileExtensionsObj) {
                        if (ext instanceof String) {
                            fileExtensions.add((String) ext);
                        }
                    }
                }

                try {
                    boolean useRegex = "regex".equalsIgnoreCase(mode);
                    String report = FileTools.replaceTextInDir(dirPath, searchText, replaceText, useRegex, charset, fileExtensions, maxFiles);
                    // 检查是否被取消
                    if (report.startsWith("【操作已取消】")) {
                        return errorResult(report);
                    }
                    return textResult(report);
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

    /**
     * 17. dlqq_list_dir_tree - 列出目录树（递归）
     */
    private void registerListDirTreeTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> pathProp = new HashMap<>();
        pathProp.put("type", "string");
        pathProp.put("description", "要列出的目录路径");
        properties.put("dir_path", pathProp);

        Map<String, Object> depthProp = new HashMap<>();
        depthProp.put("type", Arrays.asList("integer", "string"));
        depthProp.put("description", "最大递归深度（-1表示无限制，默认为-1）");
        properties.put("max_depth", depthProp);

        Map<String, Object> entriesProp = new HashMap<>();
        entriesProp.put("type", Arrays.asList("integer", "string"));
        entriesProp.put("description", "最大返回条目数（-1表示无限制，默认为-1）");
        properties.put("max_entries", entriesProp);

        Map<String, Object> hiddenProp = new HashMap<>();
        hiddenProp.put("type", "string");
        hiddenProp.put("description", "是否显示隐藏文件/目录（'true'或'false'，默认为'false'）");
        hiddenProp.put("enum", Arrays.asList("true", "false"));
        properties.put("show_hidden", hiddenProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("dir_path"));

        server.addTool("dlqq_list_dir_tree",
            "列出指定文件夹的完整目录结构，递归显示所有子目录和文件。支持指定递归深度和最大条目数限制。",
            inputSchema,
            arguments -> {
                String dirPath = (String) arguments.get("dir_path");
                Integer maxDepth = parseInteger(arguments.get("max_depth"), -1);
                Integer maxEntries = parseInteger(arguments.get("max_entries"), -1);
                String showHiddenStr = (String) arguments.getOrDefault("show_hidden", "false");
                boolean showHidden = Boolean.parseBoolean(showHiddenStr);

                if (dirPath == null || dirPath.trim().isEmpty()) {
                    return errorResult("目录路径不能为空");
                }

                try {
                    String result = FileTools.listDirectoryTree(dirPath, maxDepth, maxEntries, showHidden);
                    return textResult(result);
                } catch (IOException e) {
                    return errorResult("列出目录失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 18. dlqq_list_dir_files - 列出单层目录内容
     */
    private void registerListDirFilesTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> pathProp = new HashMap<>();
        pathProp.put("type", "string");
        pathProp.put("description", "要列出的目录路径");
        properties.put("dir_path", pathProp);

        Map<String, Object> hiddenProp = new HashMap<>();
        hiddenProp.put("type", "string");
        hiddenProp.put("description", "是否显示隐藏文件/目录（'true'或'false'，默认为'false'）");
        hiddenProp.put("enum", Arrays.asList("true", "false"));
        properties.put("show_hidden", hiddenProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("dir_path"));

        server.addTool("dlqq_list_dir_files",
            "列出指定文件夹下的文件和文件夹，只列出一层，不递归进入子目录。",
            inputSchema,
            arguments -> {
                String dirPath = (String) arguments.get("dir_path");
                String showHiddenStr = (String) arguments.getOrDefault("show_hidden", "false");
                boolean showHidden = Boolean.parseBoolean(showHiddenStr);

                if (dirPath == null || dirPath.trim().isEmpty()) {
                    return errorResult("目录路径不能为空");
                }

                try {
                    String result = FileTools.listDirectoryFiles(dirPath, showHidden);
                    return textResult(result);
                } catch (IOException e) {
                    return errorResult("列出目录失败: " + e.getMessage());
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
        startProp.put("type", Arrays.asList("integer", "string"));
        startProp.put("description", "起始字节（0-based，默认为0）");
        properties.put("start_byte", startProp);

        Map<String, Object> endProp = new HashMap<>();
        endProp.put("type", Arrays.asList("integer", "string"));
        endProp.put("description", "结束字节（0-based，包含，-1表示到文件末尾）");
        properties.put("end_byte", endProp);

        Map<String, Object> bytesProp = new HashMap<>();
        bytesProp.put("type", Arrays.asList("integer", "string"));
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

    /**
     * 17b. dlqq_search_text_in_dir - 在目录下的文本文件中搜索内容
     */
    private void registerSearchTextInDirTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> pathProp = new HashMap<>();
        pathProp.put("type", "string");
        pathProp.put("description", "搜索目录路径");
        properties.put("search_path", pathProp);

        Map<String, Object> patternProp = new HashMap<>();
        patternProp.put("type", "string");
        patternProp.put("description", "搜索文本模式");
        properties.put("pattern", patternProp);

        Map<String, Object> modeProp = new HashMap<>();
        modeProp.put("type", "string");
        modeProp.put("description", "模式：'text'（部分匹配）或 'regex'（正则表达式）");
        modeProp.put("enum", Arrays.asList("text", "regex"));
        properties.put("mode", modeProp);

        Map<String, Object> ignoreCaseProp = new HashMap<>();
        ignoreCaseProp.put("type", "boolean");
        ignoreCaseProp.put("description", "是否忽略大小写（默认true）");
        properties.put("ignore_case", ignoreCaseProp);

        Map<String, Object> extensionsProp = new HashMap<>();
        extensionsProp.put("type", "array");
        extensionsProp.put("description", "文件后缀名列表（如 ['.java', '.c', '.h']），为空时使用配置默认值");
        properties.put("file_extensions", extensionsProp);

        Map<String, Object> maxResultsProp = new HashMap<>();
        maxResultsProp.put("type", Arrays.asList("integer", "string"));
        maxResultsProp.put("description", "最大返回结果数（默认使用配置search.max.results，若无配置则为50）");
        properties.put("max_results", maxResultsProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("search_path", "pattern"));

        server.addTool("dlqq_search_text_in_dir",
            "在指定目录下的所有文本文件中搜索指定内容。支持正则表达式或部分匹配，支持忽略大小写，" +
            "返回匹配的文件路径、行号、行内容以及3行上下文。" +
            "可通过 file_extensions 指定文件类型，为空时使用 settings.properties 中的默认配置。" +
            "搜索受 search.max.files、search.max.file.size、search.max.results 配置限制。",
            inputSchema,
            arguments -> {
                String searchPath = (String) arguments.get("search_path");
                String pattern = (String) arguments.get("pattern");
                String mode = (String) arguments.getOrDefault("mode", "text");
                Boolean ignoreCase = (Boolean) arguments.getOrDefault("ignore_case", true);
                @SuppressWarnings("unchecked")
                List<String> fileExtensions = (List<String>) arguments.get("file_extensions");

                // 从配置读取默认最大结果数
                int defaultMaxResults = getSearchConfigInt("search.max.results", 50);
                Integer maxResults = parseInteger(arguments.get("max_results"), defaultMaxResults);

                if (searchPath == null || searchPath.trim().isEmpty()) {
                    return errorResult("搜索路径不能为空");
                }
                if (pattern == null || pattern.trim().isEmpty()) {
                    return errorResult("搜索模式不能为空");
                }

                try {
                    // 读取配置
                    int maxFiles = getSearchConfigInt("search.max.files", 1000);
                    long maxFileSize = getSearchConfigLong("search.max.file.size", 10 * 1024 * 1024); // 默认10MB
                    String defaultExtensions = getSearchConfigString("search.text.extensions", 
                        ".java,.c,.cpp,.h,.hpp,.cs,.py,.js,.ts,.html,.css,.json,.xml,.yaml,.yml,.md,.txt,.sh,.bat,.properties,.gradle,.ini");

                    // 如果未指定扩展名，使用默认配置
                    if (fileExtensions == null || fileExtensions.isEmpty()) {
                        fileExtensions = Arrays.asList(defaultExtensions.split(","));
                    }

                    boolean useRegex = "regex".equalsIgnoreCase(mode);

                    List<FileTools.TextSearchResult> results = FileTools.searchTextInFiles(
                        searchPath, pattern, useRegex, ignoreCase, 
                        fileExtensions, maxFiles, maxFileSize
                    );

                    String formatted = FileTools.formatSearchResults(results, maxResults);
                    return textResult(formatted);

                } catch (IOException e) {
                    return errorResult("搜索失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 17c. dlqq_search_text_in_file - 在单个文本文件中搜索内容
     */
    private void registerSearchTextInFileTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> pathProp = new HashMap<>();
        pathProp.put("type", "string");
        pathProp.put("description", "文件路径");
        properties.put("file_path", pathProp);

        Map<String, Object> patternProp = new HashMap<>();
        patternProp.put("type", "string");
        patternProp.put("description", "搜索文本模式");
        properties.put("pattern", patternProp);

        Map<String, Object> modeProp = new HashMap<>();
        modeProp.put("type", "string");
        modeProp.put("description", "模式：'text'（部分匹配）或 'regex'（正则表达式）");
        modeProp.put("enum", Arrays.asList("text", "regex"));
        properties.put("mode", modeProp);

        Map<String, Object> ignoreCaseProp = new HashMap<>();
        ignoreCaseProp.put("type", "boolean");
        ignoreCaseProp.put("description", "是否忽略大小写（默认true）");
        properties.put("ignore_case", ignoreCaseProp);

        Map<String, Object> maxResultsProp = new HashMap<>();
        maxResultsProp.put("type", Arrays.asList("integer", "string"));
        maxResultsProp.put("description", "最大返回结果数（默认使用配置search.max.results，若无配置则为50，-1表示无限制）");
        properties.put("max_results", maxResultsProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("file_path", "pattern"));

        server.addTool("dlqq_search_text_in_file",
            "在单个文本文件中搜索指定内容。支持正则表达式或部分匹配，支持忽略大小写，" +
            "返回匹配的行号、行内容以及3行上下文。" +
            "可通过 max_results 参数限制返回结果数，或使用 search.max.results 配置设置默认值。",
            inputSchema,
            arguments -> {
                String filePath = (String) arguments.get("file_path");
                String pattern = (String) arguments.get("pattern");
                String mode = (String) arguments.getOrDefault("mode", "text");
                Boolean ignoreCase = (Boolean) arguments.getOrDefault("ignore_case", true);

                // 从配置读取默认最大结果数
                int defaultMaxResults = getSearchConfigInt("search.max.results", 50);
                Integer maxResults = parseInteger(arguments.get("max_results"), defaultMaxResults);

                if (filePath == null || filePath.trim().isEmpty()) {
                    return errorResult("文件路径不能为空");
                }
                if (pattern == null || pattern.trim().isEmpty()) {
                    return errorResult("搜索模式不能为空");
                }

                try {
                    boolean useRegex = "regex".equalsIgnoreCase(mode);

                    List<FileTools.TextSearchResult> results = FileTools.searchTextInSingleFile(
                        filePath, pattern, useRegex, ignoreCase
                    );

                    // 限制结果数量
                    List<FileTools.TextSearchResult> limitedResults = maxResults > 0 && results.size() > maxResults
                        ? results.subList(0, maxResults)
                        : results;

                    String formatted = FileTools.formatSearchResults(limitedResults, -1);
                    return textResult(formatted);

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
        portProp.put("type", Arrays.asList("integer", "string"));
        portProp.put("description", "服务器端口号（用于AI后续连接，例如：8001、8002等）");
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
        portProp.put("type", Arrays.asList("integer", "string"));
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
        portProp.put("type", Arrays.asList("integer", "string"));
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
        portProp.put("type", Arrays.asList("integer", "string"));
        portProp.put("description", "串口工具的端口号（dlqq_run_uart 指定的端口号）");
        properties.put("port", portProp);

        Map<String, Object> comProp = new HashMap<>();
        comProp.put("type", "string");
        comProp.put("description", "串口号（如 COM3、ttyUSB0、USB Serial等）");
        properties.put("com", comProp);

        Map<String, Object> baudRateProp = new HashMap<>();
        baudRateProp.put("type", Arrays.asList("integer", "string"));
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
        portProp.put("type", Arrays.asList("integer", "string"));
        portProp.put("description", "串口工具的端口号（dlqq_run_uart 指定的端口号）");
        properties.put("port", portProp);

        Map<String, Object> comProp = new HashMap<>();
        comProp.put("type", "string");
        comProp.put("description", "串口号（如 COM3、ttyUSB0等）");
        properties.put("com", comProp);

        Map<String, Object> baudRateProp = new HashMap<>();
        baudRateProp.put("type", Arrays.asList("integer", "string"));
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
        portProp.put("type", Arrays.asList("integer", "string"));
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
        portProp.put("type", Arrays.asList("integer", "string"));
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
        portProp.put("type", Arrays.asList("integer", "string"));
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
     * 模式3: start_line + wait_time + line_count - 等待模式读取新数据
     * 如需固定格式，请使用 dlqq_uart_get_log_text（强制文本）或 dlqq_uart_get_log_hex（强制HEX）
     */
    private void registerUartGetLogTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", Arrays.asList("integer", "string"));
        portProp.put("description", "串口工具的端口号（dlqq_run_uart 指定的端口号）");
        properties.put("port", portProp);

        Map<String, Object> startLineProp = new HashMap<>();
        startLineProp.put("type", Arrays.asList("integer", "string"));
        startLineProp.put("description", "起始行（1-based，模式1/2必需，模式3默认为-1表示从最后一行开始读取新数据）");
        properties.put("start_line", startLineProp);

        Map<String, Object> endLineProp = new HashMap<>();
        endLineProp.put("type", Arrays.asList("integer", "string"));
        endLineProp.put("description", "结束行（1-based，模式1使用）");
        properties.put("end_line", endLineProp);

        Map<String, Object> lineCountProp = new HashMap<>();
        lineCountProp.put("type", Arrays.asList("integer", "string"));
        lineCountProp.put("description", "读取行数（模式2和模式3使用，模式3必需）");
        properties.put("line_count", lineCountProp);

        Map<String, Object> waitTimeProp = new HashMap<>();
        waitTimeProp.put("type", Arrays.asList("integer", "string"));
        waitTimeProp.put("description", "等待时间（秒，模式3使用，默认0表示不等待，最大30秒）");
        properties.put("wait_time", waitTimeProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port"));

        server.addTool("dlqq_uart_get_log",
            "获取串口缓冲区日志数据。支持3种模式：\n" +
            "模式1: 指定start_line和end_line，直接读取已有数据\n" +
            "模式2: 指定start_line和line_count，直接读取已有数据\n" +
            "模式3: 指定wait_time和line_count（start_line默认为-1），等待读取新数据\n" +
            "注意：wait_time单位是秒，最大30秒\n" +
            "返回格式根据 dlqq_uart_switch_text_hex 设置（文本/HEX），保证与UI显示一致\n" +
            "如需固定格式，请使用 dlqq_uart_get_log_text（强制文本）或 dlqq_uart_get_log_hex（强制HEX）",
            inputSchema,
            arguments -> {
                Integer port = parseInteger(arguments.get("port"), null);
                Integer startLine = parseInteger(arguments.get("start_line"), null);
                Integer endLine = parseInteger(arguments.get("end_line"), null);
                Integer lineCount = parseInteger(arguments.get("line_count"), null);
                Integer waitTimeSec = parseInteger(arguments.get("wait_time"), 0);

                if (port == null) {
                    return errorResult("必须指定端口号（port）");
                }
                if (port < 1 || port > 65535) {
                    return errorResult("端口号必须在 1~65535 范围内");
                }
                if (waitTimeSec < 0 || waitTimeSec > 30) {
                    return errorResult("wait_time必须在 0~30 秒范围内");
                }

                try {
                    // 转换为毫秒
                    int waitTimeMs = waitTimeSec * 1000;
                    String result = UartTools.getUartLog(port, startLine, endLine, lineCount, waitTimeMs);
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
        portProp.put("type", Arrays.asList("integer", "string"));
        portProp.put("description", "串口工具的端口号（dlqq_run_uart 指定的端口号）");
        properties.put("port", portProp);

        Map<String, Object> startLineProp = new HashMap<>();
        startLineProp.put("type", Arrays.asList("integer", "string"));
        startLineProp.put("description", "起始行（1-based，模式1/2必需，模式3默认为-1表示从最后一行开始读取新数据）");
        properties.put("start_line", startLineProp);

        Map<String, Object> endLineProp = new HashMap<>();
        endLineProp.put("type", Arrays.asList("integer", "string"));
        endLineProp.put("description", "结束行（1-based，模式1使用）");
        properties.put("end_line", endLineProp);

        Map<String, Object> lineCountProp = new HashMap<>();
        lineCountProp.put("type", Arrays.asList("integer", "string"));
        lineCountProp.put("description", "读取行数（模式2和模式3使用，模式3必需）");
        properties.put("line_count", lineCountProp);

        Map<String, Object> waitTimeProp = new HashMap<>();
        waitTimeProp.put("type", Arrays.asList("integer", "string"));
        waitTimeProp.put("description", "等待时间（秒，模式3使用，默认0表示不等待，最大30秒）");
        properties.put("wait_time", waitTimeProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port"));

        server.addTool("dlqq_uart_get_log_text",
            "获取串口缓冲区日志数据（强制文本格式）。支持3种模式：\n" +
            "模式1: 指定start_line和end_line，直接读取已有数据\n" +
            "模式2: 指定start_line和line_count，直接读取已有数据\n" +
            "模式3: 指定wait_time和line_count（start_line默认为-1），等待读取新数据\n" +
            "注意：wait_time单位是秒，最大30秒\n" +
            "无论当前显示格式设置如何，始终返回文本格式",
            inputSchema,
            arguments -> {
                Integer port = parseInteger(arguments.get("port"), null);
                Integer startLine = parseInteger(arguments.get("start_line"), null);
                Integer endLine = parseInteger(arguments.get("end_line"), null);
                Integer lineCount = parseInteger(arguments.get("line_count"), null);
                Integer waitTimeSec = parseInteger(arguments.get("wait_time"), 0);

                if (port == null) {
                    return errorResult("必须指定端口号（port）");
                }
                if (port < 1 || port > 65535) {
                    return errorResult("端口号必须在 1~65535 范围内");
                }
                if (waitTimeSec < 0 || waitTimeSec > 30) {
                    return errorResult("wait_time必须在 0~30 秒范围内");
                }

                try {
                    // 转换为毫秒
                    int waitTimeMs = waitTimeSec * 1000;
                    String result = UartTools.getUartLogText(port, startLine, endLine, lineCount, waitTimeMs);
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
        portProp.put("type", Arrays.asList("integer", "string"));
        portProp.put("description", "串口工具的端口号（dlqq_run_uart 指定的端口号）");
        properties.put("port", portProp);

        Map<String, Object> startLineProp = new HashMap<>();
        startLineProp.put("type", Arrays.asList("integer", "string"));
        startLineProp.put("description", "起始行（1-based，模式1/2必需，模式3默认为-1表示从最后一行开始读取新数据）");
        properties.put("start_line", startLineProp);

        Map<String, Object> endLineProp = new HashMap<>();
        endLineProp.put("type", Arrays.asList("integer", "string"));
        endLineProp.put("description", "结束行（1-based，模式1使用）");
        properties.put("end_line", endLineProp);

        Map<String, Object> lineCountProp = new HashMap<>();
        lineCountProp.put("type", Arrays.asList("integer", "string"));
        lineCountProp.put("description", "读取行数（模式2和模式3使用，模式3必需）");
        properties.put("line_count", lineCountProp);

        Map<String, Object> waitTimeProp = new HashMap<>();
        waitTimeProp.put("type", Arrays.asList("integer", "string"));
        waitTimeProp.put("description", "等待时间（秒，模式3使用，默认0表示不等待，最大30秒）");
        properties.put("wait_time", waitTimeProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port"));

        server.addTool("dlqq_uart_get_log_hex",
            "获取串口缓冲区日志数据（强制HEX格式）。支持3种模式：\n" +
            "模式1: 指定start_line和end_line，直接读取已有数据\n" +
            "模式2: 指定start_line和line_count，直接读取已有数据\n" +
            "模式3: 指定wait_time和line_count（start_line默认为-1），等待读取新数据\n" +
            "注意：wait_time单位是秒，最大30秒\n" +
            "无论当前显示格式设置如何，始终返回HEX格式",
            inputSchema,
            arguments -> {
                Integer port = parseInteger(arguments.get("port"), null);
                Integer startLine = parseInteger(arguments.get("start_line"), null);
                Integer endLine = parseInteger(arguments.get("end_line"), null);
                Integer lineCount = parseInteger(arguments.get("line_count"), null);
                Integer waitTimeSec = parseInteger(arguments.get("wait_time"), 0);

                if (port == null) {
                    return errorResult("必须指定端口号（port）");
                }
                if (port < 1 || port > 65535) {
                    return errorResult("端口号必须在 1~65535 范围内");
                }
                if (waitTimeSec < 0 || waitTimeSec > 30) {
                    return errorResult("wait_time必须在 0~30 秒范围内");
                }

                try {
                    // 转换为毫秒
                    int waitTimeMs = waitTimeSec * 1000;
                    String result = UartTools.getUartLogHex(port, startLine, endLine, lineCount, waitTimeMs);
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
        portProp.put("type", Arrays.asList("integer", "string"));
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
        portProp.put("type", Arrays.asList("integer", "string"));
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
        portProp.put("type", Arrays.asList("integer", "string"));
        portProp.put("description", "串口工具的端口号（dlqq_run_uart 指定的端口号）");
        properties.put("port", portProp);

        Map<String, Object> sizeProp = new HashMap<>();
        sizeProp.put("type", Arrays.asList("integer", "string"));
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
        portProp.put("type", Arrays.asList("integer", "string"));
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
        portProp.put("type", Arrays.asList("integer", "string"));
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

    /**
     * 41a. dlqq_rerun_uart - 重新启动串口工具
     */
    private void registerRerunUartTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", Arrays.asList("integer", "string"));
        portProp.put("description", "串口工具的端口号（先关闭再启动该端口）");
        properties.put("port", portProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port"));

        server.addTool("dlqq_rerun_uart",
            "重新启动指定端口号的串口工具。先关闭该端口的串口工具（如果正在运行），然后重新启动。用于刷新串口工具窗口或解决连接问题。",
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
                    // 先关闭（如果正在运行）
                    StringBuilder result = new StringBuilder();
                    try {
                        String exitResult = UartTools.exitUart(port);
                        result.append("关闭旧窗口: ").append(exitResult).append("\n");
                    } catch (Exception e) {
                        result.append("旧窗口未运行或关闭失败: ").append(e.getMessage()).append("\n");
                    }

                    // 等待一小段时间确保进程完全关闭
                    Thread.sleep(500);

                    // 重新启动
                    String runResult = UartTools.runUart(port);
                    result.append("启动新窗口: ").append(runResult);
                    return textResult(result.toString());
                } catch (Exception e) {
                    return errorResult("重新启动串口工具失败: " + e.getMessage());
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
     * 解析整数参数（兼容 Long、Integer 和 String）
     * 当传入非空值但无法解析时抛出IllegalArgumentException，确保AI传错参数时报错而不是使用默认值
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
        if (value instanceof String) {
            String str = ((String) value).trim();
            if (str.isEmpty()) {
                return defaultValue;
            }
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("参数格式错误：'" + str + "' 不是有效的整数");
            }
        }
        throw new IllegalArgumentException("参数类型错误：期望整数类型，但得到 " + value.getClass().getSimpleName());
    }

    /**
     * 解析长整数参数（兼容 Long、Integer 和 String）
     * 当传入非空值但无法解析时抛出IllegalArgumentException，确保AI传错参数时报错而不是使用默认值
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
        if (value instanceof String) {
            String str = ((String) value).trim();
            if (str.isEmpty()) {
                return defaultValue;
            }
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("参数格式错误：'" + str + "' 不是有效的整数");
            }
        }
        throw new IllegalArgumentException("参数类型错误：期望整数类型，但得到 " + value.getClass().getSimpleName());
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

    // ==================== 搜索配置读取方法 ====================

    /**
     * 获取搜索配置值（字符串）
     */
    private String getSearchConfigString(String key, String defaultValue) {
        String value = McpTools.getSearchConfig(key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    /**
     * 获取搜索配置值（整数）
     */
    private int getSearchConfigInt(String key, int defaultValue) {
        String value = McpTools.getSearchConfig(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 获取搜索配置值（长整数）
     */
    private long getSearchConfigLong(String key, long defaultValue) {
        String value = McpTools.getSearchConfig(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // ==================== BLE库管理工具 ====================

    /**
     * 30. dlqq_ble_lib_has - 检查Python BLE库状态
     */
    private void registerBleLibHasTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.emptyList());

        server.addTool("dlqq_ble_lib_has",
            "检查Python BLE库（bleak）是否已安装，返回Python版本和各库的安装状态及版本信息。如果库未安装，需要先使用 dlqq_ble_lib_install 安装。",
            inputSchema,
            arguments -> {
                String result = BleTools.bleLibHas();
                return textResult(result);
            });
    }

    /**
     * 31. dlqq_ble_lib_install - 安装Python BLE库
     */
    private void registerBleLibInstallTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.emptyList());

        server.addTool("dlqq_ble_lib_install",
            "自动安装Python BLE依赖库（bleak）。需要先确保Python 3.8+已安装且python命令可用。安装完成后可以使用 dlqq_ble_lib_has 检查安装结果。",
            inputSchema,
            arguments -> {
                String result = BleTools.bleLibInstall();
                return textResult(result);
            });
    }

    /**
     * 32. dlqq_ble_lib_update - 检查并更新Python BLE库
     */
    private void registerBleLibUpdateTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.emptyList());

        server.addTool("dlqq_ble_lib_update",
            "检查Python BLE库是否有新版本可用，如果有更新会弹出对话框让用户选择是否立即更新。如果用户选择更新，会自动执行更新操作。",
            inputSchema,
            arguments -> {
                String result = BleTools.bleLibUpdate();
                return textResult(result);
            });
    }

    /**
     * 33. dlqq_ble_release_script - 释放Python脚本到项目目录
     */
    private void registerBleReleaseScriptTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.emptyList());

        server.addTool("dlqq_ble_release_script",
            "释放BLE Python脚本到项目目录下的.dlqq/script/python文件夹中。首次使用BLE工具前建议先执行此操作，以便查看和管理Python脚本。",
            inputSchema,
            arguments -> {
                String result = BleTools.bleReleaseScript();
                return textResult(result);
            });
    }

    // ==================== BLE蓝牙工具 ====================

    /**
     * 34. dlqq_run_ble - 启动BLE工具
     */
    private void registerRunBleTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", Arrays.asList("integer", "string"));
        portProp.put("description", "Python服务端口号（例如：8765、8766等）");
        properties.put("port", portProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port"));

        server.addTool("dlqq_run_ble",
            "打开BLE蓝牙工具，指定Python服务端口号，启动后Python GUI窗口保持运行，AI可通过此端口与BLE工具通信。",
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
                    String result = BleTools.runBle(port);
                    return textResult(result);
                } catch (Exception e) {
                    return errorResult("启动BLE工具失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 34. dlqq_ble_scan - 扫描BLE设备
     */
    private void registerBleScanTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", Arrays.asList("integer", "string"));
        portProp.put("description", "BLE工具的Python端口号（dlqq_run_ble 指定的端口号）");
        properties.put("port", portProp);

        Map<String, Object> timeoutProp = new HashMap<>();
        timeoutProp.put("type", Arrays.asList("integer", "string"));
        timeoutProp.put("description", "扫描超时时间（秒，默认10秒）");
        properties.put("timeout", timeoutProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port"));

        server.addTool("dlqq_ble_scan",
            "扫描周围的BLE蓝牙设备。扫描完成后使用 dlqq_ble_get_devices 查看结果。",
            inputSchema,
            arguments -> {
                Integer port = parseInteger(arguments.get("port"), null);
                Integer timeoutSec = parseInteger(arguments.get("timeout"), 10);

                if (port == null) {
                    return errorResult("必须指定端口号（port）");
                }
                if (port < 1 || port > 65535) {
                    return errorResult("端口号必须在 1~65535 范围内");
                }

                try {
                    String result = BleTools.scanBleDevices(port, timeoutSec);
                    return textResult(result);
                } catch (Exception e) {
                    return errorResult("扫描BLE设备失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 35. dlqq_ble_get_devices - 获取扫描到的设备列表
     */
    private void registerBleGetDevicesTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", Arrays.asList("integer", "string"));
        portProp.put("description", "BLE工具的Python端口号（dlqq_run_ble 指定的端口号）");
        properties.put("port", portProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port"));

        server.addTool("dlqq_ble_get_devices",
            "获取扫描到的BLE设备列表。必须先执行 dlqq_ble_scan 扫描设备。",
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
                    String result = BleTools.getBleDevices(port);
                    return textResult(result);
                } catch (Exception e) {
                    return errorResult("获取设备列表失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 36. dlqq_ble_connect - 连接BLE设备
     */
    private void registerBleConnectTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", Arrays.asList("integer", "string"));
        portProp.put("description", "BLE工具的Python端口号（例如：8765、8766等）");
        properties.put("port", portProp);

        Map<String, Object> addressProp = new HashMap<>();
        addressProp.put("type", "string");
        addressProp.put("description", "BLE设备地址（如 AA:BB:CC:DD:EE:01）");
        properties.put("device_address", addressProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port", "device_address"));

        server.addTool("dlqq_ble_connect",
            "连接指定的BLE设备。设备地址可从 dlqq_ble_get_devices 获取。",
            inputSchema,
            arguments -> {
                Integer port = parseInteger(arguments.get("port"), null);
                String deviceAddress = (String) arguments.get("device_address");

                if (port == null) {
                    return errorResult("必须指定端口号（port）");
                }
                if (port < 1 || port > 65535) {
                    return errorResult("端口号必须在 1~65535 范围内");
                }
                if (deviceAddress == null || deviceAddress.trim().isEmpty()) {
                    return errorResult("设备地址不能为空");
                }

                try {
                    String result = BleTools.connectBleDevice(port, deviceAddress);
                    return textResult(result);
                } catch (Exception e) {
                    return errorResult("连接BLE设备失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 37. dlqq_ble_disconnect - 断开BLE设备连接
     */
    private void registerBleDisconnectTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", Arrays.asList("integer", "string"));
        portProp.put("description", "BLE工具的Python端口号（例如：8765、8766等）");
        properties.put("port", portProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port"));

        server.addTool("dlqq_ble_disconnect",
            "断开当前连接的BLE设备。",
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
                    String result = BleTools.disconnectBleDevice(port);
                    return textResult(result);
                } catch (Exception e) {
                    return errorResult("断开BLE设备失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 38. dlqq_ble_discover - 发现BLE服务和特征
     */
    private void registerBleDiscoverTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", Arrays.asList("integer", "string"));
        portProp.put("description", "BLE工具的Python端口号（例如：8765、8766等）");
        properties.put("port", portProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port"));

        server.addTool("dlqq_ble_discover",
            "发现已连接BLE设备的服务和特征。必须先连接设备。",
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
                    String result = BleTools.discoverBleServices(port);
                    return textResult(result);
                } catch (Exception e) {
                    return errorResult("发现服务失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 39. dlqq_ble_read - 读取BLE特征值
     */
    private void registerBleReadTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", Arrays.asList("integer", "string"));
        portProp.put("description", "BLE工具的Python端口号（例如：8765、8766等）");
        properties.put("port", portProp);

        Map<String, Object> serviceProp = new HashMap<>();
        serviceProp.put("type", "string");
        serviceProp.put("description", "服务UUID（如 0000180f-0000-1000-8000-00805f9b34fb）");
        properties.put("service_uuid", serviceProp);

        Map<String, Object> charProp = new HashMap<>();
        charProp.put("type", "string");
        charProp.put("description", "特征UUID（如 00002a19-0000-1000-8000-00805f9b34fb）");
        properties.put("char_uuid", charProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port", "service_uuid", "char_uuid"));

        server.addTool("dlqq_ble_read",
            "读取BLE设备的特征值。必须先连接设备并发现服务。",
            inputSchema,
            arguments -> {
                Integer port = parseInteger(arguments.get("port"), null);
                String serviceUuid = (String) arguments.get("service_uuid");
                String charUuid = (String) arguments.get("char_uuid");

                if (port == null) {
                    return errorResult("必须指定端口号（port）");
                }
                if (port < 1 || port > 65535) {
                    return errorResult("端口号必须在 1~65535 范围内");
                }
                if (serviceUuid == null || serviceUuid.trim().isEmpty()) {
                    return errorResult("服务UUID不能为空");
                }
                if (charUuid == null || charUuid.trim().isEmpty()) {
                    return errorResult("特征UUID不能为空");
                }

                try {
                    String result = BleTools.readBleCharacteristic(port, serviceUuid, charUuid);
                    return textResult(result);
                } catch (Exception e) {
                    return errorResult("读取特征失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 40. dlqq_ble_write_text - 写入BLE特征值（文本）
     */
    private void registerBleWriteTextTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", Arrays.asList("integer", "string"));
        portProp.put("description", "BLE工具的Python端口号（例如：8765、8766等）");
        properties.put("port", portProp);

        Map<String, Object> serviceProp = new HashMap<>();
        serviceProp.put("type", "string");
        serviceProp.put("description", "服务UUID");
        properties.put("service_uuid", serviceProp);

        Map<String, Object> charProp = new HashMap<>();
        charProp.put("type", "string");
        charProp.put("description", "特征UUID");
        properties.put("char_uuid", charProp);

        Map<String, Object> textProp = new HashMap<>();
        textProp.put("type", "string");
        textProp.put("description", "要写入的文本内容");
        properties.put("text", textProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port", "service_uuid", "char_uuid", "text"));

        server.addTool("dlqq_ble_write_text",
            "向BLE设备写入文本数据。必须先连接设备。",
            inputSchema,
            arguments -> {
                Integer port = parseInteger(arguments.get("port"), null);
                String serviceUuid = (String) arguments.get("service_uuid");
                String charUuid = (String) arguments.get("char_uuid");
                String text = (String) arguments.get("text");

                if (port == null) {
                    return errorResult("必须指定端口号（port）");
                }
                if (port < 1 || port > 65535) {
                    return errorResult("端口号必须在 1~65535 范围内");
                }
                if (serviceUuid == null || serviceUuid.trim().isEmpty()) {
                    return errorResult("服务UUID不能为空");
                }
                if (charUuid == null || charUuid.trim().isEmpty()) {
                    return errorResult("特征UUID不能为空");
                }
                if (text == null || text.isEmpty()) {
                    return errorResult("写入内容不能为空");
                }

                try {
                    String result = BleTools.writeBleCharacteristicText(port, serviceUuid, charUuid, text);
                    return textResult(result);
                } catch (Exception e) {
                    return errorResult("写入特征失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 41. dlqq_ble_write_hex - 写入BLE特征值（HEX）
     */
    private void registerBleWriteHexTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", Arrays.asList("integer", "string"));
        portProp.put("description", "BLE工具的Python端口号（例如：8765、8766等）");
        properties.put("port", portProp);

        Map<String, Object> serviceProp = new HashMap<>();
        serviceProp.put("type", "string");
        serviceProp.put("description", "服务UUID");
        properties.put("service_uuid", serviceProp);

        Map<String, Object> charProp = new HashMap<>();
        charProp.put("type", "string");
        charProp.put("description", "特征UUID");
        properties.put("char_uuid", charProp);

        Map<String, Object> hexArrayProp = new HashMap<>();
        hexArrayProp.put("type", "array");
        hexArrayProp.put("description", "16进制字符串数组（如 [\"FF\", \"01\", \"02\"]）");
        properties.put("hex_array", hexArrayProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port", "service_uuid", "char_uuid", "hex_array"));

        server.addTool("dlqq_ble_write_hex",
            "向BLE设备写入16进制数据。每个字符串应为偶数长度的16进制字符。",
            inputSchema,
            arguments -> {
                Integer port = parseInteger(arguments.get("port"), null);
                String serviceUuid = (String) arguments.get("service_uuid");
                String charUuid = (String) arguments.get("char_uuid");
                Object hexArrayRaw = arguments.get("hex_array");

                // 处理hex_array可能是List或String的情况
                List<String> hexArray;
                if (hexArrayRaw instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> temp = (List<String>) hexArrayRaw;
                    hexArray = temp;
                } else if (hexArrayRaw instanceof String) {
                    // 尝试解析JSON字符串
                    String hexStr = (String) hexArrayRaw;
                    try {
                        hexStr = hexStr.trim();
                        if (hexStr.startsWith("[")) {
                            com.google.gson.JsonArray jsonArray = com.google.gson.JsonParser.parseString(hexStr).getAsJsonArray();
                            hexArray = new ArrayList<>();
                            for (com.google.gson.JsonElement elem : jsonArray) {
                                hexArray.add(elem.getAsString());
                            }
                        } else {
                            // 单个hex字符串，按空格或逗号分割
                            hexArray = Arrays.asList(hexStr.split("[\\s,]+"));
                        }
                    } catch (Exception e) {
                        return errorResult("HEX数据格式错误，期望JSON数组或空格分隔的hex字符串: " + e.getMessage());
                    }
                } else {
                    return errorResult("HEX数据必须是字符串数组（如 [\"20\", \"A2\", \"00\", \"C2\"]）");
                }

                if (port == null) {
                    return errorResult("必须指定端口号（port）");
                }
                if (port < 1 || port > 65535) {
                    return errorResult("端口号必须在 1~65535 范围内");
                }
                if (serviceUuid == null || serviceUuid.trim().isEmpty()) {
                    return errorResult("服务UUID不能为空");
                }
                if (charUuid == null || charUuid.trim().isEmpty()) {
                    return errorResult("特征UUID不能为空");
                }
                if (hexArray == null || hexArray.isEmpty()) {
                    return errorResult("HEX数据不能为空");
                }

                try {
                    String result = BleTools.writeBleCharacteristicHex(port, serviceUuid, charUuid, hexArray);
                    return textResult(result);
                } catch (Exception e) {
                    return errorResult("写入HEX数据失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 42. dlqq_ble_subscribe - 订阅BLE通知
     */
    private void registerBleSubscribeTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", Arrays.asList("integer", "string"));
        portProp.put("description", "BLE工具的Python端口号（例如：8765、8766等）");
        properties.put("port", portProp);

        Map<String, Object> serviceProp = new HashMap<>();
        serviceProp.put("type", "string");
        serviceProp.put("description", "服务UUID");
        properties.put("service_uuid", serviceProp);

        Map<String, Object> charProp = new HashMap<>();
        charProp.put("type", "string");
        charProp.put("description", "特征UUID");
        properties.put("char_uuid", charProp);

        Map<String, Object> enableProp = new HashMap<>();
        enableProp.put("type", "boolean");
        enableProp.put("description", "是否启用通知（true启用，false取消）");
        properties.put("enable", enableProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port", "service_uuid", "char_uuid", "enable"));

        server.addTool("dlqq_ble_subscribe",
            "订阅或取消订阅BLE设备的特征通知。设备发送通知时将自动记录到日志。",
            inputSchema,
            arguments -> {
                Integer port = parseInteger(arguments.get("port"), null);
                String serviceUuid = (String) arguments.get("service_uuid");
                String charUuid = (String) arguments.get("char_uuid");
                Boolean enable = (Boolean) arguments.getOrDefault("enable", true);

                if (port == null) {
                    return errorResult("必须指定端口号（port）");
                }
                if (port < 1 || port > 65535) {
                    return errorResult("端口号必须在 1~65535 范围内");
                }
                if (serviceUuid == null || serviceUuid.trim().isEmpty()) {
                    return errorResult("服务UUID不能为空");
                }
                if (charUuid == null || charUuid.trim().isEmpty()) {
                    return errorResult("特征UUID不能为空");
                }

                try {
                    String result = BleTools.subscribeBleNotifications(port, serviceUuid, charUuid, enable);
                    return textResult(result);
                } catch (Exception e) {
                    return errorResult("订阅通知失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 43. dlqq_ble_get_log - 获取BLE日志数据
     */
    private void registerBleGetLogTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", Arrays.asList("integer", "string"));
        portProp.put("description", "BLE工具的Python端口号（例如：8765、8766等）");
        properties.put("port", portProp);

        Map<String, Object> startLineProp = new HashMap<>();
        startLineProp.put("type", Arrays.asList("integer", "string"));
        startLineProp.put("description", "起始行（1-based）");
        properties.put("start_line", startLineProp);

        Map<String, Object> endLineProp = new HashMap<>();
        endLineProp.put("type", Arrays.asList("integer", "string"));
        endLineProp.put("description", "结束行（1-based）");
        properties.put("end_line", endLineProp);

        Map<String, Object> lineCountProp = new HashMap<>();
        lineCountProp.put("type", Arrays.asList("integer", "string"));
        lineCountProp.put("description", "读取行数");
        properties.put("line_count", lineCountProp);

        Map<String, Object> waitTimeProp = new HashMap<>();
        waitTimeProp.put("type", Arrays.asList("integer", "string"));
        waitTimeProp.put("description", "等待时间（秒，默认0表示不等待，最大30秒）");
        properties.put("wait_time", waitTimeProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port"));

        server.addTool("dlqq_ble_get_log",
            "获取BLE工具缓冲区日志数据。支持3种模式：\n" +
            "模式1: 指定start_line和end_line，直接读取已有数据\n" +
            "模式2: 指定start_line和line_count，直接读取已有数据\n" +
            "模式3: 指定wait_time和line_count，等待读取新数据\n" +
            "注意：wait_time单位是秒，最大30秒\n" +
            "返回格式根据 dlqq_ble_switch_text_hex 设置（文本/HEX）",
            inputSchema,
            arguments -> {
                Integer port = parseInteger(arguments.get("port"), null);
                Integer startLine = parseInteger(arguments.get("start_line"), null);
                Integer endLine = parseInteger(arguments.get("end_line"), null);
                Integer lineCount = parseInteger(arguments.get("line_count"), null);
                Integer waitTimeSec = parseInteger(arguments.get("wait_time"), 0);

                if (port == null) {
                    return errorResult("必须指定端口号（port）");
                }
                if (port < 1 || port > 65535) {
                    return errorResult("端口号必须在 1~65535 范围内");
                }
                if (waitTimeSec < 0 || waitTimeSec > 30) {
                    return errorResult("wait_time必须在 0~30 秒范围内");
                }

                try {
                    String result = BleTools.getBleLog(port, startLine, endLine, lineCount, waitTimeSec);
                    return textResult(result);
                } catch (Exception e) {
                    return errorResult("获取日志失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 43a. dlqq_ble_get_log_text - 获取BLE日志（强制文本格式）
     */
    private void registerBleGetLogTextTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", Arrays.asList("integer", "string"));
        portProp.put("description", "BLE工具的Python端口号（例如：8765、8766等）");
        properties.put("port", portProp);

        Map<String, Object> startLineProp = new HashMap<>();
        startLineProp.put("type", Arrays.asList("integer", "string"));
        startLineProp.put("description", "起始行（1-based）");
        properties.put("start_line", startLineProp);

        Map<String, Object> endLineProp = new HashMap<>();
        endLineProp.put("type", Arrays.asList("integer", "string"));
        endLineProp.put("description", "结束行（1-based）");
        properties.put("end_line", endLineProp);

        Map<String, Object> lineCountProp = new HashMap<>();
        lineCountProp.put("type", Arrays.asList("integer", "string"));
        lineCountProp.put("description", "读取行数");
        properties.put("line_count", lineCountProp);

        Map<String, Object> waitTimeProp = new HashMap<>();
        waitTimeProp.put("type", Arrays.asList("integer", "string"));
        waitTimeProp.put("description", "等待时间（秒，默认0表示不等待，最大30秒）");
        properties.put("wait_time", waitTimeProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port"));

        server.addTool("dlqq_ble_get_log_text",
            "获取BLE缓冲区日志数据（强制文本格式）。支持3种读取模式，同dlqq_ble_get_log。\n" +
            "注意：wait_time单位是秒，最大30秒",
            inputSchema,
            arguments -> {
                Integer port = parseInteger(arguments.get("port"), null);
                Integer startLine = parseInteger(arguments.get("start_line"), null);
                Integer endLine = parseInteger(arguments.get("end_line"), null);
                Integer lineCount = parseInteger(arguments.get("line_count"), null);
                Integer waitTimeSec = parseInteger(arguments.get("wait_time"), 0);

                if (port == null) {
                    return errorResult("必须指定端口号（port）");
                }
                if (port < 1 || port > 65535) {
                    return errorResult("端口号必须在 1~65535 范围内");
                }
                if (waitTimeSec < 0 || waitTimeSec > 30) {
                    return errorResult("wait_time必须在 0~30 秒范围内");
                }

                try {
                    String result = BleTools.getBleLogText(port, startLine, endLine, lineCount, waitTimeSec);
                    return textResult(result);
                } catch (Exception e) {
                    return errorResult("获取日志失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 43b. dlqq_ble_get_log_hex - 获取BLE日志（强制HEX格式）
     */
    private void registerBleGetLogHexTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", Arrays.asList("integer", "string"));
        portProp.put("description", "BLE工具的Python端口号（例如：8765、8766等）");
        properties.put("port", portProp);

        Map<String, Object> startLineProp = new HashMap<>();
        startLineProp.put("type", Arrays.asList("integer", "string"));
        startLineProp.put("description", "起始行（1-based）");
        properties.put("start_line", startLineProp);

        Map<String, Object> endLineProp = new HashMap<>();
        endLineProp.put("type", Arrays.asList("integer", "string"));
        endLineProp.put("description", "结束行（1-based）");
        properties.put("end_line", endLineProp);

        Map<String, Object> lineCountProp = new HashMap<>();
        lineCountProp.put("type", Arrays.asList("integer", "string"));
        lineCountProp.put("description", "读取行数");
        properties.put("line_count", lineCountProp);

        Map<String, Object> waitTimeProp = new HashMap<>();
        waitTimeProp.put("type", Arrays.asList("integer", "string"));
        waitTimeProp.put("description", "等待时间（秒，默认0表示不等待，最大30秒）");
        properties.put("wait_time", waitTimeProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port"));

        server.addTool("dlqq_ble_get_log_hex",
            "获取BLE缓冲区日志数据（强制HEX格式）。支持3种读取模式，同dlqq_ble_get_log。\n" +
            "注意：wait_time单位是秒，最大30秒",
            inputSchema,
            arguments -> {
                Integer port = parseInteger(arguments.get("port"), null);
                Integer startLine = parseInteger(arguments.get("start_line"), null);
                Integer endLine = parseInteger(arguments.get("end_line"), null);
                Integer lineCount = parseInteger(arguments.get("line_count"), null);
                Integer waitTimeSec = parseInteger(arguments.get("wait_time"), 0);

                if (port == null) {
                    return errorResult("必须指定端口号（port）");
                }
                if (port < 1 || port > 65535) {
                    return errorResult("端口号必须在 1~65535 范围内");
                }
                if (waitTimeSec < 0 || waitTimeSec > 30) {
                    return errorResult("wait_time必须在 0~30 秒范围内");
                }

                try {
                    String result = BleTools.getBleLogHex(port, startLine, endLine, lineCount, waitTimeSec);
                    return textResult(result);
                } catch (Exception e) {
                    return errorResult("获取日志失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 44. dlqq_ble_clear - 清除BLE日志
     */
    private void registerBleClearTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", Arrays.asList("integer", "string"));
        portProp.put("description", "BLE工具的Python端口号（例如：8765、8766等）");
        properties.put("port", portProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port"));

        server.addTool("dlqq_ble_clear",
            "清除BLE工具接收的数据缓冲区。",
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
                    String result = BleTools.clearBleLog(port);
                    return textResult(result);
                } catch (Exception e) {
                    return errorResult("清除日志失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 44a. dlqq_ble_get_line_count - 获取BLE缓冲区行数
     */
    private void registerBleGetLineCountTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", Arrays.asList("integer", "string"));
        portProp.put("description", "BLE工具的Python端口号（例如：8765、8766等）");
        properties.put("port", portProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port"));

        server.addTool("dlqq_ble_get_line_count",
            "获取BLE工具缓冲区当前的日志行数。",
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
                    String result = BleTools.getLineCount(port);
                    return textResult(result);
                } catch (Exception e) {
                    return errorResult("获取行数失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 45. dlqq_ble_set_buffer_size - 设置BLE缓冲区大小
     */
    private void registerBleSetBufferSizeTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", Arrays.asList("integer", "string"));
        portProp.put("description", "BLE工具的Python端口号（例如：8765、8766等）");
        properties.put("port", portProp);

        Map<String, Object> sizeProp = new HashMap<>();
        sizeProp.put("type", Arrays.asList("integer", "string"));
        sizeProp.put("description", "缓冲区大小（字节），默认5MB（5242880字节）");
        properties.put("size_bytes", sizeProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port", "size_bytes"));

        server.addTool("dlqq_ble_set_buffer_size",
            "设置BLE工具的数据缓冲区大小，单位字节。大小无限制，默认5MB。超出缓冲区，最早的数据会被丢弃。",
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
                    String result = BleTools.setBleBufferSize(port, sizeBytes);
                    return textResult(result);
                } catch (Exception e) {
                    return errorResult("设置缓冲区大小失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 46. dlqq_ble_switch_text_hex - 切换BLE日志显示格式
     */
    private void registerBleSwitchTextHexTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", Arrays.asList("integer", "string"));
        portProp.put("description", "BLE工具的Python端口号（例如：8765、8766等）");
        properties.put("port", portProp);

        Map<String, Object> formatProp = new HashMap<>();
        formatProp.put("type", "string");
        formatProp.put("description", "显示格式：'text' 表示文本，'hex' 表示HEX");
        properties.put("format", formatProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port", "format"));

        server.addTool("dlqq_ble_switch_text_hex",
            "切换BLE日志的显示格式。切换后，UI日志显示和 dlqq_ble_get_log 返回的数据格式都会相应改变。",
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
                    String result = BleTools.switchDisplayFormat(port, format);
                    return textResult(result);
                } catch (Exception e) {
                    return errorResult("切换显示格式失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 47. dlqq_exit_ble - 退出BLE工具
     */
    private void registerExitBleTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", Arrays.asList("integer", "string"));
        portProp.put("description", "BLE工具的Python端口号（dlqq_run_ble 指定的端口号）");
        properties.put("port", portProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port"));

        server.addTool("dlqq_exit_ble",
            "关闭指定端口号的BLE工具窗口及其进程，释放所有资源。调用后该端口的BLE工具将完全停止运行。",
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
                    String result = BleTools.exitBle(port);
                    return textResult(result);
                } catch (Exception e) {
                    return errorResult("关闭BLE工具失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 47a. dlqq_rerun_ble - 重新启动BLE工具
     */
    private void registerRerunBleTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", Arrays.asList("integer", "string"));
        portProp.put("description", "BLE工具的Python端口号（先关闭再启动该端口）");
        properties.put("port", portProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port"));

        server.addTool("dlqq_rerun_ble",
            "重新启动指定端口号的BLE工具。先关闭该端口的BLE工具（如果正在运行），然后重新启动。用于刷新BLE工具窗口或解决连接问题。",
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
                    // 先关闭（如果正在运行）
                    StringBuilder result = new StringBuilder();
                    try {
                        String exitResult = BleTools.exitBle(port);
                        result.append("关闭旧窗口: ").append(exitResult).append("\n");
                    } catch (Exception e) {
                        result.append("旧窗口未运行或关闭失败: ").append(e.getMessage()).append("\n");
                    }

                    // 等待一小段时间确保进程完全关闭
                    Thread.sleep(500);

                    // 重新启动
                    String runResult = BleTools.runBle(port);
                    result.append("启动新窗口: ").append(runResult);
                    return textResult(result.toString());
                } catch (Exception e) {
                    return errorResult("重新启动BLE工具失败: " + e.getMessage());
                }
            }
        );
    }

    // ==================== 系统信息工具 ====================

    /**
     * dlqq_get_mcp_version - 获取MCP服务器版本和构建信息
     */
    private void registerGetMcpVersionTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.emptyList());

        server.addTool("dlqq_get_mcp_version",
            "获取MCP服务器的版本信息、JAR文件修改时间和构建信息，用于确认代码是否已更新",
            inputSchema,
            arguments -> {
                String result = McpTools.getMcpVersion();
                return textResult(result);
            }
        );
    }

    /**
     * 48. dlqq_check_bluetooth_open - 检查电脑蓝牙是否开启
     */
    private void registerCheckBluetoothTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.emptyList());

        server.addTool("dlqq_check_bluetooth_open",
            "检查Windows电脑的蓝牙适配器是否已开启",
            inputSchema,
            arguments -> {
                String result = BleTools.checkBluetoothOpen();
                return textResult(result);
            }
        );
    }

    // ==================== MCP数据管理工具 ====================

    /**
     * dlqq_clear_mcp_data - 清除MCP数据目录
     */
    private void registerClearMcpDataTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.emptyList());

        server.addTool("dlqq_clear_mcp_data",
            "清除MCP数据目录（.dlqq），删除整个目录及其所有内容（包括日志、Python脚本等）。即使部分文件被占用也会返回成功结果，并告知哪些文件被占用。",
            inputSchema,
            arguments -> {
                String result = McpTools.clearDlqqData();
                return textResult(result);
            }
        );
    }

    /**
     * dlqq_clear_mcp_cache - 清除MCP缓存目录
     */
    private void registerClearMcpCacheTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.emptyList());

        server.addTool("dlqq_clear_mcp_cache",
            "清除MCP缓存目录（.dlqq/cache），删除缓存的所有文件和子目录。即使部分文件被占用也会返回成功结果，并告知哪些文件被占用。",
            inputSchema,
            arguments -> {
                String result = McpTools.clearCache();
                return textResult(result);
            }
        );
    }

    // ==================== 项目构建和芯片操作工具 ====================

    private void registerBuildProjectTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.emptyList());

        server.addTool("dlqq_build_project",
            "构建项目。执行settings.properties中script.build定义的脚本。默认执行util/build/build.bat。",
            inputSchema,
            arguments -> {
                String result = McpTools.buildProject();
                return textResult(result);
            }
        );
    }

    private void registerRebuildProjectTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.emptyList());

        server.addTool("dlqq_rebuild_project",
            "重建项目（clean + build）。执行settings.properties中script.rebuild定义的脚本。默认执行util/build/rebuild.bat。",
            inputSchema,
            arguments -> {
                String result = McpTools.rebuildProject();
                return textResult(result);
            }
        );
    }

    private void registerChipEraseTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.emptyList());

        server.addTool("dlqq_chip_erase",
            "擦除芯片。执行settings.properties中script.chip.erase定义的脚本。默认执行util/flash/erase.bat。",
            inputSchema,
            arguments -> {
                String result = McpTools.chipErase();
                return textResult(result);
            }
        );
    }

    private void registerChipDownloadTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.emptyList());

        server.addTool("dlqq_chip_download",
            "下载固件到芯片。执行settings.properties中script.chip.download定义的脚本。默认执行util/flash/download.bat。",
            inputSchema,
            arguments -> {
                String result = McpTools.chipDownload();
                return textResult(result);
            }
        );
    }

    // ==================== 组合脚本执行工具 ====================

    private void registerEraseDownloadTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.emptyList());

        server.addTool("dlqq_erase_download",
            "擦除芯片并下载固件。执行settings.properties中script.erase.download定义的脚本。默认执行util/flash/erase_download.bat。",
            inputSchema,
            arguments -> {
                String result = McpTools.eraseDownload();
                return textResult(result);
            }
        );
    }

    private void registerBuildEraseDownloadTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.emptyList());

        server.addTool("dlqq_build_erase_download",
            "构建项目、擦除芯片并下载固件。执行settings.properties中script.build.erase.download定义的脚本。默认执行util/build/build_erase_download.bat。",
            inputSchema,
            arguments -> {
                String result = McpTools.buildEraseDownload();
                return textResult(result);
            }
        );
    }

    private void registerRebuildEraseDownloadTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.emptyList());

        server.addTool("dlqq_rebuild_erase_download",
            "重建项目、擦除芯片并下载固件。执行settings.properties中script.rebuild.erase.download定义的脚本。",
            inputSchema,
            arguments -> {
                String result = McpTools.rebuildEraseDownload();
                return textResult(result);
            }
        );
    }

    // ==================== 运行调试工具 ====================

    private void registerRunProjectTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.emptyList());

        server.addTool("dlqq_run_project",
            "运行项目。执行settings.properties中script.run定义的脚本。",
            inputSchema,
            arguments -> {
                String result = McpTools.runProject();
                return textResult(result);
            }
        );
    }

    private void registerDebugProjectTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.emptyList());

        server.addTool("dlqq_debug_project",
            "调试项目。执行settings.properties中script.debug定义的脚本。",
            inputSchema,
            arguments -> {
                String result = McpTools.debugProject();
                return textResult(result);
            }
        );
    }

    private void registerBuildRunProjectTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.emptyList());

        server.addTool("dlqq_build_run_project",
            "构建并运行项目。执行settings.properties中script.build.run定义的脚本。",
            inputSchema,
            arguments -> {
                String result = McpTools.buildRunProject();
                return textResult(result);
            }
        );
    }

    private void registerBuildDebugProjectTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.emptyList());

        server.addTool("dlqq_build_debug_project",
            "构建并调试项目。执行settings.properties中script.build.debug定义的脚本。",
            inputSchema,
            arguments -> {
                String result = McpTools.buildDebugProject();
                return textResult(result);
            }
        );
    }

    private void registerRebuildRunProjectTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.emptyList());

        server.addTool("dlqq_rebuild_run_project",
            "重建并运行项目。执行settings.properties中script.rebuild.run定义的脚本。",
            inputSchema,
            arguments -> {
                String result = McpTools.rebuildRunProject();
                return textResult(result);
            }
        );
    }

    private void registerRebuildDebugProjectTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.emptyList());

        server.addTool("dlqq_rebuild_debug_project",
            "重建并调试项目。执行settings.properties中script.rebuild.debug定义的脚本。",
            inputSchema,
            arguments -> {
                String result = McpTools.rebuildDebugProject();
                return textResult(result);
            }
        );
    }

    // ==================== MCP数据管理工具 ====================

    /**
     * 获取MCP配置属性
     */
    private void registerGetMcpPropertiesTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> keyProp = new HashMap<>();
        keyProp.put("type", "string");
        keyProp.put("description", "属性键名（可选）。如果指定，只返回该属性的值；如果不指定，返回所有属性列表");
        properties.put("key", keyProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.emptyList());

        server.addTool("dlqq_get_mcp_properties",
            "获取MCP配置属性。可以获取所有属性列表，或指定属性键获取单个属性值。配置存储在 .dlqq/settings/settings.properties 文件中。",
            inputSchema,
            arguments -> {
                String key = (String) arguments.get("key");

                if (key != null && !key.isEmpty()) {
                    // 获取单个属性
                    String value = McpTools.getConfigFromSettings(key);
                    if (value == null) {
                        return textResult("属性 '" + key + "' 不存在");
                    }
                    return textResult(key + " = " + value);
                } else {
                    // 获取所有属性
                    Map<String, String> props = McpTools.getAllProperties();
                    String formatted = McpTools.formatProperties(props);
                    return textResult(formatted);
                }
            }
        );
    }

    /**
     * 设置MCP配置属性
     */
    private void registerSetMcpPropertiesTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> keyProp = new HashMap<>();
        keyProp.put("type", "string");
        keyProp.put("description", "属性键名，例如：response.max.size、search.max.files、script.build");
        properties.put("key", keyProp);

        Map<String, Object> valueProp = new HashMap<>();
        valueProp.put("type", "string");
        valueProp.put("description", "属性值");
        properties.put("value", valueProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.emptyList());

        server.addTool("dlqq_set_mcp_properties",
            "设置MCP配置属性。至少指定 key 和 value 其中一个参数。配置保存到 .dlqq/settings/settings.properties 文件中。常用属性包括：response.max.size（响应大小限制，默认25804字节约25KB，最大258048）、search.max.files（最大搜索文件数）、search.max.file.size（最大文件大小）、search.text.extensions（文本文件后缀）、script.build（构建脚本路径）等。",
            inputSchema,
            arguments -> {
                String key = (String) arguments.get("key");
                String value = (String) arguments.get("value");

                if ((key == null || key.isEmpty()) && (value == null || value.isEmpty())) {
                    return errorResult("至少指定一个参数：key 或 value");
                }

                // 如果只指定了key，显示该属性的当前值
                if (key != null && !key.isEmpty() && (value == null || value.isEmpty())) {
                    String currentValue = McpTools.getConfigFromSettings(key);
                    if (currentValue == null) {
                        return textResult("属性 '" + key + "' 不存在");
                    }
                    return textResult(key + " = " + currentValue + "\n\n使用 dlqq_set_mcp_properties 并指定 value 参数来修改该属性");
                }

                // 设置属性
                // 对 response.max.size 进行验证，不能超过API上限
                if ("response.max.size".equals(key)) {
                    try {
                        int size = Integer.parseInt(value);
                        if (size > McpTools.MAX_RESPONSE_SIZE) {
                            return errorResult(String.format("response.max.size 不能超过 %d (API上限)", McpTools.MAX_RESPONSE_SIZE));
                        }
                        if (size < 1) {
                            return errorResult("response.max.size 必须大于0");
                        }
                    } catch (NumberFormatException e) {
                        return errorResult("response.max.size 必须是有效的整数");
                    }
                }
                String result = McpTools.setProperty(key, value);
                return textResult(result);
            }
        );
    }

    /**
     * dlqq_clear_mcp_log - 清除MCP日志文件
     */
    private void registerClearMcpLogTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.emptyList());

        server.addTool("dlqq_clear_mcp_log",
            "清除MCP日志文件（.dlqq/log目录），保留目录结构。即使部分文件被占用也会返回成功结果，并告知哪些文件被占用。",
            inputSchema,
            arguments -> {
                String result = McpTools.clearLog();
                return textResult(result);
            }
        );
    }

    // ==================== 脚本路径设置工具 ====================

    /**
     * 通用注册脚本路径设置工具
     */
    private void registerSetScriptTool(McpServer server, String toolName, String description,
                                       java.util.function.Function<String, String> setter) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> pathProp = new HashMap<>();
        pathProp.put("type", "string");
        pathProp.put("description", "脚本路径（.bat或.sh脚本的绝对路径）");
        properties.put("script_path", pathProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.singletonList("script_path"));

        server.addTool(toolName, description, inputSchema, arguments -> {
            String scriptPath = (String) arguments.get("script_path");
            if (scriptPath == null || scriptPath.trim().isEmpty()) {
                return errorResult("脚本路径不能为空");
            }
            String result = setter.apply(scriptPath.trim());
            return textResult(result);
        });
    }

    /**
     * dlqq_set_build_script - 设置构建脚本路径
     */
    private void registerSetBuildScriptTool(McpServer server) {
        registerSetScriptTool(server, "dlqq_set_build_script",
            "设置构建脚本路径。配置保存到 .dlqq/settings/settings.properties 的 script.build 项。",
            McpTools::setBuildScript);
    }

    /**
     * dlqq_get_tool_list - 获取工具列表
     */
    private void registerGetToolListTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> filterProp = new HashMap<>();
        filterProp.put("type", "string");
        filterProp.put("description", "名称过滤关键字（可选，如 wifi、keyboard、mouse、set_build，为空返回全部工具）");
        properties.put("filter", filterProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.emptyList());

        server.addTool("dlqq_get_tool_list",
            "获取MCP服务器上所有已注册工具的列表（工具名称和描述）。支持按名称关键字过滤。",
            inputSchema,
            arguments -> {
                String filter = (String) arguments.get("filter");
                return textResult(server.getToolsInfo(filter));
            }
        );
    }

    /**
     * dlqq_set_rebuild_script - 设置重建脚本路径
     */
    private void registerSetRebuildScriptTool(McpServer server) {
        registerSetScriptTool(server, "dlqq_set_rebuild_script",
            "设置重建脚本路径。配置保存到 .dlqq/settings/settings.properties 的 script.rebuild 项。",
            McpTools::setRebuildScript);
    }

    /**
     * dlqq_set_chip_erase_script - 设置芯片擦除脚本路径
     */
    private void registerSetChipEraseScriptTool(McpServer server) {
        registerSetScriptTool(server, "dlqq_set_chip_erase_script",
            "设置芯片擦除脚本路径。配置保存到 .dlqq/settings/settings.properties 的 script.chip.erase 项。",
            McpTools::setChipEraseScript);
    }

    /**
     * dlqq_set_chip_download_script - 设置芯片下载脚本路径
     */
    private void registerSetChipDownloadScriptTool(McpServer server) {
        registerSetScriptTool(server, "dlqq_set_chip_download_script",
            "设置芯片下载脚本路径。配置保存到 .dlqq/settings/settings.properties 的 script.chip.download 项。",
            McpTools::setChipDownloadScript);
    }

    /**
     * dlqq_set_erase_download_script - 设置擦除并下载组合脚本路径
     */
    private void registerSetEraseDownloadScriptTool(McpServer server) {
        registerSetScriptTool(server, "dlqq_set_erase_download_script",
            "设置擦除并下载组合脚本路径。配置保存到 .dlqq/settings/settings.properties 的 script.erase.download 项。",
            McpTools::setEraseDownloadScript);
    }

    /**
     * dlqq_set_build_erase_download_script - 设置构建擦除下载组合脚本路径
     */
    private void registerSetBuildEraseDownloadScriptTool(McpServer server) {
        registerSetScriptTool(server, "dlqq_set_build_erase_download_script",
            "设置构建、擦除并下载组合脚本路径。配置保存到 .dlqq/settings/settings.properties 的 script.build.erase.download 项。",
            McpTools::setBuildEraseDownloadScript);
    }

    /**
     * dlqq_set_rebuild_erase_download_script - 设置重建擦除下载组合脚本路径
     */
    private void registerSetRebuildEraseDownloadScriptTool(McpServer server) {
        registerSetScriptTool(server, "dlqq_set_rebuild_erase_download_script",
            "设置重建、擦除并下载组合脚本路径。配置保存到 .dlqq/settings/settings.properties 的 script.rebuild.erase.download 项。",
            McpTools::setRebuildEraseDownloadScript);
    }

    /**
     * dlqq_set_run_script - 设置运行脚本路径
     */
    private void registerSetRunScriptTool(McpServer server) {
        registerSetScriptTool(server, "dlqq_set_run_script",
            "设置运行项目脚本路径。配置保存到 .dlqq/settings/settings.properties 的 script.run 项。",
            McpTools::setRunScript);
    }

    /**
     * dlqq_set_debug_script - 设置调试脚本路径
     */
    private void registerSetDebugScriptTool(McpServer server) {
        registerSetScriptTool(server, "dlqq_set_debug_script",
            "设置调试项目脚本路径。配置保存到 .dlqq/settings/settings.properties 的 script.debug 项。",
            McpTools::setDebugScript);
    }

    /**
     * dlqq_set_build_run_script - 设置构建并运行脚本路径
     */
    private void registerSetBuildRunScriptTool(McpServer server) {
        registerSetScriptTool(server, "dlqq_set_build_run_script",
            "设置构建并运行组合脚本路径。配置保存到 .dlqq/settings/settings.properties 的 script.build.run 项。",
            McpTools::setBuildRunScript);
    }

    /**
     * dlqq_set_build_debug_script - 设置构建并调试脚本路径
     */
    private void registerSetBuildDebugScriptTool(McpServer server) {
        registerSetScriptTool(server, "dlqq_set_build_debug_script",
            "设置构建并调试组合脚本路径。配置保存到 .dlqq/settings/settings.properties 的 script.build.debug 项。",
            McpTools::setBuildDebugScript);
    }

    /**
     * dlqq_set_rebuild_run_script - 设置重建并运行脚本路径
     */
    private void registerSetRebuildRunScriptTool(McpServer server) {
        registerSetScriptTool(server, "dlqq_set_rebuild_run_script",
            "设置重建并运行组合脚本路径。配置保存到 .dlqq/settings/settings.properties 的 script.rebuild.run 项。",
            McpTools::setRebuildRunScript);
    }

    /**
     * dlqq_set_rebuild_debug_script - 设置重建并调试脚本路径
     */
    private void registerSetRebuildDebugScriptTool(McpServer server) {
        registerSetScriptTool(server, "dlqq_set_rebuild_debug_script",
            "设置重建并调试组合脚本路径。配置保存到 .dlqq/settings/settings.properties 的 script.rebuild.debug 项。",
            McpTools::setRebuildDebugScript);
    }

    /**
     * dlqq_reset_mcp_properties - 重置MCP配置为默认值
     */
    private void registerResetMcpPropertiesTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> keyProp = new HashMap<>();
        keyProp.put("type", "string");
        keyProp.put("description", "要重置的属性键名（可选）。如果为空，则重置所有配置为默认值");
        properties.put("key", keyProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.emptyList());

        server.addTool("dlqq_reset_mcp_properties",
            "重置MCP配置属性为默认值。可以重置单个属性或所有属性。配置保存到 .dlqq/settings/settings.properties 文件中。",
            inputSchema,
            arguments -> {
                String key = (String) arguments.get("key");
                String result = McpTools.resetProperties(key);
                return textResult(result);
            }
        );
    }

    /**
     * dlqq_read_office_file - 读取Office文件并转换为Markdown
     */
    private void registerReadOfficeFileTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> pathProp = new HashMap<>();
        pathProp.put("type", "string");
        pathProp.put("description", "Office文件路径（支持PDF、DOC、DOCX、XLS、XLSX、PPT、PPTX）");
        properties.put("file_path", pathProp);

        Map<String, Object> startPageProp = new HashMap<>();
        startPageProp.put("type", "integer");
        startPageProp.put("description", "起始页码/段落/Sheet/幻灯片索引（可选，从1开始。PDF:页码；Word:段落；Excel:Sheet；PPT:幻灯片）");
        properties.put("start_page", startPageProp);

        Map<String, Object> endPageProp = new HashMap<>();
        endPageProp.put("type", "integer");
        endPageProp.put("description", "结束页码/段落/Sheet/幻灯片索引（可选，包含该页。不指定则处理到末尾，最大100页/段/Sheet/幻灯片）");
        properties.put("end_page", endPageProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.singletonList("file_path"));

        server.addTool("dlqq_read_office_file",
            "读取Office文件（PDF、DOC、DOCX、XLS、XLSX、PPT、PPTX）并转换为Markdown格式。支持分页/分段处理：PDF按页码、Word按段落、Excel按Sheet、PPT按幻灯片。转换后的文件保存在 .dlqq/cache 目录，返回Markdown文件路径。",
            inputSchema,
            arguments -> {
                String filePath = (String) arguments.get("file_path");
                Integer startPage = null;
                Integer endPage = null;
                
                if (arguments.get("start_page") != null) {
                    startPage = ((Number) arguments.get("start_page")).intValue();
                }
                if (arguments.get("end_page") != null) {
                    endPage = ((Number) arguments.get("end_page")).intValue();
                }

                if (filePath == null || filePath.trim().isEmpty()) {
                    return errorResult("文件路径不能为空");
                }

                try {
                    Map<String, Object> result = OfficeTools.readOfficeFile(filePath, startPage, endPage);

                    StringBuilder sb = new StringBuilder();
                    sb.append("✓ Office文件转换成功\n");
                    sb.append("===================\n\n");
                    sb.append("原始文件: ").append(result.get("originalPath")).append("\n");
                    sb.append("标题: ").append(result.get("title")).append("\n");
                    if (startPage != null || endPage != null) {
                        sb.append("处理范围: ").append(startPage != null ? startPage : "起始").append("-").append(endPage != null ? endPage : "末尾").append("\n");
                    }
                    sb.append("\n");
                    sb.append("Markdown文件: ").append(result.get("markdownPath")).append("\n");
                    sb.append("文件大小: ").append(result.get("size")).append(" 字节\n\n");
                    sb.append("提示: 请使用 dlqq_read_file 工具读取上述Markdown文件内容。");

                    return textResult(sb.toString());
                } catch (IOException e) {
                    return errorResult("文件转换失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * dlqq_convert_office_file - 将Office文件从一种格式转换为另一种格式
     */
    private void registerConvertOfficeFileTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> sourceProp = new HashMap<>();
        sourceProp.put("type", "string");
        sourceProp.put("description", "源文件路径（支持PDF、DOC、DOCX、XLS、XLSX、PPT、PPTX、MD、TXT）");
        properties.put("source_path", sourceProp);

        Map<String, Object> formatProp = new HashMap<>();
        formatProp.put("type", "string");
        formatProp.put("description", "目标格式（docx/xlsx/pptx/md/txt，pdf暂不支持）");
        properties.put("target_format", formatProp);

        Map<String, Object> outputProp = new HashMap<>();
        outputProp.put("type", "string");
        outputProp.put("description", "输出路径（可选，默认保存到.dlqq/cache目录）");
        properties.put("output_path", outputProp);

        Map<String, Object> startPageProp = new HashMap<>();
        startPageProp.put("type", "integer");
        startPageProp.put("description", "起始页码/段落/Sheet/幻灯片索引（可选，从1开始）");
        properties.put("start_page", startPageProp);

        Map<String, Object> endPageProp = new HashMap<>();
        endPageProp.put("type", "integer");
        endPageProp.put("description", "结束页码/段落/Sheet/幻灯片索引（可选，包含该页）");
        properties.put("end_page", endPageProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("source_path", "target_format"));

        server.addTool("dlqq_convert_office_file",
            "将Office文件从一种格式转换为另一种格式。支持: PDF/DOC/DOCX/XLS/XLSX/PPT/PPTX/MD/TXT → DOCX/XLSX/PPTX/MD/TXT。支持分页/分段处理。输出路径可选，默认为.dlqq/cache目录。",
            inputSchema,
            arguments -> {
                String sourcePath = (String) arguments.get("source_path");
                String targetFormat = (String) arguments.get("target_format");
                String outputPath = (String) arguments.get("output_path");
                Integer startPage = null;
                Integer endPage = null;
                
                if (arguments.get("start_page") != null) {
                    startPage = ((Number) arguments.get("start_page")).intValue();
                }
                if (arguments.get("end_page") != null) {
                    endPage = ((Number) arguments.get("end_page")).intValue();
                }

                if (sourcePath == null || sourcePath.trim().isEmpty()) {
                    return errorResult("源文件路径不能为空");
                }

                if (targetFormat == null || targetFormat.trim().isEmpty()) {
                    return errorResult("目标格式不能为空");
                }

                try {
                    Map<String, Object> result = OfficeTools.convertOfficeFile(sourcePath, targetFormat, outputPath, startPage, endPage);

                    StringBuilder sb = new StringBuilder();
                    sb.append("✓ Office文件格式转换成功\n");
                    sb.append("===================\n\n");
                    sb.append("源文件: ").append(result.get("sourcePath")).append("\n");
                    sb.append("源格式: ").append(result.get("sourceFormat")).append("\n");
                    sb.append("目标格式: ").append(result.get("targetFormat")).append("\n");
                    if (startPage != null || endPage != null) {
                        sb.append("处理范围: ").append(startPage != null ? startPage : "起始").append("-").append(endPage != null ? endPage : "末尾").append("\n");
                    }
                    sb.append("\n输出文件: ").append(result.get("outputPath")).append("\n");
                    sb.append("文件大小: ").append(result.get("size")).append(" 字节\n");

                    return textResult(sb.toString());
                } catch (IOException e) {
                    return errorResult("文件格式转换失败: " + e.getMessage());
                }
            }
        );
    }

    // ==================== 压缩/解压工具 ====================

    /**
     * dlqq_zip - 压缩文件或目录
     */
    private void registerZipTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> sourcesProp = new HashMap<>();
        sourcesProp.put("type", "array");
        sourcesProp.put("description", "源文件/目录路径列表，支持多个文件或目录");
        properties.put("source_paths", sourcesProp);

        Map<String, Object> outputProp = new HashMap<>();
        outputProp.put("type", "string");
        outputProp.put("description", "输出压缩文件路径（可选，默认在源目录生成）");
        properties.put("output_path", outputProp);

        Map<String, Object> formatProp = new HashMap<>();
        formatProp.put("type", "string");
        formatProp.put("description", "压缩格式（可选，默认zip，目前仅支持zip）");
        formatProp.put("enum", Arrays.asList("zip"));
        properties.put("format", formatProp);

        Map<String, Object> passwordProp = new HashMap<>();
        passwordProp.put("type", "string");
        passwordProp.put("description", "压缩密码（可选，为空表示不加密）");
        properties.put("password", passwordProp);

        Map<String, Object> levelProp = new HashMap<>();
        levelProp.put("type", Arrays.asList("integer", "string"));
        levelProp.put("description", "压缩级别（1-9，默认5，1最快压缩率最低，9最慢压缩率最高）");
        properties.put("compression_level", levelProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("source_paths"));

        server.addTool("dlqq_zip",
            "压缩文件或目录为ZIP格式。支持多文件/目录压缩、密码保护、压缩级别设置。"
            + "支持的格式：zip。可设置1-9级压缩级别，数值越大压缩率越高但速度越慢。"
            + "默认输出到源文件所在目录。",
            inputSchema,
            arguments -> {
                @SuppressWarnings("unchecked")
                List<String> sourcePaths = (List<String>) arguments.get("source_paths");
                String outputPath = (String) arguments.get("output_path");
                String format = (String) arguments.get("format");
                String password = (String) arguments.get("password");
                Integer compressionLevel = parseInteger(arguments.get("compression_level"), 5);

                if (sourcePaths == null || sourcePaths.isEmpty()) {
                    return errorResult("源文件路径不能为空");
                }

                try {
                    Map<String, Object> result = ZipTools.zipFiles(sourcePaths, outputPath, format, password, compressionLevel);

                    StringBuilder sb = new StringBuilder();
                    sb.append("✓ 压缩成功\n");
                    sb.append("===================\n\n");
                    sb.append("输出文件: ").append(result.get("outputPath")).append("\n");
                    sb.append("格式: ").append(result.get("format")).append("\n");
                    sb.append("原始大小: ").append(formatFileSize((Long) result.get("originalSize"))).append("\n");
                    sb.append("压缩后大小: ").append(formatFileSize((Long) result.get("compressedSize"))).append("\n");
                    sb.append("压缩率: ").append(result.get("compressionRatio")).append("\n");
                    sb.append("文件数: ").append(result.get("fileCount")).append("\n");
                    sb.append("密码保护: ").append(((Boolean) result.get("passwordProtected")) ? "是" : "否").append("\n");

                    return textResult(sb.toString());
                } catch (IOException e) {
                    return errorResult("压缩失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * dlqq_unzip - 解压ZIP文件
     */
    private void registerUnzipTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> zipPathProp = new HashMap<>();
        zipPathProp.put("type", "string");
        zipPathProp.put("description", "ZIP文件路径");
        properties.put("zip_path", zipPathProp);

        Map<String, Object> outputDirProp = new HashMap<>();
        outputDirProp.put("type", "string");
        outputDirProp.put("description", "解压输出目录（可选，默认为ZIP文件所在目录下的同名文件夹）");
        properties.put("output_dir", outputDirProp);

        Map<String, Object> passwordProp = new HashMap<>();
        passwordProp.put("type", "string");
        passwordProp.put("description", "解压密码（可选，如果ZIP有密码则需要提供）");
        properties.put("password", passwordProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("zip_path"));

        server.addTool("dlqq_unzip",
            "解压ZIP文件。支持密码保护的ZIP文件解压。"
            + "默认解压到ZIP文件所在目录下的同名文件夹中。"
            + "可以查看ZIP文件内容后再决定是否需要解压。",
            inputSchema,
            arguments -> {
                String zipPath = (String) arguments.get("zip_path");
                String outputDir = (String) arguments.get("output_dir");
                String password = (String) arguments.get("password");

                if (zipPath == null || zipPath.trim().isEmpty()) {
                    return errorResult("ZIP文件路径不能为空");
                }

                try {
                    Map<String, Object> result = ZipTools.unzipFile(zipPath, outputDir, password);

                    StringBuilder sb = new StringBuilder();
                    sb.append("✓ 解压成功\n");
                    sb.append("===================\n\n");
                    sb.append("ZIP文件: ").append(result.get("zipPath")).append("\n");
                    sb.append("输出目录: ").append(result.get("outputDir")).append("\n");
                    sb.append("文件数: ").append(result.get("fileCount")).append("\n");
                    sb.append("解压后大小: ").append(formatFileSize((Long) result.get("extractedSize"))).append("\n\n");

                    @SuppressWarnings("unchecked")
                    List<String> files = (List<String>) result.get("files");
                    sb.append("文件列表（前20个）:\n");
                    for (String file : files) {
                        sb.append("  - ").append(file).append("\n");
                    }
                    if ((Boolean) result.get("hasMoreFiles")) {
                        sb.append("  ... 还有更多文件\n");
                    }

                    return textResult(sb.toString());
                } catch (IOException e) {
                    return errorResult("解压失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 格式化文件大小显示
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }

    // ==================== BOM 控制工具 ====================

    /**
     * 注册 dlqq_add_unicode_bom - 为文件或目录添加 UTF-8 BOM
     */
    private void registerAddUnicodeBomTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> pathProp = new HashMap<>();
        pathProp.put("type", "string");
        pathProp.put("description", "文件或目录路径（支持绝对路径和相对路径）");
        properties.put("path", pathProp);

        Map<String, Object> isDirProp = new HashMap<>();
        isDirProp.put("type", "boolean");
        isDirProp.put("description", "是否为目录模式（默认为false，表示处理单个文件）");
        properties.put("is_directory", isDirProp);

        Map<String, Object> extensionsProp = new HashMap<>();
        extensionsProp.put("type", "array");
        extensionsProp.put("description", "文件扩展名列表（如 [\"java\", \"txt\"]），仅在目录模式下有效，为空则处理所有文件");
        properties.put("file_extensions", extensionsProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("path"));

        server.addTool("dlqq_add_unicode_bom",
            "为指定文件或目录添加 UTF-8 BOM（字节顺序标记）。如果文件已有 BOM 则跳过，如果文件不存在则创建带 BOM 的新文件。目录模式下会递归处理所有文件，遵循 AI 规则（.aiignore/.aiwriterules/.aireadrules）过滤文件。",
            inputSchema,
            arguments -> {
                String path = (String) arguments.get("path");
                Boolean isDirectory = (Boolean) arguments.get("is_directory");
                @SuppressWarnings("unchecked")
                List<String> extensions = (List<String>) arguments.get("file_extensions");

                if (path == null || path.trim().isEmpty()) {
                    return errorResult("路径不能为空");
                }

                try {
                    if (isDirectory != null && isDirectory) {
                        // 目录模式
                        String result = FileTools.addUnicodeBomInDir(path, extensions);
                        return textResult(result);
                    } else {
                        // 单文件模式
                        String result = FileTools.addUnicodeBom(path);
                        return textResult(result);
                    }
                } catch (Exception e) {
                    return errorResult("添加 BOM 失败: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 注册 dlqq_remove_unicode_bom - 移除文件或目录的 UTF-8 BOM
     */
    private void registerRemoveUnicodeBomTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> pathProp = new HashMap<>();
        pathProp.put("type", "string");
        pathProp.put("description", "文件或目录路径（支持绝对路径和相对路径）");
        properties.put("path", pathProp);

        Map<String, Object> isDirProp = new HashMap<>();
        isDirProp.put("type", "boolean");
        isDirProp.put("description", "是否为目录模式（默认为false，表示处理单个文件）");
        properties.put("is_directory", isDirProp);

        Map<String, Object> extensionsProp = new HashMap<>();
        extensionsProp.put("type", "array");
        extensionsProp.put("description", "文件扩展名列表（如 [\"java\", \"txt\"]），仅在目录模式下有效，为空则处理所有文件");
        properties.put("file_extensions", extensionsProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("path"));

        server.addTool("dlqq_remove_unicode_bom",
            "移除指定文件或目录的 UTF-8 BOM（字节顺序标记）。如果文件没有 BOM 则跳过。目录模式下会递归处理所有文件，遵循 AI 规则（.aiignore/.aiwriterules/.aireadrules）过滤文件。",
            inputSchema,
            arguments -> {
                String path = (String) arguments.get("path");
                Boolean isDirectory = (Boolean) arguments.get("is_directory");
                @SuppressWarnings("unchecked")
                List<String> extensions = (List<String>) arguments.get("file_extensions");

                if (path == null || path.trim().isEmpty()) {
                    return errorResult("路径不能为空");
                }

                try {
                    if (isDirectory != null && isDirectory) {
                        // 目录模式
                        String result = FileTools.removeUnicodeBomInDir(path, extensions);
                        return textResult(result);
                    } else {
                        // 单文件模式
                        String result = FileTools.removeUnicodeBom(path);
                        return textResult(result);
                    }
                } catch (Exception e) {
                    return errorResult("移除 BOM 失败: " + e.getMessage());
                }
            }
        );
    }

    // ==================== USB工具 ====================

    /**
     * dlqq_run_usb - 启动USB工具
     */
    private void registerRunUsbTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "string");
        portProp.put("description", "实例端口号，用于区分不同的USB工具实例");
        properties.put("port", portProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.singletonList("port"));

        server.addTool("dlqq_run_usb",
            "启动USB工具（打开图形界面）。端口号用于区分不同的USB工具实例。",
            inputSchema,
            arguments -> {
                String portStr = (String) arguments.get("port");
                int port;
                try {
                    port = Integer.parseInt(portStr);
                } catch (NumberFormatException e) {
                    return errorResult("无效的端口号");
                }

                if (UsbTools.hasInstance(port)) {
                    return errorResult("USB工具实例已在运行中，端口: " + port);
                }

                UsbTools instance = UsbTools.createInstance(port);
                String result = instance.start();
                return textResult(result);
            }
        );
    }

    /**
     * dlqq_usb_scan - 扫描USB设备
     */
    private void registerUsbScanTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "string");
        portProp.put("description", "USB工具实例端口号");
        properties.put("port", portProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.singletonList("port"));

        server.addTool("dlqq_usb_scan",
            "扫描可用的USB HID/CDC设备。返回设备列表，包含VID、PID、设备名称等信息。",
            inputSchema,
            arguments -> {
                String portStr = (String) arguments.get("port");
                int port;
                try {
                    port = Integer.parseInt(portStr);
                } catch (NumberFormatException e) {
                    return errorResult("无效的端口号");
                }

                UsbTools instance = UsbTools.getInstance(port);
                if (instance == null || !UsbTools.isInstanceRunning(port)) {
                    return errorResult("USB工具实例未启动，请先使用 dlqq_run_usb");
                }

                List<Map<String, Object>> devices = instance.scanDevices();

                StringBuilder sb = new StringBuilder();
                sb.append("【USB设备扫描结果】\n");
                sb.append("共找到 ").append(devices.size()).append(" 个设备\n\n");

                for (int i = 0; i < devices.size(); i++) {
                    Map<String, Object> dev = devices.get(i);
                    sb.append(i + 1).append(". ").append(dev.get("displayName")).append("\n");
                    sb.append("   VID: ").append(dev.get("vendorIdHex")).append("\n");
                    sb.append("   PID: ").append(dev.get("productIdHex")).append("\n");
                    sb.append("   类型: ").append(dev.get("deviceClass")).append("\n\n");
                }

                if (devices.isEmpty()) {
                    sb.append("未发现USB设备\n");
                }

                return textResult(sb.toString());
            }
        );
    }

    /**
     * dlqq_usb_get_devices - 获取已扫描的设备列表
     */
    private void registerUsbGetDevicesTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "string");
        portProp.put("description", "USB工具实例端口号");
        properties.put("port", portProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.singletonList("port"));

        server.addTool("dlqq_usb_get_devices",
            "获取上次扫描的USB设备列表（不需要重新扫描）。",
            inputSchema,
            arguments -> {
                String portStr = (String) arguments.get("port");
                int port;
                try {
                    port = Integer.parseInt(portStr);
                } catch (NumberFormatException e) {
                    return errorResult("无效的端口号");
                }

                UsbTools instance = UsbTools.getInstance(port);
                if (instance == null || !UsbTools.isInstanceRunning(port)) {
                    return errorResult("USB工具实例未启动");
                }

                // 执行扫描获取设备
                List<Map<String, Object>> devices = instance.scanDevices();

                StringBuilder sb = new StringBuilder();
                sb.append("【USB设备列表】\n");
                for (int i = 0; i < devices.size(); i++) {
                    Map<String, Object> dev = devices.get(i);
                    sb.append(i + 1).append(". ").append(dev.get("displayName")).append("\n");
                }
                return textResult(sb.toString());
            }
        );
    }

    /**
     * dlqq_usb_open - 打开USB设备
     */
    private void registerUsbOpenTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "string");
        portProp.put("description", "USB工具实例端口号");
        properties.put("port", portProp);

        Map<String, Object> vidProp = new HashMap<>();
        vidProp.put("type", "string");
        vidProp.put("description", "厂商ID (VID)，十六进制，如 \"0x0483\"");
        properties.put("vid", vidProp);

        Map<String, Object> pidProp = new HashMap<>();
        pidProp.put("type", "string");
        pidProp.put("description", "产品ID (PID)，十六进制，如 \"0x5740\"");
        properties.put("pid", pidProp);

        Map<String, Object> typeProp = new HashMap<>();
        typeProp.put("type", "string");
        typeProp.put("description", "设备类型: HID 或 CDC");
        typeProp.put("enum", Arrays.asList("HID", "CDC"));
        properties.put("type", typeProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port", "vid", "pid"));

        server.addTool("dlqq_usb_open",
            "打开指定的USB设备。需要先执行 dlqq_usb_scan 获取设备列表。支持HID和CDC设备。",
            inputSchema,
            arguments -> {
                String portStr = (String) arguments.get("port");
                String vidStr = (String) arguments.get("vid");
                String pidStr = (String) arguments.get("pid");
                String type = (String) arguments.get("type");

                int port, vid, pid;
                try {
                    port = Integer.parseInt(portStr);
                } catch (NumberFormatException e) {
                    return errorResult("无效的端口号");
                }

                try {
                    vid = Integer.parseInt(vidStr.replace("0x", ""), 16);
                    pid = Integer.parseInt(pidStr.replace("0x", ""), 16);
                } catch (NumberFormatException e) {
                    return errorResult("VID/PID 格式错误");
                }

                UsbTools instance = UsbTools.getInstance(port);
                if (instance == null || !UsbTools.isInstanceRunning(port)) {
                    return errorResult("USB工具实例未启动");
                }

                String result = instance.openDevice(vid, pid, type);
                return textResult(result);
            }
        );
    }

    /**
     * dlqq_usb_close - 关闭USB设备
     */
    private void registerUsbCloseTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "string");
        portProp.put("description", "USB工具实例端口号");
        properties.put("port", portProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.singletonList("port"));

        server.addTool("dlqq_usb_close",
            "关闭当前打开的USB设备。",
            inputSchema,
            arguments -> {
                String portStr = (String) arguments.get("port");
                int port;
                try {
                    port = Integer.parseInt(portStr);
                } catch (NumberFormatException e) {
                    return errorResult("无效的端口号");
                }

                UsbTools instance = UsbTools.getInstance(port);
                if (instance == null || !UsbTools.isInstanceRunning(port)) {
                    return errorResult("USB工具实例未启动");
                }

                String result = instance.closeDevice();
                return textResult(result);
            }
        );
    }

    /**
     * dlqq_usb_send_text - 发送文本数据
     */
    private void registerUsbSendTextTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "string");
        portProp.put("description", "USB工具实例端口号");
        properties.put("port", portProp);

        Map<String, Object> textProp = new HashMap<>();
        textProp.put("type", "string");
        textProp.put("description", "要发送的文本内容");
        properties.put("text", textProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port", "text"));

        server.addTool("dlqq_usb_send_text",
            "向USB设备发送文本数据（UTF-8编码）。",
            inputSchema,
            arguments -> {
                String portStr = (String) arguments.get("port");
                String text = (String) arguments.get("text");

                int port;
                try {
                    port = Integer.parseInt(portStr);
                } catch (NumberFormatException e) {
                    return errorResult("无效的端口号");
                }

                UsbTools instance = UsbTools.getInstance(port);
                if (instance == null || !UsbTools.isInstanceRunning(port)) {
                    return errorResult("USB工具实例未启动");
                }

                String result = instance.sendText(text);
                return textResult(result);
            }
        );
    }

    /**
     * dlqq_usb_send_hex - 发送十六进制数据
     */
    private void registerUsbSendHexTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "string");
        portProp.put("description", "USB工具实例端口号");
        properties.put("port", portProp);

        Map<String, Object> hexProp = new HashMap<>();
        hexProp.put("type", "array");
        hexProp.put("description", "十六进制数据数组，如 [\"FF\", \"01\", \"02\", \"03\"]");
        properties.put("hex_array", hexProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Arrays.asList("port", "hex_array"));

        server.addTool("dlqq_usb_send_hex",
            "向USB设备发送十六进制数据。每个元素为两位的十六进制字符串。",
            inputSchema,
            arguments -> {
                String portStr = (String) arguments.get("port");
                Object hexArray = arguments.get("hex_array");

                int port;
                try {
                    port = Integer.parseInt(portStr);
                } catch (NumberFormatException e) {
                    return errorResult("无效的端口号");
                }

                UsbTools instance = UsbTools.getInstance(port);
                if (instance == null || !UsbTools.isInstanceRunning(port)) {
                    return errorResult("USB工具实例未启动");
                }

                String[] hexData;
                if (hexArray instanceof List) {
                    List<?> list = (List<?>) hexArray;
                    hexData = list.stream().map(Object::toString).toArray(String[]::new);
                } else {
                    return errorResult("hex_array 必须是数组类型");
                }

                String result = instance.sendHex(hexData);
                return textResult(result);
            }
        );
    }

    /**
     * dlqq_usb_get_log - 获取USB日志
     */
    private void registerUsbGetLogTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "string");
        portProp.put("description", "USB工具实例端口号");
        properties.put("port", portProp);

        Map<String, Object> startLineProp = new HashMap<>();
        startLineProp.put("type", "string");
        startLineProp.put("description", "起始行号（1-based，默认1）");
        properties.put("start_line", startLineProp);

        Map<String, Object> endLineProp = new HashMap<>();
        endLineProp.put("type", "string");
        endLineProp.put("description", "结束行号（-1表示到末尾）");
        properties.put("end_line", endLineProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.singletonList("port"));

        server.addTool("dlqq_usb_get_log",
            "获取USB通信日志。支持文本/HEX格式，取决于当前显示设置。",
            inputSchema,
            arguments -> {
                String portStr = (String) arguments.get("port");
                String startLineStr = (String) arguments.get("start_line");
                String endLineStr = (String) arguments.get("end_line");

                int port;
                try {
                    port = Integer.parseInt(portStr);
                } catch (NumberFormatException e) {
                    return errorResult("无效的端口号");
                }

                UsbTools instance = UsbTools.getInstance(port);
                if (instance == null || !UsbTools.isInstanceRunning(port)) {
                    return errorResult("USB工具实例未启动");
                }

                int startLine = (startLineStr != null) ? Integer.parseInt(startLineStr) : 1;
                int endLine = (endLineStr != null) ? Integer.parseInt(endLineStr) : -1;

                List<String> logs = instance.getLog(startLine, endLine, 0, 0);
                StringBuilder sb = new StringBuilder();
                sb.append("【USB通信日志】\n");
                for (String line : logs) {
                    sb.append(line).append("\n");
                }
                return textResult(sb.toString());
            }
        );
    }

    /**
     * dlqq_usb_get_log_text - 获取USB日志（强制文本格式）
     */
    private void registerUsbGetLogTextTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "string");
        portProp.put("description", "USB工具实例端口号");
        properties.put("port", portProp);

        Map<String, Object> startLineProp = new HashMap<>();
        startLineProp.put("type", "string");
        startLineProp.put("description", "起始行号");
        properties.put("start_line", startLineProp);

        Map<String, Object> lineCountProp = new HashMap<>();
        lineCountProp.put("type", "string");
        lineCountProp.put("description", "读取行数");
        properties.put("line_count", lineCountProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.singletonList("port"));

        server.addTool("dlqq_usb_get_log_text",
            "获取USB通信日志（强制文本格式）。",
            inputSchema,
            arguments -> {
                String portStr = (String) arguments.get("port");
                String startLineStr = (String) arguments.get("start_line");
                String lineCountStr = (String) arguments.get("line_count");

                int port;
                try {
                    port = Integer.parseInt(portStr);
                } catch (NumberFormatException e) {
                    return errorResult("无效的端口号");
                }

                UsbTools instance = UsbTools.getInstance(port);
                if (instance == null || !UsbTools.isInstanceRunning(port)) {
                    return errorResult("USB工具实例未启动");
                }

                int startLine = (startLineStr != null) ? Integer.parseInt(startLineStr) : 1;
                int lineCount = (lineCountStr != null) ? Integer.parseInt(lineCountStr) : 100;

                List<String> logs = instance.getLogText(startLine, -1, lineCount);
                StringBuilder sb = new StringBuilder();
                sb.append("【USB通信日志（文本）】\n");
                for (String line : logs) {
                    sb.append(line).append("\n");
                }
                return textResult(sb.toString());
            }
        );
    }

    /**
     * dlqq_usb_get_log_hex - 获取USB日志（强制HEX格式）
     */
    private void registerUsbGetLogHexTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "string");
        portProp.put("description", "USB工具实例端口号");
        properties.put("port", portProp);

        Map<String, Object> startLineProp = new HashMap<>();
        startLineProp.put("type", "string");
        startLineProp.put("description", "起始行号");
        properties.put("start_line", startLineProp);

        Map<String, Object> lineCountProp = new HashMap<>();
        lineCountProp.put("type", "string");
        lineCountProp.put("description", "读取行数");
        properties.put("line_count", lineCountProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.singletonList("port"));

        server.addTool("dlqq_usb_get_log_hex",
            "获取USB通信日志（强制HEX格式）。",
            inputSchema,
            arguments -> {
                String portStr = (String) arguments.get("port");
                String startLineStr = (String) arguments.get("start_line");
                String lineCountStr = (String) arguments.get("line_count");

                int port;
                try {
                    port = Integer.parseInt(portStr);
                } catch (NumberFormatException e) {
                    return errorResult("无效的端口号");
                }

                UsbTools instance = UsbTools.getInstance(port);
                if (instance == null || !UsbTools.isInstanceRunning(port)) {
                    return errorResult("USB工具实例未启动");
                }

                int startLine = (startLineStr != null) ? Integer.parseInt(startLineStr) : 1;
                int lineCount = (lineCountStr != null) ? Integer.parseInt(lineCountStr) : 100;

                List<String> logs = instance.getLogHex(startLine, -1, lineCount);
                StringBuilder sb = new StringBuilder();
                sb.append("【USB通信日志（HEX）】\n");
                for (String line : logs) {
                    sb.append(line).append("\n");
                }
                return textResult(sb.toString());
            }
        );
    }

    /**
     * dlqq_usb_clear - 清除USB缓冲区
     */
    private void registerUsbClearTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "string");
        portProp.put("description", "USB工具实例端口号");
        properties.put("port", portProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.singletonList("port"));

        server.addTool("dlqq_usb_clear",
            "清除USB数据缓冲区。",
            inputSchema,
            arguments -> {
                String portStr = (String) arguments.get("port");
                int port;
                try {
                    port = Integer.parseInt(portStr);
                } catch (NumberFormatException e) {
                    return errorResult("无效的端口号");
                }

                UsbTools instance = UsbTools.getInstance(port);
                if (instance == null || !UsbTools.isInstanceRunning(port)) {
                    return errorResult("USB工具实例未启动");
                }

                String result = instance.clearBuffer();
                return textResult(result);
            }
        );
    }

    /**
     * dlqq_usb_get_line_count - 获取缓冲区行数
     */
    private void registerUsbGetLineCountTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "string");
        portProp.put("description", "USB工具实例端口号");
        properties.put("port", portProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.singletonList("port"));

        server.addTool("dlqq_usb_get_line_count",
            "获取USB缓冲区当前的日志行数。",
            inputSchema,
            arguments -> {
                String portStr = (String) arguments.get("port");
                int port;
                try {
                    port = Integer.parseInt(portStr);
                } catch (NumberFormatException e) {
                    return errorResult("无效的端口号");
                }

                UsbTools instance = UsbTools.getInstance(port);
                if (instance == null || !UsbTools.isInstanceRunning(port)) {
                    return errorResult("USB工具实例未启动");
                }

                int count = instance.getLineCount();
                return textResult("当前缓冲区行数: " + count);
            }
        );
    }

    /**
     * dlqq_usb_switch_text_hex - 切换显示格式
     */
    private void registerUsbSwitchTextHexTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "string");
        portProp.put("description", "USB工具实例端口号");
        properties.put("port", portProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.singletonList("port"));

        server.addTool("dlqq_usb_switch_text_hex",
            "切换USB日志的显示格式（文本/HEX）。",
            inputSchema,
            arguments -> {
                String portStr = (String) arguments.get("port");
                int port;
                try {
                    port = Integer.parseInt(portStr);
                } catch (NumberFormatException e) {
                    return errorResult("无效的端口号");
                }

                UsbTools instance = UsbTools.getInstance(port);
                if (instance == null || !UsbTools.isInstanceRunning(port)) {
                    return errorResult("USB工具实例未启动");
                }

                String result = instance.switchTextHex();
                return textResult(result);
            }
        );
    }

    /**
     * dlqq_exit_usb - 退出USB工具
     */
    private void registerExitUsbTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "string");
        portProp.put("description", "USB工具实例端口号");
        properties.put("port", portProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.singletonList("port"));

        server.addTool("dlqq_exit_usb",
            "退出USB工具（关闭窗口及进程）。",
            inputSchema,
            arguments -> {
                String portStr = (String) arguments.get("port");
                int port;
                try {
                    port = Integer.parseInt(portStr);
                } catch (NumberFormatException e) {
                    return errorResult("无效的端口号");
                }

                UsbTools instance = UsbTools.getInstance(port);
                if (instance == null) {
                    return errorResult("USB工具实例不存在");
                }

                String result = instance.stop();
                UsbTools.removeInstance(port);
                return textResult(result);
            }
        );
    }

    /**
     * dlqq_rerun_usb - 重新启动USB工具
     */
    private void registerRerunUsbTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> portProp = new HashMap<>();
        portProp.put("type", "string");
        portProp.put("description", "USB工具实例端口号");
        properties.put("port", portProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.singletonList("port"));

        server.addTool("dlqq_rerun_usb",
            "重新启动USB工具（先关闭再启动）。用于刷新窗口。",
            inputSchema,
            arguments -> {
                String portStr = (String) arguments.get("port");
                int port;
                try {
                    port = Integer.parseInt(portStr);
                } catch (NumberFormatException e) {
                    return errorResult("无效的端口号");
                }

                UsbTools instance = UsbTools.getInstance(port);
                if (instance == null) {
                    return errorResult("USB工具实例不存在");
                }

                String result = instance.restart();
                return textResult(result);
            }
        );
    }
}

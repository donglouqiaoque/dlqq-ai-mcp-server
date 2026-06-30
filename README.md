# 鸫楼桥鹊AI工具 (dlqq-ai-tools)

一个Java编写的AI工具集，支持MCP（Model Context Protocol）协议，专为Qwen Code集成使用。

## 功能特性

- **文件操作** - 读取、创建、删除、复制、移动文件，支持行级编辑
- **目录操作** - 创建、删除、复制、移动文件夹，支持多级目录
- **二进制文件** - 读取二进制文件为十六进制，将十六进制写入为二进制
- **搜索工具** - 在目录下搜索文件或文件夹，支持部分匹配和正则
- **对话框交互** - 文本输入、确认、文件选择等GUI对话框
- **串口通信** - CH340等USB转串口设备的图形化通信工具
- **BLE蓝牙** - 蓝牙低功耗设备的扫描、连接、数据读写工具
- **MCP服务器** - 完整支持MCP协议，可作为AI工具服务器使用

## 工具列表

本MCP服务器提供97个核心工具：

### 文件操作工具 (1-14)

| 工具名称 | 描述 |
|---------|------|
| `dlqq_read_file` | 读取文件指定行范围的内容（支持字符集） |
| `dlqq_create_file` | 创建新文件（支持空文件或带内容，支持字符集） |
| `dlqq_overwrite_file` | 覆盖文件内容（不管是否存在都直接写入，支持字符集） |
| `dlqq_clear_file` | 清空文件内容（保留文件本身） |
| `dlqq_append_file` | 在文件末尾追加内容（支持字符集） |
| `dlqq_remove_lines_in_file` | 删除文件指定行范围的内容（支持字符集） |
| `dlqq_replace_lines_in_file` | 替换文件指定行范围的内容（支持字符集） |
| `dlqq_insert_text_in_file` | 在文件指定行插入内容（支持字符集） |
| `dlqq_replace_text_in_file` | 替换文件中的文本（支持正则，支持字符集） |
| `dlqq_remove_text_in_file` | 删除文件中匹配的文本（支持正则，相当于替换为空） |
| `dlqq_delete_file` | 删除文件（带确认对话框） |
| `dlqq_copy_file` | 复制文件 |
| `dlqq_move_file` | 移动文件 |
| `dlqq_rename_file` | 重命名文件（同目录下修改文件名） |

### 目录操作工具 (15-22)

| 工具名称 | 描述 |
|---------|------|
| `dlqq_mdir` | 创建文件夹（父目录必须已存在） |
| `dlqq_mdirs` | 创建文件夹（自动创建所有父文件夹） |
| `dlqq_delete_dir` | 删除文件夹（带确认对话框，递归删除） |
| `dlqq_copy_dir` | 复制文件夹（递归复制） |
| `dlqq_move_dir` | 移动文件夹 |
| `dlqq_rename_dir` | 重命名文件夹（同目录下修改文件夹名） |
| `dlqq_list_dir_tree` | 列出文件夹完整目录结构（递归显示子目录和文件） |
| `dlqq_list_dir_files` | 列出文件夹下的文件和子文件夹（只列出一层） |

### 二进制文件操作工具 (23-24)

| 工具名称 | 描述 |
|---------|------|
| `dlqq_read_bin_file` | 读取二进制文件，返回十六进制文本 |
| `dlqq_write_bin_file` | 将十六进制文本写入为二进制文件 |

### 搜索工具 (25-26)

| 工具名称 | 描述 |
|---------|------|
| `dlqq_search_file` | 搜索文件，支持部分匹配和正则表达式 |
| `dlqq_search_dir` | 搜索文件夹，支持部分匹配和正则表达式 |

### 对话框工具 (27-29)

| 工具名称 | 描述 |
|---------|------|
| `dlqq_dialog_text` | 文本输入对话框（独立窗口） |
| `dlqq_dialog_check` | 确认对话框，用户选择是/否（独立窗口） |
| `dlqq_dialog_file_path` | 文件选择对话框（独立窗口） |

### 串口工具 (30-46)

| 工具名称 | 描述 |
|---------|------|
| `dlqq_run_uart` | 启动CH340串口工具（打开图形界面） |
| `dlqq_uart_get_com` | 获取可用串口通道列表 |
| `dlqq_uart_refresh_com` | 刷新可用串口列表 |
| `dlqq_uart_open_com` | 打开指定串口通道 |
| `dlqq_uart_reopen_com` | 重新打开串口（可更换波特率） |
| `dlqq_uart_close_com` | 关闭当前串口通道 |
| `dlqq_uart_send_text` | 发送文本数据到串口 |
| `dlqq_uart_send_hex` | 发送16进制数据到串口 |
| `dlqq_uart_get_log` | 获取串口日志数据（支持文本/HEX格式） |
| `dlqq_uart_get_log_text` | 获取串口日志数据（强制文本格式） |
| `dlqq_uart_get_log_hex` | 获取串口日志数据（强制HEX格式） |
| `dlqq_uart_clear_text` | 清除串口数据缓冲区 |
| `dlqq_uart_get_line_count` | 获取当前缓冲区行数 |
| `dlqq_uart_set_text_size` | 设置串口数据缓冲区大小 |
| `dlqq_uart_switch_text_hex` | 切换日志显示格式（文本/HEX） |
| `dlqq_exit_uart` | 退出串口工具（关闭窗口及进程） |
| `dlqq_rerun_uart` | 重新启动串口工具（先关闭再启动，用于刷新窗口） |

### BLE库管理工具 (47-50)

| 工具名称 | 描述 |
|---------|------|
| `dlqq_ble_lib_has` | 检查Python BLE库（bleak）是否已安装 |
| `dlqq_ble_lib_install` | 自动安装Python BLE依赖库（bleak） |
| `dlqq_ble_lib_update` | 检查并更新Python BLE库到最新版本 |
| `dlqq_ble_release_script` | 释放BLE Python脚本到项目目录 |

### BLE蓝牙工具 (51-69)

| 工具名称 | 描述 |
|---------|------|
| `dlqq_run_ble` | 启动BLE蓝牙工具（打开图形界面） |
| `dlqq_ble_scan` | 扫描BLE设备 |
| `dlqq_ble_get_devices` | 获取扫描到的设备列表 |
| `dlqq_ble_connect` | 连接BLE设备 |
| `dlqq_ble_disconnect` | 断开BLE设备连接 |
| `dlqq_ble_discover` | 发现BLE服务和特征 |
| `dlqq_ble_read` | 读取BLE特征值 |
| `dlqq_ble_write_text` | 写入BLE特征值（文本） |
| `dlqq_ble_write_hex` | 写入BLE特征值（HEX） |
| `dlqq_ble_subscribe` | 订阅/取消订阅BLE通知 |
| `dlqq_ble_get_log` | 获取BLE日志数据（支持文本/HEX格式） |
| `dlqq_ble_get_log_text` | 获取BLE日志数据（强制文本格式） |
| `dlqq_ble_get_log_hex` | 获取BLE日志数据（强制HEX格式） |
| `dlqq_ble_clear` | 清除BLE数据缓冲区 |
| `dlqq_ble_get_line_count` | 获取当前缓冲区行数 |
| `dlqq_ble_set_buffer_size` | 设置BLE数据缓冲区大小 |
| `dlqq_ble_switch_text_hex` | 切换日志显示格式（文本/HEX） |
| `dlqq_exit_ble` | 退出BLE工具（关闭窗口及进程） |
| `dlqq_rerun_ble` | 重新启动BLE工具（先关闭再启动，用于刷新窗口） |

### 系统信息工具 (70-71)

| 工具名称 | 描述 |
|---------|------|
| `dlqq_get_mcp_version` | 获取MCP服务器版本信息、JAR文件修改时间和构建信息 |
| `dlqq_check_bluetooth_open` | 检查Windows电脑的蓝牙适配器是否已开启 |

### 数据管理工具 (72-73)

| 工具名称 | 描述 |
|---------|------|
| `dlqq_clear_data` | 清除.dlqq数据目录（包括日志、Python脚本等），即使部分文件被占用也会返回成功 |
| `dlqq_clear_cache` | 清除日志缓存文件（仅删除.dlqq/log下的日志），即使部分文件被占用也会返回成功 |

### 项目脚本工具 (74-100)

脚本工具执行 `.dlqq/settings/settings.properties` 中配置的批处理脚本，用于自动化项目构建、芯片操作等流程。

**脚本配置说明**：首次使用前需通过 `dlqq_set_*_script` 工具设置脚本路径，或手动编辑 `.dlqq/settings/settings.properties` 文件。

#### 执行类工具

| 工具名称 | 描述 |
|---------|------|
| `dlqq_build_project` | 构建项目（执行 script.build 配置的脚本） |
| `dlqq_rebuild_project` | 重建项目（执行 script.rebuild 配置的脚本） |
| `dlqq_chip_erase` | 擦除芯片（执行 script.chip.erase 配置的脚本） |
| `dlqq_chip_download` | 下载固件到芯片（执行 script.chip.download 配置的脚本） |
| `dlqq_erase_download` | 擦除并下载（执行 script.erase.download 配置的脚本） |
| `dlqq_build_erase_download` | 构建、擦除并下载（执行 script.build.erase.download 配置的脚本） |
| `dlqq_rebuild_erase_download` | 重建、擦除并下载（执行 script.rebuild.erase.download 配置的脚本） |
| `dlqq_run_project` | 运行项目（执行 script.run 配置的脚本） |
| `dlqq_debug_project` | 调试项目（执行 script.debug 配置的脚本） |
| `dlqq_build_run_project` | 构建并运行（执行 script.build.run 配置的脚本） |
| `dlqq_build_debug_project` | 构建并调试（执行 script.build.debug 配置的脚本） |
| `dlqq_rebuild_run_project` | 重建并运行（执行 script.rebuild.run 配置的脚本） |
| `dlqq_rebuild_debug_project` | 重建并调试（执行 script.rebuild.debug 配置的脚本） |

#### 设置类工具

| 工具名称 | 描述 |
|---------|------|
| `dlqq_set_build_script` | 设置构建脚本路径（script.build） |
| `dlqq_set_rebuild_script` | 设置重建脚本路径（script.rebuild） |
| `dlqq_set_chip_erase_script` | 设置芯片擦除脚本路径（script.chip.erase） |
| `dlqq_set_chip_download_script` | 设置芯片下载脚本路径（script.chip.download） |
| `dlqq_set_erase_download_script` | 设置擦除下载脚本路径（script.erase.download） |
| `dlqq_set_build_erase_download_script` | 设置构建擦除下载脚本路径（script.build.erase.download） |
| `dlqq_set_rebuild_erase_download_script` | 设置重建擦除下载脚本路径（script.rebuild.erase.download） |
| `dlqq_set_run_script` | 设置运行脚本路径（script.run） |
| `dlqq_set_debug_script` | 设置调试脚本路径（script.debug） |
| `dlqq_set_build_run_script` | 设置构建运行脚本路径（script.build.run） |
| `dlqq_set_build_debug_script` | 设置构建调试脚本路径（script.build.debug） |
| `dlqq_set_rebuild_run_script` | 设置重建运行脚本路径（script.rebuild.run） |
| `dlqq_set_rebuild_debug_script` | 设置重建调试脚本路径（script.rebuild.debug） |
| `dlqq_set_response_max_size` | 设置最大回复文本大小（response.max.size，默认32KB） |

## 参数类型说明

**所有整数参数（integer）都同时支持 integer 和 string 两种格式**，例如：

```json
// 端口号 - 两种格式都可以
{ "port": 8001 }        // integer 格式
{ "port": "8001" }      // string 格式

// 行号 - 两种格式都可以
{ "start_line": 1 }     // integer 格式
{ "start_line": "1" }   // string 格式

// 缓冲区大小 - 两种格式都可以
{ "size_bytes": 5242880 }      // integer 格式
{ "size_bytes": "5242880" }    // string 格式
```

这样设计是为了兼容不同 AI 模型的参数传递习惯，无论 AI 传递整数还是字符串都能正常工作。

## 快速开始

### 运行环境要求

#### Java运行环境

- **需要 Java 17 或更高版本**
- 如果未安装Java，可在项目 `runtime/` 目录中找到Java 17安装包

#### Python运行环境（用于串口/BLE工具）

- **需要 Python 3.8 或更高版本**
- 如果未安装Python，可在项目 `runtime/` 目录中找到Python 3.13安装包（向下兼容3.8+）

### 1. 编译项目

使用 Maven 构建（需要安装 Maven）：

```bash
mvn clean package
```

构建完成后，可执行 JAR 文件将生成在 `target/aitools-1.0.0.jar`。

### 2. 配置MCP客户端

#### Qwen Code 配置

MCP服务器配置需要放在 **settings.json** 文件中（而非 mcp.json）。

**发行固件包含示例 settings.json**，请参考该文件进行配置。

配置示例：

```json
{
  "mcpServers": {
    "dlqq-ai-tools": {
      "command": "java",
      "args": [
        "-jar",
        "A:\\MyProject\\Java\\donglouqiaoque_ai_tools\\release\\V1.0.0\\dlqq_ai_tools.jar"
      ],
      "env": {
        "JAVA_TOOL_OPTIONS": "-Dfile.encoding=UTF-8"
      },
      "timeout": 60000
    }
  }
}
```

**注意**：请将 JAR 路径修改为你本地的实际路径。

## 工具详细说明

### 1. dlqq_read_file

读取文件指定行范围的内容。

**参数**：
- `file_path` (string, 必需): 文件路径
- `start_line` (integer, 可选): 起始行（1-based，默认1）
- `end_line` (integer, 可选): 结束行（1-based，包含，-1表示到文件末尾）
- `read_lines` (integer, 可选): 读取行数（与end_line互斥）

**示例**：
```json
{
  "file_path": "src/main.java",
  "start_line": 10,
  "end_line": 20
}
```

### 2. dlqq_create_file

创建新文件，可指定内容。

**参数**：
- `file_path` (string, 必需): 文件路径
- `content` (string, 可选): 文件内容（为空则创建空文件）
- `charset` (string, 可选): 字符集编码（如 UTF-8、GBK、ISO-8859-1 等，默认为 UTF-8）

**示例**：
```json
{
  "file_path": "config.txt",
  "content": "key=value\nname=test",
  "charset": "UTF-8"
}
```

### 3. dlqq_overwrite_file

覆盖文件内容，不管文件是否存在都会直接写入。

**参数**：
- `file_path` (string, 必需): 文件路径
- `content` (string, 可选): 文件内容（会完全覆盖原文件内容）
- `charset` (string, 可选): 字符集编码（默认 UTF-8）

**示例**：
```json
{
  "file_path": "config.txt",
  "content": "new content",
  "charset": "UTF-8"
}
```

### 4. dlqq_append_file

在文件末尾追加内容。如果文件不存在会自动创建。

**参数**：
- `file_path` (string, 必需): 文件路径
- `content` (string, 必需): 要追加的内容
- `charset` (string, 可选): 字符集编码（默认 UTF-8）

**示例**：
```json
{
  "file_path": "log.txt",
  "content": "new log line\n",
  "charset": "UTF-8"
}
```

### 5. dlqq_remove_lines_in_file

删除文件中指定行范围的内容。

**参数**：
- `file_path` (string, 必需): 文件路径
- `start_line` (integer, 必需): 起始行（包含）
- `end_line` (integer, 必需): 结束行（包含）
- `charset` (string, 可选): 字符集编码（默认 UTF-8）

**示例**：
```json
{
  "file_path": "data.txt",
  "start_line": 5,
  "end_line": 10,
  "charset": "UTF-8"
}
```

### 6. dlqq_replace_lines_in_file

替换文件中指定行范围的内容。

**参数**：
- `file_path` (string, 必需): 文件路径
- `start_line` (integer, 必需): 起始行（包含）
- `end_line` (integer, 必需): 结束行（包含）
- `content` (string, 必需): 替换的新内容（支持多行）
- `charset` (string, 可选): 字符集编码（默认 UTF-8）

**示例**：
```json
{
  "file_path": "data.txt",
  "start_line": 5,
  "end_line": 10,
  "content": "new line 1\nnew line 2",
  "charset": "UTF-8"
}
```

### 7. dlqq_insert_text_in_file

在文件指定行之前插入内容。

**参数**：
- `file_path` (string, 必需): 文件路径
- `line` (integer, 必需): 插入位置（在该行之前插入）
- `content` (string, 必需): 要插入的内容（支持多行）
- `charset` (string, 可选): 字符集编码（默认 UTF-8）

**示例**：
```json
{
  "file_path": "data.txt",
  "line": 10,
  "content": "// 新插入的注释\nnewFunction();",
  "charset": "UTF-8"
}
```

### 8. dlqq_replace_text_in_file

替换文件中的文本，支持正则表达式。

**参数**：
- `file_path` (string, 必需): 文件路径
- `search_text` (string, 必需): 要搜索的文本或正则
- `replace_text` (string, 必需): 替换后的文本
- `mode` (string, 可选): 模式，"text"（普通文本）或 "regex"（正则表达式，默认"text"）
- `charset` (string, 可选): 字符集编码（默认 UTF-8）

**示例**：
```json
{
  "file_path": "config.txt",
  "search_text": "old_value",
  "replace_text": "new_value",
  "mode": "text",
  "charset": "UTF-8"
}
```

### 9. dlqq_delete_file

删除文件，会弹出确认对话框。

**参数**：
- `file_path` (string, 必需): 要删除的文件路径

**示例**：
```json
{
  "file_path": "temp.txt"
}
```

### 10. dlqq_copy_file

复制文件。

**参数**：
- `source_path` (string, 必需): 源文件路径
- `target_path` (string, 必需): 目标文件路径

**示例**：
```json
{
  "source_path": "original.txt",
  "target_path": "backup/original.txt"
}
```

### 11. dlqq_move_file

移动文件。

**参数**：
- `source_path` (string, 必需): 源文件路径
- `target_path` (string, 必需): 目标文件路径

**示例**：
```json
{
  "source_path": "old/path/file.txt",
  "target_path": "new/path/file.txt"
}
```

### 12. dlqq_rename_file

重命名文件（在同一目录下修改文件名）。

**参数**：
- `source_path` (string, 必需): 源文件路径
- `new_name` (string, 必需): 新文件名（仅文件名，不包含路径）

**示例**：
```json
{
  "source_path": "old_name.txt",
  "new_name": "new_name.txt"
}
```

### 13. dlqq_mdir

创建文件夹（只创建最后一级，父目录必须已存在）。

**参数**：
- `dir_path` (string, 必需): 文件夹路径

**示例**：
```json
{
  "dir_path": "new_folder"
}
```

### 14. dlqq_mdirs

创建文件夹（包括所有父文件夹一起创建）。

**参数**：
- `dir_path` (string, 必需): 文件夹路径

**示例**：
```json
{
  "dir_path": "parent/child/grandchild"
}
```

### 15. dlqq_delete_dir

删除文件夹（带确认对话框，递归删除所有内容）。

**参数**：
- `dir_path` (string, 必需): 要删除的文件夹路径

**示例**：
```json
{
  "dir_path": "old_folder"
}
```

**说明**：删除前会弹出确认对话框，显示目录包含的文件和子目录数量。

### 16. dlqq_copy_dir

复制文件夹（递归复制所有子目录和文件）。

**参数**：
- `source_path` (string, 必需): 源文件夹路径
- `target_path` (string, 必需): 目标文件夹路径

**示例**：
```json
{
  "source_path": "original_folder",
  "target_path": "backup/original_folder"
}
```

### 17. dlqq_move_dir

移动文件夹。

**参数**：
- `source_path` (string, 必需): 源文件夹路径
- `target_path` (string, 必需): 目标文件夹路径

**示例**：
```json
{
  "source_path": "old/path/folder",
  "target_path": "new/path/folder"
}
```

### 18. dlqq_rename_dir

重命名文件夹（在同一目录下修改文件夹名）。

**参数**：
- `source_path` (string, 必需): 源文件夹路径
- `new_name` (string, 必需): 新文件夹名（仅名称，不包含路径）

**示例**：
```json
{
  "source_path": "old_folder_name",
  "new_name": "new_folder_name"
}
```

### 19. dlqq_read_bin_file

读取二进制文件并将字节转换为十六进制文本返回。支持指定字节范围。

**参数**：
- `file_path` (string, 必需): 二进制文件路径
- `start_byte` (integer, 可选): 起始字节（0-based，默认为0）
- `end_byte` (integer, 可选): 结束字节（0-based，包含，-1表示到文件末尾）
- `read_bytes` (integer, 可选): 读取字节数（与end_byte互斥，end_byte优先）

**示例**：
```json
// 读取整个文件
{
  "file_path": "image.png"
}

// 读取指定字节范围（第0-99字节）
{
  "file_path": "image.png",
  "start_byte": 0,
  "end_byte": 99
}

// 从第100字节开始读取50字节
{
  "file_path": "image.png",
  "start_byte": 100,
  "read_bytes": 50
}
```

**返回**：十六进制文本（如 "89504E470D0A1A0A..."）

### 20. dlqq_write_bin_file

将十六进制文本转换为二进制并写入文件。支持覆盖或追加写入。

**参数**：
- `file_path` (string, 必需): 目标文件路径
- `hex_content` (string, 必需): 十六进制文本（可包含空格、换行）
- `append` (boolean, 可选): 是否追加写入（默认false，即覆盖写入）

**示例**：
```json
// 覆盖写入
{
  "file_path": "output.bin",
  "hex_content": "89504E47 0D0A1A0A 0000000D"
}

// 追加写入
{
  "file_path": "output.bin",
  "hex_content": "11223344 55667788",
  "append": true
}
```

### 21. dlqq_search_file

在指定目录下查找文件，支持部分匹配和正则表达式。

**参数**：
- `search_path` (string, 必需): 搜索目录路径
- `pattern` (string, 必需): 搜索模式
- `mode` (string, 可选): 模式，"text"（部分匹配，默认）或 "regex"（正则表达式）

**示例**：
```json
// 部分匹配（查找文件名包含 "test" 的文件）
{
  "search_path": "src",
  "pattern": "test",
  "mode": "text"
}

// 正则匹配（查找以 .java 结尾的文件）
{
  "search_path": "src",
  "pattern": "\\.java$",
  "mode": "regex"
}
```

**返回**：匹配的文件路径列表

### 22. dlqq_search_dir

在指定目录下查找文件夹，支持部分匹配和正则表达式。

**参数**：
- `search_path` (string, 必需): 搜索目录路径
- `pattern` (string, 必需): 搜索模式
- `mode` (string, 可选): 模式，"text"（部分匹配，默认）或 "regex"（正则表达式）

**示例**：
```json
// 部分匹配（查找名称包含 "config" 的文件夹）
{
  "search_path": ".",
  "pattern": "config",
  "mode": "text"
}

// 正则匹配（查找以 "test" 开头的文件夹）
{
  "search_path": ".",
  "pattern": "^test",
  "mode": "regex"
}
```

**返回**：匹配的文件夹路径列表

### 23. dlqq_dialog_text

向用户展示文本输入对话框，等待用户输入。

**参数**：
- `title` (string, 必需): 对话框标题
- `message` (string, 必需): 提示消息

**示例**：
```json
{
  "title": "请输入名称",
  "message": "请输入新文件的名称："
}
```

**返回**：用户输入的文本内容

### 24. dlqq_dialog_check

向用户展示确认对话框，用户选择是/否。

**参数**：
- `title` (string, 必需): 对话框标题
- `message` (string, 必需): 确认消息内容

**示例**：
```json
{
  "title": "确认操作",
  "message": "确定要删除这个文件吗？"
}
```

**返回**："是" 或 "否"

### 25. dlqq_dialog_file_path

向用户展示文件选择对话框。

**参数**：
- `title` (string, 必需): 对话框标题
- `extensions` (array, 可选): 允许的文件扩展名列表，如 `["txt", "java"]`

**示例**：
```json
{
  "title": "选择配置文件",
  "extensions": ["txt", "json", "xml"]
}
```

**返回**：选中的文件路径

### 26. dlqq_run_uart

启动CH340串口工具，打开图形界面。

**参数**：
- `port` (string, 必需): 服务器端口号（1~65535，例如："8001"、"8002"等）

**示例**：
```json
{
  "port": 8080
}
```

**说明**：启动后窗口保持运行，AI返回对话后用户可与UI交互。后续使用 `dlqq_uart_open_com` 等工具时需要指定相同的端口号。

### 27. dlqq_uart_get_com

获取系统中所有可用的串口通道列表。

**参数**：
- `port` (string, 必需): 串口工具的端口号（例如："8001"、"8002"等）

**示例**：
```json
{
  "port": 8080
}
```

**返回**：可用串口列表，自动标记CH340设备

### 28. dlqq_uart_refresh_com

刷新可用串口列表。获取系统中当前可用的所有串口设备。

**参数**：
- `port` (string, 必需): 串口工具的端口号（例如："8001"、"8002"等）

**示例**：
```json
{
  "port": 8080
}
```

### 29. dlqq_uart_open_com

打开指定串口通道，开始捕获数据。

**参数**：
- `port` (string, 必需): 串口工具的端口号（例如："8001"、"8002"等）
- `com` (string, 必需): 串口名称（如 COM3、ttyUSB0、USB Serial等）
- `baud_rate` (integer, 可选): 波特率（默认1000000，范围0~2000000）

**示例**：
```json
{
  "port": 8080,
  "com": "COM3",
  "baud_rate": 115200
}
```

### 30. dlqq_uart_close_com

关闭当前打开的串口通道。

**参数**：
- `port` (string, 必需): 串口工具的端口号（例如："8001"、"8002"等）

**示例**：
```json
{
  "port": 8080
}
```

### 31. dlqq_uart_reopen_com

重新打开串口，用于更改波特率。

**参数**：
- `port` (string, 必需): 串口工具的端口号（例如："8001"、"8002"等）
- `com` (string, 可选): 串口名称（不指定则使用当前串口）
- `baud_rate` (integer, 可选): 新的波特率（默认1000000）

**示例**：
```json
{
  "port": 8080,
  "baud_rate": 9600
}
```

### 32. dlqq_uart_send_text

发送文本数据到串口。

**参数**：
- `port` (string, 必需): 串口工具的端口号（例如："8001"、"8002"等）
- `text` (string, 必需): 要发送的文本内容

**示例**：
```json
{
  "port": 8080,
  "text": "Hello World"
}
```

### 33. dlqq_uart_send_hex

发送16进制数据到串口。

**参数**：
- `port` (string, 必需): 串口工具的端口号（例如："8001"、"8002"等）
- `hex_array` (array, 必需): 16进制字符串数组，如 `["FF", "01", "02"]`

**示例**：
```json
{
  "port": 8080,
  "hex_array": ["FF", "01", "02", "03"]
}
```

### 34. dlqq_uart_get_log

获取串口缓冲区日志数据，支持3种读取模式。返回格式取决于当前显示格式设置（文本/HEX）。

**参数**：
- `port` (string, 必需): 串口工具的端口号（例如："8001"、"8002"等）
- `start_line` (integer, 可选): 起始行（1-based，模式1/2使用，模式3默认为-1表示从最后一行读取新数据）
- `end_line` (integer, 可选): 结束行（模式1使用）
- `line_count` (integer, 可选): 读取行数（模式2和3使用）
- `timeout_ms` (integer, 可选): 等待超时毫秒（模式3使用，默认0表示不等待）

**模式说明**：
- 模式1（直接范围）：`start_line` + `end_line`
- 模式2（直接计数）：`start_line` + `line_count`
- 模式3（等待读取）：`timeout_ms` + `line_count`（`start_line`默认为-1表示从最后一行开始读取新数据）

**示例**：
```json
// 模式1：读取第1到第10行
{
  "port": 8080,
  "start_line": 1,
  "end_line": 10
}

// 模式2：从第5行开始读取20行
{
  "port": 8080,
  "start_line": 5,
  "line_count": 20
}

// 模式3：等待最多5000毫秒，读取至少10行新数据
{
  "port": 8080,
  "timeout_ms": 5000,
  "line_count": 10
}
```

**说明**：如需固定格式，请使用 `dlqq_uart_get_log_text`（强制文本）或 `dlqq_uart_get_log_hex`（强制HEX）。

### 35. dlqq_uart_get_log_text

获取串口缓冲区日志数据（强制文本格式）。支持3种读取模式。

**参数**：
- `port` (string, 必需): 串口工具的端口号（例如："8001"、"8002"等）
- `start_line` (integer, 可选): 起始行（1-based，模式1/2使用，模式3默认为-1表示从最后一行读取新数据）
- `end_line` (integer, 可选): 结束行（模式1使用）
- `line_count` (integer, 可选): 读取行数（模式2和3使用）
- `timeout_ms` (integer, 可选): 等待超时毫秒（模式3使用，默认0表示不等待）

**模式说明**：
- 模式1（直接范围）：`start_line` + `end_line`
- 模式2（直接计数）：`start_line` + `line_count`
- 模式3（等待读取）：`timeout_ms` + `line_count`（`start_line`默认为-1表示从最后一行开始读取新数据）

**示例**：
```json
// 模式1：读取第1到第10行
{
  "port": 8080,
  "start_line": 1,
  "end_line": 10
}

// 模式2：从第5行开始读取20行
{
  "port": 8080,
  "start_line": 5,
  "line_count": 20
}

// 模式3：等待最多5000毫秒，读取至少10行新数据
{
  "port": 8080,
  "timeout_ms": 5000,
  "line_count": 10
}
```

**说明**：无论当前显示格式设置如何，始终返回文本格式。

### 36. dlqq_uart_get_log_hex

获取串口缓冲区日志数据（强制HEX格式）。支持3种读取模式。

**参数**：
- `port` (string, 必需): 串口工具的端口号（例如："8001"、"8002"等）
- `start_line` (integer, 可选): 起始行（1-based，模式1/2使用，模式3默认为-1表示从最后一行读取新数据）
- `end_line` (integer, 可选): 结束行（模式1使用）
- `line_count` (integer, 可选): 读取行数（模式2和3使用）
- `timeout_ms` (integer, 可选): 等待超时毫秒（模式3使用，默认0表示不等待）

**模式说明**：
- 模式1（直接范围）：`start_line` + `end_line`
- 模式2（直接计数）：`start_line` + `line_count`
- 模式3（等待读取）：`timeout_ms` + `line_count`（`start_line`默认为-1表示从最后一行开始读取新数据）

**示例**：
```json
// 模式1：读取第1到第10行
{
  "port": 8080,
  "start_line": 1,
  "end_line": 10
}

// 模式2：从第5行开始读取20行
{
  "port": 8080,
  "start_line": 5,
  "line_count": 20
}

// 模式3：等待最多5000毫秒，读取至少10行新数据
{
  "port": 8080,
  "timeout_ms": 5000,
  "line_count": 10
}
```

**说明**：无论当前显示格式设置如何，始终返回HEX格式。

### 37. dlqq_uart_clear_text

清除串口数据缓冲区。

**参数**：
- `port` (string, 必需): 串口工具的端口号（例如："8001"、"8002"等）

**示例**：
```json
{
  "port": 8080
}
```

### 38. dlqq_uart_get_line_count

获取当前缓冲区行数。

**参数**：
- `port` (string, 必需): 串口工具的端口号（例如："8001"、"8002"等）

**示例**：
```json
{
  "port": 8080
}
```

**返回**：当前缓冲区中的日志行数

### 39. dlqq_uart_set_text_size

设置串口数据缓冲区大小。

**参数**：
- `port` (string, 必需): 串口工具的端口号（例如："8001"、"8002"等）
- `size_bytes` (integer, 必需): 缓冲区大小（字节）

**示例**：
```json
{
  "port": 8080,
  "size_bytes": 10485760
}
```

**说明**：默认5MB，超出后旧数据会被自动丢弃。

### 40. dlqq_uart_switch_text_hex

切换日志显示格式（文本/HEX）。

**参数**：
- `port` (string, 必需): 串口工具的端口号（例如："8001"、"8002"等）
- `format` (string, 必需): 显示格式，`text` 表示文本，`hex` 表示HEX

**示例**：
```json
// 切换为HEX显示
{
  "port": 8080,
  "format": "hex"
}

// 切换为文本显示
{
  "port": 8080,
  "format": "text"
}
```

**说明**：切换后，UI日志显示和 `dlqq_uart_get_log` 返回的数据格式都会相应改变。

### 41. dlqq_exit_uart

完全退出串口工具，关闭窗口及进程，释放所有资源。

**参数**：
- `port` (string, 必需): 串口工具的端口号（例如："8001"、"8002"等）

**示例**：
```json
{
  "port": 8080
}
```

### 42. dlqq_run_ble

启动BLE蓝牙工具，打开图形界面。

**参数**：
- `port` (string, 必需): 服务器端口号（1~65535，例如："8001"、"8002"等）

**示例**：
```json
{
  "port": 8090
}
```

**说明**：启动后窗口保持运行，AI返回对话后用户可与UI交互。后续使用 `dlqq_ble_scan` 等工具时需要指定相同的端口号。

### 43. dlqq_ble_scan

扫描周围的BLE蓝牙设备。

**参数**：
- `port` (string, 必需): BLE工具的端口号（例如："8001"、"8002"等）
- `timeout_ms` (integer, 可选): 扫描超时时间（毫秒，默认10000）

**示例**：
```json
{
  "port": 8090,
  "timeout_ms": 10000
}
```

**说明**：扫描完成后使用 `dlqq_ble_get_devices` 查看结果。

### 44. dlqq_ble_get_devices

获取扫描到的BLE设备列表。

**参数**：
- `port` (string, 必需): BLE工具的端口号（例如："8001"、"8002"等）

**示例**：
```json
{
  "port": 8090
}
```

### 45. dlqq_ble_connect

连接指定的BLE设备。

**参数**：
- `port` (string, 必需): BLE工具的端口号（例如："8001"、"8002"等）
- `device_address` (string, 必需): BLE设备地址（如 AA:BB:CC:DD:EE:01）

**示例**：
```json
{
  "port": 8090,
  "device_address": "AA:BB:CC:DD:EE:01"
}
```

### 46. dlqq_ble_disconnect

断开当前连接的BLE设备。

**参数**：
- `port` (string, 必需): BLE工具的端口号（例如："8001"、"8002"等）

**示例**：
```json
{
  "port": 8090
}
```

### 47. dlqq_ble_discover

发现已连接BLE设备的服务和特征。

**参数**：
- `port` (string, 必需): BLE工具的端口号（例如："8001"、"8002"等）

**示例**：
```json
{
  "port": 8090
}
```

**说明**：必须先连接设备。返回服务和特征列表，包含UUID和属性信息。

### 48. dlqq_ble_read

读取BLE设备的特征值。

**参数**：
- `port` (string, 必需): BLE工具的端口号（例如："8001"、"8002"等）
- `service_uuid` (string, 必需): 服务UUID
- `char_uuid` (string, 必需): 特征UUID

**示例**：
```json
{
  "port": 8090,
  "service_uuid": "0000180f-0000-1000-8000-00805f9b34fb",
  "char_uuid": "00002a19-0000-1000-8000-00805f9b34fb"
}
```

### 49. dlqq_ble_write_text

向BLE设备写入文本数据。

**参数**：
- `port` (string, 必需): BLE工具的端口号（例如："8001"、"8002"等）
- `service_uuid` (string, 必需): 服务UUID
- `char_uuid` (string, 必需): 特征UUID
- `text` (string, 必需): 要写入的文本内容

**示例**：
```json
{
  "port": 8090,
  "service_uuid": "0000fff0-0000-1000-8000-00805f9b34fb",
  "char_uuid": "0000fff2-0000-1000-8000-00805f9b34fb",
  "text": "Hello BLE"
}
```

### 50. dlqq_ble_write_hex

向BLE设备写入16进制数据。

**参数**：
- `port` (string, 必需): BLE工具的端口号（例如："8001"、"8002"等）
- `service_uuid` (string, 必需): 服务UUID
- `char_uuid` (string, 必需): 特征UUID
- `hex_array` (array, 必需): 16进制字符串数组，如 `["FF", "01", "02"]`

**示例**：
```json
{
  "port": 8090,
  "service_uuid": "0000fff0-0000-1000-8000-00805f9b34fb",
  "char_uuid": "0000fff2-0000-1000-8000-00805f9b34fb",
  "hex_array": ["FF", "01", "02", "03"]
}
```

### 51. dlqq_ble_subscribe

订阅或取消订阅BLE设备的特征通知。

**参数**：
- `port` (string, 必需): BLE工具的端口号（例如："8001"、"8002"等）
- `service_uuid` (string, 必需): 服务UUID
- `char_uuid` (string, 必需): 特征UUID
- `enable` (boolean, 可选): 是否启用通知（默认true）

**示例**：
```json
{
  "port": 8090,
  "service_uuid": "0000180f-0000-1000-8000-00805f9b34fb",
  "char_uuid": "00002a19-0000-1000-8000-00805f9b34fb",
  "enable": true
}
```

### 52. dlqq_ble_get_log

获取BLE缓冲区日志数据，支持3种读取模式。返回格式取决于当前显示格式设置（文本/HEX）。

**参数**：
- `port` (string, 必需): BLE工具的端口号（例如："8001"、"8002"等）
- `start_line` (integer, 可选): 起始行（1-based）
- `end_line` (integer, 可选): 结束行
- `line_count` (integer, 可选): 读取行数
- `timeout_ms` (integer, 可选): 等待超时毫秒（默认0）

**示例**：
```json
{
  "port": 8090,
  "start_line": 1,
  "line_count": 100
}
```

### 53. dlqq_ble_get_log_text

获取BLE日志数据（强制文本格式）。支持3种模式（同 dlqq_ble_get_log）。

**示例**：
```json
{
  "port": 8090,
  "start_line": 1,
  "line_count": 100
}
```

### 54. dlqq_ble_get_log_hex

获取BLE日志数据（强制HEX格式）。支持3种模式（同 dlqq_ble_get_log）。

**示例**：
```json
{
  "port": 8090,
  "start_line": 1,
  "line_count": 100
}
```

### 55. dlqq_ble_clear

清除BLE数据缓冲区。

**参数**：
- `port` (string, 必需): BLE工具的端口号（例如："8001"、"8002"等）

**示例**：
```json
{
  "port": 8090
}
```

### 56. dlqq_ble_set_buffer_size

设置BLE数据缓冲区大小。

**参数**：
- `port` (string, 必需): BLE工具的端口号（例如："8001"、"8002"等）
- `size_bytes` (integer, 必需): 缓冲区大小（字节）

**示例**：
```json
{
  "port": 8090,
  "size_bytes": 10485760
}
```

### 57. dlqq_ble_switch_text_hex

切换BLE日志显示格式（文本/HEX）。

**参数**：
- `port` (string, 必需): BLE工具的端口号（例如："8001"、"8002"等）
- `format` (string, 必需): 显示格式，`text` 或 `hex`

**示例**：
```json
{
  "port": 8090,
  "format": "hex"
}
```

### 58. dlqq_exit_ble

完全退出BLE工具，关闭窗口及进程，释放所有资源。

**参数**：
- `port` (string, 必需): BLE工具的端口号（例如："8001"、"8002"等）

**示例**：
```json
{
  "port": 8090
}
```

### 59. dlqq_clear_data

清除.dlqq数据目录，删除整个.dlqq目录及其所有内容（包括日志、Python脚本等）。即使部分文件被占用也会返回成功结果，并告知哪些文件被占用。

**参数**：无

**示例**：
```json
{}
```

**说明**：此操作会删除 .dlqq 目录下的所有内容，包括：
- 日志文件（.dlqq/log/logger/ 和 .dlqq/log/stderr/）
- Python脚本（.dlqq/python/）
- 其他数据文件

**注意**：部分文件（尤其是当前正在写入的日志文件）可能因被MCP服务器占用而无法删除，这是正常现象。工具会返回成功结果，并列出已删除的文件和被占用的文件。被占用的文件将在MCP服务器重启后可以被清除。

### 60. dlqq_clear_cache

清除.dlqq/log目录下的日志缓存文件，保留目录结构。即使部分文件被占用也会返回成功结果，并告知哪些文件被占用。

**参数**：无

**示例**：
```json
{}
```

**说明**：此操作仅删除日志文件，保留 .dlqq/log/ 目录结构。

**注意**：部分日志文件可能因被MCP服务器占用而无法删除，这是正常现象。工具会返回成功结果，并列出已删除的文件和被占用的文件。被占用的文件将在MCP服务器重启后可以被清除。

## 使用示例

在支持MCP的AI客户端中，可以直接请求AI使用这些工具：

### 文件操作示例

- "请读取 src/main.java 文件第10到20行的内容"
- "帮我在 config.txt 第5行插入一行注释"
- "把 data.txt 中的 'old' 替换为 'new'"
- "删除 temp.txt 文件"

### 目录操作示例

- "创建一个名为 new_folder 的文件夹"
- "创建多级目录 parent/child/grandchild"
- "删除 old_folder 文件夹"
- "复制 src_folder 到 backup/src_folder"
- "移动 old/path/folder 到 new/path/folder"

### 对话框示例

- "弹出一个对话框问用户是否要保存文件"
- "让用户选择一个配置文件"
- "询问用户项目名称"

### 二进制文件操作示例

- "读取 image.png 文件的十六进制内容"
- "将十六进制文本写入 output.bin"

### 搜索工具示例

- "在当前目录下搜索所有包含 'test' 的文件"
- "在 src 目录下搜索所有以 .java 结尾的文件（使用正则）"
- "查找所有名称包含 'config' 的文件夹"

### 串口操作示例

- "启动串口工具，端口8080"
- "获取可用串口列表"
- "打开COM3串口，波特率115200"
- "发送文本 'Hello' 到串口"
- "发送16进制数据 ['FF', '01', '02'] 到串口"
- "获取串口最近100行日志"
- "切换日志显示格式为HEX"
- "设置缓冲区大小为10MB"
- "关闭串口"
- "退出串口工具"

### BLE蓝牙操作示例

- "启动BLE工具，端口8090"
- "扫描BLE设备"
- "获取扫描到的设备列表"
- "连接设备 AA:BB:CC:DD:EE:01"
- "发现BLE服务和特征"
- "读取电池服务特征值"
- "写入文本 'Hello BLE' 到特征"
- "写入HEX数据 ['FF', '01', '02'] 到特征"
- "订阅通知"
- "获取BLE日志"
- "切换日志显示格式为HEX"
- "断开BLE连接"
- "退出BLE工具"

## 技术栈

- **Java 17+**（必需，runtime目录提供安装包）
- **Python 3.8+**（串口/BLE工具必需，runtime目录提供3.13安装包）
- MCP协议（Model Context Protocol）
- Maven构建工具
- 依赖：
  - jSerialComm（UART串口通信）
  - Python bleak库（BLE蓝牙通信）

## 项目结构

```
src/main/java/com/donglouqiaoque/aitools/
├── mcp/
│   ├── MCPServerMain.java         # MCP服务器入口
│   ├── protocol/
│   │   ├── JsonRpcMessage.java    # JSON-RPC消息
│   │   └── McpSchema.java         # MCP协议定义
│   ├── server/
│   │   └── McpServer.java         # MCP服务器核心
│   ├── tools/
│   │   ├── AITools.java           # 工具注册
│   │   ├── FileTools.java         # 文件操作工具
│   │   ├── DialogTools.java       # GUI对话框工具
│   │   ├── UartTools.java         # 串口工具
│   │   └── BleTools.java          # BLE蓝牙工具
│   └── transport/
│       ├── McpTransport.java      # 传输层接口
│       └── StdioTransport.java    # stdio传输实现
├── ai/
│   ├── ChatMessage.java           # 聊天消息模型
│   └── AIService.java             # AI服务
├── cli/
│   └── commands/                  # CLI命令（可选）
├── config/
│   └── ConfigManager.java         # 配置管理器
├── http/
│   └── HttpClient.java            # HTTP客户端
└── utils/
    └── FileUtils.java             # 文件工具类
```

## 许可证

本项目采用 Apache License 2.0 许可证

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

本MCP服务器提供102个核心工具：

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

### 搜索工具 (25-28)

| 工具名称 | 描述 |
|---------|------|
| `dlqq_search_file` | 搜索文件，支持部分匹配和正则表达式 |
| `dlqq_search_dir` | 搜索文件夹，支持部分匹配和正则表达式 |
| `dlqq_search_text_in_dir` | 在目录下所有文件中搜索文本内容，支持正则表达式 |
| `dlqq_search_text_in_file` | 在单个文件中搜索文本内容，支持正则表达式 |

### 对话框工具 (29-31)

| 工具名称 | 描述 |
|---------|------|
| `dlqq_dialog_text` | 文本输入对话框（独立窗口） |
| `dlqq_dialog_check` | 确认对话框，用户选择是/否（独立窗口） |
| `dlqq_dialog_file_path` | 文件选择对话框（独立窗口） |

### 串口工具 (32-48)

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

### BLE库管理工具 (49-52)

| 工具名称 | 描述 |
|---------|------|
| `dlqq_ble_lib_has` | 检查Python BLE库（bleak）是否已安装 |
| `dlqq_ble_lib_install` | 自动安装Python BLE依赖库（bleak） |
| `dlqq_ble_lib_update` | 检查并更新Python BLE库到最新版本 |
| `dlqq_ble_release_script` | 释放BLE Python脚本到项目目录 |

### BLE蓝牙工具 (53-72)

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
| `dlqq_check_bluetooth_open` | 检查Windows电脑的蓝牙适配器是否已开启 |
| `dlqq_rerun_ble` | 重新启动BLE工具（先关闭再启动，用于刷新窗口） |

### MCP数据管理工具 (73-78)

| 工具名称 | 描述 |
|---------|------|
| `dlqq_get_mcp_version` | 获取MCP服务器版本信息、JAR文件修改时间和构建信息 |
| `dlqq_clear_mcp_data` | 清除MCP数据目录（.dlqq），包括日志、Python脚本等 |
| `dlqq_clear_mcp_cache` | 清除MCP缓存目录（.dlqq/cache），删除所有缓存文件和子目录 |
| `dlqq_clear_mcp_log` | 清除MCP日志文件（.dlqq/log），保留目录结构 |
| `dlqq_get_mcp_properties` | 获取MCP配置属性列表或指定属性的值 |
| `dlqq_set_mcp_properties` | 设置MCP配置属性（支持脚本路径、响应大小、搜索配置等） |
| `dlqq_reset_mcp_properties` | 重置MCP配置属性为默认值（可重置单个或所有配置） |

### Office文件工具 (79-80)

| 工具名称 | 描述 |
|---------|------|
| `dlqq_read_office_file` | 读取Office文件（PDF/DOC/DOCX/XLS/XLSX/PPT/PPTX）并转换为Markdown |
| `dlqq_convert_office_file` | 将Office文件从一种格式转换为另一种格式（支持 MD/TXT/DOCX/XLSX/PPTX） |

### 压缩/解压工具 (81-82)

| 工具名称 | 描述 |
|---------|------|
| `dlqq_zip` | 压缩文件或目录为ZIP格式，支持多文件压缩、密码保护、压缩级别设置（1-9） |
| `dlqq_unzip` | 解压ZIP文件，支持密码保护的ZIP文件解压 |

**说明**：
- 压缩格式：仅支持 ZIP（后续可扩展其他格式）
- 密码保护：使用 AES-256 加密
- 压缩级别：1（最快，压缩率最低）到 9（最慢，压缩率最高），默认 5
- 多文件压缩：`source_paths` 参数支持多个文件/目录路径

### BOM控制工具 (83-84)

| 工具名称 | 描述 |
|---------|------|
| `dlqq_add_unicode_bom` | 为文件或目录下的文件添加UTF-8 BOM（Unicode字节顺序标记） |
| `dlqq_remove_unicode_bom` | 从文件或目录下的文件移除UTF-8 BOM |

**说明**：
- 支持单文件或整个目录处理
- 目录处理时遵循AI规则文件（`.aiignore`），跳过被忽略的文件
- 文件读写操作保持BOM状态：原文件有BOM则保留，无BOM则不添加

### 计算器工具 (85-87)

| 工具名称 | 描述 |
|---------|------|
| `dlqq_calculator_math` | 数学计算器 - 精确计算，支持指数、对数、三角函数、双曲函数、阶乘等 |
| `dlqq_calculator_programmer` | 程序计算器 - 整型/浮点计算，支持位运算(AND OR XOR 位移)、多进制转换 |
| `dlqq_calculator_function` | 函数计算器 - 自定义函数表达式，支持变量和内置函数计算 |

**数学计算器支持的函数**：
- `sqrt(x)` - 平方根, `cbrt(x)` - 立方根, `pow(base, exp)` 或 `base^exp` - 幂运算
- `log(x)` / `ln(x)` - 自然对数, `log10(x)` - 常用对数, `log2(x)` - 二进制对数
- `exp(x)` - e的x次幂
- `sin(x)`, `cos(x)`, `tan(x)` - 三角函数（弧度）
- `asin(x)`, `acos(x)`, `atan(x)` - 反三角函数
- `sinh(x)`, `cosh(x)`, `tanh(x)` - 双曲函数
- `abs(x)` - 绝对值, `floor(x)` - 向下取整, `ceil(x)` - 向上取整, `round(x)` - 四舍五入
- `n!` - 阶乘
- 常量: `PI`, `E`

**程序计算器支持的操作**：
- 四则运算: `+`, `-`, `*`, `/`, `%`
- 位运算: `&` (AND), `|` (OR), `^` (XOR), `~` (NOT), `<<`, `>>`, `>>>` (位移)
- 多进制表示: `0x` (十六进制), `0b` (二进制), `0o` (八进制)
- 数据类型: `int`, `long`, `float`, `double`

**函数计算器**：
- 支持自定义变量，如 `{ "x": "3.14", "y": "2.718" }`
- 支持嵌套函数调用，如 `sin(x) + cos(y)`
- 支持所有数学计算器的内置函数

### 命令执行工具 (88)

| 工具名称 | 描述 |
|---------|------|
| `dlqq_run_command` | 执行命令行指令（需要用户审批），支持设置工作目录和超时时间 |

**说明**：
- **执行前必须通过 JFrame 弹窗获得用户确认**
- 支持设置工作目录，命令将在指定目录下执行
- 支持设置超时时间（1-600秒，默认60秒）
- Windows 使用 `cmd /c`，Linux/Mac 使用 `sh -c`
- 返回命令的输出内容和退出码

**示例**：
```json
// 执行简单命令
{
  "command": "echo Hello World"
}

// 指定工作目录
{
  "command": "dir",
  "working_dir": "C:\\Users\\Documents"
}

// 设置超时时间
{
  "command": "ping 127.0.0.1 -n 5",
  "timeout": 30
}
```

### 项目脚本工具 (89-102)

脚本工具执行 `.dlqq/settings/settings.properties` 中配置的批处理脚本，用于自动化项目构建、芯片操作等流程。

**脚本配置说明**：首次使用前需通过 `dlqq_set_mcp_properties` 工具设置脚本路径，或手动编辑 `.dlqq/settings/settings.properties` 文件。

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

### 23. dlqq_search_text_in_dir

在指定目录下的所有文件中搜索文本内容，支持普通文本匹配和正则表达式。

**参数**：
- `search_path` (string, 必需): 搜索目录路径
- `pattern` (string, 必需): 搜索模式
- `use_regex` (boolean, 可选): 是否使用正则表达式，默认 false
- `ignore_case` (boolean, 可选): 是否忽略大小写，默认 true
- `file_extensions` (string, 可选): 文件扩展名过滤，如 "java,txt,md"，留空表示所有文本文件

**示例**：
```json
// 搜索包含 "TODO" 的文本文件
{
  "search_path": "src",
  "pattern": "TODO",
  "use_regex": false,
  "ignore_case": true
}

// 使用正则搜索函数定义
{
  "search_path": "src",
  "pattern": "public void.*\\(",
  "use_regex": true,
  "ignore_case": false,
  "file_extensions": "java"
}
```

**返回**：搜索结果列表，每项包含文件路径、行号、匹配内容等信息

### 24. dlqq_search_text_in_file

在单个文件中搜索文本内容，支持普通文本匹配和正则表达式。

**参数**：
- `file_path` (string, 必需): 文件路径
- `pattern` (string, 必需): 搜索模式
- `use_regex` (boolean, 可选): 是否使用正则表达式，默认 false
- `ignore_case` (boolean, 可选): 是否忽略大小写，默认 true

**示例**：
```json
// 在单个文件中搜索文本
{
  "file_path": "src/main.java",
  "pattern": "public class",
  "use_regex": false,
  "ignore_case": true
}

// 使用正则搜索特定模式
{
  "file_path": "config.properties",
  "pattern": "^\\w+=",
  "use_regex": true,
  "ignore_case": false
}
```

**返回**：搜索结果列表，每项包含行号、匹配内容等信息

### 25. dlqq_dialog_text

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

### 30. dlqq_dialog_check

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

### 27. dlqq_dialog_file_path

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

### 28. dlqq_run_uart

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

### 29. dlqq_uart_get_com

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

### 30. dlqq_uart_refresh_com

刷新可用串口列表。获取系统中当前可用的所有串口设备。

**参数**：
- `port` (string, 必需): 串口工具的端口号（例如："8001"、"8002"等）

**示例**：
```json
{
  "port": 8080
}
```

### 31. dlqq_uart_open_com

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

### 32. dlqq_uart_close_com

关闭当前打开的串口通道。

**参数**：
- `port` (string, 必需): 串口工具的端口号（例如："8001"、"8002"等）

**示例**：
```json
{
  "port": 8080
}
```

### 33. dlqq_uart_reopen_com

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

### 34. dlqq_uart_send_text

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

### 35. dlqq_uart_send_hex

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

### 36. dlqq_uart_get_log

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

### 37. dlqq_uart_get_log_text

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

### 38. dlqq_uart_get_log_hex

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

### 39. dlqq_uart_clear_text

清除串口数据缓冲区。

**参数**：
- `port` (string, 必需): 串口工具的端口号（例如："8001"、"8002"等）

**示例**：
```json
{
  "port": 8080
}
```

### 40. dlqq_uart_get_line_count

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

### 41. dlqq_uart_set_text_size

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

### 42. dlqq_uart_switch_text_hex

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

### 43. dlqq_exit_uart

完全退出串口工具，关闭窗口及进程，释放所有资源。

**参数**：
- `port` (string, 必需): 串口工具的端口号（例如："8001"、"8002"等）

**示例**：
```json
{
  "port": 8080
}
```

### 44. dlqq_run_ble

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

### 45. dlqq_ble_scan

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

### 46. dlqq_ble_get_devices

获取扫描到的BLE设备列表。

**参数**：
- `port` (string, 必需): BLE工具的端口号（例如："8001"、"8002"等）

**示例**：
```json
{
  "port": 8090
}
```

### 47. dlqq_ble_connect

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

### 48. dlqq_ble_disconnect

断开当前连接的BLE设备。

**参数**：
- `port` (string, 必需): BLE工具的端口号（例如："8001"、"8002"等）

**示例**：
```json
{
  "port": 8090
}
```

### 49. dlqq_ble_discover

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

### 50. dlqq_ble_read

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

### 51. dlqq_ble_write_text

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

### 52. dlqq_ble_write_hex

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

### 53. dlqq_ble_subscribe

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

### 54. dlqq_ble_get_log

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

### 55. dlqq_ble_get_log_text

获取BLE日志数据（强制文本格式）。支持3种模式（同 dlqq_ble_get_log）。

**示例**：
```json
{
  "port": 8090,
  "start_line": 1,
  "line_count": 100
}
```

### 56. dlqq_ble_get_log_hex

获取BLE日志数据（强制HEX格式）。支持3种模式（同 dlqq_ble_get_log）。

**示例**：
```json
{
  "port": 8090,
  "start_line": 1,
  "line_count": 100
}
```

### 57. dlqq_ble_clear

清除BLE数据缓冲区。

**参数**：
- `port` (string, 必需): BLE工具的端口号（例如："8001"、"8002"等）

**示例**：
```json
{
  "port": 8090
}
```

### 58. dlqq_ble_set_buffer_size

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

### 59. dlqq_ble_switch_text_hex

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

### 70. dlqq_exit_ble

完全退出BLE工具，关闭窗口及进程，释放所有资源。

**参数**：
- `port` (string, 必需): BLE工具的端口号（例如："8001"、"8002"等）

**示例**：
```json
{
  "port": 8090
}
```

### 71. dlqq_rerun_ble

重新启动BLE工具。先关闭指定端口号的BLE工具（如果正在运行），然后重新启动。用于刷新BLE工具窗口或解决连接问题。

**参数**：
- `port` (string, 必需): BLE工具的端口号（例如："8090"、"8091"等）

**示例**：
```json
{
  "port": 8090
}
```

### 72. dlqq_check_bluetooth_open

检查Windows电脑的蓝牙适配器是否已开启。

**参数**：无

**示例**：
```json
{}
```

**返回**：蓝牙设备状态、蓝牙服务状态、蓝牙网络适配器状态的详细检查结果

### 73. dlqq_get_mcp_version

获取MCP服务器版本信息、JAR文件修改时间和构建信息。

**参数**：无

**示例**：
```json
{}
```

**返回**：版本号、构建时间、JAR文件路径等信息

### 74. dlqq_clear_mcp_data

清除MCP数据目录（.dlqq），删除整个目录及其所有内容（包括日志、Python脚本等）。即使部分文件被占用也会返回成功结果，并告知哪些文件被占用。

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

### 75. dlqq_clear_mcp_cache

清除MCP缓存目录（.dlqq/cache），删除缓存的所有文件和子目录。即使部分文件被占用也会返回成功结果，并告知哪些文件被占用。

**参数**：无

**示例**：
```json
{}
```

**说明**：此操作删除 .dlqq/cache/ 目录下的所有内容，包括：
- Office文件转换后的Markdown文件
- 其他缓存数据

### 76. dlqq_clear_mcp_log

清除MCP日志文件（.dlqq/log目录），保留目录结构。即使部分文件被占用也会返回成功结果，并告知哪些文件被占用。

**参数**：无

**示例**：
```json
{}
```

**说明**：此操作仅删除日志文件，保留 .dlqq/log/ 目录结构。

**注意**：部分日志文件可能因被MCP服务器占用而无法删除，这是正常现象。工具会返回成功结果，并列出已删除的文件和被占用的文件。被占用的文件将在MCP服务器重启后可以被清除。

### 77. dlqq_get_mcp_properties

获取MCP配置属性列表或指定属性的值。配置存储在 `.dlqq/settings/settings.properties` 文件中。

**参数**：
- `key` (string, 可选): 属性键名。如果指定，只返回该属性的值；如果不指定，返回所有属性列表

**示例**：
```json
// 获取所有属性列表
{}

// 获取单个属性
{ "key": "response.max.size" }
```

**返回**：格式化的属性列表，按类别分组显示（响应大小限制、搜索配置、脚本配置、其他配置）

### 77. dlqq_set_mcp_properties

设置MCP配置属性。配置保存到 `.dlqq/settings/settings.properties` 文件中。

**参数**：
- `key` (string, 可选): 属性键名，例如：response.max.size、search.max.files、script.build
- `value` (string, 可选): 属性值

**说明**：至少指定 key 和 value 其中一个参数。如果只指定 key，则显示该属性的当前值。

**常用配置项**：
- `response.max.size` - 最大回复文本大小（字节），默认25804（约25KB），最大258048（API上限）
- `search.max.files` - 最大搜索文件数，默认1000
- `search.max.file.size` - 最大文件大小（字节），默认10MB
- `search.max.results` - 最大搜索结果数，默认50
- `search.text.extensions` - 默认文本文件后缀列表
- `script.build` - 构建脚本路径
- `script.rebuild` - 重建脚本路径
- `script.chip.erase` - 芯片擦除脚本路径
- `script.chip.download` - 芯片下载脚本路径
- `script.erase.download` - 擦除下载组合脚本路径
- `script.build.erase.download` - 构建擦除下载组合脚本路径
- `script.rebuild.erase.download` - 重建擦除下载组合脚本路径
- `script.run` - 运行项目脚本路径
- `script.debug` - 调试项目脚本路径
- `script.build.run` - 构建运行组合脚本路径
- `script.build.debug` - 构建调试组合脚本路径
- `script.rebuild.run` - 重建运行组合脚本路径
- `script.rebuild.debug` - 重建调试组合脚本路径

**示例**：
```json
// 设置最大回复文本大小为64KB
{ "key": "response.max.size", "value": "65536" }

// 设置搜索最大文件数为500
{ "key": "search.max.files", "value": "500" }

// 设置构建脚本路径
{ "key": "script.build", "value": "util/build/build.bat" }

// 设置默认文本文件后缀
{ "key": "search.text.extensions", "value": ".java,.c,.cpp,.h,.py" }

// 查询当前属性的值（不设置）
{ "key": "response.max.size" }
```

**返回**：设置结果消息，包含属性键、旧值（如有）和新值

### 80. dlqq_reset_mcp_properties

重置MCP配置属性为默认值。

**参数**：
- `key` (string, 可选): 要重置的属性键名。如果为空，则重置所有配置为默认值

**示例**：
```json
// 重置单个属性为默认值
{ "key": "response.max.size" }

// 重置所有配置为默认值
{}
```

**说明**：重置后会将配置恢复为系统默认值。

### 81. dlqq_read_office_file

读取Office文件（PDF、DOC、DOCX、XLS、XLSX、PPT、PPTX）并转换为Markdown格式。

**参数**：
- `file_path` (string, 必需): Office文件路径

**示例**：
```json
// 读取PDF文件
{ "file_path": "document.pdf" }

// 读取Word文档
{ "file_path": "report.docx" }

// 读取Excel表格
{ "file_path": "data.xlsx" }

// 读取PPT演示文稿
{ "file_path": "presentation.pptx" }
```

**返回**：转换成功后的Markdown文件路径，保存在 .dlqq/cache 目录

**说明**：
- 支持的格式：PDF、DOC、DOCX、XLS、XLSX、PPT、PPTX
- 转换后的Markdown文件包含文件信息头和原始内容
- 转换后的文件需要使用 dlqq_read_file 工具读取内容

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

### MCP数据管理示例

- "获取MCP版本信息"
- "清除MCP数据目录"
- "清除MCP缓存目录"
- "清除MCP日志文件"
- "获取所有MCP配置属性"
- "设置 response.max.size 为 65536"
- "重置 search.max.files 为默认值"
- "重置所有MCP配置为默认值"

### Office文件读取示例

- "读取 document.pdf 文件"
- "读取 report.docx 并转换为Markdown"
- "读取 data.xlsx 表格文件"
- "读取 presentation.pptx 演示文稿"

### Office文件格式转换示例

- "将 document.pdf 转换为 docx 格式"
- "把 report.docx 转换为 txt 纯文本"
- "将 data.xlsx 转换为 md 格式"
- "把 notes.md 转换为 docx 文档"
- "将 presentation.pptx 转换为 md 大纲"

## 技术栈

- **Java 17+**（必需，runtime目录提供安装包）
- **Python 3.8+**（串口/BLE工具必需，runtime目录提供3.13安装包）
- MCP协议（Model Context Protocol）
- Maven构建工具
- 依赖：
  - jSerialComm（UART串口通信）
  - Python bleak库（BLE蓝牙通信）
  - Apache POI（Office文件处理：DOC/DOCX/XLS/XLSX/PPT/PPTX）
  - Apache PDFBox（PDF文件处理）

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
│   │   ├── BleTools.java          # BLE蓝牙工具
│   │   ├── McpTools.java          # MCP数据管理工具
│   │   └── OfficeTools.java       # Office文件读取工具
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

## AI规则文件

MCP服务器支持三种AI规则文件，用于控制AI对项目文件的操作权限：

### 规则文件类型

| 文件名 | 作用 |
|--------|------|
| `.aiignore` | 指定AI应该忽略的文件/目录（不参与批量操作） |
| `.aiwriterules` | 指定禁止AI修改的文件/目录（写保护） |
| `.aireadrules` | 指定禁止AI读取的文件/目录（读保护） |

### 规则语法（Gitignore风格）

```
# 注释行以#开头

# 忽略所有.log文件
*.log

# 忽略特定目录
build/
target/

# 忽略特定文件
secret.key
config.ini

# 取反规则（不忽略特定文件）
!important.log

# 锚定到根目录（/开头）
/config.txt

# 匹配任意目录层级（**）
**/*.tmp
```

### 规则生效范围

1. **当前项目内**：AI严格遵守规则文件
2. **非当前项目**：不遵守规则，通过JFrame弹窗由用户确认
3. **单文件操作**：不需要遵守规则，仅批量操作需遵守

### 示例 `.aiwriterules` 文件

```
# 禁止修改敏感配置文件
config.properties
settings.json

# 禁止修改生成文件
*.generated.java
/build/

# 但允许修改特定配置文件
!app.config.xml
```

## BOM工具详细说明

### 97. dlqq_add_unicode_bom

为文件或目录下的文件添加UTF-8 BOM。

**参数**：
- `path` (string, 必需): 文件或目录路径
- `is_directory` (boolean, 可选): 是否为目录，默认false
- `file_extensions` (array, 可选): 目录模式下处理的文件扩展名列表，如 `["java", "txt", "md"]`

**示例**：
```json
// 为单个文件添加BOM
{
  "path": "src/main.java",
  "is_directory": false
}

// 为目录下所有.java和.txt文件添加BOM
{
  "path": "src",
  "is_directory": true,
  "file_extensions": ["java", "txt"]
}
```

**说明**：
- 已带BOM的文件不会被重复添加
- 目录处理时遵循 `.aiignore` 规则，跳过被忽略的文件

### 98. dlqq_remove_unicode_bom

从文件或目录下的文件移除UTF-8 BOM。

**参数**：
- `path` (string, 必需): 文件或目录路径
- `is_directory` (boolean, 可选): 是否为目录，默认false
- `file_extensions` (array, 可选): 目录模式下处理的文件扩展名列表，如 `["java", "txt", "md"]`

**示例**：
```json
// 从单个文件移除BOM
{
  "path": "src/main.java",
  "is_directory": false
}

// 从目录下所有.java和.txt文件移除BOM
{
  "path": "src",
  "is_directory": true,
  "file_extensions": ["java", "txt"]
}
```

**说明**：
- 不带BOM的文件会被跳过
- 目录处理时遵循 `.aiignore` 规则，跳过被忽略的文件

### 99. dlqq_calculator_math

数学计算器 - 精确计算支持指数、对数、三角函数、双曲函数、阶乘等。

**参数**：
- `expression` (string, 必需): 数学表达式，如 `2+3*4`, `sin(PI/2)`, `log(100)`, `pow(2, 10)`

**支持的函数和常量**：
- `sqrt(x)` - 平方根, `cbrt(x)` - 立方根
- `pow(base, exp)` 或 `base^exp` - 幂运算
- `log(x)` / `ln(x)` - 自然对数, `log10(x)` - 常用对数, `log2(x)` - 二进制对数
- `exp(x)` - e的x次幂
- `sin(x)`, `cos(x)`, `tan(x)` - 三角函数（弧度）
- `asin(x)`, `acos(x)`, `atan(x)` - 反三角函数
- `sinh(x)`, `cosh(x)`, `tanh(x)` - 双曲函数
- `abs(x)`, `floor(x)`, `ceil(x)`, `round(x)`
- `n!` - 阶乘
- `PI`, `E` - 数学常量

**示例**：
```json
// 基本运算
{
  "expression": "2 + 3 * 4"
}
// 结果: 14

// 使用函数
{
  "expression": "sin(PI/2) + cos(0)"
}
// 结果: 2

// 幂运算
{
  "expression": "pow(2, 10)"
}
// 结果: 1024

// 对数运算
{
  "expression": "log10(1000)"
}
// 结果: 3

// 阶乘
{
  "expression": "5!"
}
// 结果: 120
```

### 100. dlqq_calculator_programmer

程序计算器 - 整型/浮点计算，支持位运算(AND OR XOR 位移)和多进制转换。

**参数**：
- `expression` (string, 必需): 表达式，支持位运算和多进制数，如 `0xFF + 0b1010`, `15 << 2`, `0xFF & 0x0F`
- `type` (string, 可选): 数据类型，可选 `int` | `long` | `float` | `double`，默认 `int`

**支持的操作**：
- 算术: `+`, `-`, `*`, `/`, `%`
- 位运算: `&` (AND), `|` (OR), `^` (XOR), `~` (NOT), `<<`, `>>`, `>>>` (位移)
- 多进制: `0x` (十六进制), `0b` (二进制), `0o` (八进制)

**示例**：
```json
// 十六进制加法
{
  "expression": "0xFF + 0x01",
  "type": "int"
}
// 结果: 十进制: 256, 十六进制: 0x100

// 位左移
{
  "expression": "1 << 8",
  "type": "int"
}
// 结果: 十进制: 256, 十六进制: 0x100

// 位运算 AND
{
  "expression": "0xFF & 0x0F",
  "type": "int"
}
// 结果: 十进制: 15, 十六进制: 0xF

// 异或运算
{
  "expression": "0xFF ^ 0x0F",
  "type": "int"
}
// 结果: 十进制: 240, 十六进制: 0xF0

// 多进制混合
{
  "expression": "0x10 + 0b1010 + 16",
  "type": "int"
}
// 结果: 十进制: 42

// 浮点计算 (显示IEEE 754表示)
{
  "expression": "3.14159",
  "type": "double"
}
// 结果: 十进制: 3.14159, 十六进制: 0x400921F9F01B866E
```

### 101. dlqq_calculator_function

函数计算器 - 自定义函数表达式，支持变量和内置函数计算。

**参数**：
- `expression` (string, 必需): 函数表达式，如 `sin(x) + cos(y)`, `pow(a, 2) + pow(b, 2)`
- `variables` (object, 可选): 变量映射，键为变量名，值为变量值或表达式，如 `{ "x": "3.14", "y": "2.718" }`

**示例**：
```json
// 基本变量替换
{
  "expression": "x + y",
  "variables": {
    "x": "10",
    "y": "20"
  }
}
// 结果: 30

// 三角函数计算
{
  "expression": "sin(x) + cos(y)",
  "variables": {
    "x": "PI/2",
    "y": "0"
  }
}
// 结果: 2

// 勾股定理
{
  "expression": "sqrt(pow(a, 2) + pow(b, 2))",
  "variables": {
    "a": "3",
    "b": "4"
  }
}
// 结果: 5

// 复杂表达式
{
  "expression": "exp(-pow(x, 2) / 2) / sqrt(2 * PI)",
  "variables": {
    "x": "0"
  }
}
// 结果: 0.3989... (标准正态分布PDF)

// 嵌套变量
{
  "expression": "pow(r, 2) * PI",
  "variables": {
    "r": "5"
  }
}
// 结果: 78.5398...
```

### 102. dlqq_run_command

执行命令行指令（需要用户审批）。执行前会弹出确认对话框，用户可以选择执行或取消。支持设置工作目录和超时时间。

**参数**：
- `command` (string, 必需): 要执行的命令行指令，如 `"echo Hello World"`, `"dir"`, `"java -version"`
- `working_dir` (string, 可选): 工作目录，命令将在该目录下执行
- `timeout` (integer, 可选): 超时时间（秒），默认60秒，最大600秒

**示例**：
```json
// 执行简单命令
{
  "command": "echo Hello World"
}
// 输出: Hello World

// 指定工作目录
{
  "command": "dir",
  "working_dir": "C:\\Users\\Documents"
}
// 输出: 目录下的文件列表

// 设置超时时间（秒）
{
  "command": "ping 127.0.0.1 -n 5",
  "timeout": 30
}
// 输出: ping 命令的输出
```

**说明**：
- **执行前必须通过 JFrame 弹窗获得用户确认**
- 确认对话框会显示完整的命令、工作目录和超时时间
- 用户可以选择"执行命令"或"取消"
- Windows 使用 `cmd /c` 执行，Linux/Mac 使用 `sh -c` 执行
- 命令超时后会强制终止进程
- 返回结果包含：命令、工作目录、退出码、状态、输出内容

## 许可证

本项目采用 Apache License 2.0 许可证

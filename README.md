# 鸫楼桥鹊AI工具 (donglouqiaoque-ai-tools)

一个Java编写的AI工具集，支持MCP（Model Context Protocol）协议，可与AI客户端（如Qwen Code、Claude、Cursor等）集成使用。

## 功能特性

- **文件操作** - 读取、创建、删除、复制、移动文件，支持行级编辑
- **目录操作** - 创建、删除、复制、移动文件夹，支持多级目录
- **二进制文件** - 读取二进制文件为十六进制，将十六进制写入为二进制
- **搜索工具** - 在目录下搜索文件或文件夹，支持部分匹配和正则
- **对话框交互** - 文本输入、确认、文件选择等GUI对话框
- **串口通信** - CH340等USB转串口设备的图形化通信工具
- **MCP服务器** - 完整支持MCP协议，可作为AI工具服务器使用

## 工具列表

本MCP服务器提供21个核心工具：

### 文件操作工具 (1-8)

| 工具名称 | 描述 |
|---------|------|
| `dlqq_read_file` | 读取文件指定行范围的内容 |
| `dlqq_create_file` | 创建新文件（支持空文件或带内容） |
| `dlqq_remove_lines_in_file` | 删除文件指定行范围的内容 |
| `dlqq_insert_text_in_file` | 在文件指定行插入内容 |
| `dlqq_replace_text_in_file` | 替换文件中的文本（支持正则） |
| `dlqq_delete_file` | 删除文件（带确认对话框） |
| `dlqq_copy_file` | 复制文件 |
| `dlqq_move_file` | 移动文件 |

### 目录操作工具 (9-13)

| 工具名称 | 描述 |
|---------|------|
| `dlqq_mdir` | 创建文件夹（父目录必须已存在） |
| `dlqq_mdirs` | 创建文件夹（自动创建所有父文件夹） |
| `dlqq_delete_dir` | 删除文件夹（带确认对话框，递归删除） |
| `dlqq_copy_dir` | 复制文件夹（递归复制） |
| `dlqq_move_dir` | 移动文件夹 |

### 二进制文件操作工具 (14-15)

| 工具名称 | 描述 |
|---------|------|
| `dlqq_read_bin_file` | 读取二进制文件，返回十六进制文本 |
| `dlqq_write_bin_file` | 将十六进制文本写入为二进制文件 |

### 搜索工具 (16-17)

| 工具名称 | 描述 |
|---------|------|
| `dlqq_search_file` | 搜索文件，支持部分匹配和正则表达式 |
| `dlqq_search_dir` | 搜索文件夹，支持部分匹配和正则表达式 |

### 对话框工具 (18-20)

| 工具名称 | 描述 |
|---------|------|
| `dlqq_dialog_text` | 文本输入对话框（独立窗口） |
| `dlqq_dialog_check` | 确认对话框，用户选择是/否（独立窗口） |
| `dlqq_dialog_file_path` | 文件选择对话框（独立窗口） |

### 串口工具 (21)

| 工具名称 | 描述 |
|---------|------|
| `dlqq_uart_tools` | CH340串口图形化工具（打开界面/获取日志/清除日志） |

## 快速开始

### 1. 编译项目

使用 Maven 构建（需要安装 Maven）：

```bash
mvn clean package
```

构建完成后，可执行 JAR 文件将生成在 `target/aitools-1.0.0.jar`。

### 2. 配置MCP客户端

#### Qwen Code 配置

在项目根目录创建 `.qwen/mcp.json` 文件：

```json
{
  "mcpServers": {
    "donglouqiaoque-ai-tools": {
      "command": "java",
      "args": [
        "-jar",
        "A:\\MyProject\\Java\\donglouqiaoque_ai_tools\\release\\V1.0.0\\aitools-1.0.0.jar"
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

#### Claude Desktop 配置

编辑 `claude_desktop_config.json`：

```json
{
  "mcpServers": {
    "donglouqiaoque-ai-tools": {
      "command": "java",
      "args": [
        "-jar",
        "C:\\path\\to\\your\\project\\release\\V1.0.0\\aitools-1.0.0.jar"
      ],
      "env": {
        "JAVA_TOOL_OPTIONS": "-Dfile.encoding=UTF-8"
      }
    }
  }
}
```

#### Cursor 配置

编辑 `.cursor/mcp.json`：

```json
{
  "mcpServers": {
    "donglouqiaoque-ai-tools": {
      "command": "java",
      "args": [
        "-jar",
        "/path/to/your/project/release/V1.0.0/aitools-1.0.0.jar"
      ]
    }
  }
}
```

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

**示例**：
```json
{
  "file_path": "config.txt",
  "content": "key=value\nname=test"
}
```

### 3. dlqq_remove_lines_in_file

删除文件中指定行范围的内容。

**参数**：
- `file_path` (string, 必需): 文件路径
- `start_line` (integer, 必需): 起始行（包含）
- `end_line` (integer, 必需): 结束行（包含）

**示例**：
```json
{
  "file_path": "data.txt",
  "start_line": 5,
  "end_line": 10
}
```

### 4. dlqq_insert_text_in_file

在文件指定行之前插入内容。

**参数**：
- `file_path` (string, 必需): 文件路径
- `line` (integer, 必需): 插入位置（在该行之前插入）
- `content` (string, 必需): 要插入的内容（支持多行）

**示例**：
```json
{
  "file_path": "data.txt",
  "line": 10,
  "content": "// 新插入的注释\nnewFunction();"
}
```

### 5. dlqq_replace_text_in_file

替换文件中的文本，支持正则表达式。

**参数**：
- `file_path` (string, 必需): 文件路径
- `search_text` (string, 必需): 要搜索的文本或正则
- `replace_text` (string, 必需): 替换后的文本
- `mode` (string, 可选): 模式，"text"（普通文本）或 "regex"（正则表达式，默认"text"）

**示例**：
```json
{
  "file_path": "config.txt",
  "search_text": "old_value",
  "replace_text": "new_value",
  "mode": "text"
}
```

### 6. dlqq_delete_file

删除文件，会弹出确认对话框。

**参数**：
- `file_path` (string, 必需): 要删除的文件路径

**示例**：
```json
{
  "file_path": "temp.txt"
}
```

### 7. dlqq_copy_file

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

### 8. dlqq_move_file

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

### 9. dlqq_mdir

创建文件夹（只创建最后一级，父目录必须已存在）。

**参数**：
- `dir_path` (string, 必需): 文件夹路径

**示例**：
```json
{
  "dir_path": "new_folder"
}
```

### 10. dlqq_mdirs

创建文件夹（包括所有父文件夹一起创建）。

**参数**：
- `dir_path` (string, 必需): 文件夹路径

**示例**：
```json
{
  "dir_path": "parent/child/grandchild"
}
```

### 11. dlqq_delete_dir

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

### 12. dlqq_copy_dir

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

### 13. dlqq_move_dir

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

### 14. dlqq_read_bin_file

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

### 15. dlqq_write_bin_file

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

### 16. dlqq_search_file

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

### 17. dlqq_search_dir

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

### 18. dlqq_dialog_text

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

### 19. dlqq_dialog_check

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

### 20. dlqq_dialog_file_path

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

### 21. dlqq_uart_tools

CH340串口图形化工具，支持打开界面、获取日志、清除日志。

**参数**：
- `action` (string, 必需): 操作类型
  - `"open"`: 打开图形界面
  - `"get_logs"`: 获取日志
  - `"clear_logs"`: 清除日志
- `log_lines` (integer, 可选): 获取日志时返回的行数（默认100）

**示例**：
```json
// 打开串口工具界面
{
  "action": "open"
}

// 获取最近50行日志
{
  "action": "get_logs",
  "log_lines": 50
}

// 清除日志
{
  "action": "clear_logs"
}
```

**串口工具功能**：
- 自动查找系统中的CH340设备
- 支持多种波特率（9600-921600）
- 实时显示接收和发送数据
- 记录完整的通信日志
- 支持发送数据时自动添加换行符

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

- "打开串口工具界面"
- "获取串口最近100行日志"
- "查找系统中的CH340设备"

## 技术栈

- Java 17+
- MCP协议（Model Context Protocol）
- Maven构建工具
- 依赖：
  - jSerialComm（UART串口通信）

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
│   │   └── UartTools.java         # 串口工具
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

本项目采用 MIT 许可证

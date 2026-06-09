# 鸫楼桥鹊 AI 工具 MCP 服务器

一个功能丰富的 Java MCP 服务器，提供文件操作、目录管理、二进制文件处理、搜索、GUI对话框和串口通信等36个工具。

---

## 功能概览

| 类别 | 工具数量 | 说明 |
|------|----------|------|
| 文件操作 | 12 | 读取、创建、覆盖、追加、删除、复制、移动、重命名、行编辑等 |
| 目录操作 | 6 | 创建、删除、复制、移动、重命名文件夹 |
| 二进制文件 | 2 | 读取二进制为十六进制、将十六进制写入二进制 |
| 搜索工具 | 2 | 文件搜索、目录搜索（支持正则） |
| 对话框工具 | 3 | 文本输入、确认对话框、文件选择 |
| 串口工具 | 13 | CH340串口通信、数据收发、缓冲区管理 |

---

## 快速配置

在 `.qwen/mcp.json` 中添加：

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
      }
    }
  }
}
```

> **注意**：请将 JAR 路径修改为你本地的实际绝对路径。

---

## 工具详细说明

### 一、文件操作工具（12个）

#### 1. `dlqq_read_file` - 读取文件

读取指定文件的指定行范围内容，支持字符集编码。

**参数**：
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `file_path` | string | ✓ | 文件路径（支持绝对/相对路径） |
| `start_line` | integer | ✗ | 起始行（1-based，默认1） |
| `end_line` | integer | ✗ | 结束行（1-based，-1表示到末尾） |
| `read_lines` | integer | ✗ | 读取行数（与end_line互斥） |
| `charset` | string | ✗ | 字符集编码（默认UTF-8） |

**示例**：
```json
{
  "file_path": "src/main.java",
  "start_line": 10,
  "end_line": 20
}
```

---

#### 2. `dlqq_create_file` - 创建文件

创建新文件，可指定内容。自动创建父目录。文件已存在会覆盖。

**参数**：
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `file_path` | string | ✓ | 文件路径 |
| `content` | string | ✗ | 文件内容（为空则创建空文件） |
| `charset` | string | ✗ | 字符集编码（默认UTF-8） |

**示例**：
```json
{
  "file_path": "config.txt",
  "content": "key=value\nname=test"
}
```

---

#### 3. `dlqq_overwrite_file` - 覆盖文件

覆盖指定文件内容，不管文件是否存在都直接写入。

**参数**：同 `dlqq_create_file`

---

#### 4. `dlqq_append_file` - 追加文件内容

在文件末尾追加内容。文件不存在则自动创建。

**参数**：
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `file_path` | string | ✓ | 文件路径 |
| `content` | string | ✓ | 要追加的内容 |
| `charset` | string | ✗ | 字符集编码（默认UTF-8） |

---

#### 5. `dlqq_remove_lines_in_file` - 删除指定行

删除文件中指定行范围的内容。

**参数**：
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `file_path` | string | ✓ | 文件路径 |
| `start_line` | integer | ✓ | 起始行（包含） |
| `end_line` | integer | ✓ | 结束行（包含） |
| `charset` | string | ✗ | 字符集编码（默认UTF-8） |

---

#### 6. `dlqq_replace_lines_in_file` - 替换指定行

将文件中指定行范围的内容替换为新内容。

**参数**：
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `file_path` | string | ✓ | 文件路径 |
| `start_line` | integer | ✓ | 起始行（包含） |
| `end_line` | integer | ✓ | 结束行（包含） |
| `content` | string | ✓ | 替换的新内容（支持多行） |
| `charset` | string | ✗ | 字符集编码（默认UTF-8） |

---

#### 7. `dlqq_insert_text_in_file` - 插入文本

在文件指定行之前插入内容。

**参数**：
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `file_path` | string | ✓ | 文件路径 |
| `line` | integer | ✓ | 插入位置（在该行之前插入） |
| `content` | string | ✓ | 要插入的内容（支持多行） |
| `charset` | string | ✗ | 字符集编码（默认UTF-8） |

---

#### 8. `dlqq_replace_text_in_file` - 替换文本（支持正则）

替换文件中的指定文本。

**参数**：
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `file_path` | string | ✓ | 文件路径 |
| `search_text` | string | ✓ | 要搜索的文本或正则表达式 |
| `replace_text` | string | ✓ | 替换后的文本 |
| `mode` | string | ✗ | 模式："text"或"regex"（默认"text"） |
| `charset` | string | ✗ | 字符集编码（默认UTF-8） |

**示例**：
```json
{
  "file_path": "config.txt",
  "search_text": "old_value",
  "replace_text": "new_value",
  "mode": "text"
}
```

---

#### 9. `dlqq_delete_file` - 删除文件

删除文件，会弹出确认对话框。

**参数**：
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `file_path` | string | ✓ | 要删除的文件路径 |

---

#### 10. `dlqq_copy_file` - 复制文件

复制文件到指定位置，自动创建目标目录。

**参数**：
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `source_path` | string | ✓ | 源文件路径 |
| `target_path` | string | ✓ | 目标文件路径 |

---

#### 11. `dlqq_move_file` - 移动文件

移动文件到指定位置。

**参数**：同 `dlqq_copy_file`

---

#### 12. `dlqq_rename_file` - 重命名文件

在同一目录下重命名文件。

**参数**：
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `file_path` | string | ✓ | 源文件路径 |
| `new_name` | string | ✓ | 新文件名（仅文件名，不含路径） |

---

### 二、目录操作工具（6个）

#### 13. `dlqq_mdir` - 创建文件夹

创建文件夹，父目录必须已存在。

**参数**：
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `dir_path` | string | ✓ | 文件夹路径 |

---

#### 14. `dlqq_mdirs` - 创建多级文件夹

创建文件夹，自动创建所有父文件夹。

**参数**：同 `dlqq_mdir`

---

#### 15. `dlqq_delete_dir` - 删除文件夹

递归删除文件夹，会弹出确认对话框显示文件和子目录数量。

**参数**：
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `dir_path` | string | ✓ | 要删除的文件夹路径 |

---

#### 16. `dlqq_copy_dir` - 复制文件夹

递归复制文件夹及所有内容。

**参数**：
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `source_path` | string | ✓ | 源文件夹路径 |
| `target_path` | string | ✓ | 目标文件夹路径 |

---

#### 17. `dlqq_move_dir` - 移动文件夹

移动文件夹到指定位置。

**参数**：同 `dlqq_copy_dir`

---

#### 18. `dlqq_rename_dir` - 重命名文件夹

在同一目录下重命名文件夹。

**参数**：
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `dir_path` | string | ✓ | 源文件夹路径 |
| `new_name` | string | ✓ | 新文件夹名（仅名称，不含路径） |

---

### 三、二进制文件操作工具（2个）

#### 19. `dlqq_read_bin_file` - 读取二进制文件

读取二进制文件并将字节转换为十六进制文本。

**参数**：
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `file_path` | string | ✓ | 二进制文件路径 |
| `start_byte` | integer | ✗ | 起始字节（0-based，默认0） |
| `end_byte` | integer | ✗ | 结束字节（0-based，-1表示到末尾） |
| `read_bytes` | integer | ✗ | 读取字节数（与end_byte互斥） |

**返回**：十六进制文本（如 "89504E470D0A1A0A..."）

---

#### 20. `dlqq_write_bin_file` - 写入二进制文件

将十六进制文本转换为二进制并写入文件。

**参数**：
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `file_path` | string | ✓ | 目标文件路径 |
| `hex_content` | string | ✓ | 十六进制文本（可包含空格、换行） |
| `append` | boolean | ✗ | 是否追加写入（默认false） |

---

### 四、搜索工具（2个）

#### 21. `dlqq_search_file` - 搜索文件

在指定目录下查找文件，支持部分匹配和正则表达式。

**参数**：
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `search_path` | string | ✓ | 搜索目录路径 |
| `pattern` | string | ✓ | 搜索模式 |
| `mode` | string | ✗ | 模式："text"（默认）或"regex" |

**示例**：
```json
// 查找文件名包含 "test" 的文件
{
  "search_path": "src",
  "pattern": "test",
  "mode": "text"
}

// 查找以 .java 结尾的文件（正则）
{
  "search_path": "src",
  "pattern": "\\.java$",
  "mode": "regex"
}
```

---

#### 22. `dlqq_search_dir` - 搜索文件夹

在指定目录下查找文件夹。

**参数**：同 `dlqq_search_file`

---

### 五、对话框工具（3个）

#### 23. `dlqq_dialog_text` - 文本输入对话框

向用户展示文本输入对话框，等待用户输入。

**参数**：
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `title` | string | ✓ | 对话框标题 |
| `message` | string | ✓ | 提示消息 |

**返回**：用户输入的文本内容

---

#### 24. `dlqq_dialog_check` - 确认对话框

向用户展示确认对话框，用户选择是/否。

**参数**：
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `title` | string | ✓ | 对话框标题 |
| `message` | string | ✓ | 确认消息内容 |

**返回**："是" 或 "否"

---

#### 25. `dlqq_dialog_file_path` - 文件选择对话框

向用户展示文件选择对话框。

**参数**：
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `title` | string | ✓ | 对话框标题 |
| `extensions` | array | ✗ | 允许的文件扩展名列表，如 `["txt", "java"]` |

**返回**：选中的文件路径

---

### 六、串口工具（13个）

串口工具支持多实例管理，每个实例通过**端口号**区分。

#### 26. `dlqq_run_uart` - 启动串口服务器

打开CH340串口工具窗口，启动服务器监听指定端口。

**参数**：
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `port` | integer | ✓ | 服务器端口号（1~65535） |

**示例**：
```json
{
  "port": 9001
}
```

**注意**：必须先调用 `dlqq_run_uart` 启动串口工具，其他串口工具才能使用。

---

#### 27. `dlqq_uart_get_com` - 获取串口列表

获取系统中所有可用的串口通道列表。

**参数**：
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `port` | integer | ✓ | 串口工具实例端口号 |

---

#### 28. `dlqq_uart_refresh_com` - 刷新串口列表

重新扫描系统串口，获取最新的串口列表。

**参数**：同 `dlqq_uart_get_com`

---

#### 29. `dlqq_uart_open_com` - 打开串口

打开指定串口，开始捕获数据。

**参数**：
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `port` | integer | ✓ | 串口工具实例端口号 |
| `port_name` | string | ✓ | 串口名称，如 "COM3"、"ttyUSB0" |
| `baud_rate` | integer | ✗ | 波特率（默认1000000，范围0~2000000） |

---

#### 30. `dlqq_uart_reopen_com` - 重新打开串口

先关闭当前串口，再用新的波特率重新打开。

**参数**：同 `dlqq_uart_open_com`

---

#### 31. `dlqq_uart_close_com` - 关闭串口

关闭当前打开的串口通道。

**参数**：同 `dlqq_uart_get_com`

---

#### 32. `dlqq_uart_send_text` - 发送文本数据

发送文本数据到串口。

**参数**：
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `port` | integer | ✓ | 串口工具实例端口号 |
| `text` | string | ✓ | 要发送的文本数据 |

---

#### 33. `dlqq_uart_send_hex` - 发送十六进制数据

发送十六进制数据到串口。

**参数**：
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `port` | integer | ✓ | 串口工具实例端口号 |
| `hex_array` | array | ✓ | 十六进制字符串数组，如 `["FF", "01", "02"]` |

---

#### 34. `dlqq_uart_get_log` - 获取串口日志数据

获取串口缓冲区中的日志数据，支持3种读取模式。返回格式取决于当前显示格式设置（文本或HEX）。

**模式1 - 直接范围读取**：`start_line` + `end_line`
**模式2 - 直接计数读取**：`start_line` + `line_count`
**模式3 - 等待读取**：`timeout_ms` + `line_count`

**参数**：
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `port` | integer | ✓ | 串口工具实例端口号 |
| `start_line` | integer | ✗ | 起始行号（1-based，模式1和2使用） |
| `end_line` | integer | ✗ | 结束行号（模式1使用，-1表示到末尾） |
| `line_count` | integer | ✗ | 读取行数（模式2和3使用） |
| `timeout_ms` | integer | ✗ | 等待超时毫秒（模式3使用，0表示不等待） |

**说明**：返回格式可通过 `dlqq_uart_switch_text_hex` 切换为文本或HEX格式。

---

#### 35. `dlqq_uart_clear_text` - 清除串口数据

清除串口数据缓冲区及所有统计数据。

**参数**：同 `dlqq_uart_get_com`

---

#### 36. `dlqq_uart_get_line_count` - 获取缓冲区行数

获取当前串口数据缓冲区中的日志行数。

**参数**：同 `dlqq_uart_get_com`

---

#### 37. `dlqq_uart_set_text_size` - 设置缓冲区大小

设置串口数据缓冲区大小。

**参数**：
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `port` | integer | ✓ | 串口工具实例端口号 |
| `size_bytes` | integer | ✓ | 缓冲区大小（字节） |

---

#### 38. `dlqq_uart_switch_text_hex` - 切换显示格式

切换日志显示格式（文本/HEX）。切换后，UI日志显示和 `dlqq_uart_get_log` 返回的数据格式都会相应改变。

**参数**：
| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `port` | integer | ✓ | 串口工具实例端口号 |
| `format` | string | ✓ | 显示格式：`text` 或 `hex` |

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

---

#### 39. `dlqq_exit_uart` - 退出串口工具

关闭串口工具窗口及进程，释放所有资源。

**参数**：同 `dlqq_uart_get_com`

---

## 使用示例

### 文件操作

```
读取 src/main.java 第10到20行
在 config.txt 第5行插入 "// 新注释"
把 data.txt 中的 "old" 替换为 "new"
删除 temp.txt 文件
```

### 目录操作

```
创建文件夹 new_folder
创建多级目录 parent/child/grandchild
删除 old_folder 文件夹
复制 src_folder 到 backup/src_folder
```

### 搜索

```
在 src 目录下搜索所有 .java 文件
查找文件名包含 "test" 的文件
```

### 对话框

```
弹出一个对话框询问用户是否要保存
让用户选择一个配置文件
询问用户项目名称
```

### 串口操作

```
启动端口 9001 的串口工具
获取可用串口列表
打开 COM3 串口，波特率 115200
发送文本 "Hello" 到串口
获取最近 50 行串口日志
切换日志显示格式为HEX
设置缓冲区大小为10MB
关闭串口
退出串口工具
```

---

## 技术信息

- **开发语言**：Java 17+
- **构建工具**：Maven
- **通信协议**：MCP (Model Context Protocol)
- **串口库**：jSerialComm 2.10.4
- **字符集**：默认 UTF-8，支持 GBK、ISO-8859-1 等

---

## 项目结构

```
donglouqiaoque-ai-tools/
├── src/main/java/com/donglouqiaoque/aitools/
│   ├── mcp/
│   │   ├── MCPServerMain.java      # MCP服务器入口
│   │   ├── tools/
│   │   │   ├── AITools.java        # 工具注册
│   │   │   ├── FileTools.java      # 文件操作实现
│   │   │   ├── DialogTools.java    # 对话框实现
│   │   │   └── UartTools.java      # 串口工具实现
│   │   └── server/
│   │       └── McpServer.java      # MCP服务器核心
│   └── utils/
│       └── FileUtils.java          # 文件工具类
├── release/V1.0.0/
│   └── aitools-1.0.0.jar           # 可执行JAR
└── pom.xml                          # Maven配置
```

---

## 许可证

Apache License 2.0

# 鸫楼桥鹊AI工具

一个Java编写的AI工具集，支持命令行使用和MCP（Model Context Protocol）协议，可与AI客户端（如Claude、Cursor等）集成使用。

## 功能特性

- **配置管理** - 管理API密钥、API地址和默认模型
- **AI对话** - 与AI进行文本对话
- **文件处理** - 读取、分析和总结文件内容
- **MCP服务器** - 支持MCP协议，可作为AI工具服务器使用

## 使用方式

本工具支持两种使用方式：
1. **命令行模式** - 直接通过命令行使用
2. **MCP服务器模式** - 作为MCP服务器与AI客户端集成

## 项目结构

```
src/com/donglouqiaoque/aitools/
├── Main.java                  # CLI程序入口
├── mcp/
│   ├── MCPServerMain.java     # MCP服务器入口
│   ├── protocol/
│   │   ├── JsonRpcMessage.java    # JSON-RPC消息
│   │   └── McpSchema.java         # MCP协议定义
│   ├── server/
│   │   └── McpServer.java         # MCP服务器核心
│   ├── tools/
│   │   └── AITools.java           # AI工具注册
│   └── transport/
│       ├── McpTransport.java      # 传输层接口
│       └── StdioTransport.java    # stdio传输实现
├── ai/
│   ├── ChatMessage.java       # 聊天消息模型
│   └── AIService.java         # AI服务
├── cli/
│   ├── CommandParser.java     # 命令行解析器
│   └── commands/
│       ├── Command.java       # 命令接口
│       ├── ConfigCommand.java # 配置命令
│       ├── ChatCommand.java   # 聊天命令
│       ├── FileCommand.java   # 文件命令
│       └── HelpCommand.java   # 帮助命令
├── config/
│   └── ConfigManager.java     # 配置管理器
├── exit/
│   ├── Exit.java              # 退出工具
│   └── ExitCode.java          # 退出码枚举
├── http/
│   └── HttpClient.java        # HTTP客户端
└── utils/
    └── FileUtils.java         # 文件工具类
```

## 快速开始

### 1. 编译项目

使用 Maven 构建（需要安装 Maven）：

```bash
mvn clean package
```

构建完成后：
- CLI 工具：`java -jar target/aitools-1.0.0.jar <command>`
- MCP 服务器：`java -jar release/V1.0.0/aitools-1.0.0.jar`

### 2. 配置API密钥

```bash
# 设置OpenAI API密钥
java -jar target/aitools-1.0.0.jar config set-key your-api-key

# 查看当前配置
java -jar target/aitools-1.0.0.jar config list
```

### 3. 使用AI对话

```bash
# 基本对话
java -jar target/aitools-1.0.0.jar chat 你好

# 使用引号包含空格
java -jar target/aitools-1.0.0.jar chat "请帮我解释什么是Java"
```

### 4. 文件处理

```bash
# 读取文件
java -jar target/aitools-1.0.0.jar file read ./readme.md

# 询问文件内容
java -jar target/aitools-1.0.0.jar file ask ./code.java "这段代码做了什么"

# 总结文件
java -jar target/aitools-1.0.0.jar file summarize ./report.txt
```

## 命令详解

### config - 配置管理

```bash
config set-key <api-key>    # 设置API密钥
config set-url <api-url>    # 设置API地址（可选，默认OpenAI）
config set-model <model>    # 设置默认模型（可选，默认gpt-3.5-turbo）
config get <key>            # 获取配置项
config list                 # 列出所有配置
```

### chat - AI对话

```bash
chat <message>              # 发送消息给AI
chat -m "<message>"         # 使用-m选项发送消息
```

### file - 文件处理

```bash
file read <file>            # 读取文件内容
file ask <file> <question>  # 询问关于文件的问题
file summarize <file>       # 总结文件内容
```

### help - 帮助

```bash
help                        # 显示所有命令
help <command>              # 显示特定命令的帮助
```

## 配置文件

配置文件存储在用户主目录下的 `.aitools/config.properties`：

```properties
# API配置
api.key=your-api-key
api.url=https://api.openai.com/v1/chat/completions
api.model=gpt-3.5-turbo
```

## 注意事项

1. 使用前必须配置API密钥
2. 默认使用OpenAI API，支持兼容OpenAI格式的其他API
3. 文件处理功能会自动限制内容长度，避免超出token限制
4. 请妥善保管API密钥，配置文件存储在用户主目录下

## MCP服务器使用

### 编译MCP服务器

使用 Maven 构建（推荐）：

```bash
mvn clean package
```

构建完成后，可执行 JAR 文件将生成在 `target/aitools-1.0.0.jar`，同时复制到 `release/V1.0.0/` 目录。

### 配置MCP客户端

#### Qwen Code配置

Qwen Code 通过项目根目录下的 `.qwen/settings.json` 文件自动发现和加载 MCP 服务器。

**配置文件位置**: 项目根目录的 `.qwen/settings.json`

配置内容：
```json
{
  "mcpServers": {
    "donglouqiaoque-ai-tools": {
      "command": "java",
      "args": [
        "-jar",
        "F:\\Code\\Java\\donglouqiaoque-ai-tools\\release\\V1.0.0\\aitools-1.0.0.jar"
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

**Qwen Code 会自动读取此配置并连接到 MCP 服务器。**

连接成功后，你可以在对话中直接使用以下工具：
- `ai_chat` - AI对话
- `file_read` - 读取文件
- `file_write` - 写入文件
- `file_summarize` - 文件摘要
- `config_get` - 获取配置
- `config_set` - 设置配置

#### Claude Desktop配置 (`claude_desktop_config.json`):
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

**Cursor配置** (`.cursor/mcp.json`):
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

### MCP工具列表

MCP服务器提供以下工具：

**AI工具**：
| 工具名称 | 描述 |
|---------|------|
| `ai_chat` | 与AI进行对话，支持自定义模型参数 |
| `file_read` | 读取指定路径的文件内容 |
| `file_write` | 写入内容到指定文件，自动创建目录 |
| `file_summarize` | 读取文件并使用AI生成摘要 |
| `config_get` | 获取AI工具配置信息 |
| `config_set` | 设置AI工具配置（API密钥、地址、模型） |

**GUI对话框工具**：
| 工具名称 | 描述 |
|---------|------|
| `dialog_input` | 弹出输入对话框，支持文本输入和文件选择 |
| `dialog_file_select` | 弹出文件选择对话框 |
| `dialog_confirm` | 弹出确认对话框 |
| `dialog_message` | 弹出消息提示对话框（异步） |

**UART串口工具**：
| 工具名称 | 描述 |
|---------|------|
| `uart_list_ports` | 列出系统可用串口（包括CH340等USB转串口） |
| `uart_open_port` | 打开指定串口连接 |
| `uart_close_port` | 关闭串口连接 |
| `uart_send_data` | 向串口发送数据 |
| `uart_receive_data` | 从串口接收数据 |
| `uart_send_at` | 发送AT指令并等待响应 |
| `uart_get_logs` | 获取串口通信日志 |
| `uart_clear_logs` | 清除串口通信日志 |
| `uart_find_ch340` | 查找系统中的CH340设备 |

### 使用示例

在支持MCP的AI客户端中，可以直接请求AI使用这些工具：

- "请读取 ./readme.md 文件的内容"
- "帮我总结一下 ./report.txt 的内容"
- "使用AI分析这段代码"

## 技术栈

- Java 17+
- 标准Java HTTP客户端（HttpURLConnection）
- MCP协议（Model Context Protocol）
- Maven构建工具
- 依赖：jSerialComm（用于UART串口通信）

## 许可证

本项目采用与仓库相同的许可证

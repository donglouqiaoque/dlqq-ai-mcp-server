# AGENTS.md — dlqq-ai-mcp-server 工程约定

> 本文给 AI 助手（Qwen Code / DeepSeek Harness 等）阅读，包含硬性规则、构建发布方式、结构与工具速查。
> 完整工具手册见 [README.md](README.md)。两份文档是**唯一**的说明文档（原 `ai_res/doc` 下的内容已合并到这里与 README，目录已删除）。

## 一、硬性规则（必须遵守）

1. **语言与构建**：Java 17 + Maven。
2. **工程定位**：面向各类 AI Agent 客户端（DeepSeek Harness、Qwen Code、Claude Code、Cursor、Cline 等）的 MCP 服务器（stdio 传输、JSON-RPC 2.0），入口 `com.donglouqiaoque.dlqqaimcpserver.mcp.MCPServerMain`。
3. **脚本位置**：构建/发布脚本放工程根目录 `script\`；辅助脚本（如生成完整 JRE）放 `.ai_res\script\`。不要把 `.bat/.ps1/.sh` 放在这两处之外。
4. **.bat 禁止中文**：`.bat` 文件中**不得出现任何中文字符**（注释、字符串、`echo` 输出一律用英文/ASCII）。
   原因：cmd 默认代码页（GBK/936）与 UTF-8 混用会导致中文乱码，甚至影响批处理解析。
5. **产物命名**：JAR 固定名为 **`dlqq_ai_mcp_server.jar`**（历史名 `dlqq_ai_tools.jar` 已弃用）。
6. **改完代码必须构建**：每次修改代码后都要重新打包，并按下面的规则替换发布目录里的 JAR。
7. **Debug 版本不区分版本号**：直接把 JAR 复制到 `debug\` 下，即 `debug\dlqq_ai_mcp_server.jar`。
8. **Release 版本按版本号分目录**：`release\V{版本号}\jar\dlqq_ai_mcp_server.jar`。
9. **工具变更要同步文档**：增删或修改 MCP 工具后，必须同步更新 [README.md](README.md) 与本文的工具速查。
10. **诊断日志**：`StdioTransport`、`McpServer`、`MCPServerMain` 有详细连接诊断日志，输出到 `./.dlqq/log/logger/` 与 `./.dlqq/log/stderr/`，按时间命名（每次启动新建）。

## 二、运行环境要求

### Java

- **Java 17 或更高版本**；**必须使用完整版 JRE**：精简 jlink 镜像缺 `jdk.crypto.ec`、`jdk.zipfs`、`java.net.http` 等模块，可能在 PDF、TLS、zipfs 场景报错。
- 生成完整 JRE（`ALL-MODULE-PATH` 包含 JDK 全部模块，约 130 MB）：
  ```cmd
  "<JDK17>\bin\jlink" --module-path "<JDK17>\jmods" --add-modules ALL-MODULE-PATH --output "<目标目录>\jre" --strip-debug --no-man-pages --no-header-files
  ```
- 本机现成脚本 `.ai_res\script\make-jre.bat`（`.ai_res` 已被 git 忽略，仅本地有效）：复制到 JDK 根目录（与 `bin`、`jmods` 同级）双击运行，输出 `<JDK>\jre`。
- 生成时的 `WARNING: Using incubator modules: ...` 属正常现象（`ALL-MODULE-PATH` 含孵化器模块），退出码 0、镜像可用，忽略即可。

### Python（串口/BLE 图形工具）

- **Python 3.8 或更高版本**，BLE 工具依赖 `bleak` 库。
- 当前仓库未附带 Python 安装包，需自行安装。

## 三、MCP 客户端配置

本服务是标准 **stdio MCP Server**，可接入任意 MCP 客户端；服务端在 `initialize` 响应里的名称是 **`dlqq-ai-mcp-server`**，`tools/call` 的工具名前缀仍是 `dlqq_`。

### DeepSeek Harness (DSH)

在 `C:\Users\<用户名>\.dsh\cordis.patch.yml` 追加：

```yaml
- insert:
    - id: dlqq-mcp
      name: "@deepseek-ai/dsh-mcp-client"
      config:
        serverName: dlqq-ai-mcp-server
        transport: stdio
        command: java
        args:
          - "-Dfile.encoding=UTF-8"
          - "-jar"
          - 'E:\Code\AIProject\dlqq-ai-mcp-server\debug\dlqq_ai_mcp_server.jar'
        env:
          JAVA_TOOL_OPTIONS: "-Dfile.encoding=UTF-8"
```

保存后重启 DSH。`command` 也可换成 java 绝对路径（YAML 中加引号）。

### 其他客户端

统一是 `mcpServers` + `command/args/env` 结构（VS Code 例外，用 `servers` + `type`）：

```json
{
  "mcpServers": {
    "dlqq-ai-mcp-server": {
      "command": "java",
      "args": ["-Dfile.encoding=UTF-8", "-jar", "E:\\Code\\AIProject\\dlqq-ai-mcp-server\\debug\\dlqq_ai_mcp_server.jar"],
      "env": { "JAVA_TOOL_OPTIONS": "-Dfile.encoding=UTF-8" },
      "timeout": 60000
    }
  }
}
```

各客户端配置文件位置（Qwen Code、Claude Code/Desktop、Cursor、VS Code、Cline/Roo Code、Windsurf）见 [README.md](README.md) 的“接入 AI Agent 客户端”一节。

## 四、构建与发布

### 配置文件 `script\config.properties`

```properties
# 版本号配置
release_version=1.0.0
debug_version=255.0.0

# true  = debug 版本，输出到 debug\dlqq_ai_mcp_server.jar（不区分版本号）
# false = release 版本，输出到 release\V{release_version}\jar\dlqq_ai_mcp_server.jar
use_debug=true
```

### 脚本

| 脚本 | 作用 | 输出 |
|------|------|------|
| `script\build.bat` | 仅执行 `mvn clean package -DskipTests -Drevision={version}` | `target\dlqq_ai_mcp_server.jar` |
| `script\build_and_copy.bat` | 构建并复制到发布目录 | debug 或 release 目录 |

**发布目录规则**

| 模式 | 条件 | 目标路径 |
|------|------|----------|
| Debug | `use_debug=true` | `debug\dlqq_ai_mcp_server.jar` |
| Release | `use_debug=false` | `release\V{release_version}\jar\dlqq_ai_mcp_server.jar` |

用法：

```cmd
script\build.bat
script\build_and_copy.bat
```

改版本号只需编辑 `config.properties`；改完代码建议直接跑 `build_and_copy.bat`。

## 五、代码结构速查

```
src/main/java/com/donglouqiaoque/dlqqaimcpserver/
├── mcp/
│   ├── MCPServerMain.java          # 入口：日志目录/UTF-8 初始化、启动、健康检查线程
│   ├── protocol/                   # JsonRpcMessage、McpSchema
│   ├── server/McpServer.java       # 请求分发 + 工具注册表 + getToolsInfo()
│   ├── transport/                  # McpTransport 接口、StdioTransport（双线程+队列）
│   └── tools/                      # AITools（注册中心）与各工具实现
├── cli/                            # 命令行解析（历史保留，主程序不走这里）
├── config/ConfigManager.java       # 旧配置（~/.aitools，历史保留）
└── utils/                          # FileUtils、AIRulesUtils、CalculatorUtils
src/main/resources/
├── py/ble_tools.py                 # BLE 图形工具（Python + bleak，Java 通过 Socket 驱动）
├── image/logo.ico
└── simplelogger.properties
```

运行期目录（首次启动自动创建，已被 `.gitignore` 忽略）：`.dlqq/log/{logger,stderr}`、`.dlqq/settings/settings.properties`、`.dlqq/cache`。

## 六、工具速查

共 **159** 个工具（146 个常规工具 + 13 个 `dlqq_set_*_script`），实现见 `mcp/tools/`，逐个参数说明见 [README.md](README.md)。

| 分组 | 数量 | 说明 |
|------|------|------|
| 文件操作 | 15 | 读写/创建/覆盖/追加/行级编辑/文本替换/复制移动重命名 |
| 目录操作 | 8 | mdir、mdirs、树形列出、递归复制等 |
| 二进制 | 2 | HEX 读写 |
| BOM | 2 | UTF-8 BOM 增删 |
| 搜索 | 4 | 文件/目录/目录内文本/单文件文本 |
| 对话框 | 3 | 文本输入、确认、文件选择（Swing） |
| 串口 UART | 17 | 窗口 + 开关串口 + 收发 + 日志（Python 可选） |
| BLE | 24 | 库管理 4 + 蓝牙 20（Python bleak + Socket） |
| USB | 14 | usb4java，HID/CDC |
| WiFi | 12 | netsh / nmcli，Java 原生 |
| 鼠标 / 键盘 | 13 | `java.awt.Robot` 模拟 |
| 屏幕截图 | 1 | `dlqq_capture_screen` |
| 时间 | 1 | `dlqq_get_current_time` |
| 计算器 | 3 | 数学 / 程序 / 函数 |
| 命令执行 | 1 | `dlqq_run_command`（JFrame 人工确认） |
| Office | 2 | 读取转 Markdown、格式转换 |
| 压缩解压 | 2 | zip4j + commons-compress |
| MCP 数据/属性 | 7 | 版本信息、属性读写、清理数据/缓存/日志 |
| 项目脚本 | 15 | 构建/烧写/运行/调试 ＋ 脚本路径设置 |
| 工具列表 | 1 | `dlqq_get_tool_list` |

## 七、AI 规则文件

| 文件 | 作用 |
|------|------|
| `.aiignore` | AI 忽略的文件/目录 |
| `.aiwriterules` | 禁止 AI 修改（写保护） |
| `.aireadrules` | 禁止 AI 读取（读保护） |

语法为 Gitignore 风格（`*.log`、`build/`、`/config.txt`、`!important.log`、`**/*.tmp`）。
生效范围：工程内严格遵守；工程外弹 JFrame 由用户确认；单文件操作不受规则约束。

## 八、已知限制

- **PDF/TLS/zipfs 等**：依赖**完整版 JRE**；精简 jlink 镜像缺模块时会报错（历史 `Could not initialize class org.apache.pdfbox.io.IOUtils` 即此类）。完整 JRE 生成方式见“二、运行环境要求”。
- **硬件相关工具**（串口/BLE/USB）在没有对应硬件时只能验证窗口与管理类接口，收发的端到端链路需实机验证。
- **PDF 写入**：工具侧不支持（`dlqq_convert_office_file` 目标格式不含 PDF），需先转 DOCX 再用外部工具导出。

## 九、参考资料

- Qwen Code MCP 配置教程：<https://qwenlm.github.io/qwen-code-docs/zh/users/features/mcp/>

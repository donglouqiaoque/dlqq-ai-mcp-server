# AGENTS.md — dlqq-ai-mcp-server 工程约定

> 本文给 AI 助手（Qwen Code / DeepSeek Harness 等）阅读，包含硬性规则、构建发布方式、结构与工具速查。
> 完整工具手册见 [README.md](README.md)（英文版：[README.en.md](README.en.md)）。两份文档是**唯一**的说明文档。

## 一、硬性规则（必须遵守）

1. **语言与构建**：Java 17 + Maven。
2. **工程定位**：面向各类 AI Agent 客户端（DeepSeek Harness、Qwen Code、Claude Code、Cursor、Cline 等）的 MCP 服务器（stdio 传输、JSON-RPC 2.0），入口 `com.donglouqiaoque.dlqqaimcpserver.mcp.MCPServerMain`。
3. **脚本位置**：构建/发布/部署脚本放工程根目录 `script\`（含 `build.bat`、`build_and_copy.bat`、`start-http.bat`、`make-jre.bat`）；临时脚本放本地临时工作目录（约定见 `AIRES.md`）。不要把 `.bat/.ps1/.sh` 放在这两处之外。
4. **.bat 必须使用英文**：`.bat` 文件**只能出现英文/ASCII 字符，不得出现任何中文**（注释、`echo`、提示语、变量说明一律用英文）。
   原因：cmd 默认代码页（GBK/936）与 UTF-8 混用会导致中文乱码，甚至影响批处理解析。
5. **产物命名**：JAR 固定名为 **`dlqq_ai_mcp_server.jar`**（历史名 `dlqq_ai_tools.jar` 已弃用）。
6. **改完代码必须构建**：每次修改代码后都要重新打包，并按下面的规则替换发布目录里的 JAR。
7. **Debug 版本不区分版本号**：直接把 JAR 复制到 `debug\` 下，即 `debug\dlqq_ai_mcp_server.jar`。
8. **Release 版本按版本号分目录**：`release\V{版本号}\jar\dlqq_ai_mcp_server.jar`。
9. **工具变更要同步文档**：增删或修改 MCP 工具后，必须同步更新 [README.md](README.md) 与本文的工具速查。
10. **诊断日志**：`StdioTransport`、`McpServer`、`MCPServerMain` 有详细连接诊断日志，输出到 `<工作区>/.dlqq/log/logger/` 与 `<工作区>/.dlqq/log/stderr/`，按时间命名（每次启动新建）。
11. **临时文件位置**：临时文件/中间产物**禁止放 C 盘系统临时目录**，统一放工程根目录下的本地临时工作目录（约定见 `AIRES.md`），按类型分子目录，用完自行清理。
12. **禁止用脚本批量修改代码**：不允许用 PowerShell / Python / sed / awk 等脚本对源码做批量查找替换或自动改写。所有代码修改必须逐个文件、逐个位置人工确认后用编辑工具完成；确需大范围调整时，先列出清单交由用户确认，再逐项修改。
13. **禁止使用任何 git 命令**：包括但不限于 `git status`、`git diff`、`git log`、`git checkout/restore/reset`、`git add/commit/push/pull/stash` 等。版本管理由用户本人操作，AI 一律不得执行任何 git 命令（读取与回滚都不允许）。
14. **禁止使用全限定类名**：代码中不得出现 `new java.xxx.XXX()`、`java.util.List` 这类全限定写法。**能 import 的一律在文件顶部 import，然后使用短类名**；只有无法 import 的情况（同名类冲突、需要显式区分同名的不同包类型、静态导入等）才允许写全限定名，并在该处注释说明原因。注释/文档中的类名描述不算代码，但也不建议加包名。
15. **README 中英文必须同步**：修改 [README.md](README.md)（中文）时必须同步修改 [README.en.md](README.en.md)（英文），反之亦然；两份 README 的章节、工具数量、工具说明、配置示例必须保持一致，不允许只改其中一份。
16. **日志必须 UTF-8 且纯英文**：`.dlqq/log/` 下的日志文件必须使用 **UTF-8** 编码写入；日志内容**只能使用英文**，不得出现中文（包括 `logger.info/warn/error/debug` 的提示文本、异常附加说明）。新增或修改日志时必须写英文。
17. **配置文件必须 UTF-8 且注释纯英文**：`settings.properties` 等配置文件必须使用 **UTF-8** 编码读写；文件内的注释**一律使用英文**，不得写入中文注释；键值内容如含中文（如脚本路径）保持 UTF-8 原样，不做转义。

## 二、运行环境要求

### Java

- **Java 17 或更高版本**；**必须使用完整版 JRE**：精简 jlink 镜像缺 `jdk.crypto.ec`、`jdk.zipfs`、`java.net.http` 等模块，可能在 PDF、TLS、zipfs 场景报错。
- 生成完整 JRE（`ALL-MODULE-PATH` 包含 JDK 全部模块，约 130 MB）：
  ```cmd
  "<JDK17>\bin\jlink" --module-path "<JDK17>\jmods" --add-modules ALL-MODULE-PATH --output "<目标目录>\jre" --strip-debug --no-man-pages --no-header-files
  ```
- 本机现成脚本 `script\make-jre.bat`：复制到 JDK 根目录（与 `bin`、`jmods` 同级）双击运行，输出 `<JDK>\jre`；部署时把该 `jre\` 与 `start-http.bat`、JAR 放同一目录。
- 生成时的 `WARNING: Using incubator modules: ...` 属正常现象（`ALL-MODULE-PATH` 含孵化器模块），退出码 0、镜像可用，忽略即可。

### Python（串口/BLE 图形工具）

- **Python 3.8 或更高版本**，BLE 工具依赖 `bleak` 库。
- 当前仓库未附带 Python 安装包，需自行安装。

## 三、MCP 客户端配置

本服务是标准 **MCP Server**，支持 **stdio**（默认）与 **Streamable HTTP** 两种传输，可接入任意 MCP 客户端；服务端在 `initialize` 响应里的名称是 **`dlqq-ai-mcp-server`**，`tools/call` 的工具名前缀仍是 `dlqq_`。

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

### HTTP（Streamable HTTP）方式

先启动服务（一次启动可被多个客户端共用，服务端无鉴权、无会话状态）：

```cmd
java -jar dlqq_ai_mcp_server.jar --transport http --port 49129 --path /mcp
```

端点：`http://127.0.0.1:49129/mcp`。

**DSH**（`~/.dsh/cordis.patch.yml`，`transport: streamable-http` + `url`）：

```yaml
- insert:
    - id: dlqq-mcp-http
      name: "@deepseek-ai/dsh-mcp-client"
      config:
        serverName: dlqq-ai-mcp-server
        transport: streamable-http
        url: http://127.0.0.1:49129/mcp
```

**其他客户端**只需把 stdio 的 `command/args` 换成 URL：

| 客户端 | HTTP 关键字段 |
|--------|----------------|
| Qwen Code | `"httpUrl": "http://127.0.0.1:49129/mcp"`（旧版 SSE 用 `url`） |
| Claude Code | `claude mcp add --transport http <name> <url>`；JSON 用 `"type": "http"` + `"url"` |
| Cursor | `"url": "http://127.0.0.1:49129/mcp"` |
| VS Code (Copilot) | `"servers"` 里 `"type": "http"` + `"url"` |
| Cline / Roo Code | `"type": "streamableHttp"` + `"url"` |
| Windsurf | `"serverUrl": "http://127.0.0.1:49129/mcp"` |

完整示例见 [README.md](README.md) 的“⑧ 用 HTTP（Streamable HTTP）方式接入”。

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
| `script\start-http.bat` | 部署用 HTTP 启动脚本（与 `jre\`、JAR 同目录，默认端口 49129；启动前先要求输入工作区目录，回车=CWD） | 常驻 HTTP 服务 |
| `script\make-jre.bat` | 生成完整版 JRE（复制到 JDK 根目录执行，输出 `<JDK>\jre`） | `jre\`（约 130 MB） |

**发布目录规则**

| 模式 | 条件 | 目标路径 |
|------|------|----------|
| Debug | `use_debug=true` | `debug\dlqq_ai_mcp_server.jar` |
| Release | `use_debug=false` | `release\V{release_version}\jar\dlqq_ai_mcp_server.jar` |

### HTTP 部署结构

```text
dlqq_ai_mcp_server\
├── start-http.bat
├── dlqq_ai_mcp_server.jar
└── jre\                # 由 script\make-jre.bat 生成（完整版）
    └── bin\java.exe
```

- 默认端口 **49129**；被占用时控制台提示输入新端口，输入的新端口仍被占用会继续提示；**直接回车 = 重试默认端口**；无交互控制台则以退出码 1 结束。
- `jre\` 缺失时脚本回退到 PATH 上的 `java`（部署仍建议自带 `jre\`）。

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
│   ├── transport/                  # McpTransport 接口、StdioTransport（stdio）、HttpTransport（Streamable HTTP）
│   └── tools/                      # AITools（注册中心）与各工具实现（含 ImageTools、AboutPanel、LogoLoader、WindowsNativeUtils）
├── cli/                            # 命令行解析（历史保留，主程序不走这里）
├── config/ConfigManager.java       # 旧配置（~/.aitools，历史保留）
└── utils/                          # FileUtils、AIRulesUtils、CalculatorUtils
src/main/resources/
├── py/ble_tools.py                 # BLE 图形工具（Python + bleak，Java 通过 Socket 驱动）
├── image/logo.ico                  # 窗口图标（ICO，供 Python/BLE 使用）
├── image/logo.png                  # 窗口图标（PNG，LogoLoader 使用）
└── simplelogger.properties
```

启动参数：`--transport http|stdio`、`--port N`（默认 49129）、`--path /mcp`、`--workspace <dir>`；默认 stdio。

运行期目录（首次启动自动创建，已被 `.gitignore` 忽略）：工作区内 `.dlqq/log/{logger,stderr}`、`.dlqq/cache`、`.dlqq/settings/settings.properties`（工作区默认 = 进程 CWD）。

## 六、工具速查

共 **169** 个工具（156 个常规工具 + 13 个 `dlqq_set_*_script`），实现见 `mcp/tools/`，逐个参数说明见 [README.md](README.md)。

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
| 屏幕截图 | 1 | `dlqq_capture_screen`（含鼠标光标绘制） |
| 图片转换 | 1 | `dlqq_convert_image`（SVG/PNG/JPG/GIF/BMP，Batik 渲染 SVG） |
| 时间 | 1 | `dlqq_get_current_time` |
| 计算器 | 3 | 数学 / 程序 / 函数 |
| 命令执行 | 1 | `dlqq_run_command`（JFrame 人工确认） |
| Office | 2 | 读取转 Markdown、格式转换 |
| 压缩解压 | 2 | zip4j + commons-compress |
| MCP 数据/属性 | 15 | 版本信息（含编译进代码的真实构建时间）、热加载测试、动态工具管理（移除/刷新/恢复/直接调用）、工作区设置与权限（set/get/reset/permission）、属性读写、清理数据/缓存/日志 |
| 项目脚本 | 15 | 构建/烧写/运行/调试 ＋ 脚本路径设置 |
| 工具列表 | 1 | `dlqq_get_tool_list` |

**动态工具管理**：`dlqq_mcp_remove_tools`（移除并持久化）、`dlqq_mcp_refresh_tools`（仅推送 `notifications/tools/list_changed`）、`dlqq_mcp_reset_tools`（恢复全部并清空配置）、`dlqq_mcp_run_tools`（按名称直接调用任意已注册工具，绕过“已移除/客户端未加载”限制）。排除项存于 `<工作区>/.dlqq/settings/settings.properties` 的 `mcp.tools.excluded`（逗号分隔），服务端重启后仍保持移除状态；这四个管理工具受保护，不允许被移除。HTTP 模式下立即刷新需要客户端已打开 SSE 长连接。

**工作区**：`dlqq_workspace_set` / `dlqq_workspace_get` / `dlqq_workspace_reset` / `dlqq_workspace_permission_set`；启动参数 `--workspace <dir>`。未指定且未运行期设置时，工作区 = 进程 CWD。**工作区路径不持久化**（仅 `--workspace` 或运行期设置，重启需重新指定）；修改权限和其它全部配置（`script.*`、`response.max.size`、`mcp.tools.excluded` 等）持久化在工作区内 `.dlqq/settings/settings.properties`，权限键为 `workspace.write.mode`（`readonly` 只读 / `workspace` 仅工作区 / `approval` 工作区外审批 / `full` 完全权限），切换工作区自动重载。所有工具的相对路径、脚本执行目录、AI 规则基准都基于工作区解析；工作区内 `.dlqq`（日志、缓存、配置）整个随工作区移动。

**脚本工具规则**：`dlqq_build_project` / `dlqq_rebuild_project` / `dlqq_chip_*` / `dlqq_*erase_download` / `dlqq_run_project` / `dlqq_debug_project` 等**没有任何内置默认脚本路径**；未配置时直接报错“需要先配置工具路径”，并提示对应的 `dlqq_set_*_script`。配置写入 `<工作区>/.dlqq/settings/settings.properties` 的 `script.*` 项。

## 七、AI 规则文件

| 文件 | 作用 |
|------|------|
| `.aiignore` | AI 忽略的文件/目录 |
| `.aiwriterules` | 禁止 AI 修改（写保护） |
| `.aireadrules` | 禁止 AI 读取（读保护） |

语法为 Gitignore 风格（`*.log`、`build/`、`/config.txt`、`!important.log`、`**/*.tmp`）。
生效范围：工程内严格遵守；工程外弹 JFrame 由用户确认；单文件操作不受规则约束。

## 八、热加载与动态工具逻辑（重要）

### 结论

- JVM 不支持运行中重载已加载的类：**换 JAR 必须重启服务端进程**。
- 重启服务端会断开 SSE 长连接；HTTP 客户端（DSH，协议 2025-11-25）断开后**不会**自动重连 SSE、也不会重新 `initialize`/`tools/list`，所以客户端工具列表仍是旧的，新增/删除的工具对客户端不可见，**必须重启客户端**。
- `notifications/tools/list_changed` 只在 SSE 存活时有效：服务端不重启、工具集变化时，推送后客户端约 2ms 重新 `tools/list`（已实测）。

### 两个判断工具

| 工具 | 说明 |
|------|------|
| `dlqq_get_mcp_version` | `构建时间（代码内，真实值）` 来自 `BuildInfo.BUILD_TIME`（templating-maven-plugin 编译期注入），替换 JAR 不重启不会变；`JAR清单构建时间（磁盘）` 读磁盘清单，替换后会变。两者不一致 = JAR 已换、进程仍跑旧代码 |
| `dlqq_test_hot_reload` | 返回写死的 `HOT_RELOAD_TEST_MARK`（当前 v4），改标记重新构建可验证代码更新 |

### SSE 与重启（机制）

- HTTP 模式：客户端 `GET /mcp` + `Accept: text/event-stream` 建立 SSE；`HttpTransport.sseClients` 保存连接，`notifyToolsListChanged()` 遍历推送。
- 重启 = 旧进程退出 + 新进程重新 listen：旧 TCP 连接被内核关闭、连接列表清空；新进程无法继承旧连接（TCP 连接属于旧进程的 socket）。
- DSH 实测：服务端重启后不重连 SSE、不 `initialize`，只在调用时发 POST `tools/call` → 无推送通道（`notifyToolsListChanged()` 返回 false）。

### 验证流程

1. 构建新 JAR，记录 `BuildInfo.BUILD_TIME`；
2. 停 HTTP 服务，确认端口释放；
3. 覆盖部署目录 JAR；
4. 重启服务；
5. 调 `dlqq_get_mcp_version`：代码内时间 == 磁盘清单时间 == 新值 → 已加载；代码内仍为旧值 → 未生效；
6. 如果涉及工具增删/改名/schema 变化，重启客户端后再确认工具列表。

### 动态工具管理（不重启服务端的替代）

- `dlqq_mcp_remove_tools` / `dlqq_mcp_refresh_tools` / `dlqq_mcp_reset_tools` / `dlqq_mcp_run_tools`：运行时禁用/恢复已注册工具，持久化到 `mcp.tools.excluded`，SSE 存活时客户端实时刷新；`run_tools` 可按名称直接调用被移除或客户端未加载的工具。
- 限制：只能隐藏/恢复**已注册**的工具，不能加载新代码；四个管理工具受保护不可移除。

## 九、已知限制

- **PDF/TLS/zipfs 等**：依赖**完整版 JRE**；精简 jlink 镜像缺模块时会报错（历史 `Could not initialize class org.apache.pdfbox.io.IOUtils` 即此类）。完整 JRE 生成方式见“二、运行环境要求”。
- **硬件相关工具**（串口/BLE/USB）在没有对应硬件时只能验证窗口与管理类接口，收发的端到端链路需实机验证。
- **PDF 写入**：工具侧不支持（`dlqq_convert_office_file` 目标格式不含 PDF），需先转 DOCX 再用外部工具导出。

## 十、参考资料

### MCP 协议

- MCP 官方文档：<https://modelcontextprotocol.io/>
- MCP 规范（含 Streamable HTTP 传输定义）：<https://modelcontextprotocol.io/specification/2026-07-28>

### 各 Agent 客户端接入文档

| 客户端 | 官方文档 | 本项目配置文件 |
|--------|----------|----------------|
| **DeepSeek Harness (DSH)** | 见本文“三、MCP 客户端配置”（插件 `@deepseek-ai/dsh-mcp-client`） | `C:\Users\<用户名>\.dsh\cordis.patch.yml` |
| Qwen Code | <https://qwenlm.github.io/qwen-code-docs/zh/users/features/mcp/> | `settings.json` |
| Claude Code | <https://code.claude.com/docs/en/mcp> | `.mcp.json` / `claude mcp add` |
| Cline | <https://docs.cline.bot/mcp/mcp-overview> | `cline_mcp_settings.json` |
| Cursor | <https://cursor.com/docs/mcp> | `.cursor\mcp.json` |
| VS Code (GitHub Copilot) | <https://code.visualstudio.com/docs/agent-customization/mcp-servers> | `.vscode\mcp.json`（`servers` + `type`） |

> 以上链接均为 2026-10 实测可访问；若后续客户端改版导致失效，以各客户端官网文档导航为准。

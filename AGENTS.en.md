# AGENTS.en.md — dlqq-ai-mcp-server Engineering Conventions

> This document is for AI assistants (Qwen Code / DeepSeek Harness, etc.). It contains hard rules, build and release instructions, code structure and a tool overview.
> The full tool manual is [README.en.md](README.en.md) (Chinese: [README.md](README.md)). These documents are the **only** documentation.
> Chinese version of the rules: [AGENTS.md](AGENTS.md).

## 1. Hard rules (mandatory)

1. **Language and build**: Java 25 + Maven.
2. **Project role**: an MCP server (stdio transport, JSON-RPC 2.0) for AI agent clients (DeepSeek Harness, Qwen Code, Claude Code, Cursor, Cline, ...). Entry point: `com.donglouqiaoque.dlqqaimcpserver.mcp.MCPServerMain`.
3. **Script location**: build/release/deploy scripts live in `script\` at the project root (`build.bat`, `build_and_copy.bat`, `start-http.bat`, `make-jre.bat`, `make_python_env.bat`). Temporary scripts go to the local temporary working directory (see `AIRES.md`). Do not put `.bat/.ps1/.sh` anywhere else.
4. **`.bat` files must be English-only**: a `.bat` file may contain **only English/ASCII characters** — no Chinese at all (comments, `echo` text, prompts and variable descriptions must all be English).
   Reason: the cmd default code page (GBK/936) mixed with UTF-8 garbles Chinese and can even break batch parsing.
5. **Artifact name**: the JAR is always **`dlqq_ai_mcp_server.jar`** (the old name `dlqq_ai_tools.jar` is deprecated).
6. **Always build after code changes**: repackage after every code change and replace the JAR in the release directory as described below.
7. **Debug builds are not versioned**: copy the JAR to `debug\`, i.e. `debug\dlqq_ai_mcp_server.jar`.
8. **Release builds are versioned**: `release\V{version}\jar\dlqq_ai_mcp_server.jar`.
9. **Tool changes must update the docs**: after adding, removing or changing an MCP tool, update [README.en.md](README.en.md) and the tool overview in this file.
10. **Diagnostic logs**: `StdioTransport`, `McpServer` and `MCPServerMain` write detailed connection diagnostics to `<workspace>/.dlqq/log/logger/` and `<workspace>/.dlqq/log/stderr/`, named by time (a new file per start).
11. **Temporary files**: temporary files and intermediate output **must not go to the system temp directory on drive C**. Put them under the local temporary working directory inside the project root (see `AIRES.md`), grouped by type, and clean them up when done.
12. **No scripted bulk code edits**: PowerShell / Python / sed / awk and similar tools must not be used to bulk search-replace or rewrite source code. Every code change must be made file by file and location by location with the editing tool; if a large refactor is really needed, list the changes for user approval first, then apply them one by one.
13. **No git commands whatsoever**: including but not limited to `git status`, `git diff`, `git log`, `git checkout/restore/reset`, `git add/commit/push/pull/stash`. Version control is handled by the user only; the AI must never run any git command (not even for reading or rollback).
14. **No fully-qualified class names**: code must not contain `new java.xxx.XXX()` or `java.util.List` style references. **Anything that can be imported must be imported at the top of the file and used by its short name**; only when importing is impossible (name clash between same-named types from different packages, a static import, etc.) may a fully-qualified name be used, with a comment explaining why. Class names in comments and documentation are not code, but package prefixes are still discouraged.
15. **README and AGENTS must stay in sync across languages**: editing [README.md](README.md) (Chinese) requires the same edit in [README.en.md](README.en.md) (English) and vice versa; editing [AGENTS.md](AGENTS.md) requires the same edit in [AGENTS.en.md](AGENTS.en.md). Sections, tool counts, tool descriptions and configuration examples must match in every language; changing only one file is not allowed.
16. **Logs must be UTF-8 and English-only**: files under `.dlqq/log/` must be written in **UTF-8**, and their content must be **English only**. No Chinese anywhere, including `logger.info/warn/error/debug` messages and exception notes. New or modified log lines must be written in English.
17. **Config files must be UTF-8 with English-only comments**: `settings.properties` and similar files must be read and written as **UTF-8**, and comments inside them must be **English only** — no Chinese comments. Values that contain Chinese (such as script paths) stay as-is in UTF-8, without escaping.
18. **The AI must not execute scripts with side effects**: scripts that download, install, delete, unpack, bulk-clean, flash/download firmware, or that modify files, take a long time or require administrator rights **must never be run by the AI**. Hand the complete command to the user to run manually. The AI may only perform read-only checks (network connectivity, path existence, reading file contents), and must never confirm a dialog on the user's behalf or bypass a permission prompt.

## 2. Runtime requirements

### Java

- **Java 25 or newer**; **a full JRE is required**. Trimmed jlink images lack `jdk.crypto.ec`, `jdk.zipfs`, `java.net.http` and others, which can break PDF, TLS and zipfs scenarios.
- Generate a full JRE (`ALL-MODULE-PATH` bundles every JDK module, about 130 MB):
  ```cmd
  "<JDK25>\bin\jlink" --module-path "<JDK25>\jmods" --add-modules ALL-MODULE-PATH --output "<target>\jre" --strip-debug --no-man-pages --no-header-files
  ```
- Ready-made script `script\make-jre.bat`: copy it into the JDK root (next to `bin` and `jmods`) and double-click; it writes `<JDK>\jre`. For deployment, put that `jre\` next to `start-http.bat` and the JAR.
- The `WARNING: Using incubator modules: ...` message is normal (`ALL-MODULE-PATH` includes incubator modules). Exit code is 0 and the image works — ignore it.

### Python (BLE tool)

- **No system Python at runtime**: the BLE tool always uses the bundled `python_env\` (generated by `script\make_python_env.bat`; it contains the interpreter, tkinter, Tcl/Tk and bleak).
- For deployment, put `python_env\` in the same directory as `dlqq_ai_mcp_server.jar`. If it is missing, the BLE tool fails with an explicit error and never falls back to a Python on `PATH`.
- Building `python_env` requires a full CPython (3.10+, with tkinter) on the build machine; that is build-time only.

## 3. MCP client configuration

This is a standard **MCP Server** supporting **stdio** (default) and **Streamable HTTP**. It works with any MCP client. The server name in the `initialize` response is **`dlqq-ai-mcp-server`**, and tool names keep the `dlqq_` prefix.

### DeepSeek Harness (DSH)

Append to `C:\Users\<user>\.dsh\cordis.patch.yml`:

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

Restart DSH after saving. `command` may also be an absolute path to java (quote it in YAML).

### Other clients

They all use `mcpServers` + `command/args/env` (VS Code is the exception: `servers` + `type`):

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

Config file locations for each client (Qwen Code, Claude Code/Desktop, Cursor, VS Code, Cline/Roo Code, Windsurf) are listed in the "Connect AI agent clients" section of [README.en.md](README.en.md).

### HTTP (Streamable HTTP)

Start the server first (one instance can serve several clients; there is no authentication and no session state):

```cmd
java -jar dlqq_ai_mcp_server.jar --transport http --port 49129 --path /mcp
```

Endpoint: `http://127.0.0.1:49129/mcp`.

**DSH** (`~/.dsh/cordis.patch.yml`, `transport: streamable-http` + `url`):

```yaml
- insert:
    - id: dlqq-mcp-http
      name: "@deepseek-ai/dsh-mcp-client"
      config:
        serverName: dlqq-ai-mcp-server
        transport: streamable-http
        url: http://127.0.0.1:49129/mcp
```

**Other clients** only need a URL instead of `command/args`:

| Client | HTTP fields |
|--------|-------------|
| Qwen Code | `"httpUrl": "http://127.0.0.1:49129/mcp"` (older SSE used `url`) |
| Claude Code | `claude mcp add --transport http <name> <url>`; JSON uses `"type": "http"` + `"url"` |
| Cursor | `"url": "http://127.0.0.1:49129/mcp"` |
| VS Code (Copilot) | under `"servers"`: `"type": "http"` + `"url"` |
| Cline / Roo Code | `"type": "streamableHttp"` + `"url"` |
| Windsurf | `"serverUrl": "http://127.0.0.1:49129/mcp"` |

Full examples are in the "8) Connect over HTTP (Streamable HTTP)" section of [README.en.md](README.en.md).

## 4. Build and release

### Config file `script\config.properties`

```properties
# Version configuration
release_version=1.0.0
debug_version=255.0.0

# true  = debug build, output debug\dlqq_ai_mcp_server.jar (no version in the name)
# false = release build, output release\V{release_version}\jar\dlqq_ai_mcp_server.jar
use_debug=true
```

### Scripts

| Script | Purpose | Output |
|--------|---------|--------|
| `script\build.bat` | Runs `mvn clean package -DskipTests -Drevision={version}` only | `target\dlqq_ai_mcp_server.jar` |
| `script\build_and_copy.bat` | Builds and copies to the release directory | debug or release directory |
| `script\start-http.bat` | HTTP launcher for deployment (lives next to `jre\` and the JAR, default port 49129; asks for the workspace directory first, empty = CWD) | long-running HTTP service |
| `script\make-jre.bat` | Generates a full JRE (copy into the JDK root to run, output `<JDK>\jre`) | `jre\` (about 130 MB) |
| `script\make_python_env.bat` | Generates the portable Python for BLE (run inside the Python installation, output `<PythonDir>\python_env`) | `python_env\` (about 156 MB) |

**Release directory rules**

| Mode | Condition | Target path |
|------|-----------|-------------|
| Debug | `use_debug=true` | `debug\dlqq_ai_mcp_server.jar` |
| Release | `use_debug=false` | `release\V{release_version}\jar\dlqq_ai_mcp_server.jar` |

### HTTP deployment layout

```text
dlqq_ai_mcp_server\
├── start-http.bat
├── dlqq_ai_mcp_server.jar
├── jre\                # generated by script\make-jre.bat (full JRE)
│   └── bin\java.exe
└── python_env\         # generated by script\make_python_env.bat (BLE, full runtime)
    └── python.exe
```

- Default port is **49129**. If it is in use, the console asks for another port and keeps asking while the new one is also taken; **pressing Enter retries the default port**; with no interactive console the script exits with code 1.
- If `jre\` is missing, the script falls back to the `java` on `PATH` (shipping `jre\` is still recommended).

Usage:

```cmd
script\build.bat
script\build_and_copy.bat
```

To change the version, edit `config.properties`; after code changes simply run `build_and_copy.bat`.

## 5. Code structure

```
src/main/java/com/donglouqiaoque/dlqqaimcpserver/
├── mcp/
│   ├── MCPServerMain.java          # entry: log dir / UTF-8 init, startup, health check thread
│   ├── protocol/                   # JsonRpcMessage, McpSchema
│   ├── server/McpServer.java       # request dispatch + tool registry + getToolsInfo()
│   ├── transport/                  # McpTransport, StdioTransport (stdio), HttpTransport (Streamable HTTP)
│   └── tools/                      # AITools (registry) and tool implementations (ImageTools, AboutPanel, LogoLoader, WindowsNativeUtils, ...)
├── cli/                            # command-line parsing (legacy, not used by the main program)
├── config/ConfigManager.java       # legacy config (~/.aitools, kept for history)
└── utils/                          # FileUtils, AIRulesUtils, CalculatorUtils
src/main/resources/
├── py/ble_tools.py                 # BLE GUI tool (Python + bleak, driven by Java over a socket)
├── image/logo.ico                  # window icon (ICO, used by Python/BLE)
├── image/logo.png                  # window icon (PNG, used by LogoLoader)
└── simplelogger.properties
```

Startup arguments: `--transport http|stdio`, `--port N` (default 49129), `--path /mcp`, `--workspace <dir>`; stdio is the default.

Runtime directories (created on first start, ignored by `.gitignore`): inside the workspace, `.dlqq/log/{logger,stderr}`, `.dlqq/cache` and `.dlqq/settings/settings.properties` (the workspace defaults to the process CWD).

## 6. Tool overview

**166** tools in total (153 regular tools + 13 `dlqq_set_*_script` helpers). Implementations live in `mcp/tools/`; parameters are documented in [README.en.md](README.en.md).

| Group | Count | Notes |
|-------|-------|-------|
| File operations | 15 | read/write/create/overwrite/append, line edits, text replace, copy/move/rename |
| Directory operations | 8 | mdir, mdirs, tree listing, recursive copy, ... |
| Binary | 2 | hex read/write |
| BOM | 2 | UTF-8 BOM add/remove |
| Search | 4 | files, directories, text in a directory, text in one file |
| Dialogs | 3 | text input, confirmation, file picker (Swing) |
| Serial UART | 17 | window + open/close + send/receive + log (Python optional) |
| BLE | 21 | 20 Bluetooth tools + 1 script release (Python bleak + socket) |
| USB | 14 | usb4java, HID/CDC |
| WiFi | 12 | netsh / nmcli, native Java |
| Mouse / keyboard | 13 | `java.awt.Robot` |
| Screenshot | 1 | `dlqq_capture_screen` (draws the mouse cursor) |
| Image conversion | 1 | `dlqq_convert_image` (SVG/PNG/JPG/GIF/BMP, Batik renders SVG) |
| Time | 1 | `dlqq_get_current_time` |
| Calculators | 3 | math / programmer / function |
| Command execution | 1 | `dlqq_run_command` (JFrame confirmation) |
| Office | 2 | read to Markdown, format conversion |
| Archive | 2 | zip4j + commons-compress |
| MCP data/properties | 15 | version info (real build time compiled into the class), hot-reload check, dynamic tool management (remove/refresh/reset/run), workspace set/get/reset/permission, property read/write, cleanup of data/cache/logs |
| Project scripts | 15 | build/flash/run/debug + script path setters |
| Tool list | 1 | `dlqq_get_tool_list` |

**Dynamic tool management**: `dlqq_mcp_remove_tools` (remove and persist), `dlqq_mcp_refresh_tools` (push `notifications/tools/list_changed` only), `dlqq_mcp_reset_tools` (restore everything and clear the config), `dlqq_mcp_run_tools` (call any registered tool by name, bypassing the removed/not-loaded restriction). Exclusions are stored in `mcp.tools.excluded` (comma separated) in `<workspace>/.dlqq/settings/settings.properties` and survive a server restart; these four management tools are protected and cannot be removed. In HTTP mode an immediate refresh requires the client to hold an open SSE connection.

**Workspace**: `dlqq_workspace_set` / `dlqq_workspace_get` / `dlqq_workspace_reset` / `dlqq_workspace_permission_set`, plus the `--workspace <dir>` startup argument. If neither is given, the workspace is the process CWD. **The workspace path is not persisted** (only `--workspace` or a runtime setting, so it must be set again after a restart); the write mode and all other settings (`script.*`, `response.max.size`, `mcp.tools.excluded`, ...) are persisted in `<workspace>/.dlqq/settings/settings.properties`. The write mode key is `workspace.write.mode` (`readonly`, `workspace`, `approval` for writes outside the workspace, `full`), and switching the workspace reloads it. Relative paths, script working directories and AI rule bases are resolved against the workspace; the workspace `.dlqq` directory (logs, cache, settings) moves with it.

**Script tool rules**: `dlqq_build_project` / `dlqq_rebuild_project` / `dlqq_chip_*` / `dlqq_*erase_download` / `dlqq_run_project` / `dlqq_debug_project` and friends have **no built-in default script paths**. When unconfigured they fail with "configure the tool path first" and point at the matching `dlqq_set_*_script`. Settings are written under `script.*` in `<workspace>/.dlqq/settings/settings.properties`.

## 7. AI rule files

| File | Purpose |
|------|---------|
| `.aiignore` | files/directories the AI ignores |
| `.aiwriterules` | paths the AI must not modify (write protection) |
| `.aireadrules` | paths the AI must not read (read protection) |

Syntax is Gitignore-style (`*.log`, `build/`, `/config.txt`, `!important.log`, `**/*.tmp`).
Scope: strictly enforced inside the project; outside the project a JFrame asks the user; single-file operations are not restricted.

## 8. Hot reload and dynamic tools (important)

### Summary

- The JVM cannot reload already-loaded classes: **replacing the JAR requires restarting the server process**.
- Restarting the server drops the SSE connection; the HTTP client (DSH, protocol 2025-11-25) does **not** reconnect SSE and does not re-run `initialize`/`tools/list`, so its tool list stays stale and added/removed tools remain invisible. **The client must be restarted.**
- `notifications/tools/list_changed` only works while SSE is alive: without a server restart, when the tool set changes the client re-fetches `tools/list` about 2 ms after the push (measured).

### Two diagnostic tools

| Tool | Notes |
|------|-------|
| `dlqq_get_mcp_version` | `Build time (in code, real value)` comes from `BuildInfo.BUILD_TIME` (injected at compile time by templating-maven-plugin); it does not change when the JAR is replaced without a restart. `JAR manifest build time (on disk)` reads the manifest, so it does change. A mismatch means the JAR was replaced but the process still runs old code. |
| `dlqq_test_hot_reload` | Returns a hard-coded `HOT_RELOAD_TEST_MARK` (currently v4); change it and rebuild to verify that new code is loaded. |

### SSE and restarts (mechanism)

- HTTP mode: the client opens SSE with `GET /mcp` + `Accept: text/event-stream`; `HttpTransport.sseClients` keeps the connections and `notifyToolsListChanged()` pushes to each.
- Restart = old process exits and a new one listens again: the kernel closes the old TCP connections and the list is cleared; the new process cannot inherit them (TCP connections belong to the old process socket).
- Measured with DSH: after a server restart it does not reconnect SSE or `initialize`; it only POSTs `tools/call` when needed, so there is no push channel (`notifyToolsListChanged()` returns false).

### Verification procedure

1. Build the new JAR and record `BuildInfo.BUILD_TIME`;
2. Stop the HTTP service and confirm the port is released;
3. Overwrite the JAR in the deployment directory;
4. Restart the service;
5. Call `dlqq_get_mcp_version`: in-code time == on-disk time == the new value means loaded; an old in-code value means not loaded;
6. If tools were added/removed/renamed or schemas changed, restart the client and check the tool list.

### Dynamic tool management (no server restart)

- `dlqq_mcp_remove_tools` / `dlqq_mcp_refresh_tools` / `dlqq_mcp_reset_tools` / `dlqq_mcp_run_tools`: disable/restore registered tools at runtime, persisted in `mcp.tools.excluded`; while SSE is alive the client refreshes immediately. `run_tools` can call a removed or not-yet-loaded tool by name.
- Limits: only **registered** tools can be hidden/restored; new code cannot be loaded; the four management tools are protected.

## 9. Known limitations

- **PDF/TLS/zipfs etc.**: require a **full JRE**. Trimmed jlink images fail with missing modules (the historical `Could not initialize class org.apache.pdfbox.io.IOUtils` was one of these). See "2. Runtime requirements".
- **Hardware tools** (serial/BLE/USB) without the actual hardware can only be verified at the window and management level; end-to-end traffic needs real devices.
- **PDF output**: not supported by the tools (`dlqq_convert_office_file` has no PDF target); convert to DOCX first and export with an external tool.

## 10. References

### MCP protocol

- MCP documentation: <https://modelcontextprotocol.io/>
- MCP specification (including Streamable HTTP): <https://modelcontextprotocol.io/specification/2026-07-28>

### Client integration docs

| Client | Official docs | Config file in this project |
|--------|---------------|-----------------------------|
| **DeepSeek Harness (DSH)** | see "3. MCP client configuration" above (plugin `@deepseek-ai/dsh-mcp-client`) | `C:\Users\<user>\.dsh\cordis.patch.yml` |
| Qwen Code | <https://qwenlm.github.io/qwen-code-docs/zh/users/features/mcp/> | `settings.json` |
| Claude Code | <https://code.claude.com/docs/en/mcp> | `.mcp.json` / `claude mcp add` |
| Cline | <https://docs.cline.bot/mcp/mcp-overview> | `cline_mcp_settings.json` |
| Cursor | <https://cursor.com/docs/mcp> | `.cursor\mcp.json` |
| VS Code (GitHub Copilot) | <https://code.visualstudio.com/docs/agent-customization/mcp-servers> | `.vscode\mcp.json` (`servers` + `type`) |

> All links were reachable as of 2026-10. If a client is redesigned and a link breaks, use that client's official documentation site.

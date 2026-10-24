# DongLouQiaoQue MCP Server (dlqq-ai-mcp-server)

A **Model Context Protocol (MCP) server** written in Java. It provides local tool capabilities to AI agents (DeepSeek Harness, Qwen Code, Claude Code, Cursor, Cline, VS Code, Windsurf, …): files, serial ports, BLE, USB, Wi-Fi, Office, ZIP, screenshots, image conversion, and more.

- **Transport**: stdio (default) and Streamable HTTP
- **Tools**: 166 (153 regular tools + 13 `dlqq_set_*_script` helpers)
- **Runtime**: Java 25+ (**full JRE required**, see below)
- **JAR name**: `dlqq_ai_mcp_server.jar`

> The Chinese README ([README.md](README.md)) is the reference document; this file mirrors its structure and tool detail in English.

## Features

- **Files & directories** – read/write/create/overwrite/append/clear, line-level insert/delete/replace, regex replace, copy/move/rename/delete with confirmation
- **Binary & BOM** – hex read/write, UTF-8 BOM add/remove
- **Search** – search files/directories and text inside files, partial match or regex, context lines
- **Dialogs** – text input, yes/no confirmation, file picker (Swing)
- **Serial (UART)** – graphical tool for CH340-style USB-to-serial adapters; open/close, send text/hex, read logs
- **BLE** – scan/connect/discover/read/write/subscribe via Python `bleak` and a local socket
- **USB** – HID/CDC scan, open, read/write (`usb4java`)
- **Wi-Fi** – scan/connect/disconnect/status/profiles via `netsh` (Windows) or `nmcli` (Linux)
- **Mouse / keyboard** – `java.awt.Robot`: move, click, drag, scroll, type, hotkeys
- **Screen & images** – capture the desktop (with the real cursor drawn), convert SVG/PNG/JPG/GIF/BMP
- **Office & ZIP** – Office → Markdown, Office format conversion, password-protected ZIP, multi-format unzip
- **Calculators, command execution, project scripts**
- **MCP management** – version/build time, hot-reload check, dynamic tool management, workspace root, properties, cleanup

## Requirements

### Java

- **Java 25 or newer**
- **A full JRE is required.** Trimmed `jlink` images lack `jdk.crypto.ec`, `jdk.zipfs`, `java.net.http`, etc., which breaks PDF, TLS, and zipfs use cases.
- Generate a full JRE (about 130 MB):

```cmd
"<JDK25>\bin\jlink" --module-path "<JDK25>\jmods" --add-modules ALL-MODULE-PATH --output "<target>\jre" --strip-debug --no-man-pages --no-header-files
```

`script\make-jre.bat` does the same when copied into the JDK root (next to `bin` and `jmods`).

- `WARNING: Using incubator modules: ...` is **normal** (`ALL-MODULE-PATH` includes incubator modules); the exit code is 0 and the JRE works.

### Python (BLE tool)

- **No system Python required**: the BLE tool always uses the bundled `python_env\` (interpreter, tkinter, Tcl/Tk and bleak included)
- Ship `python_env\` next to `dlqq_ai_mcp_server.jar`; if it is missing the BLE tool fails with an explicit error and never falls back to a system Python

## Build

Requirements: Java 25+ and Maven.

```cmd
REM Build only (output in target\)
script\build.bat

REM Build and copy to the release folder (recommended)
script\build_and_copy.bat
```

The artifact is `target\dlqq_ai_mcp_server.jar` (shaded fat JAR with all dependencies).

Publishing rules (`script\config.properties`):

| Mode | Condition | Output path |
|------|-----------|-------------|
| Debug | `use_debug=true` | `debug\dlqq_ai_mcp_server.jar` (flat) |
| Release | `use_debug=false` | `release\V{release_version}\jar\dlqq_ai_mcp_server.jar` |

Or call Maven directly:

```bash
mvn clean package -DskipTests -Drevision=255.0.0
```

## Run

### Start arguments

| Argument | Meaning |
|----------|---------|
| `--transport http\|stdio` | Transport, default `stdio` |
| `--port N` | HTTP port, default `49129` |
| `--path /mcp` | HTTP path, default `/mcp` |
| `--workspace <dir>` | Workspace root for all tool relative paths (highest priority) |

### stdio (default)

```cmd
java -Dfile.encoding=UTF-8 -jar dlqq_ai_mcp_server.jar
```

### Streamable HTTP

```cmd
java -Dfile.encoding=UTF-8 -jar dlqq_ai_mcp_server.jar --transport http --port 49129 --path /mcp
```

- Endpoint: `http://127.0.0.1:49129/mcp`
- Health check: `GET http://127.0.0.1:49129/mcp` → `{"status":"ok", ...}`
- If the port is busy, the console asks for another port; **Enter** retries the default port; a non-interactive console exits with code 1.
- The HTTP server is stateless and unauthenticated — clients only need the URL.

### Deployment layout

```text
dlqq_ai_mcp_server\
├── start-http.bat        # ASCII-only launcher (port 49129)
├── dlqq_ai_mcp_server.jar
└── jre\                  # full JRE produced by make-jre.bat
    └── bin\java.exe
```

### Launcher behavior (`start-http.bat`)

Double-click `start-http.bat`: it **first asks for the workspace directory** (must be an existing absolute directory; press **Enter** to use the current directory), validates it, and only then starts the HTTP server with `--workspace`. If Enter is pressed, no `--workspace` is passed and the server workspace defaults to the process CWD. After the workspace is fixed, logs, cache and settings live under `<workspace>\.dlqq`.

Full script (ASCII only, default port **49129**):

```bat
@echo off
setlocal EnableExtensions
chcp 65001 >nul

rem Deployment layout: this bat, dlqq_ai_mcp_server.jar and jre\ in the same folder.
set "APP_DIR=%~dp0"
set "JAR=%APP_DIR%dlqq_ai_mcp_server.jar"
set "JAVA=%APP_DIR%jre\bin\java.exe"
if not exist "%JAVA%" set "JAVA=java"

if not exist "%JAR%" (
    echo [ERROR] dlqq_ai_mcp_server.jar not found:
    echo         %JAR%
    echo [ERROR] Keep start-http.bat, dlqq_ai_mcp_server.jar and jre\ in the same folder.
    pause
    exit /b 1
)

:ask_workspace
set "WORKSPACE="
echo.
echo ============================================================
echo  Workspace
echo  Enter an existing absolute directory for the MCP workspace.
echo  Press ENTER to use the current directory:
echo    %CD%
echo ============================================================
set /p "WORKSPACE=Workspace path: "
if not defined WORKSPACE goto use_cwd
set "WORKSPACE=%WORKSPACE:"=%"
if not defined WORKSPACE goto use_cwd
if not exist "%WORKSPACE%\" (
    echo [ERROR] Directory does not exist: %WORKSPACE%
    echo.
    goto ask_workspace
)
set "WS_ARG=--workspace "%WORKSPACE%""
goto workspace_ready

:use_cwd
set "WORKSPACE=%CD%"
set "WS_ARG="
echo Using current directory as workspace.

:workspace_ready
echo.
echo ============================================================
echo  dlqq-ai-mcp-server  -  HTTP mode
echo  Endpoint  : http://localhost:49129/mcp
echo  Workspace : %WORKSPACE%
echo  JAR       : %JAR%
echo  Java      : %JAVA%
echo ============================================================
echo.

"%JAVA%" -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -jar "%JAR%" --transport http --port 49129 --path /mcp %WS_ARG%

set "RC=%ERRORLEVEL%"
echo.
echo Server stopped. Exit code: %RC%
pause
endlocal
```

Port handling is implemented inside Java: an occupied port asks for a new one at the console; **Enter** retries the default port; a non-interactive console exits with code 1.

## Connect an AI agent

The server is a standard MCP server (JSON-RPC 2.0) and is not tied to a client. Examples below use a Debug JAR path — replace it with your real path.

### DeepSeek Harness (DSH)

File: `C:\Users\<you>\.dsh\cordis.patch.yml`.

stdio:

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

Streamable HTTP:

```yaml
- insert:
    - id: dlqq-mcp-http
      name: "@deepseek-ai/dsh-mcp-client"
      config:
        serverName: dlqq-ai-mcp-server
        transport: streamable-http
        url: http://127.0.0.1:49129/mcp
```

Restart DSH after editing.

### Qwen Code

`settings.json` (not `mcp.json`):

```json
{
  "mcpServers": {
    "dlqq-ai-mcp-server": {
      "command": "java",
      "args": ["-Dfile.encoding=UTF-8", "-jar", "E:\\path\\to\\dlqq_ai_mcp_server.jar"],
      "env": { "JAVA_TOOL_OPTIONS": "-Dfile.encoding=UTF-8" }
    }
  }
}
```

HTTP: `"httpUrl": "http://127.0.0.1:49129/mcp"` instead of `command`/`args`.

### Claude Code

```json
{
  "mcpServers": {
    "dlqq-ai-mcp-server": {
      "command": "java",
      "args": ["-Dfile.encoding=UTF-8", "-jar", "E:\\path\\to\\dlqq_ai_mcp_server.jar"]
    }
  }
}
```

HTTP: `claude mcp add --transport http dlqq-ai-mcp-server http://127.0.0.1:49129/mcp`, or JSON with `"type": "http"` and `"url"`.

### Cursor

`.cursor\mcp.json`:

```json
{
  "mcpServers": {
    "dlqq-ai-mcp-server": {
      "command": "java",
      "args": ["-Dfile.encoding=UTF-8", "-jar", "E:\\path\\to\\dlqq_ai_mcp_server.jar"]
    }
  }
}
```

HTTP: `"url": "http://127.0.0.1:49129/mcp"`.

### VS Code (GitHub Copilot)

`.vscode\mcp.json` (uses `servers` and `type`):

```json
{
  "servers": {
    "dlqq-ai-mcp-server": {
      "type": "stdio",
      "command": "java",
      "args": ["-Dfile.encoding=UTF-8", "-jar", "E:\\path\\to\\dlqq_ai_mcp_server.jar"]
    }
  }
}
```

HTTP: `"type": "http"` + `"url": "http://127.0.0.1:49129/mcp"`.

### Cline / Roo Code

`cline_mcp_settings.json`:

```json
{
  "mcpServers": {
    "dlqq-ai-mcp-server": {
      "command": "java",
      "args": ["-Dfile.encoding=UTF-8", "-jar", "E:\\path\\to\\dlqq_ai_mcp_server.jar"]
    }
  }
}
```

HTTP: `"type": "streamableHttp"` + `"url"`.

### Windsurf

```json
{
  "mcpServers": {
    "dlqq-ai-mcp-server": {
      "command": "java",
      "args": ["-Dfile.encoding=UTF-8", "-jar", "E:\\path\\to\\dlqq_ai_mcp_server.jar"]
    }
  }
}
```

HTTP: `"serverUrl": "http://127.0.0.1:49129/mcp"`.

## Workspace

The workspace is the **base directory for all relative paths** used by tools. Default: the process working directory; set per launch with `--workspace <dir>` or change it at runtime.

**The workspace path is never persisted**: `dlqq_workspace_set` only affects the current run; after a restart, specify `--workspace` again. This avoids saving a wrong path that would break later workspace selection.

| Tool | Purpose | Parameters |
|------|---------|------------|
| `dlqq_workspace_set` | Set the workspace (existing absolute directory); effective immediately, not persisted | `path` (string, required) |
| `dlqq_workspace_get` | Show workspace, source, CLI value, write permission, permission file and process CWD | none |
| `dlqq_workspace_reset` | Fall back to `--workspace` or the process CWD; permission config untouched | none |
| `dlqq_workspace_permission_set` | Set the workspace write permission and persist it in the workspace | `mode`: `readonly` / `workspace` / `approval` / `full` |

**Write permission** is stored **inside the workspace** at `.dlqq/settings/settings.properties` (`workspace.write.mode`) and reloaded whenever the workspace changes:

| Mode | Meaning | Behavior |
|------|---------|----------|
| `readonly` | Read-only | Every file modification is rejected (create/write/append/delete/move/rename/batch replace/BOM/image output/ZIP output, …) |
| `workspace` | Workspace-only (default) | Only files inside the workspace may be modified; outside paths are rejected |
| `approval` | Outside-workspace approval | Inside the workspace: direct writes; outside: a confirmation dialog asks the user first (previous behavior) |
| `full` | Full permission | Any location may be modified; no outside-workspace confirmation dialog |

The workspace affects relative paths in file/directory/search/binary/ZIP/Office/image/screen/dialog/command tools, the script execution directory, BLE script directory, and the base of `.aiignore/.aiwriterules/.aireadrules`. The workspace `.dlqq` directory (logs, cache and settings) follows the workspace; the workspace defaults to the process CWD.

## Dynamic tool management (runtime)

| Tool | Purpose | Parameters |
|------|---------|------------|
| `dlqq_mcp_remove_tools` | Hide/disable tools; `tools/list` no longer returns them and `tools/call` reports “tool removed”; persisted | `tool_names` (array/CSV, required) |
| `dlqq_mcp_refresh_tools` | Broadcast `notifications/tools/list_changed` only | none |
| `dlqq_mcp_reset_tools` | Restore every removed tool, clear the exclusion list, broadcast a refresh | none |
| `dlqq_mcp_run_tools` | Call any registered tool by name, bypassing the “removed / not loaded by the client” restriction | `name` (string, required), `arguments` (object) |

- Removed names are stored in `<workspace>/.dlqq/settings/settings.properties` as `mcp.tools.excluded` and re-applied at startup.
- The four management tools are protected and cannot be removed.
- Over HTTP, immediate client refresh requires an open SSE stream (`GET /mcp` with `Accept: text/event-stream`); without SSE the change still applies and persists.
- **Notification policy on connect**: the server does **not** push `notifications/tools/list_changed` immediately when a client connects. Only after `initialize` / `notifications/initialized`, if the client still has not requested `tools/list` within the delay (default **10 s**, configurable with `-Dmcp.listChanged.delay.ms=5000`), the server sends the notification once as a fallback. Any client message cancels it, and after `tools/list` it is never sent. A normal client that lists tools itself never receives it.

## Updating the JAR (hot reload)

### Standard flow (restart is mandatory)

1. Build the new JAR (`script\build_and_copy.bat`).
2. **Stop the HTTP server** and verify the port is free.
3. Copy the new JAR over the deployed one.
4. Restart the server.
5. Call `dlqq_get_mcp_version`: if `Build time (in code, real value)` equals `JAR manifest build time (on disk)` and both are the new value, the new code is loaded; if the in-code time is still old, the process was not restarted.
6. If tools were added/removed/renamed or schemas changed, **restart the client** too.

### Why

- **Class loading** – the JVM loads classes at startup; replacing the JAR on disk does not reload them. `BuildInfo.BUILD_TIME` is a compile-time constant (templating-maven-plugin), so the version tool reports the code actually running.
- **SSE** – restarting the server closes old TCP connections and clears its in-memory SSE client list; a new process cannot inherit old connections.
- **Client behavior** – with DSH (protocol 2025-11-25), after a server restart the client does not reconnect SSE and does not re-run `initialize`/`tools/list`; it only posts `tools/call`. The client keeps its old tool list until restarted.

### Scope and limitations

| Capability | Supported | Notes |
|-----------|-----------|-------|
| Load new code after a restart | ✅ | Stop server → replace JAR → restart |
| Replace the JAR without restarting | ❌ | JVM cannot reload loaded classes |
| Remove/restore tools at runtime | ✅ | Persisted; removed tools callable via `dlqq_mcp_run_tools` |
| Immediate client refresh after a tool change | ⚠️ only while SSE is alive | Client re-fetches `tools/list` ~2 ms after the push (measured) |
| Client refreshes automatically after a server restart | ❌ | Restart the client |
| New tools visible without restarting the client | ❌ | Client caches the tool list at connect time |

## Parameter conventions

- Parameters marked `*` are required; the rest are optional.
- Path parameters accept absolute paths or paths relative to the workspace; all paths are resolved through the workspace.
- Line numbers start at 1; `end_line = -1` means “to end of file”.
- `mode` in search/replace tools is `partial` (substring match) or `regex`.
- `format` in serial/BLE/USB/Wi-Fi tools is `text` or `hex`.
- Charset parameters default to UTF-8.
- File-writing tools automatically create missing parent directories and respect `.aiignore/.aiwriterules/.aireadrules`; whether writes outside the workspace are allowed depends on the workspace write permission (`workspace` rejects them, `approval` asks for confirmation, `full` allows them).

## Settings properties

All settings live in `<workspace>/.dlqq/settings/settings.properties` (the workspace defaults to the process CWD when neither `--workspace` nor a runtime workspace is set):

| Key | Default | Meaning |
|-----|---------|---------|
| `response.max.size` | 25804 | Max response bytes (~25 KB, max 258048) |
| `search.max.files` | 5000 | Max files scanned by `dlqq_search_text_in_dir` |
| `search.max.file.size` | 10485760 | Max size of a file to scan |
| `search.max.results` | 50 | Default `max_results` |
| `search.text.extensions` | (list) | Default text extensions |
| `script.*` | empty | Project script paths (build/rebuild/run/debug/chip/erase/download) |
| `mcp.tools.excluded` | empty | Removed (disabled) tool names, comma-separated |

> The write permission (`workspace.write.mode`) is stored in the same workspace settings file as every other configuration key.

## HTTP protocol & SSE

- `POST /mcp` – JSON-RPC requests (`initialize`, `tools/list`, `tools/call`, …).
- `GET /mcp` – health JSON; with `Accept: text/event-stream` it opens the SSE channel used for `notifications/tools/list_changed`.
- `DELETE /mcp` – returns 200 (session teardown, stateless server).
- CORS headers are returned for browser-based clients.
- The server is stateless and unauthenticated: bind it to loopback or protect it at the network layer.

## Troubleshooting

| Symptom | Cause / fix |
|---------|-------------|
| `Could not initialize class org.apache.pdfbox.io.IOUtils` | Trimmed JRE; use the full JRE (`ALL-MODULE-PATH`) |
| Port already in use | Stop the old server, or answer the prompt with another port |
| New tools not visible | Client caches `tools/list`; restart the client (see “Updating the JAR”) |
| Removed tools still callable | By design via `dlqq_mcp_run_tools`; use it to call hidden/removed tools |
| No `notifications/tools/list_changed` | No open SSE stream; reconnect the client |
| Script tools fail with “configure the path first” | Set the corresponding `dlqq_set_*_script` |

## Behavior details

### File & directory operations

- Write operations create missing parent directories.
- Delete operations (`dlqq_delete_file`, `dlqq_delete_dir`) open a JFrame confirmation showing the target; cancel returns “operation cancelled”.
- Batch replace (`dlqq_replace_text_in_dir`) returns a per-file report and respects the AI rule files.
- Line-based edits accept `end_line = -1` (end of file); out-of-range values are clamped.
- Text operations support `charset`; the default is UTF-8. Binary reads/writes use hex text.

### Search

- `mode=partial` matches substrings; `mode=regex` uses Java regex.
- `dlqq_search_text_in_dir` skips files larger than `search.max.file.size`, scans at most `search.max.files`, returns at most `search.max.results` (overridable with `max_results`), and includes 3 context lines around each hit.
- `file_extensions` overrides `search.text.extensions`; empty means “use the configured defaults”.

### Serial (UART)

1. `dlqq_run_uart(port)` opens the GUI for one instance.
2. `dlqq_uart_get_com` / `dlqq_uart_refresh_com` list ports.
3. `dlqq_uart_open_com(port, com, baud_rate)` starts capturing.
4. `dlqq_uart_send_text` / `dlqq_uart_send_hex` write data (also recorded in the buffer).
5. `dlqq_uart_get_log*` reads with three modes: range (`start_line`/`end_line`), start+count (`start_line`/`line_count`), or wait for new data (`wait_time`/`line_count`, `wait_time` ≤ 30 s).
6. `dlqq_uart_switch_text_hex` changes the display/return format; `dlqq_exit_uart` closes the instance.

### BLE

1. `python_env` (bundled) provides the interpreter and `bleak`; `dlqq_ble_release_script` extracts `ble_tools.py` to `.dlqq/script/python`.
2. `dlqq_run_ble(port)` opens the GUI; `dlqq_ble_scan` then `dlqq_ble_get_devices`.
3. `dlqq_ble_connect(device_address)`, `dlqq_ble_discover`, then read/write/subscribe with service and characteristic UUIDs.
4. Logs are read with the same three modes as UART; `dlqq_ble_set_buffer_size` controls memory (default 5 MB).

### USB

1. `dlqq_run_usb(port)` opens the GUI; `dlqq_usb_scan` returns VID/PID/name.
2. `dlqq_usb_open(port, vid, pid, type)` opens a HID/CDC device.
3. `dlqq_usb_send_text` / `dlqq_usb_send_hex` write; `dlqq_usb_get_log*` read (text or hex).
4. `dlqq_exit_usb` closes the instance.

### Wi-Fi

1. `dlqq_run_wifi(port)` opens the GUI.
2. `dlqq_wifi_scan` / `dlqq_wifi_get_networks` list networks.
3. `dlqq_wifi_connect(ssid, password?)`, `dlqq_wifi_disconnect`, `dlqq_wifi_status`, `dlqq_wifi_profiles`.
4. `dlqq_wifi_get_log` / `dlqq_wifi_clear_log` manage the log buffer.

### Mouse & keyboard

- `dlqq_mouse_move` accepts absolute (`x`/`y`) or relative (`dx`/`dy`) movement.
- `dlqq_mouse_click` supports left/right/middle, single/double click, optional coordinates.
- `dlqq_keyboard_type` uses clipboard paste so any Unicode text works (focus must be in the target input).
- `dlqq_keyboard_hotkey` uses `+`-joined combos such as `ctrl+shift+s`.

### Screen & images

- `dlqq_capture_screen` saves to `output_path` or `.dlqq/cache`; the PNG/JPG includes the real mouse cursor.
- `dlqq_convert_image` writes to `output_path` or `.dlqq/cache`; SVG → raster is rendered by Batik, raster → SVG embeds the bitmap (not vectorized).

### Office & ZIP

- Office files are converted to Markdown/cache first; page/paragraph/sheet/slide ranges are supported.
- `dlqq_convert_office_file` targets DOCX/XLSX/PPTX/MD/TXT (no PDF output).
- `dlqq_zip` supports multiple sources, password protection and compression level 1–9; `dlqq_unzip` supports password-protected ZIP and extracts to `output_dir` or `.dlqq/cache`.

### Project scripts

- Every script tool maps to a `script.*` key; **no built-in defaults**.
- Unconfigured scripts return an error that names the matching `dlqq_set_*_script` tool.
- Scripts run with the workspace as working directory.

### MCP data & workspace

- Removed tools stay registered and can still be called through `dlqq_mcp_run_tools`.
- `dlqq_mcp_run_tools` rejects calling itself (recursion guard).
- Workspace changes affect relative paths immediately; `dlqq_workspace_set` persists, `reset` clears.
- `.dlqq` (logs, cache and settings) lives inside the workspace and follows it (workspace defaults to the process CWD).

## Tool reference

Parameter lists below: `name` = required, `[name]` = optional. Path parameters are resolved against the workspace.

### File tools (15)

| Tool | Parameters | Notes |
|------|------------|-------|
| `dlqq_read_file` | `file_path`*, `[charset]`, `[start_line]`, `[end_line]`, `[read_lines]` | Read a line range; `start_line` + `read_lines` or `start_line` + `end_line` |
| `dlqq_create_file` | `file_path`*, `[content]`, `[charset]` | Creates/overwrites; creates parent directories |
| `dlqq_overwrite_file` | `file_path`*, `[content]`, `[charset]` | Always writes, creates parent directories |
| `dlqq_clear_file` | `file_path`*, `[charset]` | Empties the file but keeps it |
| `dlqq_append_file` | `file_path`*, `content`*, `[charset]` | Creates the file if missing |
| `dlqq_remove_lines_in_file` | `file_path`*, `start_line`*, `end_line`*, `[charset]` | `end_line = -1` means end of file |
| `dlqq_replace_lines_in_file` | `file_path`*, `start_line`*, `end_line`*, `content`*, `[charset]` | Multi-line replacement |
| `dlqq_insert_text_in_file` | `file_path`*, `line`*, `content`*, `[charset]` | Inserts before the given line |
| `dlqq_replace_text_in_file` | `file_path`*, `search_text`*, `replace_text`*, `[mode]`, `[charset]`, `[start_line]`, `[end_line]` | `mode`: text / regex; shows changed lines with ±5 context |
| `dlqq_replace_text_in_dir` | `dir_path`*, `search_text`*, `replace_text`*, `[mode]`, `[charset]`, `[file_extensions]`, `[max_files]` | Batch replace with report; respects AI rules |
| `dlqq_remove_text_in_file` | `file_path`*, `search_text`*, `[mode]`, `[charset]`, `[start_line]`, `[end_line]` | Replace matched text with empty string |
| `dlqq_delete_file` | `file_path`* | Confirmation dialog |
| `dlqq_copy_file` | `source_path`*, `target_path`* | Overwrites target; creates directories |
| `dlqq_move_file` | `source_path`*, `target_path`* | Overwrites target; creates directories |
| `dlqq_rename_file` | `source_path`*, `new_name`* | Renames within the same directory |

### Directory tools (8)

| Tool | Parameters | Notes |
|------|------------|-------|
| `dlqq_mdir` | `dir_path`* | Parent must exist |
| `dlqq_mdirs` | `dir_path`* | Creates all parents |
| `dlqq_delete_dir` | `dir_path`* | Recursive, confirmation dialog |
| `dlqq_copy_dir` | `source_path`*, `target_path`* | Recursive copy |
| `dlqq_move_dir` | `source_path`*, `target_path`* | Existing target is deleted first |
| `dlqq_rename_dir` | `source_path`*, `new_name`* | Same directory |
| `dlqq_list_dir_tree` | `dir_path`*, `[show_hidden]`, `[max_depth]`, `[max_entries]` | Recursive tree |
| `dlqq_list_dir_files` | `dir_path`*, `[show_hidden]` | One level only |

### Binary & BOM tools (4)

| Tool | Parameters | Notes |
|------|------------|-------|
| `dlqq_read_bin_file` | `file_path`*, `[start_byte]`, `[end_byte]`, `[read_bytes]` | Returns hex text |
| `dlqq_write_bin_file` | `file_path`*, `hex_content`*, `[append]` | Hex text → binary; spaces/newlines allowed |
| `dlqq_add_unicode_bom` | `path`*, `[is_directory]`, `[file_extensions]` | Recursive in directory mode; skips existing BOM |
| `dlqq_remove_unicode_bom` | `path`*, `[is_directory]`, `[file_extensions]` | Removes UTF-8 BOM |

### Search tools (4)

| Tool | Parameters | Notes |
|------|------------|-------|
| `dlqq_search_file` | `search_path`*, `pattern`*, `[mode]` | `mode`: partial / regex |
| `dlqq_search_dir` | `search_path`*, `pattern`*, `[mode]` | Search directories |
| `dlqq_search_text_in_dir` | `search_path`*, `pattern`*, `[mode]`, `[ignore_case]`, `[file_extensions]`, `[max_results]` | Returns file, line number, text and ±3 context |
| `dlqq_search_text_in_file` | `file_path`*, `pattern`*, `[mode]`, `[ignore_case]`, `[max_results]` | Single file |

### Dialog tools (3)

| Tool | Parameters | Notes |
|------|------------|-------|
| `dlqq_dialog_text` | `title`*, `message`* | Text input window |
| `dlqq_dialog_check` | `title`*, `message`* | Yes/No window |
| `dlqq_dialog_file_path` | `title`*, `[extensions]` | File picker |

### Serial (UART) tools (17)

| Tool | Parameters | Notes |
|------|------------|-------|
| `dlqq_run_uart` | `port`* | Open the GUI; `port` is the local service port |
| `dlqq_uart_get_com` | `port`* | List serial ports |
| `dlqq_uart_refresh_com` | `port`* | Refresh the list |
| `dlqq_uart_open_com` | `port`*, `com`*, `[baud_rate]` | Default baud rate 1000000 |
| `dlqq_uart_reopen_com` | `port`*, `com`*, `[baud_rate]` | Change baud rate |
| `dlqq_uart_close_com` | `port`* | Close the channel |
| `dlqq_uart_send_text` | `port`*, `text`* | Also stored in the buffer |
| `dlqq_uart_send_hex` | `port`*, `hex_array`* | Each item is even-length hex |
| `dlqq_uart_get_log` | `port`*, `[start_line]`, `[end_line]`, `[line_count]`, `[wait_time]` | 3 read modes; format follows the text/hex switch |
| `dlqq_uart_get_log_text` | `port`*, `[start_line]`, `[end_line]`, `[line_count]`, `[wait_time]` | Force text |
| `dlqq_uart_get_log_hex` | `port`*, `[start_line]`, `[end_line]`, `[line_count]`, `[wait_time]` | Force hex |
| `dlqq_uart_clear_text` | `port`* | Clear buffer |
| `dlqq_uart_get_line_count` | `port`* | Buffer line count |
| `dlqq_uart_set_text_size` | `port`*, `size_bytes`* | Buffer size, default 5 MB |
| `dlqq_uart_switch_text_hex` | `port`*, `format`* | text / hex |
| `dlqq_exit_uart` | `port`* | Close window and process |
| `dlqq_rerun_uart` | `port`* | Restart the tool |

### BLE script tools

| Tool | Parameters | Notes |
|------|------------|-------|
| `dlqq_ble_release_script` | none | Release `ble_tools.py` into `.dlqq/script/python` |

### BLE tools (20)

| Tool | Parameters | Notes |
|------|------------|-------|
| `dlqq_run_ble` | `port`* | Open the BLE GUI |
| `dlqq_ble_scan` | `port`*, `[timeout]` | Scan nearby devices |
| `dlqq_ble_get_devices` | `port`* | Devices from the last scan |
| `dlqq_ble_connect` | `port`*, `device_address`* | Connect |
| `dlqq_ble_disconnect` | `port`* | Disconnect |
| `dlqq_ble_discover` | `port`* | Discover services/characteristics |
| `dlqq_ble_read` | `port`*, `service_uuid`*, `char_uuid`* | Read characteristic |
| `dlqq_ble_write_text` | `port`*, `service_uuid`*, `char_uuid`*, `text`* | Write text |
| `dlqq_ble_write_hex` | `port`*, `service_uuid`*, `char_uuid`*, `hex_array`* | Write hex |
| `dlqq_ble_subscribe` | `port`*, `service_uuid`*, `char_uuid`*, `enable`* | Subscribe/unsubscribe notifications |
| `dlqq_ble_get_log` | `port`*, `[start_line]`, `[end_line]`, `[line_count]`, `[wait_time]` | 3 read modes |
| `dlqq_ble_get_log_text` | `port`*, same as above | Force text |
| `dlqq_ble_get_log_hex` | `port`*, same as above | Force hex |
| `dlqq_ble_clear` | `port`* | Clear buffer |
| `dlqq_ble_get_line_count` | `port`* | Buffer line count |
| `dlqq_ble_set_buffer_size` | `port`*, `size_bytes`* | Default 5 MB |
| `dlqq_ble_switch_text_hex` | `port`*, `format`* | text / hex |
| `dlqq_exit_ble` | `port`* | Close window and process |
| `dlqq_rerun_ble` | `port`* | Restart the tool |
| `dlqq_check_bluetooth_open` | none | Check the Windows Bluetooth adapter |

### USB tools (15)

| Tool | Parameters | Notes |
|------|------------|-------|
| `dlqq_run_usb` | `port`* | Open the USB GUI |
| `dlqq_usb_scan` | `port`* | Scan HID/CDC devices |
| `dlqq_usb_get_devices` | `port`* | Last scan results |
| `dlqq_usb_open` | `port`*, `vid`*, `pid`*, `[type]` | Open a device |
| `dlqq_usb_close` | `port`* | Close |
| `dlqq_usb_send_text` | `port`*, `text`* | UTF-8 text |
| `dlqq_usb_send_hex` | `port`*, `hex_array`* | Hex data |
| `dlqq_usb_get_log` | `port`*, `[start_line]`, `[end_line]` | Format follows the switch |
| `dlqq_usb_get_log_text` | `port`*, `[start_line]`, `[line_count]` | Force text |
| `dlqq_usb_get_log_hex` | `port`*, `[start_line]`, `[line_count]` | Force hex |
| `dlqq_usb_clear` | `port`* | Clear buffer |
| `dlqq_usb_get_line_count` | `port`* | Buffer line count |
| `dlqq_usb_switch_text_hex` | `port`* | text / hex |
| `dlqq_exit_usb` | `port`* | Close window and process |
| `dlqq_rerun_usb` | `port`* | Restart the tool |

### Wi-Fi tools (12)

| Tool | Parameters | Notes |
|------|------------|-------|
| `dlqq_run_wifi` | `port`* | Open the Wi-Fi GUI |
| `dlqq_wifi_scan` | `port`*, `[timeout]` | Scan networks |
| `dlqq_wifi_get_networks` | `port`* | Last scan results |
| `dlqq_wifi_connect` | `port`*, `ssid`*, `[password]`, `[timeout]` | WPA/WPA2-PSK or open |
| `dlqq_wifi_disconnect` | `port`* | Disconnect |
| `dlqq_wifi_status` | `port`* | Connection state/SSID/signal |
| `dlqq_wifi_profiles` | `port`* | Saved profiles |
| `dlqq_wifi_get_log` | `port`*, `[start_line]`, `[end_line]` | Log buffer |
| `dlqq_wifi_clear_log` | `port`* | Clear log |
| `dlqq_wifi_get_line_count` | `port`* | Log line count |
| `dlqq_exit_wifi` | `port`* | Close window |
| `dlqq_rerun_wifi` | `port`* | Restart the tool |

### Mouse tools (7)

| Tool | Parameters | Notes |
|------|------------|-------|
| `dlqq_mouse_move` | `[x]`, `[y]`, `[dx]`, `[dy]` | Absolute or relative |
| `dlqq_mouse_click` | `[button]`, `[x]`, `[y]`, `[clicks]`, `[interval]` | left/right/middle |
| `dlqq_mouse_drag` | `start_x`*, `end_x`*, `end_y`*, `[start_y]`, `[button]`, `[duration]`, `[steps]` | Press → move → release |
| `dlqq_mouse_scroll` | `[amount]`, `[direction]` | Positive scrolls up |
| `dlqq_mouse_get_position` | none | Cursor position + screen size |
| `dlqq_mouse_press` | `[button]`, `[x]`, `[y]` | Press and hold |
| `dlqq_mouse_release` | `[button]` | Release |

### Keyboard tools (6)

| Tool | Parameters | Notes |
|------|------------|-------|
| `dlqq_keyboard_type` | `text`*, `[interval]` | Types any text via clipboard paste |
| `dlqq_keyboard_tap` | `key`*, `[times]`, `[interval]` | Press + release |
| `dlqq_keyboard_press` | `key`* | Hold |
| `dlqq_keyboard_release` | `key`* | Release |
| `dlqq_keyboard_hotkey` | `combo`*, `[interval]` | e.g. `ctrl+c` |
| `dlqq_keyboard_key_list` | none | Supported key names |

### Screen & image tools (2)

| Tool | Parameters | Notes |
|------|------------|-------|
| `dlqq_capture_screen` | `[x]`, `[y]`, `[width]`, `[height]`, `[format]`, `[output_path]` | Full screen or region; draws the real cursor |
| `dlqq_convert_image` | `source_path`*, `target_format`*, `[output_path]`, `[quality]` | SVG/PNG/JPG/GIF/BMP; raster→SVG embeds the bitmap |

### Time & calculator tools (4)

| Tool | Parameters | Notes |
|------|------------|-------|
| `dlqq_get_current_time` | `[timezone]`, `[format]` | Date/time, timestamp, weekday, zone |
| `dlqq_calculator_math` | `expression`* | Precise math: powers, logs, trig, factorial |
| `dlqq_calculator_programmer` | `expression`*, `[type]` | Bitwise, shifts, base conversion |
| `dlqq_calculator_function` | `expression`*, `[variables]` | Custom functions with variables |

### Command tool (1)

| Tool | Parameters | Notes |
|------|------------|-------|
| `dlqq_run_command` | `command`*, `[working_dir]`, `[timeout]` | JFrame confirmation before execution; `cmd /c` or `sh -c` |

### Office tools (2)

| Tool | Parameters | Notes |
|------|------------|-------|
| `dlqq_read_office_file` | `file_path`*, `[start_page]`, `[end_page]` | PDF/DOC/DOCX/XLS/XLSX/PPT/PPTX → Markdown in `.dlqq/cache` |
| `dlqq_convert_office_file` | `source_path`*, `target_format`*, `[output_path]`, `[start_page]`, `[end_page]` | Target: DOCX/XLSX/PPTX/MD/TXT; default output `.dlqq/cache` |

### ZIP tools (2)

| Tool | Parameters | Notes |
|------|------------|-------|
| `dlqq_zip` | `source_paths`*, `[output_path]`, `[password]`, `[compression_level]`, `[format]` | Level 1–9; default output `.dlqq/cache` |
| `dlqq_unzip` | `zip_path`*, `[output_dir]`, `[password]` | Password-protected ZIP supported |

### MCP data, properties & workspace tools (15)

| Tool | Parameters | Notes |
|------|------------|-------|
| `dlqq_get_mcp_version` | none | Version, compiled build time, manifest time, runtime info, workspace |
| `dlqq_test_hot_reload` | none | Returns the hard-coded hot-reload marker |
| `dlqq_mcp_remove_tools` | `tool_names`* | Remove and persist |
| `dlqq_mcp_refresh_tools` | none | Push `tools/list_changed` only |
| `dlqq_mcp_reset_tools` | none | Restore all removed tools |
| `dlqq_mcp_run_tools` | `name`*, `[arguments]` | Direct call, bypasses the removed/not-loaded restriction |
| `dlqq_workspace_set` | `path`* | Set and persist the workspace |
| `dlqq_workspace_get` | none | Show workspace and source |
| `dlqq_workspace_reset` | none | Clear persisted workspace |
| `dlqq_workspace_permission_set` | `mode`* | Set workspace write permission (readonly/workspace/approval/full), persisted in the workspace |
| `dlqq_clear_mcp_data` | none | Delete `.dlqq` |
| `dlqq_clear_mcp_cache` | none | Delete `.dlqq/cache` |
| `dlqq_clear_mcp_log` | none | Delete `.dlqq/log` files |
| `dlqq_get_mcp_properties` | `[key]` | Read settings |
| `dlqq_set_mcp_properties` | `[key]`, `[value]` | Write settings (e.g. `response.max.size`) |
| `dlqq_reset_mcp_properties` | `[key]` | Reset to defaults |

### Project script tools (13)

Build/run/debug scripts are configured via `settings.properties` (`script.*`). **No built-in default paths**: when unset they return an error telling you to configure the path first.

| Tool | Script key |
|------|------------|
| `dlqq_build_project` | `script.build` |
| `dlqq_rebuild_project` | `script.rebuild` |
| `dlqq_build_debug_project` | `script.build.debug` |
| `dlqq_rebuild_debug_project` | `script.rebuild.debug` |
| `dlqq_build_erase_download` | `script.build.erase.download` |
| `dlqq_rebuild_erase_download` | `script.rebuild.erase.download` |
| `dlqq_build_run_project` | `script.build.run` |
| `dlqq_rebuild_run_project` | `script.rebuild.run` |
| `dlqq_chip_download` | `script.chip.download` |
| `dlqq_chip_erase` | `script.chip.erase` |
| `dlqq_erase_download` | `script.erase.download` |
| `dlqq_run_project` | `script.run` |
| `dlqq_debug_project` | `script.debug` |

### Script path setters (13)

Each takes `script_path*` and writes the corresponding `script.*` property: `dlqq_set_build_script`, `dlqq_set_rebuild_script`, `dlqq_set_debug_script`, `dlqq_set_run_script`, `dlqq_set_chip_download_script`, `dlqq_set_chip_erase_script`, `dlqq_set_build_debug_script`, `dlqq_set_rebuild_debug_script`, `dlqq_set_build_erase_download_script`, `dlqq_set_rebuild_erase_download_script`, `dlqq_set_build_run_script`, `dlqq_set_rebuild_run_script`, `dlqq_set_erase_download_script`.

### Tool list (1)

| Tool | Parameters | Notes |
|------|------------|-------|
| `dlqq_get_tool_list` | `[filter]` | Names and descriptions of registered tools |

## Usage examples

Read a file:

```json
{ "name": "dlqq_read_file", "arguments": { "file_path": "src/App.java", "start_line": 1, "read_lines": 50 } }
```

Search text with regex:

```json
{ "name": "dlqq_search_text_in_dir", "arguments": { "search_path": "src", "pattern": "TODO|FIXME", "mode": "regex", "ignore_case": true } }
```

Switch the workspace at runtime:

```json
{ "name": "dlqq_workspace_set", "arguments": { "path": "E:\\work\\my-project" } }
```

Call a removed tool:

```json
{ "name": "dlqq_mcp_run_tools", "arguments": { "name": "dlqq_capture_screen", "arguments": { "format": "png" } } }
```

## Tech stack

- Java 25, Maven, shaded JAR
- MCP JSON-RPC 2.0 over stdio and Streamable HTTP (`com.sun.net.httpserver`)
- Apache PDFBox / POI, Batik, zip4j, commons-compress, usb4java, JNA, Gson, SLF4J
- Python 3.10+ (build time only) to generate `python_env`; runtime uses the bundled `python_env\`

## Project structure

```text
src/main/java/com/donglouqiaoque/dlqqaimcpserver/
├── mcp/
│   ├── MCPServerMain.java        # entry point (logging/UTF-8 init, args, health check)
│   ├── protocol/                 # JsonRpcMessage, McpSchema
│   ├── server/McpServer.java     # dispatch + tool registry + dynamic tools
│   ├── transport/                # McpTransport, StdioTransport, HttpTransport (SSE)
│   └── tools/                    # AITools + all tool implementations
├── cli/                          # legacy CLI parsing
├── config/ConfigManager.java     # legacy configuration
└── utils/                        # FileUtils, AIRulesUtils, CalculatorUtils, Workspace
src/main/resources/
├── py/ble_tools.py               # BLE GUI (Python + bleak)
├── image/logo.ico                # window icon (ICO)
├── image/logo.png                # window icon (PNG)
└── simplelogger.properties
```

Runtime directories (created on first start, git-ignored): `.dlqq/log/{logger,stderr}`, `.dlqq/cache`; settings live in `<workspace>/.dlqq/settings/settings.properties` (workspace defaults to the process CWD).

## AI rule files

| File | Purpose |
|------|---------|
| `.aiignore` | Files/directories the AI should ignore |
| `.aiwriterules` | Files/directories the AI must not modify |
| `.aireadrules` | Files/directories the AI must not read |

Gitignore-style syntax (`*.log`, `build/`, `/config.txt`, `!important.log`, `**/*.tmp`). Rules are resolved against the workspace; outside the workspace a JFrame asks for confirmation; single-file operations are not restricted.

## BOM tools in detail

- `dlqq_add_unicode_bom` — adds a UTF-8 BOM (`EF BB BF`) to files; skips files that already have one; creates the file if missing. In directory mode it recurses and filters by `file_extensions`.
- `dlqq_remove_unicode_bom` — removes the BOM when present; directory mode is recursive.
- Both respect `.aiignore/.aiwriterules/.aireadrules`.

## Known limitations

- **Full JRE required** for PDF/TLS/zipfs; trimmed `jlink` images can fail with `Could not initialize class org.apache.pdfbox.io.IOUtils`.
- **No PDF writing** – `dlqq_convert_office_file` does not target PDF; convert to DOCX and export externally.
- **Font cache** – PDFBox writes `~/.pdfbox.cache`; if unwritable it is rebuilt at each start (1–3 s). Override with `-Dpdfbox.fontcache=<writable dir>`.
- **Hardware tools** (serial/BLE/USB) need real hardware for end-to-end verification.
- **GUI tools** (serial, BLE, Wi-Fi, command confirmation) need a desktop session; unavailable headless.
- **Hot reload** – the JVM cannot reload classes; a JAR update needs a server restart, and tool changes also need a client restart (see “Updating the JAR”).

## License

Apache License 2.0.


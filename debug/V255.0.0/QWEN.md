## Qwen Added Memories
- 当前项目为Java语言开发，Maven构建。
- 该项目为适用于QWEN的MCP服务器。
- **运行环境要求**：Java 17+ 和 Python 3.8+，runtime目录提供安装包
- **MCP配置位置**：配置放在settings.json（而非mcp.json），发行固件包含示例settings.json供参考
- 允许你执行脚本，但脚本必须都放在该目录下：工程目录\ai_res\doc\script
- 工程打包的jar文件要放在该目录下：工程目录\release\版本号
- 每次修改完代码，都要构建一遍jar包，替换当前release目录下的文件。
- QWEN的MCP服务器配置教程网页链接：https://qwenlm.github.io/qwen-code-docs/zh/users/features/mcp/
- 增减修改MCP服务器工具以后，README.md也要更新：工程目录\README.md
- 增减修改MCP服务器工具以后，QWEN.md也要更新：工程目录\release/debug\V1.x.x/V255.x.x\QWEN.md
- MCP服务器连接诊断日志：已在 StdioTransport、McpServer、MCPServerMain 中增加详细的连接诊断日志，帮助分析连接失败原因。日志文件位于 `./.dlqq/log/logger/` 和 `./.dlqq/log/stderr/` 目录下，按时间命名。

## 运行环境要求

### Java运行环境
- **需要 Java 17 或更高版本**
- 如果未安装Java，可在项目 `runtime/` 目录中找到Java 17安装包

### Python运行环境（用于串口/BLE工具）
- **需要 Python 3.8 或更高版本**
- 如果未安装Python，可在项目 `runtime/` 目录中找到Python 3.13安装包（向下兼容3.8+）

## MCP配置说明

MCP服务器配置需要放在 **settings.json** 文件中（而非 mcp.json）。

**发行固件包含示例 settings.json**，请参考该文件进行配置。

## 自动构建脚本

项目提供以下自动构建脚本，位于 `util/build/` 目录：

### 配置文件 config.properties

构建脚本会从 `util/build/config.properties` 读取版本号，自动确定发布目录。

**配置文件示例：**
```properties
# 版本号配置
release_version=1.0.0
debug_version=255.0.0

# 打包模式配置
# true  = 打包 debug 版本 (输出到 debug/V255.0.x)
# false = 打包 release 版本 (输出到 release/V1.0.x)
use_debug=true
```

### 1. build.bat - 普通构建
仅执行 Maven 构建，生成 JAR 文件到 `target/` 目录。

**使用方法：**
```cmd
util\build\build.bat
```

### 2. build_and_copy.bat - 构建并复制
执行 Maven 构建，并将生成的 JAR 文件复制到发布目录（release 或 debug）。

**使用方法：**
```cmd
util\build\build_and_copy.bat
```

**构建流程：**
1. 从 `config.properties` 读取配置
   - `use_debug=true`: 使用 `debug_version`，输出到 `debug/V{debug_version}/`
   - `use_debug=false`: 使用 `release_version`，输出到 `release/V{release_version}/`
2. 执行 `mvn clean package -DskipTests -Drevision={version}`
3. 创建发布目录
4. 将生成的 JAR 复制到对应目录

**示例：**
```properties
# debug 版本配置
release_version=1.0.0
debug_version=255.0.0
use_debug=true
# 输出: debug/V255.0.0/donglouqiaoque_ai_tools.jar

# release 版本配置
release_version=1.0.0
debug_version=255.0.0
use_debug=false
# 输出: release/V1.0.0/donglouqiaoque_ai_tools.jar
```

> **提示：** 修改代码后，建议直接使用 `build_and_copy.bat` 进行构建。需要修改版本号时，只需编辑 `config.properties`。

## MCP工具列表

### 数据管理工具

| 工具名称 | 描述 |
|---------|------|
| `dlqq_clear_data` | 清除.dlqq数据目录（包括日志、Python脚本等） |
| `dlqq_clear_cache` | 清除日志缓存文件（仅删除.dlqq/log下的日志） |

**说明**：
- `dlqq_clear_data`：删除整个 .dlqq 目录及其所有内容。即使部分文件被占用（如日志文件正在被MCP服务器写入），也会返回成功结果，并告知哪些文件被占用。这是正常现象，被占用的文件将在MCP服务器重启后可以被清除。
- `dlqq_clear_cache`：仅清除 .dlqq/log 目录下的日志文件，保留目录结构。即使部分文件被占用也会返回成功结果，并告知哪些文件被占用。

### Office文件工具

| 工具名称 | 描述 |
|---------|------|
| `dlqq_read_office_file` | 读取Office文件（PDF/DOC/DOCX/XLS/XLSX/PPT/PPTX）并转换为Markdown，支持分页/分段处理 |
| `dlqq_convert_office_file` | 将Office文件从一种格式转换为另一种格式（支持 MD/TXT/DOCX/XLSX/PPTX） |

**说明**：
- 支持的源格式：PDF、DOC、DOCX、XLS、XLSX、PPT、PPTX、MD、TXT
- 支持的目标格式：DOCX、XLSX、PPTX、MD、TXT（PDF暂不支持写入）
- 转换后的文件默认保存到 `.dlqq/cache` 目录
- 未指定输出路径时自动生成带时间戳的文件名
- **分页/分段参数** `start_page` / `end_page`（可选，适用于读取和转换工具）：
  - PDF：按页码（从1开始）
  - Word(DOC/DOCX)：按段落序号
  - Excel(XLS/XLSX)：按Sheet索引（第几个工作表）
  - PPT：按幻灯片页码
  - 不指定则处理全部内容，最大限制100页/段/Sheet/幻灯片
- **PDF特殊限制**：
  - 处理超时：90秒（复杂图形/表格的PDF可能超时）

### 压缩/解压工具

| 工具名称 | 描述 |
|---------|------|
| `dlqq_zip` | 压缩文件/目录为ZIP（支持密码保护和压缩级别设置） |
| `dlqq_unzip` | 解压缩多种格式（ZIP/7Z/TAR/GZ/BZ2/XZ/RAR/CPIO/AR等，ZIP支持密码） |

**说明**：
- 压缩目前支持 ZIP 格式（AES-256 加密，压缩级别 1-9，默认5）
- **解压支持多种格式**：zip、7z、tar、tar.gz、tgz、tar.bz2、tar.xz、gz、bz2、xz、rar、jar、war、cpio、ar
- 密码仅适用于ZIP格式（7z/rar加密暂不支持）
- 默认输出路径自动生成

### AI规则文件支持

MCP服务器支持三种AI规则文件，用于控制AI对项目文件的操作权限：

| 文件名 | 作用 |
|--------|------|
| `.aiignore` | 指定AI应该忽略的文件/目录（不参与批量操作） |
| `.aiwriterules` | 指定禁止AI修改的文件/目录（写保护） |
| `.aireadrules` | 指定禁止AI读取的文件/目录（读保护） |

**规则语法**（Gitignore风格）：
- `*.log` - 忽略所有.log文件
- `build/` - 忽略特定目录
- `/config.txt` - 锚定到根目录
- `!important.log` - 取反规则
- `**/*.tmp` - 匹配任意目录层级

**规则生效范围**：
- 当前项目内：AI严格遵守规则文件
- 非当前项目：通过JFrame弹窗由用户确认
- 单文件操作：不需要遵守规则

### BOM控制工具

| 工具名称 | 描述 |
|---------|------|
| `dlqq_add_unicode_bom` | 为文件或目录添加UTF-8 BOM（遵循AI规则） |
| `dlqq_remove_unicode_bom` | 从文件或目录移除UTF-8 BOM（遵循AI规则） |

**说明**：
- 支持单文件或整个目录处理
- 目录处理时遵循 `.aiignore` 规则
- 文件读写操作保持BOM状态一致性

### 计算器工具

| 工具名称 | 描述 |
|---------|------|
| `dlqq_calculator_math` | 数学计算器 - 精确计算支持指数、对数、三角函数等 |
| `dlqq_calculator_programmer` | 程序计算器 - 整型/浮点计算，支持位运算(AND OR XOR 位移) |
| `dlqq_calculator_function` | 函数计算器 - 自定义函数表达式，支持变量和内置函数计算 |

**数学计算器支持的函数**：
- `sqrt(x)` - 平方根, `cbrt(x)` - 立方根, `pow(base, exp)` - 幂运算
- `log(x)` / `ln(x)` - 自然对数, `log10(x)` - 常用对数, `log2(x)` - 二进制对数
- `exp(x)` - e的x次幂
- `sin(x)`, `cos(x)`, `tan(x)` - 三角函数（弧度）
- `asin(x)`, `acos(x)`, `atan(x)` - 反三角函数
- `sinh(x)`, `cosh(x)`, `tanh(x)` - 双曲函数
- `abs(x)`, `floor(x)`, `ceil(x)`, `round(x)`
- `n!` - 阶乘
- 常量: `PI`, `E`

**程序计算器支持的操作**：
- 四则运算: `+`, `-`, `*`, `/`, `%`
- 位运算: `&` (AND), `|` (OR), `^` (XOR), `~` (NOT), `<<`, `>>`, `>>>` (位移)
- 多进制表示: `0x` (十六进制), `0b` (二进制), `0o` (八进制)
- 数据类型: `int`, `long`, `float`, `double`

**函数计算器**：
- 支持自定义变量
- 支持嵌套函数调用
- 支持所有数学计算器的内置函数

### 命令执行工具

| 工具名称 | 描述 |
|---------|------|
| `dlqq_run_command` | 执行命令行指令（**需要用户审批**） |

**说明**：
- **执行前必须通过 JFrame 弹窗获得用户确认**
- 支持设置工作目录
- 支持设置超时时间（1-600秒，默认60秒）
- Windows 使用 `cmd /c`，Linux/Mac 使用 `sh -c`
- 返回命令的输出内容和退出码

### USB工具

USB通信工具，支持HID和CDC设备的扫描、连接和数据收发。

| 工具名称 | 描述 |
|---------|------|
| `dlqq_run_usb` | 启动USB工具 |
| `dlqq_usb_scan` | 扫描USB HID/CDC设备 |
| `dlqq_usb_get_devices` | 获取已扫描的设备列表 |
| `dlqq_usb_open` | 打开USB设备（通过VID/PID） |
| `dlqq_usb_close` | 关闭USB设备 |
| `dlqq_usb_send_text` | 发送文本数据 |
| `dlqq_usb_send_hex` | 发送十六进制数据 |
| `dlqq_usb_get_log` | 获取USB通信日志 |
| `dlqq_exit_usb` | 退出USB工具 |

**说明**：
- 支持HID和CDC两种设备类型
- 基于usb4java库实现跨平台USB通信
- 使用VID/PID标识设备
- 支持文本/HEX两种数据显示格式

### WiFi工具

WiFi工具（Java原生实现），支持扫描周围WiFi网络、查看连接状态、连接/断开WiFi。

| 工具名称 | 描述 |
|---------|------|
| `dlqq_run_wifi` | 启动WiFi工具（打开图形界面窗口） |
| `dlqq_wifi_scan` | 扫描周围的WiFi网络 |
| `dlqq_wifi_get_networks` | 获取最近一次扫描的网络列表 |
| `dlqq_wifi_status` | 获取当前WiFi连接状态 |
| `dlqq_wifi_profiles` | 列出系统已保存的WiFi配置文件 |
| `dlqq_wifi_connect` | 连接指定WiFi（SSID+密码，支持WPA/WPA2-PSK和开放网络） |
| `dlqq_wifi_disconnect` | 断开当前WiFi连接 |
| `dlqq_wifi_get_log` | 获取WiFi工具日志 |
| `dlqq_wifi_clear_log` | 清除WiFi工具日志 |
| `dlqq_wifi_get_line_count` | 获取WiFi工具日志行数 |
| `dlqq_exit_wifi` | 关闭WiFi工具 |
| `dlqq_rerun_wifi` | 重新启动WiFi工具 |

**说明**：
- Java原生实现（Swing GUI + 系统命令），不依赖Python
- Windows使用 `netsh wlan`，Linux使用 `nmcli`
- 需要管理员权限执行部分操作（如添加配置文件）

### 鼠标工具

鼠标工具（Java原生实现，基于 java.awt.Robot），模拟鼠标点击、移动、滚动。

| 工具名称 | 描述 |
|---------|------|
| `dlqq_mouse_get_position` | 获取鼠标当前位置坐标 |
| `dlqq_mouse_move` | 移动鼠标（支持绝对坐标x/y和相对位移dx/dy） |
| `dlqq_mouse_click` | 模拟鼠标点击（左/右/中键，单击/双击，可指定坐标） |
| `dlqq_mouse_press` | 按下鼠标按钮（不释放） |
| `dlqq_mouse_release` | 释放鼠标按钮 |
| `dlqq_mouse_scroll` | 滚动鼠标滚轮 |
| `dlqq_mouse_drag` | 模拟鼠标拖拽（按下→移动→释放） |

### 键盘工具

键盘工具（Java原生实现，基于 java.awt.Robot），模拟键盘输入、按键、组合键。

| 工具名称 | 描述 |
|---------|------|
| `dlqq_keyboard_type` | 输入文本（支持中文等任意字符，通过剪贴板粘贴） |
| `dlqq_keyboard_tap` | 敲击一次按键（字母/数字/功能键/方向键/特殊键） |
| `dlqq_keyboard_press` | 按下按键（不释放） |
| `dlqq_keyboard_release` | 释放按键 |
| `dlqq_keyboard_hotkey` | 执行组合键（如 ctrl+c、ctrl+alt+delete、alt+tab） |
| `dlqq_keyboard_key_list` | 列出支持的按键名称列表 |

**说明**：
- 使用前请确保目标窗口获得焦点
- 组合键格式：用+连接按键名，如 `ctrl+c`、`ctrl+shift+esc`
- 鼠标/键盘模拟为系统级操作，请谨慎使用

### 时间工具

| 工具名称 | 描述 |
|---------|------|
| `dlqq_get_current_time` | 获取当前日期时间（支持指定时区和格式，返回时间戳、星期、时区等） |

### 项目脚本工具

脚本工具执行 `.dlqq/settings/settings.properties` 中配置的批处理脚本。首次使用前需通过 `dlqq_set_*_script` 工具设置脚本路径。

#### 执行类工具

| 工具名称 | 描述 |
|---------|------|
| `dlqq_build_project` | 构建项目 |
| `dlqq_rebuild_project` | 重建项目 |
| `dlqq_chip_erase` | 擦除芯片 |
| `dlqq_chip_download` | 下载固件到芯片 |
| `dlqq_erase_download` | 擦除并下载 |
| `dlqq_build_erase_download` | 构建、擦除并下载 |
| `dlqq_rebuild_erase_download` | 重建、擦除并下载 |
| `dlqq_run_project` | 运行项目 |
| `dlqq_debug_project` | 调试项目 |
| `dlqq_build_run_project` | 构建并运行 |
| `dlqq_build_debug_project` | 构建并调试 |
| `dlqq_rebuild_run_project` | 重建并运行 |
| `dlqq_rebuild_debug_project` | 重建并调试 |

#### 设置类工具

| 工具名称 | 描述 |
|---------|------|
| `dlqq_set_build_script` | 设置构建脚本路径 |
| `dlqq_set_rebuild_script` | 设置重建脚本路径 |
| `dlqq_set_chip_erase_script` | 设置芯片擦除脚本路径 |
| `dlqq_set_chip_download_script` | 设置芯片下载脚本路径 |
| `dlqq_set_erase_download_script` | 设置擦除下载脚本路径 |
| `dlqq_set_build_erase_download_script` | 设置构建擦除下载脚本路径 |
| `dlqq_set_rebuild_erase_download_script` | 设置重建擦除下载脚本路径 |
| `dlqq_set_run_script` | 设置运行脚本路径 |
| `dlqq_set_debug_script` | 设置调试脚本路径 |
| `dlqq_set_build_run_script` | 设置构建运行脚本路径 |
| `dlqq_set_build_debug_script` | 设置构建调试脚本路径 |
| `dlqq_set_rebuild_run_script` | 设置重建运行脚本路径 |
| `dlqq_set_rebuild_debug_script` | 设置重建调试脚本路径 |
| `dlqq_set_mcp_properties` | 设置MCP配置属性（含response.max.size，默认25804字节，最大258048） |

**说明**：
- 所有脚本路径默认空，首次执行会提示需要先使用 `dlqq_set_*_script` 工具设置
- 脚本配置保存在 `.dlqq/settings/settings.properties` 文件中
- `response.max.size` 控制文件操作返回内容的最大字节数（默认25804约25KB，最大258048约252KB）

# 鸫楼桥鹊 MCP Server (dlqq-ai-mcp-server)

一个 Java 编写的 **MCP 服务器**（Model Context Protocol），为各类 AI Agent 工具（DeepSeek Harness、Qwen Code、Claude Code、Cursor、Cline 等）提供本地工具能力。

## 功能特性

- **文件/目录操作** - 读写、创建、删除、复制、移动、重命名，支持行级编辑与正则替换
- **二进制文件** - 读取二进制文件为十六进制，将十六进制写入为二进制
- **搜索工具** - 在目录下搜索文件或文件夹，支持部分匹配和正则
- **对话框交互** - 文本输入、确认、文件选择等GUI对话框
- **串口通信** - CH340等USB转串口设备的图形化通信工具
- **BLE蓝牙** - 蓝牙低功耗设备的扫描、连接、数据读写工具（Python bleak + Socket）
- **USB通信** - HID/CDC 设备的扫描、连接与收发（usb4java）
- **WiFi工具** - 扫描、连接、断开 WiFi（netsh / nmcli）
- **键鼠/截图** - `java.awt.Robot` 模拟鼠标键盘、截取桌面图像（截图会绘制真实鼠标光标）
- **图片转换** - SVG/PNG/JPG/GIF/BMP 互转（SVG 由 Apache Batik 渲染）
- **Office/压缩** - Office 转 Markdown、格式转换；ZIP(带密码) 与多格式解压
- **计算器/命令/脚本** - 数学与程序计算、人工确认的命令执行、项目脚本编排
- **MCP服务器** - 完整支持MCP协议，可作为各类 AI Agent 的本地工具服务器；支持 stdio 与 Streamable HTTP 两种传输

## 工具列表

本MCP服务器提供 **164** 个工具（151 个常规工具 + 13 个 `dlqq_set_*_script` 脚本路径设置工具）。
> 说明：分组标题中的序号为历史遗留编号，实际工具数以各分组表格为准。

产物 JAR 名为 **`dlqq_ai_mcp_server.jar`**（早期版本叫 `dlqq_ai_tools.jar`）。

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
| `dlqq_replace_text_in_dir` | 在目录下所有文件中批量替换文本（支持正则，遵循AI规则） |
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
| `dlqq_get_mcp_version` | 获取MCP服务器版本信息、**真实构建时间（编译进代码）**、JAR 信息与运行时信息 |
| `dlqq_test_hot_reload` | 热加载测试：返回代码中写死的标记字符串，验证替换 JAR 后是否真的加载了新代码 |
| `dlqq_mcp_remove_tools` | 动态移除（禁用）指定工具，立即隐藏并持久化到 `mcp.tools.excluded`，下次启动仍保持移除 |
| `dlqq_mcp_refresh_tools` | 仅发送 `notifications/tools/list_changed`，让客户端重新拉取工具列表（不改工具集） |
| `dlqq_mcp_reset_tools` | 恢复所有被移除的工具、清空 `mcp.tools.excluded`，并发送刷新通知 |
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
| `dlqq_zip` | 压缩文件或目录为ZIP格式，支持多文件压缩、密码保护、压缩级别设置（1-9），默认输出到 `.dlqq/cache` |
| `dlqq_unzip` | 解压多种压缩格式（ZIP/7Z/TAR/GZ/BZ2/XZ/RAR/CPIO/AR等），ZIP支持密码保护，默认解压到 `.dlqq/cache` |

**说明**：
- 压缩格式：仅支持 ZIP（AES-256 加密，压缩级别 1-9，默认 5）
- **解压格式**：zip、7z、tar、tar.gz、tgz、tar.bz2、tar.xz、gz、bz2、xz、rar、jar、war、cpio、ar
- 密码仅适用于ZIP格式（7z/rar加密暂不支持）
- 多文件压缩：`source_paths` 参数支持多个文件/目录路径
- **默认输出目录**：未指定 `output_path` / `output_dir` 时，统一保存到 `.dlqq/cache`

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

### USB工具 (89-102)

USB通信工具，支持HID和CDC设备的扫描、连接和数据收发。基于usb4java库实现跨平台USB通信。

| 工具名称 | 描述 |
|---------|------|
| `dlqq_run_usb` | 启动USB工具（打开图形界面） |
| `dlqq_usb_scan` | 扫描可用的USB HID/CDC设备 |
| `dlqq_usb_get_devices` | 获取已扫描的USB设备列表 |
| `dlqq_usb_open` | 打开指定的USB设备（通过VID/PID） |
| `dlqq_usb_close` | 关闭当前打开的USB设备 |
| `dlqq_usb_send_text` | 向USB设备发送文本数据 |
| `dlqq_usb_send_hex` | 向USB设备发送十六进制数据 |
| `dlqq_usb_get_log` | 获取USB通信日志 |
| `dlqq_usb_get_log_text` | 获取USB通信日志（强制文本格式） |
| `dlqq_usb_get_log_hex` | 获取USB通信日志（强制HEX格式） |
| `dlqq_usb_clear` | 清除USB数据缓冲区 |
| `dlqq_usb_get_line_count` | 获取USB缓冲区当前的日志行数 |
| `dlqq_usb_switch_text_hex` | 切换USB日志的显示格式（文本/HEX） |
| `dlqq_exit_usb` | 退出USB工具（关闭窗口及进程） |
| `dlqq_rerun_usb` | 重新启动USB工具（先关闭再启动） |

**支持的设备类型**：
- **HID设备** - USB人体学接口设备，如键盘、鼠标、自定义HID设备
- **CDC设备** - USB通信设备类，如虚拟串口设备

**示例**：
```json
// 启动USB工具
{ "port": 9001 }

// 扫描设备
{ "port": 9001 }

// 打开设备（VID=0x0483, PID=0x5740）
{
  "port": 9001,
  "vid": "0x0483",
  "pid": "0x5740",
  "type": "HID"
}

// 发送十六进制数据
{
  "port": 9001,
  "hex_array": ["FF", "01", "02", "03"]
}

// 发送文本数据
{
  "port": 9001,
  "text": "Hello USB"
}
```

### WiFi工具 (103-114)

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
- 部分操作（如添加配置文件）需要管理员权限

### 鼠标工具 (115-121)

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

### 键盘工具 (122-127)

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

### 时间工具 (128)

| 工具名称 | 描述 |
|---------|------|
| `dlqq_get_current_time` | 获取当前日期时间（支持指定时区和格式，返回时间戳、星期、时区等） |

### 项目脚本工具 (129-142)

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
| `dlqq_set_erase_download_script` | 设置擦除下载组合脚本路径（script.erase.download） |
| `dlqq_set_build_erase_download_script` | 设置构建擦除下载组合脚本路径（script.build.erase.download） |
| `dlqq_set_rebuild_erase_download_script` | 设置重建擦除下载组合脚本路径（script.rebuild.erase.download） |
| `dlqq_set_run_script` | 设置运行脚本路径（script.run） |
| `dlqq_set_debug_script` | 设置调试脚本路径（script.debug） |
| `dlqq_set_build_run_script` | 设置构建运行组合脚本路径（script.build.run） |
| `dlqq_set_build_debug_script` | 设置构建调试组合脚本路径（script.build.debug） |
| `dlqq_set_rebuild_run_script` | 设置重建运行组合脚本路径（script.rebuild.run） |
| `dlqq_set_rebuild_debug_script` | 设置重建调试组合脚本路径（script.rebuild.debug） |
| `dlqq_set_mcp_properties` | 设置MCP配置属性（含response.max.size，默认25804字节，最大258048） |

**说明**：
- **不使用任何内置默认脚本**：所有脚本路径默认为空，未配置时工具会直接返回错误“未配置「xxx」脚本路径，需要先配置工具路径”，并给出对应的 `dlqq_set_*_script` 工具名
- 执行类工具（`dlqq_build_project`、`dlqq_rebuild_project`、`dlqq_chip_erase`、`dlqq_chip_download`、`dlqq_erase_download`、`dlqq_build_erase_download`、`dlqq_rebuild_erase_download`、`dlqq_run_project`、`dlqq_debug_project`、`dlqq_build_run_project` 等）都遵循此规则
- 首次使用前先调用对应的 `dlqq_set_*_script`，或用 `dlqq_set_mcp_properties` 直接写 `script.build` 等配置项
- 脚本配置保存在 `.dlqq/settings/settings.properties` 文件中
- `response.max.size` 控制文件操作返回内容的最大字节数（默认25804约25KB，最大258048约252KB）

### 屏幕截图工具

屏幕截图工具（Java原生实现，基于 `java.awt.Robot`），把桌面或指定区域保存为图片。

| 工具名称 | 描述 |
|---------|------|
| `dlqq_capture_screen` | 截取整个桌面或指定区域，保存为 PNG/JPG 并返回文件路径和尺寸 |

**参数**：
- `output_path` (string, 可选): 输出文件路径（默认 `.dlqq/cache/screenshot_时间戳.png`）
- `x` / `y` / `width` / `height` (integer, 可选): 截取区域，默认整个屏幕

**说明**：
- **截图包含鼠标光标**：通过 JNA 调用 Windows `GetCursorInfo` / `GetIconInfo` / `GetDIBits` 获取真实光标位图（含热点、alpha 通道、单色光标），按 alpha 混合绘制到截图中；非 Windows 平台或光标被隐藏时自动跳过
- 返回结果中包含「鼠标光标: 已绘制到截图中 / 未绘制」状态
- 默认保存到 `.dlqq/cache` 目录下带时间戳的文件

### 图片工具

| 工具名称 | 描述 |
|---------|------|
| `dlqq_convert_image` | 图片格式转换，支持 SVG/PNG/JPG/GIF/BMP 互转 |

**参数**：
- `source_path` (string, 必需): 源图片路径
- `target_format` (string, 必需): 目标格式 `svg` / `png` / `jpg` / `gif` / `bmp`
- `output_path` (string, 可选): 输出路径（默认 `.dlqq/cache` 下带时间戳的文件）
- `quality` (integer, 可选): JPG 质量 1-100，默认 90

**说明**：
- **SVG → 光栅**（png/jpg/gif/bmp）：通过 Apache Batik 渲染后写出
- **光栅 → 光栅**：ImageIO 直接转换；JPG 自动白底填充（不支持透明）
- **光栅 → SVG**：以 data URI 嵌入位图生成 SVG，**非矢量化**，仅便于统一成 SVG 容器

### 工具列表查询工具

| 工具名称 | 描述 |
|---------|------|
| `dlqq_get_tool_list` | 获取服务器上所有已注册工具的名称和描述，支持关键字过滤 |

**参数**：
- `filter` (string, 可选): 名称过滤关键字，如 `wifi`、`keyboard`、`set_build`；为空返回全部工具

**说明**：
- 用于让 AI 在不重启会话的情况下自查询当前可用的工具清单
- 返回内容来自运行时注册表，与实际 `tools/list` 一致

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
- **必须使用完整版 JRE**：精简的 jlink 运行时只含部分模块，缺少 `jdk.crypto.ec`、`jdk.zipfs`、`java.net.http` 等，可能在 PDF、TLS、zip 文件系统等场景报错
- 用 JDK 生成完整 JRE（`ALL-MODULE-PATH` 会把 JDK 的全部模块打进镜像，约 130 MB）：

```cmd
"<JDK17>\bin\jlink" --module-path "<JDK17>\jmods" --add-modules ALL-MODULE-PATH --output "<目标目录>\jre" --strip-debug --no-man-pages --no-header-files
```

- 也可以把同一条命令存成 `.bat` 放到 JDK 根目录（与 `bin`、`jmods` 同级），用 `%~dp0` 自动定位：

```cmd
"%~dp0bin\jlink" --module-path "%~dp0jmods" --add-modules ALL-MODULE-PATH --output "%~dp0jre" --strip-debug --no-man-pages --no-header-files
pause
```

- 生成后把 `<目标目录>\jre\bin\java` 作为客户端的 `command` 即可（详见下文各客户端配置）
- 也可以直接用完整 JDK 运行，无需单独生成 JRE：`"<JDK17>\bin\java" -Dfile.encoding=UTF-8 -jar dlqq_ai_mcp_server.jar`
- **执行时出现警告属正常现象**：`WARNING: Using incubator modules: jdk.incubator.vector, jdk.incubator.foreign`（JDK 24 只有 `jdk.incubator.vector`）只是提示 `ALL-MODULE-PATH` 把 JDK 自带的孵化器模块也打包进了镜像，这些 API 尚属实验性；jlink 退出码仍为 0，生成结果可正常使用，**忽略即可**。

#### Python运行环境（用于串口/BLE工具）

- **需要 Python 3.8 或更高版本**（BLE 工具依赖 `bleak` 库）
- 当前仓库未附带 Python 安装包，请自行安装

### 1. 编译与打包

需要 Java 17+ 与 Maven。

```cmd
REM 只构建，产物在 target\ 下
script\build.bat

REM 构建并复制到发布目录（推荐）
script\build_and_copy.bat
```

构建产物为 `target\dlqq_ai_mcp_server.jar`（shade fat jar，已内嵌全部依赖）。

发布目录规则（由 `script\config.properties` 中的 `use_debug` 决定）：

| 模式 | 条件 | 目标路径 |
|------|------|----------|
| Debug | `use_debug=true` | `debug\dlqq_ai_mcp_server.jar`（不区分版本号，直接平铺） |
| Release | `use_debug=false` | `release\V{release_version}\jar\dlqq_ai_mcp_server.jar` |

也可以直接调用 Maven：

```bash
mvn clean package -DskipTests -Drevision=255.0.0
```

### 2. 接入 AI Agent 客户端

本服务是标准 **MCP Server**（JSON-RPC 2.0），支持 **stdio**（默认）与 **Streamable HTTP** 两种传输，不绑定某个客户端，任何支持 MCP 的 Agent 工具都能接入。下面按常用客户端给出配置，**DeepSeek Harness 放在第一位**；①~⑦ 为 stdio 配置，⑧ 为各客户端的 HTTP 配置。

> 示例中的 JAR 路径请替换为本机真实路径：Debug 包是 `debug\dlqq_ai_mcp_server.jar`，Release 包是 `release\V1.0.0\jar\dlqq_ai_mcp_server.jar`。

#### ① DeepSeek Harness (DSH)

DSH 在用户目录的 `cordis.patch.yml`（Windows：`C:\Users\<用户名>\.dsh\cordis.patch.yml`）里注册 MCP 客户端插件，追加一个 `- insert:` 条目即可：

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

说明：
- `id` 在文件内唯一即可；`serverName` 就是会话中显示的服务器名。
- `command: java` 要求 java 在 PATH 中；也可以写绝对路径并加引号，例如 `command: 'D:\Soft\Java\jdk-17\bin\java.exe'`。
- 保存后重启 DeepSeek Harness，新会话即可通过 `tools/list` 看到全部 `dlqq_*` 工具。

#### ② Qwen Code

Qwen Code 的 MCP 配置放在 **settings.json**（不是 mcp.json）：

```json
{
  "mcpServers": {
    "dlqq-ai-mcp-server": {
      "command": "java",
      "args": [
        "-Dfile.encoding=UTF-8",
        "-jar",
        "E:\\Code\\AIProject\\dlqq-ai-mcp-server\\debug\\dlqq_ai_mcp_server.jar"
      ],
      "env": {
        "JAVA_TOOL_OPTIONS": "-Dfile.encoding=UTF-8"
      },
      "timeout": 60000
    }
  }
}
```

配置教程：<https://qwenlm.github.io/qwen-code-docs/zh/users/features/mcp/>

#### ③ Claude Code / Claude Desktop

Claude Code 项目级配置 `.mcp.json`：

```json
{
  "mcpServers": {
    "dlqq-ai-mcp-server": {
      "type": "stdio",
      "command": "java",
      "args": ["-Dfile.encoding=UTF-8", "-jar", "E:\\Code\\AIProject\\dlqq-ai-mcp-server\\debug\\dlqq_ai_mcp_server.jar"]
    }
  }
}
```

也可以用命令行添加：

```bash
claude mcp add dlqq-ai-mcp-server -- java -Dfile.encoding=UTF-8 -jar E:\Code\AIProject\dlqq-ai-mcp-server\debug\dlqq_ai_mcp_server.jar
```

Claude Desktop 使用 `claude_desktop_config.json`（Windows：`%APPDATA%\Claude\claude_desktop_config.json`），结构与上面相同（`type` 可省略）。

#### ④ Cursor

全局配置 `%USERPROFILE%\.cursor\mcp.json`，或项目级 `<项目>\.cursor\mcp.json`：

```json
{
  "mcpServers": {
    "dlqq-ai-mcp-server": {
      "command": "java",
      "args": ["-Dfile.encoding=UTF-8", "-jar", "E:\\Code\\AIProject\\dlqq-ai-mcp-server\\debug\\dlqq_ai_mcp_server.jar"]
    }
  }
}
```

#### ⑤ VS Code（GitHub Copilot Chat）

`.vscode/mcp.json`，注意顶层键是 **`servers`**（不是 `mcpServers`），且需要 `type`：

```json
{
  "servers": {
    "dlqq-ai-mcp-server": {
      "type": "stdio",
      "command": "java",
      "args": ["-Dfile.encoding=UTF-8", "-jar", "E:\\Code\\AIProject\\dlqq-ai-mcp-server\\debug\\dlqq_ai_mcp_server.jar"]
    }
  }
}
```

#### ⑥ Cline / Roo Code（VS Code 扩展）

`cline_mcp_settings.json`（也可以用扩展里的 MCP Servers 面板添加）：

```json
{
  "mcpServers": {
    "dlqq-ai-mcp-server": {
      "command": "java",
      "args": ["-Dfile.encoding=UTF-8", "-jar", "E:\\Code\\AIProject\\dlqq-ai-mcp-server\\debug\\dlqq_ai_mcp_server.jar"],
      "env": { "JAVA_TOOL_OPTIONS": "-Dfile.encoding=UTF-8" },
      "disabled": false,
      "autoApprove": []
    }
  }
}
```

#### ⑦ Windsurf

`%USERPROFILE%\.codeium\windsurf\mcp_config.json`：

```json
{
  "mcpServers": {
    "dlqq-ai-mcp-server": {
      "command": "java",
      "args": ["-Dfile.encoding=UTF-8", "-jar", "E:\\Code\\AIProject\\dlqq-ai-mcp-server\\debug\\dlqq_ai_mcp_server.jar"]
    }
  }
}
```

#### ⑧ 用 HTTP（Streamable HTTP）方式接入

先以 HTTP 模式启动服务（一次启动，可被多个客户端共用）：

```bash
java -jar dlqq_ai_mcp_server.jar --transport http --port 49129 --path /mcp
```

端点：`http://127.0.0.1:49129/mcp`（端口/路径按启动参数调整；服务端无鉴权、无会话状态，各客户端直接填 URL 即可）。

**DeepSeek Harness (DSH)** — `~/.dsh/cordis.patch.yml`（`transport: streamable-http` + `url`）：

```yaml
- insert:
    - id: dlqq-mcp-http
      name: "@deepseek-ai/dsh-mcp-client"
      config:
        serverName: dlqq-ai-mcp-server
        transport: streamable-http
        url: http://127.0.0.1:49129/mcp
```

**Qwen Code** — `settings.json`（Streamable HTTP 用 `httpUrl`；旧版 SSE 用 `url`）：

```json
{
  "mcpServers": {
    "dlqq-ai-mcp-server": {
      "httpUrl": "http://127.0.0.1:49129/mcp"
    }
  }
}
```

**Claude Code** — 命令行：

```bash
claude mcp add --transport http dlqq-ai-mcp-server http://127.0.0.1:49129/mcp
```

或写进 `.mcp.json`：

```json
{
  "mcpServers": {
    "dlqq-ai-mcp-server": {
      "type": "http",
      "url": "http://127.0.0.1:49129/mcp"
    }
  }
}
```

**Cursor** — `.cursor/mcp.json`：

```json
{
  "mcpServers": {
    "dlqq-ai-mcp-server": {
      "url": "http://127.0.0.1:49129/mcp"
    }
  }
}
```

**VS Code (GitHub Copilot)** — `.vscode/mcp.json`（注意是 `servers` + `type`）：

```json
{
  "servers": {
    "dlqq-ai-mcp-server": {
      "type": "http",
      "url": "http://127.0.0.1:49129/mcp"
    }
  }
}
```

**Cline / Roo Code** — `cline_mcp_settings.json`：

```json
{
  "mcpServers": {
    "dlqq-ai-mcp-server": {
      "type": "streamableHttp",
      "url": "http://127.0.0.1:49129/mcp"
    }
  }
}
```

**Windsurf** — `mcp_config.json`（远程服务器用 `serverUrl`）：

```json
{
  "mcpServers": {
    "dlqq-ai-mcp-server": {
      "serverUrl": "http://127.0.0.1:49129/mcp"
    }
  }
}
```

#### 通用提示

- ①~⑦ 为 **stdio** 方式（客户端拉起进程，不占端口、不需要网络）；⑧ 为 **HTTP** 方式（先手动起服务，多客户端可共用）。
- 编码：务必带 `-Dfile.encoding=UTF-8`（或设 `JAVA_TOOL_OPTIONS`），否则中文路径/内容可能乱码。
- 启动耗时约 1~2 秒，客户端 `timeout` 建议 ≥ 30 秒。
- 验证是否打通：让 Agent 调用 `dlqq_get_mcp_version`（返回版本与构建时间），或 `dlqq_get_tool_list`（列出全部工具）。

### 3. 传输方式（STDIO / HTTP）

服务器支持两种传输方式，通过启动参数切换（各客户端的 HTTP 配置示例见上一节“⑧ 用 HTTP（Streamable HTTP）方式接入”）：

#### STDIO（默认）

```bash
java -jar dlqq_ai_mcp_server.jar
```

客户端以子进程方式启动服务器，通过标准输入/输出通信，适用于上面所有 Agent 客户端。

#### HTTP（Streamable HTTP）

```bash
java -jar dlqq_ai_mcp_server.jar --transport http --port 49129 --path /mcp
```

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `--transport http` | 启用 HTTP 传输（`stdio` 为默认） | stdio |
| `--port N` | HTTP 监听端口 | 49129 |
| `--path /mcp` | MCP 端点路径 | /mcp |

**协议说明**：
- 客户端 `POST http://localhost:49129/mcp` 发送 JSON-RPC 请求，返回 JSON 响应
- 通知（无 `id` 的请求）返回 `202 Accepted`
- `GET /mcp` 返回服务健康状态（可用于探活）
- `DELETE /mcp` 用于终止会话
- 支持 CORS（`Access-Control-Allow-Origin: *`），可直接用浏览器/curl 调试

**验证**：

```bash
# 健康检查
curl http://localhost:49129/mcp

# initialize
curl -X POST http://localhost:49129/mcp -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}'
```

#### 部署与启动脚本（start-http.bat）

发布时把 `start-http.bat`、`dlqq_ai_mcp_server.jar`、`jre\` 放在**同一目录**，双击 `start-http.bat` 即可启动 HTTP 服务：

```text
dlqq_ai_mcp_server\
├── start-http.bat
├── dlqq_ai_mcp_server.jar
└── jre\
    ├── bin\java.exe
    ├── lib\
    └── ...
```

`script\start-http.bat` 内容（默认端口 **49129**，ASCII 编写，无中文）：

```bat
@echo off
setlocal
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

echo ============================================================
echo  dlqq-ai-mcp-server  -  HTTP mode
echo  Endpoint : http://localhost:49129/mcp
echo  JAR      : %JAR%
echo  Java     : %JAVA%
echo ============================================================
echo.

"%JAVA%" -Dfile.encoding=UTF-8 -jar "%JAR%" --transport http --port 49129 --path /mcp

set "RC=%ERRORLEVEL%"
echo.
echo Server stopped. Exit code: %RC%
pause
endlocal
```

**端口占用处理**（Java 内实现，启动后在控制台交互）：

| 情况 | 行为 |
|------|------|
| 49129 空闲 | 直接监听，控制台输出 `[HTTP] MCP server is running: http://localhost:49129/mcp` |
| 49129 被占用 | 提示 `[HTTP] Port 49129 is already in use.`，要求输入新端口（1-65535） |
| 输入的新端口也被占用 | 提示 `Port X is still in use`，继续要求重新输入 |
| **直接回车** | 重新尝试默认端口 49129；若仍被占用则继续提示 |
| 无交互控制台（被服务/计划任务拉起） | 打印提示后以退出码 1 结束，可用 `--port` 或环境方式重新指定 |

端口参数：`--port` 默认 **49129**（与脚本一致）；如需换端口，改脚本里的 `--port 49129`，或在启动参数中指定 `--port N`。

> `jre\` 可用 `script\make-jre.bat` 生成：把该脚本复制到 JDK 根目录（与 `bin`、`jmods` 同级）双击运行，会输出完整版 `<JDK>\jre`（`ALL-MODULE-PATH`，约 130 MB），再复制到部署目录即可。

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

获取MCP服务器版本信息、**真实构建时间**（编译期写入 class 文件的常量）、JAR 信息与运行时信息。

**参数**：无

**示例**：
```json
{}
```

**返回**：
- `构建时间（代码内，真实值）`：编译进代码的构建时间，**不受替换 JAR 影响**，反映当前进程实际运行的代码版本
- `JAR清单构建时间（磁盘）`：磁盘上 JAR 包清单里的构建时间（替换 JAR 后会变化）
- 版本号、JAR 文件路径/大小/修改时间、Java 运行时信息

> 判断热加载：两者一致说明进程运行的正是磁盘上的 jar；代码内时间更旧则说明 jar 已被替换但未重启，新代码未生效。

#### dlqq_test_hot_reload（热加载测试）

返回 `McpTools.HOT_RELOAD_TEST_MARK` 这个编译期写死的字符串（当前值 `hot-reload-test-v4`）。

**用途**：修改该字符串 → 重新构建 → 覆盖部署目录的 jar；**不重启**再调用仍返回旧值，**重启后**返回新值，可直观验证“是否真的加载了新代码”。

**参数**：无

**示例**：
```json
{}
```

**返回**：`热加载测试标记: hot-reload-test-v4` 及说明文字

#### dlqq_mcp_remove_tools / dlqq_mcp_refresh_tools / dlqq_mcp_reset_tools（动态工具管理）

这三个工具用于**运行时动态修改工具列表**，不需要重启服务端。

| 工具 | 作用 | 参数 |
|------|------|------|
| `dlqq_mcp_remove_tools` | 移除（禁用）指定工具：`tools/list` 不再返回、`tools/call` 直接报“工具已被移除”；同时写入 `settings.properties` 的 `mcp.tools.excluded` | `tool_names`：数组（如 `["dlqq_capture_screen"]`）或逗号分隔字符串 |
| `dlqq_mcp_refresh_tools` | 仅推送 `notifications/tools/list_changed`，客户端重新拉取列表 | 无 |
| `dlqq_mcp_reset_tools` | 恢复全部被移除工具、清空 `mcp.tools.excluded`，并推送刷新通知 | 无 |

**持久化**：被移除的工具记录在 `.dlqq/settings/settings.properties` 的 `mcp.tools.excluded`（逗号分隔），服务端下次启动时会自动应用，保持“已移除”状态。

**保护规则**：`dlqq_mcp_remove_tools` / `dlqq_mcp_refresh_tools` / `dlqq_mcp_reset_tools` 三个管理工具不允许被移除，避免无法恢复。

**刷新通道**：HTTP 模式下需要客户端已打开 SSE 长连接（`GET /mcp`，`Accept: text/event-stream`）才能立即刷新；没有 SSE 连接时移除仍然生效并持久化，客户端下次重连后生效。stdio 模式直接写 stdout。

**实测**：移除 2 个工具后 `tools/list` 从 164 降到 162、SSE 客户端收到 `notifications/tools/list_changed`；重启服务端后仍为 162（日志：`被移除工具（持久化）: 1 个`）；`reset` 后恢复 164 并清空配置。

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
{ "key": "script.build", "value": "script\\build.bat" }

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

- **Java 17+**（必需，须使用完整版 JRE，见“运行环境要求”）
- **Python 3.8+**（串口/BLE 工具必需，需自行安装）
- MCP协议（Model Context Protocol）
- Maven构建工具
- 依赖：
  - jSerialComm（UART串口通信）
  - usb4java（USB HID/CDC 通信）
  - Python bleak库（BLE蓝牙通信，经 `ble_tools.py` + Socket）
  - Apache POI 5.2.5（Office文件处理：DOC/DOCX/XLS/XLSX/PPT/PPTX）
  - Apache PDFBox 3.0.1（PDF文件处理）
  - Apache Batik 1.17（SVG 渲染，图片格式转换）
  - zip4j（ZIP压缩/AES加密）
  - Apache Commons Compress 1.25 + XZ（7z/tar/gz/bz2/xz/rar/cpio/ar 解压）
  - Gson（JSON 序列化）
  - SLF4J Simple（日志）

## 项目结构

```
src/main/java/com/donglouqiaoque/dlqqaimcpserver/
├── mcp/
│   ├── MCPServerMain.java         # MCP服务器入口（日志/UTF-8初始化、健康检查）
│   ├── protocol/
│   │   ├── JsonRpcMessage.java    # JSON-RPC消息
│   │   └── McpSchema.java         # MCP协议定义
│   ├── server/
│   │   └── McpServer.java         # MCP服务器核心（分发 + 工具注册表）
│   ├── tools/
│   │   ├── AITools.java           # 工具注册中心
│   │   ├── FileTools.java         # 文件/目录操作工具
│   │   ├── DialogTools.java       # GUI对话框工具
│   │   ├── UartTools.java         # 串口工具
│   │   ├── BleTools.java          # BLE蓝牙工具（Python桥）
│   │   ├── BleManager.java        # BLE进程/脚本管理
│   │   ├── UsbTools.java          # USB工具
│   │   ├── WifiTools.java         # WiFi工具
│   │   ├── MouseTools.java        # 鼠标模拟
│   │   ├── KeyboardTools.java     # 键盘模拟
│   │   ├── ScreenTools.java       # 屏幕截图（含鼠标光标绘制）
│   │   ├── ImageTools.java        # 图片格式转换（SVG/PNG/JPG/GIF/BMP）
│   │   ├── TimeTools.java         # 时间工具
│   │   ├── OfficeTools.java       # Office读取/转换
│   │   ├── ZipTools.java          # 压缩/解压
│   │   ├── CalculatorTools.java   # 计算器
│   │   ├── RunCommandTools.java   # 命令执行（人工确认）
│   │   ├── AboutPanel.java        # GUI 关于/作者信息栏（共用）
│   │   ├── LogoLoader.java        # 窗口图标加载（logo.png）
│   │   ├── WindowsNativeUtils.java# Windows 原生能力（光标位图等，JNA）
│   │   └── McpTools.java          # MCP数据/属性/版本/项目脚本
│   └── transport/
│       ├── McpTransport.java      # 传输层接口
│       ├── StdioTransport.java    # stdio传输实现
│       └── HttpTransport.java     # Streamable HTTP 传输实现
├── cli/                           # 命令行解析（历史保留）
├── config/
│   └── ConfigManager.java         # 旧配置管理器
└── utils/
    ├── FileUtils.java             # 路径/文件工具
    ├── AIRulesUtils.java          # .aiignore/.aiwriterules/.aireadrules
    └── CalculatorUtils.java       # 计算内核
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

## 热加载（更新 JAR）说明

### 相关工具

| 工具 | 作用 |
|------|------|
| `dlqq_get_mcp_version` | 显示 `构建时间（代码内，真实值）`（编译进 class 的常量）和 `JAR清单构建时间（磁盘）`（磁盘 JAR 清单），用于判断新 JAR 是否已加载 |
| `dlqq_test_hot_reload` | 返回代码中写死的标记字符串（当前 `hot-reload-test-v4`），改标记后重新构建即可验证代码是否更新 |
| `dlqq_mcp_remove_tools` / `dlqq_mcp_refresh_tools` / `dlqq_mcp_reset_tools` | 运行时不重启服务端地移除/刷新/恢复工具列表（见“动态工具管理”），用于验证客户端实时刷新能力 |

### 标准更新流程（换 JAR 必须重启）

1. 构建新 JAR（`script\build_and_copy.bat`），记录 `BuildInfo.BUILD_TIME`；
2. **停掉旧 HTTP 服务**，确认端口释放（`netstat` 无 LISTENING）；
3. 复制新 JAR 覆盖部署目录；
4. 重启 HTTP 服务（`start-http.bat`）；
5. 调 `dlqq_get_mcp_version` 查看：
   - `构建时间（代码内，真实值）` 与 `JAR清单构建时间（磁盘）` 一致、且为新的构建时间 → 新代码已加载；
   - 代码内时间仍是旧的、磁盘清单已是新的 → JAR 换了但进程没重启（未生效）；
6. 新增/删除/修改了工具时，还需**重启客户端（DSH）**，原因见下。

### 原理

- **类加载**：JVM 启动时加载 JAR 中的类，运行期间不会因为磁盘上的 JAR 被替换而重新加载；因此换 JAR 必须重启进程。`dlqq_get_mcp_version` 的“代码内真实值”来自编译期常量，替换 JAR 不会变，能可靠反映“当前进程跑的代码版本”；“磁盘清单”只反映磁盘文件。
- **SSE 长连接**：HTTP 模式下客户端通过 `GET /mcp` + `Accept: text/event-stream` 建立 SSE 长连接，服务端记录连接并在其上推送 `notifications/tools/list_changed`。重启服务端会关闭旧进程的 TCP 连接、清空连接列表；新进程无法继承旧连接。
- **客户端行为**（DSH，协议 2025-11-25，实测）：服务端重启后 DSH 不会重连 SSE、也不会重新 `initialize`/`tools/list`，只在调用工具时发 POST `tools/call`。因此重启后服务端没有可用的推送通道，客户端工具列表仍是旧的。

### 范围与限制

| 能力 | 是否支持 | 说明 |
|------|---------|------|
| 重启服务端后加载新代码 | ✅ | 必须停服务 → 换 JAR → 重启 |
| 不重启进程替换 JAR | ❌ | JVM 不支持运行中重载已加载的类 |
| 不重启服务端动态移除/恢复工具 | ✅ | `dlqq_mcp_remove_tools` / `dlqq_mcp_reset_tools`，状态持久化 |
| 工具列表变化时客户端实时刷新 | ⚠️ 仅 SSE 存活时 | 推送后约 2ms 客户端重新 `tools/list`（实测） |
| 重启服务端后客户端自动刷新工具列表 | ❌ | DSH 不重连 SSE、不重新初始化，必须重启 DSH |
| 不重启客户端看到新增工具 | ❌ | 客户端工具列表是连接时缓存的 |

### 为什么“服务端主动通知”救不了换 JAR

通知只能沿**已建立的 SSE 连接**推送；换 JAR 需要重启进程，SSE 必然断开，重启后客户端又不重建连接，所以没有接收方。通知机制适用于“服务端不重启、工具集变化”的场景（如动态工具管理），不能替代“换代码必须重启”的约束。

> 结论：换 JAR 的标准操作是 **停服务 → 覆盖 JAR → 重启服务 → 重启客户端**；只改工具内部实现（名称/参数/描述不变）时，客户端可不重启。

## 已知限制

- **PDF 等模块相关功能**：请使用**完整版 JRE**。精简 jlink 运行时缺少 `jdk.crypto.ec`、`jdk.zipfs`、`java.net.http` 等模块，可能在 PDF（PDFBox 字体/IOUtils）、TLS、zip 文件系统等场景报错；历史报错 `Could not initialize class org.apache.pdfbox.io.IOUtils` 即与运行环境相关。完整 JRE 的生成命令见“运行环境要求”。
- **PDF 写入**：工具侧不提供（`dlqq_convert_office_file` 的目标格式不含 PDF），请先转 DOCX 再用外部工具导出。
- **字体缓存**：PDFBox 会尝试写 `~/.pdfbox.cache`，写不进去只会在每次启动时重建缓存（多花 1~3 秒），不影响功能；可用 `-Dpdfbox.fontcache=<可写目录>` 指定。
- **硬件相关工具**：串口 / BLE / USB 在没有对应硬件时只能验证窗口与管理类接口，收发的端到端链路需要实机验证。
- **GUI 工具**：串口、BLE、WiFi、命令确认等依赖桌面环境，纯服务器（无显示）环境下不可用。

## 许可证

本项目采用 Apache License 2.0 许可证

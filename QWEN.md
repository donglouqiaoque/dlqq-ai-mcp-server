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
| `dlqq_read_office_file` | 读取Office文件（PDF/DOC/DOCX/XLS/XLSX/PPT/PPTX）并转换为Markdown |
| `dlqq_convert_office_file` | 将Office文件从一种格式转换为另一种格式（支持 MD/TXT/DOCX/XLSX/PPTX） |

**说明**：
- 支持的源格式：PDF、DOC、DOCX、XLS、XLSX、PPT、PPTX、MD、TXT
- 支持的目标格式：DOCX、XLSX、PPTX、MD、TXT（PDF暂不支持写入）
- 转换后的文件默认保存到 `.dlqq/cache` 目录
- 未指定输出路径时自动生成带时间戳的文件名

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
| `dlqq_set_response_max_size` | 设置最大回复文本大小（默认32KB） |

**说明**：
- 所有脚本路径默认空，首次执行会提示需要先使用 `dlqq_set_*_script` 工具设置
- 脚本配置保存在 `.dlqq/settings/settings.properties` 文件中
- `response.max.size` 控制文件操作返回内容的最大字节数（默认32768）

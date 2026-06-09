## Qwen Added Memories
- 当前项目为Java语言开发，Maven构建。
- 该项目为适用于QWEN的MCP服务器。
- 允许你执行脚本，但脚本必须都放在该目录下：工程目录\ai_res\doc\script
- 工程打包的jar文件要放在该目录下：工程目录\release\版本号
- 每次修改完代码，都要构建一遍jar包，替换当前release目录下的文件。
- QWEN的MCP服务器配置教程网页链接：https://qwenlm.github.io/qwen-code-docs/zh/users/features/mcp/
- 增减修改MCP服务器工具以后，README.md也要更新：工程目录\README.md
- 增减修改MCP服务器工具以后，QWEN.md也要更新：工程目录\release/debug\V1.x.x/V255.x.x\QWEN.md

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

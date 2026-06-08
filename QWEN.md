## Qwen Added Memories
- 当前项目为Java语言开发，Maven构建。
- 该项目为适用于QWEN的MCP服务器。
- 允许你执行脚本，但脚本必须都放在该目录下：工程目录\ai_res\doc\script
- 工程打包的jar文件要放在该目录下：工程目录\release\版本号
- 每次修改完代码，都要构建一遍jar包，替换当前release目录下的文件。
- QWEN的MCP服务器配置教程网页链接：https://qwenlm.github.io/qwen-code-docs/zh/users/features/mcp/

## 自动构建脚本

项目提供以下自动构建脚本，位于 `util/build/` 目录：

### 配置文件 config.properties

构建脚本会从 `util/build/config.properties` 读取版本号，自动确定发布目录。

**配置文件示例：**
```properties
# 版本号配置 - 修改此值会自动更新发布目录
version=1.0.0
```

### 1. build.bat - 普通构建
仅执行 Maven 构建，生成 JAR 文件到 `target/` 目录。

**使用方法：**
```cmd
util\build\build.bat
```

### 2. build_release.bat - 发布构建
执行 Maven 构建，并将生成的 JAR 文件复制到 `release/V{版本号}/` 目录。

**使用方法：**
```cmd
util\build\build_release.bat
```

**构建流程：**
1. 从 `config.properties` 读取版本号（如 `1.0.0`）
2. 执行 `mvn clean package -DskipTests`
3. 创建发布目录 `release/V1.0.0/`
4. 将 `target/aitools-1.0.0.jar` 复制到 `release/V1.0.0/aitools-1.0.0.jar`

> **提示：** 修改代码后，建议直接使用 `build_release.bat` 进行构建，它会自动处理文件复制。需要修改版本号时，只需编辑 `config.properties` 即可。

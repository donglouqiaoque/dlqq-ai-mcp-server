---
name: java-stdio-utf8-encoding
description: Java程序在Windows上通过stdin/stdout传输UTF-8中文时修复乱码的方案
source: auto-skill
extracted_at: '2026-05-31T07:11:51.644Z'
---

# Java Stdio UTF-8 中文编码修复

## 问题场景
Java程序在Windows系统上通过标准输入输出(stdio)与外部进程通信时，中文字符显示乱码。

根本原因：
- Windows系统默认使用 GBK 编码
- Java 的 `System.in` 和 `System.out` 默认使用平台编码
- 当外部进程发送 UTF-8 编码的中文数据时，Java 程序用 GBK 解码导致乱码

## 解决方案

### 1. 程序入口处设置 UTF-8 编码（尽早执行）

```java
public static void main(String[] args) {
    // 在最早阶段设置UTF-8编码
    System.setProperty("file.encoding", "UTF-8");
    System.setProperty("sun.stdout.encoding", "UTF-8");
    System.setProperty("sun.stderr.encoding", "UTF-8");

    // 重新初始化标准输入输出流的编码
    try {
        System.setOut(new java.io.PrintStream(System.out, true, "UTF-8"));
        System.setErr(new java.io.PrintStream(System.err, true, "UTF-8"));
    } catch (Exception ignored) {
    }
    
    // ... 其他代码
}
```

### 2. 正确读取 stdin 中的 UTF-8 数据

错误方式（使用平台默认编码）：
```java
// 错误！直接使用 (char) b 会使用平台默认编码
StringBuilder lineBuilder = new StringBuilder();
int b = inputStream.read();
lineBuilder.append((char) b);  // 这里会乱码！
String line = lineBuilder.toString();
```

正确方式（使用 UTF-8 解码）：
```java
// 正确：使用 ByteArrayOutputStream + UTF-8 解码
ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();
int b = inputStream.read();
lineBuffer.write(b);
// ... 循环读取直到换行
String line = lineBuffer.toString(StandardCharsets.UTF_8.name());
```

### 3. MCP 服务器完整示例

```java
public class MCPServerMain {
    public static void main(String[] args) {
        // 1. 最先设置编码
        System.setProperty("file.encoding", "UTF-8");
        try {
            System.setOut(new PrintStream(System.out, true, "UTF-8"));
            System.setErr(new PrintStream(System.err, true, "UTF-8"));
        } catch (Exception ignored) {}

        // 2. 创建传输层
        StdioTransport transport = new StdioTransport();
        // ...
    }
}

public class StdioTransport {
    private String readMessage() throws IOException {
        // 使用字节缓冲区读取，然后UTF-8解码
        ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();
        int b;
        
        while ((b = inputStream.read()) != -1) {
            if (b == '\n') break;
            if (b != '\r') lineBuffer.write(b);
        }
        
        // 关键：使用UTF-8解码字节数组
        // 注意：必须用 new String(bytes, charset) 而不是 toString(charsetName)
        // 因为 toString() 可能在某些JDK版本中使用平台默认编码
        return new String(lineBuffer.toByteArray(), StandardCharsets.UTF_8);
    }
}
```

## 验证方法

在程序启动时输出当前编码：
```java
System.err.println("Default charset: " + java.nio.charset.Charset.defaultCharset().name());
System.err.println("file.encoding: " + System.getProperty("file.encoding"));
```

## 常见错误

1. **设置时机太晚** - 必须在任何 I/O 操作之前设置编码
2. **只设置系统属性** - 需要同时重新初始化 `System.out` 和 `System.err`
3. **使用 Reader/Writer 读取 stdin** - 如果必须使用，确保指定 UTF-8 编码
4. **直接 `(char) b` 转换** - 这是最常见的错误，必须使用编码解码

## 相关配置

如果通过命令行启动，可以添加 JVM 参数：
```bash
java -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -jar app.jar
```

但代码中显式设置更可靠，因为 JVM 参数可能在某些环境下被忽略。
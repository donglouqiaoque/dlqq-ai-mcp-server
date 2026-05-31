---
name: java-json-integer-parsing
description: 修复Java MCP工具中JSON整数参数类型转换错误（Long vs Integer）
source: auto-skill
extracted_at: '2026-05-31T07:19:35.186Z'
---

# Java JSON 整数参数类型转换修复

## 问题场景
在Java MCP服务器或类似JSON-RPC场景中，从JSON解析的参数整数类型可能是`Long`或`Integer`，取决于具体的JSON解析器实现。直接强制转换为`Integer`会导致`ClassCastException`。

## 错误示例
```java
// 错误！直接强制转换可能抛出 ClassCastException
Integer timeout = (Integer) arguments.getOrDefault("timeout", 0);
// java.lang.Long cannot be cast to java.lang.Integer
```

## 解决方案

### 方法1：使用Number类型（推荐用于简单场景）
```java
Number timeoutNum = (Number) arguments.getOrDefault("timeout", 0);
int timeout = timeoutNum.intValue();
```

### 方法2：通用解析方法（推荐用于复杂场景）
```java
/**
 * 解析整数参数（兼容 Long 和 Integer）
 * 适用于JSON解析器可能返回Long或Integer的情况
 */
private Integer parseInteger(Object value, Integer defaultValue) {
    if (value == null) {
        return defaultValue;
    }
    if (value instanceof Integer) {
        return (Integer) value;
    }
    if (value instanceof Long) {
        return ((Long) value).intValue();
    }
    if (value instanceof Number) {
        return ((Number) value).intValue();
    }
    return defaultValue;
}

// 使用示例
Integer timeout = parseInteger(arguments.get("timeout"), 0);
Integer baudRate = parseInteger(arguments.get("baud_rate"), 115200);
Integer dataBits = parseInteger(arguments.get("data_bits"), 8);
Integer count = parseInteger(arguments.get("count"), 100);
```

### 方法3：处理浮点数参数
```java
private Double parseDouble(Object value, Double defaultValue) {
    if (value == null) {
        return defaultValue;
    }
    if (value instanceof Double) {
        return (Double) value;
    }
    if (value instanceof Number) {
        return ((Number) value).doubleValue();
    }
    return defaultValue;
}
```

## 完整MCP工具示例
```java
private void registerTool(McpServer server) {
    server.addTool("dialog_input",
        "弹出输入对话框",
        inputSchema,
        arguments -> {
            // 字符串参数直接转换
            String title = (String) arguments.get("title");
            
            // 布尔参数直接转换
            Boolean showFileSelect = (Boolean) arguments.getOrDefault("show_file_select", false);
            
            // 整数参数使用parseInteger兼容Long和Integer
            Integer timeout = parseInteger(arguments.get("timeout"), 0);
            
            // 列表参数
            @SuppressWarnings("unchecked")
            List<String> extensions = (List<String>) arguments.get("file_extensions");
            
            // ... 使用参数
        }
    );
}

/**
 * 解析整数参数（兼容 Long 和 Integer）
 */
private Integer parseInteger(Object value, Integer defaultValue) {
    if (value == null) {
        return defaultValue;
    }
    if (value instanceof Integer) {
        return (Integer) value;
    }
    if (value instanceof Long) {
        return ((Long) value).intValue();
    }
    if (value instanceof Number) {
        return ((Number) value).intValue();
    }
    return defaultValue;
}
```

## 常见参数类型处理
| 参数类型 | 处理方式 | 示例 |
|---------|---------|------|
| String | 直接强制转换 | `(String) args.get("title")` |
| Boolean | 直接强制转换 | `(Boolean) args.get("flag")` |
| Integer | 使用parseInteger | `parseInteger(args.get("timeout"), 0)` |
| Long | 使用Number转换 | `((Number) value).longValue()` |
| Double | 使用parseDouble | `parseDouble(args.get("rate"), 1.0)` |
| List | 泛型转换 | `(List<String>) args.get("items")` |
| Map | 泛型转换 | `(Map<String, Object>) args.get("config")` |

## 注意事项

1. **为什么JSON解析器返回Long？** - 很多JSON解析器（如Jackson、Gson在某些配置下）将所有整数解析为Long以确保精度

2. **MCP协议中的整数** - MCP协议通常使用JSON-RPC，数字默认为64位，解析为Long是符合规范的

3. **避免使用 `getOrDefault` 与强制转换的组合** - 这是最常见的错误模式：
   ```java
   // 危险！如果值为Long会报错
   Integer timeout = (Integer) arguments.getOrDefault("timeout", 0);
   ```

4. **建议使用 `get` + 单独处理默认值** - 更清晰、更容易处理类型转换：
   ```java
   // 更安全
   Integer timeout = parseInteger(arguments.get("timeout"), 0);
   ```

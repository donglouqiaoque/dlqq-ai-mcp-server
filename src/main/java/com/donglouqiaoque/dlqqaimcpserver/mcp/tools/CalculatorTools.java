package com.donglouqiaoque.dlqqaimcpserver.mcp.tools;

import com.donglouqiaoque.dlqqaimcpserver.mcp.protocol.McpSchema.CallToolResult;
import com.donglouqiaoque.dlqqaimcpserver.mcp.protocol.McpSchema.ToolContent;
import com.donglouqiaoque.dlqqaimcpserver.mcp.protocol.McpSchema.ToolInputSchema;
import com.donglouqiaoque.dlqqaimcpserver.mcp.server.McpServer;
import com.donglouqiaoque.dlqqaimcpserver.utils.CalculatorUtils;

import java.util.*;

/**
 * 计算器工具 - 数学计算器、程序计算器、函数计算器
 */
public class CalculatorTools {

    /**
     * 注册计算器相关工具
     */
    public static void registerTools(McpServer server) {
        registerMathCalculatorTool(server);
        registerProgrammerCalculatorTool(server);
        registerFunctionCalculatorTool(server);
    }

    /**
     * 注册数学计算器工具
     */
    private static void registerMathCalculatorTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> expressionProp = new HashMap<>();
        expressionProp.put("type", "string");
        expressionProp.put("description", "数学表达式，支持 + - * / ^ ( ) 和数学函数，如: 2+3*4, sin(PI/2), log(100)");
        properties.put("expression", expressionProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.singletonList("expression"));

        server.addTool("dlqq_calculator_math",
            "数学计算器 - 精确计算支持指数、对数、三角函数、双曲函数、阶乘等",
            inputSchema,
            params -> {
                Object expressionObj = params.get("expression");
                if (expressionObj == null) {
                    return createErrorResult("缺少必需参数: expression");
                }
                String expression = String.valueOf(expressionObj);

                try {
                    CalculatorUtils.MathResult result = CalculatorUtils.calculateMath(expression);

                    StringBuilder content = new StringBuilder();
                    content.append("【数学计算器】\n");
                    content.append("表达式: ").append(result.expression).append("\n");
                    content.append("结果: ").append(result.result).append("\n");
                    content.append("精确计算: ").append(result.isExact ? "是" : "否");

                    return createSuccessResult(content.toString());
                } catch (CalculatorUtils.CalculatorException e) {
                    return createErrorResult("计算错误: " + e.getMessage());
                } catch (Exception e) {
                    return createErrorResult("未知错误: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 注册程序计算器工具
     */
    private static void registerProgrammerCalculatorTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> expressionProp = new HashMap<>();
        expressionProp.put("type", "string");
        expressionProp.put("description", "表达式，支持 + - * / % & | ^ << >> >>> 和不同进制，如: 0xFF + 0b1010, 15 << 2");
        properties.put("expression", expressionProp);

        Map<String, Object> typeProp = new HashMap<>();
        typeProp.put("type", "string");
        typeProp.put("description", "数据类型: int(整型), long(长整型), float(单精度浮点), double(双精度浮点)，默认int");
        List<String> enumValues = Arrays.asList("int", "long", "float", "double");
        typeProp.put("enum", enumValues);
        properties.put("type", typeProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.singletonList("expression"));

        server.addTool("dlqq_calculator_programmer",
            "程序计算器 - 整型/浮点计算，支持位运算(AND OR XOR 位移)、多进制转换",
            inputSchema,
            params -> {
                Object expressionObj = params.get("expression");
                if (expressionObj == null) {
                    return createErrorResult("缺少必需参数: expression");
                }
                String expression = String.valueOf(expressionObj);

                String type = "int";
                Object typeObj = params.get("type");
                if (typeObj != null) {
                    type = String.valueOf(typeObj);
                }

                try {
                    CalculatorUtils.ProgrammerResult result = CalculatorUtils.calculateProgrammer(expression, type);

                    StringBuilder content = new StringBuilder();
                    content.append("【程序计算器】\n");
                    content.append("表达式: ").append(result.expression).append("\n");
                    content.append("数据类型: ").append(result.type).append("\n");
                    content.append("十进制: ").append(result.dec).append("\n");
                    content.append("十六进制: ").append(result.hex).append("\n");
                    content.append("八进制: ").append(result.oct).append("\n");
                    content.append("二进制: ").append(result.bin);

                    return createSuccessResult(content.toString());
                } catch (CalculatorUtils.CalculatorException e) {
                    return createErrorResult("计算错误: " + e.getMessage());
                } catch (Exception e) {
                    return createErrorResult("未知错误: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 注册函数计算器工具
     */
    private static void registerFunctionCalculatorTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> expressionProp = new HashMap<>();
        expressionProp.put("type", "string");
        expressionProp.put("description", "函数表达式，支持变量和内置函数，如: sin(x) + cos(y), pow(a, 2) + pow(b, 2)");
        properties.put("expression", expressionProp);

        Map<String, Object> variablesProp = new HashMap<>();
        variablesProp.put("type", "object");
        variablesProp.put("description", "变量映射，键为变量名，值为变量值或表达式，如: { \"x\": \"3.14\", \"y\": \"2.718\" }");
        properties.put("variables", variablesProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.singletonList("expression"));

        server.addTool("dlqq_calculator_function",
            "函数计算器 - 自定义函数表达式，支持变量和内置三角函数、对数、幂函数等",
            inputSchema,
            params -> {
                Object expressionObj = params.get("expression");
                if (expressionObj == null) {
                    return createErrorResult("缺少必需参数: expression");
                }
                String expression = String.valueOf(expressionObj);

                // 解析变量
                Map<String, String> variables = new HashMap<>();
                Object variablesObj = params.get("variables");
                if (variablesObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> varsMap = (Map<String, Object>) variablesObj;
                    for (Map.Entry<String, Object> entry : varsMap.entrySet()) {
                        String varName = entry.getKey();
                        Object varValue = entry.getValue();
                        if (varValue != null) {
                            variables.put(varName, String.valueOf(varValue));
                        }
                    }
                }

                try {
                    CalculatorUtils.FunctionResult result = CalculatorUtils.calculateFunction(expression, variables);

                    StringBuilder content = new StringBuilder();
                    content.append("【函数计算器】\n");
                    content.append("表达式: ").append(result.expression).append("\n");

                    if (!result.variables.isEmpty()) {
                        content.append("变量定义:\n");
                        for (Map.Entry<String, String> entry : result.variables.entrySet()) {
                            content.append("  ").append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
                        }
                        content.append("\n");
                    }

                    content.append("结果: ").append(result.result);

                    return createSuccessResult(content.toString());
                } catch (CalculatorUtils.CalculatorException e) {
                    return createErrorResult("计算错误: " + e.getMessage());
                } catch (Exception e) {
                    return createErrorResult("未知错误: " + e.getMessage());
                }
            }
        );
    }

    /**
     * 创建成功结果
     */
    private static CallToolResult createSuccessResult(String content) {
        return new CallToolResult(
            Collections.singletonList(ToolContent.text(content)),
            false
        );
    }

    /**
     * 创建错误结果
     */
    private static CallToolResult createErrorResult(String message) {
        return new CallToolResult(
            Collections.singletonList(ToolContent.text(message)),
            true
        );
    }
}

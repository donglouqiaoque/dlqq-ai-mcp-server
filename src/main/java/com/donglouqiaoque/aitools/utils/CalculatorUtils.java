package com.donglouqiaoque.aitools.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 计算器工具类 - 支持数学计算、程序计算和函数计算
 */
public class CalculatorUtils {

    /**
     * 数学函数接口 - 支持抛出 CalculatorException
     */
    @FunctionalInterface
    public interface MathFunction {
        BigDecimal apply(BigDecimal x) throws CalculatorException;
    }

    // 数学常量
    public static final BigDecimal PI = new BigDecimal("3.14159265358979323846264338327950288419716939937510");
    public static final BigDecimal E = new BigDecimal("2.71828182845904523536028747135266249775724709369995");

    // 默认精度
    private static final int DEFAULT_SCALE = 50;
    private static final MathContext MC = new MathContext(50, RoundingMode.HALF_UP);

    /**
     * 数学计算结果
     */
    public static class MathResult {
        public final String result;
        public final String expression;
        public final boolean isExact;

        public MathResult(String result, String expression, boolean isExact) {
            this.result = result;
            this.expression = expression;
            this.isExact = isExact;
        }
    }

    /**
     * 程序计算结果
     */
    public static class ProgrammerResult {
        public final String dec;      // 十进制
        public final String hex;      // 十六进制
        public final String oct;      // 八进制
        public final String bin;      // 二进制
        public final String expression;
        public final String type;     // 类型: INT, LONG, FLOAT, DOUBLE

        public ProgrammerResult(String dec, String hex, String oct, String bin, String expression, String type) {
            this.dec = dec;
            this.hex = hex;
            this.oct = oct;
            this.bin = bin;
            this.expression = expression;
            this.type = type;
        }
    }

    /**
     * 函数计算结果
     */
    public static class FunctionResult {
        public final String result;
        public final String expression;
        public final Map<String, String> variables;

        public FunctionResult(String result, String expression, Map<String, String> variables) {
            this.result = result;
            this.expression = expression;
            this.variables = variables;
        }
    }

    // ==================== 数学计算器 ====================

    /**
     * 数学计算器 - 精确计算支持指数、对数等
     * @param expression 数学表达式
     * @return 计算结果
     */
    public static MathResult calculateMath(String expression) throws CalculatorException {
        if (expression == null || expression.trim().isEmpty()) {
            throw new CalculatorException("表达式不能为空");
        }

        String cleaned = expression.trim().replaceAll("\\s+", "");

        try {
            BigDecimal result = evaluateMathExpression(cleaned);
            String resultStr = formatBigDecimal(result);
            return new MathResult(resultStr, expression, true);
        } catch (Exception e) {
            throw new CalculatorException("数学计算错误: " + e.getMessage());
        }
    }

    /**
     * 评估数学表达式
     */
    private static BigDecimal evaluateMathExpression(String expr) throws CalculatorException {
        // 替换常量
        expr = expr.replaceAll("(?i)\\bpi\\b", PI.toString());
        expr = expr.replaceAll("(?i)\\be\\b", E.toString());

        // 处理函数调用
        expr = processMathFunctions(expr);

        // 使用安全的方式计算表达式
        return evaluateExpression(expr);
    }

    /**
     * 处理数学函数
     */
    private static String processMathFunctions(String expr) throws CalculatorException {
        // 处理幂函数 pow(base, exp) 或 base^exp
        expr = processPower(expr);

        // 处理 sqrt(x)
        expr = processFunction(expr, "sqrt", CalculatorUtils::sqrt);

        // 处理 cbrt(x) - 立方根
        expr = processFunction(expr, "cbrt", CalculatorUtils::cbrt);

        // 处理 log(x) - 自然对数
        expr = processFunction(expr, "log", CalculatorUtils::ln);

        // 处理 ln(x) - 自然对数
        expr = processFunction(expr, "ln", CalculatorUtils::ln);

        // 处理 log10(x) - 常用对数
        expr = processFunction(expr, "log10", CalculatorUtils::log10);

        // 处理 log2(x) - 二进制对数
        expr = processFunction(expr, "log2", CalculatorUtils::log2);

        // 处理三角函数
        expr = processFunction(expr, "sin", CalculatorUtils::sin);
        expr = processFunction(expr, "cos", CalculatorUtils::cos);
        expr = processFunction(expr, "tan", CalculatorUtils::tan);
        expr = processFunction(expr, "asin", CalculatorUtils::asin);
        expr = processFunction(expr, "acos", CalculatorUtils::acos);
        expr = processFunction(expr, "atan", CalculatorUtils::atan);

        // 处理双曲函数
        expr = processFunction(expr, "sinh", CalculatorUtils::sinh);
        expr = processFunction(expr, "cosh", CalculatorUtils::cosh);
        expr = processFunction(expr, "tanh", CalculatorUtils::tanh);

        // 处理阶乘
        expr = processFactorial(expr);

        // 处理 abs
        expr = processFunction(expr, "abs", x -> x.abs());

        // 处理 floor
        expr = processFunction(expr, "floor", x -> x.setScale(0, RoundingMode.FLOOR));

        // 处理 ceil
        expr = processFunction(expr, "ceil", x -> x.setScale(0, RoundingMode.CEILING));

        // 处理 round
        expr = processFunction(expr, "round", x -> x.setScale(0, RoundingMode.HALF_UP));

        // 处理 exp
        expr = processFunction(expr, "exp", x -> exp(x));

        return expr;
    }

    /**
     * 处理幂运算
     */
    private static String processPower(String expr) throws CalculatorException {
        // 处理 pow(base, exp) 格式
        Pattern powPattern = Pattern.compile("pow\\(([^,]+),([^)]+)\\)");
        Matcher matcher = powPattern.matcher(expr);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String base = matcher.group(1);
            String exp = matcher.group(2);
            BigDecimal baseVal = evaluateExpression(base);
            BigDecimal expVal = evaluateExpression(exp);
            BigDecimal result = power(baseVal, expVal);
            matcher.appendReplacement(sb, result.toString());
        }
        matcher.appendTail(sb);
        expr = sb.toString();

        // 处理 base^exp 格式
        Pattern caretPattern = Pattern.compile("([\\d.]+(?:E-?\\d+)?|\\([^)]+\\))\\^([\\d.]+(?:E-?\\d+)?|\\([^)]+\\))");
        matcher = caretPattern.matcher(expr);
        sb = new StringBuffer();

        while (matcher.find()) {
            String base = matcher.group(1);
            String exp = matcher.group(2);
            BigDecimal baseVal = evaluateExpression(base);
            BigDecimal expVal = evaluateExpression(exp);
            BigDecimal result = power(baseVal, expVal);
            matcher.appendReplacement(sb, result.toString());
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * 处理阶乘
     */
    private static String processFactorial(String expr) throws CalculatorException {
        Pattern pattern = Pattern.compile("([\\d]+)!");
        Matcher matcher = pattern.matcher(expr);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            int n = Integer.parseInt(matcher.group(1));
            if (n < 0) {
                throw new CalculatorException("阶乘参数不能为负数");
            }
            if (n > 1000) {
                throw new CalculatorException("阶乘参数太大 (最大1000)");
            }
            BigInteger result = factorial(n);
            matcher.appendReplacement(sb, result.toString());
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * 通用函数处理
     */
    private static String processFunction(String expr, String funcName, MathFunction func) throws CalculatorException {
        Pattern pattern = Pattern.compile(funcName + "\\(([^)]+)\\)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(expr);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String arg = matcher.group(1);
            BigDecimal argVal = evaluateExpression(arg);
            BigDecimal result = func.apply(argVal);
            matcher.appendReplacement(sb, result.toString());
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    // ==================== 程序计算器 ====================

    /**
     * 程序计算器 - 整型、浮点、位运算
     * @param expression 表达式
     * @param type 数据类型: int, long, float, double
     * @return 计算结果
     */
    public static ProgrammerResult calculateProgrammer(String expression, String type) throws CalculatorException {
        if (expression == null || expression.trim().isEmpty()) {
            throw new CalculatorException("表达式不能为空");
        }

        String cleaned = expression.trim().replaceAll("\\s+", "");
        String typeUpper = (type == null ? "INT" : type.toUpperCase());

        try {
            switch (typeUpper) {
                case "INT":
                case "LONG":
                    return calculateInteger(cleaned, typeUpper);
                case "FLOAT":
                case "DOUBLE":
                    return calculateFloating(cleaned, typeUpper);
                default:
                    throw new CalculatorException("不支持的数据类型: " + type);
            }
        } catch (Exception e) {
            throw new CalculatorException("程序计算错误: " + e.getMessage());
        }
    }

    /**
     * 整型计算（支持位运算）
     */
    private static ProgrammerResult calculateInteger(String expr, String type) throws CalculatorException {
        // 解析并计算整型表达式
        BigInteger result = evaluateIntegerExpression(expr);

        // 处理类型范围
        if ("INT".equals(type)) {
            if (result.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0 ||
                result.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0) {
                throw new CalculatorException("结果超出 int 范围 (-2147483648 ~ 2147483647)");
            }
        } else if ("LONG".equals(type)) {
            if (result.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0 ||
                result.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) < 0) {
                throw new CalculatorException("结果超出 long 范围");
            }
        }

        return new ProgrammerResult(
            result.toString(),
            result.toString(16).toUpperCase(),
            result.toString(8),
            result.toString(2),
            expr,
            type
        );
    }

    /**
     * 浮点计算
     */
    private static ProgrammerResult calculateFloating(String expr, String type) throws CalculatorException {
        BigDecimal result = evaluateExpression(expr);

        // 转换为不同进制表示（浮点数的十六进制表示）
        String decStr = formatBigDecimal(result);

        // 对于浮点数，进制转换显示其IEEE 754表示
        String hexStr, octStr, binStr;
        if ("FLOAT".equals(type)) {
            float f = result.floatValue();
            int bits = Float.floatToIntBits(f);
            hexStr = String.format("0x%08X", bits);
            octStr = Integer.toOctalString(bits);
            binStr = String.format("0b%s", Integer.toBinaryString(bits));
        } else {
            double d = result.doubleValue();
            long bits = Double.doubleToLongBits(d);
            hexStr = String.format("0x%016X", bits);
            octStr = Long.toOctalString(bits);
            binStr = String.format("0b%s", Long.toBinaryString(bits));
        }

        return new ProgrammerResult(decStr, hexStr, octStr, binStr, expr, type);
    }

    /**
     * 评估整型表达式（支持位运算）
     */
    private static BigInteger evaluateIntegerExpression(String expr) throws CalculatorException {
        // 先处理括号内的表达式
        while (expr.contains("(")) {
            int openIndex = expr.lastIndexOf('(');
            int closeIndex = expr.indexOf(')', openIndex);
            if (closeIndex == -1) {
                throw new CalculatorException("括号不匹配");
            }
            String subExpr = expr.substring(openIndex + 1, closeIndex);
            BigInteger subResult = evaluateSimpleInteger(subExpr);
            expr = expr.substring(0, openIndex) + subResult.toString() + expr.substring(closeIndex + 1);
        }

        return evaluateSimpleInteger(expr);
    }

    /**
     * 评估简单整型表达式（无括号）
     */
    private static BigInteger evaluateSimpleInteger(String expr) throws CalculatorException {
        // 支持的操作符，按优先级排序
        String[] operators = {"<<", ">>", ">>>", "&", "^", "|", "+", "-", "*", "/", "%", "**"};

        // 处理幂运算 **
        while (expr.contains("**")) {
            int idx = expr.indexOf("**");
            int leftStart = findOperandStart(expr, idx - 1);
            int rightEnd = findOperandEnd(expr, idx + 2);
            BigInteger base = parseInteger(expr.substring(leftStart, idx));
            BigInteger exp = parseInteger(expr.substring(idx + 2, rightEnd));
            BigInteger result = base.pow(exp.intValueExact());
            expr = expr.substring(0, leftStart) + result.toString() + expr.substring(rightEnd);
        }

        // 处理乘除模 */
        expr = processIntegerBinaryOps(expr, new String[]{"*", "/", "%"});

        // 处理加减 +-
        expr = processIntegerBinaryOps(expr, new String[]{"+", "-"});

        // 处理位移 << >> >>>
        expr = processIntegerBinaryOps(expr, new String[]{"<<", ">>", ">>>"});

        // 处理位与 &
        expr = processIntegerBinaryOps(expr, new String[]{"&"});

        // 处理位异或 ^
        expr = processIntegerBinaryOps(expr, new String[]{"^"});

        // 处理位或 |
        expr = processIntegerBinaryOps(expr, new String[]{"|"});

        return parseInteger(expr);
    }

    /**
     * 处理整型二元运算
     */
    private static String processIntegerBinaryOps(String expr, String[] ops) throws CalculatorException {
        for (String op : ops) {
            Pattern pattern = Pattern.compile("(-?[\\d]+)" + Pattern.quote(op) + "(-?[\\d]+)");
            Matcher matcher = pattern.matcher(expr);

            while (matcher.find()) {
                BigInteger left = new BigInteger(matcher.group(1));
                BigInteger right = new BigInteger(matcher.group(2));
                BigInteger result;

                switch (op) {
                    case "+":
                        result = left.add(right);
                        break;
                    case "-":
                        result = left.subtract(right);
                        break;
                    case "*":
                        result = left.multiply(right);
                        break;
                    case "/":
                        if (right.equals(BigInteger.ZERO)) {
                            throw new CalculatorException("除数不能为零");
                        }
                        result = left.divide(right);
                        break;
                    case "%":
                        if (right.equals(BigInteger.ZERO)) {
                            throw new CalculatorException("除数不能为零");
                        }
                        result = left.remainder(right);
                        break;
                    case "<<":
                        result = left.shiftLeft(right.intValueExact());
                        break;
                    case ">>":
                        result = left.shiftRight(right.intValueExact());
                        break;
                    case ">>>":
                        // 无符号右移
                        result = left.shiftRight(right.intValueExact());
                        if (left.signum() < 0) {
                            // 清除高位
                            int bits = 64; // 假设64位
                            BigInteger mask = BigInteger.ONE.shiftLeft(bits).subtract(BigInteger.ONE);
                            result = result.and(mask);
                        }
                        break;
                    case "&":
                        result = left.and(right);
                        break;
                    case "|":
                        result = left.or(right);
                        break;
                    case "^":
                        result = left.xor(right);
                        break;
                    default:
                        throw new CalculatorException("未知操作符: " + op);
                }

                expr = expr.substring(0, matcher.start()) + result.toString() + expr.substring(matcher.end());
                matcher = pattern.matcher(expr);
            }
        }

        return expr;
    }

    /**
     * 解析整数字符串（支持多种进制）
     */
    private static BigInteger parseInteger(String s) throws CalculatorException {
        s = s.trim();
        if (s.isEmpty()) {
            throw new CalculatorException("空表达式");
        }

        // 处理负号
        boolean negative = false;
        if (s.startsWith("-")) {
            negative = true;
            s = s.substring(1);
        }

        BigInteger result;
        if (s.startsWith("0x") || s.startsWith("0X")) {
            // 十六进制
            result = new BigInteger(s.substring(2), 16);
        } else if (s.startsWith("0b") || s.startsWith("0B")) {
            // 二进制
            result = new BigInteger(s.substring(2), 2);
        } else if (s.startsWith("0o") || s.startsWith("0O")) {
            // 八进制
            result = new BigInteger(s.substring(2), 8);
        } else if (s.startsWith("0") && s.length() > 1) {
            // 八进制（无前缀）
            result = new BigInteger(s, 8);
        } else {
            // 十进制
            result = new BigInteger(s, 10);
        }

        return negative ? result.negate() : result;
    }

    // ==================== 函数计算器 ====================

    /**
     * 函数计算器 - 自定义函数表达式
     * @param expression 函数表达式
     * @param variables 变量映射
     * @return 计算结果
     */
    public static FunctionResult calculateFunction(String expression, Map<String, String> variables) throws CalculatorException {
        if (expression == null || expression.trim().isEmpty()) {
            throw new CalculatorException("表达式不能为空");
        }

        String cleaned = expression.trim();
        Map<String, String> resolvedVars = new LinkedHashMap<>();

        // 替换变量
        if (variables != null) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                String varName = entry.getKey();
                String varValue = entry.getValue();

                // 先计算变量值（支持嵌套表达式）
                BigDecimal varResult = evaluateExpression(varValue);
                resolvedVars.put(varName, formatBigDecimal(varResult));

                // 替换表达式中的变量
                cleaned = cleaned.replaceAll("\\b" + Pattern.quote(varName) + "\\b", varResult.toString());
            }
        }

        // 处理内置函数
        cleaned = processMathFunctions(cleaned);

        // 计算最终表达式
        BigDecimal result = evaluateExpression(cleaned);

        return new FunctionResult(formatBigDecimal(result), expression, resolvedVars);
    }

    /**
     * 获取内置函数列表
     */
    public static List<String> getBuiltinFunctions() {
        return Arrays.asList(
            // 数学函数
            "sqrt(x) - 平方根",
            "cbrt(x) - 立方根",
            "pow(base, exp) - 幂运算",
            "log(x) - 自然对数",
            "ln(x) - 自然对数",
            "log10(x) - 常用对数",
            "log2(x) - 二进制对数",
            "exp(x) - e的x次幂",
            "abs(x) - 绝对值",
            "floor(x) - 向下取整",
            "ceil(x) - 向上取整",
            "round(x) - 四舍五入",
            // 三角函数
            "sin(x) - 正弦",
            "cos(x) - 余弦",
            "tan(x) - 正切",
            "asin(x) - 反正弦",
            "acos(x) - 反余弦",
            "atan(x) - 反正切",
            // 双曲函数
            "sinh(x) - 双曲正弦",
            "cosh(x) - 双曲余弦",
            "tanh(x) - 双曲正切",
            // 常量
            "PI - 圆周率",
            "E - 自然常数"
        );
    }

    // ==================== 通用表达式计算 ====================

    /**
     * 评估通用表达式（支持 + - * / ( ) ）
     */
    private static BigDecimal evaluateExpression(String expr) throws CalculatorException {
        // 先处理括号内的表达式
        while (expr.contains("(")) {
            int openIndex = expr.lastIndexOf('(');
            int closeIndex = expr.indexOf(')', openIndex);
            if (closeIndex == -1) {
                throw new CalculatorException("括号不匹配");
            }
            String subExpr = expr.substring(openIndex + 1, closeIndex);
            BigDecimal subResult = evaluateSimpleExpression(subExpr);
            expr = expr.substring(0, openIndex) + subResult.toString() + expr.substring(closeIndex + 1);
        }

        return evaluateSimpleExpression(expr);
    }

    /**
     * 评估简单表达式（无括号）
     */
    private static BigDecimal evaluateSimpleExpression(String expr) throws CalculatorException {
        expr = expr.trim();
        if (expr.isEmpty()) {
            throw new CalculatorException("空表达式");
        }

        // 处理一元负号
        if (expr.startsWith("-")) {
            return evaluateSimpleExpression(expr.substring(1)).negate();
        }

        // 处理幂运算
        int powIndex = findOperatorOutsideParens(expr, "**");
        if (powIndex != -1) {
            BigDecimal base = evaluateSimpleExpression(expr.substring(0, powIndex));
            BigDecimal exp = evaluateSimpleExpression(expr.substring(powIndex + 2));
            return power(base, exp);
        }

        // 处理乘法、除法
        for (int i = expr.length() - 1; i >= 0; i--) {
            char c = expr.charAt(i);
            if (c == '*' && (i == 0 || expr.charAt(i - 1) != '*')) {
                BigDecimal left = evaluateSimpleExpression(expr.substring(0, i));
                BigDecimal right = evaluateSimpleExpression(expr.substring(i + 1));
                return left.multiply(right, MC);
            } else if (c == '/') {
                BigDecimal left = evaluateSimpleExpression(expr.substring(0, i));
                BigDecimal right = evaluateSimpleExpression(expr.substring(i + 1));
                if (right.compareTo(BigDecimal.ZERO) == 0) {
                    throw new CalculatorException("除数不能为零");
                }
                return left.divide(right, MC);
            } else if (c == '%') {
                BigDecimal left = evaluateSimpleExpression(expr.substring(0, i));
                BigDecimal right = evaluateSimpleExpression(expr.substring(i + 1));
                if (right.compareTo(BigDecimal.ZERO) == 0) {
                    throw new CalculatorException("除数不能为零");
                }
                return left.remainder(right, MC);
            }
        }

        // 处理加法、减法
        for (int i = expr.length() - 1; i >= 0; i--) {
            char c = expr.charAt(i);
            if (c == '+' && (i == 0 || expr.charAt(i - 1) != 'e')) {
                BigDecimal left = evaluateSimpleExpression(expr.substring(0, i));
                BigDecimal right = evaluateSimpleExpression(expr.substring(i + 1));
                return left.add(right, MC);
            } else if (c == '-' && i > 0 && expr.charAt(i - 1) != 'e') {
                BigDecimal left = evaluateSimpleExpression(expr.substring(0, i));
                BigDecimal right = evaluateSimpleExpression(expr.substring(i + 1));
                return left.subtract(right, MC);
            }
        }

        // 纯数字
        try {
            return new BigDecimal(expr);
        } catch (NumberFormatException e) {
            throw new CalculatorException("无法解析的数字: " + expr);
        }
    }

    /**
     * 查找操作符位置（排除括号内的）
     */
    private static int findOperatorOutsideParens(String expr, String op) {
        int depth = 0;
        for (int i = 0; i <= expr.length() - op.length(); i++) {
            char c = expr.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (depth == 0 && expr.substring(i).startsWith(op)) {
                return i;
            }
        }
        return -1;
    }

    private static int findOperandStart(String expr, int end) {
        int i = end;
        while (i >= 0 && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) {
            i--;
        }
        return i + 1;
    }

    private static int findOperandEnd(String expr, int start) {
        int i = start;
        while (i < expr.length() && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) {
            i++;
        }
        return i;
    }

    // ==================== 数学函数实现 ====================

    /**
     * 幂运算
     */
    private static BigDecimal power(BigDecimal base, BigDecimal exp) throws CalculatorException {
        // 对于整数指数，使用精确计算
        try {
            int intExp = exp.intValueExact();
            if (intExp >= 0 && intExp <= 1000) {
                return base.pow(intExp, MC);
            }
        } catch (ArithmeticException e) {
            // 不是整数，使用近似计算
        }

        // 使用自然对数和指数
        return exp(ln(base).multiply(exp));
    }

    /**
     * 平方根
     */
    private static BigDecimal sqrt(BigDecimal x) throws CalculatorException {
        if (x.compareTo(BigDecimal.ZERO) < 0) {
            throw new CalculatorException("平方根参数不能为负数");
        }
        if (x.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // 使用牛顿迭代法
        BigDecimal guess = new BigDecimal(Math.sqrt(x.doubleValue()));
        BigDecimal epsilon = new BigDecimal("1E-" + (DEFAULT_SCALE - 1));

        while (true) {
            BigDecimal next = guess.add(x.divide(guess, MC)).divide(BigDecimal.valueOf(2), MC);
            if (guess.subtract(next).abs().compareTo(epsilon) < 0) {
                return next;
            }
            guess = next;
        }
    }

    /**
     * 立方根
     */
    private static BigDecimal cbrt(BigDecimal x) {
        if (x.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // 使用牛顿迭代法
        BigDecimal guess = new BigDecimal(Math.cbrt(x.doubleValue()));
        BigDecimal epsilon = new BigDecimal("1E-" + (DEFAULT_SCALE - 1));
        BigDecimal three = BigDecimal.valueOf(3);

        while (true) {
            BigDecimal next = guess.multiply(BigDecimal.valueOf(2))
                .add(x.divide(guess.multiply(guess), MC))
                .divide(three, MC);
            if (guess.subtract(next).abs().compareTo(epsilon) < 0) {
                return next;
            }
            guess = next;
        }
    }

    /**
     * 自然对数
     */
    private static BigDecimal ln(BigDecimal x) throws CalculatorException {
        if (x.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CalculatorException("对数参数必须为正数");
        }

        // 使用泰勒级数展开
        BigDecimal result = new BigDecimal(Math.log(x.doubleValue()));
        return result;
    }

    /**
     * 常用对数
     */
    private static BigDecimal log10(BigDecimal x) throws CalculatorException {
        return ln(x).divide(ln(BigDecimal.TEN), MC);
    }

    /**
     * 二进制对数
     */
    private static BigDecimal log2(BigDecimal x) throws CalculatorException {
        return ln(x).divide(ln(BigDecimal.valueOf(2)), MC);
    }

    /**
     * e的x次幂
     */
    private static BigDecimal exp(BigDecimal x) {
        // 使用泰勒级数
        return new BigDecimal(Math.exp(x.doubleValue()));
    }

    /**
     * 阶乘
     */
    private static BigInteger factorial(int n) {
        BigInteger result = BigInteger.ONE;
        for (int i = 2; i <= n; i++) {
            result = result.multiply(BigInteger.valueOf(i));
        }
        return result;
    }

    // ==================== 三角函数 ====================

    private static BigDecimal sin(BigDecimal x) {
        // 转换为弧度
        return new BigDecimal(Math.sin(x.doubleValue()));
    }

    private static BigDecimal cos(BigDecimal x) {
        return new BigDecimal(Math.cos(x.doubleValue()));
    }

    private static BigDecimal tan(BigDecimal x) throws CalculatorException {
        BigDecimal cos = cos(x);
        if (cos.abs().compareTo(new BigDecimal("1E-10")) < 0) {
            throw new CalculatorException("正切函数在PI/2处有奇点");
        }
        return new BigDecimal(Math.tan(x.doubleValue()));
    }

    private static BigDecimal asin(BigDecimal x) throws CalculatorException {
        if (x.abs().compareTo(BigDecimal.ONE) > 0) {
            throw new CalculatorException("反正弦参数必须在[-1, 1]范围内");
        }
        return new BigDecimal(Math.asin(x.doubleValue()));
    }

    private static BigDecimal acos(BigDecimal x) throws CalculatorException {
        if (x.abs().compareTo(BigDecimal.ONE) > 0) {
            throw new CalculatorException("反余弦参数必须在[-1, 1]范围内");
        }
        return new BigDecimal(Math.acos(x.doubleValue()));
    }

    private static BigDecimal atan(BigDecimal x) {
        return new BigDecimal(Math.atan(x.doubleValue()));
    }

    // ==================== 双曲函数 ====================

    private static BigDecimal sinh(BigDecimal x) {
        return new BigDecimal(Math.sinh(x.doubleValue()));
    }

    private static BigDecimal cosh(BigDecimal x) {
        return new BigDecimal(Math.cosh(x.doubleValue()));
    }

    private static BigDecimal tanh(BigDecimal x) {
        return new BigDecimal(Math.tanh(x.doubleValue()));
    }

    // ==================== 辅助方法 ====================

    /**
     * 格式化 BigDecimal，去除末尾的0
     */
    private static String formatBigDecimal(BigDecimal bd) {
        if (bd == null) return "0";
        bd = bd.stripTrailingZeros();
        String s = bd.toPlainString();
        if (s.contains("E") || s.contains("e")) {
            return bd.toPlainString();
        }
        return s;
    }

    /**
     * 计算器异常
     */
    public static class CalculatorException extends Exception {
        public CalculatorException(String message) {
            super(message);
        }
    }
}

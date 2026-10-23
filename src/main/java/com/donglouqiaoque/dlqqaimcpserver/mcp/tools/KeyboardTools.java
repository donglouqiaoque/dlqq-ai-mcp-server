package com.donglouqiaoque.dlqqaimcpserver.mcp.tools;

import com.donglouqiaoque.dlqqaimcpserver.mcp.protocol.McpSchema.CallToolResult;
import com.donglouqiaoque.dlqqaimcpserver.mcp.protocol.McpSchema.ToolContent;
import com.donglouqiaoque.dlqqaimcpserver.mcp.protocol.McpSchema.ToolInputSchema;
import com.donglouqiaoque.dlqqaimcpserver.mcp.server.McpServer;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;

/**
 * 键盘工具 - 模拟键盘输入、按键、组合键（Java原生实现，使用 java.awt.Robot）
 */
public class KeyboardTools {

    private static final Robot robot = createRobot();

    // 按键名称到KeyEvent VK码的映射
    private static final Map<String, Integer> KEY_MAP = new HashMap<>();

    static {
        // 字母
        for (char c = 'a'; c <= 'z'; c++) {
            KEY_MAP.put(String.valueOf(c), KeyEvent.getExtendedKeyCodeForChar(c));
        }
        // 数字
        for (char c = '0'; c <= '9'; c++) {
            KEY_MAP.put(String.valueOf(c), KeyEvent.getExtendedKeyCodeForChar(c));
        }
        // 功能键
        for (int i = 1; i <= 24; i++) {
            KEY_MAP.put("f" + i, KeyEvent.VK_F1 + i - 1);
        }
        // 修饰键
        KEY_MAP.put("ctrl", KeyEvent.VK_CONTROL);
        KEY_MAP.put("control", KeyEvent.VK_CONTROL);
        KEY_MAP.put("alt", KeyEvent.VK_ALT);
        KEY_MAP.put("shift", KeyEvent.VK_SHIFT);
        KEY_MAP.put("win", KeyEvent.VK_WINDOWS);
        KEY_MAP.put("meta", KeyEvent.VK_WINDOWS);
        KEY_MAP.put("altgr", KeyEvent.VK_ALT_GRAPH);
        // 特殊键
        KEY_MAP.put("enter", KeyEvent.VK_ENTER);
        KEY_MAP.put("return", KeyEvent.VK_ENTER);
        KEY_MAP.put("回车", KeyEvent.VK_ENTER);
        KEY_MAP.put("space", KeyEvent.VK_SPACE);
        KEY_MAP.put("空格", KeyEvent.VK_SPACE);
        KEY_MAP.put("tab", KeyEvent.VK_TAB);
        KEY_MAP.put("esc", KeyEvent.VK_ESCAPE);
        KEY_MAP.put("escape", KeyEvent.VK_ESCAPE);
        KEY_MAP.put("backspace", KeyEvent.VK_BACK_SPACE);
        KEY_MAP.put("bs", KeyEvent.VK_BACK_SPACE);
        KEY_MAP.put("delete", KeyEvent.VK_DELETE);
        KEY_MAP.put("del", KeyEvent.VK_DELETE);
        KEY_MAP.put("insert", KeyEvent.VK_INSERT);
        KEY_MAP.put("ins", KeyEvent.VK_INSERT);
        KEY_MAP.put("home", KeyEvent.VK_HOME);
        KEY_MAP.put("end", KeyEvent.VK_END);
        KEY_MAP.put("pageup", KeyEvent.VK_PAGE_UP);
        KEY_MAP.put("pagedown", KeyEvent.VK_PAGE_DOWN);
        KEY_MAP.put("up", KeyEvent.VK_UP);
        KEY_MAP.put("down", KeyEvent.VK_DOWN);
        KEY_MAP.put("left", KeyEvent.VK_LEFT);
        KEY_MAP.put("right", KeyEvent.VK_RIGHT);
        // 符号键
        KEY_MAP.put("minus", KeyEvent.VK_MINUS);
        KEY_MAP.put("dash", KeyEvent.VK_MINUS);
        KEY_MAP.put("equals", KeyEvent.VK_EQUALS);
        KEY_MAP.put("comma", KeyEvent.VK_COMMA);
        KEY_MAP.put("period", KeyEvent.VK_PERIOD);
        KEY_MAP.put("dot", KeyEvent.VK_PERIOD);
        KEY_MAP.put("slash", KeyEvent.VK_SLASH);
        KEY_MAP.put("semicolon", KeyEvent.VK_SEMICOLON);
        KEY_MAP.put("quote", KeyEvent.VK_QUOTE);
        KEY_MAP.put("backslash", KeyEvent.VK_BACK_SLASH);
        KEY_MAP.put("openbracket", KeyEvent.VK_OPEN_BRACKET);
        KEY_MAP.put("closebracket", KeyEvent.VK_CLOSE_BRACKET);
        KEY_MAP.put("backquote", KeyEvent.VK_BACK_QUOTE);
        KEY_MAP.put("grave", KeyEvent.VK_BACK_QUOTE);
        KEY_MAP.put("capslock", KeyEvent.VK_CAPS_LOCK);
        KEY_MAP.put("printscreen", KeyEvent.VK_PRINTSCREEN);
        KEY_MAP.put("scrolllock", KeyEvent.VK_SCROLL_LOCK);
        KEY_MAP.put("numlock", KeyEvent.VK_NUM_LOCK);
        KEY_MAP.put("pause", KeyEvent.VK_PAUSE);
        // 小键盘
        KEY_MAP.put("num0", KeyEvent.VK_NUMPAD0);
        KEY_MAP.put("num1", KeyEvent.VK_NUMPAD1);
        KEY_MAP.put("num2", KeyEvent.VK_NUMPAD2);
        KEY_MAP.put("num3", KeyEvent.VK_NUMPAD3);
        KEY_MAP.put("num4", KeyEvent.VK_NUMPAD4);
        KEY_MAP.put("num5", KeyEvent.VK_NUMPAD5);
        KEY_MAP.put("num6", KeyEvent.VK_NUMPAD6);
        KEY_MAP.put("num7", KeyEvent.VK_NUMPAD7);
        KEY_MAP.put("num8", KeyEvent.VK_NUMPAD8);
        KEY_MAP.put("num9", KeyEvent.VK_NUMPAD9);
        KEY_MAP.put("numadd", KeyEvent.VK_ADD);
        KEY_MAP.put("numsub", KeyEvent.VK_SUBTRACT);
        KEY_MAP.put("nummul", KeyEvent.VK_MULTIPLY);
        KEY_MAP.put("numdiv", KeyEvent.VK_DIVIDE);
    }

    private static Robot createRobot() {
        try {
            return new Robot();
        } catch (AWTException e) {
            return null;
        }
    }

    private static CallToolResult ok(String text) {
        return new CallToolResult(Collections.singletonList(ToolContent.text(text)), false);
    }

    private static CallToolResult err(String text) {
        return new CallToolResult(Collections.singletonList(ToolContent.text(text)), true);
    }

    private static Robot getRobot() {
        return robot;
    }

    /**
     * 解析按键名，返回VK码；失败返回-1
     */
    private static int parseKey(String keyName) {
        if (keyName == null) return -1;
        String key = keyName.trim().toLowerCase();
        // 直接查表
        Integer code = KEY_MAP.get(key);
        if (code != null) return code;
        // 尝试KeyEvent中的静态字段名（如 VK_A）
        try {
            Field field = KeyEvent.class.getField("VK_" + key.toUpperCase());
            return field.getInt(null);
        } catch (Exception e) {
            // 忽略
        }
        return -1;
    }

    /**
     * 解析组合键字符串（如 "ctrl+c"、"ctrl+alt+delete"）
     */
    private static List<Integer> parseCombination(String combo) {
        List<Integer> codes = new ArrayList<>();
        if (combo == null) return codes;
        String[] parts = combo.split("\\+");
        for (String part : parts) {
            int code = parseKey(part.trim());
            if (code == -1) {
                return null; // 非法按键
            }
            codes.add(code);
        }
        return codes;
    }

    public static void registerTools(McpServer server) {
        registerKeyboardTypeTool(server);
        registerKeyboardTapTool(server);
        registerKeyboardPressTool(server);
        registerKeyboardReleaseTool(server);
        registerKeyboardHotkeyTool(server);
        registerKeyboardKeyListTool(server);
    }

    // ==================== dlqq_keyboard_type ====================

    private static void registerKeyboardTypeTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> textProp = new HashMap<>();
        textProp.put("type", "string");
        textProp.put("description", "要输入的文本内容（支持中文等任意字符）");
        properties.put("text", textProp);
        addIntProp(properties, "interval", "每字符间隔毫秒（可选，默认20）");
        ToolInputSchema schema = new ToolInputSchema(properties, Collections.singletonList("text"));

        server.addTool("dlqq_keyboard_type",
            "模拟键盘输入文本。支持中文等任意字符（通过剪贴板粘贴方式实现，确保焦点在目标输入框中）。",
            schema,
            args -> {
                Robot r = getRobot();
                if (r == null) return err("无法初始化Robot（可能不支持图形环境）");
                String text = (String) args.get("text");
                if (text == null || text.isEmpty()) return err("文本不能为空");
                try {
                    // 使用剪贴板粘贴，支持任意字符
                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    clipboard.setContents(new StringSelection(text), null);
                    r.delay(80);
                    pressCombo(r, Arrays.asList(KeyEvent.VK_CONTROL, KeyEvent.VK_V));
                    return ok("已输入文本 " + text.length() + " 个字符: " + truncate(text, 50));
                } catch (Exception e) {
                    return err("输入文本失败: " + e.getMessage());
                }
            }
        );
    }

    // ==================== dlqq_keyboard_tap ====================

    private static void registerKeyboardTapTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> keyProp = new HashMap<>();
        keyProp.put("type", "string");
        keyProp.put("description", "按键名称，如: a、0、enter、tab、space、f5、delete、up、ctrl等");
        properties.put("key", keyProp);
        addIntProp(properties, "times", "重复次数（默认1）");
        addIntProp(properties, "interval", "重复间隔毫秒（默认100）");
        ToolInputSchema schema = new ToolInputSchema(properties, Collections.singletonList("key"));

        server.addTool("dlqq_keyboard_tap",
            "敲击一次按键（按下并释放）。按键名支持字母、数字、功能键、方向键和常见特殊键。",
            schema,
            args -> {
                Robot r = getRobot();
                if (r == null) return err("无法初始化Robot（可能不支持图形环境）");
                int code = parseKey((String) args.get("key"));
                if (code == -1) {
                    return err("不支持的按键: " + args.get("key") + "，可使用 dlqq_keyboard_key_list 查看支持列表");
                }
                int times = args.get("times") != null ? Math.max(1, toInt(args.get("times"))) : 1;
                int interval = args.get("interval") != null ? Math.max(10, toInt(args.get("interval"))) : 100;
                try {
                    for (int i = 0; i < times; i++) {
                        r.keyPress(code);
                        r.delay(30);
                        r.keyRelease(code);
                        if (i < times - 1) r.delay(interval);
                    }
                    return ok("已敲击按键 '" + args.get("key") + "' " + times + " 次");
                } catch (Exception e) {
                    return err("按键失败: " + e.getMessage());
                }
            }
        );
    }

    // ==================== dlqq_keyboard_press / release ====================

    private static void registerKeyboardPressTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> keyProp = new HashMap<>();
        keyProp.put("type", "string");
        keyProp.put("description", "按键名称");
        properties.put("key", keyProp);
        ToolInputSchema schema = new ToolInputSchema(properties, Collections.singletonList("key"));

        server.addTool("dlqq_keyboard_press",
            "按下按键（不释放）。与 dlqq_keyboard_release 配合实现长按。",
            schema,
            args -> {
                Robot r = getRobot();
                if (r == null) return err("无法初始化Robot（可能不支持图形环境）");
                int code = parseKey((String) args.get("key"));
                if (code == -1) return err("不支持的按键: " + args.get("key"));
                try {
                    r.keyPress(code);
                    return ok("已按下按键 '" + args.get("key") + "'");
                } catch (Exception e) {
                    return err("按下失败: " + e.getMessage());
                }
            }
        );
    }

    private static void registerKeyboardReleaseTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> keyProp = new HashMap<>();
        keyProp.put("type", "string");
        keyProp.put("description", "按键名称");
        properties.put("key", keyProp);
        ToolInputSchema schema = new ToolInputSchema(properties, Collections.singletonList("key"));

        server.addTool("dlqq_keyboard_release",
            "释放按键。与 dlqq_keyboard_press 配合使用。",
            schema,
            args -> {
                Robot r = getRobot();
                if (r == null) return err("无法初始化Robot（可能不支持图形环境）");
                int code = parseKey((String) args.get("key"));
                if (code == -1) return err("不支持的按键: " + args.get("key"));
                try {
                    r.keyRelease(code);
                    return ok("已释放按键 '" + args.get("key") + "'");
                } catch (Exception e) {
                    return err("释放失败: " + e.getMessage());
                }
            }
        );
    }

    // ==================== dlqq_keyboard_hotkey ====================

    private static void registerKeyboardHotkeyTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> comboProp = new HashMap<>();
        comboProp.put("type", "string");
        comboProp.put("description", "组合键，用+连接，如: ctrl+c、ctrl+alt+delete、alt+tab、ctrl+shift+esc");
        properties.put("combo", comboProp);
        addIntProp(properties, "interval", "按下到释放的间隔毫秒（默认80）");
        ToolInputSchema schema = new ToolInputSchema(properties, Collections.singletonList("combo"));

        server.addTool("dlqq_keyboard_hotkey",
            "执行组合键（快捷键）。例如 ctrl+c 复制、ctrl+v 粘贴、alt+tab 切换窗口。",
            schema,
            args -> {
                Robot r = getRobot();
                if (r == null) return err("无法初始化Robot（可能不支持图形环境）");
                List<Integer> codes = parseCombination((String) args.get("combo"));
                if (codes == null || codes.isEmpty()) {
                    return err("无效的组合键: " + args.get("combo") + "，格式如 ctrl+c、ctrl+alt+delete");
                }
                int interval = args.get("interval") != null ? Math.max(20, toInt(args.get("interval"))) : 80;
                try {
                    pressCombo(r, codes, interval);
                    return ok("已执行组合键: " + args.get("combo"));
                } catch (Exception e) {
                    return err("组合键失败: " + e.getMessage());
                }
            }
        );
    }

    // ==================== dlqq_keyboard_key_list ====================

    private static void registerKeyboardKeyListTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        ToolInputSchema schema = new ToolInputSchema(properties, Collections.emptyList());

        server.addTool("dlqq_keyboard_key_list",
            "列出键盘工具支持的按键名称列表。",
            schema,
            args -> {
                StringBuilder sb = new StringBuilder();
                sb.append("【支持的按键名称】\n");
                sb.append("字母: a-z\n");
                sb.append("数字: 0-9\n");
                sb.append("功能键: f1-f24\n");
                sb.append("修饰键: ctrl, alt, shift, win, altgr\n");
                sb.append("特殊键: enter, tab, space, esc, backspace, delete, insert, home, end, pageup, pagedown\n");
                sb.append("方向键: up, down, left, right\n");
                sb.append("符号键: minus, equals, comma, period, slash, semicolon, quote, backslash, openbracket, closebracket, backquote\n");
                sb.append("其他: capslock, printscreen, scrolllock, numlock, pause\n");
                sb.append("小键盘: num0-num9, numadd, numsub, nummul, numdiv\n");
                return ok(sb.toString());
            }
        );
    }

    // ==================== 辅助 ====================

    private static void pressCombo(Robot r, List<Integer> codes) {
        pressCombo(r, codes, 80);
    }

    private static void pressCombo(Robot r, List<Integer> codes, int interval) {
        for (int code : codes) {
            r.keyPress(code);
            r.delay(Math.max(10, interval / Math.max(1, codes.size())));
        }
        for (int i = codes.size() - 1; i >= 0; i--) {
            r.keyRelease(codes.get(i));
            r.delay(Math.max(10, interval / Math.max(1, codes.size())));
        }
    }

    private static void addIntProp(Map<String, Object> properties, String name, String description) {
        Map<String, Object> prop = new HashMap<>();
        prop.put("type", "integer");
        prop.put("description", description);
        properties.put(name, prop);
    }

    private static int toInt(Object obj) throws NumberFormatException {
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        return Integer.parseInt(String.valueOf(obj).trim());
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}

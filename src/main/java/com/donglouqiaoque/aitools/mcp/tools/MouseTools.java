package com.donglouqiaoque.aitools.mcp.tools;

import com.donglouqiaoque.aitools.mcp.protocol.McpSchema.CallToolResult;
import com.donglouqiaoque.aitools.mcp.protocol.McpSchema.ToolContent;
import com.donglouqiaoque.aitools.mcp.protocol.McpSchema.ToolInputSchema;
import com.donglouqiaoque.aitools.mcp.server.McpServer;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 鼠标工具 - 模拟鼠标点击、移动、滚动（Java原生实现，使用 java.awt.Robot）
 */
public class MouseTools {

    private static final Robot robot = createRobot();

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
        if (robot == null) {
            return null;
        }
        return robot;
    }

    /**
     * 解析按钮名称对应的掩码
     */
    private static int parseButtonMask(String button) {
        if (button == null) return InputEvent.BUTTON1_DOWN_MASK;
        switch (button.trim().toLowerCase()) {
            case "left":
            case "左键":
                return InputEvent.BUTTON1_DOWN_MASK;
            case "right":
            case "右键":
                return InputEvent.BUTTON3_DOWN_MASK;
            case "middle":
            case "中键":
                return InputEvent.BUTTON2_DOWN_MASK;
            default:
                return InputEvent.BUTTON1_DOWN_MASK;
        }
    }

    private static int parseButtonNumber(String button) {
        if (button == null) return MouseEvent.BUTTON1;
        switch (button.trim().toLowerCase()) {
            case "right":
            case "右键":
                return MouseEvent.BUTTON3;
            case "middle":
            case "中键":
                return MouseEvent.BUTTON2;
            default:
                return MouseEvent.BUTTON1;
        }
    }

    private static Point currentPosition() {
        try {
            return MouseInfo.getPointerInfo().getLocation();
        } catch (Exception e) {
            return new Point(0, 0);
        }
    }

    public static void registerTools(McpServer server) {
        registerMouseGetPositionTool(server);
        registerMouseMoveTool(server);
        registerMouseClickTool(server);
        registerMousePressTool(server);
        registerMouseReleaseTool(server);
        registerMouseScrollTool(server);
        registerMouseDragTool(server);
    }

    // ==================== dlqq_mouse_get_position ====================

    private static void registerMouseGetPositionTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        ToolInputSchema schema = new ToolInputSchema(properties, Collections.emptyList());

        server.addTool("dlqq_mouse_get_position",
            "获取鼠标当前在屏幕上的位置坐标 (x, y)。",
            schema,
            args -> {
                if (getRobot() == null) return err("无法初始化Robot（可能不支持图形环境）");
                Point p = currentPosition();
                return ok("鼠标当前位置: x=" + p.x + ", y=" + p.y + " (屏幕分辨率: " +
                    Toolkit.getDefaultToolkit().getScreenSize().width + "x" +
                    Toolkit.getDefaultToolkit().getScreenSize().height + ")");
            }
        );
    }

    // ==================== dlqq_mouse_move ====================

    private static void registerMouseMoveTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        addIntProp(properties, "x", "目标X坐标（绝对坐标）");
        addIntProp(properties, "y", "目标Y坐标（绝对坐标）");
        addIntProp(properties, "dx", "相对X位移（指定dx/dy时按相对移动，x/y可省略）");
        addIntProp(properties, "dy", "相对Y位移");
        ToolInputSchema schema = new ToolInputSchema(properties, Collections.emptyList());

        server.addTool("dlqq_mouse_move",
            "移动鼠标指针到指定位置。支持绝对坐标（x/y）和相对位移（dx/dy）。",
            schema,
            args -> {
                Robot r = getRobot();
                if (r == null) return err("无法初始化Robot（可能不支持图形环境）");
                try {
                    boolean hasRelative = args.get("dx") != null || args.get("dy") != null;
                    Point current = currentPosition();
                    int targetX = current.x;
                    int targetY = current.y;

                    if (hasRelative) {
                        if (args.get("dx") != null) targetX = current.x + toInt(args.get("dx"));
                        if (args.get("dy") != null) targetY = current.y + toInt(args.get("dy"));
                    } else {
                        if (args.get("x") == null || args.get("y") == null) {
                            return err("必须指定 x/y（绝对坐标）或 dx/dy（相对位移）");
                        }
                        targetX = toInt(args.get("x"));
                        targetY = toInt(args.get("y"));
                    }

                    // 限制在屏幕范围内
                    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
                    targetX = Math.max(0, Math.min(screen.width - 1, targetX));
                    targetY = Math.max(0, Math.min(screen.height - 1, targetY));

                    r.mouseMove(targetX, targetY);
                    return ok("鼠标已移动到: x=" + targetX + ", y=" + targetY);
                } catch (NumberFormatException e) {
                    return err("坐标参数必须是整数: " + e.getMessage());
                } catch (Exception e) {
                    return err("移动鼠标失败: " + e.getMessage());
                }
            }
        );
    }

    // ==================== dlqq_mouse_click ====================

    private static void registerMouseClickTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        addIntProp(properties, "x", "点击X坐标（可选，默认当前位置）");
        addIntProp(properties, "y", "点击Y坐标（可选，默认当前位置）");
        Map<String, Object> buttonProp = new HashMap<>();
        buttonProp.put("type", "string");
        buttonProp.put("description", "按键: left(左键)/right(右键)/middle(中键)，默认left");
        buttonProp.put("enum", java.util.Arrays.asList("left", "right", "middle"));
        properties.put("button", buttonProp);
        addIntProp(properties, "clicks", "点击次数（1=单击，2=双击，默认1）");
        addIntProp(properties, "interval", "多次点击之间的间隔毫秒（默认100）");
        ToolInputSchema schema = new ToolInputSchema(properties, Collections.emptyList());

        server.addTool("dlqq_mouse_click",
            "模拟鼠标点击。支持左键/右键/中键，单击/双击，可指定坐标或使用当前位置。",
            schema,
            args -> {
                Robot r = getRobot();
                if (r == null) return err("无法初始化Robot（可能不支持图形环境）");
                try {
                    Point p = currentPosition();
                    if (args.get("x") != null) p.x = toInt(args.get("x"));
                    if (args.get("y") != null) p.y = toInt(args.get("y"));

                    int buttonMask = parseButtonMask((String) args.get("button"));
                    int clicks = args.get("clicks") != null ? Math.max(1, toInt(args.get("clicks"))) : 1;
                    int interval = args.get("interval") != null ? Math.max(10, toInt(args.get("interval"))) : 100;

                    r.mouseMove(p.x, p.y);
                    r.delay(50);
                    for (int i = 0; i < clicks; i++) {
                        r.mousePress(buttonMask);
                        r.delay(50);
                        r.mouseRelease(buttonMask);
                        if (i < clicks - 1) {
                            r.delay(interval);
                        }
                    }
                    String btnName = args.get("button") == null ? "左键" : String.valueOf(args.get("button"));
                    return ok("已点击 " + btnName + " " + clicks + " 次，位置: (" + p.x + ", " + p.y + ")");
                } catch (NumberFormatException e) {
                    return err("参数必须是整数: " + e.getMessage());
                } catch (Exception e) {
                    return err("点击失败: " + e.getMessage());
                }
            }
        );
    }

    // ==================== dlqq_mouse_press / release ====================

    private static void registerMousePressTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> buttonProp = new HashMap<>();
        buttonProp.put("type", "string");
        buttonProp.put("description", "按键: left(左键)/right(右键)/middle(中键)，默认left");
        buttonProp.put("enum", java.util.Arrays.asList("left", "right", "middle"));
        properties.put("button", buttonProp);
        addIntProp(properties, "x", "按下位置的X坐标（可选，默认当前位置）");
        addIntProp(properties, "y", "按下位置的Y坐标（可选，默认当前位置）");
        ToolInputSchema schema = new ToolInputSchema(properties, Collections.emptyList());

        server.addTool("dlqq_mouse_press",
            "按下鼠标按钮（不释放）。常与 dlqq_mouse_release 配合实现长按或拖拽。",
            schema,
            args -> {
                Robot r = getRobot();
                if (r == null) return err("无法初始化Robot（可能不支持图形环境）");
                try {
                    Point p = currentPosition();
                    if (args.get("x") != null) p.x = toInt(args.get("x"));
                    if (args.get("y") != null) p.y = toInt(args.get("y"));
                    r.mouseMove(p.x, p.y);
                    r.mousePress(parseButtonMask((String) args.get("button")));
                    return ok("已按下 " + (args.get("button") == null ? "左键" : args.get("button")) + "，位置: (" + p.x + ", " + p.y + ")");
                } catch (Exception e) {
                    return err("按下失败: " + e.getMessage());
                }
            }
        );
    }

    private static void registerMouseReleaseTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> buttonProp = new HashMap<>();
        buttonProp.put("type", "string");
        buttonProp.put("description", "按键: left(左键)/right(右键)/middle(中键)，默认left");
        buttonProp.put("enum", java.util.Arrays.asList("left", "right", "middle"));
        properties.put("button", buttonProp);
        ToolInputSchema schema = new ToolInputSchema(properties, Collections.emptyList());

        server.addTool("dlqq_mouse_release",
            "释放鼠标按钮。与 dlqq_mouse_press 配合使用。",
            schema,
            args -> {
                Robot r = getRobot();
                if (r == null) return err("无法初始化Robot（可能不支持图形环境）");
                try {
                    r.mouseRelease(parseButtonMask((String) args.get("button")));
                    return ok("已释放 " + (args.get("button") == null ? "左键" : args.get("button")));
                } catch (Exception e) {
                    return err("释放失败: " + e.getMessage());
                }
            }
        );
    }

    // ==================== dlqq_mouse_scroll ====================

    private static void registerMouseScrollTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        addIntProp(properties, "amount", "滚动量（正数向上滚动，负数向下滚动，单位：滚轮刻度）");
        Map<String, Object> directionProp = new HashMap<>();
        directionProp.put("type", "string");
        directionProp.put("description", "滚动方向: up(向上)/down(向下)，与amount正负等价（可选）");
        directionProp.put("enum", java.util.Arrays.asList("up", "down"));
        properties.put("direction", directionProp);
        ToolInputSchema schema = new ToolInputSchema(properties, Collections.emptyList());

        server.addTool("dlqq_mouse_scroll",
            "滚动鼠标滚轮。amount为正数向上滚动，负数向下滚动。",
            schema,
            args -> {
                Robot r = getRobot();
                if (r == null) return err("无法初始化Robot（可能不支持图形环境）");
                try {
                    int amount = args.get("amount") != null ? toInt(args.get("amount")) : 1;
                    String direction = (String) args.get("direction");
                    if ("down".equalsIgnoreCase(direction)) {
                        amount = -Math.abs(amount);
                    } else if ("up".equalsIgnoreCase(direction)) {
                        amount = Math.abs(amount);
                    }
                    if (amount == 0) return err("滚动量不能为0");
                    r.mouseWheel(amount);
                    return ok("已滚动滚轮 " + (amount > 0 ? "向上 " + amount + " 格" : "向下 " + (-amount) + " 格"));
                } catch (NumberFormatException e) {
                    return err("amount必须是整数: " + e.getMessage());
                } catch (Exception e) {
                    return err("滚动失败: " + e.getMessage());
                }
            }
        );
    }

    // ==================== dlqq_mouse_drag ====================

    private static void registerMouseDragTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();
        addIntProp(properties, "start_x", "起始X坐标（可选，默认当前位置）");
        addIntProp(properties, "start_y", "起始Y坐标（可选，默认当前位置）");
        addIntProp(properties, "end_x", "结束X坐标");
        addIntProp(properties, "end_y", "结束Y坐标");
        addIntProp(properties, "duration", "拖拽持续时间毫秒（默认500）");
        addIntProp(properties, "steps", "拖拽移动步数（默认20）");
        Map<String, Object> buttonProp = new HashMap<>();
        buttonProp.put("type", "string");
        buttonProp.put("description", "按键: left(左键)/right(右键)/middle(中键)，默认left");
        buttonProp.put("enum", java.util.Arrays.asList("left", "right", "middle"));
        properties.put("button", buttonProp);
        ToolInputSchema schema = new ToolInputSchema(properties, java.util.Arrays.asList("end_x", "end_y"));

        server.addTool("dlqq_mouse_drag",
            "模拟鼠标拖拽：从起点按下按钮，移动鼠标到终点后释放。",
            schema,
            args -> {
                Robot r = getRobot();
                if (r == null) return err("无法初始化Robot（可能不支持图形环境）");
                try {
                    if (args.get("end_x") == null || args.get("end_y") == null) {
                        return err("必须指定 end_x/end_y");
                    }
                    Point start = currentPosition();
                    if (args.get("start_x") != null) start.x = toInt(args.get("start_x"));
                    if (args.get("start_y") != null) start.y = toInt(args.get("start_y"));
                    int endX = toInt(args.get("end_x"));
                    int endY = toInt(args.get("end_y"));
                    int duration = args.get("duration") != null ? Math.max(50, toInt(args.get("duration"))) : 500;
                    int steps = args.get("steps") != null ? Math.max(2, toInt(args.get("steps"))) : 20;
                    int buttonMask = parseButtonMask((String) args.get("button"));

                    r.mouseMove(start.x, start.y);
                    r.delay(50);
                    r.mousePress(buttonMask);
                    r.delay(50);

                    int delayPerStep = Math.max(10, duration / steps);
                    for (int i = 1; i <= steps; i++) {
                        int x = start.x + (endX - start.x) * i / steps;
                        int y = start.y + (endY - start.y) * i / steps;
                        r.mouseMove(x, y);
                        r.delay(delayPerStep);
                    }
                    r.mouseRelease(buttonMask);
                    return ok("拖拽完成: (" + start.x + ", " + start.y + ") → (" + endX + ", " + endY + ")");
                } catch (NumberFormatException e) {
                    return err("参数必须是整数: " + e.getMessage());
                } catch (Exception e) {
                    return err("拖拽失败: " + e.getMessage());
                }
            }
        );
    }

    // ==================== 辅助 ====================

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
}

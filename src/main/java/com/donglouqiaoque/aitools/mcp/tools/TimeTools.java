package com.donglouqiaoque.aitools.mcp.tools;

import com.donglouqiaoque.aitools.mcp.protocol.McpSchema.CallToolResult;
import com.donglouqiaoque.aitools.mcp.protocol.McpSchema.ToolContent;
import com.donglouqiaoque.aitools.mcp.protocol.McpSchema.ToolInputSchema;
import com.donglouqiaoque.aitools.mcp.server.McpServer;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 时间工具 - 获取当前时间（Java原生实现）
 */
public class TimeTools {

    public static void registerTools(McpServer server) {
        registerGetCurrentTimeTool(server);
    }

    /**
     * dlqq_get_current_time - 获取当前时间
     */
    private static void registerGetCurrentTimeTool(McpServer server) {
        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> timezoneProp = new HashMap<>();
        timezoneProp.put("type", "string");
        timezoneProp.put("description", "时区ID（可选，如 Asia/Shanghai、UTC、America/New_York，默认使用系统时区）");
        properties.put("timezone", timezoneProp);

        Map<String, Object> formatProp = new HashMap<>();
        formatProp.put("type", "string");
        formatProp.put("description", "时间格式（可选，Java DateTimeFormatter格式，默认 yyyy-MM-dd HH:mm:ss）");
        properties.put("format", formatProp);

        ToolInputSchema inputSchema = new ToolInputSchema(properties, Collections.emptyList());

        server.addTool("dlqq_get_current_time",
            "获取当前日期时间。返回日期时间、时间戳、星期、时区等信息。支持指定时区和格式。",
            inputSchema,
            args -> {
                try {
                    // 解析时区
                    ZoneId zone;
                    String timezone = (String) args.get("timezone");
                    if (timezone != null && !timezone.trim().isEmpty()) {
                        try {
                            zone = ZoneId.of(timezone.trim());
                        } catch (Exception e) {
                            return new CallToolResult(
                                Collections.singletonList(ToolContent.text("错误：无效的时区ID: " + timezone +
                                    "（例如 Asia/Shanghai、UTC、America/New_York）")), true);
                        }
                    } else {
                        zone = ZoneId.systemDefault();
                    }

                    // 解析格式
                    String format = (String) args.get("format");
                    if (format == null || format.trim().isEmpty()) {
                        format = "yyyy-MM-dd HH:mm:ss";
                    }
                    DateTimeFormatter formatter;
                    try {
                        formatter = DateTimeFormatter.ofPattern(format.trim());
                    } catch (Exception e) {
                        return new CallToolResult(
                            Collections.singletonList(ToolContent.text("错误：无效的时间格式: " + format)), true);
                    }

                    ZonedDateTime now = ZonedDateTime.now(zone);
                    Instant instant = now.toInstant();
                    long epochSecond = instant.getEpochSecond();
                    long epochMillis = instant.toEpochMilli();

                    // 星期中文
                    String[] weekdays = {"星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};
                    String weekday = weekdays[now.getDayOfWeek().getValue() - 1];

                    StringBuilder sb = new StringBuilder();
                    sb.append("【当前时间】\n");
                    sb.append("===================\n\n");
                    sb.append("日期时间: ").append(now.format(formatter)).append("\n");
                    sb.append("时间戳: ").append(epochSecond).append(" 秒 / ").append(epochMillis).append(" 毫秒\n");
                    sb.append("星期: ").append(weekday).append("\n");
                    sb.append("时区: ").append(zone.getId())
                      .append(" (").append(now.getOffset()).append(")\n");
                    sb.append("ISO 8601: ").append(now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)).append("\n");
                    if (timezone != null && !timezone.trim().isEmpty()) {
                        sb.append("系统时区: ").append(ZoneId.systemDefault().getId()).append("\n");
                    }

                    return new CallToolResult(Collections.singletonList(ToolContent.text(sb.toString())), false);
                } catch (Exception e) {
                    return new CallToolResult(
                        Collections.singletonList(ToolContent.text("错误：获取时间失败: " + e.getMessage())), true);
                }
            }
        );
    }
}

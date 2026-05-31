package com.donglouqiaoque.aitools.ai;

import com.donglouqiaoque.aitools.config.ConfigManager;
import com.donglouqiaoque.aitools.http.HttpClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI服务 - 提供AI对话和文本生成功能
 */
public class AIService {

    private final HttpClient httpClient;
    private final ConfigManager configManager;

    public AIService() {
        this.httpClient = new HttpClient();
        this.configManager = ConfigManager.getInstance();
    }

    /**
     * 发送单条消息并获取回复
     *
     * @param message 用户消息
     * @return AI回复
     * @throws IOException 请求异常
     */
    public String chat(String message) throws IOException {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.user(message));
        return chat(messages);
    }

    /**
     * 发送消息列表并获取回复（支持多轮对话）
     *
     * @param messages 消息列表
     * @return AI回复
     * @throws IOException 请求异常
     */
    public String chat(List<ChatMessage> messages) throws IOException {
        return chat(messages, configManager.getModel());
    }

    /**
     * 发送消息列表并获取回复（指定模型）
     *
     * @param messages 消息列表
     * @param model    模型名称
     * @return AI回复
     * @throws IOException 请求异常
     */
    public String chat(List<ChatMessage> messages, String model) throws IOException {
        if (!configManager.hasApiKey()) {
            throw new IllegalStateException("未配置API密钥，请使用 'config set-key <your-api-key>' 进行配置");
        }

        String apiUrl = configManager.getApiUrl();
        String apiKey = configManager.getApiKey();

        // 构建请求体
        StringBuilder requestBody = new StringBuilder();
        requestBody.append("{");
        requestBody.append("\"model\": \"").append(model).append("\",");
        requestBody.append("\"messages\": [");

        for (int i = 0; i < messages.size(); i++) {
            ChatMessage msg = messages.get(i);
            requestBody.append("{");
            requestBody.append("\"role\": \"").append(escapeJson(msg.getRole())).append("\",");
            requestBody.append("\"content\": \"").append(escapeJson(msg.getContent())).append("\"");
            requestBody.append("}");
            if (i < messages.size() - 1) {
                requestBody.append(",");
            }
        }

        requestBody.append("]");
        requestBody.append("}");

        // 设置请求头
        Map<String, String> headers = Map.of(
                "Content-Type", "application/json",
                "Authorization", "Bearer " + apiKey
        );

        // 发送请求
        String response = httpClient.post(apiUrl, requestBody.toString(), headers);

        // 解析响应
        return parseChatResponse(response);
    }

    /**
     * 流式对话（逐步返回内容）
     *
     * @param message 用户消息
     * @param callback 回调函数，接收每个片段
     * @throws IOException 请求异常
     */
    public void chatStream(String message, StreamCallback callback) throws IOException {
        // 简化版：先获取完整响应再分段返回
        String response = chat(message);
        // 模拟流式输出
        int chunkSize = 10;
        for (int i = 0; i < response.length(); i += chunkSize) {
            int end = Math.min(i + chunkSize, response.length());
            callback.onChunk(response.substring(i, end));
        }
        callback.onComplete();
    }

    /**
     * 解析聊天响应
     */
    private String parseChatResponse(String response) {
        // 简化的JSON解析，提取第一个choice的content
        // 实际项目中建议使用JSON库如Jackson或Gson
        try {
            // 查找 "content": " 的位置
            int contentIndex = response.indexOf("\"content\":");
            if (contentIndex == -1) {
                return "无法解析响应";
            }

            // 找到content值开始的位置
            int valueStart = response.indexOf("\"", contentIndex + 10) + 1;
            if (valueStart == 0) {
                return "无法解析响应";
            }

            // 找到content值结束的位置
            int valueEnd = response.indexOf("\"", valueStart);
            if (valueEnd == -1) {
                return "无法解析响应";
            }

            String content = response.substring(valueStart, valueEnd);
            return unescapeJson(content);

        } catch (Exception e) {
            return "解析响应失败: " + e.getMessage();
        }
    }

    /**
     * 转义JSON字符串
     */
    private String escapeJson(String input) {
        if (input == null) return "";
        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 反转义JSON字符串
     */
    private String unescapeJson(String input) {
        if (input == null) return "";
        return input
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    /**
     * 流式回调接口
     */
    public interface StreamCallback {
        void onChunk(String chunk);
        void onComplete();
        default void onError(String error) {
            System.err.println("流式输出错误: " + error);
        }
    }
}

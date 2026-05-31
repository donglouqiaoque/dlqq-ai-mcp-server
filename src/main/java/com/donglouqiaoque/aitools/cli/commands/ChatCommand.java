package com.donglouqiaoque.aitools.cli.commands;

import com.donglouqiaoque.aitools.ai.AIService;
import com.donglouqiaoque.aitools.cli.CommandParser;

import java.io.IOException;

/**
 * 聊天命令
 */
public class ChatCommand implements Command {

    private final AIService aiService;

    public ChatCommand() {
        this.aiService = new AIService();
    }

    @Override
    public int execute(CommandParser parser) {
        if (parser.getCommandArgCount() == 0 && !parser.hasOption("m")) {
            showHelp();
            return 0;
        }

        String message;
        if (parser.hasOption("m")) {
            message = parser.getOption("m");
        } else {
            message = String.join(" ", parser.getRemainingArgs());
        }

        if (message == null || message.trim().isEmpty()) {
            System.err.println("错误: 消息内容不能为空");
            return 1;
        }

        try {
            System.out.println("正在请求AI...");
            String response = aiService.chat(message);
            System.out.println("\nAI回复:");
            System.out.println(response);
            return 0;
        } catch (IOException e) {
            System.err.println("请求失败: " + e.getMessage());
            return 1;
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
            return 1;
        }
    }

    @Override
    public String getName() {
        return "chat";
    }

    @Override
    public String getDescription() {
        return "与AI进行对话";
    }

    @Override
    public String getUsage() {
        return "chat <message>\n" +
                "       chat -m \"<message>\"\n" +
                "示例:\n" +
                "  chat 你好\n" +
                "  chat -m \"请帮我解释什么是Java\"";
    }
}

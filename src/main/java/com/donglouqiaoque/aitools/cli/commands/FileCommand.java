package com.donglouqiaoque.aitools.cli.commands;

import com.donglouqiaoque.aitools.ai.AIService;
import com.donglouqiaoque.aitools.cli.CommandParser;
import com.donglouqiaoque.aitools.utils.FileUtils;

import java.io.IOException;

/**
 * 文件处理命令
 */
public class FileCommand implements Command {

    private final AIService aiService;

    public FileCommand() {
        this.aiService = new AIService();
    }

    @Override
    public int execute(CommandParser parser) {
        if (parser.getCommandArgCount() == 0) {
            showHelp();
            return 0;
        }

        String subCommand = parser.getCommandArg(0);

        switch (subCommand) {
            case "read":
                return handleRead(parser);
            case "ask":
                return handleAsk(parser);
            case "summarize":
                return handleSummarize(parser);
            case "help":
            default:
                showHelp();
                return 0;
        }
    }

    /**
     * 读取文件
     */
    private int handleRead(CommandParser parser) {
        if (parser.getCommandArgCount() < 2) {
            System.err.println("错误: 缺少文件路径");
            System.out.println("用法: file read <file-path>");
            return 1;
        }

        String filePath = parser.getCommandArg(1);
        try {
            String content = FileUtils.readFile(filePath);
            System.out.println(content);
            return 0;
        } catch (IOException e) {
            System.err.println("读取文件失败: " + e.getMessage());
            return 1;
        }
    }

    /**
     * 询问关于文件的问题
     */
    private int handleAsk(CommandParser parser) {
        if (parser.getCommandArgCount() < 3) {
            System.err.println("错误: 参数不足");
            System.out.println("用法: file ask <file-path> <question>");
            return 1;
        }

        String filePath = parser.getCommandArg(1);
        StringBuilder questionBuilder = new StringBuilder();
        for (int i = 2; i < parser.getCommandArgCount(); i++) {
            if (i > 2) questionBuilder.append(" ");
            questionBuilder.append(parser.getCommandArg(i));
        }
        String question = questionBuilder.toString();

        try {
            String content = FileUtils.readFile(filePath);
            // 限制内容长度，避免超出token限制
            content = FileUtils.truncate(content, 8000);

            String prompt = "请根据以下文件内容回答问题:\n\n" +
                    "文件内容:\n" + content + "\n\n" +
                    "问题: " + question;

            System.out.println("正在分析文件...");
            String response = aiService.chat(prompt);
            System.out.println("\nAI回复:");
            System.out.println(response);
            return 0;
        } catch (IOException e) {
            System.err.println("处理失败: " + e.getMessage());
            return 1;
        }
    }

    /**
     * 总结文件内容
     */
    private int handleSummarize(CommandParser parser) {
        if (parser.getCommandArgCount() < 2) {
            System.err.println("错误: 缺少文件路径");
            System.out.println("用法: file summarize <file-path>");
            return 1;
        }

        String filePath = parser.getCommandArg(1);
        try {
            String content = FileUtils.readFile(filePath);
            // 限制内容长度
            content = FileUtils.truncate(content, 8000);

            String prompt = "请总结以下文件的主要内容，用简洁的语言描述:\n\n" + content;

            System.out.println("正在生成摘要...");
            String response = aiService.chat(prompt);
            System.out.println("\n文件摘要:");
            System.out.println(response);
            return 0;
        } catch (IOException e) {
            System.err.println("处理失败: " + e.getMessage());
            return 1;
        }
    }

    @Override
    public String getName() {
        return "file";
    }

    @Override
    public String getDescription() {
        return "文件处理工具，支持读取、分析和总结文件";
    }

    @Override
    public String getUsage() {
        return "file <subcommand> [args...]\n" +
                "子命令:\n" +
                "  read <file>              读取文件内容\n" +
                "  ask <file> <question>      询问关于文件的问题\n" +
                "  summarize <file>           总结文件内容\n" +
                "\n示例:\n" +
                "  file read ./readme.md\n" +
                "  file ask ./code.java 这段代码做了什么\n" +
                "  file summarize ./report.txt";
    }
}

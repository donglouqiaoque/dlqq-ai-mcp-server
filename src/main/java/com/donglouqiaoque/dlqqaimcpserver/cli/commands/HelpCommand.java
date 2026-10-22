package com.donglouqiaoque.dlqqaimcpserver.cli.commands;

import com.donglouqiaoque.dlqqaimcpserver.cli.CommandParser;

import java.util.Map;

/**
 * 帮助命令
 */
public class HelpCommand implements Command {

    private final Map<String, Command> commands;

    public HelpCommand(Map<String, Command> commands) {
        this.commands = commands;
    }

    @Override
    public int execute(CommandParser parser) {
        if (parser.getCommandArgCount() > 0) {
            // 显示特定命令的帮助
            String commandName = parser.getCommandArg(0);
            Command command = commands.get(commandName);
            if (command != null) {
                command.showHelp();
            } else {
                System.err.println("未知命令: " + commandName);
                System.out.println("可用命令: " + String.join(", ", commands.keySet()));
            }
            return 0;
        }

        // 显示通用帮助
        showGeneralHelp();
        return 0;
    }

    /**
     * 显示通用帮助信息
     */
    private void showGeneralHelp() {
        System.out.println("鸫楼桥鹊AI工具 - 命令行AI助手");
        System.out.println();
        System.out.println("用法: aitools <command> [args...]");
        System.out.println();
        System.out.println("可用命令:");
        System.out.println();

        for (Map.Entry<String, Command> entry : commands.entrySet()) {
            Command cmd = entry.getValue();
            System.out.printf("  %-12s %s%n", cmd.getName(), cmd.getDescription());
        }

        System.out.println();
        System.out.println("使用 'aitools help <command>' 查看具体命令的详细用法");
        System.out.println();
        System.out.println("示例:");
        System.out.println("  aitools config set-key your-api-key");
        System.out.println("  aitools chat 你好");
        System.out.println("  aitools file summarize ./readme.md");
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "显示帮助信息";
    }

    @Override
    public String getUsage() {
        return "help [command]";
    }
}

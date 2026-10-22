package com.donglouqiaoque.dlqqaimcpserver.cli.commands;

import com.donglouqiaoque.dlqqaimcpserver.cli.CommandParser;
import com.donglouqiaoque.dlqqaimcpserver.config.ConfigManager;

/**
 * 配置命令
 */
public class ConfigCommand implements Command {

    @Override
    public int execute(CommandParser parser) {
        if (parser.getCommandArgCount() == 0) {
            showHelp();
            return 0;
        }

        String subCommand = parser.getCommandArg(0);
        ConfigManager config = ConfigManager.getInstance();

        switch (subCommand) {
            case "set-key":
                if (parser.getCommandArgCount() < 2) {
                    System.err.println("错误: 缺少API密钥参数");
                    System.out.println("用法: config set-key <api-key>");
                    return 1;
                }
                config.setApiKey(parser.getCommandArg(1));
                System.out.println("API密钥已设置");
                return 0;

            case "set-url":
                if (parser.getCommandArgCount() < 2) {
                    System.err.println("错误: 缺少API地址参数");
                    System.out.println("用法: config set-url <api-url>");
                    return 1;
                }
                config.setApiUrl(parser.getCommandArg(1));
                System.out.println("API地址已设置为: " + config.getApiUrl());
                return 0;

            case "set-model":
                if (parser.getCommandArgCount() < 2) {
                    System.err.println("错误: 缺少模型参数");
                    System.out.println("用法: config set-model <model-name>");
                    return 1;
                }
                config.setModel(parser.getCommandArg(1));
                System.out.println("默认模型已设置为: " + config.getModel());
                return 0;

            case "get":
                if (parser.getCommandArgCount() < 2) {
                    System.err.println("错误: 缺少配置项名称");
                    System.out.println("用法: config get <key>");
                    return 1;
                }
                String key = parser.getCommandArg(1);
                String value = config.getProperty(key, "");
                System.out.println(key + " = " + value);
                return 0;

            case "list":
                System.out.println("当前配置:");
                System.out.println("  API密钥: " + (config.hasApiKey() ? "已设置" : "未设置"));
                System.out.println("  API地址: " + config.getApiUrl());
                System.out.println("  默认模型: " + config.getModel());
                return 0;

            case "help":
            default:
                showHelp();
                return 0;
        }
    }

    @Override
    public String getName() {
        return "config";
    }

    @Override
    public String getDescription() {
        return "管理 MCP 服务器的配置";
    }

    @Override
    public String getUsage() {
        return "config <subcommand> [args...]\n" +
                "子命令:\n" +
                "  set-key <api-key>    设置API密钥\n" +
                "  set-url <api-url>    设置API地址\n" +
                "  set-model <model>    设置默认模型\n" +
                "  get <key>            获取配置项\n" +
                "  list                 列出所有配置";
    }
}

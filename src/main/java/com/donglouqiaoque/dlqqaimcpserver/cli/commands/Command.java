package com.donglouqiaoque.dlqqaimcpserver.cli.commands;

import com.donglouqiaoque.dlqqaimcpserver.cli.CommandParser;

/**
 * 命令接口
 */
public interface Command {

    /**
     * 执行命令
     *
     * @param parser 命令行解析器
     * @return 执行结果
     */
    int execute(CommandParser parser);

    /**
     * 获取命令名称
     *
     * @return 命令名称
     */
    String getName();

    /**
     * 获取命令描述
     *
     * @return 命令描述
     */
    String getDescription();

    /**
     * 获取命令用法
     *
     * @return 命令用法
     */
    String getUsage();

    /**
     * 显示帮助信息
     */
    default void showHelp() {
        System.out.println("命令: " + getName());
        System.out.println("描述: " + getDescription());
        System.out.println("用法: " + getUsage());
    }
}

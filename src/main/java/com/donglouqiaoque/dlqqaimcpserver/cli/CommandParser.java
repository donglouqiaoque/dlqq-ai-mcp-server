package com.donglouqiaoque.dlqqaimcpserver.cli;

import java.util.*;

/**
 * 命令行解析器
 */
public class CommandParser {

    private final List<String> arguments;
    private final Map<String, String> options;
    private String command;
    private List<String> commandArgs;

    public CommandParser(String[] args) {
        this.arguments = new ArrayList<>(Arrays.asList(args));
        this.options = new HashMap<>();
        this.commandArgs = new ArrayList<>();
        parse();
    }

    /**
     * 解析命令行参数
     */
    private void parse() {
        if (arguments.isEmpty()) {
            return;
        }

        // 第一个参数是命令
        this.command = arguments.get(0);

        // 解析剩余参数
        for (int i = 1; i < arguments.size(); i++) {
            String arg = arguments.get(i);

            if (arg.startsWith("--")) {
                // 长选项 --key=value 或 --key value
                String key = arg.substring(2);
                String value = "true";

                if (key.contains("=")) {
                    int eqIndex = key.indexOf('=');
                    value = key.substring(eqIndex + 1);
                    key = key.substring(0, eqIndex);
                } else if (i + 1 < arguments.size() && !arguments.get(i + 1).startsWith("-")) {
                    value = arguments.get(i + 1);
                    i++;
                }

                options.put(key, value);
            } else if (arg.startsWith("-") && arg.length() > 1) {
                // 短选项 -k value 或 -k (布尔值)
                String key = arg.substring(1);
                String value = "true";

                if (key.length() > 1 && !key.startsWith("-")) {
                    // 处理组合选项如 -abc
                    for (char c : key.toCharArray()) {
                        options.put(String.valueOf(c), "true");
                    }
                } else {
                    if (i + 1 < arguments.size() && !arguments.get(i + 1).startsWith("-")) {
                        value = arguments.get(i + 1);
                        i++;
                    }
                    options.put(key, value);
                }
            } else {
                // 普通参数
                commandArgs.add(arg);
            }
        }
    }

    /**
     * 获取命令
     */
    public String getCommand() {
        return command;
    }

    /**
     * 获取命令参数
     */
    public List<String> getCommandArgs() {
        return commandArgs;
    }

    /**
     * 获取所有选项
     */
    public Map<String, String> getOptions() {
        return options;
    }

    /**
     * 检查是否包含选项
     */
    public boolean hasOption(String key) {
        return options.containsKey(key);
    }

    /**
     * 获取选项值
     */
    public String getOption(String key) {
        return options.get(key);
    }

    /**
     * 获取选项值（带默认值）
     */
    public String getOption(String key, String defaultValue) {
        return options.getOrDefault(key, defaultValue);
    }

    /**
     * 获取布尔选项
     */
    public boolean getBooleanOption(String key, boolean defaultValue) {
        String value = options.get(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    /**
     * 获取整数选项
     */
    public int getIntOption(String key, int defaultValue) {
        String value = options.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 获取命令参数数量
     */
    public int getCommandArgCount() {
        return commandArgs.size();
    }

    /**
     * 获取指定位置的命令参数
     */
    public String getCommandArg(int index) {
        if (index < 0 || index >= commandArgs.size()) {
            return null;
        }
        return commandArgs.get(index);
    }

    /**
     * 获取所有剩余参数
     */
    public String[] getRemainingArgs() {
        return commandArgs.toArray(new String[0]);
    }

    @Override
    public String toString() {
        return "CommandParser{" +
                "command='" + command + '\'' +
                ", options=" + options +
                ", commandArgs=" + commandArgs +
                '}';
    }
}

package com.donglouqiaoque.dlqqaimcpserver;

/**
 * 构建信息（编译期常量）。
 *
 * <p>{@link #BUILD_TIME} 由 Maven 在编译前用 templating-maven-plugin 填充，
 * 并作为字符串常量编译进 class 文件。运行中替换 JAR 不会改变已加载的值，
 * 因此版本工具用它报告“真实运行代码的构建时间”。</p>
 */
public final class BuildInfo {

    /** 构建时间（UTC，格式 yyyy-MM-dd HH:mm:ss），编译期写死 */
    public static final String BUILD_TIME = "${build.timestamp}";

    private BuildInfo() {
    }
}
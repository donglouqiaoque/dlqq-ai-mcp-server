package com.donglouqiaoque.dlqqaimcpserver.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 工作区根目录与修改权限管理。
 *
 * <p>工作区路径<b>不持久化</b>：只能来自启动参数 {@code --workspace <dir>}，
 * 或运行期通过 {@code dlqq_workspace_set} 动态设置（需用户 JFrame 审批）；
 * 未指定时使用进程启动目录（CWD）。</p>
 *
 * <p>修改权限持久化在工作区的 {@code .dlqq/settings/settings.properties}
 * （键 {@code workspace.write.mode}）：</p>
 * <ul>
 *   <li>{@code readonly}：只读模式，禁止任何文件修改；</li>
 *   <li>{@code workspace}：工作区修改（默认），只允许修改工作区内的文件；</li>
 *   <li>{@code approval}：工作区外审批，工作区内直接修改、工作区外弹窗确认；</li>
 *   <li>{@code full}：完全权限，允许修改任意位置的文件。</li>
 * </ul>
 */
public final class Workspace {

    /** 工作区来源：启动参数 */
    public static final String SOURCE_CLI = "--workspace argument";
    /** 工作区来源：运行期设置 */
    public static final String SOURCE_RUNTIME = "runtime setting";
    /** 工作区来源：进程工作目录 */
    public static final String SOURCE_CWD = "process working directory";

    /** 只读模式：禁止修改任何文件 */
    public static final String PERMISSION_READONLY = "readonly";
    /** 工作区修改模式：只允许修改工作区内的文件 */
    public static final String PERMISSION_WORKSPACE = "workspace";
    /** 工作区外审批模式：工作区内直接修改，工作区外弹窗审批 */
    public static final String PERMISSION_APPROVAL = "approval";
    /** 完全权限：允许修改任意位置的文件 */
    public static final String PERMISSION_FULL = "full";

    /** 工作区权限配置键 */
    public static final String PERMISSION_KEY = "workspace.write.mode";
    /** 工作区内配置文件相对路径（与历史路径保持一致） */
    public static final String SETTINGS_RELATIVE_PATH = ".dlqq/settings/settings.properties";

    private static volatile Path root = normalizeCwd();
    private static volatile String source = SOURCE_CWD;
    private static volatile String cliPath;
    private static volatile String permission = PERMISSION_WORKSPACE;

    private Workspace() {
    }

    /**
     * 初始化工作区（服务启动时调用一次）：优先启动参数，否则进程 CWD。
     *
     * @param cliWorkspace 启动参数 --workspace 的值，可为 null
     */
    public static synchronized void init(String cliWorkspace) {
        cliPath = isBlank(cliWorkspace) ? null : cliWorkspace.trim();
        if (cliPath != null) {
            Path path = toExistingDirectory(cliPath);
            if (path != null) {
                root = path;
                source = SOURCE_CLI;
                reloadPermission();
                return;
            }
        }
        root = normalizeCwd();
        source = SOURCE_CWD;
        reloadPermission();
    }

    /**
     * 运行期设置工作区（不持久化；是否允许由调用方先做用户审批）。
     *
     * @param directory 已存在的绝对目录
     * @throws IllegalArgumentException 目录不存在或不是目录
     */
    public static synchronized void setRoot(String directory) {
        Path path = toExistingDirectory(directory);
        if (path == null) {
            throw new IllegalArgumentException("工作区必须是已存在的绝对目录: " + directory);
        }
        root = path;
        source = SOURCE_RUNTIME;
        reloadPermission();
    }

    /**
     * 回退到启动参数指定的工作区（若有效），否则回退到进程 CWD。
     */
    public static synchronized void resetToDefault() {
        if (cliPath != null) {
            Path path = toExistingDirectory(cliPath);
            if (path != null) {
                root = path;
                source = SOURCE_CLI;
                reloadPermission();
                return;
            }
        }
        root = normalizeCwd();
        source = SOURCE_CWD;
        reloadPermission();
    }

    /** 当前工作区根目录（绝对路径）。 */
    public static Path getRoot() {
        return root;
    }

    /** 当前工作区来源说明。 */
    public static String getSource() {
        return source;
    }

    /** 启动参数指定的工作区（未指定时返回 null）。 */
    public static String getCliPath() {
        return cliPath;
    }

    /** 当前修改权限：readonly / workspace / approval / full。 */
    public static String getPermission() {
        return permission;
    }

    /** 修改权限的中文说明。 */
    public static String getPermissionLabel() {
        switch (permission) {
            case PERMISSION_READONLY:
                return "只读模式（readonly）";
            case PERMISSION_FULL:
                return "完全权限（full）";
            case PERMISSION_APPROVAL:
                return "工作区外审批（approval）";
            default:
                return "工作区修改（workspace）";
        }
    }

    /** 当前工作区的 settings.properties 路径（权限与其它配置都存这里）。 */
    public static Path getSettingsFile() {
        return root.resolve(SETTINGS_RELATIVE_PATH).normalize();
    }

    /** 当前工作区的权限配置文件路径（与 settings.properties 同一文件）。 */
    public static Path getPermissionFile() {
        return getSettingsFile();
    }

    /**
     * 校验是否为已存在的绝对目录（不改变当前工作区）。
     */
    public static boolean isValidDirectory(String directory) {
        return toExistingDirectory(directory) != null;
    }

    /**
     * 把用户路径解析为绝对路径：空值返回工作区根目录；绝对路径原样规范化；相对路径基于工作区解析。
     */
    public static Path resolve(String path) {
        if (isBlank(path)) {
            return root;
        }
        Path value = Paths.get(path.trim());
        return value.isAbsolute() ? value.normalize() : root.resolve(value).normalize();
    }

    /** 路径是否位于当前工作区内。 */
    public static boolean isInside(Path path) {
        return path.toAbsolutePath().normalize().startsWith(root);
    }

    /**
     * 该路径在当前权限模式下修改时是否需要弹窗审批（仅 approval 模式且在工作区外时为 true）。
     */
    public static boolean requiresApproval(Path path) {
        return PERMISSION_APPROVAL.equals(permission) && !isInside(path);
    }

    /**
     * 检查是否允许修改指定路径（写操作前调用）。
     *
     * @param path 已解析的绝对路径
     * @return null 表示允许；否则返回错误消息
     */
    public static String checkWrite(Path path) {
        Path absolute = path.toAbsolutePath().normalize();
        switch (permission) {
            case PERMISSION_READONLY:
                return "✗ 当前工作区为只读模式（readonly），禁止修改文件: " + absolute
                        + "\n可用 dlqq_workspace_permission_set 切换为 workspace、approval 或 full";
            case PERMISSION_FULL:
                return null;
            case PERMISSION_APPROVAL:
                return null; // 工作区外由调用方弹窗审批
            default:
                if (!isInside(absolute)) {
                    return "✗ 当前工作区权限为“工作区修改”（workspace），目标在工作区外: " + absolute
                            + "\n可用 dlqq_workspace_permission_set 切换为 approval 或 full";
                }
                return null;
        }
    }

    /**
     * 设置当前工作区的修改权限，并持久化到工作区的 settings.properties（UTF-8，保留其它配置与注释）。
     *
     * @param mode readonly / workspace / approval / full
     * @throws IllegalArgumentException 模式非法或写入配置失败
     */
    public static synchronized void setPermission(String mode) {
        String normalized = normalizePermission(mode);
        if (normalized == null) {
            throw new IllegalArgumentException("权限模式必须是 readonly、workspace、approval 或 full: " + mode);
        }
        try {
            writeSettingsValue(getSettingsFile(), PERMISSION_KEY, normalized);
        } catch (IOException e) {
            throw new IllegalArgumentException("写入工作区权限配置失败: " + e.getMessage());
        }
        permission = normalized;
    }

    /** 从当前工作区配置文件重新加载权限（文件不存在或非法时使用 workspace）。 */
    private static void reloadPermission() {
        permission = PERMISSION_WORKSPACE;
        String value = readSettingsValue(getSettingsFile(), PERMISSION_KEY);
        String normalized = normalizePermission(value);
        if (normalized != null) {
            permission = normalized;
        }
    }

    /**
     * 按 UTF-8 行格式读取配置值；文件或键不存在时返回 null。
     */
    private static String readSettingsValue(Path settingsFile, String key) {
        if (!Files.isRegularFile(settingsFile)) {
            return null;
        }
        try {
            for (String line : Files.readAllLines(settingsFile, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.startsWith(key)) {
                    int equalIndex = trimmed.indexOf('=');
                    if (equalIndex > 0 && trimmed.substring(0, equalIndex).trim().equals(key)) {
                        return trimmed.substring(equalIndex + 1).trim();
                    }
                }
            }
        } catch (IOException ignored) {
            // 读取失败时按默认值处理
        }
        return null;
    }

    /**
     * 按 UTF-8 行格式写入/更新配置键，保留其它行与注释。
     */
    private static void writeSettingsValue(Path settingsFile, String key, String value) throws IOException {
        Path parent = settingsFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        List<String> lines = new ArrayList<>();
        if (Files.isRegularFile(settingsFile)) {
            lines.addAll(Files.readAllLines(settingsFile, StandardCharsets.UTF_8));
        }
        boolean replaced = false;
        for (int i = 0; i < lines.size(); i++) {
            String trimmed = lines.get(i).trim();
            int equalIndex = trimmed.indexOf('=');
            if (equalIndex > 0 && trimmed.substring(0, equalIndex).trim().equals(key)) {
                lines.set(i, key + "=" + value);
                replaced = true;
            }
        }
        if (!replaced) {
            lines.add(key + "=" + value);
        }
        Files.write(settingsFile, lines, StandardCharsets.UTF_8);
    }

    private static String normalizePermission(String mode) {
        if (mode == null) {
            return null;
        }
        String value = mode.trim().toLowerCase();
        switch (value) {
            case PERMISSION_READONLY:
            case PERMISSION_WORKSPACE:
            case PERMISSION_APPROVAL:
            case PERMISSION_FULL:
                return value;
            default:
                return null;
        }
    }

    private static Path normalizeCwd() {
        return Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
    }

    private static Path toExistingDirectory(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            Path path = Paths.get(value.trim());
            if (!path.isAbsolute()) {
                return null;
            }
            path = path.normalize();
            if (!Files.isDirectory(path)) {
                return null;
            }
            return path;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

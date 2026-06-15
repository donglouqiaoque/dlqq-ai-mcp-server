package com.donglouqiaoque.aitools.config;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

/**
 * 配置管理器 - 管理AI工具的各类配置
 * 支持多进程并发访问（使用文件锁）
 */
public class ConfigManager {

    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.aitools";
    private static final String CONFIG_FILE = CONFIG_DIR + "/config.properties";
    private static final String LOCK_FILE = CONFIG_DIR + "/.config.lock";
    private static final String DEFAULT_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String DEFAULT_MODEL = "gpt-3.5-turbo";
    private static final long LOCK_TIMEOUT_MS = 5000; // 5秒超时

    private final Properties properties;
    private static ConfigManager instance;

    private ConfigManager() {
        this.properties = new Properties();
        loadConfig();
    }

    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    /**
     * 获取文件锁（带超时）
     */
    private FileLock acquireLock(FileChannel channel) throws IOException {
        long startTime = System.currentTimeMillis();
        FileLock lock = null;
        while (lock == null) {
            try {
                lock = channel.tryLock();
                if (lock != null) {
                    return lock;
                }
            } catch (Exception e) {
                // 锁被其他进程持有，继续等待
            }
            if (System.currentTimeMillis() - startTime > LOCK_TIMEOUT_MS) {
                throw new IOException("获取配置文件锁超时");
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("获取锁被中断");
            }
        }
        return lock;
    }

    /**
     * 加载配置文件（带文件锁）
     */
    private void loadConfig() {
        Path configPath = Paths.get(CONFIG_FILE);
        if (!Files.exists(configPath)) {
            return;
        }

        // 使用锁文件防止并发读写
        Path lockPath = Paths.get(LOCK_FILE);
        try {
            Files.createDirectories(Paths.get(CONFIG_DIR));
            try (FileChannel lockChannel = FileChannel.open(lockPath,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 FileLock lock = acquireLock(lockChannel)) {
                // 重新读取（持有锁后）
                if (Files.exists(configPath)) {
                    try (InputStream is = new FileInputStream(CONFIG_FILE)) {
                        properties.load(is);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("加载配置文件失败: " + e.getMessage());
        }
    }

    /**
     * 保存配置到文件（带文件锁）
     */
    public void saveConfig() {
        Path lockPath = Paths.get(LOCK_FILE);
        try {
            Files.createDirectories(Paths.get(CONFIG_DIR));
            try (FileChannel lockChannel = FileChannel.open(lockPath,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 FileLock lock = acquireLock(lockChannel);
                 OutputStream os = new FileOutputStream(CONFIG_FILE)) {
                properties.store(os, "AI Tools Configuration");
            }
        } catch (IOException e) {
            System.err.println("保存配置文件失败: " + e.getMessage());
        }
    }

    /**
     * 获取API密钥
     */
    public String getApiKey() {
        return properties.getProperty("api.key", "");
    }

    /**
     * 设置API密钥
     */
    public void setApiKey(String apiKey) {
        properties.setProperty("api.key", apiKey);
        saveConfig();
    }

    /**
     * 获取API地址
     */
    public String getApiUrl() {
        return properties.getProperty("api.url", DEFAULT_API_URL);
    }

    /**
     * 设置API地址
     */
    public void setApiUrl(String apiUrl) {
        properties.setProperty("api.url", apiUrl);
        saveConfig();
    }

    /**
     * 获取默认模型
     */
    public String getModel() {
        return properties.getProperty("api.model", DEFAULT_MODEL);
    }

    /**
     * 设置默认模型
     */
    public void setModel(String model) {
        properties.setProperty("api.model", model);
        saveConfig();
    }

    /**
     * 获取配置值
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * 设置配置值
     */
    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
        saveConfig();
    }

    /**
     * 检查是否已配置API密钥
     */
    public boolean hasApiKey() {
        String apiKey = getApiKey();
        return apiKey != null && !apiKey.trim().isEmpty();
    }
}

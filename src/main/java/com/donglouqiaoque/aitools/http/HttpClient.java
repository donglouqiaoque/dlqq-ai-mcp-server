package com.donglouqiaoque.aitools.http;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * HTTP客户端 - 用于发送HTTP请求
 */
public class HttpClient {

    private static final int DEFAULT_TIMEOUT = 30000; // 30秒超时
    private static final int CONNECT_TIMEOUT = 10000; // 10秒连接超时

    /**
     * 发送POST请求
     *
     * @param url     请求地址
     * @param body    请求体
     * @param headers 请求头
     * @return 响应内容
     * @throws IOException 请求异常
     */
    public String post(String url, String body, Map<String, String> headers) throws IOException {
        HttpURLConnection conn = null;
        try {
            URL requestUrl = new URL(url);
            conn = (HttpURLConnection) requestUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(DEFAULT_TIMEOUT);

            // 设置请求头
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            // 发送请求体
            if (body != null && !body.isEmpty()) {
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = body.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }

            // 读取响应
            int responseCode = conn.getResponseCode();
            InputStream is;
            if (responseCode >= 200 && responseCode < 300) {
                is = conn.getInputStream();
            } else {
                is = conn.getErrorStream();
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }

            return response.toString();

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 发送POST请求（带默认Content-Type）
     */
    public String post(String url, String body) throws IOException {
        return post(url, body, Map.of("Content-Type", "application/json"));
    }

    /**
     * 发送GET请求
     *
     * @param url     请求地址
     * @param headers 请求头
     * @return 响应内容
     * @throws IOException 请求异常
     */
    public String get(String url, Map<String, String> headers) throws IOException {
        HttpURLConnection conn = null;
        try {
            URL requestUrl = new URL(url);
            conn = (HttpURLConnection) requestUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(DEFAULT_TIMEOUT);

            // 设置请求头
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            // 读取响应
            int responseCode = conn.getResponseCode();
            InputStream is;
            if (responseCode >= 200 && responseCode < 300) {
                is = conn.getInputStream();
            } else {
                is = conn.getErrorStream();
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }

            return response.toString();

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 发送GET请求（无额外请求头）
     */
    public String get(String url) throws IOException {
        return get(url, null);
    }
}

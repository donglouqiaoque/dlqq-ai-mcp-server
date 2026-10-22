package com.donglouqiaoque.dlqqaimcpserver.mcp.protocol;

import java.util.Map;

/**
 * JSON-RPC消息基类
 */
public abstract class JsonRpcMessage {
    protected String jsonrpc = "2.0";

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    /**
     * JSON-RPC请求消息
     */
    public static class Request extends JsonRpcMessage {
        private Object id;
        private String method;
        private Map<String, Object> params;

        public Request() {}

        public Request(Object id, String method, Map<String, Object> params) {
            this.id = id;
            this.method = method;
            this.params = params;
        }

        public Object getId() {
            return id;
        }

        public void setId(Object id) {
            this.id = id;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public Map<String, Object> getParams() {
            return params;
        }

        public void setParams(Map<String, Object> params) {
            this.params = params;
        }

        public boolean isNotification() {
            return id == null;
        }
    }

    /**
     * JSON-RPC响应消息
     */
    public static class Response extends JsonRpcMessage {
        private Object id;
        private Object result;
        private JsonRpcError error;

        public Response() {}

        public Response(Object id, Object result) {
            this.id = id;
            this.result = result;
        }

        public Response(Object id, JsonRpcError error) {
            this.id = id;
            this.error = error;
        }

        public Object getId() {
            return id;
        }

        public void setId(Object id) {
            this.id = id;
        }

        public Object getResult() {
            return result;
        }

        public void setResult(Object result) {
            this.result = result;
        }

        public JsonRpcError getError() {
            return error;
        }

        public void setError(JsonRpcError error) {
            this.error = error;
        }

        public boolean isError() {
            return error != null;
        }

        public static Response success(Object id, Object result) {
            return new Response(id, result);
        }

        public static Response error(Object id, int code, String message) {
            return new Response(id, new JsonRpcError(code, message));
        }

        public static Response error(Object id, int code, String message, Object data) {
            return new Response(id, new JsonRpcError(code, message, data));
        }
    }

    /**
     * JSON-RPC错误
     */
    public static class JsonRpcError {
        private int code;
        private String message;
        private Object data;

        public JsonRpcError() {}

        public JsonRpcError(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public JsonRpcError(int code, String message, Object data) {
            this.code = code;
            this.message = message;
            this.data = data;
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }
    }

    // 标准错误码
    public static final int ERROR_PARSE_ERROR = -32700;
    public static final int ERROR_INVALID_REQUEST = -32600;
    public static final int ERROR_METHOD_NOT_FOUND = -32601;
    public static final int ERROR_INVALID_PARAMS = -32602;
    public static final int ERROR_INTERNAL_ERROR = -32603;
}

package com.donglouqiaoque.aitools.mcp.protocol;

import java.util.List;
import java.util.Map;

/**
 * MCP协议模式定义
 */
public class McpSchema {

    // MCP协议版本
    public static final String LATEST_PROTOCOL_VERSION = "2024-11-05";
    
    // 支持的能力
    public static class Capabilities {
        private boolean tools = true;
        private boolean resources = false;
        private boolean prompts = false;
        private boolean logging = true;

        public Capabilities() {}

        public boolean isTools() {
            return tools;
        }

        public void setTools(boolean tools) {
            this.tools = tools;
        }

        public boolean isResources() {
            return resources;
        }

        public void setResources(boolean resources) {
            this.resources = resources;
        }

        public boolean isPrompts() {
            return prompts;
        }

        public void setPrompts(boolean prompts) {
            this.prompts = prompts;
        }

        public boolean isLogging() {
            return logging;
        }

        public void setLogging(boolean logging) {
            this.logging = logging;
        }
    }

    // 初始化请求
    public static class InitializeRequest {
        private String protocolVersion;
        private Capabilities capabilities;
        private ClientInfo clientInfo;

        public String getProtocolVersion() {
            return protocolVersion;
        }

        public void setProtocolVersion(String protocolVersion) {
            this.protocolVersion = protocolVersion;
        }

        public Capabilities getCapabilities() {
            return capabilities;
        }

        public void setCapabilities(Capabilities capabilities) {
            this.capabilities = capabilities;
        }

        public ClientInfo getClientInfo() {
            return clientInfo;
        }

        public void setClientInfo(ClientInfo clientInfo) {
            this.clientInfo = clientInfo;
        }
    }

    // 客户端信息
    public static class ClientInfo {
        private String name;
        private String version;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }

    // 服务器信息
    public static class ServerInfo {
        private String name;
        private String version;

        public ServerInfo() {}

        public ServerInfo(String name, String version) {
            this.name = name;
            this.version = version;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }

    // 初始化结果
    public static class InitializeResult {
        private String protocolVersion;
        private Capabilities capabilities;
        private ServerInfo serverInfo;

        public InitializeResult() {}

        public InitializeResult(String protocolVersion, Capabilities capabilities, ServerInfo serverInfo) {
            this.protocolVersion = protocolVersion;
            this.capabilities = capabilities;
            this.serverInfo = serverInfo;
        }

        public String getProtocolVersion() {
            return protocolVersion;
        }

        public void setProtocolVersion(String protocolVersion) {
            this.protocolVersion = protocolVersion;
        }

        public Capabilities getCapabilities() {
            return capabilities;
        }

        public void setCapabilities(Capabilities capabilities) {
            this.capabilities = capabilities;
        }

        public ServerInfo getServerInfo() {
            return serverInfo;
        }

        public void setServerInfo(ServerInfo serverInfo) {
            this.serverInfo = serverInfo;
        }
    }

    // 工具定义
    public static class Tool {
        private String name;
        private String description;
        private ToolInputSchema inputSchema;

        public Tool() {}

        public Tool(String name, String description, ToolInputSchema inputSchema) {
            this.name = name;
            this.description = description;
            this.inputSchema = inputSchema;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public ToolInputSchema getInputSchema() {
            return inputSchema;
        }

        public void setInputSchema(ToolInputSchema inputSchema) {
            this.inputSchema = inputSchema;
        }
    }

    // 工具输入模式
    public static class ToolInputSchema {
        private String type = "object";
        private Map<String, Object> properties;
        private List<String> required;

        public ToolInputSchema() {}

        public ToolInputSchema(Map<String, Object> properties, List<String> required) {
            this.properties = properties;
            this.required = required;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Map<String, Object> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, Object> properties) {
            this.properties = properties;
        }

        public List<String> getRequired() {
            return required;
        }

        public void setRequired(List<String> required) {
            this.required = required;
        }
    }

    // 工具列表结果
    public static class ListToolsResult {
        private List<Tool> tools;

        public ListToolsResult() {}

        public ListToolsResult(List<Tool> tools) {
            this.tools = tools;
        }

        public List<Tool> getTools() {
            return tools;
        }

        public void setTools(List<Tool> tools) {
            this.tools = tools;
        }
    }

    // 调用工具请求
    public static class CallToolRequest {
        private String name;
        private Map<String, Object> arguments;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Map<String, Object> getArguments() {
            return arguments;
        }

        public void setArguments(Map<String, Object> arguments) {
            this.arguments = arguments;
        }
    }

    // 调用工具结果
    public static class CallToolResult {
        private List<ToolContent> content;
        private boolean isError;

        public CallToolResult() {}

        public CallToolResult(List<ToolContent> content, boolean isError) {
            this.content = content;
            this.isError = isError;
        }

        public List<ToolContent> getContent() {
            return content;
        }

        public void setContent(List<ToolContent> content) {
            this.content = content;
        }

        public boolean isError() {
            return isError;
        }

        public void setError(boolean isError) {
            this.isError = isError;
        }
    }

    // 工具内容
    public static class ToolContent {
        private String type;
        private String text;

        public ToolContent() {}

        public ToolContent(String type, String text) {
            this.type = type;
            this.text = text;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public static ToolContent text(String text) {
            return new ToolContent("text", text);
        }

        public static ToolContent error(String text) {
            ToolContent content = new ToolContent("text", text);
            return content;
        }
    }

    // 资源定义
    public static class Resource {
        private String uri;
        private String name;
        private String description;
        private String mimeType;

        public Resource() {}

        public Resource(String uri, String name, String description, String mimeType) {
            this.uri = uri;
            this.name = name;
            this.description = description;
            this.mimeType = mimeType;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }
    }

    // 资源列表结果
    public static class ListResourcesResult {
        private List<Resource> resources;

        public ListResourcesResult() {}

        public ListResourcesResult(List<Resource> resources) {
            this.resources = resources;
        }

        public List<Resource> getResources() {
            return resources;
        }

        public void setResources(List<Resource> resources) {
            this.resources = resources;
        }
    }

    // 提示定义
    public static class Prompt {
        private String name;
        private String description;
        private List<PromptArgument> arguments;

        public Prompt() {}

        public Prompt(String name, String description, List<PromptArgument> arguments) {
            this.name = name;
            this.description = description;
            this.arguments = arguments;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<PromptArgument> getArguments() {
            return arguments;
        }

        public void setArguments(List<PromptArgument> arguments) {
            this.arguments = arguments;
        }
    }

    // 提示参数
    public static class PromptArgument {
        private String name;
        private String description;
        private boolean required;

        public PromptArgument() {}

        public PromptArgument(String name, String description, boolean required) {
            this.name = name;
            this.description = description;
            this.required = required;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }
    }

    // 提示列表结果
    public static class ListPromptsResult {
        private List<Prompt> prompts;

        public ListPromptsResult() {}

        public ListPromptsResult(List<Prompt> prompts) {
            this.prompts = prompts;
        }

        public List<Prompt> getPrompts() {
            return prompts;
        }

        public void setPrompts(List<Prompt> prompts) {
            this.prompts = prompts;
        }
    }

    // 获取提示结果
    public static class GetPromptResult {
        private String description;
        private List<PromptMessage> messages;

        public GetPromptResult() {}

        public GetPromptResult(String description, List<PromptMessage> messages) {
            this.description = description;
            this.messages = messages;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<PromptMessage> getMessages() {
            return messages;
        }

        public void setMessages(List<PromptMessage> messages) {
            this.messages = messages;
        }
    }

    // 提示消息
    public static class PromptMessage {
        private String role;
        private PromptContent content;

        public PromptMessage() {}

        public PromptMessage(String role, PromptContent content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public PromptContent getContent() {
            return content;
        }

        public void setContent(PromptContent content) {
            this.content = content;
        }
    }

    // 提示内容
    public static class PromptContent {
        private String type;
        private String text;

        public PromptContent() {}

        public PromptContent(String type, String text) {
            this.type = type;
            this.text = text;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}

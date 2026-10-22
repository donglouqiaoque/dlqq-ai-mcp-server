package com.donglouqiaoque.dlqqaimcpserver.mcp.protocol;

import java.util.List;
import java.util.Map;

/**
 * MCP协议模式定义
 */
public class McpSchema {

    // MCP协议版本 - 更新到最新版本，服务器将根据客户端请求返回相应版本
    public static final String LATEST_PROTOCOL_VERSION = "2025-11-25";
    
    // 支持的能力 - 2025-11-25协议更新
    public static class Capabilities {
        // 工具能力
        private ToolsCapability tools;
        // 资源能力
        private ResourcesCapability resources;
        // 提示能力
        private PromptsCapability prompts;
        // 日志能力
        private LoggingCapability logging;
        // 自动完成能力（可选）
        private CompletionsCapability completions;
        // 实验性功能（可选）
        private Map<String, Object> experimental;

        public Capabilities() {
            this.tools = new ToolsCapability();
            this.logging = new LoggingCapability();
        }

        public ToolsCapability getTools() {
            return tools;
        }

        public void setTools(ToolsCapability tools) {
            this.tools = tools;
        }

        // 向后兼容：设置方法
        public void setTools(boolean enabled) {
            if (enabled) {
                if (this.tools == null) {
                    this.tools = new ToolsCapability();
                }
                this.tools.setListChanged(true);
            } else {
                this.tools = null;
            }
        }

        // 向后兼容：判断方法
        public boolean isTools() {
            return tools != null;
        }

        public ResourcesCapability getResources() {
            return resources;
        }

        public void setResources(ResourcesCapability resources) {
            this.resources = resources;
        }

        // 向后兼容：设置方法
        public void setResources(boolean enabled) {
            if (enabled) {
                this.resources = new ResourcesCapability();
            } else {
                this.resources = null;
            }
        }

        // 向后兼容：判断方法
        public boolean isResources() {
            return resources != null;
        }

        public PromptsCapability getPrompts() {
            return prompts;
        }

        public void setPrompts(PromptsCapability prompts) {
            this.prompts = prompts;
        }

        // 向后兼容：设置方法
        public void setPrompts(boolean enabled) {
            if (enabled) {
                this.prompts = new PromptsCapability();
            } else {
                this.prompts = null;
            }
        }

        // 向后兼容：判断方法
        public boolean isPrompts() {
            return prompts != null;
        }

        public LoggingCapability getLogging() {
            return logging;
        }

        public void setLogging(LoggingCapability logging) {
            this.logging = logging;
        }

        // 向后兼容：设置方法
        public void setLogging(boolean enabled) {
            if (enabled) {
                this.logging = new LoggingCapability();
            } else {
                this.logging = null;
            }
        }

        // 向后兼容：判断方法
        public boolean isLogging() {
            return logging != null;
        }

        public CompletionsCapability getCompletions() {
            return completions;
        }

        public void setCompletions(CompletionsCapability completions) {
            this.completions = completions;
        }

        public Map<String, Object> getExperimental() {
            return experimental;
        }

        public void setExperimental(Map<String, Object> experimental) {
            this.experimental = experimental;
        }
    }

    // 工具能力
    public static class ToolsCapability {
        private boolean listChanged = false;

        public ToolsCapability() {}

        public boolean isListChanged() {
            return listChanged;
        }

        public void setListChanged(boolean listChanged) {
            this.listChanged = listChanged;
        }
    }

    // 资源能力
    public static class ResourcesCapability {
        private boolean subscribe = false;
        private boolean listChanged = false;

        public ResourcesCapability() {}

        public boolean isSubscribe() {
            return subscribe;
        }

        public void setSubscribe(boolean subscribe) {
            this.subscribe = subscribe;
        }

        public boolean isListChanged() {
            return listChanged;
        }

        public void setListChanged(boolean listChanged) {
            this.listChanged = listChanged;
        }
    }

    // 提示能力
    public static class PromptsCapability {
        private boolean listChanged = false;

        public PromptsCapability() {}

        public boolean isListChanged() {
            return listChanged;
        }

        public void setListChanged(boolean listChanged) {
            this.listChanged = listChanged;
        }
    }

    // 日志能力
    public static class LoggingCapability {
        public LoggingCapability() {}
    }

    // 自动完成能力（可选）
    public static class CompletionsCapability {
        public CompletionsCapability() {}
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

    // 客户端信息 - 2025-11-25协议更新
    public static class ClientInfo {
        private String name;
        private String version;
        // 可选字段
        private String title;
        private String description;
        private List<Icon> icons;
        private String websiteUrl;

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

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<Icon> getIcons() {
            return icons;
        }

        public void setIcons(List<Icon> icons) {
            this.icons = icons;
        }

        public String getWebsiteUrl() {
            return websiteUrl;
        }

        public void setWebsiteUrl(String websiteUrl) {
            this.websiteUrl = websiteUrl;
        }
    }

    // 服务器信息 - 2025-11-25协议更新
    public static class ServerInfo {
        private String name;
        private String version;
        // 可选字段
        private String title;
        private String description;
        private List<Icon> icons;
        private String websiteUrl;

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

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<Icon> getIcons() {
            return icons;
        }

        public void setIcons(List<Icon> icons) {
            this.icons = icons;
        }

        public String getWebsiteUrl() {
            return websiteUrl;
        }

        public void setWebsiteUrl(String websiteUrl) {
            this.websiteUrl = websiteUrl;
        }
    }

    // 图标定义
    public static class Icon {
        private String src;
        private String mimeType;
        private List<String> sizes;

        public Icon() {}

        public String getSrc() {
            return src;
        }

        public void setSrc(String src) {
            this.src = src;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public List<String> getSizes() {
            return sizes;
        }

        public void setSizes(List<String> sizes) {
            this.sizes = sizes;
        }
    }

    // 初始化结果 - 2025-11-25协议更新
    public static class InitializeResult {
        private String protocolVersion;
        private Capabilities capabilities;
        private ServerInfo serverInfo;
        // 可选：服务器提供的指令/提示
        private String instructions;

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

        public String getInstructions() {
            return instructions;
        }

        public void setInstructions(String instructions) {
            this.instructions = instructions;
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

    // ==================== 2025-11-25协议新增：通知类 ====================

    // 资源更新通知
    public static class ResourceUpdatedNotification {
        private String uri;

        public ResourceUpdatedNotification() {}

        public ResourceUpdatedNotification(String uri) {
            this.uri = uri;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }
    }

    // 资源列表变更通知
    public static class ResourceListChangedNotification {
        // 空通知，表示资源列表已变更
        public ResourceListChangedNotification() {}
    }

    // 提示列表变更通知
    public static class PromptListChangedNotification {
        // 空通知，表示提示列表已变更
        public PromptListChangedNotification() {}
    }

    // 工具列表变更通知
    public static class ToolListChangedNotification {
        // 空通知，表示工具列表已变更
        public ToolListChangedNotification() {}
    }

    // 日志消息通知（服务器向客户端发送）
    public static class LogMessageNotification {
        private String level;  // debug, info, warning, error
        private String logger;
        private Object data;

        public LogMessageNotification() {}

        public LogMessageNotification(String level, String logger, Object data) {
            this.level = level;
            this.logger = logger;
            this.data = data;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public String getLogger() {
            return logger;
        }

        public void setLogger(String logger) {
            this.logger = logger;
        }

        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }
    }

    // 取消订阅请求
    public static class UnsubscribeRequest {
        private String uri;

        public UnsubscribeRequest() {}

        public UnsubscribeRequest(String uri) {
            this.uri = uri;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }
    }

    // 取消订阅结果（空）
    public static class UnsubscribeResult {
        public UnsubscribeResult() {}
    }
}

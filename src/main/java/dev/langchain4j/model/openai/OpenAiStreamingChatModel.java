package dev.langchain4j.model.openai;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.internal.ExceptionMapper;
import dev.langchain4j.internal.ToolExecutionRequestBuilder;
import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.DefaultChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.internal.OpenAiClient;
import dev.langchain4j.model.openai.internal.chat.*;
import dev.langchain4j.model.openai.internal.shared.StreamOptions;
import dev.langchain4j.model.openai.spi.OpenAiStreamingChatModelBuilderFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static dev.langchain4j.internal.InternalStreamingChatResponseHandlerUtils.withLoggingExceptions;
import static dev.langchain4j.internal.Utils.*;
import static dev.langchain4j.model.ModelProvider.OPEN_AI;
import static dev.langchain4j.model.openai.internal.OpenAiUtils.*;
import static dev.langchain4j.spi.ServiceHelper.loadFactories;
import static java.time.Duration.ofSeconds;

/**
 * Represents an OpenAI language model with a chat completion interface, such as gpt-4o-mini and o3.
 * The model's response is streamed token by token and should be handled with {@link StreamingResponseHandler}.
 * You can find description of parameters <a href="https://platform.openai.com/docs/api-reference/chat/create">here</a>.
 */
public class OpenAiStreamingChatModel implements StreamingChatModel {

    private final OpenAiClient client;
    private final OpenAiChatRequestParameters defaultRequestParameters;
    private final Boolean strictJsonSchema;
    private final Boolean strictTools;
    private final List<ChatModelListener> listeners;

    public OpenAiStreamingChatModel(OpenAiStreamingChatModelBuilder builder) {
        this.client = OpenAiClient.builder()
                .httpClientBuilder(builder.httpClientBuilder)
                .baseUrl(getOrDefault(builder.baseUrl, DEFAULT_OPENAI_URL))
                .apiKey(builder.apiKey)
                .organizationId(builder.organizationId)
                .projectId(builder.projectId)
                .connectTimeout(getOrDefault(builder.timeout, ofSeconds(15)))
                .readTimeout(getOrDefault(builder.timeout, ofSeconds(60)))
                .logRequests(getOrDefault(builder.logRequests, false))
                .logResponses(getOrDefault(builder.logResponses, false))
                .userAgent(DEFAULT_USER_AGENT)
                .customHeaders(builder.customHeaders)
                .build();

        ChatRequestParameters commonParameters;
        if (builder.defaultRequestParameters != null) {
            validate(builder.defaultRequestParameters);
            commonParameters = builder.defaultRequestParameters;
        } else {
            commonParameters = DefaultChatRequestParameters.EMPTY;
        }

        OpenAiChatRequestParameters openAiParameters;
        if (builder.defaultRequestParameters instanceof OpenAiChatRequestParameters openAiChatRequestParameters) {
            openAiParameters = openAiChatRequestParameters;
        } else {
            openAiParameters = OpenAiChatRequestParameters.EMPTY;
        }

        this.defaultRequestParameters = OpenAiChatRequestParameters.builder()
                // common parameters
                .modelName(getOrDefault(builder.modelName, commonParameters.modelName()))
                .temperature(getOrDefault(builder.temperature, commonParameters.temperature()))
                .topP(getOrDefault(builder.topP, commonParameters.topP()))
                .frequencyPenalty(getOrDefault(builder.frequencyPenalty, commonParameters.frequencyPenalty()))
                .presencePenalty(getOrDefault(builder.presencePenalty, commonParameters.presencePenalty()))
                .maxOutputTokens(getOrDefault(builder.maxTokens, commonParameters.maxOutputTokens()))
                .stopSequences(getOrDefault(builder.stop, commonParameters.stopSequences()))
                .toolSpecifications(commonParameters.toolSpecifications())
                .toolChoice(commonParameters.toolChoice())
                .responseFormat(getOrDefault(fromOpenAiResponseFormat(builder.responseFormat), commonParameters.responseFormat()))
                // OpenAI-specific parameters
                .maxCompletionTokens(getOrDefault(builder.maxCompletionTokens, openAiParameters.maxCompletionTokens()))
                .logitBias(getOrDefault(builder.logitBias, openAiParameters.logitBias()))
                .parallelToolCalls(getOrDefault(builder.parallelToolCalls, openAiParameters.parallelToolCalls()))
                .seed(getOrDefault(builder.seed, openAiParameters.seed()))
                .user(getOrDefault(builder.user, openAiParameters.user()))
                .store(getOrDefault(builder.store, openAiParameters.store()))
                .metadata(getOrDefault(builder.metadata, openAiParameters.metadata()))
                .serviceTier(getOrDefault(builder.serviceTier, openAiParameters.serviceTier()))
                .reasoningEffort(openAiParameters.reasoningEffort())
                .build();
        this.strictJsonSchema = getOrDefault(builder.strictJsonSchema, false);
        this.strictTools = getOrDefault(builder.strictTools, false);
        this.listeners = copy(builder.listeners);
    }

    @Override
    public OpenAiChatRequestParameters defaultRequestParameters() {
        return defaultRequestParameters;
    }

    @Override
    public void doChat(ChatRequest chatRequest, StreamingChatResponseHandler handler) {
        // 取请求参数并校验
        OpenAiChatRequestParameters parameters = (OpenAiChatRequestParameters) chatRequest.parameters();
        validate(parameters);

        // 构建 OpenAI 流式对话请求：启用流式输出 + 返回 token 用量
        ChatCompletionRequest openAiRequest =
                toOpenAiChatRequest(chatRequest, parameters, strictTools, strictJsonSchema)
                        .stream(true)
                        .streamOptions(StreamOptions.builder()
                                .includeUsage(true)
                                .build())
                        .build();

        // 构建响应累积器（用于流式拼装完整响应）
        OpenAiStreamingResponseBuilder openAiResponseBuilder = new OpenAiStreamingResponseBuilder();
        // 构建工具请求累积器（用于流式拼装工具调用参数）
        ToolExecutionRequestBuilder toolBuilder = new ToolExecutionRequestBuilder();

        // 发起流式请求并注册回调
        client.chatCompletion(openAiRequest)
                // 每收到一个流式分片：累积响应并解析出文本/工具增量回调给处理器
                .onPartialResponse(partialResponse -> {
                    openAiResponseBuilder.append(partialResponse);
                    handle(partialResponse, toolBuilder, handler);
                })
                // 流式完成后：若有工具请求则回调完成事件，最后回调完整响应
                .onComplete(() -> {
                    // 工具请求已完整则通知处理器
                    if (toolBuilder.hasToolExecutionRequests()) {
                        try {
                            handler.onCompleteToolExecutionRequest(toolBuilder.index(), toolBuilder.build());
                        } catch (Exception e) {
                            withLoggingExceptions(() -> handler.onError(e));
                        }
                    }
                    // 构建并回调完整响应
                    ChatResponse chatResponse = openAiResponseBuilder.build();
                    try {
                        handler.onCompleteResponse(chatResponse);
                    } catch (Exception e) {
                        withLoggingExceptions(() -> handler.onError(e));
                    }
                })
                // 出错时映射异常并回调错误处理器
                .onError(throwable -> {
                    RuntimeException mappedException = ExceptionMapper.DEFAULT.mapException(throwable);
                    withLoggingExceptions(() -> handler.onError(mappedException));
                })
                // 启动请求
                .execute();
    }

    /**
     * 处理单个流式分片：提取文本增量或工具参数增量并回调给上层处理器
     */
    private static void handle(ChatCompletionResponse partialResponse,
                               ToolExecutionRequestBuilder toolBuilder,
                               StreamingChatResponseHandler handler) {
        // 空分片直接忽略
        if (partialResponse == null) {
            return;
        }

        // 取候选响应列表
        List<ChatCompletionChoice> choices = partialResponse.choices();
        // 无候选则忽略
        if (choices == null || choices.isEmpty()) {
            return;
        }

        // 取第一个候选
        ChatCompletionChoice chatCompletionChoice = choices.get(0);
        // 候选为空则忽略
        if (chatCompletionChoice == null) {
            return;
        }

        // 取 delta（增量数据）
        Delta delta = chatCompletionChoice.delta();
        // 无增量则忽略
        if (delta == null) {
            return;
        }

        // 提取文本增量内容
        String content = delta.content();
        // 文本非空则回调文本增量给处理器
        if (!isNullOrEmpty(content)) {
            try {
                handler.onPartialResponse(content);
            } catch (Exception e) {
                withLoggingExceptions(() -> handler.onError(e));
            }
        }
        List<ToolCall> toolCalls = delta.toolCalls();
        if (toolCalls != null) {
            for (ToolCall toolCall : toolCalls) {

                int index = toolCall.index();
                if (toolBuilder.index() != index) {
                    try {
                        handler.onCompleteToolExecutionRequest(toolBuilder.index(), toolBuilder.build());
                    } catch (Exception e) {
                        withLoggingExceptions(() -> handler.onError(e));
                    }
                    toolBuilder.updateIndex(index);
                }

                String id = toolBuilder.updateId(toolCall.id());
                String name = toolBuilder.updateName(toolCall.function().name());

                String partialArguments = toolCall.function().arguments();
                if (isNotNullOrEmpty(partialArguments)) {
                    toolBuilder.appendArguments(partialArguments);

                    ToolExecutionRequest partialToolExecutionRequest = ToolExecutionRequest.builder()
                            .id(id)
                            .name(name)
                            .arguments(partialArguments)
                            .build();
                    try {
                        handler.onPartialToolExecutionRequest(index, partialToolExecutionRequest);
                    } catch (Exception e) {
                        withLoggingExceptions(() -> handler.onError(e));
                    }
                }
            }
        }
    }

    @Override
    public List<ChatModelListener> listeners() {
        return listeners;
    }

    @Override
    public ModelProvider provider() {
        return OPEN_AI;
    }

    public static OpenAiStreamingChatModelBuilder builder() {
        for (OpenAiStreamingChatModelBuilderFactory factory : loadFactories(OpenAiStreamingChatModelBuilderFactory.class)) {
            return factory.get();
        }
        return new OpenAiStreamingChatModelBuilder();
    }

    public static class OpenAiStreamingChatModelBuilder {

        private HttpClientBuilder httpClientBuilder;
        private String baseUrl;
        private String apiKey;
        private String organizationId;
        private String projectId;

        private ChatRequestParameters defaultRequestParameters;
        private String modelName;
        private Double temperature;
        private Double topP;
        private List<String> stop;
        private Integer maxTokens;
        private Integer maxCompletionTokens;
        private Double presencePenalty;
        private Double frequencyPenalty;
        private Map<String, Integer> logitBias;
        private String responseFormat;
        private Boolean strictJsonSchema;
        private Integer seed;
        private String user;
        private Boolean strictTools;
        private Boolean parallelToolCalls;
        private Boolean store;
        private Map<String, String> metadata;
        private String serviceTier;
        private Duration timeout;
        private Boolean logRequests;
        private Boolean logResponses;
        private Map<String, String> customHeaders;
        private List<ChatModelListener> listeners;

        public OpenAiStreamingChatModelBuilder() {
            // This is public so it can be extended
        }

        public OpenAiStreamingChatModelBuilder httpClientBuilder(HttpClientBuilder httpClientBuilder) {
            this.httpClientBuilder = httpClientBuilder;
            return this;
        }

        /**
         * Sets default common {@link ChatRequestParameters} or OpenAI-specific {@link OpenAiChatRequestParameters}.
         * <br>
         * When a parameter is set via an individual builder method (e.g., {@link #modelName(String)}),
         * its value takes precedence over the same parameter set via {@link ChatRequestParameters}.
         */
        public OpenAiStreamingChatModelBuilder defaultRequestParameters(ChatRequestParameters parameters) {
            this.defaultRequestParameters = parameters;
            return this;
        }

        public OpenAiStreamingChatModelBuilder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public <OpenAiChatModelName> OpenAiStreamingChatModelBuilder modelName(OpenAiChatModelName modelName) {
            this.modelName = modelName.toString();
            return this;
        }

        public OpenAiStreamingChatModelBuilder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public OpenAiStreamingChatModelBuilder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public OpenAiStreamingChatModelBuilder organizationId(String organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        public OpenAiStreamingChatModelBuilder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public OpenAiStreamingChatModelBuilder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public OpenAiStreamingChatModelBuilder topP(Double topP) {
            this.topP = topP;
            return this;
        }

        public OpenAiStreamingChatModelBuilder stop(List<String> stop) {
            this.stop = stop;
            return this;
        }

        public OpenAiStreamingChatModelBuilder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public OpenAiStreamingChatModelBuilder maxCompletionTokens(Integer maxCompletionTokens) {
            this.maxCompletionTokens = maxCompletionTokens;
            return this;
        }

        public OpenAiStreamingChatModelBuilder presencePenalty(Double presencePenalty) {
            this.presencePenalty = presencePenalty;
            return this;
        }

        public OpenAiStreamingChatModelBuilder frequencyPenalty(Double frequencyPenalty) {
            this.frequencyPenalty = frequencyPenalty;
            return this;
        }

        public OpenAiStreamingChatModelBuilder logitBias(Map<String, Integer> logitBias) {
            this.logitBias = logitBias;
            return this;
        }

        public OpenAiStreamingChatModelBuilder responseFormat(String responseFormat) {
            this.responseFormat = responseFormat;
            return this;
        }

        public OpenAiStreamingChatModelBuilder strictJsonSchema(Boolean strictJsonSchema) {
            this.strictJsonSchema = strictJsonSchema;
            return this;
        }

        public OpenAiStreamingChatModelBuilder seed(Integer seed) {
            this.seed = seed;
            return this;
        }

        public OpenAiStreamingChatModelBuilder user(String user) {
            this.user = user;
            return this;
        }

        public OpenAiStreamingChatModelBuilder strictTools(Boolean strictTools) {
            this.strictTools = strictTools;
            return this;
        }

        public OpenAiStreamingChatModelBuilder parallelToolCalls(Boolean parallelToolCalls) {
            this.parallelToolCalls = parallelToolCalls;
            return this;
        }

        public OpenAiStreamingChatModelBuilder store(Boolean store) {
            this.store = store;
            return this;
        }

        public OpenAiStreamingChatModelBuilder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        public OpenAiStreamingChatModelBuilder serviceTier(String serviceTier) {
            this.serviceTier = serviceTier;
            return this;
        }

        public OpenAiStreamingChatModelBuilder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public OpenAiStreamingChatModelBuilder logRequests(Boolean logRequests) {
            this.logRequests = logRequests;
            return this;
        }

        public OpenAiStreamingChatModelBuilder logResponses(Boolean logResponses) {
            this.logResponses = logResponses;
            return this;
        }

        public OpenAiStreamingChatModelBuilder customHeaders(Map<String, String> customHeaders) {
            this.customHeaders = customHeaders;
            return this;
        }

        public OpenAiStreamingChatModelBuilder listeners(List<ChatModelListener> listeners) {
            this.listeners = listeners;
            return this;
        }

        public OpenAiStreamingChatModel build() {
            return new OpenAiStreamingChatModel(this);
        }
    }
}

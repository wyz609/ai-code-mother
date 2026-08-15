package dev.langchain4j.service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.ChatExecutor;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.tool.ToolExecutor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiServiceStreamingResponseHandlerTest {

    @Test
    void malformedToolArgumentsAreReturnedToModelForCorrection() {
        long memoryId = 42L;
        AiServiceContext context = new AiServiceContext(AiServiceStreamingResponseHandlerTest.class);
        StreamingChatModel streamingChatModel = mock(StreamingChatModel.class);
        context.streamingChatModel = streamingChatModel;

        ToolExecutor toolExecutor = mock(ToolExecutor.class);
        RuntimeException malformedJson = new RuntimeException(
                new JsonParseException((JsonParser) null, "unexpected character"));
        when(toolExecutor.execute(any(ToolExecutionRequest.class), eq(memoryId)))
                .thenThrow(malformedJson);

        AtomicReference<Throwable> streamError = new AtomicReference<>();
        ChatMemory memory = MessageWindowChatMemory.builder().maxMessages(10).build();
        var toolExecutionHandler = mock(java.util.function.Consumer.class);
        AiServiceStreamingResponseHandler handler = new AiServiceStreamingResponseHandler(
                mock(ChatExecutor.class),
                context,
                memoryId,
                ignored -> { },
                null,
                null,
                toolExecutionHandler,
                ignored -> { },
                streamError::set,
                memory,
                new TokenUsage(),
                List.of(),
                Map.of("writeFile", toolExecutor),
                null,
                null
        );
        ToolExecutionRequest toolRequest = ToolExecutionRequest.builder()
                .id("call-1")
                .name("writeFile")
                .arguments("{\"relativeFilePath\":\"src/App.vue\" \"content\":\"broken\"}")
                .build();
        ChatResponse response = ChatResponse.builder()
                .aiMessage(AiMessage.from(toolRequest))
                .metadata(ChatResponseMetadata.builder().tokenUsage(new TokenUsage()).build())
                .build();

        handler.onCompleteResponse(response);

        assertNull(streamError.get());
        verify(streamingChatModel).chat(any(ChatRequest.class), any(StreamingChatResponseHandler.class));
        verify(toolExecutionHandler, never()).accept(any());
    }
}

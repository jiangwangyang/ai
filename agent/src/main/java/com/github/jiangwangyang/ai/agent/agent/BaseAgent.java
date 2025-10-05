package com.github.jiangwangyang.ai.agent.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BaseAgent {

    private static final Logger log = LoggerFactory.getLogger(BaseAgent.class);

    // 基础属性
    protected final ChatModel chatModel;
    protected final ChatMemory chatMemory;
    protected final List<ToolCallback> availableTools;
    protected final ToolCallbackResolver toolCallbackResolver;
    // 核心属性
    protected final int maxSteps;
    protected final String systemPrompt;
    protected final String nextStepPrompt;
    // 会话属性
    protected ThreadLocal<String> conversationIdThreadLocal = new ThreadLocal<>();
    protected Map<String, Sinks.Many<String>> conversationIdSinksMap = new ConcurrentHashMap<>();

    public BaseAgent(ChatModel chatModel, ChatMemory chatMemory, List<ToolCallback> availableTools, ToolCallbackResolver toolCallbackResolver, int maxSteps, String systemPrompt, String nextStepPrompt) {
        this.chatModel = chatModel;
        this.chatMemory = chatMemory;
        this.availableTools = availableTools;
        this.toolCallbackResolver = toolCallbackResolver;
        this.maxSteps = maxSteps;
        this.systemPrompt = systemPrompt;
        this.nextStepPrompt = nextStepPrompt;
    }

    /**
     * 执行单个步骤
     */
    public abstract boolean step();

    /**
     * 运行代理主循环
     */
    public Flux<String> run(String conversationId, String user, String query) {
        Sinks.Many<String> sinks = Sinks.many().multicast().onBackpressureBuffer();

        CompletableFuture.runAsync(() -> {
            try {
                conversationIdThreadLocal.set(conversationId);
                conversationIdSinksMap.put(conversationId, sinks);

                int currentStep = 0;
                String systemPromptRender = new PromptTemplate(systemPrompt).render(Map.of(
                        "conversationId", conversationId,
                        "user", user,
                        "time", LocalDateTime.now().toString()));
                chatMemory.add(conversationId, new SystemMessage(systemPromptRender));
                chatMemory.add(conversationId, new UserMessage(query));

                while (currentStep < maxSteps) {
                    currentStep++;
                    log.info("{} Executing step {}/{}", conversationId, currentStep, maxSteps);
                    if (!step()) {
                        break;
                    }
                    String nextStepPromptRender = new PromptTemplate(nextStepPrompt).render(Map.of(
                            "conversationId", conversationId,
                            "user", user,
                            "query", query));
                    chatMemory.add(conversationId, new SystemMessage(nextStepPromptRender));
                }

                if (currentStep >= maxSteps) {
                    sinks.tryEmitNext("Terminated: Reached max steps (" + maxSteps + ")\n\n");
                }

            } catch (Exception e) {
                log.error("Agent Error", e);
                sinks.tryEmitError(e);
            } finally {
                conversationIdSinksMap.remove(conversationId);
                conversationIdThreadLocal.remove();
                sinks.tryEmitComplete();
            }
        });

        return sinks.asFlux();
    }

    protected List<ChatResponse> sendAssistantMessage(Flux<ChatResponse> chatResponseFlux) {
        String conversationId = conversationIdThreadLocal.get();
        Sinks.Many<String> sinks = conversationIdSinksMap.get(conversationId);
        sinks.tryEmitNext("**[Assistant]** ");
        return chatResponseFlux
                .doOnNext(chatResponse -> sinks.tryEmitNext(chatResponse.getResult().getOutput().getText()))
                .doOnNext(chatResponse -> chatMemory.add(conversationId, chatResponse.getResult().getOutput()))
                .collectList()
                .block();
    }

    protected void sendToolMessage(ToolResponseMessage toolResponseMessage) {
        String conversationId = conversationIdThreadLocal.get();
        Sinks.Many<String> sinks = conversationIdSinksMap.get(conversationId);
        for (ToolResponseMessage.ToolResponse response : toolResponseMessage.getResponses()) {
            sinks.tryEmitNext("**[Tool]** ");
            sinks.tryEmitNext(response.responseData());
            sinks.tryEmitNext("\n\n");
        }
        chatMemory.add(conversationId, toolResponseMessage);
    }

    protected ToolResponseMessage executeToolCalls(AssistantMessage assistantMessage) {
        ToolContext toolContext = new ToolContext(Collections.emptyMap());
        List<ToolResponseMessage.ToolResponse> toolResponses = new ArrayList<>();
        for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
            log.debug("Executing tool call: {}", toolCall.name());
            String toolName = toolCall.name();
            String toolInputArguments = toolCall.arguments();
            String finalToolInputArguments;
            if (!StringUtils.hasText(toolInputArguments)) {
                log.warn("Tool call arguments are null or empty for tool: {}. Using empty JSON object as default.", toolName);
                finalToolInputArguments = "{}";
            } else {
                finalToolInputArguments = toolInputArguments;
            }

            ToolCallback toolCallback = availableTools.stream().filter((tool) -> toolName.equals(tool.getToolDefinition().name())).findFirst().orElseGet(() -> this.toolCallbackResolver.resolve(toolName));
            if (toolCallback == null) {
                throw new IllegalStateException("No ToolCallback found for tool name: " + toolName);
            }

            String toolCallResult;
            try {
                toolCallResult = toolCallback.call(finalToolInputArguments, toolContext);
            } catch (ToolExecutionException ex) {
                log.error("Error executing tool call: {}", toolName, ex);
                toolCallResult = "";
            }
            toolResponses.add(new ToolResponseMessage.ToolResponse(toolCall.id(), toolName, toolCallResult));
        }

        return new ToolResponseMessage(toolResponses, Map.of());
    }

}
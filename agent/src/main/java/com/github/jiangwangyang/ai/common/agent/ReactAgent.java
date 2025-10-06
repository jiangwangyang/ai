package com.github.jiangwangyang.ai.common.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;

public class ReactAgent extends BaseAgent {

    private static final Logger log = LoggerFactory.getLogger(ReactAgent.class);

    private final ChatOptions options;

    public ReactAgent(ChatModel chatModel, ChatMemory chatMemory, List<ToolCallback> availableTools, ToolCallbackResolver toolCallbackResolver, int maxSteps, String systemPrompt, String nextStepPrompt) {
        super(chatModel, chatMemory, availableTools, toolCallbackResolver, maxSteps, systemPrompt, nextStepPrompt);
        options = chatModel.getDefaultOptions();
        if (options instanceof ToolCallingChatOptions toolCallingChatOptions) {
            toolCallingChatOptions.setInternalToolExecutionEnabled(false);
            toolCallingChatOptions.setToolCallbacks(availableTools);
        }
    }

    @Override
    public boolean step() {
        List<AssistantMessage> thinkList = think();
        if (CollectionUtils.isEmpty(thinkList)) {
            return false;
        }
        act(thinkList);
        return true;
    }

    public List<AssistantMessage> think() {
        try {
            Prompt prompt = Prompt.builder()
                    .messages(chatMemory.get(conversationIdThreadLocal.get()))
                    .chatOptions(options)
                    .build();
            List<ChatResponse> chatResponseList = sendAssistantMessage(chatModel.stream(prompt));

            return chatResponseList.stream()
                    .flatMap(chatResponse -> chatResponse.getResults().stream())
                    .map(Generation::getOutput)
                    .filter(assistantMessage -> !CollectionUtils.isEmpty(assistantMessage.getToolCalls()))
                    .toList();

        } catch (Exception e) {

            log.error("{} react think error", conversationIdThreadLocal.get(), e);
            sendAssistantMessage(Flux.just(new ChatResponse(List.of(new Generation(new AssistantMessage(
                    "\n\nError encountered while processing: " + e.getMessage() + "\n\n"))))));
            return Collections.emptyList();
        }
    }

    public void act(List<AssistantMessage> thinkList) {

        thinkList.forEach(assistantMessage -> {
            ToolResponseMessage toolResponseMessage = executeToolCalls(assistantMessage);
            sendToolMessage(toolResponseMessage);
        });
    }

}

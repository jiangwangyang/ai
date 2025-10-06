package com.github.jiangwangyang.ai.config;

import com.github.jiangwangyang.ai.common.agent.BaseAgent;
import com.github.jiangwangyang.ai.common.agent.ReactAgent;
import com.github.jiangwangyang.ai.service.MemoryService;
import com.github.jiangwangyang.ai.service.PlanService;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Configuration
public class AgentConfig {

    @Autowired
    private ChatModel chatModel;
    @Autowired
    private ToolCallbackProvider toolCallbackProvider;
    @Autowired
    private ToolCallbackResolver toolCallbackResolver;
    @Autowired
    private PlanService planService;
    @Autowired
    private MemoryService memoryService;

    @Bean("planAgent")
    public BaseAgent planAgent(@Value("${agent.plan.system-prompt}") String systemPrompt) {
        return new ReactAgent(
                chatModel,
                chatMemory(),
                List.of(ToolCallbacks.from(planService)),
                toolCallbackResolver,
                1,
                systemPrompt,
                "没有下一步"
        );
    }

    @Primary
    @Bean("reactAgent")
    public BaseAgent reactAgent(@Value("${agent.react.system-prompt}") String systemPrompt,
                                @Value("${agent.react.next-step-prompt}") String nextStepPrompt) {
        return new ReactAgent(
                chatModel,
                chatMemory(),
                Stream.concat(
                        Arrays.stream(toolCallbackProvider.getToolCallbacks()),
                        Arrays.stream(ToolCallbacks.from(memoryService))
                ).toList(),
                toolCallbackResolver,
                10,
                systemPrompt,
                nextStepPrompt
        );
    }

    @Bean
    public ChatMemory chatMemory() {
        InMemoryChatMemoryRepository chatMemoryRepository = new InMemoryChatMemoryRepository();
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(1000)
                .build();
    }

}

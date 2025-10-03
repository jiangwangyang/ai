package com.github.jiangwangyang.ai.agent.chat.config;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.a2a.A2aRemoteAgent;
import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardProvider;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.github.jiangwangyang.ai.agent.chat.AgentToolProvider;
import com.github.jiangwangyang.ai.agent.chat.service.TimeService;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

@Configuration
public class ChatAgentConfig {

    @Bean("chatAgent")
    public BaseAgent chatAgent(@Value("${agent.prompt.chat-agent-instruction}") String instruction,
                               ChatModel chatModel,
                               TimeService timeService,
                               AgentCardProvider agentCardProvider) throws GraphStateException {
        KeyStrategyFactory stateFactory = () -> {
            HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
            keyStrategyHashMap.put("messages", new ReplaceStrategy());
            return keyStrategyHashMap;
        };

        AgentToolProvider agentToolProvider = AgentToolProvider.builder()
                .addAgent(A2aRemoteAgent.builder()
                        .agentCardProvider(agentCardProvider)
                        .name("memory_agent")
                        .description("能够对用户历史行为进行分析和记忆")
                        .inputKey("messages")
                        .outputKey("messages")
                        .build(), "messages")
                .addAgent(A2aRemoteAgent.builder()
                        .agentCardProvider(agentCardProvider)
                        .name("search_agent")
                        .description("能够进行网络搜索")
                        .inputKey("messages")
                        .outputKey("messages")
                        .build(), "messages")
                .build();
        MethodToolCallbackProvider methodToolProvider = MethodToolCallbackProvider.builder()
                .toolObjects(timeService)
                .build();
        List<ToolCallback> toolCallbackList = Stream.of(
                        agentToolProvider.getToolCallbacks(),
                        methodToolProvider.getToolCallbacks())
                .flatMap(Arrays::stream)
                .toList();

        return ReactAgent
                .builder()
                .instruction(instruction)
                .model(chatModel)
                .tools(toolCallbackList)
                .state(stateFactory)
                .name("chat_agent")
                .description("负责与用户进行对话")
                .inputKey("messages")
                .outputKey("messages")
                .build();
    }

}

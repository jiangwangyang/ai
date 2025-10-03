package com.github.jiangwangyang.ai.agent.chat.config;

import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardProvider;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.github.jiangwangyang.ai.agent.chat.A2AToolProvider;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ChatAgentConfig {

    @Bean("chatAgent")
    public BaseAgent chatAgent(@Value("${agent.prompt.chat-agent-instruction}") String instruction,
                               ChatModel chatModel,
                               AgentCardProvider agentCardProvider) throws GraphStateException {

        A2AToolProvider a2AToolProvider = new A2AToolProvider(List.of("memory_agent", "search_agent"), agentCardProvider);
        List<ToolCallback> toolCallbackList = List.of(a2AToolProvider.getToolCallbacks());

        return ReactAgent
                .builder()
                .instruction(instruction)
                .model(chatModel)
                .tools(toolCallbackList)
                .name("chat_agent")
                .description("负责与用户进行对话")
                .inputKey("messages")
                .outputKey("messages")
                .build();
    }

}

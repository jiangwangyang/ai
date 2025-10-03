package com.github.jiangwangyang.ai.client.config;

import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.agent.a2a.A2aRemoteAgent;
import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardProvider;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RemoteAgentConfig {

    @Bean
    public BaseAgent remoteAgent(AgentCardProvider agentCardProvider) throws GraphStateException {
        return A2aRemoteAgent.builder()
                .agentCardProvider(agentCardProvider)
                .name("chat_agent")
                .description("负责与用户进行对话")
                .inputKey("messages")
                .outputKey("messages")
                .build();
    }

}

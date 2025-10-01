package com.github.jiangwangyang.ai.client.config;

import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.agent.a2a.A2aRemoteAgent;
import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardProvider;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentConfig {

    @Bean
    public BaseAgent remoteAgent(AgentCardProvider agentCardProvider) throws GraphStateException {
        return A2aRemoteAgent.builder()
                .agentCardProvider(agentCardProvider)
                .name("ReactAgent")
                .description("A React Agent")
                .build();
    }

}

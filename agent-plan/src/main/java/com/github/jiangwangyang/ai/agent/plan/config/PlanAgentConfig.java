package com.github.jiangwangyang.ai.agent.plan.config;

import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardProvider;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.github.jiangwangyang.ai.agent.plan.PlanAgent;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class PlanAgentConfig {

    @Bean("planAgent")
    public BaseAgent planAgent(ChatModel chatModel, AgentCardProvider agentCardProvider) throws GraphStateException {

        return new PlanAgent("plan_agent", "将复杂问题分解为可管理的步骤，然后按照步骤执行，最后汇总结果",
                chatModel, List.of("memory_agent", "search_agent", "chat_agent"), agentCardProvider);
    }

}

package com.github.jiangwangyang.ai.agent.plan.config;

import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.a2a.A2aRemoteAgent;
import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardProvider;
import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardWrapper;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.mcp.discovery.client.tool.LoadbalancedSyncMcpToolCallbackProvider;
import com.alibaba.cloud.ai.mcp.discovery.client.transport.LoadbalancedMcpSyncClient;
import com.github.jiangwangyang.ai.agent.plan.A2AToolProvider;
import com.github.jiangwangyang.ai.agent.plan.service.TimeService;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Configuration
public class CollaborativeAgentConfig {

    @Bean("collaborativeAgent")
    public BaseAgent collaborativeAgent(
            @Value("""
                    你是一个智能助手，负责与用户进行对话。
                    你可以调用其他智能助手来完成任务。
                    """) String instruction,
            ChatModel chatModel,
            AgentCardProvider agentCardProvider,
            List<LoadbalancedMcpSyncClient> mcpClientList,
            TimeService timeService) throws GraphStateException {

        A2AToolProvider a2AToolProvider = new A2AToolProvider(agentList(agentCardProvider));
        LoadbalancedSyncMcpToolCallbackProvider nacosMcpToolProvider = new LoadbalancedSyncMcpToolCallbackProvider(
                mcpClientList.stream()
                        .filter(client -> "mcp-memory".equals(client.getServerName()))
                        .toList());
        MethodToolCallbackProvider localToolsProvider = MethodToolCallbackProvider.builder()
                .toolObjects(timeService)
                .build();
        List<ToolCallback> toolCallbackList = Stream.of(
                        a2AToolProvider.getToolCallbacks(),
                        nacosMcpToolProvider.getToolCallbacks(),
                        localToolsProvider.getToolCallbacks())
                .flatMap(Arrays::stream)
                .toList();

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

    private List<BaseAgent> agentList(AgentCardProvider agentCardProvider) {
        return Stream.of("dify_agent", "search_agent")
                .map(name -> {
                    try {
                        AgentCardWrapper agentCardWrapper = agentCardProvider.getAgentCard(name);
                        return (BaseAgent) A2aRemoteAgent.builder()
                                .agentCard(agentCardWrapper.getAgentCard())
                                .name(agentCardWrapper.name())
                                .description(agentCardWrapper.description())
                                .inputKey("messages")
                                .outputKey("messages")
                                .build();
                    } catch (GraphStateException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
    }

}

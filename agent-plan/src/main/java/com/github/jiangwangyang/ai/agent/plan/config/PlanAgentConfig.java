package com.github.jiangwangyang.ai.agent.plan.config;

import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.mcp.discovery.client.tool.LoadbalancedSyncMcpToolCallbackProvider;
import com.alibaba.cloud.ai.mcp.discovery.client.transport.LoadbalancedMcpSyncClient;
import com.github.jiangwangyang.ai.agent.plan.PlanAgent;
import com.github.jiangwangyang.ai.agent.plan.service.TimeService;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Configuration
public class PlanAgentConfig {

    @Autowired
    @Qualifier("searchAgent")
    private BaseAgent searchAgent;
    @Autowired
    @Qualifier("railwayAgent")
    private BaseAgent railwayAgent;

    @Primary
    @Bean("planAgent")
    public BaseAgent planAgent(ChatModel chatModel,
                               List<LoadbalancedMcpSyncClient> mcpClientList,
                               TimeService timeService) throws GraphStateException {

        LoadbalancedSyncMcpToolCallbackProvider nacosMcpToolProvider = new LoadbalancedSyncMcpToolCallbackProvider(
                mcpClientList.stream()
                        .filter(client -> "mcp-memory".equals(client.getServerName()))
                        .toList());
        MethodToolCallbackProvider localToolsProvider = MethodToolCallbackProvider.builder()
                .toolObjects(timeService)
                .build();
        List<ToolCallback> toolCallbackList = Stream.of(
                        nacosMcpToolProvider.getToolCallbacks(),
                        localToolsProvider.getToolCallbacks())
                .flatMap(Arrays::stream)
                .toList();

        return new PlanAgent("plan_agent", "将复杂问题分解为可管理的步骤，然后按照步骤执行，最后汇总结果",
                chatModel, toolCallbackList, List.of(searchAgent, railwayAgent));
    }

}

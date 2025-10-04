package com.github.jiangwangyang.ai.agent.plan.config;

import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.mcp.discovery.client.tool.LoadbalancedSyncMcpToolCallbackProvider;
import com.alibaba.cloud.ai.mcp.discovery.client.transport.LoadbalancedMcpSyncClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class RailwayAgentConfig {

    @Bean("railwayAgent")
    public BaseAgent railwayAgent(@Value("${agent.prompt.railway-agent-instruction}") String instruction,
                                  ChatModel chatModel,
                                  List<LoadbalancedMcpSyncClient> mcpClientList) throws GraphStateException {

        LoadbalancedSyncMcpToolCallbackProvider nacosMcpToolProvider = new LoadbalancedSyncMcpToolCallbackProvider(
                mcpClientList.stream()
                        .filter(client -> "china-railway".equals(client.getServerName()))
                        .toList());

        return ReactAgent
                .builder()
                .instruction(instruction)
                .model(chatModel)
                .tools(List.of(nacosMcpToolProvider.getToolCallbacks()))
                .name("railway_agent")
                .description("能够查询中国铁路信息")
                .inputKey("messages")
                .outputKey("messages")
                .build();
    }

}

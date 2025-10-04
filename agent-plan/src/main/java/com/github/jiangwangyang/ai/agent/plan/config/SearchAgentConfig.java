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
public class SearchAgentConfig {

    @Bean("searchAgent")
    public BaseAgent searchAgent(@Value("${agent.prompt.search-agent-instruction}") String instruction,
                                 ChatModel chatModel,
                                 List<LoadbalancedMcpSyncClient> mcpClientList) throws GraphStateException {

        LoadbalancedSyncMcpToolCallbackProvider nacosMcpToolProvider = new LoadbalancedSyncMcpToolCallbackProvider(
                mcpClientList.stream()
                        .filter(client -> "WebSearch".equals(client.getServerName()))
                        .toList());

        return ReactAgent
                .builder()
                .instruction(instruction)
                .model(chatModel)
                .tools(List.of(nacosMcpToolProvider.getToolCallbacks()))
                .name("search_agent")
                .description("能够进行网络搜索")
                .inputKey("messages")
                .outputKey("messages")
                .build();
    }

}

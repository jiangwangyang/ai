package com.github.jiangwangyang.ai.agent.memory.config;

import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.mcp.discovery.client.tool.LoadbalancedSyncMcpToolCallbackProvider;
import com.alibaba.cloud.ai.mcp.discovery.client.transport.LoadbalancedMcpSyncClient;
import com.github.jiangwangyang.ai.agent.memory.service.TimeService;
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
public class MemoryAgentConfig {

    @Bean("memoryAgent")
    public BaseAgent memoryAgent(@Value("${agent.prompt.memory-agent-instruction}") String instruction,
                                 ChatModel chatModel,
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

        return ReactAgent
                .builder()
                .instruction(instruction)
                .model(chatModel)
                .tools(toolCallbackList)
                .name("memory_agent")
                .description("能够对用户历史行为进行分析和记忆")
                .inputKey("messages")
                .outputKey("messages")
                .build();
    }

}

package com.github.jiangwangyang.ai.agent.plan.config;

import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.mcp.discovery.client.tool.LoadbalancedSyncMcpToolCallbackProvider;
import com.alibaba.cloud.ai.mcp.discovery.client.transport.LoadbalancedMcpSyncClient;
import com.github.jiangwangyang.ai.agent.plan.A2AToolProvider;
import com.github.jiangwangyang.ai.agent.plan.service.TimeService;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Configuration
public class CollaborativeAgentConfig {

    @Autowired
    @Qualifier("searchAgent")
    private BaseAgent searchAgent;
    @Autowired
    @Qualifier("railwayAgent")
    private BaseAgent railwayAgent;

    @Bean("collaborativeAgent")
    public BaseAgent collaborativeAgent(
            ChatModel chatModel,
            List<LoadbalancedMcpSyncClient> mcpClientList,
            TimeService timeService) throws GraphStateException {

        A2AToolProvider a2AToolProvider = new A2AToolProvider(List.of(searchAgent, railwayAgent));
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
                .instruction("""
                        你是一个智能助手，负责与用户进行对话。
                        你可以调用工具或其他智能助手来完成任务。
                        """)
                .model(chatModel)
                .tools(toolCallbackList)
                .name("chat_agent")
                .description("负责与用户进行对话")
                .inputKey("messages")
                .outputKey("messages")
                .build();
    }

}

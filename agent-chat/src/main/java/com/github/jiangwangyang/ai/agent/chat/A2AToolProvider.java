package com.github.jiangwangyang.ai.agent.chat;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.a2a.A2aRemoteAgent;
import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardProvider;
import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardWrapper;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.tool.support.ToolDefinitions;
import org.springframework.ai.tool.support.ToolUtils;
import org.springframework.lang.NonNull;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class A2AToolProvider implements ToolCallbackProvider {

    private static final Method CALL_METHOD;

    static {
        try {
            CALL_METHOD = AgentTool.class.getMethod("call", String.class);
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final List<AgentTool> agentToolList;

    public A2AToolProvider(List<String> remoteAgentNameList, AgentCardProvider agentCardProvider) {
        agentToolList = remoteAgentNameList.stream()
                .map(name -> {
                    try {
                        AgentCardWrapper agentCardWrapper = agentCardProvider.getAgentCard(name);
                        return A2aRemoteAgent.builder()
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
                .map(AgentTool::new)
                .toList();
    }

    @Override
    @NonNull
    public ToolCallback[] getToolCallbacks() {
        return agentToolList.stream()
                .map(agentTool -> MethodToolCallback.builder()
                        .toolDefinition(new DefaultToolDefinition(
                                agentTool.agent().name(),
                                agentTool.agent().description(),
                                ToolDefinitions.from(CALL_METHOD).inputSchema()))
                        .toolMetadata(ToolMetadata.from(CALL_METHOD))
                        .toolMethod(CALL_METHOD)
                        .toolObject(agentTool)
                        .toolCallResultConverter(ToolUtils.getToolCallResultConverter(CALL_METHOD))
                        .build()
                ).toArray(ToolCallback[]::new);
    }

    public record AgentTool(A2aRemoteAgent agent) {
        public String call(@ToolParam(description = "详细描述你需要完成什么功能") String requirement) {
            Map<String, Object> inputMap = Map.of("messages", requirement);
            try {
                Optional<OverAllState> output = agent.invoke(inputMap);
                return output
                        .map(OverAllState::data)
                        .map(outPutMap -> outPutMap.get(agent.outputKey()))
                        .map(Object::toString)
                        .orElse("");
            } catch (GraphStateException | GraphRunnerException e) {
                throw new RuntimeException(e);
            }
        }
    }

}

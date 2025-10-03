package com.github.jiangwangyang.ai.agent.search;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
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
import java.util.*;

public class AgentToolProvider implements ToolCallbackProvider {

    private static final Method CALL_METHOD;

    static {
        try {
            CALL_METHOD = AgentTool.class.getMethod("call", String.class);
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final List<AgentTool> agentToolList = new ArrayList<>();

    public AgentToolProvider(Builder builder) {
        agentToolList.addAll(builder.agentToolList);
    }

    public static Builder builder() {
        return new Builder();
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

    public record AgentTool(BaseAgent agent, String inputKey) {
        public String call(@ToolParam(description = "详细描述你需要完成什么功能") String requirement) {
            Map<String, Object> inputMap = Map.of(inputKey, requirement);
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

    public static final class Builder {

        private final List<AgentTool> agentToolList = new ArrayList<>();

        public Builder addAgent(BaseAgent agent, String inputKey) {
            agentToolList.add(new AgentTool(agent, inputKey));
            return this;
        }

        public Builder addAgent(AgentTool... agentTool) {
            agentToolList.addAll(Arrays.asList(agentTool));
            return this;
        }

        public Builder addAgent(Collection<AgentTool> agentToolCollection) {
            this.agentToolList.addAll(agentToolCollection);
            return this;
        }

        public AgentToolProvider build() {
            return new AgentToolProvider(this);
        }

    }

}

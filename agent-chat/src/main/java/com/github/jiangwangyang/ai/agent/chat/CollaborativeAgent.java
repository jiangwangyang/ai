package com.github.jiangwangyang.ai.agent.chat;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.FlowAgent;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowAgentBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CollaborativeAgent extends FlowAgent {

    private static final String AGENT_NAME_KEY = "agent_name";
    private static final String THINK_RESULT_KEY = "think_result";
    private static final String COLLABORATION_RESULT_KEY = "collaboration_results";
    private static final String THINK_STEP = "think";
    private static final String COLLABORATE_STEP = "collaborate";
    private static final String RESULT_MERGE_STEP = "result_merge";

    private final ChatClient chatClient;

    protected CollaborativeAgent(CollaborativeAgentBuilder builder) throws GraphStateException {
        super(builder.name, builder.description, builder.outputKey, builder.inputKey, builder.keyStrategyFactory, builder.compileConfig, builder.subAgents);
        this.chatClient = builder.chatClient;
    }

    public static CollaborativeAgentBuilder builder() {
        return new CollaborativeAgentBuilder();
    }

    @Override
    protected StateGraph buildSpecificGraph(FlowGraphBuilder.FlowGraphConfig config) throws GraphStateException {
        Map<String, BaseAgent> collaboratorAgentMap = subAgents().stream()
                .collect(Collectors.toMap(BaseAgent::name, agent -> agent));
        StateGraph graph = new StateGraph(config.getName(), config.getKeyStrategyFactory());

        // 1. 先思考要不要调用协作者
        graph.addNode(THINK_STEP, overAllState -> {
            Map<String, Object> data = new HashMap<>(overAllState.data());
            String input = data.get(inputKey).toString();
            ThinkResult thinkResult = chatClient
                    .prompt()
                    .user(input)
                    .call()
                    .entity(ThinkResult.class);
            data.put(THINK_RESULT_KEY, thinkResult);

            // 返回
            return CompletableFuture.completedFuture(data);
        });

        // 2. 添加协作执行节点
        graph.addNode(COLLABORATE_STEP, overAllState -> {
            Map<String, Object> data = new HashMap<>(overAllState.data());
            ThinkResult thinkResult = (ThinkResult) data.get(THINK_RESULT_KEY);

            // 并行调用协作者Agent
            if (thinkResult.collaboratorDecisionList() == null) {
                data.put(COLLABORATION_RESULT_KEY, "");
                return CompletableFuture.completedFuture(data);
            }
            List<CompletableFuture<Optional<OverAllState>>> resultFutureList =
                    thinkResult.collaboratorDecisionList()
                            .stream()
                            .map(collaboratorDecision -> {
                                String agentName = collaboratorDecision.agentName();
                                BaseAgent collaboratorAgent = collaboratorAgentMap.get(agentName);
                                HashMap<String, Object> inputMap = new HashMap<>(overAllState.data());
                                inputMap.put(AGENT_NAME_KEY, agentName);
                                inputMap.put(inputKey, collaboratorDecision.requirement());
                                return CompletableFuture.supplyAsync(() -> {
                                    try {
                                        return collaboratorAgent.invoke(inputMap);
                                    } catch (GraphStateException | GraphRunnerException e) {
                                        throw new RuntimeException(e);
                                    }
                                });
                            }).toList();
            CompletableFuture.allOf(resultFutureList.toArray(CompletableFuture[]::new)).join();

            // 处理协作者结果
            String collaborationResult = resultFutureList.stream()
                    .map(CompletableFuture::join)
                    .flatMap(optional -> optional
                            .map(OverAllState::data)
                            .stream())
                    .map(dataMap -> dataMap.get(AGENT_NAME_KEY) + "result: " + dataMap.get(outputKey))
                    .toList()
                    .toString();
            data.put(COLLABORATION_RESULT_KEY, collaborationResult);

            // 返回
            return CompletableFuture.completedFuture(data);
        });

        // 4. 添加结果合并节点
        graph.addNode(RESULT_MERGE_STEP, overAllState -> {
            Map<String, Object> data = new HashMap<>(overAllState.data());
            String collaborationResult = (String) data.get(COLLABORATION_RESULT_KEY);

            // 总结结果
            Flux<String> summaryFlux = chatClient.prompt()
                    .messages(new AssistantMessage(collaborationResult))
                    .stream()
                    .content();
            data.put(outputKey, summaryFlux);

            // 返回
            return CompletableFuture.completedFuture(data);
        });

        // 构建状态流转图
        graph.addEdge(StateGraph.START, THINK_STEP);
        graph.addEdge(THINK_STEP, COLLABORATE_STEP);
        graph.addEdge(COLLABORATE_STEP, RESULT_MERGE_STEP);
        graph.addEdge(RESULT_MERGE_STEP, StateGraph.END);
        return graph;
    }

    // Builder模式实现
    public static class CollaborativeAgentBuilder extends FlowAgentBuilder<CollaborativeAgent, CollaborativeAgentBuilder> {
        private ChatClient chatClient;

        public CollaborativeAgentBuilder chatClient(ChatClient chatClient) {
            this.chatClient = chatClient;
            return self();
        }

        @Override
        protected CollaborativeAgentBuilder self() {
            return this;
        }

        @Override
        public CollaborativeAgent build() throws GraphStateException {
            return new CollaborativeAgent(this);
        }
    }

    record ThinkResult(String thinkResultString, List<CollaboratorDecision> collaboratorDecisionList) {
        record CollaboratorDecision(String agentName, String requirement) {
        }
    }
}

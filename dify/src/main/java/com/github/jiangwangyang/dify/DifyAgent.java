package com.github.jiangwangyang.dify;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.scheduling.ScheduleConfig;
import com.alibaba.cloud.ai.graph.scheduling.ScheduledAgentTask;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.jiangwangyang.dify.util.ObjectMapperUtil;
import com.github.jiangwangyang.dify.util.UnicodeUtil;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

public class DifyAgent extends BaseAgent {

    public static final ParameterizedTypeReference<ServerSentEvent<String>> SSE_STRING_TYPE_REFERENCE = new ParameterizedTypeReference<>() {
    };
    public static final TypeReference<Map<String, Object>> MAP_STRING_OBJECT_TYPE_REFERENCE = new TypeReference<>() {
    };
    public static final KeyStrategyFactory KEY_STRATEGY_FACTORY = () -> {
        HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
        keyStrategyHashMap.put("query", new ReplaceStrategy());
        keyStrategyHashMap.put("inputs", new ReplaceStrategy());
        keyStrategyHashMap.put("answer", new ReplaceStrategy());
        return keyStrategyHashMap;
    };

    protected final WebClient webClient;
    protected final String url;
    protected final Map<String, String> headers;

    public DifyAgent(WebClient webClient, String url, Map<String, String> headers, String name, String description) throws GraphStateException {
        super(name, description, "answer");
        this.webClient = webClient;
        this.url = url;
        this.headers = headers;
    }

    @Override
    protected StateGraph initGraph() throws GraphStateException {

        // 定义Dify节点
        NodeAction difyAction = (state) -> {
            Map<String, String> requestHeaders = new HashMap<>(headers);
            requestHeaders.putAll((Map<String, String>) state.value("headers").orElse(Collections.emptyMap()));
            Map<String, Object> bodyValue = new HashMap<>(state.data());
            if (!bodyValue.containsKey("query") && bodyValue.containsKey("messages")) {
                bodyValue.put("query", bodyValue.get("messages").toString());
            }
            if (!bodyValue.containsKey("inputs")) {
                bodyValue.put("inputs", Collections.emptyMap());
            }
            if (!bodyValue.containsKey("response_mode")) {
                bodyValue.put("response_mode", "streaming");
            }
            Flux<ChatResponse> responseFlux = webClient.post()
                    .uri(url)
                    .headers(h -> requestHeaders.forEach(h::set))
                    .header("content-type", "application/json")
                    .bodyValue(bodyValue)
                    .retrieve()
                    .bodyToFlux(SSE_STRING_TYPE_REFERENCE)
                    .map(ServerSentEvent::data)
                    .map(UnicodeUtil::unescape)
                    .flatMap(s -> {
                        try {
                            Map<String, Object> event = ObjectMapperUtil.readValue(s, MAP_STRING_OBJECT_TYPE_REFERENCE);
                            if ("message".equals(event.get("event")) && event.get("answer") != null) {
                                return Flux.just(event.get("answer").toString());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return Flux.empty();
                    })
                    .map(answer -> new ChatResponse(List.of(new Generation(new AssistantMessage(answer)))));
            return Map.of("answer", responseFlux, "messages", responseFlux);
        };

        // 构建状态流转图
        StateGraph graph = new StateGraph(name, KEY_STRATEGY_FACTORY);
        graph.addNode("difyNode", AsyncNodeAction.node_async(difyAction));
        graph.addEdge(StateGraph.START, "difyNode");
        graph.addEdge("difyNode", StateGraph.END);
        return graph;
    }

    @Override
    public AsyncNodeAction asAsyncNodeAction(String inputKeyFromParent, String outputKeyToParent) throws GraphStateException {
        if (this.compiledGraph == null) {
            this.compiledGraph = getAndCompileGraph();
        }
        return node_async(new ReactAgent.SubGraphStreamingNodeAdapter(inputKeyFromParent, outputKeyToParent, this.compiledGraph));
    }

    @Override
    public ScheduledAgentTask schedule(ScheduleConfig scheduleConfig) throws GraphStateException {
        CompiledGraph compiledGraph = getAndCompileGraph();
        return compiledGraph.schedule(scheduleConfig);
    }
}

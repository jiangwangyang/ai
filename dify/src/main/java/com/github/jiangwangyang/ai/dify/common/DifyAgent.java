package com.github.jiangwangyang.ai.dify.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.jiangwangyang.ai.dify.util.ObjectMapperUtil;
import com.github.jiangwangyang.ai.dify.util.UnicodeUtil;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

public class DifyAgent {

    public static final ParameterizedTypeReference<ServerSentEvent<String>> SSE_STRING_PARAMETERIZED_TYPE_REFERENCE = new ParameterizedTypeReference<>() {
    };
    public static final ParameterizedTypeReference<Map<String, Object>> MAP_STRING_OBJECT_PARAMETERIZED_TYPE_REFERENCE = new ParameterizedTypeReference<>() {
    };
    public static final TypeReference<Map<String, Object>> MAP_STRING_OBJECT_TYPE_REFERENCE = new TypeReference<>() {
    };

    protected final WebClient webClient;
    protected final String url;

    public DifyAgent(WebClient webClient, String url) {
        this.webClient = webClient;
        this.url = url;
    }

    public Flux<Map<String, Object>> stream(String query, Map<String, Object> inputs, Map<String, String> headers) {
        return webClient.post()
                .uri(url)
                .headers(h -> headers.forEach(h::set))
                .accept(MediaType.TEXT_EVENT_STREAM)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(ObjectMapperUtil.writeValueAsString(Map.of(
                        "conversation_id", "",
                        "files", List.of(),
                        "user", "test-user-001",
                        "query", query,
                        "inputs", inputs,
                        "response_mode", "streaming"
                )))
                .retrieve()
                .bodyToFlux(SSE_STRING_PARAMETERIZED_TYPE_REFERENCE)
                .map(ServerSentEvent::data)
                .map(UnicodeUtil::unescape)
                .map(s -> ObjectMapperUtil.readValue(s, MAP_STRING_OBJECT_TYPE_REFERENCE));
    }

    public Map<String, Object> call(String query, Map<String, Object> inputs, Map<String, String> headers) {
        return webClient.post()
                .uri(url)
                .headers(h -> headers.forEach(h::set))
                .header("content-type", "application/json")
                .bodyValue(Map.of(
                        "query", query,
                        "inputs", inputs,
                        "response_mode", "blocking"
                ))
                .retrieve()
                .bodyToMono(MAP_STRING_OBJECT_PARAMETERIZED_TYPE_REFERENCE)
                .block();
    }

}

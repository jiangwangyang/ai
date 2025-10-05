package com.github.jiangwangyang.ai.common.a2a;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import io.a2a.spec.*;
import io.a2a.util.Utils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static io.a2a.util.Assert.checkNotNullParam;

public class WebClientA2AClient {

    public static final ParameterizedTypeReference<ServerSentEvent<String>> SSE_STRING_TYPE_REFERENCE = new ParameterizedTypeReference<>() {
    };
    public static final TypeReference<SendMessageResponse> SEND_MESSAGE_RESPONSE_REFERENCE = new TypeReference<>() {
    };

    private final WebClient webClient;

    public WebClientA2AClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public Flux<StreamingEventKind> sendStreamingMessage(String url, Message message) {
        return sendStreamingMessage(url, message, Collections.emptyMap());
    }

    public Flux<StreamingEventKind> sendStreamingMessage(String url, Message message, Map<String, String> headers) {
        checkNotNullParam("message", message);
        SendStreamingMessageRequest requestBody = new SendStreamingMessageRequest.Builder()
                .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                .method(SendStreamingMessageRequest.METHOD)
                .params(new MessageSendParams(message, null, null))
                .build();
        String requestBodyJson;
        try {
            requestBodyJson = Utils.OBJECT_MAPPER.writeValueAsString(requestBody);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return webClient.post()
                .uri(url)
                .headers(httpHeaders -> headers.forEach(httpHeaders::set))
                .header("content-type", "application/json")
                .header("accept", "text/event-stream")
                .bodyValue(requestBodyJson)
                .retrieve()
                .bodyToFlux(SSE_STRING_TYPE_REFERENCE)
                .map(ServerSentEvent::data)
                .map(msg -> {
                    try {
                        JsonNode jsonNode = Utils.OBJECT_MAPPER.readTree(msg);
                        if (jsonNode.has("error")) {
                            throw Utils.OBJECT_MAPPER.treeToValue(jsonNode.get("error"), JSONRPCError.class);
                        } else if (jsonNode.has("result")) {
                            JsonNode result = jsonNode.path("result");
                            return Utils.OBJECT_MAPPER.treeToValue(result, StreamingEventKind.class);
                        } else {
                            throw new IllegalArgumentException("Unknown message type");
                        }
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Failed to send message: " + e, e.getCause());
                    }
                });
    }

    public SendMessageResponse sendMessage(String url, Message message) {
        return sendMessage(url, message, Collections.emptyMap());
    }

    public SendMessageResponse sendMessage(String url, Message message, Map<String, String> headers) {
        checkNotNullParam("message", message);
        SendMessageRequest requestBody = new SendMessageRequest.Builder()
                .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                .method(SendMessageRequest.METHOD)
                .params(new MessageSendParams(message, null, null))
                .build();
        String requestBodyJson;
        try {
            requestBodyJson = Utils.OBJECT_MAPPER.writeValueAsString(requestBody);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        String responseBody = webClient.post()
                .uri(url)
                .headers(h -> headers.forEach(h::add))
                .header("content-type", "application/json")
                .bodyValue(requestBodyJson)
                .retrieve()
                .toEntity(String.class)
                .map(ResponseEntity::getBody)
                .block();
        try {
            SendMessageResponse value = Utils.unmarshalFrom(responseBody, SEND_MESSAGE_RESPONSE_REFERENCE);
            JSONRPCError error = value.getError();
            if (error != null) {
                throw new A2AServerException(error.getMessage() + (error.getData() != null ? ": " + error.getData() : ""), error);
            }
            return value;
        } catch (IOException e) {
            throw new RuntimeException("Failed to send message: " + e, e.getCause());
        }
    }

}

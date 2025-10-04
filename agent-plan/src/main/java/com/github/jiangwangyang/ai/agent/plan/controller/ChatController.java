package com.github.jiangwangyang.ai.agent.plan.controller;

import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
public class ChatController {

    private final int MAX_MESSAGES = 100;
    private final InMemoryChatMemoryRepository chatMemoryRepository = new InMemoryChatMemoryRepository();
    private final MessageWindowChatMemory messageWindowChatMemory = MessageWindowChatMemory.builder()
            .chatMemoryRepository(chatMemoryRepository)
            .maxMessages(MAX_MESSAGES)
            .build();

    @Autowired
    @Qualifier("planAgent")
    private BaseAgent planAgent;
    @Autowired
    @Qualifier("collaborativeAgent")
    private BaseAgent collaborativeAgent;

    @RequestMapping("/chat")
    public Flux<String> chat(@RequestParam("message") String message, @RequestParam("user") String user) throws GraphStateException, GraphRunnerException {
        messageWindowChatMemory.add(user, new UserMessage(message));
        StringBuilder stringBuilder = new StringBuilder();
        return collaborativeAgent
                .stream(Map.of(
                        "messages", messageWindowChatMemory.get(user),
                        "user", user
                ))
                .mapNotNull(output -> {
                    if (output.isSTART() || output.isEND()) {
                        return null;
                    }
                    if (output instanceof StreamingOutput) {
                        return ((StreamingOutput) output).chunk();
                    }
                    return null;
                })
                .doOnNext(stringBuilder::append)
                .doFinally(signalType -> messageWindowChatMemory.add(user, new AssistantMessage(stringBuilder.toString())));
    }

    @RequestMapping("/plan")
    public Flux<String> plan(@RequestParam("message") String message, @RequestParam("user") String user) throws GraphStateException, GraphRunnerException {
        messageWindowChatMemory.add(user, new UserMessage(message));
        StringBuilder stringBuilder = new StringBuilder();
        return planAgent
                .stream(Map.of(
                        "messages", messageWindowChatMemory.get(user),
                        "user", user
                ))
                .mapNotNull(output -> {
                    if (output.isSTART() || output.isEND()) {
                        return null;
                    }
                    if (output instanceof StreamingOutput) {
                        return ((StreamingOutput) output).chunk();
                    }
                    return null;
                })
                .doOnNext(stringBuilder::append)
                .doFinally(signalType -> messageWindowChatMemory.add(user, new AssistantMessage(stringBuilder.toString())));
    }

}

package com.github.jiangwangyang.ai.client.controller;

import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

@RestController
public class ChatController {

    @Autowired
    private BaseAgent baseAgent;

    @GetMapping("/chat")
    public Flux<String> chat(@RequestParam("message") String message) throws GraphStateException, GraphRunnerException {
        return baseAgent
                .stream(Map.of("input", List.of(new UserMessage(message))))
                .mapNotNull(output -> {
                    if (output.isSTART() || output.isEND()) {
                        return null;
                    }
                    if (output instanceof StreamingOutput) {
                        return ((StreamingOutput) output).chunk();
                    }
                    return null;
                }).publishOn(Schedulers.parallel());
    }

}

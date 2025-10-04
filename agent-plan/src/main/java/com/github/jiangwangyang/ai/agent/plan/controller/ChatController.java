package com.github.jiangwangyang.ai.agent.plan.controller;

import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
public class ChatController {

    @Autowired
    @Qualifier("planAgent")
    private BaseAgent planAgent;
    @Autowired
    @Qualifier("collaborativeAgent")
    private BaseAgent collaborativeAgent;

    @RequestMapping("/chat")
    public Flux<String> chat(@RequestParam("message") String message, @RequestParam("user") String user) throws GraphStateException, GraphRunnerException {
        return collaborativeAgent
                .stream(Map.of(
                        "messages", List.of(new UserMessage(message)),
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
                });
    }

    @RequestMapping("/plan")
    public Flux<String> plan(@RequestParam("message") String message, @RequestParam("user") String user) throws GraphStateException, GraphRunnerException {
        return planAgent
                .stream(Map.of(
                        "messages", List.of(new UserMessage(message)),
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
                });
    }

}

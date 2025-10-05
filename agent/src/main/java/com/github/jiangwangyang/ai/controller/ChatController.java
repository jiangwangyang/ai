package com.github.jiangwangyang.ai.controller;

import com.github.jiangwangyang.ai.common.a2a.WebClientA2AClient;
import com.github.jiangwangyang.ai.common.agent.BaseAgent;
import com.github.jiangwangyang.ai.common.tool.PlanTool;
import io.a2a.A2A;
import io.a2a.spec.Message;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public class ChatController {

    @Autowired
    private WebClientA2AClient webClientA2AClient;
    @Autowired
    private PlanTool planTool;
    @Autowired
    @Qualifier("planAgent")
    private BaseAgent planAgent;
    @Autowired
    @Qualifier("reactAgent")
    private BaseAgent reactAgent;

    @RequestMapping("/plan")
    public Flux<String> plan(@RequestParam("user") String user,
                             @RequestParam("message") String message) {
        String conversationId = UUID.randomUUID().toString();
        Sinks.Many<String> sinks = Sinks.many().unicast().onBackpressureBuffer();

        CompletableFuture.runAsync(() -> {
            try {
                // 创建计划
                planAgent.run(conversationId, user, message)
                        .doOnNext(sinks::tryEmitNext)
                        .blockLast();
                PlanTool.Plan plan = planTool.getPlan(conversationId);
                sinks.tryEmitNext("\n\n");
                // 输出计划
                if (plan == null) {
                    sinks.tryEmitNext("计划创建失败");
                    return;
                }
                sinks.tryEmitNext("**[PLAN]** " + plan.title() + "\n\n");
                for (String step : plan.steps()) {
                    sinks.tryEmitNext("**[STEP]** " + step + "\n\n");
                }
                // 执行计划
                for (String step : plan.steps()) {
                    reactAgent.run(conversationId, user, step)
                            .doOnNext(sinks::tryEmitNext)
                            .blockLast();
                    sinks.tryEmitNext("\n\n");
                }
            } catch (Exception e) {
                sinks.tryEmitError(e);
            } finally {
                planTool.removePlan(conversationId);
                sinks.tryEmitComplete();
            }
        });
        return sinks.asFlux();
    }

    @RequestMapping("/chat")
    public Flux<String> chat(@RequestParam("user") String user,
                             @RequestParam("message") String message) {
        return webClientA2AClient.sendStreamingMessage("http://localhost:8000/a2a", A2A.toUserMessage(message), Collections.emptyMap())
                .flatMap(streamingEventKind -> {
                    if (streamingEventKind instanceof TaskStatusUpdateEvent event) {
                        String msg = Optional.of(event.getStatus())
                                .map(TaskStatus::message)
                                .map(Message::getParts)
                                .filter(parts -> !parts.isEmpty())
                                .map(parts -> parts.get(0))
                                .filter(part -> part instanceof TextPart)
                                .map(part -> (TextPart) part)
                                .map(TextPart::getText)
                                .orElse("");
                        return Flux.just(msg);
                    }
                    return Flux.empty();
                });
    }

}

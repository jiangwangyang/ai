package com.github.jiangwangyang.ai.agent.controller;

import com.github.jiangwangyang.ai.agent.OpenAiChatModelWithoutTool;
import com.github.jiangwangyang.ai.agent.agent.BaseAgent;
import com.github.jiangwangyang.ai.agent.service.PlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public class ChatController {

    @Autowired
    private PlanService planService;
    @Autowired
    private OpenAiChatModelWithoutTool chatModel;
    @Autowired
    @Qualifier("reactAgent")
    private BaseAgent reactAgent;
    @Autowired
    @Qualifier("planAgent")
    private BaseAgent planAgent;

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
                PlanService.Plan plan = planService.getPlan(conversationId);
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
                sinks.tryEmitComplete();
            }
        });
        return sinks.asFlux();
    }

    @RequestMapping("/chat")
    public Flux<String> chat(@RequestParam("user") String user,
                             @RequestParam("message") String message) {
        return reactAgent.run(UUID.randomUUID().toString(), user, message);
    }

}

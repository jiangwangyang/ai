package com.github.jiangwangyang.ai.dify.controller;

import com.github.jiangwangyang.ai.dify.common.DifyAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
public class ChatController {

    @Autowired
    private DifyAgent difyAgent;

    @RequestMapping("/chat")
    public Flux<String> chat(@RequestParam("message") String message) {
        return difyAgent.stream(message, Map.of(), Map.of())
                .map(Object::toString)
                .doOnNext(System.out::println);
    }

}

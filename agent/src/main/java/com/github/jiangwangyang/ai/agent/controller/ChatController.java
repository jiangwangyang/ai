package com.github.jiangwangyang.ai.agent.controller;

import com.github.jiangwangyang.ai.agent.OpenAiChatModelWithoutTool;
import com.github.jiangwangyang.ai.agent.agent.BaseAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class ChatController {

    @Autowired
    private OpenAiChatModelWithoutTool chatModel;

    @Autowired
    private BaseAgent agent;

    @RequestMapping("/chat")
    public Flux<String> chat(@RequestParam("user") String user,
                             @RequestParam("message") String message) {
        return agent.run(user, message);
    }

}

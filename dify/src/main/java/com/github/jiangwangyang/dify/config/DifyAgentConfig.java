package com.github.jiangwangyang.dify.config;

import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.github.jiangwangyang.dify.DifyAgent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Configuration
public class DifyAgentConfig {

    @Bean("difyAgent")
    public BaseAgent difyAgent(WebClient webClient) throws GraphStateException {
        return new DifyAgent(webClient,
                "http://localhost/v1/chat-messages",
                Map.of("Authorization", "Bearer " + System.getenv("API_KEY")),
                "dify_agent", "Dify平台提供的Agent，可以和用户进行聊天");
    }

}

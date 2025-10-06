package com.github.jiangwangyang.ai.dify.config;

import com.github.jiangwangyang.ai.dify.common.DifyAgent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class DifyAgentConfig {

    @Bean
    public DifyAgent difyAgent(WebClient webClient) {
        return new DifyAgent(webClient, "https://localhost/v1/chat-messages");
    }

}

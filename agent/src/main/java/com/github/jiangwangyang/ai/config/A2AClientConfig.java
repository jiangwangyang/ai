package com.github.jiangwangyang.ai.config;

import com.github.jiangwangyang.ai.common.a2a.WebClientA2AClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class A2AClientConfig {

    @Bean
    public WebClientA2AClient webClientA2AClient(WebClient.Builder webClientBuilder) {
        return new WebClientA2AClient(webClientBuilder.build());
    }

}

package com.github.jiangwangyang.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    /**
     * 配置访问MCP服务的Header
     */
    @Bean
    public WebClient.Builder webClientBuilder(@Value("${spring.ai.openai.api-key}") String apiKey) {
        return WebClient.builder()
                .defaultHeader("Authorization", "Bearer " + apiKey);
    }

}

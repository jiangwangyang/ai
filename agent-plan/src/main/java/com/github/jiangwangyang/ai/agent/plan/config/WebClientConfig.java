package com.github.jiangwangyang.ai.agent.plan.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 配置访问MCP服务时添加Authorization
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClient(@Value("${spring.ai.dashscope.api-key}") String apiKey) {
        return WebClient.builder()
                .defaultHeader("Authorization", "Bearer " + apiKey);
    }

}

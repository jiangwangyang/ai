package com.github.jiangwangyang.ai.common.autoconfigure;

import com.alibaba.cloud.ai.a2a.A2aServerAgentCardProperties;
import com.alibaba.cloud.ai.a2a.A2aServerProperties;
import com.alibaba.cloud.ai.a2a.route.JsonRpcA2aRouterProvider;
import com.alibaba.cloud.ai.a2a.server.JsonRpcA2aRequestHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration
@EnableConfigurationProperties({A2aServerProperties.class, A2aServerAgentCardProperties.class})
public class A2aServerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RouterFunction<ServerResponse> a2aRouterFunction(A2aServerProperties a2aServerProperties, JsonRpcA2aRequestHandler a2aRequestHandler) {
        return (new JsonRpcA2aRouterProvider(a2aServerProperties.getAgentCardUrl(), a2aServerProperties.getMessageUrl())).getRouter(a2aRequestHandler);
    }

}

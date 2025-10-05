package com.github.jiangwangyang.ai.common.autoconfigure;

import com.alibaba.cloud.ai.a2a.A2aServerAgentCardProperties;
import com.alibaba.cloud.ai.a2a.A2aServerProperties;
import com.alibaba.cloud.ai.a2a.server.A2aServerExecutorProvider;
import com.alibaba.cloud.ai.a2a.server.DefaultA2aServerExecutorProvider;
import com.alibaba.cloud.ai.a2a.server.JsonRpcA2aRequestHandler;
import com.github.jiangwangyang.ai.common.a2a.DefaultAgentExecutor;
import com.github.jiangwangyang.ai.common.agent.BaseAgent;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.events.InMemoryQueueManager;
import io.a2a.server.events.QueueManager;
import io.a2a.server.requesthandlers.DefaultRequestHandler;
import io.a2a.server.requesthandlers.JSONRPCHandler;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.server.tasks.*;
import io.a2a.spec.AgentCard;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({A2aServerProperties.class, A2aServerAgentCardProperties.class})
public class A2aServerHandlerAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public A2aServerExecutorProvider a2aServerExecutorProvider() {
        return new DefaultA2aServerExecutorProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentExecutor agentExecutor(BaseAgent agent) {
        return new DefaultAgentExecutor(agent);
    }

    @Bean
    @ConditionalOnMissingBean
    public TaskStore taskStore() {
        return new InMemoryTaskStore();
    }

    @Bean
    @ConditionalOnMissingBean
    public QueueManager queueManager() {
        return new InMemoryQueueManager();
    }

    @Bean
    @ConditionalOnMissingBean
    public PushNotificationConfigStore pushConfigStore() {
        return new InMemoryPushNotificationConfigStore();
    }

    @Bean
    @ConditionalOnMissingBean
    public PushNotificationSender pushSender(PushNotificationConfigStore pushConfigStore) {
        return new BasePushNotificationSender(pushConfigStore);
    }

    @Bean
    @ConditionalOnMissingBean
    public RequestHandler requestHandler(AgentExecutor agentExecutor, TaskStore taskStore, QueueManager queueManager, PushNotificationConfigStore pushConfigStore, PushNotificationSender pushSender, A2aServerExecutorProvider a2aServerExecutorProvider) {
        return new DefaultRequestHandler(agentExecutor, taskStore, queueManager, pushConfigStore, pushSender, a2aServerExecutorProvider.getA2aServerExecutor());
    }

    @Bean
    @ConditionalOnMissingBean
    public JSONRPCHandler jsonrpcHandler(AgentCard agentCard, RequestHandler requestHandler) {
        return new JSONRPCHandler(agentCard, requestHandler);
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonRpcA2aRequestHandler jsonRpcA2aRequestHandler(JSONRPCHandler jsonrpcHandler) {
        return new JsonRpcA2aRequestHandler(jsonrpcHandler);
    }
}

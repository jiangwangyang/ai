package com.github.jiangwangyang.ai.agent.config;

import com.github.jiangwangyang.ai.agent.OpenAiChatModelWithoutTool;
import com.github.jiangwangyang.ai.agent.agent.BaseAgent;
import com.github.jiangwangyang.ai.agent.agent.ReactAgent;
import com.github.jiangwangyang.ai.agent.service.PlanService;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.model.openai.autoconfigure.OpenAIAutoConfigurationUtil;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatProperties;
import org.springframework.ai.model.openai.autoconfigure.OpenAiConnectionProperties;
import org.springframework.ai.model.tool.DefaultToolExecutionEligibilityPredicate;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionEligibilityPredicate;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;

@Configuration
public class AgentConfig {

    @Autowired
    private ToolCallbackProvider toolCallbackProvider;
    @Autowired
    private ToolCallbackResolver toolCallbackResolver;
    @Autowired
    private PlanService planService;

    @Bean("planAgent")
    public BaseAgent planAgent(OpenAiChatModelWithoutTool openAiChatModelWithoutTool,
                               @Value("${agent.plan.system-prompt}") String systemPrompt) {
        return new ReactAgent(
                openAiChatModelWithoutTool,
                chatMemory(),
                List.of(ToolCallbacks.from(planService)),
                toolCallbackResolver,
                1,
                systemPrompt,
                "没有下一步"
        );
    }

    @Bean("reactAgent")
    public BaseAgent reactAgent(OpenAiChatModelWithoutTool openAiChatModelWithoutTool,
                                @Value("${agent.react.system-prompt}") String systemPrompt,
                                @Value("${agent.react.next-step-prompt}") String nextStepPrompt) {
        return new ReactAgent(
                openAiChatModelWithoutTool,
                chatMemory(),
                Arrays.stream(toolCallbackProvider.getToolCallbacks()).toList(),
                toolCallbackResolver,
                10,
                systemPrompt,
                nextStepPrompt
        );
    }


    @Bean
    public OpenAiChatModelWithoutTool openAiChatModelWithoutTool(OpenAiConnectionProperties commonProperties, OpenAiChatProperties chatProperties, ObjectProvider<RestClient.Builder> restClientBuilderProvider, ObjectProvider<WebClient.Builder> webClientBuilderProvider, ToolCallingManager toolCallingManager, RetryTemplate retryTemplate, ResponseErrorHandler responseErrorHandler, ObjectProvider<ObservationRegistry> observationRegistry, ObjectProvider<ChatModelObservationConvention> observationConvention, ObjectProvider<ToolExecutionEligibilityPredicate> openAiToolExecutionEligibilityPredicate) {
        OpenAiApi openAiApi = this.openAiApi(chatProperties, commonProperties, restClientBuilderProvider.getIfAvailable(RestClient::builder), webClientBuilderProvider.getIfAvailable(WebClient::builder), responseErrorHandler, "chat");
        return new OpenAiChatModelWithoutTool(openAiApi,
                chatProperties.getOptions(),
                toolCallingManager,
                retryTemplate,
                observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP),
                openAiToolExecutionEligibilityPredicate.getIfUnique(DefaultToolExecutionEligibilityPredicate::new));
    }

    @Bean
    public ChatMemory chatMemory() {
        InMemoryChatMemoryRepository chatMemoryRepository = new InMemoryChatMemoryRepository();
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(1000)
                .build();
    }

    private OpenAiApi openAiApi(OpenAiChatProperties chatProperties, OpenAiConnectionProperties commonProperties, RestClient.Builder restClientBuilder, WebClient.Builder webClientBuilder, ResponseErrorHandler responseErrorHandler, String modelType) {
        OpenAIAutoConfigurationUtil.ResolvedConnectionProperties resolved = OpenAIAutoConfigurationUtil.resolveConnectionProperties(commonProperties, chatProperties, modelType);
        return OpenAiApi.builder().baseUrl(resolved.baseUrl()).apiKey(new SimpleApiKey(resolved.apiKey())).headers(resolved.headers()).completionsPath(chatProperties.getCompletionsPath()).embeddingsPath("/v1/embeddings").restClientBuilder(restClientBuilder).webClientBuilder(webClientBuilder).responseErrorHandler(responseErrorHandler).build();
    }

}

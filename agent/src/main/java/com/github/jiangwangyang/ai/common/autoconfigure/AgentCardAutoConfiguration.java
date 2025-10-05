package com.github.jiangwangyang.ai.common.autoconfigure;

import com.alibaba.cloud.ai.a2a.A2aServerAgentCardProperties;
import com.alibaba.cloud.ai.a2a.A2aServerProperties;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@EnableConfigurationProperties({A2aServerProperties.class, A2aServerAgentCardProperties.class})
public class AgentCardAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AgentCard agentCard(A2aServerProperties a2aServerProperties, A2aServerAgentCardProperties a2AServerAgentCardProperties) {
        return (new AgentCard.Builder())
                .name(a2AServerAgentCardProperties.getName() == null ? "" : a2AServerAgentCardProperties.getName())
                .description(a2AServerAgentCardProperties.getDescription() == null ? "" : a2AServerAgentCardProperties.getDescription())
                .defaultInputModes(a2AServerAgentCardProperties.getDefaultInputModes() == null ? List.of() : a2AServerAgentCardProperties.getDefaultInputModes())
                .defaultOutputModes(a2AServerAgentCardProperties.getDefaultOutputModes() == null ? List.of() : a2AServerAgentCardProperties.getDefaultOutputModes())
                .skills(a2AServerAgentCardProperties.getSkills() == null ? List.of() : a2AServerAgentCardProperties.getSkills())
                .capabilities(a2AServerAgentCardProperties.getCapabilities() == null ? new AgentCapabilities(true, false, false, List.of()) : a2AServerAgentCardProperties.getCapabilities())
                .version(a2aServerProperties.getVersion() == null ? "" : a2aServerProperties.getVersion())
                .protocolVersion("0.2.5")
                .url(a2aServerProperties.getMessageUrl())
                .preferredTransport(a2aServerProperties.getType())
                .supportsAuthenticatedExtendedCard(a2AServerAgentCardProperties.isSupportsAuthenticatedExtendedCard())
                .provider(a2AServerAgentCardProperties.getProvider())
                .documentationUrl(a2AServerAgentCardProperties.getDocumentationUrl())
                .security(a2AServerAgentCardProperties.getSecurity())
                .securitySchemes(a2AServerAgentCardProperties.getSecuritySchemes())
                .iconUrl(a2AServerAgentCardProperties.getIconUrl())
                .additionalInterfaces(a2AServerAgentCardProperties.getAdditionalInterfaces())
                .build();
    }

}

package com.github.jiangwangyang.ai.mcp.memory;

import com.github.jiangwangyang.ai.mcp.memory.service.MemoryService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class McpApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider toolCallbackProvider(MemoryService memoryService) {
        return MethodToolCallbackProvider.builder().toolObjects(memoryService).build();
    }

}

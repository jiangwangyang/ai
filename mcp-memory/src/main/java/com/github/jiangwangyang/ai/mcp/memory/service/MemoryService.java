package com.github.jiangwangyang.ai.mcp.memory.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MemoryService {

    private final Map<String, String> memory = new ConcurrentHashMap<>();

    @Tool(name = "memory-store", description = """
            存储用户的多维度偏好和习惯信息
            在存储数据时，你应该同时记录当前时间，以确保数据的时效性和准确性
            在存储之前，你应该先查询用户历史记录，以确定是否需要更新或合并新的偏好和习惯信息
            """)
    public String storeMemory(
            @ToolParam(description = "用户唯一标识符，用于关联用户的所有记忆信息") String userId,
            @ToolParam(description = "用户偏好和习惯的详细描述") String content) {
        return memory.put(userId, content);
    }

    @Tool(name = "memory-query", description = "查询用户的历史偏好、习惯和反馈信息")
    public String queryMemory(@ToolParam(description = "用户唯一标识符，用于检索该用户的所有记忆信息") String userId) {
        return memory.get(userId);
    }

}

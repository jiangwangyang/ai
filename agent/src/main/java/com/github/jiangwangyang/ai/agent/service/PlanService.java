package com.github.jiangwangyang.ai.agent.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PlanService {

    private final Map<String, Plan> planMap = new ConcurrentHashMap<>();

    public Plan getPlan(String conversationId) {
        return planMap.get(conversationId);
    }

    @Tool(description = "这是一个计划工具，可让代理创建和管理用于解决复杂任务的计划。\n该工具提供创建计划功能。\n使用中文回答")
    public String createPlan(
            @ToolParam(description = "对话ID，用于标识不同的对话") String conversationId,
            @ToolParam(description = "计划标题，描述总体计划") String title,
            @ToolParam(description = "计划中每一步骤列表，表示该步骤具体要求和执行动作的描述") List<String> steps) {
        planMap.put(conversationId, new Plan(title, steps));
        return "我已创建plan";
    }

    public record Plan(String title, List<String> steps) {
    }

}

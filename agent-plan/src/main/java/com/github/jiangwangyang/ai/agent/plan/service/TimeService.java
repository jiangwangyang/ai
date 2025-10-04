package com.github.jiangwangyang.ai.agent.plan.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TimeService {

    @Tool(name = "get-time", description = "获取当前时间")
    public String getTime() {
        return String.valueOf(LocalDateTime.now());
    }

}

package com.github.jiangwangyang.ai.mcp.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
public class WeatherService {

    @Tool(description = "获取指定城市的天气信息")
    public String getWeather(@ToolParam(description = "城市名称") String city) {
        return "天气晴朗";
    }
    
}

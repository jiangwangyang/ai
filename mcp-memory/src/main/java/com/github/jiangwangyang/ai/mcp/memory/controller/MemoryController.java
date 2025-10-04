package com.github.jiangwangyang.ai.mcp.memory.controller;

import com.github.jiangwangyang.ai.mcp.memory.service.MemoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/memory")
public class MemoryController {

    @Autowired
    private MemoryService memoryService;

    @GetMapping
    public Map<String, String> memory() {
        return memoryService.getMemory();
    }

}

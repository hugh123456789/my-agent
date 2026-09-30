package com.agent.codeagent.agents.tool;

import dev.langchain4j.agent.tool.ToolSpecification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class ToolDispatcher implements ToolExecutor {

    private final Map<String, ToolExecutor> executors = new LinkedHashMap<>();
    private final List<ToolSpecification> specifications = new ArrayList<>();

    /**
     * 注册一个工具。
     *
     * @param spec     工具规格（名称、描述、参数 schema）
     * @param executor 工具执行逻辑
     */
    public void register(ToolSpecification spec, ToolExecutor executor) {
        specifications.add(spec);
        executors.put(spec.name(), executor);
    }

    /**
     * 统一执行入口 — 根据工具名称分发到对应的执行器。
     */
    @Override
    public String execute(String toolName, String arguments) throws Exception {
        return execute(null, toolName, arguments);
    }

    @Override
    public String execute(String sessionId, String toolName, String arguments) throws Exception {
        ToolExecutor executor = executors.get(toolName);
        if (executor == null) {
            return "Unknown tool: " + toolName;
        }
        return executor.execute(sessionId, toolName, arguments);
    }

    /**
     * 获取所有已注册的工具规格列表。
     */
    public List<ToolSpecification> getSpecifications() {
        return Collections.unmodifiableList(specifications);
    }
}

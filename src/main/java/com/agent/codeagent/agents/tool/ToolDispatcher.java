package com.agent.codeagent.agents.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


public class ToolDispatcher implements ToolExecutor {

    private final Map<String, ToolExecutor> executors = new LinkedHashMap<>();
    private final List<ToolSpecification> specifications = new ArrayList<>();
    private final Set<String> deferredToolNames = new LinkedHashSet<>();
    private final Map<String, Set<String>> activatedToolsBySession = new ConcurrentHashMap<>();
    private final Map<String, Boolean> concurrencySafeByTool = new LinkedHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private boolean currentRegistrarConcurrencySafe;

    /**
     * 注册一个工具。
     *
     * @param spec     工具规格（名称、描述、参数 schema）
     * @param executor 工具执行逻辑
     */
    public void register(ToolSpecification spec, ToolExecutor executor) {
        register(spec, executor, currentRegistrarConcurrencySafe);
    }

    public void register(ToolSpecification spec, ToolExecutor executor, boolean concurrencySafe) {
        specifications.add(spec);
        executors.put(spec.name(), executor);
        concurrencySafeByTool.put(spec.name(), concurrencySafe);
    }

    /** Sets the safety metadata inherited by registrations made by one registrar. */
    public void setCurrentRegistrarConcurrencySafe(boolean concurrencySafe) {
        currentRegistrarConcurrencySafe = concurrencySafe;
    }

    public boolean isConcurrencySafe(String toolName) {
        return concurrencySafeByTool.getOrDefault(toolName, false);
    }

    public void markDeferred(List<String> names) {
        deferredToolNames.addAll(names);
    }

    public List<ToolSpecification> getActiveToolDefinitions(String sessionId) {
        Set<String> activated = activatedToolsBySession.getOrDefault(
                sessionKey(sessionId), Set.of());
        return specifications.stream()
                .filter(spec -> !deferredToolNames.contains(spec.name())
                        || activated.contains(spec.name()))
                .toList();
    }

    public List<String> getDeferredToolNames() {
        return deferredToolNames.stream().toList();
    }

    public void activateTools(String sessionId, List<String> names) {
        activatedToolsBySession.computeIfAbsent(sessionKey(sessionId), ignored -> ConcurrentHashMap.newKeySet())
                .addAll(names.stream().filter(deferredToolNames::contains).toList());
    }

    public String searchAndActivate(String sessionId, String query) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        List<ToolSpecification> matches = specifications.stream()
                .filter(spec -> deferredToolNames.contains(spec.name()))
                .filter(spec -> normalizedQuery.isBlank()
                        || spec.name().toLowerCase().contains(normalizedQuery)
                        || spec.description().toLowerCase().contains(normalizedQuery))
                .toList();
        activateTools(sessionId, matches.stream().map(ToolSpecification::name).toList());
        if (matches.isEmpty()) {
            return "No deferred tools matched query: " + query;
        }
        return matches.stream()
                .map(spec -> spec.name() + ": " + spec.description())
                .collect(Collectors.joining("\n"));
    }

    public void registerToolSearch() {
        register(ToolSpecification.builder()
                        .name("tool_search")
                        .description("搜索并激活延迟加载的工具。激活后，下一轮请求会提供该工具的完整参数 schema。")
                        .parameters(JsonObjectSchema.builder()
                                .addProperty("query", JsonStringSchema.builder()
                                        .description("工具名称或用途关键词")
                                        .build())
                                .required("query")
                                .build())
                        .build(),
                (sessionId, toolName, arguments) -> searchAndActivate(sessionId, parseQuery(arguments)));
    }

    private String parseQuery(String arguments) {
        try {
            JsonNode query = objectMapper.readTree(arguments).get("query");
            return query == null ? "" : query.asText();
        } catch (Exception error) {
            return "";
        }
    }

    private String sessionKey(String sessionId) {
        return sessionId == null ? "<anonymous>" : sessionId;
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

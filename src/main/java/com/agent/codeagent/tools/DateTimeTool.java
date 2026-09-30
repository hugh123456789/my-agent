package com.agent.codeagent.tools;

import com.agent.codeagent.agents.tool.ToolDispatcher;
import com.agent.codeagent.agents.tool.ToolRegistrar;
import dev.langchain4j.agent.tool.ToolSpecification;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日期时间工具 — 提供获取当前日期时间的方法。
 * 工具规格和注册逻辑由本类自身维护。
 */
@Component
public class DateTimeTool implements ToolRegistrar {

    @Override
    public boolean deferred() {
        return true;
    }

    @Override
    public void register(ToolDispatcher dispatcher) {
        dispatcher.register(specification("getCurrentDateTime", "获取当前的日期和时间"),
                (sessionId, toolName, arguments) -> getCurrentDateTime());
        dispatcher.register(specification("getCurrentDate", "获取当前的日期"),
                (sessionId, toolName, arguments) -> getCurrentDate());
        dispatcher.register(specification("getCurrentTime", "获取当前的时间"),
                (sessionId, toolName, arguments) -> getCurrentTime());
    }

    public String getCurrentDateTime() {
        LocalDateTime now = LocalDateTime.now();
        return now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public String getCurrentDate() {
        LocalDateTime now = LocalDateTime.now();
        return now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    public String getCurrentTime() {
        LocalDateTime now = LocalDateTime.now();
        return now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    private ToolSpecification specification(String name, String description) {
        return ToolSpecification.builder().name(name).description(description).build();
    }
}

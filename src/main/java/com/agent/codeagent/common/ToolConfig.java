package com.agent.codeagent.common;

import com.agent.codeagent.agents.tool.ToolDispatcher;
import com.agent.codeagent.agents.tool.ToolRegistrar;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ToolConfig {

    @Bean("mainToolDispatcher")
    public ToolDispatcher mainToolDispatcher(List<ToolRegistrar> tools) {
        return createDispatcher(tools, false);
    }

    @Bean("subAgentToolDispatcher")
    public ToolDispatcher subAgentToolDispatcher(List<ToolRegistrar> tools) {
        return createDispatcher(tools, true);
    }

    private ToolDispatcher createDispatcher(List<ToolRegistrar> tools, boolean subAgent) {
        ToolDispatcher dispatcher = new ToolDispatcher();
        tools.stream()
                .filter(tool -> !subAgent || tool.availableToSubAgent())
                .forEach(tool -> tool.register(dispatcher));
        return dispatcher;
    }
}

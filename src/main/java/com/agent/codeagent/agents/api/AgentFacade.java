package com.agent.codeagent.agents.api;

import com.agent.codeagent.agents.execution.AgentLoopExecutor;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * 门面层 — 统一封装 Agent 循环入口，
 * Controller 只需依赖这一个类。
 */
@Service
public class AgentFacade {

    private final AgentLoopExecutor agentLoopExecutor;

    public AgentFacade(AgentLoopExecutor agentLoopExecutor) {
        this.agentLoopExecutor = agentLoopExecutor;
    }

    public Flux<AgentEvent> agentLoop(String sessionId, String userMessage) {
        return agentLoopExecutor.execute(sessionId, userMessage);
    }

    /**
     * 用户审批工具调用后恢复 Agent 循环
     */
    public Flux<AgentEvent> approveTool(String requestId, boolean approved) {
        return agentLoopExecutor.approve(requestId, approved);
    }
}

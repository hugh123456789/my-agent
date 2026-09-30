package com.agent.codeagent.web;

import com.agent.codeagent.agents.api.AgentEvent;
import com.agent.codeagent.agents.api.AgentFacade;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/agent")
public class AgentController {

    private final AgentFacade agentFacade;

    public AgentController(AgentFacade agentFacade) {
        this.agentFacade = agentFacade;
    }

    @GetMapping(value = "/loop", produces = "text/event-stream;charset=UTF-8")
    public Flux<AgentEvent> agentLoop(
            @RequestParam(required = false) String sessionId,
            @RequestParam(defaultValue = "你好") String message) {
        return agentFacade.agentLoop(sessionId, message);
    }

    @GetMapping(value = "/loop/approve", produces = "text/event-stream;charset=UTF-8")
    public Flux<AgentEvent> approveTool(@RequestParam String requestId,
                                         @RequestParam(defaultValue = "true") boolean approved) {
        return agentFacade.approveTool(requestId, approved);
    }

}

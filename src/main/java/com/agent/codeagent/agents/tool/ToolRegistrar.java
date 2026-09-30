package com.agent.codeagent.agents.tool;

/** A tool owns its public specification and registration wiring. */
public interface ToolRegistrar {

    void register(ToolDispatcher dispatcher);

    default boolean availableToSubAgent() {
        return true;
    }
}

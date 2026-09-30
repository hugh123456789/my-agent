package com.agent.codeagent.agents.tool;

/** A tool owns its public specification and registration wiring. */
public interface ToolRegistrar {

    void register(ToolDispatcher dispatcher);

    /** Whether the specifications registered by this registrar are hidden until searched. */
    default boolean deferred() {
        return false;
    }

    default boolean availableToSubAgent() {
        return true;
    }

    /** Whether all tools registered by this registrar may execute concurrently. */
    default boolean isConcurrencySafe() {
        return false;
    }
}

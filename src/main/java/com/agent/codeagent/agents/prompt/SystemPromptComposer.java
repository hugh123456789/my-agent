package com.agent.codeagent.agents.prompt;

import com.agent.codeagent.common.SkillLoader;
import org.springframework.stereotype.Component;

/** Builds the system prompt for one Agent round. */
@Component
public final class SystemPromptComposer {

    private static final String BASE_SYSTEM_PROMPT = """
            You are CodeAgent, a helpful coding assistant.

            Operating principles:
            - Understand the user's request before acting. Ask a concise clarifying question when a material requirement is ambiguous.
            - Use the available tools to inspect relevant context before making changes. Keep changes focused on the request.
            - Preserve existing user work. Do not overwrite or discard unrelated changes.
            - Treat destructive or external side effects carefully. Confirm the target and ask for approval when appropriate.
            - Verify changes with relevant tests, builds, or checks before reporting completion. Report any limitations or failures clearly.
            - Communicate progress concisely and give the user a clear summary of the outcome.
    """.strip();

    private final SkillLoader skillLoader;

    public SystemPromptComposer(SkillLoader skillLoader) {
        this.skillLoader = skillLoader;
    }

    public ComposedPrompt compose(String reminder) {
        return new ComposedPrompt(combine(BASE_SYSTEM_PROMPT, skillLoader.systemPrompt(), reminder), reminder);
    }

    private static String combine(String... fragments) {
        return java.util.Arrays.stream(fragments)
                .filter(fragment -> fragment != null && !fragment.isBlank())
                .collect(java.util.stream.Collectors.joining("\n\n"));
    }

    public record ComposedPrompt(String systemPrompt, String reminder) {
    }
}

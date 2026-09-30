package com.agent.codeagent.agents.prompt;

import com.agent.codeagent.common.SkillLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SystemPromptComposerTest {

    @Test
    void prefixesSkillsAndReminderWithBaseAgentInstructions() {
        SkillLoader skillLoader = mock(SkillLoader.class);
        when(skillLoader.systemPrompt()).thenReturn("skills");

        SystemPromptComposer composer = new SystemPromptComposer(skillLoader);

        String systemPrompt = composer.compose("todo reminder").systemPrompt();

        assertTrue(systemPrompt.startsWith("You are CodeAgent, a helpful coding assistant."));
        assertTrue(systemPrompt.contains("Understand the user's request before acting."));
        assertTrue(systemPrompt.endsWith("skills\n\ntodo reminder"));
    }

    @Test
    void combinesSkillPromptAndRoundReminder() {
        SkillLoader skillLoader = mock(SkillLoader.class);
        when(skillLoader.systemPrompt()).thenReturn("skills");

        SystemPromptComposer composer = new SystemPromptComposer(skillLoader);

        SystemPromptComposer.ComposedPrompt prompt = composer.compose("todo reminder");

        assertEquals("todo reminder", prompt.reminder());
        assertTrue(prompt.systemPrompt().endsWith("skills\n\ntodo reminder"));
    }

    @Test
    void keepsSkillPromptWhenReminderIsBlank() {
        SkillLoader skillLoader = mock(SkillLoader.class);
        when(skillLoader.systemPrompt()).thenReturn("skills");

        SystemPromptComposer composer = new SystemPromptComposer(skillLoader);

        SystemPromptComposer.ComposedPrompt prompt = composer.compose("  ");

        assertEquals("  ", prompt.reminder());
        assertTrue(prompt.systemPrompt().endsWith("skills"));
    }
}

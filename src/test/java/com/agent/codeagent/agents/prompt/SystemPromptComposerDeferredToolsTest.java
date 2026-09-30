package com.agent.codeagent.agents.prompt;

import com.agent.codeagent.common.SkillLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SystemPromptComposerDeferredToolsTest {

    @Test
    void tellsModelHowToActivateDeferredTools() {
        SkillLoader skillLoader = mock(SkillLoader.class);
        when(skillLoader.systemPrompt()).thenReturn("");

        String prompt = new SystemPromptComposer(skillLoader)
                .compose(null, "rare_tool, another_tool")
                .systemPrompt();

        assertTrue(prompt.contains("tool_search"));
        assertTrue(prompt.contains("rare_tool, another_tool"));
    }
}

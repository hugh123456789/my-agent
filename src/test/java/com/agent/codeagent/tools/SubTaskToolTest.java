package com.agent.codeagent.tools;

import com.agent.codeagent.agents.execution.SubAgentRunner;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubTaskToolTest {

    @Test
    void delegatesPromptAndReturnsChildText() throws Exception {
        SubTaskTool tool = new SubTaskTool(prompt -> "child result: " + prompt);

        assertEquals("child result: inspect files", tool.execute(
                "task", "{\"prompt\":\"inspect files\"}"));
    }

    @Test
    void returnsReadableErrorForMalformedArguments() throws Exception {
        SubTaskTool tool = new SubTaskTool(prompt -> "unused");

        String result = tool.execute("task", "not-json");

        assertEquals(true, result.startsWith("Error:"));
    }

    @Test
    void marksSpringConstructorForAutowiredInjection() throws Exception {
        Constructor<SubTaskTool> constructor = SubTaskTool.class.getConstructor(
                ObjectProvider.class, ObjectMapper.class);

        assertTrue(constructor.isAnnotationPresent(Autowired.class));
    }
}

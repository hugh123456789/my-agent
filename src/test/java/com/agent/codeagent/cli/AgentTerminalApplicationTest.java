package com.agent.codeagent.cli;

import com.agent.codeagent.agents.api.AgentEvent;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentTerminalApplicationTest {

    @Test
    void exitCommandStopsInteractiveClient() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        AgentTerminalApplication application = new AgentTerminalApplication(
                null,
                new BufferedReader(new InputStreamReader(
                        new ByteArrayInputStream("/exit\n".getBytes(StandardCharsets.UTF_8)),
                        StandardCharsets.UTF_8)),
                new PrintWriter(bytes, true, StandardCharsets.UTF_8));

        application.run();

        assertTrue(bytes.toString(StandardCharsets.UTF_8).contains("已退出"));
    }

    @Test
    void rendersThinkingAsOneTaggedBlockBeforeAnswer() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        AgentTerminalApplication application = new AgentTerminalApplication(
                null,
                null,
                new PrintWriter(bytes, true, StandardCharsets.UTF_8));
        Method render = AgentTerminalApplication.class.getDeclaredMethod("render", AgentEvent.class);
        render.setAccessible(true);

        render.invoke(application, new AgentEvent.ThinkingDelta("The "));
        render.invoke(application, new AgentEvent.ThinkingDelta("user"));
        render.invoke(application, new AgentEvent.TextDelta("你好"));
        render.invoke(application, new AgentEvent.Complete(null));

        assertEquals("\n<thinking>The user</thinking>\n你好\r\n", bytes.toString(StandardCharsets.UTF_8));
    }
}

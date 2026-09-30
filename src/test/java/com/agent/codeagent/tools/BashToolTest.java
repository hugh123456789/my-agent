package com.agent.codeagent.tools;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BashToolTest {

    @Test
    void acceptsJsonAndRawCommands() throws Exception {
        BashTool tool = new BashTool(Duration.ofSeconds(5));

        assertEquals("hello", tool.execute("{\"command\":\"echo hello\"}").trim());
        assertEquals("world", tool.execute("echo world").trim());
    }

    @Test
    void terminatesAProcessThatExceedsTheTimeout() throws Exception {
        BashTool tool = new BashTool(Duration.ofMillis(100));

        long started = System.nanoTime();
        String output = tool.execute("ping 127.0.0.1 -n 10 > nul");
        long elapsedMillis = Duration.ofNanos(System.nanoTime() - started).toMillis();

        assertTrue(elapsedMillis < 2_000, "process exceeded timeout: " + elapsedMillis);
        assertTrue(output.contains("超时") || output.contains("timeout"));
    }
}

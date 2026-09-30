package com.agent.codeagent.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrepSearchToolTest {

    @TempDir
    Path tempDir;

    @Test
    void searchesRecursivelyAndHonorsIncludePattern() throws Exception {
        Files.writeString(tempDir.resolve("one.txt"), "hello\nworld\n");
        Files.writeString(tempDir.resolve("two.java"), "hello();\n");

        String result = new GrepSearchTool().execute(
                "{\"pattern\":\"hello\",\"path\":\"" + path(tempDir) + "\",\"include\":\"*.txt\"}");

        assertTrue(result.contains("one.txt:1:hello"));
        assertTrue(!result.contains("two.java"));
    }

    @Test
    void limitsResultsToOneHundredMatches() throws Exception {
        Files.writeString(tempDir.resolve("many.txt"),
                IntStream.rangeClosed(1, 101).mapToObj(i -> "match " + i).reduce((a, b) -> a + "\n" + b).orElseThrow());

        String result = new GrepSearchTool().execute(
                "{\"pattern\":\"match\",\"path\":\"" + path(tempDir) + "\"}");

        assertEquals(100, result.lines().filter(line -> !line.startsWith("... and ")).count());
        assertTrue(result.contains("and 1 more matches"));
    }

    private static String path(Path path) {
        return path.toString().replace("\\", "\\\\");
    }
}

package com.agent.codeagent.tools;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReadFileToolTest {

    @Test
    void readsUtf8FileWithPaddedLineNumbers() throws Exception {
        Path file = Files.createTempFile("read-file-tool", ".txt");
        Files.writeString(file, "第一行\nsecond line");

        String result = new ReadFileTool(new FileVersionTracker()).execute(
                "session-a",
                "{\"file_path\":\"" + file.toString().replace("\\", "\\\\") + "\"}");

        assertEquals("   1 | 第一行\n   2 | second line", result);
    }

    @Test
    void returnsErrorWhenFileCannotBeRead() {
        String result = new ReadFileTool().execute("{\"file_path\":\"missing-file.txt\"}");

        assertTrue(result.startsWith("Error reading file: "));
    }
}

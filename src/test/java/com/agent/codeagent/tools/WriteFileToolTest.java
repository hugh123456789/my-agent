package com.agent.codeagent.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WriteFileToolTest {

    @TempDir
    Path tempDir;

    @Test
    void createsParentDirectoriesAndWritesContent() throws Exception {
        Path file = tempDir.resolve("nested").resolve("file.txt");

        String result = new WriteFileTool().execute(
                "{\"file_path\":\"" + path(file) + "\",\"content\":\"hello\\n世界\"}");

        assertEquals("Successfully wrote to " + file, result);
        assertEquals("hello\n世界", Files.readString(file));
    }

    @Test
    void returnsErrorWhenRequiredArgumentIsMissing() {
        String result = new WriteFileTool().execute("{\"file_path\":\"file.txt\"}");

        org.junit.jupiter.api.Assertions.assertTrue(result.startsWith("Error writing file:"));
    }

    @Test
    void refusesToOverwriteExistingFileWithoutReadingItFirst() throws Exception {
        Path file = tempDir.resolve("existing.txt");
        Files.writeString(file, "original");
        WriteFileTool writer = new WriteFileTool(new FileVersionTracker());

        String result = writer.execute("session-a", "{\"file_path\":\"" + path(file)
                + "\",\"content\":\"replacement\"}");

        org.junit.jupiter.api.Assertions.assertTrue(result.contains("must be read first"));
        assertEquals("original", Files.readString(file));
    }

    private static String path(Path path) {
        return path.toString().replace("\\", "\\\\");
    }
}

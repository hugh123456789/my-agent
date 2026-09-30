package com.agent.codeagent.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditFileToolTest {

    @TempDir
    Path tempDir;

    @Test
    void editsAUniqueStringAndSupportsQuoteNormalization() throws Exception {
        Path file = tempDir.resolve("sample.txt");
        Files.writeString(file, "const text = “value”;\n");
        FileVersionTracker tracker = new FileVersionTracker();
        new ReadFileTool(tracker).execute("session-a", json("{\"file_path\":\"%s\"}".formatted(path(file))));

        String result = new EditFileTool(tracker).execute("session-a", json("{\"file_path\":\"%s\",\"old_string\":\"const text = \\\"value\\\";\",\"new_string\":\"const text = $value;\"}".formatted(path(file))));

        assertEquals("Successfully edited " + file, result);
        assertEquals("const text = $value;\n", Files.readString(file));
    }

    @Test
    void rejectsMissingAndNonUniqueStrings() throws Exception {
        Path file = tempDir.resolve("sample.txt");
        Files.writeString(file, "same\nsame");
        FileVersionTracker tracker = new FileVersionTracker();
        new ReadFileTool(tracker).execute("session-a", json("{\"file_path\":\"%s\"}".formatted(path(file))));

        String result = new EditFileTool(tracker).execute("session-a", json("{\"file_path\":\"%s\",\"old_string\":\"same\",\"new_string\":\"new\"}".formatted(path(file))));

        assertTrue(result.contains("found 2 times"));
        assertEquals("same\nsame", Files.readString(file));
    }

    @Test
    void refusesToEditBeforeReadingOrAfterExternalModification() throws Exception {
        Path file = tempDir.resolve("sample.txt");
        Files.writeString(file, "before");
        FileVersionTracker tracker = new FileVersionTracker();
        EditFileTool editor = new EditFileTool(tracker);

        assertTrue(editor.execute("session-a", json("{\"file_path\":\"%s\",\"old_string\":\"before\",\"new_string\":\"first\"}".formatted(path(file))))
                .contains("must be read first"));

        new ReadFileTool(tracker).execute("session-a", json("{\"file_path\":\"%s\"}".formatted(path(file))));
        Files.writeString(file, "external change");

        assertTrue(editor.execute("session-a", json("{\"file_path\":\"%s\",\"old_string\":\"external change\",\"new_string\":\"second\"}".formatted(path(file))))
                .contains("changed externally"));
    }

    private static String json(String value) {
        return value;
    }

    private static String path(Path path) {
        return path.toString().replace("\\", "\\\\");
    }
}

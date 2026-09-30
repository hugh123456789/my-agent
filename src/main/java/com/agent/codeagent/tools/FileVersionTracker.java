package com.agent.codeagent.tools;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks the version of files read by each agent session. */
@Component
public final class FileVersionTracker {

    private final Map<String, Map<Path, FileVersion>> versionsBySession = new ConcurrentHashMap<>();

    public void recordRead(String sessionId, Path path) throws IOException {
        remember(sessionId, normalize(path));
    }

    public void recordWritten(String sessionId, Path path) throws IOException {
        remember(sessionId, normalize(path));
    }

    public void requireUnchanged(String sessionId, Path path) throws IOException {
        path = normalize(path);
        FileVersion expected = versionsBySession
                .getOrDefault(sessionKey(sessionId), Map.of())
                .get(path);
        if (expected == null) {
            throw new IllegalStateException("file must be read first: " + path);
        }

        FileVersion actual = capture(path);
        if (!expected.equals(actual)) {
            throw new IllegalStateException("file changed externally, please read it again: " + path);
        }
    }

    private void remember(String sessionId, Path path) throws IOException {
        versionsBySession.computeIfAbsent(sessionKey(sessionId), ignored -> new ConcurrentHashMap<>())
                .put(path, capture(path));
    }

    private Path normalize(Path path) {
        return path.toAbsolutePath().normalize();
    }

    private FileVersion capture(Path path) throws IOException {
        byte[] content = Files.readAllBytes(path);
        return new FileVersion(
                Files.getLastModifiedTime(path).toMillis(),
                content.length,
                sha256(content));
    }

    private String sessionKey(String sessionId) {
        return sessionId == null ? "<anonymous>" : sessionId;
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }

    private record FileVersion(long mtimeMs, long size, String contentHash) {
    }
}

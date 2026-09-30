package com.agent.codeagent.tools;

import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/** Serializes writes to the same normalized path within this application process. */
@Component
public final class FileWriteCoordinator {

    private final ConcurrentHashMap<Path, ReentrantLock> locks = new ConcurrentHashMap<>();

    public <T> T withExclusiveWrite(Path path, WriteAction<T> action) throws Exception {
        ReentrantLock lock = locks.computeIfAbsent(
                path.toAbsolutePath().normalize(), ignored -> new ReentrantLock());
        lock.lock();
        try {
            return action.run();
        } finally {
            lock.unlock();
        }
    }

    @FunctionalInterface
    public interface WriteAction<T> {
        T run() throws Exception;
    }
}

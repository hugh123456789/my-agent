package com.agent.codeagent.tools;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileWriteCoordinatorTest {

    @Test
    void serializesWritesForTheSameFile() throws Exception {
        FileWriteCoordinator coordinator = new FileWriteCoordinator();
        Path file = Path.of("same-file.txt");
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicBoolean secondEntered = new AtomicBoolean();

        Thread first = new Thread(() -> run(coordinator, file, () -> {
            firstEntered.countDown();
            releaseFirst.await();
            return null;
        }));
        Thread second = new Thread(() -> run(coordinator, file, () -> {
            secondEntered.set(true);
            return null;
        }));

        first.start();
        assertTrue(firstEntered.await(1, TimeUnit.SECONDS));
        second.start();
        Thread.sleep(100);
        assertFalse(secondEntered.get());

        releaseFirst.countDown();
        first.join(1_000);
        second.join(1_000);
        assertTrue(secondEntered.get());
    }

    private static void run(FileWriteCoordinator coordinator, Path path,
                            FileWriteCoordinator.WriteAction<Void> action) {
        try {
            coordinator.withExclusiveWrite(path, action);
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
    }
}

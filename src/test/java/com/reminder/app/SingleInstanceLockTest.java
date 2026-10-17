package com.reminder.app;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SingleInstanceLockTest {

    @AfterEach
    void releaseLock() {
        SingleInstanceLock.release();
    }

    @Test
    void secondAcquireFailsAndReleaseAllowsReacquire(@TempDir Path dir) {
        assertTrue(SingleInstanceLock.acquire(dir));
        assertFalse(SingleInstanceLock.acquire(dir));

        SingleInstanceLock.release();

        assertTrue(SingleInstanceLock.acquire(dir));
    }
}
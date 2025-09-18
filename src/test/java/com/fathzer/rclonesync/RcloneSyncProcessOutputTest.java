package com.fathzer.rclonesync;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RcloneSyncProcessOutputTest {
    private RcloneSyncCmd rcloneSync;
    private SynchronizationResult result;
    private List<Progress> capturedProgress;

    @BeforeEach
    void setUp() {
        SynchronizationParameters parameters = new SynchronizationParameters("source", "destination");
        rcloneSync = new RcloneSyncCmd(parameters);
        result = new SynchronizationResult();
        capturedProgress = new ArrayList<>();
        parameters.withEventConsumer(p -> capturedProgress.add(p));
    }

    @Test
    void testProcessOutput_WithProgress() throws IOException {
        // Given
        String progressLine = "Transferred:  0 B / 0 B, -, 0 B/s, ETA -\n";
        String checksLine = "Checks:  0 / 0, -, Listed 11\n";
        String input = progressLine + checksLine;
        Supplier<InputStream> inputSupplier = () -> new ByteArrayInputStream(input.getBytes());

        // When
        rcloneSync.processOutput(inputSupplier, result);

        // Then
        assertFalse(capturedProgress.isEmpty(), "Expected at least one progress update");
        Progress progress = capturedProgress.get(0);
        assertNotNull(progress, "Progress should not be null");
    }

    @Test
    void testProcessOutput_WithNonProgressLines() throws IOException {
        // Given
        List<String> lines = List.of(
            "2023/01/01 12:00:00 INFO  : file.txt: Copied (new)",
            "2023/01/01 12:00:01 INFO  : file2.txt: Copied (replaced existing)",
            "2023/01/01 12:00:01 INFO  : file3.txt: Deleted",
            "2023/01/01 12:00:01 INFO  : file4.txt: Deleted"
        );
        String input = String.join("\n", lines);
        Supplier<InputStream> inputSupplier = () -> new ByteArrayInputStream(input.getBytes());

        // When
        rcloneSync.processOutput(inputSupplier, result);

        // Then
        assertTrue(capturedProgress.isEmpty(), "No progress should be captured for non-progress lines");
        assertEquals(2, result.deleted());
        assertEquals(1, result.replaced());
        assertEquals(1, result.copied());
    }

    @Test
    void testProcessOutput_WithIOException() throws IOException, InterruptedException {
        final InputStream failingInputStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("Test IOException");
            }
        };

        IOException exception = assertThrows(IOException.class, 
            () -> rcloneSync.processOutput(() -> failingInputStream, result));
        assertEquals("Test IOException", exception.getMessage());

        // Test the exception consumer
        List<IOException> exceptions = new LinkedList<>();
        SynchronizationParameters synchronizationParameters = new SynchronizationParameters("source", "destination");
        RcloneSyncCmd test = new RcloneSyncCmd(synchronizationParameters) {
            @Override
            protected Process buildProcess(List<String> cmd) {
                Process process = mock(Process.class);
                when(process.getInputStream()).thenReturn(failingInputStream);
                when(process.isAlive()).thenReturn(true);
                return process;
            }
        };
        final CountDownLatch latch = new CountDownLatch(1);
        synchronizationParameters.withExceptionConsumer(e -> {
            latch.countDown();
            exceptions.add(e);
        });
        Synchronization synchronization = test.run();
        latch.await(1, TimeUnit.SECONDS);
        synchronization.waitFor();
        assertEquals(1, exceptions.size());
        assertEquals("Test IOException", exceptions.get(0).getMessage());
    }

    @Test
    void testProcessOutput_EmptyInput() throws IOException {
        // Given
        String input = "";
        Supplier<InputStream> inputSupplier = () -> new ByteArrayInputStream(input.getBytes());

        // When
        rcloneSync.processOutput(inputSupplier, result);

        // Then
        assertTrue(capturedProgress.isEmpty(), "No progress should be captured for empty input");
        // Verify no exceptions were thrown and the test completes
    }

    @Test
    void testProcessOutput_WithPartialProgress() throws IOException {
        // Given - Only progress start without matching checks line
        String progressLine = "Transferred:    	         0 / 0 B, -, 0 B/s, ETA -\n";
        String nonMatchingLine = "Some other line\n";
        String input = progressLine + nonMatchingLine;
        Supplier<InputStream> inputSupplier = () -> new ByteArrayInputStream(input.getBytes());

        // When
        rcloneSync.processOutput(inputSupplier, result);

        // Then - Verify no progress was captured
        assertTrue(capturedProgress.isEmpty(), "No progress should be captured for partial progress");
    }
}

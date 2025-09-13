package com.fathzer.rclonesync;

/**
 * Represents an ongoing rclone synchronization operation.
 * This class provides methods to monitor and control the synchronization process.
 */
public class Synchronization {
    private final Process process;
    private final SynchronizationResult result;
    private volatile boolean cancelled;

    Synchronization(Process process, SynchronizationResult result) {
        this.process = process;
        this.result = result;
    }

    /**
     * Gets the underlying Process instance for this synchronization.
     *
     * @return the Process instance
     */
    Process process() {
        return this.process;
    }

    /**
     * Gets the current result of the synchronization.
     * The result is updated as the synchronization progresses.
     *
     * @return the current synchronization result
     */
    public SynchronizationResult result() {
        return this.result;
    }

    /**
     * Waits for the synchronization to complete.
     * This method blocks until the rclone process terminates.
     *
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public void waitFor() throws InterruptedException {
        this.process.waitFor();
        this.result.setExitCode(this.process.exitValue());
    }

    /**
     * Attempts to cancel the ongoing synchronization.
     * This sends a termination signal to the rclone process.
     * Note that there might be a delay before the process actually terminates.
     */
    public void cancel() {
        this.cancelled = true;
        this.process.destroy();
    }

    /**
     * Checks if the synchronization has been cancelled.
     *
     * @return true if {@link #cancel()} has been called, false otherwise
     */
    public boolean isCancelled() {
        return this.cancelled;
    }
}

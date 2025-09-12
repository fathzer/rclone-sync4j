package com.fathzer.rclonesync;

public class Synchronization {
    private final Process process;
    private final SynchronizationResult result;
    private volatile boolean cancelled;

    Synchronization(Process process, SynchronizationResult result) {
        this.process = process;
        this.result = result;
    }

    Process process() {
        return this.process;
    }

    public SynchronizationResult result() {
        return this.result;
    }

    public void waitFor() throws InterruptedException {
        this.process.waitFor();
        this.result.setExitCode(this.process.exitValue());
    }

    public void cancel() {
        this.cancelled = true;
        this.process.destroy();
    }

    public boolean isCancelled() {
        return this.cancelled;
    }
}

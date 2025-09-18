package com.fathzer.rclonesync.cmd;

import com.fathzer.rclonesync.Synchronization;
import com.fathzer.rclonesync.SynchronizationResult;

final class Sync extends Synchronization {
    private final Process process;

    Sync(Process process, SynchronizationResult result) {
        super(result);
        this.process = process;
    }

    @Override
    public void waitFor() throws InterruptedException {
        this.process.waitFor();
        this.result().setExitCode(this.process.exitValue());
    }

    @Override
    public void cancel() {
        super.cancel();
        this.process.destroy();
    }

    /**
     * Gets the underlying Process instance for this synchronization.
     *
     * @return the Process instance
     */
    Process process() {
        return this.process;
    }
}

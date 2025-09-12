package com.fathzer.rclonesync;

public class SynchronizationResult {
    private int exitCode;
    private int deleted;
    private int copied;
    private int replaced;

    public int exitCode() {
        return exitCode;
    }
    
    public void setExitCode(int exitCode) {
    	this.exitCode = exitCode;
    }

    public int deleted() {
        return deleted;
    }

    public void incrementDeleted() {
        this.deleted++;
    }

    public int copied() {
        return copied;
    }

    public void incrementCopied() {
        this.copied++;
    }

    public int replaced() {
        return replaced;
    }

    public void incrementReplaced() {
        this.replaced++;
    }

    public String toString() {
        return "SyncResult{" +
            "exitCode=" + exitCode +
            ", deleted=" + deleted +
            ", copied=" + copied +
            ", replaced=" + replaced +
            '}';
    }
}
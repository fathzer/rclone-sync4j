package com.fathzer.rclonesync;

/**
 * Represents the result of a synchronization operation.
 * This class tracks various statistics about the files processed during the sync.
 */
public class SynchronizationResult {
    private int exitCode;
    private int deleted;
    private int copied;
    private int replaced;

    SynchronizationResult() {
    }

    /**
     * Gets the exit code of the rclone process.
     *
     * @return the exit code (0 indicates success, non-zero indicates an error or a cancellation)
     */
    public int exitCode() {
        return exitCode;
    }
    
    /**
     * Sets the exit code of the rclone process.
     *
     * @param exitCode the exit code to set
     */
    void setExitCode(int exitCode) {
    	this.exitCode = exitCode;
    }

    /**
     * Gets the number of files that were deleted during the sync.
     *
     * @return the number of deleted files
     */
    public int deleted() {
        return deleted;
    }

    /**
     * Increments the count of deleted files by one.
     */
    void incrementDeleted() {
        this.deleted++;
    }

    /**
     * Gets the number of files that were newly copied during the sync.
     *
     * @return the number of newly copied files
     */
    public int copied() {
        return copied;
    }

    /**
     * Increments the count of newly copied files by one.
     */
    void incrementCopied() {
        this.copied++;
    }

    /**
     * Gets the number of files that were replaced during the sync.
     *
     * @return the number of replaced files
     */
    public int replaced() {
        return replaced;
    }

    /**
     * Increments the count of replaced files by one.
     */
    void incrementReplaced() {
        this.replaced++;
    }

    /**
     * Returns a string representation of the synchronization result.
     *
     * @return a string containing the exit code and file operation counts
     */
    @Override
    public String toString() {
        return "SyncResult{" +
            "exitCode=" + exitCode +
            ", deleted=" + deleted +
            ", copied=" + copied +
            ", replaced=" + replaced +
            '}';
    }
}
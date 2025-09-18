package com.fathzer.rclonesync;

import java.io.IOException;

/**
 * Interface for running rclone sync with progress tracking.
 * This interface allows you to configure and execute rclone sync while receiving progress
 * updates through event callbacks.
 */
public interface RcloneSync {
    /**
     * Starts the rclone sync operation asynchronously.
     * A new daemon thread is created to read the process output.
     *
     * @return a {@link Synchronization} object that can be used to monitor and control the operation
     * @throws IOException if an I/O error occurs when starting the process
     * @throws SecurityException if a security manager exists and its checkExec method doesn't allow some operation
     */
    public Synchronization run() throws IOException;
}

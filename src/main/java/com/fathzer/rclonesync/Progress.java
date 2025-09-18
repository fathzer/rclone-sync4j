package com.fathzer.rclonesync;

/**
 * Represents the progress of a file synchronization operation.
 * This record captures various metrics about the progress of a file transfer or check operation.
 *
 * @param processedBytes The number of bytes that have been processed so far
 * @param totalBytes The total number of bytes to process (may be 0 if unknown)
 * @param bytesThroughput The current transfer throughput (e.g., "1.2 MB/s")
 * @param eta Estimated time remaining for the operation to complete
 * @param processedChecks Number of files that have been checked
 * @param totalChecks Total number of files to check
 */
public record Progress(
    long processedBytes,
    long totalBytes,
    String bytesThroughput,
    String eta,
    int processedChecks,
    int totalChecks
) {
}
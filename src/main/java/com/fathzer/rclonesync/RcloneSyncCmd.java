package com.fathzer.rclonesync;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A class for running rclone sync commands with progress tracking.
 * This class allows you to configure and execute rclone sync commands while receiving progress
 * updates through event callbacks.
 * <b>Warning:</b> For an unknown reason, the rclone sync progress output does not work when run from docker container.
 */
public class RcloneSyncCmd implements RcloneSync {
    private static final String TRANSFERRED_PREFIX = "Transferred:";
    private static final String CHECKS_PREFIX = "Checks:";

    private final SynchronizationParameters parameters;

    /**
     * Creates a new RcloneSyncCmd instance with the specified parameters.
     *
     * @param parameters The parameters for the sync operation
     * @throws NullPointerException if parameters is null
     */
    public RcloneSyncCmd(SynchronizationParameters parameters) {
        if (parameters == null) {
            throw new NullPointerException("parameters must not be null");
        }
        this.parameters = parameters;
    }

    /**
     * Starts the rclone sync operation asynchronously.
     * A new daemon thread is created to read the process output.
     *
     * @return a {@link Synchronization} object that can be used to monitor and control the operation
     * @throws IOException if an I/O error occurs when starting the process
     * @throws SecurityException if a security manager exists and its checkExec method doesn't allow creation of the subprocess
     */
    public Synchronization run() throws IOException {
        final List<String> cmd = buildCommand();
        final Process process = buildProcess(cmd);
        final Synchronization synchronization = new Synchronization(process, new SynchronizationResult());

        final Thread thread = new Thread(() -> readProcessOutput(synchronization));
        thread.setDaemon(true);
        thread.start();

        return synchronization;
    }

    /**
     * Creates a new process with the specified command.
     *
     * @param cmd the command to run and its arguments
     * @return a new Process instance
     * @throws IOException if an I/O error occurs
     */
    Process buildProcess(List<String> cmd) throws IOException {
        return new ProcessBuilder(cmd).redirectErrorStream(true).start();
    }

    /**
     * Builds the command line arguments for the rclone sync operation.
     * This method can be overridden by subclasses to inspect and modify the command line.
     *
     * @return a list of command line arguments
     */
    protected List<String> buildCommand() {
        final List<String> cmd = new LinkedList<>(List.of(
            "rclone", "sync", parameters.getSource(), parameters.getDestination(),
            "--fast-list",
            "--stats", "1s",
            "--log-level", "INFO"
        ));
        if (parameters.isChecksum()) {
            cmd.add("--checksum");
        }
        if (parameters.getExcludesFile() != null) {
            cmd.add("--exclude-from");
            cmd.add(parameters.getExcludesFile());
        }
        parameters.getExcludes().forEach(exclude -> {
            cmd.add("--exclude");
            cmd.add(exclude);
        });
        if (parameters.getConfigFile() != null) {
            cmd.add("--config");
            cmd.add(parameters.getConfigFile());
        }
        return cmd;
    }

    private void readProcessOutput(Synchronization synchronization) {
        final Process process = synchronization.process();
        final SynchronizationResult result = synchronization.result();
        try {
            processOutput(process::getInputStream, result);
        } catch (IOException e) {
            if (!synchronization.isCancelled()) {
                parameters.getExceptionConsumer().accept(e);
            }
        }
    }

    void processOutput(Supplier<InputStream> inputStreamSupplier, SynchronizationResult result) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStreamSupplier.get()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (isProgressStart(line)) {
                    final String checks = reader.readLine();
                    if (checks.startsWith(CHECKS_PREFIX)) {
                        final String transfered = line.substring(TRANSFERRED_PREFIX.length()).trim();
                        final String checksLine = checks.substring(CHECKS_PREFIX.length()).trim();
                        final Optional<Progress> oProgress = Progress.parse(transfered, checksLine);
                        if (oProgress.isPresent()) {
                            final Progress progress = oProgress.get();
                            parameters.getEventConsumer().accept(progress);
                            continue;
                        }
                    }
                    onNonProgressLine(line, result);
                    onNonProgressLine(checks, result);
                } else {
                    onNonProgressLine(line, result);
                }
            }
        }
    }

    /**
     * Processes a line of output from rclone that doesn't contain progress information.
     * This method can be overridden by subclasses to handle additional output lines.
     *
     * @param line the output line to process
     * @param result the synchronization result to update
     * @return true if the line was processed, false otherwise
     */
    boolean onNonProgressLine(String line, SynchronizationResult result) {
        if (line.endsWith(": Deleted")) {
            result.incrementDeleted();
        } else if (line.endsWith(": Copied (new)")) {
            result.incrementCopied();
        } else if (line.endsWith(": Copied (replaced existing)")) {
            result.incrementReplaced();
        } else if (!line.endsWith(": checking") && !line.endsWith(": transferring") && !line.endsWith(": There was nothing to transfer")) {
            return true;
        }
        return false;
    }

    private boolean isProgressStart(String line) {
        return line.startsWith(TRANSFERRED_PREFIX);
    }
}

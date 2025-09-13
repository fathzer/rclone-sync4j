package com.fathzer.rclonesync;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.LogManager;

/**
 * A class that provides a fluent API for running rclone sync operations with progress tracking.
 * This class allows you to configure and execute rclone sync commands while receiving progress
 * updates through event callbacks.
 */
public class RcloneSync {
    private static final String TRANSFERRED_PREFIX = "Transferred:";
    private static final String CHECKS_PREFIX = "Checks:";

    private final String source;
    private final String destination;
    private boolean checksum = false;
    private String excludesFile = null;
    private String configFile = null;
    private Consumer<Progress> eventConsumer = event -> {};
    private Consumer<IOException> exceptionConsumer = exception ->
        LogManager.getLogManager().getLogger(this.getClass().getName()).log(Level.SEVERE, "Error reading process output", exception);

    /**
     * Creates a new RcloneSync instance with the specified source and destination paths.
     *
     * @param source The source path for the sync operation
     * @param destination The destination path for the sync operation
     * @throws NullPointerException if either source or destination is null
     */
    public RcloneSync(String source, String destination) {
        if (source == null || destination == null) {
            throw new NullPointerException("source and destination must not be null");
        }
        this.source = source;
        this.destination = destination;
    }

    /**
     * Enables or disables checksum verification during the sync operation.
     * When enabled, rclone will compare file checksums instead of just file sizes and modification times.
     *
     * @param checksum true to enable checksum verification (default: false)
     * @return this instance for method chaining
     */
    public RcloneSync withCheckSum(boolean checksum) {
        this.checksum = checksum;
        return this;
    }

    /**
     * Sets a file containing exclusion patterns for the sync operation.
     * Each line in the file specifies a pattern to exclude from the sync.
     *
     * @param excludesFile path to the file containing exclusion patterns (default: null)
     * @return this instance for method chaining
     */
    public RcloneSync withExcludesFile(String excludesFile) {
        this.excludesFile = excludesFile;
        return this;
    }

    /**
     * Sets a custom rclone configuration file to use for the sync operation.
     *
     * @param configFile path to the rclone configuration file (null by default which means the file is the rclone's default one: ~/.config/rclone/rclone.conf)
     * @return this instance for method chaining
     */
    public RcloneSync withConfigFile(String configFile) {
        this.configFile = configFile;
        return this;
    }

    /**
     * Sets a consumer to receive progress updates during the sync operation.
     * The consumer will be called periodically with progress information.
     *
     * @param eventConsumer the consumer to receive progress updates (default: empty consumer)
     * @return this instance for method chaining
     * @throws NullPointerException if eventConsumer is null
     */
    public RcloneSync withEventConsumer(Consumer<Progress> eventConsumer) {
        if (eventConsumer == null) {
            throw new NullPointerException("eventConsumer must not be null");
        }
        this.eventConsumer = eventConsumer;
        return this;
    }

    /**
     * Sets a consumer to handle IOExceptions that occur during the sync operation.
     * If not set, exceptions will be logged using java.util.logging.
     *
     * @param exceptionConsumer the consumer to handle IOExceptions
     * @return this instance for method chaining
     * @throws NullPointerException if exceptionConsumer is null
     */
    public RcloneSync withExceptionConsumer(Consumer<IOException> exceptionConsumer) {
        if (exceptionConsumer == null) {
            throw new NullPointerException("exceptionConsumer must not be null");
        }
        this.exceptionConsumer = exceptionConsumer;
        return this;
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
            "rclone", "sync", source, destination,
            "--fast-list",
            "--stats", "1s",
            "--log-level", "INFO"
        ));
        if (checksum) {
            cmd.add("--checksum");
        }
        if (excludesFile != null) {
            cmd.add("--exclude-from");
            cmd.add(excludesFile);
        }
        if (configFile != null) {
            cmd.add("--config");
            cmd.add(configFile);
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
                exceptionConsumer.accept(e);
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
                            eventConsumer.accept(progress);
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

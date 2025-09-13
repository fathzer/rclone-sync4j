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

    public RcloneSync(String source, String destination) {
        this.source = source;
        this.destination = destination;
    }

    public RcloneSync withCheckSum(boolean checksum) {
        this.checksum = checksum;
        return this;
    }

    public RcloneSync withExcludesFile(String excludesFile) {
        this.excludesFile = excludesFile;
        return this;
    }

    public RcloneSync withConfigFile(String configFile) {
        this.configFile = configFile;
        return this;
    }

    public RcloneSync withEventConsumer(Consumer<Progress> eventConsumer) {
        this.eventConsumer = eventConsumer;
        return this;
    }

    public RcloneSync withExceptionConsumer(Consumer<IOException> exceptionConsumer) {
        this.exceptionConsumer = exceptionConsumer;
        return this;
    }

    public Synchronization run() throws IOException {
        final List<String> cmd = buildCommand();
        final Process process = buildProcess(cmd);
        final Synchronization synchronization = new Synchronization(process, new SynchronizationResult());

        final Thread thread = new Thread(() -> readProcessOutput(synchronization));
        thread.setDaemon(true);
        thread.start();

        return synchronization;
    }

    Process buildProcess(List<String> cmd) throws IOException {
        return new ProcessBuilder(cmd).redirectErrorStream(true).start();
    }

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
            System.err.println("Error reading process output: " + e.getMessage());
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

    protected boolean onNonProgressLine(String line, SynchronizationResult result) {
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

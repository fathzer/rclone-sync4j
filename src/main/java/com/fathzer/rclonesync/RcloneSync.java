package com.fathzer.rclonesync;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RcloneSync {
    private static final Logger log = LoggerFactory.getLogger(RcloneSync.class);
    private static final String TRANSFERRED_PREFIX = "Transferred:";
    private static final String CHECKS_PREFIX = "Checks:";

    private final String source;
    private final String destination;
    private boolean checksum = false;
    private String excludesFile = null;
    private String configFile = null;
    private Consumer<Progress> eventConsumer = event -> {};

    public RcloneSync(String source, String destination) {
        this.source = source;
        this.destination = destination;
    }

    public void setCheckSum(boolean checksum) {
        this.checksum = checksum;
    }

    public void setExcludesFile(String excludesFile) {
        this.excludesFile = excludesFile;
    }

    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }

    public void setEventConsumer(Consumer<Progress> eventConsumer) {
        this.eventConsumer = eventConsumer;
    }

    public Synchronization run() throws IOException {
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

        if (log.isInfoEnabled()) log.info("Running command: {}", String.join(" ", cmd));
        final Process process = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        final Synchronization synchronization = new Synchronization(process, new SynchronizationResult());

        final Thread thread = new Thread(() -> readProcessOutput(synchronization));
        thread.setDaemon(true);
        thread.start();

        return synchronization;
    }

    private void readProcessOutput(Synchronization synchronization) {
        final Process process = synchronization.process();
        final SynchronizationResult result = synchronization.result();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while (process.isAlive() && (line = reader.readLine()) != null) {
                if (isProgressStart(line)) {
                    final String checks = reader.readLine();
                    if (checks.startsWith(CHECKS_PREFIX)) {
                        final String transfered = line.substring(TRANSFERRED_PREFIX.length()).trim();
                        final String checksLine = checks.substring(CHECKS_PREFIX.length()).trim();
                        final Optional<Progress> oProgress = Progress.parse(transfered, checksLine);
                        if (oProgress.isPresent()) {
                            final Progress progress = oProgress.get();
                            log.debug("Progress: {}", progress);
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
        } catch (IOException e) {
            if (!synchronization.isCancelled()) {
                log.error("Error reading process output", e);
            }
        }
    }

    private void onNonProgressLine(String line, SynchronizationResult result) {
        boolean unknown = false;
        if (line.endsWith(": Deleted")) {
            result.incrementDeleted();
        } else if (line.endsWith(": Copied (new)")) {
            result.incrementCopied();
        } else if (line.endsWith(": Copied (replaced existing)")) {
            result.incrementReplaced();
        } else if (!line.endsWith(": checking") && !line.endsWith(": transferring") && !line.endsWith(": There was nothing to transfer")) {
            unknown = true;
        }
        if (unknown) {
            log.debug("Unknown non-progress line: {}", line);
        } else {
            log.debug("Non-progress line: {}", line);
        }
    }

    private boolean isProgressStart(String line) {
        return line.startsWith(TRANSFERRED_PREFIX);
    }
}

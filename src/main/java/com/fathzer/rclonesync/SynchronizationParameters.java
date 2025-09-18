package com.fathzer.rclonesync;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.LogManager;

/**
 * Class with a fluent API for configuring rclone sync parameters.
 * This class allows you to configure and execute rclone sync while receiving progress
 * updates through event callbacks.
 */
public class SynchronizationParameters {
    private final String source;
    private final String destination;
    private boolean checksum = false;
    private String excludesFile = null;
    private List<String> excludes = List.of();
    private String configFile = null;
    private Consumer<Progress> eventConsumer = event -> {};
    private Consumer<IOException> exceptionConsumer = exception ->
        LogManager.getLogManager().getLogger(this.getClass().getName()).log(Level.SEVERE, "Error reading process output", exception);

    /**
     * Creates a new SynchronizationParameters instance with the specified source and destination paths.
     *
     * @param source The source path for the sync operation
     * @param destination The destination path for the sync operation
     * @throws NullPointerException if either source or destination is null
     */
    public SynchronizationParameters(String source, String destination) {
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
    public SynchronizationParameters withCheckSum(boolean checksum) {
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
    public SynchronizationParameters withExcludesFile(String excludesFile) {
        this.excludesFile = excludesFile;
        return this;
    }

    /**
     * Sets exclusion patterns for the sync operation.
     * Each pattern specifies a file or directory to exclude from the sync.
     *
     * @param excludes array of exclusion patterns (default: empty array)
     * @return this instance for method chaining
     */
    public SynchronizationParameters withExcludes(String... excludes) {
        if (excludes == null) {
            throw new NullPointerException("excludes must not be null");
        }
        this.excludes = List.of(excludes);
        return this;
    }

    /**
     * Sets a custom rclone configuration file to use for the sync operation.
     *
     * @param configFile path to the rclone configuration file (null by default which means the file is the rclone's default one: ~/.config/rclone/rclone.conf)
     * @return this instance for method chaining
     */
    public SynchronizationParameters withConfigFile(String configFile) {
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
    public SynchronizationParameters withEventConsumer(Consumer<Progress> eventConsumer) {
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
    public SynchronizationParameters withExceptionConsumer(Consumer<IOException> exceptionConsumer) {
        if (exceptionConsumer == null) {
            throw new NullPointerException("exceptionConsumer must not be null");
        }
        this.exceptionConsumer = exceptionConsumer;
        return this;
    }

    /**
     * Returns the source path for the sync operation.
     *
     * @return the source path
     */
    public String getSource() {
        return source;
    }

    /**
     * Returns the destination path for the sync operation.
     *
     * @return the destination path
     */
    public String getDestination() {
        return destination;
    }

    /**
     * Returns whether checksum verification is enabled during the sync operation.
     *
     * @return true if checksum verification is enabled, false otherwise
     */
    public boolean isChecksum() {
        return checksum;
    }

    /**
     * Returns the path to the file containing exclusion patterns for the sync operation.
     *
     * @return the path to the excludes file, or null if no excludes file is set
     */
    public String getExcludesFile() {
        return excludesFile;
    }

    /**
     * Returns the list of exclusion patterns for the sync operation.
     *
     * @return the list of exclusion patterns
     */
    public List<String> getExcludes() {
        return excludes;
    }

    /**
     * Returns the path to the rclone configuration file for the sync operation.
     *
     * @return the path to the rclone configuration file, or null if no custom configuration file is set
     */
    public String getConfigFile() {
        return configFile;
    }

    /**
     * Returns the consumer to receive progress updates during the sync operation.
     *
     * @return the consumer to receive progress updates
     */
    public Consumer<Progress> getEventConsumer() {
        return eventConsumer;
    }

    /**
     * Returns the consumer to handle IOExceptions that occur during the sync operation.
     *
     * @return the consumer to handle IOExceptions
     */
    public Consumer<IOException> getExceptionConsumer() {
        return exceptionConsumer;
    }
}

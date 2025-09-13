package com.fathzer.rclonesync;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final Pattern pattern = Pattern.compile(
        "^([\\d]*\\.?[\\d]+)\\s*([KMGT])?i?B$"
    );
    
    /**
     * Parses rclone's progress output strings into a Progress object.
     *
     * @param transfered The transfer progress string from rclone (e.g., "1.2 GB / 2.5 GB, 42%, 1.2 MB/s, ETA 12:34")
     * @param checks The checks progress string from rclone (e.g., "42/100, 42%, 1.2 files/s")
     * @return An Optional containing the parsed Progress object, or empty if parsing fails
     */
    static Optional<Progress> parse(String transfered, String checks) {
        try {
            final String[] parts = transfered.split(",");
            if (parts.length != 4) return Optional.empty();
            final String[] bytes = parts[0].trim().split("/");
            if (bytes.length != 2) return Optional.empty();
            final long processedBytes = decode(bytes[0]);
            final long totalBytes = decode(bytes[1]);

            final String throughput = parts[2].trim();
            if (throughput.isEmpty()) return Optional.empty();

            String eta = parts[3].trim();
            if (!eta.startsWith("ETA ")) return Optional.empty();
            eta = eta.substring(4);
            if (eta.isEmpty()) return Optional.empty();

            final String[] checksParts = checks.split(",");
            if (checksParts.length != 3) return Optional.empty();
            final String[] checksBytes = checksParts[0].trim().split("/");
            if (checksBytes.length != 2) return Optional.empty();
            final int processedChecks = Integer.parseInt(checksBytes[0].trim());
            final int totalChecks = Integer.parseInt(checksBytes[1].trim());
            return Optional.of(new Progress(processedBytes, totalBytes, throughput, eta, processedChecks, totalChecks));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }


    /**
     * Converts a human-readable byte string (e.g., "1.5 MB") to its byte count.
     * Supports both binary (KiB, MiB, etc.) and decimal (KB, MB, etc.) units.
     *
     * @param bytesAsString The string representation of bytes (e.g., "1.5 MB", "1.2 GiB")
     * @return The number of bytes as a long
     * @throws NullPointerException if the input string is null
     * @throws NumberFormatException if the input string is empty or has an invalid format
     */
    static long decode(String bytesAsString) {
        if (bytesAsString == null) {
            throw new NullPointerException("Input string cannot be null");
        }
        
        // Remove leading/trailing whitespace and normalize internal whitespace
        String input = bytesAsString.trim().replaceAll("\\s+", " ");
        if (input.isEmpty()) {
            throw new NumberFormatException("Input string cannot be empty");
        }
        
        Matcher matcher = pattern.matcher(input);
        
        if (!matcher.matches()) {
            throw new NumberFormatException("Invalid format: " + bytesAsString);
        }
        
        double number = Double.parseDouble(matcher.group(1));
        String unitAndModifier = matcher.group(2);
        
        // Determine if it's binary (1024) or decimal (1000)
        boolean isBinary = input.contains("i");
        long base = isBinary ? 1024L : 1000L;
            
        // Calculate the multiplier based on unit
        long multiplier = 1L;
        if (unitAndModifier != null && !unitAndModifier.isEmpty()) {
            char unit = unitAndModifier.charAt(0);
            switch (unit) {
                case 'K':
                    multiplier = base;
                break;
            case 'M':
                multiplier = base * base;
                break;
            case 'G':
                multiplier = base * base * base;
                break;
            case 'T':
                multiplier = base * base * base * base;
                break;
            default:
                throw new NumberFormatException("Unsupported unit: " + unit);
            }
        }
        
        double result = number * multiplier;
        
        if (result > Long.MAX_VALUE) {
            throw new NumberFormatException("Result exceeds Long.MAX_VALUE");
        }
        return Math.round(result);
    }
}
package com.fathzer.rclonesync;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RcloneSyncTest {
    private static final String SOURCE = "/source/dir";
    private static final String DESTINATION = "/destination/dir";
    private static final String EXCLUDES_FILE = "/path/to/excludes";
    private static final String CONFIG_FILE = "/path/to/config";

    private SynchronizationParameters parameters;
    private RcloneSyncCmd rcloneSync;

    @BeforeEach
    void setUp() {
        parameters = new SynchronizationParameters(SOURCE, DESTINATION);
        rcloneSync = new RcloneSyncCmd(parameters);
    }

    @Test
    void testBuildCommand_Default() {
        List<String> command = rcloneSync.buildCommand();
        
        assertCommandContains(command, 
            "rclone", "sync", SOURCE, DESTINATION,
            "--fast-list", "--stats", "1s", "--log-level", "INFO"
        );
    }

    @Test
    void testBuildCommand_WithChecksum() {
        parameters.withCheckSum(true);
        List<String> command = rcloneSync.buildCommand();
        
        assertCommandContains(command, "--checksum");
    }

    @Test
    void testBuildCommand_WithExcludesFile() {
        parameters.withExcludesFile(EXCLUDES_FILE);
        List<String> command = rcloneSync.buildCommand();
        
        assertCommandContains(command, "--exclude-from", EXCLUDES_FILE);
    }

    @Test
    void testBuildCommand_WithExcludes() {
        parameters.withExcludes("*.{xml,txt}");
        List<String> command = rcloneSync.buildCommand();
        assertCommandContains(command, "--exclude", "*.{xml,txt}");
    }

    @Test
    void testBuildCommand_WithConfigFile() {
        parameters.withConfigFile(CONFIG_FILE);
        List<String> command = rcloneSync.buildCommand();
        
        assertCommandContains(command, "--config", CONFIG_FILE);
    }

    @Test
    void testBuildCommand_WithLotOfOptions() {
        parameters.withCheckSum(true);
        parameters.withExcludesFile(EXCLUDES_FILE);
        parameters.withConfigFile(CONFIG_FILE);
        
        List<String> command = rcloneSync.buildCommand();
        
        assertCommandContains(command, 
            "rclone", "sync", SOURCE, DESTINATION,
            "--fast-list", "--stats", "1s", "--log-level", "INFO",
            "--checksum",
            "--exclude-from", EXCLUDES_FILE,
            "--config", CONFIG_FILE
        );
    }

    private void assertCommandContains(List<String> command, String... expectedParts) {
        int index = 0;
        for (String expected : expectedParts) {
            assertTrue(command.contains(expected), 
                String.format("Command should contain '%s' at position %d. Command: %s", 
                    expected, command.indexOf(expected), command));
            
            // Verify the order of expected parts
            if (index > 0) {
                int prevIndex = command.indexOf(expectedParts[index - 1]);
                int currIndex = command.indexOf(expected);
                assertTrue(currIndex > prevIndex,
                    String.format("'%s' should appear after '%s' in command. Command: %s",
                        expected, expectedParts[index - 1], command));
            }
            index++;
        }
    }

    @Test
    void testConstructor_WithNullSourceOrDestination() {
        // Test null source
        assertThrows(NullPointerException.class, () -> new RcloneSyncCmd(null));
   }
}

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

    private RcloneSync rcloneSync;

    @BeforeEach
    void setUp() {
        rcloneSync = new RcloneSync(SOURCE, DESTINATION);
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
        rcloneSync.withCheckSum(true);
        List<String> command = rcloneSync.buildCommand();
        
        assertCommandContains(command, "--checksum");
    }

    @Test
    void testBuildCommand_WithExcludesFile() {
        rcloneSync.withExcludesFile(EXCLUDES_FILE);
        List<String> command = rcloneSync.buildCommand();
        
        assertCommandContains(command, "--exclude-from", EXCLUDES_FILE);
    }

    @Test
    void testBuildCommand_WithExcludes() {
        assertThrows(NullPointerException.class, () -> rcloneSync.withExcludes((String[])null));
        rcloneSync.withExcludes("*.{xml,txt}");
        List<String> command = rcloneSync.buildCommand();
        assertCommandContains(command, "--exclude", "*.{xml,txt}");
    }

    @Test
    void testBuildCommand_WithConfigFile() {
        rcloneSync.withConfigFile(CONFIG_FILE);
        List<String> command = rcloneSync.buildCommand();
        
        assertCommandContains(command, "--config", CONFIG_FILE);
    }

    @Test
    void testBuildCommand_WithAllOptions() {
        rcloneSync.withCheckSum(true);
        rcloneSync.withExcludesFile(EXCLUDES_FILE);
        rcloneSync.withConfigFile(CONFIG_FILE);
        
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
        assertThrows(NullPointerException.class, () -> new RcloneSync(null, DESTINATION));
        
        // Test null destination
        assertThrows(NullPointerException.class, () -> new RcloneSync(SOURCE, null));
        
        // Test both null
        assertThrows(NullPointerException.class, () -> new RcloneSync(null, null));
    }

    @Test
    void testWithEventConsumer_Null() {
        assertThrows(NullPointerException.class, () -> rcloneSync.withEventConsumer(null));
    }

    @Test
    void testWithExceptionConsumer_Null() {
        assertThrows(NullPointerException.class, () -> rcloneSync.withExceptionConsumer(null));
    }

    @Test
    void testWithExcludesFile_Null() {
        // Should not throw for null
        assertSame(rcloneSync, rcloneSync.withExcludesFile(null));
    }

    @Test
    void testWithConfigFile_Null() {
        // Should not throw for null
        assertSame(rcloneSync, rcloneSync.withConfigFile(null));
    }
}

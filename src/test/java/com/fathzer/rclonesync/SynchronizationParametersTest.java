package com.fathzer.rclonesync;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SynchronizationParametersTest {
    private static final String SOURCE = "/source/dir";
    private static final String DESTINATION = "/destination/dir";
    private static final String EXCLUDES_FILE = "/path/to/excludes";
    private static final String CONFIG_FILE = "/path/to/config";

    private SynchronizationParameters parameters;

    @BeforeEach
    void setUp() {
        parameters = new SynchronizationParameters(SOURCE, DESTINATION);
    }

    @Test
    void testConstructor() {
        assertEquals(SOURCE, parameters.getSource());
        assertEquals(DESTINATION, parameters.getDestination());

        // Test null source
        assertThrows(NullPointerException.class, () -> new SynchronizationParameters(null, DESTINATION));
        
        // Test null destination
        assertThrows(NullPointerException.class, () -> new SynchronizationParameters(SOURCE, null));
        
        // Test both null
        assertThrows(NullPointerException.class, () -> new SynchronizationParameters(null, null));
    }

    @Test
    void testWithChecksum() {
        assertFalse(parameters.isChecksum());
        assertSame(parameters, parameters.withCheckSum(true));
        assertTrue(parameters.isChecksum());
        assertDoesNotThrow(() -> parameters.withCheckSum(false));
    }

    @Test
    void testWithExcludes() {
        assertTrue(parameters.getExcludes().isEmpty());
        assertThrows(NullPointerException.class, () -> parameters.withExcludes((String[])null));
        assertSame(parameters, parameters.withExcludes("*.{xml,txt}","toto/*"));
        assertEquals(Set.of("*.{xml,txt}","toto/*"), Set.copyOf(parameters.getExcludes()));
    }

    @Test
    void testWithExcludesFile() {
        assertNull(parameters.getExcludesFile());
        assertSame(parameters, parameters.withExcludesFile(EXCLUDES_FILE));
        assertEquals(EXCLUDES_FILE, parameters.getExcludesFile());
        assertDoesNotThrow(() -> parameters.withExcludesFile(null));
        assertNull(parameters.getExcludesFile());
    }

    @Test
    void testWithConfigFile() {
        assertNull(parameters.getConfigFile());
        assertSame(parameters, parameters.withConfigFile(CONFIG_FILE));
        assertEquals(CONFIG_FILE, parameters.getConfigFile());
        assertDoesNotThrow(() -> parameters.withConfigFile(null));
        assertNull(parameters.getConfigFile());
    }

    @Test
    void testWithEventConsumer() {
        assertNotNull(parameters.getEventConsumer());
        assertThrows(NullPointerException.class, () -> parameters.withEventConsumer(null));
        Consumer<Progress> eventConsumer = event -> {};
        assertSame(parameters, parameters.withEventConsumer(eventConsumer));
        assertSame(eventConsumer, parameters.getEventConsumer());
    }

    @Test
    void testWithExceptionConsumer() {
        assertNotNull(parameters.getEventConsumer());
        assertThrows(NullPointerException.class, () -> parameters.withExceptionConsumer(null));
        Consumer<IOException> exceptionConsumer = exception -> {};
        assertSame(parameters, parameters.withExceptionConsumer(exceptionConsumer));
        assertSame(exceptionConsumer, parameters.getExceptionConsumer());
    }
}

package com.fathzer.rclonesync;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class RcloneSyncTest {

    @ParameterizedTest(name = "valid inputs {index} : {arguments}")
    @CsvSource({
        "1B, 1",
        "1 B, 1",
        "1024B, 1024",
        "1KiB, 1024",
        "1 MiB, 1048576",
        "25 MiB, 26214400",
        "1GiB, 1073741824",
        "1 TiB, 1099511627776",
        "1KB, 1000",
        "1.5KiB, 1536"
    })
    void testDecode_ValidInputs(String input, long expected) {
        assertEquals(expected, Progress.decode(input));
    }

    @ParameterizedTest(name = "invalid inputs {index} : {arguments}")
    @ValueSource(strings = {
        "", " ", "B", "X", "1", "123", "1X", "1.5", "1.5X", "1.2.3KiB", "1 K"
    })
    void testDecode_InvalidInputs(String input) {
        assertThrows(NumberFormatException.class, () -> Progress.decode(input));
    }

    @Test
    void testDecode_NullInput() {
        assertThrows(NullPointerException.class, () -> Progress.decode(null));
    }

    @Test
    void basicTest() {
        assertEquals(1, Progress.decode("1B"));
    }
}
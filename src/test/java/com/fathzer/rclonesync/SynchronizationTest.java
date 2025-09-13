package com.fathzer.rclonesync;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class SynchronizationTest {
    @Mock
    private Process process;
    
    private SynchronizationResult result;
    private Synchronization synchronization;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        result = new SynchronizationResult();
        synchronization = new Synchronization(process, result);
    }

    @Test
    void testCancel() {
        // When
        synchronization.cancel();
        
        // Then
        assertTrue(synchronization.isCancelled());
        verify(process).destroy();
    }

    @Test
    void testIsCancelled_InitiallyFalse() {
        assertFalse(synchronization.isCancelled());
    }

    @Test
    void testWaitFor() throws Exception {
        // Given
        when(process.waitFor()).thenReturn(0);
        when(process.exitValue()).thenReturn(1);
        
        // When
        synchronization.waitFor();
        
        // Then
        verify(process).waitFor();
        verify(process).exitValue();
        assertEquals(1, result.exitCode());
    }

    @Test
    void testWaitFor_Interrupted() throws Exception {
        // Given
        when(process.waitFor()).thenThrow(new InterruptedException("Test interrupt"));
        
        // When/Then
        assertThrows(InterruptedException.class, () -> synchronization.waitFor());
        verify(process).waitFor();
        // exitValue() should not be called if waitFor throws
        verify(process, never()).exitValue();
    }

    @Test
    void testResult() {
        assertSame(result, synchronization.result());
    }

    @Test
    void testProcess() {
        assertSame(process, synchronization.process());
    }
}

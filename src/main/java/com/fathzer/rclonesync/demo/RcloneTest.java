package com.fathzer.rclonesync.demo;

import com.fathzer.rclonesync.RcloneSyncCmd;
import com.fathzer.rclonesync.SynchronizationParameters;

@SuppressWarnings("squid:S106")
public class RcloneTest {
    public static void main(String[] args) throws Exception {
        String source = "pCloud:PhotosJM/1998";
        String destination = System.getProperty("user.home") + "/Bureau/test/1998";

        final var sync = new SynchronizationParameters(source, destination)
        .withCheckSum(true)  // Enable checksum verification
        .withEventConsumer(progress ->
            // Handle progress updates
            System.out.println("Progress: " + progress)
        )
        .withExceptionConsumer(Exception::printStackTrace);
    
        final var syncOp = new RcloneSyncCmd(sync).run();
        syncOp.waitFor();  // Wait for sync to complete
        
        final var result = syncOp.result();
        System.out.println("Sync completed: " + result);        
    }
}

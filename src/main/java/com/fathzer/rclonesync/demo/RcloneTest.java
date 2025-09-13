package com.fathzer.rclonesync.demo;

import com.fathzer.rclonesync.RcloneSync;

@SuppressWarnings("squid:S106")
public class RcloneTest {
    public static void main(String[] args) throws Exception {
        String source = "mypcloud:Mes photos/Images publiques/1998";
        String destination = System.getProperty("user.home") + "/Bureau/test/1998";

        var sync = new RcloneSync(source, destination)
        .withCheckSum(true)  // Enable checksum verification
        .withEventConsumer(progress ->
            // Handle progress updates
            System.out.printf("Progress: %s / %s%n", 
                progress.processedChecks(), progress.totalChecks())
        )
        .withExceptionConsumer(Exception::printStackTrace);
    
        var syncOp = sync.run();
        syncOp.waitFor();  // Wait for sync to complete
        
        var result = syncOp.result();
        System.out.println("Sync completed: " + result);        
    }
}

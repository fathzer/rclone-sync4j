# rclone-sync4j

[![Maven Central](https://img.shields.io/maven-central/v/com.fathzer/rclone-sync4j)](https://search.maven.org/artifact/com.fathzer/rclone-sync4j)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Javadocs](https://www.javadoc.io/badge/com.fathzer/rclone-sync4j.svg)](https://www.javadoc.io/doc/com.fathzer/rclone-sync4j)
[![SonarCloud](https://sonarcloud.io/api/project_badges/measure?project=com.fathzer%3Arclone-sync4j&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=com.fathzer%3Arclone-sync4j)

A Java facade for [rclone](https://rclone.org/) sync operations with progress tracking support. This library provides a fluent API to execute rclone sync commands and monitor their progress in real-time.

## Features

- Fluent API for configuring and running rclone sync operations
- Real-time progress tracking
- Support for checksum verification
- File exclusion patterns support

## Requirements

- Java 17 or higher
- rclone installed and available in system PATH

## Installation

### Maven

```xml
<dependency>
    <groupId>com.fathzer</groupId>
    <artifactId>rclone-sync4j</artifactId>
    <version>0.0.1</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.fathzer:rclone-sync4j:0.0.1'
```

## Usage

### Basic Example

```java
RcloneSync sync = new RcloneSync("local/path", "remote:path")
    .withCheckSum(true)  // Enable checksum verification
    .withExcludesFile("exclude-patterns.txt")  // Optional: exclude files
    .withEventConsumer(progress -> 
        // Handle progress updates
        System.out.printf("Progress: %s / %s%n", 
                progress.processedChecks(), progress.totalChecks());
    )
    .withExceptionConsumer(Exception::printStackTrace);

Synchronization syncOp = sync.run();
syncOp.waitFor();  // Wait for sync to complete

SynchronizationResult result = syncOp.result();
System.out.println("Sync completed: " + result);
```

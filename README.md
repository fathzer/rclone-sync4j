# rclone-sync4j

[![Maven Central](https://img.shields.io/maven-central/v/com.fathzer/rclone-sync4j)](https://search.maven.org/artifact/com.fathzer/rclone-sync4j)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Javadocs](https://www.javadoc.io/badge/com.fathzer/rclone-sync4j.svg)](https://www.javadoc.io/doc/com.fathzer/rclone-sync4j)
[![SonarCloud](https://sonarcloud.io/api/project_badges/measure?project=fathzer_clone-sync4j&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=fathzer_rclone-sync4j)

A Java facade for [rclone](https://rclone.org/) sync operations with progress tracking support. This library provides a fluent API to execute rclone sync commands and monitor their progress in real-time.

## Features

- Fluent API for configuring and running rclone sync operations
- Real-time progress tracking
- Support for checksum verification
- File exclusion patterns support

## Requirements

- Java 21 or higher
- rclone installed and available in system PATH

## Installation

### Maven

```xml
<dependency>
    <groupId>com.fathzer</groupId>
    <artifactId>rclone-sync4j</artifactId>
    <version>0.0.3</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.fathzer:rclone-sync4j:0.0.3'
```

## Usage

### Basic Example

```java
SynchronizationParameters parameters = new SynchronizationParameters("local/path", "remote:path")
    .withCheckSum(true)  // Enable checksum verification
    .withExcludesFile("exclude-patterns.txt")  // Optional: exclude files
    .withEventConsumer(progress -> 
        // Handle progress updates
        System.out.println("Progress: " + progress)
    )
    .withExceptionConsumer(Exception::printStackTrace);

SynchronizationOperation syncOp = new RcloneSyncCmd(parameters).run();
syncOp.waitFor();  // Wait for sync to complete

SynchronizationResult result = syncOp.result();
System.out.println("Sync completed: " + result);
```


## Rclone Configuration

### When rclone is already configured

If you already have rclone configured on your machine, the library will use your existing configuration file, typically located at:
- `~/.config/rclone/rclone.conf` on Linux/macOS
- `%APPDATA%\rclone\rclone.conf` on Windows

### When rclone is not configured

If rclone is not configured on the machine, you have two options:

1. **Create a configuration file manually**:
   ```bash
   # Create config directory
   mkdir -p ~/.config/rclone/
   
   # Create a minimal rclone config
   cat > ~/.config/rclone/rclone.conf << 'EOL'
   [remote]
   type = your_remote_type  # e.g., s3, google drive, dropbox, etc.
   # Add your remote configuration here
   EOL
   ```

2. **Use a custom config file location** with `withConfigFile`:
   ```java
   SynchronizationParameters parameters = new SynchronizationParameters("source", "remote:path")
       .withConfigFile("/path/to/your/rclone.conf");
   ```

### Remote Configuration Examples

#### S3 Example
```ini
[my-s3]
type = s3
provider = AWS
access_key_id = your_access_key
secret_access_key = your_secret_key
region = us-east-1
```

#### Google Drive Example
```ini
[my-gdrive]
type = drive
client_id = your_client_id
client_secret = your_client_secret
token = your_token
```

> **Security Note**: Never commit sensitive credentials to version control. Use environment variables or a secrets manager for production environments.

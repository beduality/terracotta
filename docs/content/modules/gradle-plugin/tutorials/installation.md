# Installing the Terracotta Gradle Plugin

This page describes how to add the Terracotta Gradle plugin to your Minecraft project.

## System Requirements

- **JDK**: 17+
- **Gradle**: 8.0+ (recommended)

## Installation

Add the Terracotta plugin to your `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.beduality.terracotta") version "0.4.0"
}
```

### Snapshots (optional)

If you want to use snapshot versions, add the snapshot repository to your `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
}
```

Then use a snapshot version in your `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.beduality.terracotta") version "0.1.0-SNAPSHOT"
}
```

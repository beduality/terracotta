# Installing the Terracotta Gradle Plugin

This page describes how to add the Terracotta Gradle plugin to your Minecraft project.

## System Requirements

- **JDK**: 17+
- **Gradle**: 8.0+ (recommended)

## Installation

Add the Terracotta plugin to your `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.beduality.terracotta") version "0.8.0"
}
```

### Filesystem state backend

The Gradle plugin uses a pluggable state backend. To persist run state — which powers stable gallery image identities across runs — add the `terracotta-state-filesystem` module to your buildscript classpath:

```kotlin
buildscript {
    dependencies {
        classpath("io.github.beduality:terracotta-state-filesystem:0.8.0")
    }
}
```

If the backend is missing, the build fails fast with an error listing the available factory IDs and the dependency coordinates needed to restore the default backend. See the [State Filesystem module docs](../../../terracotta-state-filesystem/README.md) for advanced configuration.

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

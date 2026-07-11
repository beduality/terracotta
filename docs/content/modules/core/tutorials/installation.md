# Installing Terracotta as a Library

This tutorial explains how to add the Terracotta core library and provider modules to a Kotlin or Java project.

## Core library

=== "Gradle (Kotlin)"

    ```kotlin
    dependencies {
        implementation("io.github.beduality:terracotta-core:0.2.0")
    }
    ```

=== "Maven"

    ```xml
    <dependency>
        <groupId>io.github.beduality</groupId>
        <artifactId>terracotta-core</artifactId>
        <version>0.2.0</version>
    </dependency>
    ```

## Provider modules

Add the provider for each registry you want to support:

### Modrinth

=== "Gradle (Kotlin)"

    ```kotlin
    implementation("io.github.beduality:terracotta-provider-modrinth:0.2.0")
    ```

=== "Maven"

    ```xml
    <dependency>
        <groupId>io.github.beduality</groupId>
        <artifactId>terracotta-provider-modrinth</artifactId>
        <version>0.2.0</version>
    </dependency>
    ```

### Hangar

=== "Gradle (Kotlin)"

    ```kotlin
    implementation("io.github.beduality:terracotta-provider-hangar:0.2.0")
    ```

=== "Maven"

    ```xml
    <dependency>
        <groupId>io.github.beduality</groupId>
        <artifactId>terracotta-provider-hangar</artifactId>
        <version>0.2.0</version>
    </dependency>
    ```

## What's next?

- [Implement a Custom Provider](implementing-a-custom-provider.md)
- [Modrinth Provider Tutorial](../../provider-modrinth/tutorials/using-modrinth.md)
- [Hangar Provider Tutorial](../../provider-hangar/tutorials/using-hangar.md)

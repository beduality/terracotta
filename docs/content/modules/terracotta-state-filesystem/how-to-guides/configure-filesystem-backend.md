# Configure the Filesystem Backend

The `terracotta-state-filesystem` backend stores run state in a YAML file on the local filesystem. Add the module to your classpath and select the `"filesystem"` backend through your frontend.

## Install the backend

=== "Gradle Plugin"

    Add the module to the buildscript classpath so the Gradle plugin can discover it:

    ```kotlin
    buildscript {
        dependencies {
            classpath("io.github.beduality:terracotta-state-filesystem:<version>")
        }
    }
    ```

=== "Gradle (library)"

    ```kotlin
    dependencies {
        implementation("io.github.beduality:terracotta-state-filesystem:<version>")
    }
    ```

=== "Maven"

    ```xml
    <dependency>
        <groupId>io.github.beduality</groupId>
        <artifactId>terracotta-state-filesystem</artifactId>
        <version>VERSION</version>
    </dependency>
    ```

Once the module is on the classpath, the `StateSourceFactory` is discovered automatically through `ServiceLoader` under the id `"filesystem"`.

## Change the state file path

The backend writes state to `.terracotta-state.yml` in the project directory by default. Pass the `path` setting through your frontend.

=== "Gradle Plugin"

    ```kotlin
    terracotta {
        stateSource.set("filesystem")
        stateSourceSettings.put("path", "state/terracotta.yml")
    }
    ```

=== "Library"

    ```kotlin
    import io.github.beduality.terracotta.core.state.StateSourceConfig

    val config = StateSourceConfig(
        projectDir = projectDir,
        settings = mapOf("path" to "state/terracotta.yml"),
    )
    ```

Relative paths are resolved against the project directory.

## If the backend is missing

If `"filesystem"` is selected but `terracotta-state-filesystem` is not on the classpath, the frontend fails fast with a clear error that lists available factories and includes the dependency coordinates needed to restore the backend.

## Replace with a custom backend

To use a different backend, implement the state SPI and put the JAR on the classpath. Then select the custom factory id through your frontend. See the [State Management explanation](../../core/explanation/state-management.md) for the SPI contract.

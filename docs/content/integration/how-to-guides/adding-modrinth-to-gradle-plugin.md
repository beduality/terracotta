# Adding Modrinth to the Gradle Plugin

This guide explains how to publish releases to Modrinth using the Terracotta Gradle plugin.

## Before you start

You need:

- A Modrinth account.
- A Modrinth API token from [Modrinth settings](https://modrinth.com/settings/tokens).
- The Terracotta Gradle plugin installed.

See the [Gradle plugin installation guide](../../modules/gradle-plugin/tutorials/installation.md) if you have not installed the plugin yet.

## 1. Add the Modrinth provider to `terracotta.yml`

```yaml
providers:
  modrinth:
    projectId: "my-plugin" # your Modrinth project slug or ID
```

## 2. Configure authentication

Set the token as an environment variable. Terracotta uses the `<PROVIDER>_TOKEN` convention, so the variable name for Modrinth is `MODRINTH_TOKEN`:

```=== "Linux / macOS"

    ```bash
    export MODRINTH_TOKEN="your_modrinth_token"
    ```

=== "Windows PowerShell"

    ```powershell
    $env:MODRINTH_TOKEN="your_modrinth_token"
    ```
```

Or set the token explicitly in `build.gradle.kts`:

```kotlin
providers {
    create("modrinth") {
        projectId.set("my-modrinth-project-id")
        token.set(System.getenv("MODRINTH_TOKEN"))
    }
}
```

## 3. Plan and apply

Preview the changes:

```bash
./gradlew terracottaPlanModrinth
```

Apply them:

```bash
./gradlew terracottaApplyModrinth
```

You can also run the aggregate `terracottaPlan` and `terracottaApply` tasks to operate on all configured providers at once.

## What's next?

- [Adding Hangar to the Gradle plugin](adding-hangar-to-gradle-plugin.md)
- [CI/CD setup with GitHub Actions](ci-cd-setup.md)
- [Modrinth provider module docs](../../modules/provider-modrinth/README.md)

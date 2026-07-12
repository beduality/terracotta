# Publishing to Multiple Providers

This tutorial walks you through publishing a Minecraft plugin to both Modrinth and Hangar in a single Terracotta run.

## What you'll learn

- How to configure multiple providers in `terracotta.yml`.
- How to authenticate with Modrinth and Hangar tokens.
- How to plan and apply changes across both providers.

## Prerequisites

- The Terracotta Gradle plugin installed. See [Installing the Gradle plugin](../../modules/gradle-plugin/tutorials/installation.md).
- A Modrinth account and API token.
- A Hangar account and API key.
- A Minecraft plugin project with a JAR artifact.

## 1. Configure providers

Create or edit `terracotta.yml` in your project root:

```yaml
providers:
  modrinth:
    projectId: "my-plugin"
  hangar:
    projectId: "my-plugin"
```

Use the same slug if it is available on both platforms, or use different values if your project slugs differ.

## 2. Set tokens

Export both tokens in your shell:

```bash
export MODRINTH_TOKEN="your_modrinth_token"
export HANGAR_TOKEN="your_hangar_token"
```

On Windows PowerShell:

```powershell
$env:MODRINTH_TOKEN="your_modrinth_token"
$env:HANGAR_TOKEN="your_hangar_token"
```

Terracotta reads tokens from `<PROVIDER>_TOKEN` environment variables by default.

## 3. Plan the release

Run the aggregate plan task to see what Terracotta will do on both providers:

```bash
./gradlew terracottaPlan
```

The output shows operations for each provider separately. Review the project metadata, version, supported loaders, and game versions.

## 4. Apply the release

When the plan looks correct, apply it:

```bash
./gradlew terracottaApply
```

Terracotta creates or updates the project on each provider and uploads the version. If one provider fails, the other provider's operations are not rolled back automatically — check the output and fix the failing provider before re-running.

## What's next?

- [Add Modrinth to the Gradle plugin](../how-to-guides/adding-modrinth-to-gradle-plugin.md)
- [Add Hangar to the Gradle plugin](../how-to-guides/adding-hangar-to-gradle-plugin.md)
- [Provider configuration](../reference/provider-configuration.md)
- [Troubleshoot provider integration](../how-to-guides/troubleshooting.md)

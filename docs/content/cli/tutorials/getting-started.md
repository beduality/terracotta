# Getting Started

This tutorial walks you through publishing your first plugin to Modrinth using Terracotta.

By the end of this tutorial, you will have:

- Installed the Terracotta CLI
- Configured a Terracotta project
- Connected Terracotta to your Modrinth account
- Uploaded your first release

## Prerequisites

Before starting, you need:

1. A [Modrinth](https://modrinth.com/) account.
2. A Modrinth API token.
3. A compiled plugin artifact (for example `build/libs/my-plugin-1.0.0.jar`).

## 1. Install Terracotta

Download the latest Terracotta binary from the [GitHub Releases](https://github.com/beduality/terracotta/releases) page.

=== "Linux"

    Download `terracotta-linux-amd64`:

    ```bash
    chmod +x terracotta-linux-amd64
    sudo mv terracotta-linux-amd64 /usr/local/bin/terracotta
    ```

=== "macOS"

    Download `terracotta-macos-universal`:

    ```bash
    chmod +x terracotta-macos-universal
    sudo mv terracotta-macos-universal /usr/local/bin/terracotta
    ```

=== "Windows"

    Download `terracotta-windows-amd64.exe`.

    1. Rename it to `terracotta.exe`.
    2. Move it to a directory such as `C:\Tools\Terracotta`.
    3. Add that directory to your system PATH.
    4. Restart your terminal.

Verify the installation:

```bash
terracotta --version
````

You should see the installed Terracotta version.

## 2. Create your Terracotta configuration

Navigate to your plugin project directory.

Create a file named `terracotta.yaml`:

```yaml
project:
  id: my-plugin
  name: My Plugin
  summary: Lightweight Paper plugin

description: README.md

versions:
  - version: 1.0.0
    artifact: build/libs/my-plugin-1.0.0.jar
    gameVersions:
      - 1.21.8
      - 1.21.7

tags:
  - paper
  - utility

license: MIT
```

Make sure your project contains a `README.md` file with your plugin description.

## 3. Configure Modrinth authentication

Create a Modrinth API token from your Modrinth account settings.

Set the token as an environment variable:

=== "Linux / macOS"

````
```bash
export MODRINTH_TOKEN="your_modrinth_api_token"
```
````

=== "Windows PowerShell"

````
```powershell
$env:MODRINTH_TOKEN="your_modrinth_api_token"
```
````

## 4. Preview the changes

Before uploading anything, use `plan` to see what Terracotta will change:

```bash
terracotta plan
```

Terracotta will compare your local configuration with the Modrinth project and show the operations it would perform.

## 5. Publish your plugin

Apply the changes:

```bash
terracotta apply
```

Terracotta will update your Modrinth project metadata, upload your release, and publish your compiled artifact.

## Finished

Your plugin is now published on Modrinth through Terracotta.

Next steps:

* Learn about all available configuration options
* Automate releases in CI/CD
* Manage multiple versions and platforms

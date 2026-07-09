# Getting Started

This tutorial guides you through configuring and deploying a project to Modrinth using Terracotta.

## Prerequisites

1. An account on [Modrinth](https://modrinth.com/).
2. A Modrinth API token. You can generate one in your Account Settings.
3. Your compiled plugin artifact (e.g. `build/libs/my-plugin-1.0.0.jar`).
4. Terracotta CLI binary installed.

## 1. Create your configuration

Create a file named `terracotta.yaml` in the root of your plugin project:

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

Make sure that `README.md` exists and contains your project description.

## 2. Dry Run with `plan`

Define your Modrinth authentication token as an environment variable:

```bash
export MODRINTH_TOKEN="your_modrinth_api_token"
```

Run `plan` to check what operations Terracotta will execute:

```bash
terracotta plan
```

Terracotta will compare your `terracotta.yaml` configuration with the remote project on Modrinth and output the changes.

## 3. Apply the changes

When you are ready to upload, execute `apply`:

```bash
terracotta apply
```

Terracotta will now update the project metadata, set your tags, update the description from `README.md`, and upload your compiled JAR as version `1.0.0`.

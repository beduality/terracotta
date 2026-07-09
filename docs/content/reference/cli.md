# CLI Command Reference

This reference describes the options and commands for the Terracotta CLI.

## Global Options

The following options apply to the root `terracotta` command and all subcommands:

- `-f, --file=<path>`: Specifies the path to the configuration file (default: `terracotta.yaml`).
- `--modrinth-token=<token>`: Modrinth API authentication token. If omitted, the CLI checks the `MODRINTH_TOKEN` environment variable.
- `-h, --help`: Display help message and exit.
- `-V, --version`: Display version information and exit.

---

## Subcommands

### `plan`

Generate a semantic diff comparing your local `terracotta.yaml` config against the remote registry's project state. No changes are applied.

**Usage:**
```bash
terracotta plan [options]
```

**Output Indicators:**
- `+`: Indicates a resource (e.g. version) that will be uploaded or created.
- `-`: Indicates a resource or metadata element (e.g. tags) that will be deleted or removed.
- `~`: Indicates metadata fields, tags, or descriptions that will be updated on the remote registry.

---

### `apply`

Applies the planned operations, updating registry metadata and uploading missing versions.

**Usage:**
```bash
terracotta apply [options]
```

**Requirements:**
- A valid `MODRINTH_TOKEN` environment variable or `--modrinth-token` option.

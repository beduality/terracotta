# Configuration Reference

Terracotta configurations are written in YAML (usually named `terracotta.yaml`).

## Schema Specification

```yaml
# Section defining project information
project:
  id: "my-plugin"           # Required. The project slug or ID in the registry
  name: "My Plugin"         # Required. Display title of the project
  summary: "Lightweight..." # Required. Short description / excerpt

description: "README.md"    # Required. Path to markdown file or raw text body

license: "MIT"              # Required. Project license identifier (e.g. MIT, Apache-2.0)

tags:                       # Optional. List of tags / categories
  - "paper"
  - "utility"

versions:                   # Optional. List of versions to verify and deploy
  - version: "1.2.0"        # Required. Unique version string identifier
    artifact: "libs/jar"    # Required. Path to the compiled binary file
    gameVersions:           # Required. List of supported Minecraft versions
      - "1.21.8"
      - "1.21.7"
    loaders:                # Optional/Required by registry. List of target platforms/mod loaders
      - "paper"
      - "spigot"
```

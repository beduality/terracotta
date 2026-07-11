# External YAML Configuration — Remaining Work

The core `terracotta.yml` loader, Gradle plugin integration, and metadata resolution are already implemented (see `modules/terracotta-core/src/main/kotlin/io/github/beduality/terracotta/core/config`).

What remains to fully realize the original proposal:

1. **Standalone CLI entry point**
   - `terracotta plan --config <file>`
   - `terracotta apply --config <file>`

2. **Library API surface**
   - `TerracottaProject.from(config)`
   - Provider factories driven by `TerracottaConfig`

3. **Inline environment variable interpolation**
   - Resolve `${VAR}` placeholders inside YAML values at load time

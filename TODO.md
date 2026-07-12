# TODO

- Add support to load any Terracotta config value from file just using `<field>Path` convention, e.g. `descriptionPath`
- Stabilize gallery item identity via persisted state
- Add [Narrow License](./proposals/2025-07-narrow-license.md)
- Add [Narrow Tags](./proposals/2025-07-narrow-tags.md)

- Add [Import Task](./proposals/2026-07-import-task.md)
- Add [Override Pattern](./proposals/2025-07-override-pattern.md)

- Add Keywords (useful for hangar & future GitHub provider)

- Finish [External YAML Configuration](./proposals/2025-07-external-yaml-config.md)
- Add support for multiple Terracotta files
    - Import mode
    - `platform/` convention

- Add "create" task

- Add [Terracotta Cloud](./proposals/2026-07-terracotta-cloud.md)
    - Add [Cloud Preview Service](./proposals/2025-07-cloud-preview-service.md) (paid Cloud plan, web UI + CLI)
    - Add [Terracotta Registry Gateway](./proposals/2026-07-terracotta-registry-gateway.md)
- [gha] Add [Terracotta GHA Module](./proposals/2026-07-terracotta-gha-module.md)

- Try migrate ClockTime to this project

---

- Add [Config Validation](./proposals/2025-07-config-validation.md)

- Add [Authentication Workflows](./proposals/2025-07-authentication-workflows.md)

- Add [Staging Environments](./proposals/2025-07-staging-environments.md) like Modrinth Staging

- Add [YAML Schema for IDEs](./proposals/2026-07-yaml-schema-for-ides.md)

- Add [CurseForge Provider](./proposals/2025-07-curseforge-provider.md)
---
- Add [Provider Toposort](./proposals/2025-07-provider-toposort.md)
- Add [Git Provider](./proposals/2025-07-git-provider.md) (host-agnostic Git CLI for tags and file commits)
- Add [GitHub Provider](./proposals/2025-07-github-provider.md) (GitHub control-plane metadata: description, topics, homepage, license, lifecycle)
- Add [GitHub Release Provider](./proposals/2025-07-github-release-provider.md) (GitHub Releases distribution, depends on Git Provider for tags)
- Add to GitHub modules docs to clarify: `terracotta-github` (Pulumi infra), `terracotta-provider-github` (control provider), `terracotta-provider-github-release` (release provider), and `terracotta-provider-git` (host-agnostic Git CLI)

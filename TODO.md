# TODO

- Add support to load any Terracotta config value from file just using `<field>Path` convention, e.g. `descriptionPath`
- Stabilize gallery item identity via persisted state
- Add [Pluggable State Backends](./proposals/26-07-12-add-state-pluggable-backends.md) (extract filesystem backend, add StateSourceFactory SPI)
- Add [Narrow License](./proposals/26-07-12-narrow-license-hangar.md)
- Add [Narrow Tags](./proposals/25-07-10-narrow-tags-canonical.md)

- Add [Override Pattern](./proposals/25-07-10-add-config-override-pattern.md)
- Add [Project Keywords](./proposals/26-07-12-add-project-keywords.md)
- Add [Import Task](./proposals/26-07-12-add-task-import.md)

- Finish [External YAML Configuration](./proposals/26-07-10-add-config-external-yaml.md)
- Add support for multiple Terracotta files
    - Import mode
    - `platform/` convention

- Add "create" task

- Try migrate ClockTime to this project
- Ask for friend feedback on the project

---

- Add [Terracotta Cloud](./proposals/26-07-11-add-cloud-terracotta.md)  in a separate private repo
    - Add website with auth, token management, billing (Stripe), etc
- [gha] Add [Terracotta GHA Module](./proposals/26-07-12-add-module-gha.md)

- Add standalone CLI

- Add Provider feature support table to integration docs

- Add [Cloud Preview Service](./proposals/25-07-12-add-cloud-preview-service.md) (paid Cloud plan, web UI + CLI)
- Add [Terracotta Registry Gateway](./proposals/26-07-11-add-cloud-registry-gateway.md)

---

- Add [Config Validation](./proposals/25-07-12-add-config-validation.md)

- Add [Authentication Workflows](./proposals/25-07-10-add-auth-workflows.md)

- Add [Staging Environments](./proposals/25-07-12-add-cloud-staging-environments.md) like Modrinth Staging

- Add [YAML Schema for IDEs](./proposals/26-07-11-add-ide-yaml-schema.md)

- Add [CurseForge Provider](./proposals/25-07-10-add-provider-curseforge.md)
---
- Add [Provider Toposort](./proposals/25-07-12-add-provider-toposort.md)
- Add [Git Provider](./proposals/25-07-12-add-provider-git.md) (host-agnostic Git CLI for tags and file commits)
- Add [GitHub Provider](./proposals/25-07-12-add-provider-github.md) (GitHub control-plane metadata: description, topics, homepage, license, lifecycle)
- Add [GitHub Release Provider](./proposals/25-07-12-add-provider-github-release.md) (GitHub Releases distribution, depends on Git Provider for tags)
- Add to GitHub modules docs to clarify: `terracotta-github` (Pulumi infra), `terracotta-provider-github` (control provider), `terracotta-provider-github-release` (release provider), and `terracotta-provider-git` (host-agnostic Git CLI)

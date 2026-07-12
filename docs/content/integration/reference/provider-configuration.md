# Provider Configuration Reference

Terracotta provider configuration lives under `providers:` in `terracotta.yml` or in the Gradle Kotlin DSL.

## Common fields

Every provider accepts these fields:

| Field | Type | Required | Description |
|---|---|---|---|
| `projectId` | string | Yes* | Project slug or ID on the provider. |
| `token` | string | No | API token. Defaults to `<PROVIDER>_TOKEN` environment variable. |

*Can be supplied in the Kotlin DSL instead of `terracotta.yml`.

## Provider IDs

| ID | Registry | Documentation |
|---|---|---|
| `modrinth` | [Modrinth](https://modrinth.com/) | [Modrinth provider module](../../modules/provider-modrinth/README.md) |
| `hangar` | [Hangar](https://hangar.papermc.io/) | [Hangar provider module](../../modules/provider-hangar/README.md) |

## Environment variables

| Variable | Provider | Where to create |
|---|---|---|
| `MODRINTH_TOKEN` | Modrinth | [Modrinth settings](https://modrinth.com/settings/tokens) |
| `HANGAR_TOKEN` | Hangar | Hangar account settings |

Terracotta reads these automatically when `token` is not set explicitly.

## Example `terracotta.yml`

```yaml
providers:
  modrinth:
    projectId: "my-plugin"
  hangar:
    projectId: "my-plugin"
```

## Example Kotlin DSL

```kotlin
terracotta {
    providers {
        create("modrinth") {
            projectId.set("my-plugin")
            token.set(System.getenv("MODRINTH_TOKEN"))
        }
        create("hangar") {
            projectId.set("my-plugin")
            token.set(System.getenv("HANGAR_TOKEN"))
        }
    }
}
```

For a full field reference, see the [Config Schema](../../modules/core/reference/config-schema.md).

# Loaders

`TerracottaLoader` implementations detect which mod/plugin platform a project targets by inspecting files in the project directory.

## Built-in loaders

| ID | Display Name | Detection File | Parent |
|----|--------------|----------------|--------|
| `bukkit` | Bukkit | `src/main/resources/plugin.yml` | — |
| `spigot` | Spigot | `src/main/resources/plugin.yml` | Bukkit |
| `paper` | Paper | `src/main/resources/paper-plugin.yml` | Spigot |
| `folia` | Folia | `src/main/resources/paper-plugin.yml` | Paper |
| `purpur` | Purpur | `src/main/resources/paper-plugin.yml` | Paper |
| `fabric` | Fabric | `src/main/resources/fabric.mod.json` | — |
| `quilt` | Quilt | `src/main/resources/quilt.mod.json` | Fabric |
| `forge` | Forge | `src/main/resources/META-INF/mods.toml` | — |
| `neoforge` | NeoForge | `src/main/resources/META-INF/neoforge.mods.toml` | Forge |
| `sponge` | Sponge | `src/main/resources/META-INF/plugins.json` or `mcmod.info` | — |
| `velocity` | Velocity | `src/main/resources/velocity-plugin.json` | — |
| `bungeecord` | BungeeCord | `src/main/resources/bungee.yml` | — |
| `waterfall` | Waterfall | `src/main/resources/bungee.yml` | BungeeCord |

## Detection behavior

- A loader returns `true` from `detect(cache)` when its descriptor file is found.
- If a child loader is detected, its parent chain is included automatically.
- Loader IDs are case-insensitive when looked up via `TerracottaLoaderRegistry.findById` or `fromId`.

## Runtime registration

Additional loaders can be registered at runtime:

```kotlin
TerracottaLoaderRegistry.register(MyLoader())
```

## See also

- [Implement a Custom Loader](../tutorials/implementing-a-custom-loader.md)
- [Add a New Loader](../how-to-guides/add-a-new-loader.md)
- [Loader Hierarchy](../explanation/loader-hierarchy.md)
- [API Documentation](api.md)

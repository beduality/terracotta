# Loader Hierarchy

Terracotta loaders model the real-world relationship between Minecraft platforms. A loader can declare a parent so that detecting a fork also records the platforms it extends.

## Why parent loaders matter

Paper is built on Spigot, and Spigot is built on Bukkit. If a project targets Paper, registries should know it also supports Spigot and Bukkit. Parent loaders capture this automatically.

## How detection expands

When `TerracottaLoaderRegistry.detectAll` finds a loader, it walks the parent chain and includes every ancestor:

```
PaperLoader -> SpigotLoader -> BukkitLoader
```

A project with `paper-plugin.yml` produces the loaders `[paper, spigot, bukkit]`.

## Why equality is identity-based

Loaders are compared by `id`, not by instance. This lets the registry keep a single canonical loader per platform and avoids duplicate entries when the same loader is registered twice.

## When to declare a parent

Declare a parent when your loader targets a platform that is a fork or extension of another supported platform. Do not declare a parent for independent platforms.

## Built-in hierarchy

| Child | Parent |
|-------|--------|
| Spigot | Bukkit |
| Paper | Spigot |
| Folia | Paper |
| Purpur | Paper |
| Quilt | Fabric |
| NeoForge | Forge |
| Waterfall | BungeeCord |

## See also

- [Loaders Reference](../reference/loaders.md)
- [Add a New Loader](../how-to-guides/add-a-new-loader.md)
- [Implement a Custom Loader](../tutorials/implementing-a-custom-loader.md)

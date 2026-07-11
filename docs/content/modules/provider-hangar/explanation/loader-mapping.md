# Hangar Loader Mapping

Hangar uses a coarser platform model than individual mod loaders. The `HangarLoaderMapper` maps Terracotta loader IDs to Hangar platforms and back.

## Loader to platform

| Terracotta loader | Hangar platform |
|---|---|
| `bukkit` | `PAPER` |
| `spigot` | `PAPER` |
| `paper` | `PAPER` |
| `purpur` | `PAPER` |
| `folia` | `PAPER` |
| `velocity` | `VELOCITY` |
| `bungeecord` | `WATERFALL` |
| `waterfall` | `WATERFALL` |

## Platform to loader

| Hangar platform | Terracotta loader |
|---|---|
| `PAPER` | `paper` |
| `VELOCITY` | `velocity` |
| `WATERFALL` | `waterfall` |

## Unsupported loaders

Loaders such as `fabric`, `forge`, `quilt`, `neoforge`, and `sponge` are not supported by Hangar. The provider skips them with a warning and will throw an error if no supported platform remains for a version upload.

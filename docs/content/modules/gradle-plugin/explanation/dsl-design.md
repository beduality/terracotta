# DSL Design

The Terracotta Gradle DSL is modeled as a single nested extension with lazy properties.

## Lazy properties

Every metadata field is exposed as a Gradle `Property` or `ListProperty`. This lets the plugin set conventions from `terracotta.yml` and detected values while still allowing the build script to override them explicitly.

```kotlin
terracotta {
    name.set("My Plugin")          // explicit override
    summary.convention("Default")  // convention from terracotta.yml
}
```

Values are resolved lazily during task execution, not at configuration time, so they can depend on other Gradle tasks or computed inputs.

## Nested providers block

Providers are configured through a `NamedDomainObjectContainer`. Each provider declared in `terracotta.yml` is created automatically if it does not already exist in the DSL, preventing name collisions when the DSL and YAML both reference the same provider.

## Conventions block

The nested `conventions` block selects the README and CHANGELOG conventions that Terracotta uses to interpret project files. Conventions are pluggable in core; the Gradle DSL only selects which conventions to apply.

For the available conventions, see the [Core Conventions reference](../../core/reference/conventions.md).

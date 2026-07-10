# Writing Changelog Entries

This tutorial teaches how to write changelog entries that are clear, useful, and follow project conventions.

## What is a Changelog?

A changelog is a curated, structured record of notable changes made to a project. It helps users, developers, and operators understand what has changed between versions.

## Changelog Format

The changelog follows the [Keep a Changelog](https://keepachangelog.com/) format and is organized by:

1. **Category** - Added, Changed, Fixed, Deprecated, Removed, Security
2. **Module** - Docs, Core, Gradle Plugin, Modrinth, SDK
3. **Audience** - Players, Server Admins, Developers, Maintainers (optional)

Example structure:

```md
## [Unreleased]

### Added
#### Core
- New feature for X

### Changed
#### Gradle Plugin
- Updated Y to improve Z

### Fixed
#### Modrinth
- Bug fix for ABC
```

## Writing Good Changelog Entries

### 1. Focus on Impact, Not Implementation

**Good**:

- "Added support for Minecraft 1.21"
- "Fixed issue with version uploads failing"

**Bad**:

- "Refactored `VersionService` to use `VersionHandler`"
- "Updated dependency from OkHttp 4.9 to 4.12"

### 2. Be Specific and Concrete

**Good**:

- " terracottaPlan now shows `~` for metadata updates"
- "Added `TerracottaEnvironment.CLIENT_ONLY` support"

**Bad**:

- "Improved the plan output"
- "Added new environment option"

### 3. Include "Why" When Helpful

Add a `- **Why**:` line if the motivation isn't obvious:

```md
### Added
#### Core
- `TerracottaVersion.displayName` field
  - **Why**: Support user-defined version names instead of auto-generated ones
```

### 4. Group Related Changes

If multiple changes relate to the same feature, group them:

```md
### Added
#### Gradle Plugin
- `terracottaPlanModrinth` and `terracottaApplyModrinth` tasks
  - **Why**: Allow publishing to Modrinth without affecting other registries
- `terracottaPlanHangar` and `terracottaApplyHangar` tasks
  - **Why**: Allow publishing to Hangar without affecting other registries
```

## Step-by-Step Workflow

### Step 1: Identify the Change

Review your PR and identify what changed from a user's perspective:

- Did you add a new feature?
- Did you fix a bug that users might encounter?
- Did you change behavior or API?

### Step 2: Determine the Category

Choose the most appropriate category:

| Category | When to Use |
|----------|-------------|
| **Added** | New functionality |
| **Changed** | Existing behavior modified |
| **Fixed** | Bug fixes |
| **Deprecated** | Feature marked for removal |
| **Removed** | Feature deleted |
| **Security** | Security-related fixes |

### Step 3: Determine the Module

Choose the module that contains the changed code:

- **Docs** - Documentation changes
- **Core** - `terracotta-core` module
- **Gradle Plugin** - `terracotta-gradle-plugin` module
- **Modrinth** - `terracotta-provider-modrinth` module
- **SDK** - SDK-related changes

### Step 4: Write the Entry

Follow the format:

```md
### Category
#### Module
- Change description
  - **Why**: (optional) Reason for the change
```

### Step 5: Place It in the Changelog

Add the entry under `## [Unreleased]` in `CHANGELOG.md`, maintaining alphabetical order within categories and modules.

## Changelog Guidelines Reference

For more detailed guidelines, see [`guidelines/changelog.md`](../../../guidelines/changelog.md).

## Common Patterns

### Breaking Changes

Mark clearly as breaking:
```md
### Changed
#### Core
- `TerracottaVersion` fields are now required
  - **Breaking**: Versions without required fields will fail validation
  - **Migration**: Add all required fields to your version definitions
```

### API Changes

Document API changes:

```md
### Changed
#### SDK
- `DiffEngine.diff()` now returns `List<Operation>` instead of `OperationGroup`
  - **API**: The return type changed to simplify operation handling
  - **Migration**: Update your code to handle `List<Operation>` directly
```

### Performance Improvements

```md
### Changed
#### Core
- Version comparison is now 3x faster
  - **Why**: Improved diff algorithm using tree-based comparison
```
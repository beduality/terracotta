# Repository Documentation

This section is for people who work **on** Terracotta itself: contributors, maintainers, and release managers. If you use Terracotta in your own project, start with the [Core](../modules/core/README.md), [Gradle Plugin](../modules/gradle-plugin/README.md), or [Integration](../integration/README.md) docs instead.

Repo docs follow the [Diátaxis framework](explanation/documentation-framework.md) strictly:

- **Tutorials** teach by doing.
- **How-to guides** solve a specific task.
- **Reference** lists facts for lookup.
- **Explanation** answers *why* something is the way it is and never includes step-by-step instructions.

## Documentation Structure

This documentation follows the [Documentation Framework](explanation/documentation-framework.md), which organizes content by user intent:

### Tutorials

Learning-oriented guides that help you learn by doing.

- **[Quick Start](tutorials/quick-start.md)**: Get up and running in under 10 minutes
- **[Navigating the Codebase](tutorials/navigating-the-codebase.md)**: Understand project structure and Git workflow

### How-To Guides

Task-oriented guides that help you accomplish specific tasks.

- **[Contributing](how-to-guides/contributing.md)**: Contribution process and requirements
- **[Writing Changelog](how-to-guides/writing-changelog.md)**: Learn how to document changes properly
- **[Building](how-to-guides/building.md)**: Compile and test the project
- **[Testing](how-to-guides/testing.md)**: Run tests and understand coverage
- **[Documentation](how-to-guides/documentation.md)**: Preview and deploy documentation
- **[Releasing](how-to-guides/releasing.md)**: Create a new release

### Reference

Information-oriented reference for looking up details.

- **[Tech Stack](reference/tech-stack.md)**: Tools and versions used
- **[File Structure](reference/file-structure.md)**: Repository directory structure
- **[Configuration](reference/configuration.md)**: All configuration files
- **[Changelog Guidelines](reference/changelog-guidelines.md)**: Rules and examples for changelog entries

### Explanation

Understanding-oriented content that explains concepts and "why" decisions were made.

- **[Architecture](../modules/core/explanation/architecture.md)**: How Terracotta is designed and why
- **[Project Management](explanation/project-management.md)**: Why planning happens through PRs against the `project` branch

## Getting Started

**New contributor?** Start with the [Quick Start](tutorials/quick-start.md) tutorial.

**Want to understand the project?** Read the [Architecture](../modules/core/explanation/architecture.md) explanation.

## Links

- [Main README](https://github.com/beduality/terracotta/blob/main/README.md): Project overview
- [Core Documentation](../modules/core/README.md): For developers using the Terracotta core library
- [Gradle Plugin Documentation](../modules/gradle-plugin/tutorials/installation.md): For users of the Gradle plugin

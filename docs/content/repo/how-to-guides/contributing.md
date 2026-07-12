# Contributing

This guide shows how to contribute to Terracotta.

## Getting Started

1. **Fork the repository** on GitHub
2. **Clone your fork** locally
3. **Set up upstream** for fetching changes:
   ```bash
   git remote add upstream https://github.com/beduality/terracotta.git
   ```

## Making Changes

### 1. Choose a Task

- Check `project/TODO.md` for existing tasks
- If not found, add it to TODO (with PR against `project` branch)
- Open a PR against `project` branch removing the task from TODO
- Wait for merge before starting implementation

### 2. Create a Branch

```bash
git checkout main
git pull upstream main
git checkout -b feature/your-feature-name
```

### 3. Make Your Changes

Implement your changes following the [Code Style Reference](../reference/code-style.md).

### 4. Test Your Changes

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
./gradlew build
```

This runs:
- Compilation of all modules
- All tests (unit, integration, smoke)
- Code formatting verification (Spotless)

### 5. Commit Your Changes

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```bash
git commit -m "feat(core): add new feature"
git commit -m "fix(modrinth): handle edge case"
```

### 6. Open a Pull Request

- Push your branch to your fork
- Open a PR against `main` branch
- Fill in the PR template
- Request review from maintainers

## PR Checklist

Before merging, a PR must:

- [ ] Build passes: `./gradlew build`
- [ ] Tests pass: `./gradlew test`
- [ ] Spotless passes: `./gradlew spotlessCheck`
- [ ] Test coverage maintained or improved
- [ ] Documentation updated (if applicable)

## After Approval

1. **Merge** the PR (or have a maintainer merge it)
2. **Delete** your branch
3. **Update** your local main:
   ```bash
   git checkout main
   git pull upstream main
   ```

## Questions?

- Join the [Discord server](https://discord.gg/D5meCv2Wnd)
- Check the [Architecture Overview](../../modules/core/explanation/architecture.md)
- Review the [Tech Stack Reference](../reference/tech-stack.md)
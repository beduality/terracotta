# Documentation

This guide covers working with Terracotta's documentation site.

## Tech Stack

- **MkDocs**: Documentation generator
- **Material for MkDocs**: Theme
- **MkDocs Material Extensions**: Extra features
- **uv**: Python dependency management
- **mike**: Versioned documentation deployment

## Prerequisites

Install [uv](https://docs.astral.sh/uv/getting-started/installation/) for Python dependency management.

## Local Preview

### Option 1: Full Versioned Preview (Recommended)

Deploy the docs under a local version alias and serve:

```bash
uv run mike deploy -t "Unreleased" unreleased
uv run mike set-default unreleased
uv run mike serve
```

This serves the site at `http://localhost:8000` with:

- Version switching working as in production
- Multi-version support

### Option 2: Quick Single-Version Preview

For a quick preview without committing to the `gh-pages` branch:

```bash
uv run mkdocs serve
```

This starts a live-reloading server at `http://localhost:8000` but without multi-version support.

## Documentation Structure

```
docs/
├── index.md                        # Landing page
├── content/
│   ├── tutorials/                  # Learning-oriented guides
│   ├── how-to-guides/              # Task-oriented guides
│   ├── reference/                  # Information-oriented reference
│   └── explanation/                # Understanding-oriented content
└── hooks/                          # MkDocs build hooks
mkdocs.yml                          # Site configuration & navigation
pyproject.toml                      # Python dependencies (uv)
```

The content follows the [Documentation Framework](../explanation/documentation-framework.md) structure. Each page must belong to exactly one type:

- **Tutorial**: Learning by doing (beginner-focused)
- **How-to guide**: Solving specific tasks
- **Reference**: Exhaustive information lookup
- **Explanation**: Understanding concepts and "why"

## Adding a Page

1. Create a `.md` file under `docs/content/` in the appropriate category
2. Add the page to the `nav` section in `mkdocs.yml`
3. Preview with `uv run mkdocs serve`

## Rebuilding All Versions

If you change templates, overrides, hooks, or `mkdocs.yml` and want to apply them across all historical versions:

```bash
uv run python scripts/redeploy_all_docs.py
```

This script:

- Checks out each release tag
- Applies the latest config and overrides from `main`
- Re-deploys all versions to the local `gh-pages` branch using `mike`

## Deployment

Documentation is automatically deployed to GitHub Pages on every push to `main` via `.github/workflows/deploy-docs.yml`. Normally, no manual deployment is needed.

### Manual Deployment (if needed)

```bash
# Deploy current docs
uv run mike deploy <version-alias>

# Set default version
uv run mike set-default <version-alias>

# List versions
uv run mike list
```

## Writing Documentation

### Follow Documentation Framework

See the [Documentation Framework](../explanation/documentation-framework.md) for detailed guidance on each type.

### Page Checklist

- [ ] Page belongs to exactly one Diátaxis type
- [ ] Content matches the purpose of that type
- [ ] Title reflects the type (command for how-to, concept for explanation)
- [ ] No mixing of types in a single page
- [ ] Code examples are tested and working
- [ ] Links point to correct targets

## Building Documentation for Production

The CI/CD workflow handles production builds. The workflow:

1. Installs Python dependencies
2. Builds documentation with MkDocs
3. Deploys to GitHub Pages using `mike`
4. Updates the `gh-pages` branch

# CI/CD Setup with GitHub Actions

You can automate publishing version updates and syncing project metadata using GitHub Actions.

## 1. Store your Modrinth Token

First, add your Modrinth API token to your GitHub repository secrets:
1. Go to your repository **Settings** > **Secrets and variables** > **Actions**.
2. Click **New repository secret**.
3. Name it `MODRINTH_TOKEN` and paste your API token.

## 2. Create the workflow file

Create a workflow file in `.github/workflows/deploy.yml`:

```yaml
name: Deploy Plugin

on:
  push:
    tags:
      - 'v*'

jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'zulu'
          java-version: '17'
          cache: 'gradle'

      - name: Deploy with Terracotta
        env:
          MODRINTH_TOKEN: ${{ secrets.MODRINTH_TOKEN }}
        run: ./gradlew terracottaApply
```

Whenever you push a tag like `v1.2.0`, this workflow will build your plugin and execute Terracotta to upload your release artifact automatically.

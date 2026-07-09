
# Remove terracotta-cli Plan

## Repository Research Conclusion
We need to remove the `terracotta-cli` module from the project as we're shifting focus to the `terracotta-gradle-plugin` for MVP.

## Files and Modules to Modify
1. `settings.gradle.kts` - Remove terracotta-cli include
2. Remove `modules/terracotta-cli` directory entirely
3. `gradle/libs.versions.toml` - Review and remove unused dependencies (picocli, graalvm)
4. Check other config files (like .github workflows) for references to terracotta-cli

## Steps for Modifications
1. Update `settings.gradle.kts` to exclude terracotta-cli
2. Delete `modules/terracotta-cli` directory
3. Clean up `libs.versions.toml` from unused dependencies
4. Review GitHub workflows for any terracotta-cli references
5. Build project to verify it compiles without errors

## Potential Dependencies or Considerations
- We're only removing terracotta-cli; keep terracotta-core, terracotta-provider-modrinth, and terracotta-github
- Will need to add terracotta-gradle-plugin later (separate task)

## Risk Handling
- Check git status after removal to ensure only intended files are modified
- Run `./gradlew build` to verify project still compiles


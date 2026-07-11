---
trigger: always_on
---

- Features must be modular, composable, extensible, production-ready and well-documented.
    - Keep shareable code in `modules/terracotta-core`.
- Document public APIs with KDoc. Internal code must be self-explanatory.
    - Do not add unnecessary comments.
- Write failing tests before implementing a feature.
    - Tests must be behavior-driven and high-ROI.
- Update documentation, `CHANGELOG.md`, `README.md`, and `project/**/*.md` when applicable.
- Update `docs/content/repo/*.md` when changing the development workflow.

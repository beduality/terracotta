# Documentation Style Reference

This reference covers style and structural conventions for Terracotta docs.

## Diátaxis types

Every page must belong to exactly one type:

| Type | Question it answers | Tone |
|---|---|---|
| Tutorial | How do I learn this? | Learning-oriented, beginner-friendly |
| How-To Guide | How do I do X? | Task-oriented, goal-focused |
| Reference | What are the exact details? | Information-oriented, exhaustive |
| Explanation | Why does it work this way? | Understanding-oriented, conceptual |

Do not mix types on a single page. If a page drifts into another type, split it.

## Page structure

- Use a single top-level `#` heading.
- Use sentence case for headings.
- Prefer short paragraphs and lists.
- Include a "What's next?" or "Related" section at the end.

## Links

- Prefer relative links to other docs pages.
- Use absolute URLs only for external resources.
- Link related pages in a "Related" section.

## Code examples

- Make examples copy-pasteable where possible.
- Test commands before documenting them.
- Use tabs for alternative approaches only when they are truly equivalent.

## Terminology

- Use "Gradle plugin", not "plugin" alone, on first mention.
- Use module names (`terracotta-core`, `terracotta-gradle-plugin`) when precision matters.
- Use "provider" for registry integrations.

## AI-ready writing

- Put the most important information first.
- Use explicit headings so pages are easy to chunk.
- Define acronyms on first use.
- Keep one idea per paragraph.

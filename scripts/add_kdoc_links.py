#!/usr/bin/env python3
"""Add @see GitHub Pages links to public API KDoc comments.

Usage:
    python scripts/add_kdoc_links.py <source-dir> <docs-base-url> <mapping.json>

mapping.json example:
    {
        "io.github.beduality.terracotta.core.config.*": "content/core/how-to-guides/load-terracotta-config.html",
        "io.github.beduality.terracotta.core.diff.*": "content/core/reference/operations.html",
        "io.github.beduality.terracotta.core.model.loader.*": "content/core/reference/loaders.html",
        "io.github.beduality.terracotta.core.model.*": "content/core/reference/models.html",
        "io.github.beduality.terracotta.core.provider.*": "content/core/reference/provider-interfaces.html"
    }

Patterns are matched against the relative package path (e.g. "config/TerracottaConfig.kt").
Longer/more specific patterns win. The script skips files that already reference docs-base-url.
"""

import argparse
import json
import os
from pathlib import Path


def load_mapping(mapping_path: str) -> list[tuple[str, str]]:
    with open(mapping_path) as f:
        data = json.load(f)
    # Sort by length descending so more specific patterns match first.
    return sorted(data.items(), key=lambda kv: len(kv[0]), reverse=True)


def matching_links(rel_path: str, links: list[tuple[str, str]]) -> list[str]:
    result = []
    for pattern, rel_url in links:
        # Allow simple glob-like matching.
        if pattern.endswith("*"):
            prefix = pattern[:-1]
            if rel_path.startswith(prefix):
                result.append(rel_url)
        elif pattern in rel_path:
            result.append(rel_url)
    return result


def process_file(path: Path, base_url: str, links: list[str]) -> bool:
    text = path.read_text()

    if base_url in text:
        return False

    lines = text.splitlines(keepends=True)
    see_lines = [f" * @see [{rel_url.split('/')[-1].replace('.html', '').replace('-', ' ').title()}]({base_url.rstrip('/')}/{rel_url.lstrip('/')})\n" for rel_url in links]

    decl_index = None
    for i, line in enumerate(lines):
        stripped = line.lstrip()
        if stripped.startswith(("class ", "data class ", "interface ", "object ", "enum ", "abstract class ", "sealed ", "fun ")):
            decl_index = i
            break

    if decl_index is None:
        return False

    # Walk back to find KDoc block.
    kdoc_end = None
    i = decl_index - 1
    while i >= 0 and lines[i].strip() in ("", "\n"):
        i -= 1
    if i >= 0 and lines[i].strip() == "*/":
        kdoc_end = i

    if kdoc_end is not None:
        prev = lines[kdoc_end - 1].strip()
        insert = []
        if prev and prev != "*" and not prev.startswith("@see"):
            insert.append(" *\n")
        insert.extend(see_lines)
        lines = lines[:kdoc_end] + insert + lines[kdoc_end:]
    else:
        block = ["/**\n"] + see_lines + [" */\n", "\n"]
        lines = lines[:decl_index] + block + lines[decl_index:]

    path.write_text("".join(lines))
    return True


def main():
    parser = argparse.ArgumentParser(description="Add @see links to KDoc comments.")
    parser.add_argument("source_dir", help="Root Kotlin source directory")
    parser.add_argument("docs_base_url", help="Base URL for the generated docs")
    parser.add_argument("mapping", help="JSON file mapping package patterns to doc paths")
    args = parser.parse_args()

    source = Path(args.source_dir)
    base_url = args.docs_base_url.rstrip("/")
    mapping = load_mapping(args.mapping)

    for path in sorted(source.rglob("*.kt")):
        rel = path.relative_to(source).as_posix()
        links = matching_links(rel, mapping)
        if links and process_file(path, base_url, links):
            print(f"UPDATED: {rel}")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Deployment manifest management for Terracotta docs.

Parses module changelogs to extract structured deployment metadata and maintains
a ``deployments.json`` manifest that feeds the docs "Changes" page.

Schema
------
Each deployment entry has:

- ``version`` (str, optional): Semver string, e.g. ``"0.8.0"``. Omitted for non-versioned deployments (e.g. infrastructure applies).
- ``createdAt`` (str): ISO 8601 datetime, e.g. ``"2026-07-13T00:00:00Z"``.
- ``title`` (str): Short human-readable title.
- ``summary`` (str): One-to-four sentence summary from the changelog.
- ``modules`` (list[str]): Canonical module identifiers touched by this deployment.
- ``isRelease`` (bool): ``True`` for major milestones, ``False`` for routine deployments.
"""

from __future__ import annotations

import json
import re
from pathlib import Path

from rich.console import Console
from semver import Version as SemVerVersion

console = Console()

MANIFEST_PATH = Path("deployments.json")


def parse_changelog_section(content: str, version: str) -> str | None:
    """Extract the body of a ``## [version]`` section from changelog content.

    Returns the section body (everything after the header line) or ``None``
    if the section is not found.
    """
    pattern = rf"##\s+\[{re.escape(version)}\].*?(?=\n##\s+\[|\Z)"
    match = re.search(pattern, content, re.DOTALL)
    if not match:
        return None
    section = match.group(0)
    return section.split("\n", 1)[1].strip() if "\n" in section else ""


def extract_summary(section_body: str) -> str | None:
    """Extract the first paragraph (summary) from a changelog section body.

    The summary is the text before the first ``###`` category heading.
    Returns ``None`` if no summary paragraph is found.
    """
    if not section_body.strip():
        return None
    if re.match(r"\s*###", section_body):
        return None
    parts = re.split(r"\n\s*###\s", section_body, maxsplit=1)
    summary = parts[0].strip()
    if not summary:
        return None
    return summary


def derive_title(summary: str, max_words: int = 5) -> str:
    """Derive a short title from a summary paragraph.

    Strips leading verbs and articles, then takes the first ``max_words``
    significant words. Falls back to the first ``max_words`` words of the
    summary if no known leading verb is found.
    """
    leading_verbs = [
        "adds", "fixes", "stabilizes", "narrows", "introduces", "replaces",
        "removes", "updates", "tightens", "corrects", "republished",
        "improves", "decouples", "simplifies", "generalizes", "moves",
        "renamed", "reorganized", "enforces", "configures", "implemented",
        "bootstraps", "expands", "reworked", "extracts",
    ]
    words = summary.split()
    if words and words[0].lower().rstrip(",.;:") in leading_verbs:
        words = words[1:]
    while words and words[0].lower().rstrip(",.;:") in {"the", "a", "an"}:
        words = words[1:]
    title_words = words[:max_words]
    title = " ".join(title_words).rstrip(",.;:")
    if len(words) > max_words:
        title += "..."
    return title.title() if title else summary[:60]


def generate_deployment_entry(
    version: str,
    created_at: str,
    section_body: str,
    modules: list[str],
    title: str | None = None,
    is_release: bool = False,
) -> dict:
    """Generate a deployment entry dict from changelog section data.

    Parameters
    ----------
    version : str
        The version string (e.g. ``"0.8.0"``).
    created_at : str
        ISO 8601 datetime string (e.g. ``"2026-07-13T00:00:00Z"``).
    section_body : str
        The body of the changelog version section.
    modules : list[str]
        Canonical module identifiers for this deployment.
    title : str, optional
        Explicit title. If omitted, one is derived from the summary.
    is_release : bool
        Whether this deployment is a major milestone.

    Raises
    ------
    ValueError
        If the section body has no summary paragraph.
    """
    summary = extract_summary(section_body)
    if not summary:
        raise ValueError(
            f"Changelog section for version {version} has no summary paragraph."
        )
    entry_title = title if title else derive_title(summary)
    return {
        "version": version,
        "createdAt": created_at,
        "title": entry_title,
        "summary": summary,
        "modules": modules,
        "isRelease": is_release,
    }


def load_manifest(path: Path = MANIFEST_PATH) -> dict:
    """Load the deployments manifest, returning an empty structure if missing."""
    if not path.exists():
        return {"deployments": []}
    data = json.loads(path.read_text(encoding="utf-8"))
    if "deployments" not in data:
        data["deployments"] = []
    return data


def save_manifest(manifest: dict, path: Path = MANIFEST_PATH) -> None:
    """Save the manifest with pretty-printed JSON."""
    path.write_text(
        json.dumps(manifest, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )


def append_deployment(entry: dict, manifest_path: Path = MANIFEST_PATH) -> bool:
    """Append or replace a deployment entry in the manifest.

    If a versioned entry with the same version already exists, it is replaced.
    Versionless entries are always appended (never replaced).
    Entries are kept sorted: versioned entries first (descending), then
    versionless entries (descending by ``createdAt``).

    Returns ``True`` if a new entry was added, ``False`` if an existing
    entry was replaced.
    """
    manifest = load_manifest(manifest_path)
    deployments = manifest["deployments"]
    version = entry.get("version")

    existing_idx = None
    if version is not None:
        for i, d in enumerate(deployments):
            if d.get("version") == version:
                existing_idx = i
                break

    if existing_idx is not None:
        deployments[existing_idx] = entry
        added = False
    else:
        deployments.append(entry)
        added = True

    deployments.sort(key=_semver_sort_key, reverse=True)
    manifest["deployments"] = deployments
    save_manifest(manifest, manifest_path)
    return added


def _semver_sort_key(entry: dict) -> tuple:
    version = entry.get("version")
    if version:
        return (1, SemVerVersion.parse(version))
    return (0, entry.get("createdAt", ""))


if __name__ == "__main__":
    print("Use release.py to manage deployments.")

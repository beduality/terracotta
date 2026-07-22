#!/usr/bin/env python3
"""Deployment manifest management for Terracotta docs.

Parses CHANGELOG.md to extract structured deployment metadata and maintains
a ``deployments.json`` manifest that feeds the docs "Changes" page.

Schema
------
Each deployment entry has:

- ``version`` (str): Semver string, e.g. ``"0.8.0"``.
- ``createdAt`` (str): ISO 8601 datetime, e.g. ``"2026-07-13T00:00:00Z"``.
- ``title`` (str): Short human-readable title.
- ``summary`` (str): One-to-four sentence summary from the changelog.
- ``modules`` (list[str]): Canonical module identifiers touched by this deployment.
- ``isRelease`` (bool): ``True`` for major milestones, ``False`` for routine deployments.
"""

from __future__ import annotations

import json
import re
import sys
from datetime import date, datetime, timezone
from pathlib import Path

from cyclopts import App
from rich.console import Console
from semver import Version as SemVerVersion

console = Console()
app = App(
    usage="deployments.py <command> [OPTIONS]\n"
          "       deployments.py seed [OPTIONS]\n"
          "       deployments.py generate <version> [OPTIONS]"
)

MANIFEST_PATH = Path("deployments.json")

PSEUDO_MODULES: dict[str, str] = {
    "docs": "docs",
    "repo": "repo",
}


def _discover_module_map() -> dict[str, str]:
    """Build a mapping from changelog heading text to canonical module id.

    Scans the ``modules/`` directory for ``terracotta-*`` subdirectories and
    derives canonical ids (e.g. ``terracotta-provider-modrinth`` -> ``modrinth``).
    Merges with :data:`PSEUDO_MODULES` for non-code sections like Docs/Repo.
    """
    mapping: dict[str, str] = {}

    modules_dir = Path("modules")
    if modules_dir.is_dir():
        for child in sorted(modules_dir.iterdir()):
            if not child.is_dir() or not child.name.startswith("terracotta-"):
                continue
            canonical = child.name.removeprefix("terracotta-")
            if canonical.startswith("provider-"):
                canonical = canonical.removeprefix("provider-")
            mapping[canonical] = canonical

    mapping.update(PSEUDO_MODULES)
    mapping["core-/-state-filesystem"] = "core"
    return mapping


MODULE_MAP = _discover_module_map()


def _canonical_module(raw: str) -> str | None:
    key = raw.strip().lower().replace(" ", "-")
    if key in MODULE_MAP:
        return MODULE_MAP[key]
    for map_key, canonical in MODULE_MAP.items():
        if map_key in key:
            return canonical
    return None


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


def extract_modules(section_body: str) -> list[str]:
    """Extract canonical module identifiers from ``**Module**`` or ``#### Module`` headings.

    Scans for ``**ModuleName**`` lines or ``#### ModuleName`` lines within the
    section body and returns a de-duplicated, sorted list of canonical module ids.
    """
    modules: set[str] = set()
    for match in re.finditer(r"^\*\*(.+?)\*\*\s*$", section_body, re.MULTILINE):
        canonical = _canonical_module(match.group(1))
        if canonical:
            modules.add(canonical)
    for match in re.finditer(r"^####\s+(.+?)\s*$", section_body, re.MULTILINE):
        canonical = _canonical_module(match.group(1))
        if canonical:
            modules.add(canonical)
    return sorted(modules)


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
    title: str | None = None,
    is_release: bool = False,
    modules: list[str] | None = None,
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
    title : str, optional
        Explicit title. If omitted, one is derived from the summary.
    is_release : bool
        Whether this deployment is a major milestone.
    modules : list[str], optional
        Explicit module identifiers. When omitted, they are inferred from
        ``**Module**`` headings in the changelog section.

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
    entry_modules = modules if modules is not None else extract_modules(section_body)
    entry_title = title if title else derive_title(summary)
    return {
        "version": version,
        "createdAt": created_at,
        "title": entry_title,
        "summary": summary,
        "modules": entry_modules,
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

    If an entry with the same version already exists, it is replaced.
    Entries are kept sorted by version (descending).

    Returns ``True`` if a new entry was added, ``False`` if an existing
    entry was replaced.
    """
    manifest = load_manifest(manifest_path)
    deployments = manifest["deployments"]
    version = entry["version"]

    existing_idx = None
    for i, d in enumerate(deployments):
        if d["version"] == version:
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


def _semver_sort_key(entry: dict) -> SemVerVersion:
    version = entry.get("version", "0.0.0")
    return SemVerVersion.parse(version)


def seed_from_changelog(
    changelog_path: Path = Path("CHANGELOG.md"),
    manifest_path: Path = MANIFEST_PATH,
    titles: dict[str, str] | None = None,
    releases: set[str] | None = None,
) -> int:
    """Seed the entire manifest from CHANGELOG.md.

    Parses every version section (excluding ``[Unreleased]``) and generates
    deployment entries. Existing entries with the same version are replaced.

    Returns the number of entries written.
    """
    content = changelog_path.read_text(encoding="utf-8")
    titles = titles or {}
    releases = releases or set()

    version_pattern = re.compile(
        r"^##\s+\[(\d+\.\d+\.\d+)\]\s*-\s*(\d{4}-\d{2}-\d{2})",
        re.MULTILINE,
    )

    entries: list[dict] = []
    for match in version_pattern.finditer(content):
        version = match.group(1)
        date_str = match.group(2) + "T00:00:00Z"
        section_body = parse_changelog_section(content, version)
        if not section_body:
            continue
        title = titles.get(version)
        is_release = version in releases
        try:
            entry = generate_deployment_entry(
                version, date_str, section_body, title, is_release
            )
            entries.append(entry)
        except ValueError:
            continue

    manifest = {"deployments": entries}
    entries.sort(key=_semver_sort_key, reverse=True)
    save_manifest(manifest, manifest_path)
    return len(entries)


@app.command
def seed(
    changelog: str = "CHANGELOG.md",
    manifest: str = "deployments.json",
):
    """Seed the deployment manifest from CHANGELOG.md.

    Parameters
    ----------
    changelog : str
        Path to the changelog file.
    manifest : str
        Path to the output manifest file.
    """
    count = seed_from_changelog(Path(changelog), Path(manifest))
    console.print(f"[green]✔[/green] Seeded {count} deployment entries to {manifest}")


@app.command
def generate(
    version: str,
    created_at: str | None = None,
    title: str | None = None,
    release: bool = False,
    changelog: str = "CHANGELOG.md",
    manifest: str = "deployments.json",
):
    """Generate and append a single deployment entry from the changelog.

    Parameters
    ----------
    version : str
        The version to generate an entry for (e.g. ``"0.9.0"``).
    created_at : str, optional
        ISO 8601 datetime string. Defaults to now.
    title : str, optional
        Explicit title. Derived from the summary if omitted.
    release : bool
        Mark this deployment as a major milestone.
    changelog : str
        Path to the changelog file.
    manifest : str
        Path to the manifest file.
    """
    content = Path(changelog).read_text(encoding="utf-8")
    section_body = parse_changelog_section(content, version)
    if not section_body:
        console.print(f"[red]No section found for version {version} in {changelog}[/red]")
        sys.exit(1)

    created_val = created_at or datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    entry = generate_deployment_entry(version, created_val, section_body, title, release)
    added = append_deployment(entry, Path(manifest))
    action = "Added" if added else "Updated"
    console.print(f"[green]✔[/green] {action} deployment entry for v{version} in {manifest}")


if __name__ == "__main__":
    app()

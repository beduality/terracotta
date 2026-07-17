import json
import tempfile
import unittest
from pathlib import Path

import sys
sys.path.insert(0, str(Path(__file__).parent.parent))

from scripts.deployments import (
    parse_changelog_section,
    extract_summary,
    extract_modules,
    derive_title,
    generate_deployment_entry,
    load_manifest,
    save_manifest,
    append_deployment,
    seed_from_changelog,
    _semver_sort_key,
    _canonical_module,
)


SAMPLE_CHANGELOG = """\
# Changelog

## [Unreleased]

### Changed

**Docs**

- Reworked the docs homepage.

## [0.8.0] - 2026-07-13

Stabilizes gallery item identity using persisted state so images can be renamed or reordered without triggering a delete-and-reupload cycle.

### Added

**Core**

- Added optional `key` property to `TerracottaGalleryItem` for stable local identity.

**Gradle Plugin**

- Added `key` property to `TerracottaGalleryExtension`.

**Modrinth**

- `ModrinthRegistryProvider` implements `GalleryIdentityReporter`.

## [0.7.1] - 2026-07-13

Tightens the changelog standard so `[Unreleased]` summaries are written as direct summaries.

### Fixed

**Repo**

- Corrected the 0.7.0 changelog summary.

### Changed

**Repo**

- Updated changelog guidelines.

## [0.7.0] - 2026-07-13

Narrows Hangar license handling by mapping common SPDX identifiers to Hangar's license values.

### Added

**Core**

- Added `TerracottaCategory` model.

**Hangar**

- Added `HangarLicenseMapper`.

**Core / State Filesystem**

- Moved `FileSystemStateSource` to new module.
"""


class TestParseChangelogSection(unittest.TestCase):

    def test_extracts_version_section(self):
        body = parse_changelog_section(SAMPLE_CHANGELOG, "0.8.0")
        self.assertIsNotNone(body)
        self.assertIn("Stabilizes gallery item identity", body)
        self.assertIn("**Core**", body)

    def test_returns_none_for_missing_version(self):
        body = parse_changelog_section(SAMPLE_CHANGELOG, "9.9.9")
        self.assertIsNone(body)

    def test_does_not_match_unreleased(self):
        body = parse_changelog_section(SAMPLE_CHANGELOG, "Unreleased")
        self.assertIsNotNone(body)
        self.assertIn("Reworked the docs homepage", body)


class TestExtractSummary(unittest.TestCase):

    def test_extracts_first_paragraph(self):
        body = parse_changelog_section(SAMPLE_CHANGELOG, "0.8.0")
        summary = extract_summary(body)
        self.assertIsNotNone(summary)
        self.assertTrue(summary.startswith("Stabilizes gallery item identity"))

    def test_returns_none_for_empty_body(self):
        self.assertIsNone(extract_summary(""))

    def test_returns_none_for_body_starting_with_heading(self):
        self.assertIsNone(extract_summary("### Added\n\n**Core**\n\n- Something"))


class TestExtractModules(unittest.TestCase):

    def test_extracts_multiple_modules(self):
        body = parse_changelog_section(SAMPLE_CHANGELOG, "0.8.0")
        modules = extract_modules(body)
        self.assertIn("core", modules)
        self.assertIn("gradle-plugin", modules)
        self.assertIn("modrinth", modules)

    def test_extracts_compound_module(self):
        body = parse_changelog_section(SAMPLE_CHANGELOG, "0.7.0")
        modules = extract_modules(body)
        self.assertIn("core", modules)
        self.assertIn("hangar", modules)

    def test_returns_empty_for_no_modules(self):
        modules = extract_modules("Just some text without module headings.")
        self.assertEqual(modules, [])

    def test_deduplicates_modules(self):
        modules = extract_modules("**Core**\n\n- Item 1\n\n**Core**\n\n- Item 2")
        self.assertEqual(modules, ["core"])


class TestCanonicalModule(unittest.TestCase):

    def test_known_mappings(self):
        self.assertEqual(_canonical_module("Core"), "core")
        self.assertEqual(_canonical_module("Gradle Plugin"), "gradle-plugin")
        self.assertEqual(_canonical_module("State Filesystem"), "state-filesystem")

    def test_unknown_returns_none(self):
        self.assertIsNone(_canonical_module("Unknown Module"))


class TestDeriveTitle(unittest.TestCase):

    def test_strips_leading_verb(self):
        title = derive_title("Adds project link management and full Gradle DSL parity.")
        self.assertNotIn("Adds", title)
        self.assertIn("Project", title)

    def test_strips_article_after_verb(self):
        title = derive_title("Fixes the docs homepage layout issues.")
        self.assertFalse(title.lower().startswith("the"))

    def test_fallback_when_no_leading_verb(self):
        title = derive_title("Gallery images now have stable identity keys.")
        self.assertTrue(len(title) > 0)

    def test_truncates_long_summaries(self):
        title = derive_title("Stabilizes gallery item identity using persisted state so images can be renamed or reordered without triggering a delete-and-reupload cycle.")
        self.assertTrue(title.endswith("..."))

    def test_preserves_short_summaries(self):
        title = derive_title("Fixes a bug.")
        self.assertFalse(title.endswith("..."))


class TestGenerateDeploymentEntry(unittest.TestCase):

    def test_generates_full_entry(self):
        body = parse_changelog_section(SAMPLE_CHANGELOG, "0.8.0")
        entry = generate_deployment_entry("0.8.0", "2026-07-13T00:00:00Z", body)
        self.assertEqual(entry["version"], "0.8.0")
        self.assertEqual(entry["createdAt"], "2026-07-13T00:00:00Z")
        self.assertTrue(entry["title"])
        self.assertTrue(entry["summary"].startswith("Stabilizes"))
        self.assertIn("core", entry["modules"])
        self.assertFalse(entry["isRelease"])

    def test_explicit_title_overrides_derived(self):
        body = parse_changelog_section(SAMPLE_CHANGELOG, "0.8.0")
        entry = generate_deployment_entry("0.8.0", "2026-07-13T00:00:00Z", body, title="My Custom Title")
        self.assertEqual(entry["title"], "My Custom Title")

    def test_is_release_flag(self):
        body = parse_changelog_section(SAMPLE_CHANGELOG, "0.8.0")
        entry = generate_deployment_entry("0.8.0", "2026-07-13T00:00:00Z", body, is_release=True)
        self.assertTrue(entry["isRelease"])

    def test_raises_on_missing_summary(self):
        with self.assertRaises(ValueError):
            generate_deployment_entry("0.0.0", "2026-01-01T00:00:00Z", "### Added\n\n**Core**\n\n- Item")


class TestManifestOperations(unittest.TestCase):

    def setUp(self):
        self.tmp = tempfile.NamedTemporaryFile(
            mode="w", suffix=".json", delete=False, encoding="utf-8"
        )
        self.tmp.write('{"deployments": []}')
        self.tmp.close()
        self.manifest_path = Path(self.tmp.name)

    def tearDown(self):
        self.manifest_path.unlink(missing_ok=True)

    def test_load_empty_manifest(self):
        manifest = load_manifest(self.manifest_path)
        self.assertEqual(manifest["deployments"], [])

    def test_load_missing_manifest(self):
        manifest = load_manifest(Path("/nonexistent/path.json"))
        self.assertEqual(manifest["deployments"], [])

    def test_save_and_reload(self):
        manifest = {"deployments": [{"version": "0.1.0", "createdAt": "2026-01-01T00:00:00Z"}]}
        save_manifest(manifest, self.manifest_path)
        loaded = load_manifest(self.manifest_path)
        self.assertEqual(len(loaded["deployments"]), 1)

    def test_append_new_entry(self):
        entry = {"version": "0.9.0", "createdAt": "2026-07-16T00:00:00Z", "title": "Test", "summary": "s", "modules": [], "isRelease": False}
        added = append_deployment(entry, self.manifest_path)
        self.assertTrue(added)
        manifest = load_manifest(self.manifest_path)
        self.assertEqual(len(manifest["deployments"]), 1)

    def test_append_replaces_existing(self):
        entry1 = {"version": "0.9.0", "createdAt": "2026-07-16T00:00:00Z", "title": "Old", "summary": "s", "modules": [], "isRelease": False}
        entry2 = {"version": "0.9.0", "createdAt": "2026-07-16T00:00:00Z", "title": "New", "summary": "s", "modules": [], "isRelease": False}
        append_deployment(entry1, self.manifest_path)
        added = append_deployment(entry2, self.manifest_path)
        self.assertFalse(added)
        manifest = load_manifest(self.manifest_path)
        self.assertEqual(len(manifest["deployments"]), 1)
        self.assertEqual(manifest["deployments"][0]["title"], "New")

    def test_append_sorts_descending(self):
        entries = [
            {"version": "0.1.0", "createdAt": "2026-01-01T00:00:00Z", "title": "A", "summary": "s", "modules": [], "isRelease": False},
            {"version": "0.3.0", "createdAt": "2026-03-01T00:00:00Z", "title": "B", "summary": "s", "modules": [], "isRelease": False},
            {"version": "0.2.0", "createdAt": "2026-02-01T00:00:00Z", "title": "C", "summary": "s", "modules": [], "isRelease": False},
        ]
        for e in entries:
            append_deployment(e, self.manifest_path)
        manifest = load_manifest(self.manifest_path)
        versions = [d["version"] for d in manifest["deployments"]]
        self.assertEqual(versions, ["0.3.0", "0.2.0", "0.1.0"])


class TestSeedFromChangelog(unittest.TestCase):

    def setUp(self):
        self.tmp_changelog = tempfile.NamedTemporaryFile(
            mode="w", suffix=".md", delete=False, encoding="utf-8"
        )
        self.tmp_changelog.write(SAMPLE_CHANGELOG)
        self.tmp_changelog.close()
        self.changelog_path = Path(self.tmp_changelog.name)

        self.tmp_manifest = tempfile.NamedTemporaryFile(
            mode="w", suffix=".json", delete=False, encoding="utf-8"
        )
        self.tmp_manifest.write('{"deployments": []}')
        self.tmp_manifest.close()
        self.manifest_path = Path(self.tmp_manifest.name)

    def tearDown(self):
        self.changelog_path.unlink(missing_ok=True)
        self.manifest_path.unlink(missing_ok=True)

    def test_seeds_all_version_sections(self):
        count = seed_from_changelog(self.changelog_path, self.manifest_path)
        self.assertEqual(count, 3)
        manifest = load_manifest(self.manifest_path)
        versions = [d["version"] for d in manifest["deployments"]]
        self.assertEqual(versions, ["0.8.0", "0.7.1", "0.7.0"])

    def test_seed_with_titles(self):
        count = seed_from_changelog(
            self.changelog_path,
            self.manifest_path,
            titles={"0.8.0": "Custom Title"},
        )
        self.assertEqual(count, 3)
        manifest = load_manifest(self.manifest_path)
        entry = next(d for d in manifest["deployments"] if d["version"] == "0.8.0")
        self.assertEqual(entry["title"], "Custom Title")

    def test_seed_with_releases(self):
        count = seed_from_changelog(
            self.changelog_path,
            self.manifest_path,
            releases={"0.8.0"},
        )
        self.assertEqual(count, 3)
        manifest = load_manifest(self.manifest_path)
        release_entry = next(d for d in manifest["deployments"] if d["version"] == "0.8.0")
        patch_entry = next(d for d in manifest["deployments"] if d["version"] == "0.7.1")
        self.assertTrue(release_entry["isRelease"])
        self.assertFalse(patch_entry["isRelease"])

    def test_seed_excludes_unreleased(self):
        count = seed_from_changelog(self.changelog_path, self.manifest_path)
        manifest = load_manifest(self.manifest_path)
        versions = [d["version"] for d in manifest["deployments"]]
        self.assertNotIn("Unreleased", versions)


class TestSemverSortKey(unittest.TestCase):

    def test_sorts_correctly(self):
        entries = [
            {"version": "0.10.0"},
            {"version": "0.2.0"},
            {"version": "0.1.0"},
        ]
        entries.sort(key=_semver_sort_key, reverse=True)
        versions = [e["version"] for e in entries]
        self.assertEqual(versions, ["0.10.0", "0.2.0", "0.1.0"])


if __name__ == "__main__":
    unittest.main()

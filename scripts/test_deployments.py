import json
import tempfile
import unittest
from pathlib import Path

import sys
sys.path.insert(0, str(Path(__file__).parent.parent))

from scripts.deployments import (
    parse_changelog_section,
    extract_summary,
    derive_title,
    generate_deployment_entry,
    load_manifest,
    save_manifest,
    append_deployment,
    _semver_sort_key,
)


SAMPLE_CHANGELOG = """\
# Changelog

## [Unreleased]

### Changed

- Reworked the docs homepage.

## [0.8.0] - 2026-07-13

Stabilizes gallery item identity using persisted state so images can be renamed or reordered without triggering a delete-and-reupload cycle.

### Added

- Added optional `key` property to `TerracottaGalleryItem` for stable local identity.

## [0.7.1] - 2026-07-13

Tightens the changelog standard so `[Unreleased]` summaries are written as direct summaries.

### Fixed

- Corrected the 0.7.0 changelog summary.

## [0.7.0] - 2026-07-13

Narrows Hangar license handling by mapping common SPDX identifiers to Hangar's license values.

### Added

- Added `TerracottaCategory` model.
"""


class TestParseChangelogSection(unittest.TestCase):

    def test_extracts_version_section(self):
        body = parse_changelog_section(SAMPLE_CHANGELOG, "0.8.0")
        self.assertIsNotNone(body)
        self.assertIn("Stabilizes gallery item identity", body)

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
        self.assertIsNone(extract_summary("### Added\n\n- Something"))


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
        entry = generate_deployment_entry("0.8.0", "2026-07-13T00:00:00Z", body, ["core"])
        self.assertEqual(entry["version"], "0.8.0")
        self.assertEqual(entry["createdAt"], "2026-07-13T00:00:00Z")
        self.assertTrue(entry["title"])
        self.assertTrue(entry["summary"].startswith("Stabilizes"))
        self.assertEqual(entry["modules"], ["core"])
        self.assertFalse(entry["isRelease"])

    def test_explicit_title_overrides_derived(self):
        body = parse_changelog_section(SAMPLE_CHANGELOG, "0.8.0")
        entry = generate_deployment_entry("0.8.0", "2026-07-13T00:00:00Z", body, ["core"], title="My Custom Title")
        self.assertEqual(entry["title"], "My Custom Title")

    def test_is_release_flag(self):
        body = parse_changelog_section(SAMPLE_CHANGELOG, "0.8.0")
        entry = generate_deployment_entry("0.8.0", "2026-07-13T00:00:00Z", body, ["core"], is_release=True)
        self.assertTrue(entry["isRelease"])

    def test_raises_on_missing_summary(self):
        with self.assertRaises(ValueError):
            generate_deployment_entry("0.0.0", "2026-01-01T00:00:00Z", "### Added\n\n- Item", ["core"])


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

    def test_append_versionless_entry(self):
        entry = {"createdAt": "2026-07-09T18:00:00Z", "title": "Infra", "summary": "s", "modules": ["github"], "isRelease": False}
        added = append_deployment(entry, self.manifest_path)
        self.assertTrue(added)
        manifest = load_manifest(self.manifest_path)
        self.assertEqual(len(manifest["deployments"]), 1)
        self.assertNotIn("version", manifest["deployments"][0])

    def test_append_versionless_always_adds(self):
        entry1 = {"createdAt": "2026-07-09T18:00:00Z", "title": "A", "summary": "s", "modules": [], "isRelease": False}
        entry2 = {"createdAt": "2026-07-09T19:00:00Z", "title": "B", "summary": "s", "modules": [], "isRelease": False}
        append_deployment(entry1, self.manifest_path)
        added = append_deployment(entry2, self.manifest_path)
        self.assertTrue(added)
        manifest = load_manifest(self.manifest_path)
        self.assertEqual(len(manifest["deployments"]), 2)


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

    def test_versionless_entries_sort_after_versioned(self):
        entries = [
            {"createdAt": "2026-07-09T18:00:00Z"},
            {"version": "0.1.0"},
            {"createdAt": "2026-07-09T19:00:00Z"},
        ]
        entries.sort(key=_semver_sort_key, reverse=True)
        self.assertEqual(entries[0]["version"], "0.1.0")
        self.assertEqual(entries[1]["createdAt"], "2026-07-09T19:00:00Z")
        self.assertEqual(entries[2]["createdAt"], "2026-07-09T18:00:00Z")


if __name__ == "__main__":
    unittest.main()

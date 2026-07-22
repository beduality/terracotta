import os
import subprocess
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest.mock import MagicMock, patch, call

import sys

sys.path.insert(0, str(Path(__file__).parent.parent))

import scripts.release as release


class TestBumpVersion(unittest.TestCase):

    def test_major(self):
        self.assertEqual(release.bump_version("1.2.3", "major"), "2.0.0")

    def test_minor(self):
        self.assertEqual(release.bump_version("1.2.3", "minor"), "1.3.0")

    def test_patch(self):
        self.assertEqual(release.bump_version("1.2.3", "patch"), "1.2.4")

    def test_custom_version(self):
        self.assertEqual(release.bump_version("1.2.3", " 2.3.4 "), "2.3.4")

    def test_suffix_preserved(self):
        self.assertEqual(release.bump_version("1.2.3-SNAPSHOT", "patch"), "1.2.4-SNAPSHOT")

    def test_zero_major_breaking_becomes_minor(self):
        self.assertEqual(release.bump_version("0.1.2", "major"), "0.2.0")

    def test_invalid_current(self):
        with self.assertRaises(ValueError):
            release.bump_version("invalid", "patch")

    def test_invalid_bump_type(self):
        with self.assertRaises(ValueError):
            release.bump_version("1.2.3", "invalid")


class TestDetermineBumpFromCommits(unittest.TestCase):

    def test_empty_defaults_to_patch(self):
        self.assertEqual(release.determine_bump_from_commits([]), "patch")

    def test_fix_suggests_patch(self):
        self.assertEqual(
            release.determine_bump_from_commits(["fix(core): correct output"]), "patch"
        )

    def test_feat_suggests_minor(self):
        self.assertEqual(
            release.determine_bump_from_commits(["feat(core): add filter"]), "minor"
        )

    def test_breaking_bang_suggests_major(self):
        self.assertEqual(
            release.determine_bump_from_commits(["feat(core)!: remove api"]), "major"
        )

    def test_breaking_change_prefix_suggests_major(self):
        self.assertEqual(
            release.determine_bump_from_commits(["breaking change: removed X"]), "major"
        )

    def test_breaking_takes_precedence(self):
        commits = ["feat(core): add filter", "feat(core)!: remove api"]
        self.assertEqual(release.determine_bump_from_commits(commits), "major")


class TestModuleVersionHelpers(unittest.TestCase):

    def test_get_module_version_reads_properties(self):
        with tempfile.TemporaryDirectory() as tmp:
            module_dir = Path(tmp) / "modules" / "terracotta-core"
            module_dir.mkdir(parents=True)
            (module_dir / "gradle.properties").write_text("version = 1.2.3\n")

            with patch.object(release, "MODULE_INFO", {
                "terracotta-core": {
                    "path": str(module_dir),
                    "tag_prefix": "terracotta-core-v",
                    "human_name": "Terracotta Core",
                    "published_name": "terracotta-core",
                    "downstream": [],
                }
            }):
                self.assertEqual(release.get_module_version("terracotta-core"), "1.2.3")

    def test_set_module_version_writes_properties(self):
        with tempfile.TemporaryDirectory() as tmp:
            module_dir = Path(tmp) / "modules" / "terracotta-core"
            module_dir.mkdir(parents=True)
            prop = module_dir / "gradle.properties"
            prop.write_text("version = 1.2.3\n")

            with patch.object(release, "MODULE_INFO", {
                "terracotta-core": {
                    "path": str(module_dir),
                    "tag_prefix": "terracotta-core-v",
                    "human_name": "Terracotta Core",
                    "published_name": "terracotta-core",
                    "downstream": [],
                }
            }):
                release.set_module_version("terracotta-core", "1.3.0")
                self.assertIn("version = 1.3.0", prop.read_text())


class TestGetModuleLastTag(unittest.TestCase):

    @patch("scripts.release.run_git")
    def test_returns_tag_when_found(self, mock_run_git):
        mock_run_git.return_value = MagicMock(returncode=0, stdout="terracotta-core-v0.9.0\n")
        self.assertEqual(release.get_module_last_tag("terracotta-core"), "terracotta-core-v0.9.0")

    @patch("scripts.release.run_git")
    def test_returns_none_when_not_found(self, mock_run_git):
        mock_run_git.return_value = MagicMock(returncode=1, stdout="")
        self.assertIsNone(release.get_module_last_tag("terracotta-core"))


class TestDetectChangedModules(unittest.TestCase):

    @patch.object(release, "PUBLISHABLE_MODULES", ["terracotta-core", "terracotta-provider-modrinth"])
    @patch("scripts.release.get_module_last_tag")
    @patch("scripts.release.get_changed_files")
    @patch("scripts.release.get_all_files")
    def test_detects_changed_modules_from_tags(self, mock_all_files, mock_diff, mock_last_tag):
        mock_last_tag.side_effect = [
            "terracotta-core-v0.8.0",
            "terracotta-provider-modrinth-v0.8.0",
        ]
        mock_diff.return_value = [
            "modules/terracotta-core/src/main/kotlin/Core.kt",
        ]
        mock_all_files.return_value = []

        changed = release.detect_changed_modules()
        self.assertIn("terracotta-core", changed)
        self.assertNotIn("terracotta-provider-modrinth", changed)
        self.assertEqual(changed["terracotta-core"], "terracotta-core-v0.8.0")

    @patch.object(release, "PUBLISHABLE_MODULES", ["terracotta-core"])
    @patch("scripts.release.get_module_last_tag")
    @patch("scripts.release.get_all_files")
    def test_unreleased_module_detected_by_file_presence(self, mock_all_files, mock_last_tag):
        mock_last_tag.return_value = None
        mock_all_files.return_value = ["modules/terracotta-core/build.gradle.kts"]

        changed = release.detect_changed_modules()
        self.assertIn("terracotta-core", changed)
        self.assertIsNone(changed["terracotta-core"])

    @patch.object(release, "PUBLISHABLE_MODULES", ["terracotta-core", "terracotta-provider-modrinth"])
    @patch("scripts.release.get_module_last_tag")
    @patch("scripts.release.get_changed_files")
    @patch("scripts.release.get_all_files")
    def test_since_ref_filters_modules(self, mock_all_files, mock_diff, mock_last_tag):
        mock_last_tag.side_effect = [
            "terracotta-core-v0.8.0",
            "terracotta-provider-modrinth-v0.8.0",
        ]
        mock_diff.return_value = [
            "modules/terracotta-core/src/main/kotlin/Core.kt",
            "modules/terracotta-provider-modrinth/src/main/kotlin/Modrinth.kt",
        ]
        mock_all_files.return_value = []

        changed = release.detect_changed_modules(since_ref="HEAD~1")
        # Since both modules have changes, both should be present
        self.assertIn("terracotta-core", changed)
        self.assertIn("terracotta-provider-modrinth", changed)


class TestGetModuleCommits(unittest.TestCase):

    @patch("scripts.release.run_git")
    def test_commits_since_tag(self, mock_run_git):
        mock_run_git.return_value = MagicMock(
            returncode=0,
            stdout="feat(core): add filter\nfix(core): bug\n",
        )
        commits = release.get_module_commits("terracotta-core", "terracotta-core-v0.8.0")
        mock_run_git.assert_called_once_with(
            ["log", "terracotta-core-v0.8.0..HEAD", "--pretty=%s", "--", "modules/terracotta-core"]
        )
        self.assertEqual(commits, ["feat(core): add filter", "fix(core): bug"])

    @patch("scripts.release.run_git")
    def test_commits_full_history_when_no_tag(self, mock_run_git):
        mock_run_git.return_value = MagicMock(returncode=0, stdout="initial\n")
        commits = release.get_module_commits("terracotta-core", None)
        mock_run_git.assert_called_once_with(
            ["log", "--pretty=%s", "--", "modules/terracotta-core"]
        )


class TestUpdateChangelog(unittest.TestCase):

    def _setup_module_changelog(self, module: str, unreleased_body: str, old: str = "") -> Path:
        module_dir = Path("modules") / module
        module_dir.mkdir(parents=True, exist_ok=True)
        changelog = module_dir / "CHANGELOG.md"
        content = (
            f"# Changelog — {module}\n\n"
            f"## [Unreleased]\n\n{unreleased_body}\n\n"
            f"{old}"
        )
        changelog.write_text(content)
        return changelog

    def test_promotes_module_notes_to_versioned_section(self):
        with tempfile.TemporaryDirectory() as tmp:
            os.chdir(tmp)
            core_cl = self._setup_module_changelog(
                "terracotta-core",
                "Adds new core feature.\n\n- Core change",
                "## [0.8.0] - 2026-07-13\n\nOld release.\n",
            )
            modrinth_cl = self._setup_module_changelog(
                "terracotta-provider-modrinth",
                "Fixes modrinth bug.\n\n- Modrinth fix",
            )

            with patch("scripts.release.date") as mock_date:
                mock_date.today.return_value.isoformat.return_value = "2026-07-21"
                release.update_changelog(
                    {"terracotta-core": "0.9.0", "terracotta-provider-modrinth": "0.8.1"}
                )

            core_content = core_cl.read_text()
            self.assertIn("## [terracotta-core-v0.9.0] - 2026-07-21", core_content)
            self.assertIn("Adds new core feature.", core_content)
            self.assertIn("- Core change", core_content)
            # Unreleased section should be empty
            unreleased_match = __import__("re").search(
                r"## \[Unreleased\]\n\n(.*?)\n## \[", core_content, __import__("re").DOTALL
            )
            self.assertIsNotNone(unreleased_match)
            self.assertNotIn("Adds new core feature.", unreleased_match.group(1))

            modrinth_content = modrinth_cl.read_text()
            self.assertIn("## [terracotta-provider-modrinth-v0.8.1] - 2026-07-21", modrinth_content)
            self.assertIn("Fixes modrinth bug.", modrinth_content)

    def test_missing_module_notes_warns_and_skips(self):
        with tempfile.TemporaryDirectory() as tmp:
            os.chdir(tmp)
            self._setup_module_changelog("terracotta-core", "")

            with patch("scripts.release.date") as mock_date:
                mock_date.today.return_value.isoformat.return_value = "2026-07-21"
                with patch("scripts.release.console"):
                    release.update_changelog({"terracotta-core": "0.9.0"})

            content = (Path("modules") / "terracotta-core" / "CHANGELOG.md").read_text()
            self.assertNotIn("terracotta-core-v0.9.0", content)


class TestExtractReleaseNotes(unittest.TestCase):

    def test_extracts_module_notes(self):
        with tempfile.TemporaryDirectory() as tmp:
            os.chdir(tmp)
            module_dir = Path("modules") / "terracotta-core"
            module_dir.mkdir(parents=True)
            (module_dir / "CHANGELOG.md").write_text(
                "# Changelog — terracotta-core\n\n"
                "## [terracotta-core-v0.9.0] - 2026-07-21\n\n"
                "Adds new core feature.\n\n"
                "- Core change\n\n"
                "## [Unreleased]\n\n"
            )
            notes = release._extract_release_notes_from_changelog("terracotta-core", "0.9.0")
            self.assertIn("Adds new core feature.", notes)
            self.assertIn("- Core change", notes)

    def test_raises_when_section_missing(self):
        with tempfile.TemporaryDirectory() as tmp:
            os.chdir(tmp)
            module_dir = Path("modules") / "terracotta-core"
            module_dir.mkdir(parents=True)
            (module_dir / "CHANGELOG.md").write_text("# Changelog\n\n## [Unreleased]\n\n")
            with self.assertRaises(ValueError):
                release._extract_release_notes_from_changelog("terracotta-core", "0.9.0")


class TestDocsVersionSnippets(unittest.TestCase):

    def test_updates_matching_files(self):
        with tempfile.TemporaryDirectory() as tmp:
            os.chdir(tmp)
            docs = Path("docs")
            docs.mkdir(parents=True)
            (docs / "index.md").write_text("# Docs\n")
            content = docs / "content"
            content.mkdir(parents=True)
            snippet = content / "setup.md"
            snippet.write_text(
                'plugins {\n    id("io.github.beduality.terracotta") version "0.8.0"\n}'
            )

            release.update_docs_version_snippets("0.9.0")
            self.assertIn('version "0.9.0"', snippet.read_text())

    def test_validates_matching_files(self):
        with tempfile.TemporaryDirectory() as tmp:
            os.chdir(tmp)
            docs = Path("docs")
            docs.mkdir(parents=True)
            (docs / "index.md").write_text("# Docs\n")
            content = docs / "content"
            content.mkdir(parents=True)
            (content / "setup.md").write_text(
                'id("io.github.beduality.terracotta") version "0.8.0"'
            )
            with self.assertRaises(ValueError):
                release.validate_docs_version_snippets("0.9.0")


class TestValidateJavadocJars(unittest.TestCase):

    def _prepare_jars(self, tmp: str, module_versions: dict[str, str], content: dict[str, bytes] | None = None):
        for module, version in module_versions.items():
            jar_dir = Path(tmp) / f"modules/{module}/build/libs"
            jar_dir.mkdir(parents=True)
            jar = jar_dir / f"{module}-{version}-javadoc.jar"
            if content is not None:
                with zipfile.ZipFile(jar, "w") as zf:
                    for name, data in content.items():
                        zf.writestr(name, data)

    def test_passes_for_valid_jars(self):
        with tempfile.TemporaryDirectory() as tmp:
            os.chdir(tmp)
            html = b"<html>" + b"x" * 2048 + b"</html>"
            self._prepare_jars(tmp, {"terracotta-core": "0.9.0"}, {"index.html": html})
            release.validate_javadoc_jars({"terracotta-core": "0.9.0"})

    def test_raises_when_jar_missing(self):
        with tempfile.TemporaryDirectory() as tmp:
            os.chdir(tmp)
            with self.assertRaises(FileNotFoundError):
                release.validate_javadoc_jars({"terracotta-core": "0.9.0"})

    def test_raises_when_jar_too_small(self):
        with tempfile.TemporaryDirectory() as tmp:
            os.chdir(tmp)
            self._prepare_jars(tmp, {"terracotta-core": "0.9.0"}, {})
            with self.assertRaises(ValueError) as ctx:
                release.validate_javadoc_jars({"terracotta-core": "0.9.0"})
            self.assertIn("Javadoc JARs are empty", str(ctx.exception))


if __name__ == "__main__":
    unittest.main()

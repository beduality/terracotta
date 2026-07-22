import os
import re
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

    def test_prerelease_dropped_on_bump(self):
        self.assertEqual(release.bump_version("1.2.3-SNAPSHOT", "patch"), "1.2.4")
        self.assertEqual(release.bump_version("0.8.0-beta.1", "minor"), "0.9.0")

    def test_zero_major_breaking_becomes_minor(self):
        self.assertEqual(release.bump_version("0.1.2", "major"), "0.2.0")

    def test_invalid_current(self):
        with self.assertRaises(ValueError):
            release.bump_version("invalid", "patch")

    def test_invalid_bump_type(self):
        with self.assertRaises(ValueError):
            release.bump_version("1.2.3", "invalid")


class TestValidateNextVersion(unittest.TestCase):

    def test_accepts_next_patch(self):
        release.validate_next_version("0.8.0", "0.8.1")

    def test_accepts_next_minor(self):
        release.validate_next_version("0.8.0", "0.9.0")

    def test_accepts_next_major_zero_x(self):
        release.validate_next_version("0.8.0", "0.9.0")

    def test_accepts_next_major_one_x(self):
        release.validate_next_version("1.2.3", "2.0.0")

    def test_accepts_next_minor_with_suffix(self):
        release.validate_next_version("0.8.0", "0.9.0-beta.1")

    def test_accepts_stable_from_prerelease(self):
        release.validate_next_version("0.8.0-beta.1", "0.8.0")

    def test_accepts_next_patch_from_prerelease(self):
        release.validate_next_version("0.8.0-beta.1", "0.8.1")

    def test_rejects_same_version(self):
        with self.assertRaises(ValueError) as ctx:
            release.validate_next_version("0.8.0", "0.8.0")
        self.assertIn("already released", str(ctx.exception))

    def test_rejects_downgrade(self):
        with self.assertRaises(ValueError) as ctx:
            release.validate_next_version("0.8.0", "0.7.0")
        self.assertIn("downgrade", str(ctx.exception))

    def test_rejects_jump_skipping_minor(self):
        with self.assertRaises(ValueError) as ctx:
            release.validate_next_version("0.8.0", "0.10.0")
        self.assertIn("not a valid next bump", str(ctx.exception))

    def test_rejects_jump_skipping_patch(self):
        with self.assertRaises(ValueError) as ctx:
            release.validate_next_version("0.8.0", "0.8.2")
        self.assertIn("not a valid next bump", str(ctx.exception))

    def test_rejects_jump_from_prerelease(self):
        with self.assertRaises(ValueError) as ctx:
            release.validate_next_version("0.8.0-beta.1", "0.10.0")
        self.assertIn("not a valid next bump", str(ctx.exception))

    def test_rejects_older_patch_of_same_minor(self):
        with self.assertRaises(ValueError) as ctx:
            release.validate_next_version("0.8.5", "0.8.3")
        self.assertIn("downgrade", str(ctx.exception))


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
            # No double heading
            self.assertEqual(core_content.count("## [Unreleased]"), 1)
            # Unreleased section should be empty — match up to the next ## heading
            unreleased_match = __import__("re").search(
                r"## \[Unreleased\]\n\n(.*?)(?=## \[)", core_content, __import__("re").DOTALL
            )
            self.assertIsNotNone(unreleased_match)
            unreleased_body = unreleased_match.group(1).strip()
            self.assertEqual(unreleased_body, "")

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


class TestReleaseDryRun(unittest.TestCase):
    """Dry-run tests for the release command.

    All tests use dry_run=True so no files are modified, no builds run,
    and no artifacts are published. We mock git and version-reading
    functions to control the inputs.
    """

    def _setup_module_props(self, module: str, version: str):
        module_dir = Path("modules") / module
        module_dir.mkdir(parents=True, exist_ok=True)
        (module_dir / "gradle.properties").write_text(f"version = {version}\n")

    def _setup_module_changelog(self, module: str, unreleased_body: str = ""):
        module_dir = Path("modules") / module
        module_dir.mkdir(parents=True, exist_ok=True)
        (module_dir / "CHANGELOG.md").write_text(
            f"# Changelog — {module}\n\n"
            f"## [Unreleased]\n\n{unreleased_body}\n\n"
        )

    def _setup_all_modules(self, versions: dict[str, str]):
        for module, version in versions.items():
            self._setup_module_props(module, version)
            self._setup_module_changelog(module, "Some changes.\n\n- Entry")

    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        os.chdir(self._tmp.name)

    def tearDown(self):
        os.chdir(Path(__file__).resolve().parent.parent)
        self._tmp.cleanup()

    # --- Explicit module selection (--modules) ---

    @patch("scripts.release.console")
    def test_single_module_explicit_bump(self, _mock_console):
        self._setup_all_modules({"terracotta-core": "0.8.0"})
        with patch("scripts.release.get_module_last_tag", return_value="terracotta-core-v0.8.0"):
            with self.assertRaises(SystemExit) as ctx:
                release.release(
                    bump="minor", modules="terracotta-core", dry_run=True, yes=True
                )
            self.assertEqual(ctx.exception.code, 0)

    @patch("scripts.release.console")
    def test_multiple_modules_explicit_bump(self, _mock_console):
        self._setup_all_modules({
            "terracotta-core": "0.8.0",
            "terracotta-provider-modrinth": "0.8.0",
        })
        with patch("scripts.release.get_module_last_tag") as mock_tag:
            mock_tag.side_effect = lambda m: f"{release.MODULE_INFO[m]['tag_prefix']}0.8.0"
            with self.assertRaises(SystemExit) as ctx:
                release.release(
                    bump="patch",
                    modules="terracotta-core,terracotta-provider-modrinth",
                    dry_run=True,
                    yes=True,
                )
            self.assertEqual(ctx.exception.code, 0)

    @patch("scripts.release.console")
    def test_unknown_module_raises(self, _mock_console):
        with self.assertRaises(ValueError) as ctx:
            release.release(modules="nonexistent-module", dry_run=True, yes=True)
        self.assertIn("Unknown modules", str(ctx.exception))

    # --- Auto bump from commits ---

    @patch("scripts.release.console")
    def test_auto_bump_minor_from_feat_commits(self, _mock_console):
        self._setup_all_modules({"terracotta-core": "0.8.0"})
        with patch("scripts.release.get_module_last_tag", return_value="terracotta-core-v0.8.0"):
            with patch("scripts.release.get_module_commits", return_value=[
                "feat(core): add new feature",
                "fix(core): fix something",
            ]):
                with self.assertRaises(SystemExit) as ctx:
                    release.release(
                        bump="auto", modules="terracotta-core", dry_run=True, yes=True
                    )
                self.assertEqual(ctx.exception.code, 0)

    @patch("scripts.release.console")
    def test_auto_bump_major_from_breaking_commits(self, _mock_console):
        self._setup_all_modules({"terracotta-core": "0.8.0"})
        with patch("scripts.release.get_module_last_tag", return_value="terracotta-core-v0.8.0"):
            with patch("scripts.release.get_module_commits", return_value=[
                "feat(core)!: breaking change",
            ]):
                with self.assertRaises(SystemExit) as ctx:
                    release.release(
                        bump="auto", modules="terracotta-core", dry_run=True, yes=True
                    )
                self.assertEqual(ctx.exception.code, 0)

    @patch("scripts.release.console")
    def test_auto_bump_patch_from_fix_commits(self, _mock_console):
        self._setup_all_modules({"terracotta-core": "0.8.0"})
        with patch("scripts.release.get_module_last_tag", return_value="terracotta-core-v0.8.0"):
            with patch("scripts.release.get_module_commits", return_value=[
                "fix(core): fix something",
                "docs: update readme",
            ]):
                with self.assertRaises(SystemExit) as ctx:
                    release.release(
                        bump="auto", modules="terracotta-core", dry_run=True, yes=True
                    )
                self.assertEqual(ctx.exception.code, 0)

    # --- Custom version bump ---

    @patch("scripts.release.console")
    def test_custom_version_bump(self, _mock_console):
        self._setup_all_modules({"terracotta-core": "0.8.0"})
        with patch("scripts.release.get_module_last_tag", return_value="terracotta-core-v0.8.0"):
            with self.assertRaises(SystemExit) as ctx:
                release.release(
                    bump="0.9.0", modules="terracotta-core", dry_run=True, yes=True
                )
            self.assertEqual(ctx.exception.code, 0)

    @patch("scripts.release.console")
    def test_custom_version_jump_rejected(self, _mock_console):
        self._setup_all_modules({"terracotta-core": "0.8.0"})
        with patch("scripts.release.get_module_last_tag", return_value="terracotta-core-v0.8.0"):
            with self.assertRaises(ValueError) as ctx:
                release.release(
                    bump="0.10.0", modules="terracotta-core", dry_run=True, yes=True
                )
            self.assertIn("not a valid next bump", str(ctx.exception))

    @patch("scripts.release.console")
    def test_custom_version_downgrade_rejected(self, _mock_console):
        self._setup_all_modules({"terracotta-core": "0.8.0"})
        with patch("scripts.release.get_module_last_tag", return_value="terracotta-core-v0.8.0"):
            with self.assertRaises(ValueError) as ctx:
                release.release(
                    bump="0.7.0", modules="terracotta-core", dry_run=True, yes=True
                )
            self.assertIn("downgrade", str(ctx.exception))

    @patch("scripts.release.console")
    def test_custom_version_same_rejected(self, _mock_console):
        self._setup_all_modules({"terracotta-core": "0.8.0"})
        with patch("scripts.release.get_module_last_tag", return_value="terracotta-core-v0.8.0"):
            with self.assertRaises(ValueError) as ctx:
                release.release(
                    bump="0.8.0", modules="terracotta-core", dry_run=True, yes=True
                )
            self.assertIn("already released", str(ctx.exception))

    # --- Change detection (no --modules) ---

    @patch("scripts.release.console")
    def test_no_changed_modules_exits_zero(self, _mock_console):
        self._setup_all_modules({"terracotta-core": "0.8.0"})
        with patch("scripts.release.detect_changed_modules", return_value={}):
            with self.assertRaises(SystemExit) as ctx:
                release.release(dry_run=True, yes=True)
            self.assertEqual(ctx.exception.code, 0)

    @patch("scripts.release.console")
    def test_detect_changed_modules_auto_bump(self, _mock_console):
        self._setup_all_modules({
            "terracotta-core": "0.8.0",
            "terracotta-gradle-plugin": "0.8.0",
        })
        with patch("scripts.release.detect_changed_modules", return_value={
            "terracotta-core": "terracotta-core-v0.8.0",
        }):
            with patch("scripts.release.get_module_commits", return_value=[
                "feat(core): new thing",
            ]):
                with self.assertRaises(SystemExit) as ctx:
                    release.release(bump="auto", dry_run=True, yes=True)
                self.assertEqual(ctx.exception.code, 0)

    # --- Dry-run does not modify files ---

    @patch("scripts.release.console")
    def test_dry_run_does_not_modify_gradle_properties(self, _mock_console):
        self._setup_all_modules({"terracotta-core": "0.8.0"})
        props_path = Path("modules/terracotta-core/gradle.properties")
        original = props_path.read_text()
        with patch("scripts.release.get_module_last_tag", return_value="terracotta-core-v0.8.0"):
            with self.assertRaises(SystemExit):
                release.release(bump="minor", modules="terracotta-core", dry_run=True, yes=True)
        self.assertEqual(props_path.read_text(), original)

    @patch("scripts.release.console")
    def test_dry_run_does_not_modify_changelog(self, _mock_console):
        self._setup_all_modules({"terracotta-core": "0.8.0"})
        cl_path = Path("modules/terracotta-core/CHANGELOG.md")
        original = cl_path.read_text()
        with patch("scripts.release.get_module_last_tag", return_value="terracotta-core-v0.8.0"):
            with self.assertRaises(SystemExit):
                release.release(bump="minor", modules="terracotta-core", dry_run=True, yes=True)
        self.assertEqual(cl_path.read_text(), original)

    @patch("scripts.release.console")
    def test_dry_run_does_not_call_build_or_publish(self, _mock_console):
        self._setup_all_modules({"terracotta-core": "0.8.0"})
        with patch("scripts.release.get_module_last_tag", return_value="terracotta-core-v0.8.0"):
            with patch("scripts.release.build_modules") as mock_build:
                with patch("scripts.release.build_javadoc_jars") as mock_javadoc:
                    with patch("scripts.release.publish_module_to_central") as mock_publish:
                        with patch("scripts.release.create_github_release") as mock_gh:
                            with patch("scripts.release.run_command") as mock_run:
                                with self.assertRaises(SystemExit):
                                    release.release(
                                        bump="minor", modules="terracotta-core",
                                        dry_run=True, publish=True, push=True, yes=True,
                                    )
                                mock_build.assert_not_called()
                                mock_javadoc.assert_not_called()
                                mock_publish.assert_not_called()
                                mock_gh.assert_not_called()
                                mock_run.assert_not_called()

    # --- All modules at once ---

    @patch("scripts.release.console")
    def test_all_modules_explicit(self, _mock_console):
        versions = {m: "0.8.0" for m in release.PUBLISHABLE_MODULES}
        self._setup_all_modules(versions)
        all_modules = ",".join(release.PUBLISHABLE_MODULES)
        with patch("scripts.release.get_module_last_tag") as mock_tag:
            mock_tag.side_effect = lambda m: f"{release.MODULE_INFO[m]['tag_prefix']}0.8.0"
            with self.assertRaises(SystemExit) as ctx:
                release.release(
                    bump="patch", modules=all_modules, dry_run=True, yes=True
                )
            self.assertEqual(ctx.exception.code, 0)

    # --- Module with no prior tag (first release) ---

    @patch("scripts.release.console")
    def test_first_release_no_prior_tag(self, _mock_console):
        self._setup_all_modules({"terracotta-core": "0.1.0"})
        with patch("scripts.release.get_module_last_tag", return_value=None):
            with patch("scripts.release.get_module_commits", return_value=[
                "feat(core): initial feature",
            ]):
                with self.assertRaises(SystemExit) as ctx:
                    release.release(
                        bump="auto", modules="terracotta-core", dry_run=True, yes=True
                    )
                self.assertEqual(ctx.exception.code, 0)

    # --- Prerelease handling in version bump ---

    @patch("scripts.release.console")
    def test_prerelease_dropped_in_bump_dry_run(self, _mock_console):
        self._setup_module_props("terracotta-core", "0.8.0-beta.1")
        self._setup_module_changelog("terracotta-core", "Beta changes.\n\n- Entry")
        with patch("scripts.release.get_module_last_tag", return_value="terracotta-core-v0.8.0-beta.1"):
            with self.assertRaises(SystemExit) as ctx:
                release.release(
                    bump="patch", modules="terracotta-core", dry_run=True, yes=True
                )
            self.assertEqual(ctx.exception.code, 0)

    # --- Since ref filtering ---

    @patch("scripts.release.console")
    def test_since_ref_filters_modules(self, _mock_console):
        self._setup_all_modules({
            "terracotta-core": "0.8.0",
            "terracotta-provider-modrinth": "0.8.0",
        })
        with patch("scripts.release.detect_changed_modules", return_value={
            "terracotta-core": "terracotta-core-v0.8.0",
        }):
            with patch("scripts.release.get_module_commits", return_value=[
                "fix(core): bug fix",
            ]):
                with self.assertRaises(SystemExit) as ctx:
                    release.release(
                        bump="auto", since="origin/main", dry_run=True, yes=True
                    )
                self.assertEqual(ctx.exception.code, 0)

    # --- Core version resolution for downstream modules ---

    @patch("scripts.release.console")
    def test_core_version_resolved_when_core_not_released(self, _mock_console):
        self._setup_all_modules({
            "terracotta-core": "0.8.0",
            "terracotta-provider-modrinth": "0.8.0",
        })
        with patch("scripts.release.get_module_last_tag") as mock_tag:
            mock_tag.side_effect = lambda m: f"{release.MODULE_INFO[m]['tag_prefix']}0.8.0"
            with patch("scripts.release.get_module_commits", return_value=[
                "feat(modrinth): new feature",
            ]):
                with self.assertRaises(SystemExit) as ctx:
                    release.release(
                        bump="auto",
                        modules="terracotta-provider-modrinth",
                        dry_run=True,
                        yes=True,
                    )
                self.assertEqual(ctx.exception.code, 0)


class TestAbortCommand(unittest.TestCase):
    """Tests for the abort command — verifies gh CLI is called via subprocess.run, not run_git."""

    @patch("scripts.release.console")
    @patch("scripts.release.subprocess")
    def test_abort_uses_subprocess_not_run_git(self, mock_subprocess, _mock_console):
        mock_subprocess.run.return_value = MagicMock(stdout="12345\n", returncode=0)
        with patch("scripts.release.run_command") as mock_run_command:
            with patch("scripts.release.questionary.confirm") as mock_confirm:
                mock_confirm.return_value.ask.return_value = True
                release.abort(run_id="12345", yes=True)
                mock_run_command.assert_called_once_with(["gh", "run", "cancel", "12345"])

    @patch("scripts.release.console")
    @patch("scripts.release.subprocess")
    def test_abort_no_active_run_exits(self, mock_subprocess, _mock_console):
        mock_subprocess.run.return_value = MagicMock(stdout="", returncode=0)
        with self.assertRaises(SystemExit) as ctx:
            release.abort(yes=True)
        self.assertEqual(ctx.exception.code, 0)


class TestRollbackDocsRestore(unittest.TestCase):
    """Tests that rollback only restores docs paths when gradle-plugin was released."""

    @patch("scripts.release.console")
    def test_rollback_restores_docs_when_gradle_plugin_released(self, _mock_console):
        module_versions = {"terracotta-core": "0.9.0", "terracotta-gradle-plugin": "0.9.0"}
        with patch("scripts.release.run_command") as mock_run:
            release._rollback_release(
                module_versions=module_versions,
                actions_taken=["files_modified"],
                branch="main",
            )
            restore_call = [c for c in mock_run.call_args_list if c.args[0][0] == "git" and c.args[0][1] == "restore"]
            self.assertTrue(len(restore_call) > 0)
            paths = restore_call[0].args[0][2:]
            self.assertIn("docs/index.md", paths)
            self.assertIn("docs/content", paths)

    @patch("scripts.release.console")
    def test_rollback_skips_docs_when_gradle_plugin_not_released(self, _mock_console):
        module_versions = {"terracotta-core": "0.9.0"}
        with patch("scripts.release.run_command") as mock_run:
            release._rollback_release(
                module_versions=module_versions,
                actions_taken=["files_modified"],
                branch="main",
            )
            restore_call = [c for c in mock_run.call_args_list if c.args[0][0] == "git" and c.args[0][1] == "restore"]
            self.assertTrue(len(restore_call) > 0)
            paths = restore_call[0].args[0][2:]
            self.assertNotIn("docs/index.md", paths)
            self.assertNotIn("docs/content", paths)
            self.assertIn("deployments.json", paths)


class TestTagFormatRegexSync(unittest.TestCase):
    """Verify that tags produced by release.py match the deploy-docs.yml regex.

    The deploy-docs.yml workflow uses `grep -E 'terracotta-.*-v[0-9]+\\.[0-9]+\\.[0-9]+'`
    to detect per-module version tags. This test ensures the tag_prefix values in
    MODULE_INFO always produce tags that match that pattern.
    """

    DEPLOY_DOCS_TAG_REGEX = re.compile(r"terracotta-.*-v[0-9]+\.[0-9]+\.[0-9]+")

    def test_all_module_tags_match_deploy_docs_regex(self):
        for module in release.PUBLISHABLE_MODULES:
            prefix = release.MODULE_INFO[module]["tag_prefix"]
            tag = f"{prefix}0.9.0"
            self.assertRegex(
                tag,
                self.DEPLOY_DOCS_TAG_REGEX,
                f"Tag '{tag}' for module '{module}' does not match deploy-docs.yml regex",
            )

    def test_old_monolithic_tag_does_not_match(self):
        self.assertNotRegex("v0.9.0", self.DEPLOY_DOCS_TAG_REGEX)


if __name__ == "__main__":
    unittest.main()

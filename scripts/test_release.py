import os
import unittest
from unittest.mock import patch, MagicMock, call
import subprocess
import tempfile
from pathlib import Path
import sys
import zipfile
from pathlib import Path as PathlibPath

# Add parent directory to path to import scripts module
sys.path.insert(0, str(PathlibPath(__file__).parent.parent))

# Import functions/app from release.py
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

    def test_invalid_current(self):
        with self.assertRaises(ValueError):
            release.bump_version("invalid", "patch")

    def test_invalid_bump_type(self):
        with self.assertRaises(ValueError):
            release.bump_version("1.2.3", "invalid")


class TestGetNextVersion(unittest.TestCase):

    def _mock_git(self, mock_run, tag_returncode, tag_stdout, log_stdout):
        """Helper to set up subprocess.run side effects for git describe then git log."""
        def side_effect(cmd, **kwargs):
            result = MagicMock()
            if "describe" in cmd:
                result.returncode = tag_returncode
                result.stdout = tag_stdout
            elif "log" in cmd:
                result.returncode = 0
                result.stdout = log_stdout
            return result
        mock_run.side_effect = side_effect

    @patch("scripts.release.subprocess.run")
    def test_feat_commit_suggests_minor(self, mock_run):
        self._mock_git(mock_run, 0, "v0.1.0\n", "feat(core): add new filter\nchore: update deps\n")
        bump, version = release.get_next_version("0.1.0")
        self.assertEqual(bump, "minor")
        self.assertEqual(version, "0.2.0")

    @patch("scripts.release.subprocess.run")
    def test_fix_commit_suggests_patch(self, mock_run):
        self._mock_git(mock_run, 0, "v0.1.0\n", "fix(cli): correct output format\n")
        bump, version = release.get_next_version("0.1.0")
        self.assertEqual(bump, "patch")
        self.assertEqual(version, "0.1.1")

    @patch("scripts.release.subprocess.run")
    def test_breaking_bang_in_zero_major_suggests_minor(self, mock_run):
        self._mock_git(mock_run, 0, "v0.1.0\n", "feat(core)!: remove deprecated API\nfeat: something\n")
        bump, version = release.get_next_version("0.1.0")
        self.assertEqual(bump, "minor")
        self.assertEqual(version, "0.2.0")

    @patch("scripts.release.subprocess.run")
    def test_breaking_change_prefix_in_zero_major_suggests_minor(self, mock_run):
        self._mock_git(mock_run, 0, "v0.1.0\n", "breaking change: removed X\n")
        bump, version = release.get_next_version("0.1.0")
        self.assertEqual(bump, "minor")
        self.assertEqual(version, "0.2.0")

    @patch("scripts.release.subprocess.run")
    def test_breaking_bang_at_major_one_or_higher_suggests_major(self, mock_run):
        self._mock_git(mock_run, 0, "v1.2.3\n", "feat(core)!: remove deprecated API\n")
        bump, version = release.get_next_version("1.2.3")
        self.assertEqual(bump, "major")
        self.assertEqual(version, "2.0.0")

    @patch("scripts.release.subprocess.run")
    def test_breaking_change_prefix_at_major_one_or_higher_suggests_major(self, mock_run):
        self._mock_git(mock_run, 0, "v1.2.3\n", "breaking change: removed X\n")
        bump, version = release.get_next_version("1.2.3")
        self.assertEqual(bump, "major")
        self.assertEqual(version, "2.0.0")

    @patch("scripts.release.subprocess.run")
    def test_no_tags_uses_full_history(self, mock_run):
        self._mock_git(mock_run, 1, "", "feat: initial feature\n")
        bump, version = release.get_next_version("0.1.0")
        self.assertEqual(bump, "minor")
        self.assertEqual(version, "0.2.0")

    @patch("scripts.release.subprocess.run")
    def test_no_commits_falls_back_to_patch(self, mock_run):
        self._mock_git(mock_run, 0, "v0.1.0\n", "")
        bump, version = release.get_next_version("0.1.0")
        self.assertEqual(bump, "patch")
        self.assertEqual(version, "0.1.1")

    @patch("scripts.release.subprocess.run")
    def test_git_failure_falls_back_to_patch(self, mock_run):
        mock_run.side_effect = Exception("git not found")
        bump, version = release.get_next_version("0.1.0")
        self.assertEqual(bump, "patch")
        self.assertEqual(version, "0.1.1")

    @patch("scripts.release.subprocess.run")
    def test_breaking_takes_precedence_over_feat_in_zero_major(self, mock_run):
        # A breaking change appears after a feat — it wins, but in 0.x it is downgraded to minor
        self._mock_git(mock_run, 0, "v0.1.0\n", "feat: new thing\nfeat(core)!: remove API\n")
        bump, _ = release.get_next_version("0.1.0")
        self.assertEqual(bump, "minor")

    @patch("scripts.release.subprocess.run")
    def test_breaking_takes_precedence_over_feat_at_major_one_or_higher(self, mock_run):
        self._mock_git(mock_run, 0, "v1.0.0\n", "feat: new thing\nfeat(core)!: remove API\n")
        bump, _ = release.get_next_version("1.0.0")
        self.assertEqual(bump, "major")

    @patch("scripts.release.subprocess.run")
    def test_scoped_feat_suggests_minor(self, mock_run):
        self._mock_git(mock_run, 0, "v0.1.0\n", "feat(modrinth): loader version filtering\n")
        bump, _ = release.get_next_version("0.1.0")
        self.assertEqual(bump, "minor")


class TestGetCurrentVersion(unittest.TestCase):

    @patch("scripts.release.Path.exists")
    @patch("scripts.release.Path.read_text")
    def test_reads_version(self, mock_read, mock_exists):
        mock_exists.return_value = True
        mock_read.return_value = "version = 0.3.0\nother_prop = true"
        self.assertEqual(release.get_current_version(), "0.3.0")

    @patch("scripts.release.Path.exists")
    @patch("scripts.release.Path.read_text")
    def test_missing_version_key(self, mock_read, mock_exists):
        mock_exists.return_value = True
        mock_read.return_value = "no version here"
        with self.assertRaises(ValueError):
            release.get_current_version()

    @patch("scripts.release.Path.exists")
    def test_missing_file(self, mock_exists):
        mock_exists.return_value = False
        with self.assertRaises(FileNotFoundError):
            release.get_current_version()


class TestUpdateFiles(unittest.TestCase):

    @patch("scripts.release.Path.read_text")
    @patch("scripts.release.Path.write_text")
    def test_update_gradle_properties(self, mock_write, mock_read):
        mock_read.return_value = "version = 0.3.0\n"
        release.update_gradle_properties("0.4.0")
        mock_write.assert_called_once_with("version = 0.4.0\n")

    @patch("scripts.release.Path.read_text")
    @patch("scripts.release.Path.write_text")
    def test_update_pyproject_toml(self, mock_write, mock_read):
        mock_read.return_value = '[project]\nversion = "0.3.0"\n'
        release.update_pyproject_toml("0.4.0")
        mock_write.assert_called_once_with('[project]\nversion = "0.4.0"\n')

    @patch("scripts.release.Path.read_text")
    @patch("scripts.release.Path.write_text")
    @patch("scripts.release.date")
    def test_update_changelog(self, mock_date, mock_write, mock_read):
        mock_date.today.return_value.isoformat.return_value = "2026-07-07"
        mock_read.return_value = "## [Unreleased]\n\nAdds feature X.\n\n### Added\n- Feature"
        release.update_changelog("0.4.0")
        mock_write.assert_called_once_with(
            "## [Unreleased]\n\n## [0.4.0] - 2026-07-07\n\nAdds feature X.\n\n### Added\n- Feature"
        )

    @patch("scripts.release.Path.read_text")
    @patch("scripts.release.Path.write_text")
    @patch("scripts.release.date")
    def test_update_changelog_missing_summary(self, mock_date, mock_write, mock_read):
        mock_date.today.return_value.isoformat.return_value = "2026-07-07"
        mock_read.return_value = "## [Unreleased]\n\n### Added\n- Feature"
        with self.assertRaises(ValueError):
            release.update_changelog("0.4.0")

    @patch("scripts.release.Path.read_text")
    @patch("scripts.release.Path.write_text")
    @patch("scripts.release.date")
    def test_update_changelog_missing_unreleased(self, mock_date, mock_write, mock_read):
        mock_date.today.return_value.isoformat.return_value = "2026-07-07"
        mock_read.return_value = "## [0.3.0]\nSome content"
        with self.assertRaises(ValueError):
            release.update_changelog("0.4.0")

    @patch("scripts.release.Path.read_text")
    @patch("scripts.release.Path.write_text")
    @patch("scripts.release.date")
    def test_update_changelog_rejects_this_release_wording(self, mock_date, mock_write, mock_read):
        mock_date.today.return_value.isoformat.return_value = "2026-07-07"
        mock_read.return_value = (
            "## [Unreleased]\n\n"
            "This release adds feature X.\n\n"
            "### Added\n- Feature"
        )
        with self.assertRaises(ValueError):
            release.update_changelog("0.4.0")

    @patch("scripts.release.Path.read_text")
    @patch("scripts.release.Path.write_text")
    @patch("scripts.release.date")
    def test_update_changelog_rejects_unreleased_wording(self, mock_date, mock_write, mock_read):
        mock_date.today.return_value.isoformat.return_value = "2026-07-07"
        mock_read.return_value = (
            "## [Unreleased]\n\n"
            "This unreleased set of changes narrows Hangar license handling.\n\n"
            "### Added\n- Feature"
        )
        with self.assertRaises(ValueError):
            release.update_changelog("0.4.0")

    @patch("scripts.release.Path.read_text")
    @patch("scripts.release.Path.write_text")
    @patch("scripts.release.date")
    def test_update_changelog_allows_legitimate_unreleased_mention(self, mock_date, mock_write, mock_read):
        mock_date.today.return_value.isoformat.return_value = "2026-07-07"
        mock_read.return_value = (
            "## [Unreleased]\n\n"
            "Fixes an unreleased state bug.\n\n"
            "### Added\n- Feature"
        )
        release.update_changelog("0.4.0")
        mock_write.assert_called_once_with(
            "## [Unreleased]\n\n"
            "## [0.4.0] - 2026-07-07\n\n"
            "Fixes an unreleased state bug.\n\n"
            "### Added\n- Feature"
        )


class TestExtractReleaseNotes(unittest.TestCase):

    @patch("scripts.release.Path")
    def test_prints_body_without_heading(self, mock_path):
        mock_instance = MagicMock()
        mock_instance.exists.return_value = True
        mock_instance.read_text.return_value = (
            "## [Unreleased]\n\n### Added\n- Feature\n\n"
            "## [0.4.0] - 2026-07-07\n\n### Fixed\n- Bug fix\n\n"
            "## [0.3.0] - 2026-07-06\n- Old\n"
        )
        mock_path.return_value = mock_instance
        with patch("builtins.print") as mock_print:
            release.extract_release_notes("0.4.0")
        mock_print.assert_called_once_with("### Fixed\n- Bug fix")

    @patch("scripts.release.Path")
    def test_writes_to_output_file(self, mock_path):
        changelog_mock = MagicMock()
        changelog_mock.exists.return_value = True
        changelog_mock.read_text.return_value = (
            "## [0.4.0] - 2026-07-07\n\n### Fixed\n- Bug fix\n"
        )
        output_mock = MagicMock()
        def path_side_effect(p):
            return changelog_mock if p == "CHANGELOG.md" else output_mock
        mock_path.side_effect = path_side_effect
        release.extract_release_notes("0.4.0", output="RELEASE_NOTES.md")
        output_mock.write_text.assert_called_once_with("### Fixed\n- Bug fix")

    @patch("scripts.release.Path")
    def test_missing_section_raises(self, mock_path):
        mock_instance = MagicMock()
        mock_instance.exists.return_value = True
        mock_instance.read_text.return_value = "## [Unreleased]\n- Feature"
        mock_path.return_value = mock_instance
        with self.assertRaises(ValueError):
            release.extract_release_notes("0.4.0")

    @patch("scripts.release.Path")
    def test_empty_section_raises(self, mock_path):
        mock_instance = MagicMock()
        mock_instance.exists.return_value = True
        mock_instance.read_text.return_value = (
            "## [0.4.0] - 2026-07-07\n\n## [0.3.0]\n- Old"
        )
        mock_path.return_value = mock_instance
        with self.assertRaises(ValueError):
            release.extract_release_notes("0.4.0")

    @patch("scripts.release.Path")
    def test_missing_changelog_raises(self, mock_path):
        mock_instance = MagicMock()
        mock_instance.exists.return_value = False
        mock_path.return_value = mock_instance
        with self.assertRaises(FileNotFoundError):
            release.extract_release_notes("0.4.0")


class TestUpdateReadme(unittest.TestCase):

    @patch("scripts.release.Path.exists")
    @patch("scripts.release.Path.read_text")
    @patch("scripts.release.Path.write_text")
    def test_updates_version_string(self, mock_write, mock_read, mock_exists):
        mock_exists.return_value = True
        mock_read.return_value = 'plugins {\n    id("io.github.beduality.terracotta") version "0.3.0"\n}'
        release.update_readme("0.4.0")
        mock_write.assert_called_once_with(
            'plugins {\n    id("io.github.beduality.terracotta") version "0.4.0"\n}'
        )

    @patch("scripts.release.Path.exists")
    @patch("scripts.release.Path.write_text")
    def test_skips_when_file_missing(self, mock_write, mock_exists):
        mock_exists.return_value = False
        release.update_readme("0.4.0")
        mock_write.assert_not_called()

    @patch("scripts.release.Path.exists")
    @patch("scripts.release.Path.read_text")
    @patch("scripts.release.Path.write_text")
    def test_no_change_when_version_not_found(self, mock_write, mock_read, mock_exists):
        mock_exists.return_value = True
        content = "No version placeholder here\n"
        mock_read.return_value = content
        release.update_readme("0.4.0")
        mock_write.assert_not_called()


class TestValidateReadmeVersion(unittest.TestCase):

    @patch("scripts.release.Path.exists")
    @patch("scripts.release.Path.read_text")
    def test_passes_when_version_present(self, mock_read, mock_exists):
        mock_exists.return_value = True
        mock_read.return_value = 'id("io.github.beduality.terracotta") version "0.4.0"'
        release.validate_readme_version("0.4.0")

    @patch("scripts.release.Path.exists")
    @patch("scripts.release.Path.read_text")
    def test_raises_when_version_missing(self, mock_read, mock_exists):
        mock_exists.return_value = True
        mock_read.return_value = 'id("io.github.beduality.terracotta") version "0.3.0"'
        with self.assertRaises(ValueError):
            release.validate_readme_version("0.4.0")

    @patch("scripts.release.Path.exists")
    def test_skips_when_file_missing(self, mock_exists):
        mock_exists.return_value = False
        release.validate_readme_version("0.4.0")


class TestValidateChangelogReleaseSection(unittest.TestCase):

    @patch("scripts.release.Path.read_text")
    def test_passes_when_section_has_content(self, mock_read):
        mock_read.return_value = "## [Unreleased]\n\n## [0.4.0] - 2026-07-07\n\n### Fixed\n- Bug fix\n"
        release.validate_changelog_release_section("0.4.0")

    @patch("scripts.release.Path.read_text")
    def test_raises_when_section_missing(self, mock_read):
        mock_read.return_value = "## [Unreleased]\n- Feature\n"
        with self.assertRaises(ValueError):
            release.validate_changelog_release_section("0.4.0")

    @patch("scripts.release.Path.read_text")
    def test_raises_when_section_empty(self, mock_read):
        mock_read.return_value = "## [0.4.0] - 2026-07-07\n\n## [0.3.0] - 2026-07-06\n- Old\n"
        with self.assertRaises(ValueError):
            release.validate_changelog_release_section("0.4.0")


class TestUpdateDocsVersionSnippets(unittest.TestCase):

    def test_updates_matching_files(self):
        with tempfile.TemporaryDirectory() as tmp:
            cwd = os.getcwd()
            os.chdir(tmp)
            try:
                docs = Path("docs/content")
                docs.mkdir(parents=True)
                snippet = docs / "setup.md"
                snippet.write_text(
                    'plugins {\n    id("io.github.beduality.terracotta") version "0.3.0"\n}'
                )
                unchanged = docs / "other.md"
                unchanged.write_text("No version here\n")

                release.update_docs_version_snippets("0.4.0")

                self.assertIn('version "0.4.0"', snippet.read_text())
                self.assertNotIn('version "0.3.0"', snippet.read_text())
                self.assertEqual(unchanged.read_text(), "No version here\n")
            finally:
                os.chdir(cwd)

    def test_no_files_is_fine(self):
        with tempfile.TemporaryDirectory() as tmp:
            cwd = os.getcwd()
            os.chdir(tmp)
            try:
                Path("docs/content").mkdir(parents=True)
                release.update_docs_version_snippets("0.4.0")
            finally:
                os.chdir(cwd)


class TestValidateDocsVersionSnippets(unittest.TestCase):

    def test_passes_when_all_snippets_match(self):
        with tempfile.TemporaryDirectory() as tmp:
            cwd = os.getcwd()
            os.chdir(tmp)
            try:
                docs = Path("docs/content")
                docs.mkdir(parents=True)
                (docs / "setup.md").write_text(
                    'id("io.github.beduality.terracotta") version "0.4.0"'
                )
                release.validate_docs_version_snippets("0.4.0")
            finally:
                os.chdir(cwd)

    def test_raises_when_snippet_mismatches(self):
        with tempfile.TemporaryDirectory() as tmp:
            cwd = os.getcwd()
            os.chdir(tmp)
            try:
                docs = Path("docs/content")
                docs.mkdir(parents=True)
                (docs / "setup.md").write_text(
                    'id("io.github.beduality.terracotta") version "0.3.0"'
                )
                with self.assertRaises(ValueError):
                    release.validate_docs_version_snippets("0.4.0")
            finally:
                os.chdir(cwd)


class TestValidateJavadocJars(unittest.TestCase):

    def _prepare_jars(
        self, tmp: str, version: str, content: dict[str, bytes] | None = None
    ):
        for module in [
            "terracotta-core",
            "terracotta-gradle-plugin",
            "terracotta-provider-modrinth",
        ]:
            jar_dir = Path(tmp) / f"modules/{module}/build/libs"
            jar_dir.mkdir(parents=True)
            jar = jar_dir / f"{module}-{version}-javadoc.jar"
            if content:
                with zipfile.ZipFile(jar, "w") as zf:
                    for name, data in content.items():
                        zf.writestr(name, data)

    def test_passes_for_valid_jars(self):
        with tempfile.TemporaryDirectory() as tmp:
            cwd = os.getcwd()
            os.chdir(tmp)
            try:
                html = b"<html>" + b"x" * 2048 + b"</html>"
                self._prepare_jars(tmp, "0.4.0", {"index.html": html})
                release.validate_javadoc_jars("0.4.0")
            finally:
                os.chdir(cwd)

    def test_raises_when_jar_missing(self):
        with tempfile.TemporaryDirectory() as tmp:
            cwd = os.getcwd()
            os.chdir(tmp)
            try:
                with self.assertRaises(FileNotFoundError):
                    release.validate_javadoc_jars("0.4.0")
            finally:
                os.chdir(cwd)

    def test_raises_when_jar_too_small(self):
        with tempfile.TemporaryDirectory() as tmp:
            cwd = os.getcwd()
            os.chdir(tmp)
            try:
                for module in ["terracotta-core", "terracotta-gradle-plugin", "terracotta-provider-modrinth"]:
                    jar_dir = Path(tmp) / f"modules/{module}/build/libs"
                    jar_dir.mkdir(parents=True)
                    (jar_dir / f"{module}-0.4.0-javadoc.jar").write_bytes(b"tiny")
                with self.assertRaises(ValueError) as ctx:
                    release.validate_javadoc_jars("0.4.0")
                self.assertIn("Javadoc JARs are empty", str(ctx.exception))
            finally:
                os.chdir(cwd)

    def test_raises_when_jar_only_manifest(self):
        with tempfile.TemporaryDirectory() as tmp:
            cwd = os.getcwd()
            os.chdir(tmp)
            try:
                manifest = b"Manifest-Version: 1.0\n" + b"# padding\n" * 200
                self._prepare_jars(tmp, "0.4.0", {"META-INF/MANIFEST.MF": manifest})
                with self.assertRaises(ValueError) as ctx:
                    release.validate_javadoc_jars("0.4.0")
                self.assertIn("only manifest", str(ctx.exception))
            finally:
                os.chdir(cwd)


class TestRunCommand(unittest.TestCase):

    @patch("scripts.release.subprocess.run")
    def test_success(self, mock_run):
        mock_run.return_value.returncode = 0
        release.run_command(["git", "status"])
        mock_run.assert_called_once_with(["git", "status"], env=None)

    @patch("scripts.release.subprocess.run")
    def test_failure_raises(self, mock_run):
        mock_run.return_value.returncode = 1
        with self.assertRaises(subprocess.CalledProcessError):
            release.run_command(["git", "status"])


class TestRollback(unittest.TestCase):

    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.subprocess.run")
    def test_rollback_deletes_tags_and_resets_commit(self, mock_run, mock_confirm):
        mock_confirm_obj = MagicMock()
        mock_confirm_obj.ask.side_effect = [True, True, True]
        mock_confirm.return_value = mock_confirm_obj

        mock_run.return_value = MagicMock(returncode=0, stdout="chore: release version 0.4.0\n")

        release.rollback("0.4.0")

        run_calls = [call[0][0] for call in mock_run.call_args_list]
        self.assertTrue(any(any("v0.4.0" in arg for arg in cmd) for cmd in run_calls if isinstance(cmd, list)))

    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.subprocess.run")
    def test_rollback_skips_reset_when_commit_differs(self, mock_run, mock_confirm):
        mock_confirm_obj = MagicMock()
        mock_confirm_obj.ask.side_effect = [True, True]
        mock_confirm.return_value = mock_confirm_obj

        # Last commit is not the release commit
        mock_run.return_value = MagicMock(returncode=0, stdout="feat: something else\n")

        release.rollback("0.4.0")

        run_calls = [call[0][0] for call in mock_run.call_args_list]
        self.assertFalse(any("reset" in cmd for cmd in run_calls if isinstance(cmd, list)))

    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.subprocess.run")
    def test_rollback_remote_tag_failure_continues(self, mock_run, mock_confirm):
        mock_confirm_obj = MagicMock()
        mock_confirm_obj.ask.side_effect = [True, True, True]
        mock_confirm.return_value = mock_confirm_obj

        def run_side_effect(cmd, **kwargs):
            result = MagicMock()
            if "push" in cmd and "--delete" in cmd:
                raise Exception("remote tag not found")
            result.returncode = 0
            result.stdout = "chore: release version 0.4.0\n"
            return result

        mock_run.side_effect = run_side_effect

        release.rollback("0.4.0")

        # Should continue to delete local tag despite remote failure
        run_calls = [call[0][0] for call in mock_run.call_args_list]
        self.assertTrue(any("tag" in cmd and "-d" in cmd for cmd in run_calls if isinstance(cmd, list)))

    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.subprocess.run")
    def test_rollback_local_tag_failure_continues(self, mock_run, mock_confirm):
        mock_confirm_obj = MagicMock()
        mock_confirm_obj.ask.side_effect = [True, True, True]
        mock_confirm.return_value = mock_confirm_obj

        def run_side_effect(cmd, **kwargs):
            result = MagicMock()
            if "tag" in cmd and "-d" in cmd:
                raise Exception("local tag not found")
            result.returncode = 0
            result.stdout = "chore: release version 0.4.0\n"
            return result

        mock_run.side_effect = run_side_effect

        release.rollback("0.4.0")

        # Should still attempt to check for release commit
        run_calls = [call[0][0] for call in mock_run.call_args_list]
        self.assertTrue(any("log" in cmd for cmd in run_calls if isinstance(cmd, list)))

    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.subprocess.run")
    def test_rollback_commit_reset_failure_continues(self, mock_run, mock_confirm):
        mock_confirm_obj = MagicMock()
        mock_confirm_obj.ask.side_effect = [True, True, True]
        mock_confirm.return_value = mock_confirm_obj

        def run_side_effect(cmd, **kwargs):
            result = MagicMock()
            if "reset" in cmd:
                raise Exception("reset failed")
            result.returncode = 0
            result.stdout = "chore: release version 0.4.0\n"
            return result

        mock_run.side_effect = run_side_effect

        release.rollback("0.4.0")

        # Should complete without crashing despite reset failure
        self.assertTrue(True)

    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.subprocess.run")
    def test_rollback_user_declines_remote_delete(self, mock_run, mock_confirm):
        mock_confirm_obj = MagicMock()
        mock_confirm_obj.ask.side_effect = [False, True, True]
        mock_confirm.return_value = mock_confirm_obj

        mock_run.return_value = MagicMock(returncode=0, stdout="chore: release version 0.4.0\n")

        release.rollback("0.4.0")

        run_calls = [call[0][0] for call in mock_run.call_args_list]
        # Should not attempt to delete remote tag
        self.assertFalse(any("push" in cmd and "--delete" in cmd for cmd in run_calls if isinstance(cmd, list)))

    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.subprocess.run")
    def test_rollback_user_declines_local_delete(self, mock_run, mock_confirm):
        mock_confirm_obj = MagicMock()
        mock_confirm_obj.ask.side_effect = [True, False, True]
        mock_confirm.return_value = mock_confirm_obj

        mock_run.return_value = MagicMock(returncode=0, stdout="chore: release version 0.4.0\n")

        release.rollback("0.4.0")

        run_calls = [call[0][0] for call in mock_run.call_args_list]
        # Should not attempt to delete local tag
        self.assertFalse(any("tag" in cmd and "-d" in cmd for cmd in run_calls if isinstance(cmd, list)))

    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.subprocess.run")
    def test_rollback_user_declines_reset(self, mock_run, mock_confirm):
        mock_confirm_obj = MagicMock()
        mock_confirm_obj.ask.side_effect = [True, True, False]
        mock_confirm.return_value = mock_confirm_obj

        mock_run.return_value = MagicMock(returncode=0, stdout="chore: release version 0.4.0\n")

        release.rollback("0.4.0")

        run_calls = [call[0][0] for call in mock_run.call_args_list]
        # Should not attempt to reset
        self.assertFalse(any("reset" in cmd for cmd in run_calls if isinstance(cmd, list)))


class TestMainWizardFlow(unittest.TestCase):

    def setUp(self):
        self.patches = [
            patch("scripts.release.validate_readme_version"),
            patch("scripts.release.validate_docs_version_snippets"),
            patch("scripts.release.validate_changelog_release_section"),
            patch("scripts.release.validate_javadoc_jars"),
        ]
        for p in self.patches:
            p.start()

    def tearDown(self):
        for p in self.patches:
            p.stop()

    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.questionary.select")
    @patch("scripts.release.get_next_version")
    @patch("scripts.release.get_current_version")
    @patch("scripts.release.update_gradle_properties")
    @patch("scripts.release.update_pyproject_toml")
    @patch("scripts.release.update_changelog")
    @patch("scripts.release.run_command")
    def test_auto_choice_uses_detected_bump(self, mock_run_cmd, mock_up_changelog,
                                             mock_up_pyproject, mock_up_gradle,
                                             mock_get_ver, mock_get_next,
                                             mock_select, mock_confirm):
        mock_get_ver.return_value = "0.3.0"
        mock_get_next.return_value = ("minor", "0.4.0")

        mock_select.return_value = MagicMock(ask=MagicMock(return_value="auto"))
        mock_confirm.return_value = MagicMock(ask=MagicMock(side_effect=[True, True, True]))

        release.main(bump=None)

        mock_up_gradle.assert_called_once_with("0.4.0")
        mock_up_pyproject.assert_called_once_with("0.4.0")
        mock_up_changelog.assert_called_once_with("0.4.0")

    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.questionary.select")
    @patch("scripts.release.get_next_version")
    @patch("scripts.release.get_current_version")
    @patch("scripts.release.update_gradle_properties")
    @patch("scripts.release.update_pyproject_toml")
    @patch("scripts.release.update_changelog")
    @patch("scripts.release.run_command")
    def test_manual_choice_overrides_auto(self, mock_run_cmd, mock_up_changelog,
                                           mock_up_pyproject, mock_up_gradle,
                                           mock_get_ver, mock_get_next,
                                           mock_select, mock_confirm):
        mock_get_ver.return_value = "0.3.0"
        mock_get_next.return_value = ("patch", "0.3.1")  # auto suggests patch

        mock_select.return_value = MagicMock(ask=MagicMock(return_value="minor"))  # user picks minor
        mock_confirm.return_value = MagicMock(ask=MagicMock(side_effect=[True, True, True]))

        release.main(bump=None)

        # Should have bumped to minor (0.4.0), not patch (0.3.1)
        mock_up_gradle.assert_called_once_with("0.4.0")

    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.questionary.select")
    @patch("scripts.release.questionary.text")
    @patch("scripts.release.get_next_version")
    @patch("scripts.release.get_current_version")
    @patch("scripts.release.update_gradle_properties")
    @patch("scripts.release.update_pyproject_toml")
    @patch("scripts.release.update_changelog")
    @patch("scripts.release.run_command")
    def test_custom_version_input(self, mock_run_cmd, mock_up_changelog,
                                    mock_up_pyproject, mock_up_gradle,
                                    mock_get_ver, mock_get_next,
                                    mock_text, mock_select, mock_confirm):
        mock_get_ver.return_value = "0.3.0"
        mock_get_next.return_value = ("patch", "0.3.1")
        mock_select.return_value = MagicMock(ask=MagicMock(return_value="custom"))
        mock_text.return_value = MagicMock(ask=MagicMock(return_value="1.0.0"))
        mock_confirm.return_value = MagicMock(ask=MagicMock(side_effect=[True, True, True]))

        release.main(bump=None)

        mock_up_gradle.assert_called_once_with("1.0.0")
        mock_up_pyproject.assert_called_once_with("1.0.0")
        mock_up_changelog.assert_called_once_with("1.0.0")

    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.questionary.select")
    @patch("scripts.release.questionary.text")
    @patch("scripts.release.get_next_version")
    @patch("scripts.release.get_current_version")
    @patch("scripts.release.update_gradle_properties")
    @patch("scripts.release.update_pyproject_toml")
    @patch("scripts.release.update_changelog")
    @patch("scripts.release.run_command")
    def test_custom_version_invalid_format(self, mock_run_cmd, mock_up_changelog,
                                            mock_up_pyproject, mock_up_gradle,
                                            mock_get_ver, mock_get_next,
                                            mock_text, mock_select, mock_confirm):
        mock_get_ver.return_value = "0.3.0"
        mock_get_next.return_value = ("patch", "0.3.1")
        mock_select.return_value = MagicMock(ask=MagicMock(return_value="custom"))
        mock_text.return_value = MagicMock(ask=MagicMock(return_value="invalid"))

        with self.assertRaises(SystemExit):
            release.main(bump=None)

        mock_up_gradle.assert_not_called()

    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.questionary.select")
    @patch("scripts.release.get_next_version")
    @patch("scripts.release.get_current_version")
    @patch("scripts.release.update_gradle_properties")
    @patch("scripts.release.update_pyproject_toml")
    @patch("scripts.release.update_changelog")
    @patch("scripts.release.run_command")
    def test_abort_on_select_ctrl_c(self, mock_run_cmd, mock_up_changelog,
                                     mock_up_pyproject, mock_up_gradle,
                                     mock_get_ver, mock_get_next,
                                     mock_select, mock_confirm):
        mock_get_ver.return_value = "0.3.0"
        mock_get_next.return_value = ("minor", "0.4.0")
        mock_select.return_value = MagicMock(ask=MagicMock(return_value=None))

        with self.assertRaises(SystemExit):
            release.main(bump=None)

        mock_up_gradle.assert_not_called()

    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.questionary.select")
    @patch("scripts.release.questionary.text")
    @patch("scripts.release.get_next_version")
    @patch("scripts.release.get_current_version")
    @patch("scripts.release.update_gradle_properties")
    @patch("scripts.release.update_pyproject_toml")
    @patch("scripts.release.update_changelog")
    @patch("scripts.release.run_command")
    def test_abort_on_custom_version_ctrl_c(self, mock_run_cmd, mock_up_changelog,
                                            mock_up_pyproject, mock_up_gradle,
                                            mock_get_ver, mock_get_next,
                                            mock_text, mock_select, mock_confirm):
        mock_get_ver.return_value = "0.3.0"
        mock_get_next.return_value = ("patch", "0.3.1")
        mock_select.return_value = MagicMock(ask=MagicMock(return_value="custom"))
        mock_text.return_value = MagicMock(ask=MagicMock(return_value=None))

        with self.assertRaises(SystemExit):
            release.main(bump=None)

        mock_up_gradle.assert_not_called()

    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.questionary.select")
    @patch("scripts.release.get_next_version")
    @patch("scripts.release.get_current_version")
    @patch("scripts.release.update_gradle_properties")
    @patch("scripts.release.update_pyproject_toml")
    @patch("scripts.release.update_changelog")
    @patch("scripts.release.run_command")
    def test_abort_on_proceed_no(self, mock_run_cmd, mock_up_changelog,
                                  mock_up_pyproject, mock_up_gradle,
                                  mock_get_ver, mock_get_next,
                                  mock_select, mock_confirm):
        mock_get_ver.return_value = "0.3.0"
        mock_get_next.return_value = ("minor", "0.4.0")
        mock_select.return_value = MagicMock(ask=MagicMock(return_value="auto"))
        mock_confirm.return_value = MagicMock(ask=MagicMock(return_value=False))

        with self.assertRaises(SystemExit):
            release.main(bump=None)

        mock_up_gradle.assert_not_called()


class TestMainCliFlow(unittest.TestCase):

    def setUp(self):
        self.patches = [
            patch("scripts.release.validate_readme_version"),
            patch("scripts.release.validate_docs_version_snippets"),
            patch("scripts.release.validate_changelog_release_section"),
            patch("scripts.release.validate_javadoc_jars"),
        ]
        for p in self.patches:
            p.start()

    def tearDown(self):
        for p in self.patches:
            p.stop()

    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.get_current_version")
    @patch("scripts.release.update_gradle_properties")
    @patch("scripts.release.update_pyproject_toml")
    @patch("scripts.release.update_changelog")
    @patch("scripts.release.run_command")
    @patch("scripts.release.subprocess.run")
    def test_rollback_on_commit_failure(self, mock_sub_run, mock_run_cmd, mock_up_changelog,
                                         mock_up_pyproject, mock_up_gradle,
                                         mock_get_ver, mock_confirm):
        mock_get_ver.return_value = "0.3.0"
        mock_confirm.return_value = MagicMock(ask=MagicMock(side_effect=[True, True]))

        def run_cmd_side_effect(cmd, env=None):
            if "commit" in cmd:
                raise subprocess.CalledProcessError(1, cmd)

        mock_run_cmd.side_effect = run_cmd_side_effect

        with self.assertRaises(SystemExit):
            release.main(bump="patch")

        rollback_calls = [c[0][0] for c in mock_sub_run.call_args_list]
        # Should attempt to restore files since committed failed but files were modified
        self.assertTrue(any("restore" in c for c in rollback_calls))

    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.get_current_version")
    @patch("scripts.release.update_gradle_properties")
    @patch("scripts.release.update_pyproject_toml")
    @patch("scripts.release.update_changelog")
    @patch("scripts.release.run_command")
    def test_cli_bump_skips_wizard(self, mock_run_cmd, mock_up_changelog,
                                    mock_up_pyproject, mock_up_gradle,
                                    mock_get_ver, mock_confirm):
        mock_get_ver.return_value = "0.3.0"
        mock_confirm.return_value = MagicMock(ask=MagicMock(side_effect=[True, True]))

        release.main(bump="patch")

        mock_up_gradle.assert_called_once_with("0.3.1")
        mock_up_pyproject.assert_called_once_with("0.3.1")
        mock_up_changelog.assert_called_once_with("0.3.1")

    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.get_current_version")
    def test_cli_invalid_bump_type_exits(self, mock_get_ver, mock_confirm):
        mock_get_ver.return_value = "0.3.0"
        mock_confirm.return_value = MagicMock(ask=MagicMock(return_value=True))

        with self.assertRaises(SystemExit):
            release.main(bump="invalid")

    @patch("scripts.release.get_current_version")
    def test_cli_version_read_error_exits(self, mock_get_ver):
        mock_get_ver.side_effect = FileNotFoundError("gradle.properties not found")

        with self.assertRaises(SystemExit):
            release.main(bump="patch")

    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.get_current_version")
    @patch("scripts.release.update_gradle_properties")
    @patch("scripts.release.update_pyproject_toml")
    @patch("scripts.release.update_changelog")
    @patch("scripts.release.run_command")
    def test_cli_dry_run_false_skips_verification(self, mock_run_cmd, mock_up_changelog,
                                                   mock_up_pyproject, mock_up_gradle,
                                                   mock_get_ver, mock_confirm):
        mock_get_ver.return_value = "0.3.0"
        # When dry_run=False, still need confirm for proceed and commit steps
        mock_confirm.return_value = MagicMock(ask=MagicMock(side_effect=[True, True]))

        release.main(bump="patch", dry_run=False)

        # Should not call gradlew for dry-run verification
        run_calls = [c[0][0] for c in mock_run_cmd.call_args_list]
        self.assertFalse(any("gradlew" in cmd for cmd in run_calls if isinstance(cmd, list)))

    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.get_current_version")
    @patch("scripts.release.update_gradle_properties")
    @patch("scripts.release.update_pyproject_toml")
    @patch("scripts.release.update_changelog")
    @patch("scripts.release.run_command")
    def test_cli_push_false_skips_git_operations(self, mock_run_cmd, mock_up_changelog,
                                                  mock_up_pyproject, mock_up_gradle,
                                                  mock_get_ver, mock_confirm):
        mock_get_ver.return_value = "0.3.0"
        mock_confirm.return_value = MagicMock(ask=MagicMock(return_value=True))

        release.main(bump="patch", push=False)

        # Should not call git add, commit, tag, or push
        run_calls = [c[0][0] for c in mock_run_cmd.call_args_list]
        self.assertFalse(any("git" in cmd for cmd in run_calls if isinstance(cmd, list)))

    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.get_current_version")
    @patch("scripts.release.update_gradle_properties")
    @patch("scripts.release.update_pyproject_toml")
    @patch("scripts.release.update_changelog")
    @patch("scripts.release.run_command")
    @patch("scripts.release.subprocess.run")
    def test_rollback_on_file_update_failure(self, mock_sub_run, mock_run_cmd, mock_up_changelog,
                                              mock_up_pyproject, mock_up_gradle,
                                              mock_get_ver, mock_confirm):
        mock_get_ver.return_value = "0.3.0"
        mock_confirm.return_value = MagicMock(ask=MagicMock(return_value=True))
        # File update happens after actions_taken is updated, so rollback should occur
        # Use OSError which is caught by the specific exception handling
        mock_up_gradle.side_effect = OSError("Failed to update gradle.properties")

        with self.assertRaises(SystemExit):
            release.main(bump="patch")

        # Should attempt to restore files since files_modified was added to actions_taken
        rollback_calls = [c[0][0] for c in mock_sub_run.call_args_list]
        self.assertTrue(any("restore" in c for c in rollback_calls))

    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.get_current_version")
    @patch("scripts.release.update_gradle_properties")
    @patch("scripts.release.update_pyproject_toml")
    @patch("scripts.release.update_changelog")
    @patch("scripts.release.run_command")
    @patch("scripts.release.subprocess.run")
    def test_rollback_on_uv_lock_failure(self, mock_sub_run, mock_run_cmd, mock_up_changelog,
                                          mock_up_pyproject, mock_up_gradle,
                                          mock_get_ver, mock_confirm):
        mock_get_ver.return_value = "0.3.0"
        mock_confirm.return_value = MagicMock(ask=MagicMock(side_effect=[True, True]))

        def run_cmd_side_effect(cmd, env=None):
            if "uv" in cmd and "lock" in cmd:
                raise subprocess.CalledProcessError(1, cmd)

        mock_run_cmd.side_effect = run_cmd_side_effect

        with self.assertRaises(SystemExit):
            release.main(bump="patch")

        # Should restore modified files since they were updated but not committed
        rollback_calls = [c[0][0] for c in mock_sub_run.call_args_list]
        self.assertTrue(any("restore" in c for c in rollback_calls))


    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.get_current_version")
    @patch("scripts.release.update_gradle_properties")
    @patch("scripts.release.update_pyproject_toml")
    @patch("scripts.release.update_changelog")
    @patch("scripts.release.run_command")
    @patch("scripts.release.subprocess.run")
    def test_partial_rollback_committed_but_not_tagged(self, mock_sub_run, mock_run_cmd, mock_up_changelog,
                                                         mock_up_pyproject, mock_up_gradle,
                                                         mock_get_ver, mock_confirm):
        mock_get_ver.return_value = "0.3.0"
        mock_confirm.return_value = MagicMock(ask=MagicMock(side_effect=[True, True]))

        def run_cmd_side_effect(cmd, env=None):
            if "tag" in cmd:
                raise subprocess.CalledProcessError(1, cmd)

        mock_run_cmd.side_effect = run_cmd_side_effect

        with self.assertRaises(SystemExit):
            release.main(bump="patch")

        # Should attempt to reset commit (committed) but not delete tags (not tagged/pushed)
        rollback_calls = [c[0][0] for c in mock_sub_run.call_args_list]
        has_reset = any("reset" in c for c in rollback_calls if isinstance(c, list))
        has_remote_delete = any("push" in c and "--delete" in c for c in rollback_calls if isinstance(c, list))
        has_local_delete = any("tag" in c and "-d" in c for c in rollback_calls if isinstance(c, list))
        self.assertTrue(has_reset)
        self.assertFalse(has_remote_delete)
        self.assertFalse(has_local_delete)


class TestPromptBump(unittest.TestCase):

    @patch("scripts.release.get_next_version")
    def test_yes_returns_auto(self, mock_get_next):
        mock_get_next.return_value = ("minor", "0.4.0")
        strategy, custom = release.prompt_bump("0.3.0", yes=True)
        self.assertEqual(strategy, "auto")
        self.assertIsNone(custom)

    @patch("scripts.release.questionary.select")
    @patch("scripts.release.get_next_version")
    def test_auto_choice_returns_auto(self, mock_get_next, mock_select):
        mock_get_next.return_value = ("minor", "0.4.0")
        mock_select.return_value = MagicMock(ask=MagicMock(return_value="auto"))
        strategy, custom = release.prompt_bump("0.3.0")
        self.assertEqual(strategy, "auto")
        self.assertIsNone(custom)

    @patch("scripts.release.questionary.select")
    @patch("scripts.release.get_next_version")
    def test_patch_choice_returns_patch(self, mock_get_next, mock_select):
        mock_get_next.return_value = ("patch", "0.3.1")
        mock_select.return_value = MagicMock(ask=MagicMock(return_value="patch"))
        strategy, custom = release.prompt_bump("0.3.0")
        self.assertEqual(strategy, "patch")
        self.assertIsNone(custom)

    @patch("scripts.release.questionary.select")
    @patch("scripts.release.questionary.text")
    @patch("scripts.release.get_next_version")
    def test_custom_choice_returns_custom_version(self, mock_get_next, mock_text, mock_select):
        mock_get_next.return_value = ("patch", "0.3.1")
        mock_select.return_value = MagicMock(ask=MagicMock(return_value="custom"))
        mock_text.return_value = MagicMock(ask=MagicMock(return_value="1.0.0"))
        strategy, custom = release.prompt_bump("0.3.0")
        self.assertEqual(strategy, "custom")
        self.assertEqual(custom, "1.0.0")

    @patch("scripts.release.questionary.select")
    @patch("scripts.release.questionary.text")
    @patch("scripts.release.get_next_version")
    def test_custom_invalid_version_exits(self, mock_get_next, mock_text, mock_select):
        mock_get_next.return_value = ("patch", "0.3.1")
        mock_select.return_value = MagicMock(ask=MagicMock(return_value="custom"))
        mock_text.return_value = MagicMock(ask=MagicMock(return_value="invalid"))

        with self.assertRaises(SystemExit):
            release.prompt_bump("0.3.0")

    @patch("scripts.release.questionary.select")
    @patch("scripts.release.get_next_version")
    def test_abort_on_select_ctrl_c(self, mock_get_next, mock_select):
        mock_get_next.return_value = ("patch", "0.3.1")
        mock_select.return_value = MagicMock(ask=MagicMock(return_value=None))

        with self.assertRaises(SystemExit):
            release.prompt_bump("0.3.0")

    @patch("scripts.release.questionary.select")
    @patch("scripts.release.questionary.text")
    @patch("scripts.release.get_next_version")
    def test_abort_on_custom_ctrl_c(self, mock_get_next, mock_text, mock_select):
        mock_get_next.return_value = ("patch", "0.3.1")
        mock_select.return_value = MagicMock(ask=MagicMock(return_value="custom"))
        mock_text.return_value = MagicMock(ask=MagicMock(return_value=None))

        with self.assertRaises(SystemExit):
            release.prompt_bump("0.3.0")


class TestTrigger(unittest.TestCase):

    @patch("scripts.release.run_command")
    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.prompt_bump")
    @patch("scripts.release.subprocess.run")
    @patch("scripts.release.get_current_version")
    def test_trigger_auto_runs_gh_workflow(
        self, mock_get_ver, mock_sub_run, mock_prompt_bump, mock_confirm, mock_run_cmd
    ):
        mock_get_ver.return_value = "0.3.0"
        mock_prompt_bump.return_value = ("auto", None)
        mock_confirm.return_value = MagicMock(ask=MagicMock(return_value=True))

        def sub_run_side_effect(cmd, **kwargs):
            result = MagicMock()
            result.returncode = 0
            if "rev-parse" in cmd:
                result.stdout = "main\n"
            elif cmd[0:2] == ["gh", "workflow"]:
                result.stdout = "https://github.com/beduality/terracotta/actions/runs/12345\n"
            return result

        mock_sub_run.side_effect = sub_run_side_effect

        release.trigger(bump="auto", yes=True)

        sub_calls = [c[0][0] for c in mock_sub_run.call_args_list]
        workflow_call = next((c for c in sub_calls if c[0:2] == ["gh", "workflow"]), None)
        self.assertIsNotNone(workflow_call)
        self.assertEqual(workflow_call[0:6], ["gh", "workflow", "run", "release.yml", "--ref", "main"])
        self.assertIn("-f", workflow_call)
        self.assertIn("bump=auto", workflow_call)
        # --yes skips monitoring, so run_command should not be called for watch
        mock_run_cmd.assert_not_called()

    @patch("scripts.release.run_command")
    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.prompt_bump")
    @patch("scripts.release.subprocess.run")
    @patch("scripts.release.get_current_version")
    def test_trigger_custom_runs_gh_workflow_with_version(
        self, mock_get_ver, mock_sub_run, mock_prompt_bump, mock_confirm, mock_run_cmd
    ):
        mock_get_ver.return_value = "0.3.0"
        mock_prompt_bump.return_value = ("custom", "1.0.0")
        mock_confirm.return_value = MagicMock(ask=MagicMock(return_value=True))

        def sub_run_side_effect(cmd, **kwargs):
            result = MagicMock()
            result.returncode = 0
            if "rev-parse" in cmd:
                result.stdout = "main\n"
            elif cmd[0:2] == ["gh", "workflow"]:
                result.stdout = "https://github.com/beduality/terracotta/actions/runs/12345\n"
            return result

        mock_sub_run.side_effect = sub_run_side_effect

        release.trigger(bump="custom", version="1.0.0", yes=True)

        sub_calls = [c[0][0] for c in mock_sub_run.call_args_list]
        workflow_call = next((c for c in sub_calls if c[0:2] == ["gh", "workflow"]), None)
        self.assertIsNotNone(workflow_call)
        self.assertIn("bump=custom", workflow_call)
        self.assertIn("version=1.0.0", workflow_call)
        mock_run_cmd.assert_not_called()

    @patch("scripts.release.run_command")
    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.subprocess.run")
    @patch("scripts.release.get_current_version")
    def test_trigger_specific_version_string_maps_to_custom(
        self, mock_get_ver, mock_sub_run, mock_confirm, mock_run_cmd
    ):
        mock_get_ver.return_value = "0.3.0"
        mock_confirm.return_value = MagicMock(ask=MagicMock(return_value=True))

        def sub_run_side_effect(cmd, **kwargs):
            result = MagicMock()
            result.returncode = 0
            if "rev-parse" in cmd:
                result.stdout = "main\n"
            elif cmd[0:2] == ["gh", "workflow"]:
                result.stdout = "https://github.com/beduality/terracotta/actions/runs/12345\n"
            return result

        mock_sub_run.side_effect = sub_run_side_effect

        release.trigger(bump="1.0.0", yes=True)

        sub_calls = [c[0][0] for c in mock_sub_run.call_args_list]
        workflow_call = next((c for c in sub_calls if c[0:2] == ["gh", "workflow"]), None)
        self.assertIsNotNone(workflow_call)
        self.assertIn("bump=custom", workflow_call)
        self.assertIn("version=1.0.0", workflow_call)
        mock_run_cmd.assert_not_called()

    @patch("scripts.release.get_current_version")
    def test_trigger_custom_without_version_exits(self, mock_get_ver):
        mock_get_ver.return_value = "0.3.0"

        with patch("scripts.release.subprocess.run") as mock_sub_run:
            mock_sub_run.return_value = MagicMock(returncode=0)
            with self.assertRaises(SystemExit):
                release.trigger(bump="custom", yes=True)

    @patch("scripts.release.get_current_version")
    def test_trigger_invalid_bump_exits(self, mock_get_ver):
        mock_get_ver.return_value = "0.3.0"

        with patch("scripts.release.subprocess.run") as mock_sub_run:
            mock_sub_run.return_value = MagicMock(returncode=0)
            with self.assertRaises(SystemExit):
                release.trigger(bump="not-a-version", yes=True)



    @patch("scripts.release.get_current_version")
    def test_trigger_no_gh_cli_exits(self, mock_get_ver):
        mock_get_ver.return_value = "0.3.0"

        with patch("scripts.release.subprocess.run") as mock_sub_run:
            mock_sub_run.side_effect = FileNotFoundError("gh not found")
            with self.assertRaises(SystemExit):
                release.trigger(bump="auto", yes=True)

    @patch("scripts.release.get_current_version")
    def test_trigger_unauthenticated_gh_exits(self, mock_get_ver):
        mock_get_ver.return_value = "0.3.0"

        with patch("scripts.release.subprocess.run") as mock_sub_run:
            mock_sub_run.side_effect = subprocess.CalledProcessError(1, ["gh", "auth", "status"])
            with self.assertRaises(SystemExit):
                release.trigger(bump="auto", yes=True)

    @patch("scripts.release.run_command")
    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.prompt_bump")
    @patch("scripts.release.subprocess.run")
    @patch("scripts.release.get_current_version")
    def test_trigger_aborted_confirmation(
        self, mock_get_ver, mock_sub_run, mock_prompt_bump, mock_confirm, mock_run_cmd
    ):
        mock_get_ver.return_value = "0.3.0"
        mock_prompt_bump.return_value = ("auto", None)
        mock_confirm.return_value = MagicMock(ask=MagicMock(return_value=False))

        def sub_run_side_effect(cmd, **kwargs):
            result = MagicMock()
            result.returncode = 0
            if "rev-parse" in cmd:
                result.stdout = "main\n"
            return result

        mock_sub_run.side_effect = sub_run_side_effect

        with self.assertRaises(SystemExit):
            release.trigger(bump="auto")

        mock_run_cmd.assert_not_called()


class TestTriggerGaps(unittest.TestCase):

    @patch("scripts.release.get_current_version")
    def test_trigger_get_current_version_error_exits(self, mock_get_ver):
        mock_get_ver.side_effect = FileNotFoundError("gradle.properties not found")

        with self.assertRaises(SystemExit):
            release.trigger()

    @patch("scripts.release.run_command")
    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.prompt_bump")
    @patch("scripts.release.subprocess.run")
    @patch("scripts.release.get_current_version")
    def test_trigger_bump_none_uses_wizard(
        self, mock_get_ver, mock_sub_run, mock_prompt_bump, mock_confirm, mock_run_cmd
    ):
        mock_get_ver.return_value = "0.3.0"
        mock_prompt_bump.return_value = ("minor", None)
        mock_confirm.return_value = MagicMock(ask=MagicMock(return_value=True))

        def sub_run_side_effect(cmd, **kwargs):
            result = MagicMock()
            result.returncode = 0
            if "rev-parse" in cmd:
                result.stdout = "main\n"
            elif cmd[0:2] == ["gh", "workflow"]:
                result.stdout = "https://github.com/beduality/terracotta/actions/runs/12345\n"
            return result

        mock_sub_run.side_effect = sub_run_side_effect

        release.trigger()

        sub_calls = [c[0][0] for c in mock_sub_run.call_args_list]
        workflow_call = next((c for c in sub_calls if c[0:2] == ["gh", "workflow"]), None)
        self.assertIsNotNone(workflow_call)
        self.assertIn("bump=minor", workflow_call)

        run_calls = [c[0][0] for c in mock_run_cmd.call_args_list]
        watch_call = next((c for c in run_calls if c[0:3] == ["gh", "run", "watch"]), None)
        self.assertIsNotNone(watch_call)
        self.assertIn("12345", watch_call)

    @patch("scripts.release.get_current_version")
    def test_trigger_git_branch_error_exits(self, mock_get_ver):
        mock_get_ver.return_value = "0.3.0"

        with patch("scripts.release.subprocess.run") as mock_sub_run:
            def side_effect(cmd, **kwargs):
                if "rev-parse" in cmd:
                    raise subprocess.CalledProcessError(1, cmd)
                result = MagicMock()
                result.returncode = 0
                return result

            mock_sub_run.side_effect = side_effect
            with self.assertRaises(SystemExit):
                release.trigger(bump="auto", yes=True)

    @patch("scripts.release.run_command")
    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.subprocess.run")
    @patch("scripts.release.get_current_version")
    def test_trigger_warns_on_non_main_branch(
        self, mock_get_ver, mock_sub_run, mock_confirm, mock_run_cmd
    ):
        mock_get_ver.return_value = "0.3.0"
        mock_confirm.return_value = MagicMock(ask=MagicMock(return_value=True))

        def sub_run_side_effect(cmd, **kwargs):
            result = MagicMock()
            result.returncode = 0
            if "rev-parse" in cmd:
                result.stdout = "feature/x\n"
            elif cmd[0:2] == ["gh", "workflow"]:
                result.stdout = "https://github.com/beduality/terracotta/actions/runs/12345\n"
            return result

        mock_sub_run.side_effect = sub_run_side_effect

        release.trigger(bump="auto", yes=True)

        sub_calls = [c[0][0] for c in mock_sub_run.call_args_list]
        workflow_call = next((c for c in sub_calls if c[0:2] == ["gh", "workflow"]), None)
        self.assertIsNotNone(workflow_call)
        self.assertIn("feature/x", workflow_call)
        mock_run_cmd.assert_not_called()


class TestMainGaps(unittest.TestCase):

    def setUp(self):
        self.patches = [
            patch("scripts.release.validate_readme_version"),
            patch("scripts.release.validate_docs_version_snippets"),
            patch("scripts.release.validate_changelog_release_section"),
            patch("scripts.release.validate_javadoc_jars"),
        ]
        for p in self.patches:
            p.start()

    def tearDown(self):
        for p in self.patches:
            p.stop()

    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.get_current_version")
    @patch("scripts.release.get_next_version")
    @patch("scripts.release.update_gradle_properties")
    @patch("scripts.release.update_pyproject_toml")
    @patch("scripts.release.update_changelog")
    @patch("scripts.release.run_command")
    def test_main_explicit_auto_bump(
        self, mock_run_cmd, mock_up_changelog, mock_up_pyproject, mock_up_gradle,
        mock_get_next, mock_get_ver, mock_confirm
    ):
        mock_get_ver.return_value = "0.3.0"
        mock_get_next.return_value = ("minor", "0.4.0")
        mock_confirm.return_value = MagicMock(ask=MagicMock(return_value=True))

        release.main(bump="auto", yes=True, dry_run=False, push=False)

        mock_get_next.assert_called_once_with("0.3.0")
        mock_up_gradle.assert_called_once_with("0.4.0")

    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.get_current_version")
    @patch("scripts.release.prompt_bump")
    @patch("scripts.release.update_gradle_properties")
    @patch("scripts.release.update_pyproject_toml")
    @patch("scripts.release.update_changelog")
    @patch("scripts.release.run_command")
    def test_main_wizard_custom_bump_version_error_exits(
        self, mock_run_cmd, mock_up_changelog, mock_up_pyproject, mock_up_gradle,
        mock_prompt_bump, mock_get_ver, mock_confirm
    ):
        # An invalid current version plus a non-matching custom string forces bump_version to raise.
        mock_get_ver.return_value = "invalid-version"
        mock_prompt_bump.return_value = ("custom", "not-a-version")
        mock_confirm.return_value = MagicMock(ask=MagicMock(return_value=True))

        with self.assertRaises(SystemExit):
            release.main(bump=None, yes=True, dry_run=False, push=False)

        mock_up_gradle.assert_not_called()

    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.get_current_version")
    @patch("scripts.release.prompt_bump")
    @patch("scripts.release.update_gradle_properties")
    @patch("scripts.release.update_pyproject_toml")
    @patch("scripts.release.update_changelog")
    @patch("scripts.release.run_command")
    def test_main_wizard_strategy_bump_version_error_exits(
        self, mock_run_cmd, mock_up_changelog, mock_up_pyproject, mock_up_gradle,
        mock_prompt_bump, mock_get_ver, mock_confirm
    ):
        mock_get_ver.return_value = "invalid-version"
        mock_prompt_bump.return_value = ("patch", None)
        mock_confirm.return_value = MagicMock(ask=MagicMock(return_value=True))

        with self.assertRaises(SystemExit):
            release.main(bump=None, yes=True, dry_run=False, push=False)

        mock_up_gradle.assert_not_called()

    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.get_current_version")
    @patch("scripts.release.update_gradle_properties")
    @patch("scripts.release.update_pyproject_toml")
    @patch("scripts.release.update_changelog")
    @patch("scripts.release.run_command")
    @patch("scripts.release.subprocess.run")
    def test_main_branch_detection_fallback(
        self, mock_sub_run, mock_run_cmd, mock_up_changelog,
        mock_up_pyproject, mock_up_gradle, mock_get_ver, mock_confirm
    ):
        mock_get_ver.return_value = "0.3.0"
        mock_confirm.return_value = MagicMock(ask=MagicMock(side_effect=[True, True]))

        def sub_run_side_effect(cmd, **kwargs):
            result = MagicMock()
            if "rev-parse" in cmd:
                raise subprocess.CalledProcessError(1, cmd)
            result.returncode = 0
            result.stdout = ""
            return result

        mock_sub_run.side_effect = sub_run_side_effect

        release.main(bump="patch", yes=False, dry_run=False)

        run_calls = [c[0][0] for c in mock_run_cmd.call_args_list]
        push_call = next((c for c in run_calls if "push" in c and "origin" in c), None)
        self.assertIsNotNone(push_call)
        self.assertIn("main", push_call)

    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.get_current_version")
    @patch("scripts.release.update_gradle_properties")
    @patch("scripts.release.update_pyproject_toml")
    @patch("scripts.release.update_changelog")
    @patch("scripts.release.run_command")
    def test_main_user_declines_commit(
        self, mock_run_cmd, mock_up_changelog, mock_up_pyproject,
        mock_up_gradle, mock_get_ver, mock_confirm
    ):
        mock_get_ver.return_value = "0.3.0"
        # proceed=True, commit=False
        mock_confirm.return_value = MagicMock(ask=MagicMock(side_effect=[True, False]))

        with self.assertRaises(SystemExit):
            release.main(bump="patch", yes=False, dry_run=False)

        run_calls = [c[0][0] for c in mock_run_cmd.call_args_list]
        self.assertFalse(any("commit" in c for c in run_calls if isinstance(c, list)))

    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.get_current_version")
    @patch("scripts.release.update_gradle_properties")
    @patch("scripts.release.update_pyproject_toml")
    @patch("scripts.release.update_changelog")
    @patch("scripts.release.run_command")
    def test_main_publish_runs_gradle_publish(
        self, mock_run_cmd, mock_up_changelog, mock_up_pyproject,
        mock_up_gradle, mock_get_ver, mock_confirm
    ):
        mock_get_ver.return_value = "0.3.0"
        mock_confirm.return_value = MagicMock(ask=MagicMock(return_value=True))

        release.main(bump="patch", yes=True, dry_run=False, push=False, publish=True)

        run_calls = [c[0][0] for c in mock_run_cmd.call_args_list]
        self.assertTrue(any("validatePublishing" in c for c in run_calls if isinstance(c, list)))
        self.assertTrue(any("publishToCentral" in c for c in run_calls if isinstance(c, list)))


class TestMainRollbackFailurePaths(unittest.TestCase):

    def setUp(self):
        self.patches = [
            patch("scripts.release.validate_readme_version"),
            patch("scripts.release.validate_docs_version_snippets"),
            patch("scripts.release.validate_changelog_release_section"),
            patch("scripts.release.validate_javadoc_jars"),
        ]
        for p in self.patches:
            p.start()

    def tearDown(self):
        for p in self.patches:
            p.stop()

    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.get_current_version")
    @patch("scripts.release.update_gradle_properties")
    @patch("scripts.release.update_pyproject_toml")
    @patch("scripts.release.update_changelog")
    @patch("scripts.release.run_command")
    @patch("scripts.release.subprocess.run")
    def test_rollback_remote_tag_delete_failure(
        self, mock_sub_run, mock_run_cmd, mock_up_changelog,
        mock_up_pyproject, mock_up_gradle, mock_get_ver, mock_confirm
    ):
        mock_get_ver.return_value = "0.3.0"
        mock_confirm.return_value = MagicMock(ask=MagicMock(side_effect=[True, True]))

        def run_cmd_side_effect(cmd, env=None):
            if "push" in cmd and "origin" in cmd and "--tags" in cmd:
                raise subprocess.CalledProcessError(1, cmd)

        mock_run_cmd.side_effect = run_cmd_side_effect

        def sub_run_side_effect(cmd, **kwargs):
            result = MagicMock()
            result.returncode = 0
            result.stdout = "main\n"
            if "push" in cmd and "--delete" in cmd:
                raise subprocess.CalledProcessError(1, cmd)
            return result

        mock_sub_run.side_effect = sub_run_side_effect

        with self.assertRaises(SystemExit):
            release.main(bump="patch", yes=True, dry_run=False)

        rollback_calls = [c[0][0] for c in mock_sub_run.call_args_list]
        # Should attempt remote tag delete because "pushed" was recorded
        self.assertTrue(
            any("push" in c and "--delete" in c and "v0.3.1" in c for c in rollback_calls if isinstance(c, list))
        )

    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.get_current_version")
    @patch("scripts.release.update_gradle_properties")
    @patch("scripts.release.update_pyproject_toml")
    @patch("scripts.release.update_changelog")
    @patch("scripts.release.run_command")
    @patch("scripts.release.subprocess.run")
    def test_rollback_local_tag_delete_failure(
        self, mock_sub_run, mock_run_cmd, mock_up_changelog,
        mock_up_pyproject, mock_up_gradle, mock_get_ver, mock_confirm
    ):
        mock_get_ver.return_value = "0.3.0"
        mock_confirm.return_value = MagicMock(ask=MagicMock(side_effect=[True, True]))

        def run_cmd_side_effect(cmd, env=None):
            if "tag" in cmd and cmd != ["git", "tag", "v0.3.1"]:
                # shouldn't happen; tag creation is the only tag call before failure
                pass
            if "push" in cmd and "origin" in cmd and "--tags" in cmd:
                raise subprocess.CalledProcessError(1, cmd)

        mock_run_cmd.side_effect = run_cmd_side_effect

        def sub_run_side_effect(cmd, **kwargs):
            result = MagicMock()
            if "tag" in cmd and "-d" in cmd:
                raise subprocess.CalledProcessError(1, cmd)
            result.returncode = 0
            result.stdout = "main\n"
            return result

        mock_sub_run.side_effect = sub_run_side_effect

        with self.assertRaises(SystemExit):
            release.main(bump="patch", yes=True, dry_run=False)

        rollback_calls = [c[0][0] for c in mock_sub_run.call_args_list]
        self.assertTrue(
            any("tag" in c and "-d" in c and "v0.3.1" in c for c in rollback_calls if isinstance(c, list))
        )

    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.get_current_version")
    @patch("scripts.release.update_gradle_properties")
    @patch("scripts.release.update_pyproject_toml")
    @patch("scripts.release.update_changelog")
    @patch("scripts.release.run_command")
    @patch("scripts.release.subprocess.run")
    def test_rollback_commit_reset_failure(
        self, mock_sub_run, mock_run_cmd, mock_up_changelog,
        mock_up_pyproject, mock_up_gradle, mock_get_ver, mock_confirm
    ):
        mock_get_ver.return_value = "0.3.0"
        mock_confirm.return_value = MagicMock(ask=MagicMock(side_effect=[True, True]))

        def run_cmd_side_effect(cmd, env=None):
            if "tag" in cmd:
                raise subprocess.CalledProcessError(1, cmd)

        mock_run_cmd.side_effect = run_cmd_side_effect

        def sub_run_side_effect(cmd, **kwargs):
            result = MagicMock()
            if "reset" in cmd:
                raise subprocess.CalledProcessError(1, cmd)
            result.returncode = 0
            result.stdout = "main\n"
            return result

        mock_sub_run.side_effect = sub_run_side_effect

        with self.assertRaises(SystemExit):
            release.main(bump="patch", yes=True, dry_run=False)

        rollback_calls = [c[0][0] for c in mock_sub_run.call_args_list]
        self.assertTrue(any("reset" in c for c in rollback_calls if isinstance(c, list)))

    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.get_current_version")
    @patch("scripts.release.update_gradle_properties")
    @patch("scripts.release.update_pyproject_toml")
    @patch("scripts.release.update_changelog")
    @patch("scripts.release.run_command")
    @patch("scripts.release.subprocess.run")
    def test_rollback_uv_lock_restore_exception_ignored(
        self, mock_sub_run, mock_run_cmd, mock_up_changelog,
        mock_up_pyproject, mock_up_gradle, mock_get_ver, mock_confirm
    ):
        mock_get_ver.return_value = "0.3.0"
        mock_confirm.return_value = MagicMock(ask=MagicMock(return_value=True))

        def run_cmd_side_effect(cmd, env=None):
            if "uv" in cmd and "lock" in cmd:
                raise subprocess.CalledProcessError(1, cmd)

        mock_run_cmd.side_effect = run_cmd_side_effect

        def sub_run_side_effect(cmd, **kwargs):
            result = MagicMock()
            if cmd == ["git", "restore", "uv.lock"]:
                raise subprocess.CalledProcessError(1, cmd)
            result.returncode = 0
            result.stdout = ""
            return result

        mock_sub_run.side_effect = sub_run_side_effect

        with self.assertRaises(SystemExit):
            release.main(bump="patch", yes=True, dry_run=False, push=False)

        rollback_calls = [c[0][0] for c in mock_sub_run.call_args_list]
        # First file restore should succeed, uv.lock restore should be attempted
        restore_calls = [c for c in rollback_calls if isinstance(c, list) and "restore" in c]
        self.assertEqual(len(restore_calls), 2)

    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.get_current_version")
    @patch("scripts.release.update_gradle_properties")
    @patch("scripts.release.update_pyproject_toml")
    @patch("scripts.release.update_changelog")
    @patch("scripts.release.run_command")
    @patch("scripts.release.subprocess.run")
    def test_rollback_file_restore_failure_sets_rollback_failed(
        self, mock_sub_run, mock_run_cmd, mock_up_changelog,
        mock_up_pyproject, mock_up_gradle, mock_get_ver, mock_confirm
    ):
        mock_get_ver.return_value = "0.3.0"
        mock_confirm.return_value = MagicMock(ask=MagicMock(return_value=True))

        def run_cmd_side_effect(cmd, env=None):
            if "uv" in cmd and "lock" in cmd:
                raise subprocess.CalledProcessError(1, cmd)

        mock_run_cmd.side_effect = run_cmd_side_effect

        def sub_run_side_effect(cmd, **kwargs):
            result = MagicMock()
            if "restore" in cmd:
                raise subprocess.CalledProcessError(1, cmd)
            result.returncode = 0
            result.stdout = ""
            return result

        mock_sub_run.side_effect = sub_run_side_effect

        with self.assertRaises(SystemExit):
            release.main(bump="patch", yes=True, dry_run=False, push=False)

        # The function should exit with rollback_failed set and print partial success
        # We assert the failure path was reached by checking the restore attempt
        rollback_calls = [c[0][0] for c in mock_sub_run.call_args_list]
        self.assertTrue(any("restore" in c for c in rollback_calls if isinstance(c, list)))


class TestMonitor(unittest.TestCase):

    @patch("scripts.release.run_command")
    @patch("scripts.release.subprocess.run")
    def test_monitor_with_run_id_watches_it(self, mock_sub_run, mock_run_cmd):
        mock_sub_run.return_value = MagicMock(returncode=0)

        release.monitor(run_id="12345")

        mock_run_cmd.assert_called_once_with(["gh", "run", "watch", "12345"])

    @patch("scripts.release.run_command")
    @patch("scripts.release.subprocess.run")
    def test_monitor_without_run_id_uses_latest(self, mock_sub_run, mock_run_cmd):
        def sub_run_side_effect(cmd, **kwargs):
            result = MagicMock(returncode=0)
            if cmd[:4] == ["gh", "run", "list", "--workflow=release.yml"]:
                result.stdout = "67890\n"
            return result

        mock_sub_run.side_effect = sub_run_side_effect

        release.monitor()

        mock_run_cmd.assert_called_once_with(["gh", "run", "watch", "67890"])

    @patch("scripts.release.subprocess.run")
    def test_monitor_no_gh_auth_exits(self, mock_sub_run):
        mock_sub_run.side_effect = subprocess.CalledProcessError(1, ["gh", "auth", "status"])

        with self.assertRaises(SystemExit):
            release.monitor(run_id="12345")

    @patch("scripts.release.subprocess.run")
    def test_monitor_no_recent_run_exits(self, mock_sub_run):
        def sub_run_side_effect(cmd, **kwargs):
            result = MagicMock(returncode=0)
            if cmd[:4] == ["gh", "run", "list", "--workflow=release.yml"]:
                result.stdout = "\n"
            return result

        mock_sub_run.side_effect = sub_run_side_effect

        with self.assertRaises(SystemExit):
            release.monitor()



class TestAbort(unittest.TestCase):

    def _auth_ok(self, mock_sub_run):
        """Set up subprocess.run so gh auth status succeeds."""
        def side_effect(cmd, **kwargs):
            result = MagicMock()
            if cmd[0:2] == ["gh", "auth"]:
                result.returncode = 0
            elif "run" in cmd and "list" in cmd:
                result.returncode = 0
                result.stdout = "12345\n"
            elif "run" in cmd and "cancel" in cmd:
                result.returncode = 0
            return result
        mock_sub_run.side_effect = side_effect

    @patch("scripts.release.subprocess.run")
    def test_abort_with_run_id_and_yes_cancels_immediately(self, mock_sub_run):
        self._auth_ok(mock_sub_run)

        release.abort(run_id="55555", yes=True)

        cancel_call = next(
            (c[0][0] for c in mock_sub_run.call_args_list if "cancel" in c[0][0]),
            None,
        )
        self.assertIsNotNone(cancel_call)
        self.assertEqual(cancel_call, ["gh", "run", "cancel", "55555"])

    @patch("scripts.release.subprocess.run")
    def test_abort_without_run_id_fetches_latest_active_run(self, mock_sub_run):
        self._auth_ok(mock_sub_run)

        release.abort(yes=True)

        list_call = next(
            (c[0][0] for c in mock_sub_run.call_args_list if "list" in c[0][0]),
            None,
        )
        self.assertIsNotNone(list_call)
        self.assertIn("--workflow=release.yml", list_call)
        cancel_call = next(
            (c[0][0] for c in mock_sub_run.call_args_list if "cancel" in c[0][0]),
            None,
        )
        self.assertEqual(cancel_call, ["gh", "run", "cancel", "12345"])

    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.subprocess.run")
    def test_abort_prompts_and_cancels_when_confirmed(self, mock_sub_run, mock_confirm):
        self._auth_ok(mock_sub_run)
        mock_confirm.return_value = MagicMock(ask=MagicMock(return_value=True))

        release.abort(run_id="55555")

        mock_confirm.assert_called_once()
        cancel_call = next(
            (c[0][0] for c in mock_sub_run.call_args_list if "cancel" in c[0][0]),
            None,
        )
        self.assertEqual(cancel_call, ["gh", "run", "cancel", "55555"])

    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.subprocess.run")
    def test_abort_prompts_and_exits_when_declined(self, mock_sub_run, mock_confirm):
        self._auth_ok(mock_sub_run)
        mock_confirm.return_value = MagicMock(ask=MagicMock(return_value=False))

        with self.assertRaises(SystemExit):
            release.abort(run_id="55555")

        cancel_call = next(
            (c[0][0] for c in mock_sub_run.call_args_list if "cancel" in c[0][0]),
            None,
        )
        self.assertIsNone(cancel_call)

    @patch("scripts.release.subprocess.run")
    def test_abort_no_active_runs_exits_cleanly(self, mock_sub_run):
        def side_effect(cmd, **kwargs):
            result = MagicMock()
            if cmd[0:2] == ["gh", "auth"]:
                result.returncode = 0
            elif "run" in cmd and "list" in cmd:
                result.returncode = 0
                result.stdout = "\n"
            return result
        mock_sub_run.side_effect = side_effect

        with self.assertRaises(SystemExit):
            release.abort(yes=True)

    @patch("scripts.release.subprocess.run")
    def test_abort_gh_auth_failure_exits(self, mock_sub_run):
        def side_effect(cmd, **kwargs):
            if cmd[0:2] == ["gh", "auth"]:
                raise subprocess.CalledProcessError(1, cmd)
            return MagicMock(returncode=0)
        mock_sub_run.side_effect = side_effect

        with self.assertRaises(SystemExit):
            release.abort(run_id="55555", yes=True)

    @patch("scripts.release.subprocess.run")
    def test_abort_cancel_failure_exits(self, mock_sub_run):
        def side_effect(cmd, **kwargs):
            result = MagicMock()
            if cmd[0:2] == ["gh", "auth"]:
                result.returncode = 0
            elif "cancel" in cmd:
                raise subprocess.CalledProcessError(1, cmd)
            return result
        mock_sub_run.side_effect = side_effect

        with self.assertRaises(SystemExit):
            release.abort(run_id="55555", yes=True)


if __name__ == "__main__":
    unittest.main()

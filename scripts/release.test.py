import unittest
from unittest.mock import patch, MagicMock, call
import subprocess
from pathlib import Path
import sys
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
    def test_breaking_bang_suggests_major(self, mock_run):
        self._mock_git(mock_run, 0, "v0.1.0\n", "feat(core)!: remove deprecated API\nfeat: something\n")
        bump, version = release.get_next_version("0.1.0")
        self.assertEqual(bump, "major")
        self.assertEqual(version, "1.0.0")

    @patch("scripts.release.subprocess.run")
    def test_breaking_change_prefix_suggests_major(self, mock_run):
        self._mock_git(mock_run, 0, "v0.1.0\n", "breaking change: removed X\n")
        bump, version = release.get_next_version("0.1.0")
        self.assertEqual(bump, "major")
        self.assertEqual(version, "1.0.0")

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
    def test_major_takes_precedence_over_feat(self, mock_run):
        # A breaking change appears after a feat — major must win regardless of order
        self._mock_git(mock_run, 0, "v0.1.0\n", "feat: new thing\nfeat(core)!: remove API\n")
        bump, _ = release.get_next_version("0.1.0")
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
        mock_read.return_value = "## [Unreleased]\n\n### Added\n- Feature"
        release.update_changelog("0.4.0")
        mock_write.assert_called_once_with("## [0.4.0] - 2026-07-07\n\n### Added\n- Feature")

    @patch("scripts.release.Path.read_text")
    @patch("scripts.release.Path.write_text")
    @patch("scripts.release.date")
    def test_update_changelog_missing_unreleased(self, mock_date, mock_write, mock_read):
        mock_date.today.return_value.isoformat.return_value = "2026-07-07"
        mock_read.return_value = "## [0.3.0]\nSome content"
        with self.assertRaises(ValueError):
            release.update_changelog("0.4.0")


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


if __name__ == "__main__":
    unittest.main()

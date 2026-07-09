import unittest
from unittest.mock import patch, MagicMock
import subprocess
from pathlib import Path

# Import functions/app from release.py
import scripts.release as release

class TestReleaseScript(unittest.TestCase):
    
    def test_bump_version_valid(self):
        # Major bump
        self.assertEqual(release.bump_version("1.2.3", "major"), "2.0.0")
        # Minor bump
        self.assertEqual(release.bump_version("1.2.3", "minor"), "1.3.0")
        # Patch bump
        self.assertEqual(release.bump_version("1.2.3", "patch"), "1.2.4")
        # Custom version
        self.assertEqual(release.bump_version("1.2.3", " 2.3.4 "), "2.3.4")
        # Suffix preservation
        self.assertEqual(release.bump_version("1.2.3-SNAPSHOT", "patch"), "1.2.4-SNAPSHOT")

    def test_bump_version_invalid(self):
        with self.assertRaises(ValueError):
            release.bump_version("invalid", "patch")
        with self.assertRaises(ValueError):
            release.bump_version("1.2.3", "invalid")

    @patch("scripts.release.Path.exists")
    @patch("scripts.release.Path.read_text")
    def test_get_current_version(self, mock_read, mock_exists):
        mock_exists.return_value = True
        mock_read.return_value = "version = 0.3.0\nother_prop = true"
        self.assertEqual(release.get_current_version(), "0.3.0")
        
        mock_read.return_value = "no version here"
        with self.assertRaises(ValueError):
            release.get_current_version()
            
        mock_exists.return_value = False
        with self.assertRaises(FileNotFoundError):
            release.get_current_version()

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
        
        # Test missing section
        mock_read.return_value = "## [0.3.0]\nSome content"
        with self.assertRaises(ValueError):
            release.update_changelog("0.4.0")

    @patch("scripts.release.subprocess.run")
    def test_run_command_success(self, mock_run):
        mock_run.return_value.returncode = 0
        release.run_command(["git", "status"])
        mock_run.assert_called_once_with(["git", "status"], env=None)

    @patch("scripts.release.subprocess.run")
    def test_run_command_failure(self, mock_run):
        mock_run.return_value.returncode = 1
        with self.assertRaises(subprocess.CalledProcessError):
            release.run_command(["git", "status"])

    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.subprocess.run")
    def test_rollback_cmd(self, mock_run, mock_confirm):
        mock_confirm_obj = MagicMock()
        mock_confirm_obj.ask.side_effect = [True, True, True]
        mock_confirm.return_value = mock_confirm_obj
        
        # Mock git log returning release commit message
        mock_run.return_value = MagicMock(returncode=0, stdout="chore: release version 0.4.0\n")
        
        release.rollback("0.4.0")
        
        # Check that v0.4.0 is part of any argument in the run calls
        run_calls = [call[0][0] for call in mock_run.call_args_list]
        self.assertTrue(any(any("v0.4.0" in arg for arg in cmd) for cmd in run_calls if isinstance(cmd, list)))

    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.questionary.select")
    @patch("scripts.release.get_current_version")
    @patch("scripts.release.update_gradle_properties")
    @patch("scripts.release.update_pyproject_toml")
    @patch("scripts.release.update_changelog")
    @patch("scripts.release.run_command")
    @patch("scripts.release.subprocess.run")
    def test_main_wizard_flow(self, mock_sub_run, mock_run_cmd, mock_up_changelog, mock_up_pyproject, mock_up_gradle, mock_get_ver, mock_select, mock_confirm):
        mock_get_ver.return_value = "0.3.0"
        
        mock_select_obj = MagicMock()
        mock_select_obj.ask.return_value = "minor"
        mock_select.return_value = mock_select_obj
        
        mock_confirm_obj = MagicMock()
        mock_confirm_obj.ask.side_effect = [True, True, True]
        mock_confirm.return_value = mock_confirm_obj
        
        release.main(bump=None)
        
        mock_up_gradle.assert_called_once_with("0.4.0")
        mock_up_pyproject.assert_called_once_with("0.4.0")
        mock_up_changelog.assert_called_once_with("0.4.0")
        
        # Verify run_command got called with "0.4.0" somewhere in its arguments
        run_calls = [call[0][0] for call in mock_run_cmd.call_args_list]
        self.assertTrue(any(any("0.4.0" in arg for arg in cmd) for cmd in run_calls if isinstance(cmd, list)))

    @patch("scripts.release.questionary.confirm")
    @patch("scripts.release.get_current_version")
    @patch("scripts.release.update_gradle_properties")
    @patch("scripts.release.update_pyproject_toml")
    @patch("scripts.release.update_changelog")
    @patch("scripts.release.run_command")
    @patch("scripts.release.subprocess.run")
    def test_main_cli_flow_with_rollback(self, mock_sub_run, mock_run_cmd, mock_up_changelog, mock_up_pyproject, mock_up_gradle, mock_get_ver, mock_confirm):
        mock_get_ver.return_value = "0.3.0"
        
        mock_confirm_obj = MagicMock()
        mock_confirm_obj.ask.side_effect = [True, True, True]
        mock_confirm.return_value = mock_confirm_obj
        
        # Make one command fail to trigger rollback
        def run_cmd_side_effect(cmd, env=None):
            if "commit" in cmd:
                raise subprocess.CalledProcessError(1, cmd)
            return None
        mock_run_cmd.side_effect = run_cmd_side_effect
        
        with self.assertRaises(SystemExit):
            release.main(bump="patch")
            
        # Verify subprocess.run was called with git checkout to restore modified files on rollback
        rollback_calls = [call[0][0] for call in mock_sub_run.call_args_list]
        self.assertTrue(any("reset" in call or "checkout" in call for call in rollback_calls))

if __name__ == "__main__":
    unittest.main()

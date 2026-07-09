import unittest
from unittest.mock import patch, MagicMock
import subprocess

# Import function from redeploy_all_docs.py
import scripts.redeploy_all_docs as redeploy_docs

class TestRedeployDocsScript(unittest.TestCase):

    @patch("scripts.redeploy_all_docs.subprocess.run")
    def test_get_versions_with_tags(self, mock_run):
        # Mock git tag output with unordered tags and non-semver tags
        mock_stdout = "v0.2.0\nv0.1.0\nv0.3.0\nbuild-123\nv0.10.0\n"
        mock_run.return_value = MagicMock(returncode=0, stdout=mock_stdout)
        
        versions = redeploy_docs.get_versions()
        
        # Expected versions dictionary with semver order and latest tag having "latest" alias
        expected = {
            "v0.1.0": ("0.1.0", None),
            "v0.2.0": ("0.2.0", None),
            "v0.3.0": ("0.3.0", None),
            "v0.10.0": ("0.10.0", "latest"),
            "main": ("unreleased", "Unreleased")
        }
        
        self.assertEqual(versions, expected)
        mock_run.assert_called_once_with(
            ["git", "tag"],
            cwd=redeploy_docs.root,
            capture_output=True,
            text=True,
            check=True
        )

    @patch("scripts.redeploy_all_docs.subprocess.run")
    def test_get_versions_no_tags(self, mock_run):
        # Mock git tag returning nothing
        mock_run.return_value = MagicMock(returncode=0, stdout="")
        
        versions = redeploy_docs.get_versions()
        
        expected = {
            "main": ("unreleased", "Unreleased")
        }
        self.assertEqual(versions, expected)

    @patch("scripts.redeploy_all_docs.subprocess.run")
    def test_get_versions_git_error(self, mock_run):
        # Mock subprocess failing
        mock_run.side_effect = subprocess.CalledProcessError(1, ["git", "tag"])
        
        versions = redeploy_docs.get_versions()
        
        expected = {
            "main": ("unreleased", "Unreleased")
        }
        self.assertEqual(versions, expected)

    @patch("scripts.redeploy_all_docs.subprocess.run")
    def test_run_command(self, mock_run):
        mock_run.return_value = MagicMock(returncode=0)
        redeploy_docs.run(["git", "status"])
        mock_run.assert_called_once_with(
            ["git", "status"],
            cwd=redeploy_docs.root,
            env=None,
            check=True
        )

if __name__ == "__main__":
    unittest.main()

#!/usr/bin/env python3
"""Tests for load-pulumi-secrets.py with 100% behavior coverage."""

import base64
import importlib.util
import sys
from pathlib import Path
from unittest.mock import MagicMock, patch

import pytest

# Import the module under test (hyphenated filename requires importlib)
_spec = importlib.util.spec_from_file_location(
    "load_pulumi_secrets",
    Path(__file__).parent / "load-pulumi-secrets.py",
)
mod = importlib.util.module_from_spec(_spec)
sys.modules["load_pulumi_secrets"] = mod
_spec.loader.exec_module(mod)

find_env_file = mod.find_env_file
load_env = mod.load_env
detect_secrets_from_app_kt = mod.detect_secrets_from_app_kt
to_camel_case = mod.to_camel_case
decode_signing_key = mod.decode_signing_key
main = mod.main


# --- find_env_file ---


class TestFindEnvFile:
    def test_finds_dot_env_in_cwd(self, tmp_path, monkeypatch):
        (tmp_path / ".env").write_text("KEY=value")
        monkeypatch.chdir(tmp_path)
        result = find_env_file()
        assert result == Path(".env")

    def test_finds_dot_env_in_grandparent(self, tmp_path, monkeypatch):
        (tmp_path / ".env").write_text("KEY=value")
        subdir = tmp_path / "a" / "b"
        subdir.mkdir(parents=True)
        monkeypatch.chdir(subdir)
        result = find_env_file()
        assert result == Path("../../.env")

    def test_exits_when_no_env_file(self, tmp_path, monkeypatch):
        monkeypatch.chdir(tmp_path)
        with pytest.raises(SystemExit) as exc_info:
            find_env_file()
        assert exc_info.value.code == 1


# --- load_env ---


class TestLoadEnv:
    def test_parses_key_value_pairs(self, tmp_path):
        env_file = tmp_path / ".env"
        env_file.write_text("FOO=bar\nBAZ=qux\n")
        assert load_env(env_file) == {"FOO": "bar", "BAZ": "qux"}

    def test_ignores_comments(self, tmp_path):
        env_file = tmp_path / ".env"
        env_file.write_text("# comment\nFOO=bar\n")
        assert load_env(env_file) == {"FOO": "bar"}

    def test_ignores_blank_lines(self, tmp_path):
        env_file = tmp_path / ".env"
        env_file.write_text("\n\nFOO=bar\n\n")
        assert load_env(env_file) == {"FOO": "bar"}

    def test_handles_values_with_equals(self, tmp_path):
        env_file = tmp_path / ".env"
        env_file.write_text("KEY=val=ue\n")
        assert load_env(env_file) == {"KEY": "val=ue"}

    def test_strips_whitespace(self, tmp_path):
        env_file = tmp_path / ".env"
        env_file.write_text("  KEY  =  value  \n")
        assert load_env(env_file) == {"KEY": "value"}

    def test_skips_lines_without_equals(self, tmp_path):
        env_file = tmp_path / ".env"
        env_file.write_text("NOEQUALSSIGN\nKEY=val\n")
        assert load_env(env_file) == {"KEY": "val"}


# --- detect_secrets_from_app_kt ---


class TestDetectSecretsFromAppKt:
    def _make_app_kt(self, tmp_path, content: str):
        app_kt = (
            tmp_path / "modules" / "terracotta-github" / "src" / "main"
            / "kotlin" / "io" / "github" / "beduality" / "terracotta" / "github" / "App.kt"
        )
        app_kt.parent.mkdir(parents=True)
        app_kt.write_text(content)

    def test_extracts_secrets_from_valid_source(self, tmp_path):
        self._make_app_kt(tmp_path, '''
        val secrets =
            listOf(
                "SONATYPE_USERNAME",
                "SONATYPE_PASSWORD",
                "SIGNING_KEY",
                "SIGNING_PASSWORD",
            )
        ''')
        with patch.object(mod, "__file__", str(tmp_path / "scripts" / "load-pulumi-secrets.py")):
            result = detect_secrets_from_app_kt()
        assert result == ["SONATYPE_USERNAME", "SONATYPE_PASSWORD", "SIGNING_KEY", "SIGNING_PASSWORD"]

    def test_returns_empty_when_file_missing(self, tmp_path):
        with patch.object(mod, "__file__", str(tmp_path / "scripts" / "load-pulumi-secrets.py")):
            result = detect_secrets_from_app_kt()
        assert result == []

    def test_returns_empty_when_no_secrets_list_found(self, tmp_path):
        self._make_app_kt(tmp_path, "fun main() { }")
        with patch.object(mod, "__file__", str(tmp_path / "scripts" / "load-pulumi-secrets.py")):
            result = detect_secrets_from_app_kt()
        assert result == []


# --- to_camel_case ---


class TestToCamelCase:
    @pytest.mark.parametrize("input_val,expected", [
        ("SONATYPE_USERNAME", "sonatypeUsername"),
        ("SONATYPE_PASSWORD", "sonatypePassword"),
        ("SIGNING_KEY", "signingKey"),
        ("SIGNING_PASSWORD", "signingPassword"),
        ("TOKEN", "token"),
        ("MY_LONG_VARIABLE", "myLongVariable"),
    ])
    def test_conversion(self, input_val, expected):
        assert to_camel_case(input_val) == expected


# --- decode_signing_key ---


class TestDecodeSigningKey:
    def test_returns_pgp_key_as_is(self):
        pgp_key = "-----BEGIN PGP PRIVATE KEY BLOCK-----\ndata\n-----END PGP PRIVATE KEY BLOCK-----"
        assert decode_signing_key(pgp_key) == pgp_key

    def test_decodes_base64_encoded_key(self, capsys):
        original = "-----BEGIN PGP PRIVATE KEY BLOCK-----\nsecret\n-----END PGP PRIVATE KEY BLOCK-----"
        encoded = base64.b64encode(original.encode()).decode()
        result = decode_signing_key(encoded)
        assert result == original
        assert "Decoded base64" in capsys.readouterr().out

    def test_returns_value_unchanged_on_invalid_base64(self):
        value = "not-base64-!!!@@@"
        assert decode_signing_key(value) == value


# --- main ---


class TestMain:
    def _setup(self, tmp_path, monkeypatch, env_content: str, app_kt_content: str):
        (tmp_path / ".env").write_text(env_content)
        monkeypatch.chdir(tmp_path)
        app_kt = (
            tmp_path / "modules" / "terracotta-github" / "src" / "main"
            / "kotlin" / "io" / "github" / "beduality" / "terracotta" / "github" / "App.kt"
        )
        app_kt.parent.mkdir(parents=True)
        app_kt.write_text(app_kt_content)
        # Create Pulumi.yaml so the script finds the project dir
        pulumi_dir = tmp_path / "modules" / "terracotta-github"
        (pulumi_dir / "Pulumi.yaml").write_text("name: terracotta-github\n")
        monkeypatch.setattr(mod, "__file__", str(tmp_path / "scripts" / "load-pulumi-secrets.py"))

    def test_successful_run(self, tmp_path, monkeypatch):
        self._setup(
            tmp_path, monkeypatch,
            "SONATYPE_USERNAME=user\nSONATYPE_PASSWORD=pass\n",
            'val secrets = listOf("SONATYPE_USERNAME", "SONATYPE_PASSWORD")',
        )
        mock_result = MagicMock(returncode=0)
        with patch("load_pulumi_secrets.subprocess.run", return_value=mock_result) as mock_run:
            main()

        assert mock_run.call_count == 2
        assert mock_run.call_args_list[0][0][0] == [
            "pulumi", "config", "set", "--secret", "sonatypeUsername", "--", "user"
        ]
        assert mock_run.call_args_list[1][0][0] == [
            "pulumi", "config", "set", "--secret", "sonatypePassword", "--", "pass"
        ]
        # Verify cwd is set to the Pulumi project directory
        assert mock_run.call_args_list[0][1]["cwd"] == tmp_path / "modules" / "terracotta-github"

    def test_exits_when_no_secrets_detected(self, tmp_path, monkeypatch):
        (tmp_path / ".env").write_text("KEY=val\n")
        monkeypatch.chdir(tmp_path)
        monkeypatch.setattr(mod, "__file__", str(tmp_path / "scripts" / "load-pulumi-secrets.py"))

        with pytest.raises(SystemExit) as exc_info:
            main()
        assert exc_info.value.code == 1

    def test_exits_when_pulumi_yaml_missing(self, tmp_path, monkeypatch):
        (tmp_path / ".env").write_text("SONATYPE_USERNAME=user\n")
        monkeypatch.chdir(tmp_path)
        # Create App.kt but no Pulumi.yaml
        app_kt = (
            tmp_path / "modules" / "terracotta-github" / "src" / "main"
            / "kotlin" / "io" / "github" / "beduality" / "terracotta" / "github" / "App.kt"
        )
        app_kt.parent.mkdir(parents=True)
        app_kt.write_text('val secrets = listOf("SONATYPE_USERNAME")')
        monkeypatch.setattr(mod, "__file__", str(tmp_path / "scripts" / "load-pulumi-secrets.py"))

        with pytest.raises(SystemExit) as exc_info:
            main()
        assert exc_info.value.code == 1

    def test_skips_unset_secrets(self, tmp_path, monkeypatch, capsys):
        self._setup(
            tmp_path, monkeypatch,
            "SONATYPE_USERNAME=user\n",
            'val secrets = listOf("SONATYPE_USERNAME", "SONATYPE_PASSWORD")',
        )
        monkeypatch.delenv("SONATYPE_PASSWORD", raising=False)

        mock_result = MagicMock(returncode=0)
        with patch("load_pulumi_secrets.subprocess.run", return_value=mock_result) as mock_run:
            main()

        assert mock_run.call_count == 1
        assert "SONATYPE_PASSWORD is not set (skipping)" in capsys.readouterr().out

    def test_falls_back_to_env_var(self, tmp_path, monkeypatch):
        self._setup(
            tmp_path, monkeypatch,
            "",
            'val secrets = listOf("SONATYPE_USERNAME")',
        )
        monkeypatch.setenv("SONATYPE_USERNAME", "from_env")

        mock_result = MagicMock(returncode=0)
        with patch("load_pulumi_secrets.subprocess.run", return_value=mock_result) as mock_run:
            main()

        assert mock_run.call_count == 1
        assert mock_run.call_args_list[0][0][0][-1] == "from_env"

    def test_decodes_signing_key(self, tmp_path, monkeypatch):
        original = "-----BEGIN PGP PRIVATE KEY BLOCK-----\ndata\n-----END PGP PRIVATE KEY BLOCK-----"
        encoded = base64.b64encode(original.encode()).decode()
        self._setup(
            tmp_path, monkeypatch,
            f"SIGNING_KEY={encoded}\n",
            'val secrets = listOf("SIGNING_KEY")',
        )

        mock_result = MagicMock(returncode=0)
        with patch("load_pulumi_secrets.subprocess.run", return_value=mock_result) as mock_run:
            main()

        assert mock_run.call_args_list[0][0][0][-1] == original

    def test_exits_on_pulumi_failure(self, tmp_path, monkeypatch):
        self._setup(
            tmp_path, monkeypatch,
            "SONATYPE_USERNAME=user\n",
            'val secrets = listOf("SONATYPE_USERNAME")',
        )

        mock_result = MagicMock(returncode=1, stderr="some error")
        with patch("load_pulumi_secrets.subprocess.run", return_value=mock_result):
            with pytest.raises(SystemExit) as exc_info:
                main()
            assert exc_info.value.code == 1

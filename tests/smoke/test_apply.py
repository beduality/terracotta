"""
Smoke tests for the Terracotta CLI.

These tests exercise the CLI against the live Modrinth API and verify that
the end-to-end deployment workflow completes successfully.

Prerequisites:
  - MODRINTH_TOKEN is set in the environment (or loaded from a .env file).
  - The CLI has been built with:
      ./gradlew :terracotta-cli:installDist

Notes:
  - These tests interact with the live Modrinth API.
  - They create or update the configured test project.
  - They are intended for manual verification or CI smoke testing.

Run with:
  uv run pytest tests/smoke -v
"""

from pathlib import Path
import subprocess

import pytest

ROOT = Path(__file__).resolve().parents[2]

CLI_BIN = (
    ROOT
    / "modules/terracotta-cli/build/install/terracotta-cli/bin/terracotta-cli"
)

SMOKE_CONFIG = (
    Path(__file__).parent
    / "fixtures"
    / "terracotta-smoke-test.yaml"
)


@pytest.fixture(scope="session", autouse=True)
def require_cli_binary():
    """Skip the suite if the CLI has not been built."""
    if not CLI_BIN.exists():
        pytest.skip(
            f"CLI binary not found at {CLI_BIN.relative_to(ROOT)}.\n"
            "Run ./gradlew :terracotta-cli:installDist first."
        )

    if not SMOKE_CONFIG.exists():
        pytest.fail(f"Smoke test configuration not found: {SMOKE_CONFIG}")


def test_apply_completes_successfully(dummy_jar):
    """The apply command completes successfully against the live Modrinth API."""

    result = subprocess.run(
        [str(CLI_BIN), "apply", "-f", str(SMOKE_CONFIG)],
        cwd=ROOT,
    )

    assert result.returncode == 0
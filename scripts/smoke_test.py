#!/usr/bin/env python3
"""
Smoke test for the Terracotta CLI.

Builds a dummy Paper plugin JAR, then runs `terracotta-cli apply` against
the Modrinth staging environment to verify the full create + upload flow.

Usage:
    python scripts/smoke_test.py
"""

import subprocess
import sys
import zipfile
from pathlib import Path

from rich.console import Console

console = Console()

ROOT = Path(__file__).resolve().parent.parent
CLI_BIN = ROOT / "modules/terracotta-cli/build/install/terracotta-cli/bin/terracotta-cli"
SMOKE_DIR = ROOT / "test/smoke"
SMOKE_CONFIG = SMOKE_DIR / "terracotta-smoke-test.yaml"
DUMMY_JAR = SMOKE_DIR / "build/my-plugin-1.0.0.jar"

PLUGIN_YML = """\
name: TerracottaSmokeTest
version: 1.0.0
main: io.github.beduality.TerracottaSmokeTest
api-version: 1.20
description: Dummy plugin for Terracotta smoke testing
author: Terracotta
"""


def build_dummy_jar() -> None:
    console.print("[bold]Building dummy plugin JAR...[/bold]")
    DUMMY_JAR.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(DUMMY_JAR, "w", zipfile.ZIP_DEFLATED) as zf:
        zf.writestr("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n")
        zf.writestr("plugin.yml", PLUGIN_YML)
    console.print(f"[green]✔[/green] Created {DUMMY_JAR.relative_to(ROOT)} ({DUMMY_JAR.stat().st_size} bytes)")


def run_apply() -> None:
    console.print("\n[bold]Running terracotta-cli apply...[/bold]")
    if not CLI_BIN.exists():
        console.print(f"[red]✘[/red] CLI binary not found at {CLI_BIN.relative_to(ROOT)}")
        console.print("  Run [bold]./gradlew :terracotta-cli:installDist[/bold] first.")
        sys.exit(1)

    result = subprocess.run(
        [str(CLI_BIN), "apply", "-f", str(SMOKE_CONFIG)],
        cwd=ROOT,
    )

    if result.returncode != 0:
        console.print("\n[bold red]✘ Smoke test failed.[/bold red]")
        sys.exit(result.returncode)

    console.print("\n[bold green]✔ Smoke test passed.[/bold green]")


if __name__ == "__main__":
    build_dummy_jar()
    run_apply()

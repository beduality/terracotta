from pathlib import Path
import zipfile

import pytest

ROOT = Path(__file__).resolve().parents[2]

PLUGIN_YML = """\
name: TerracottaSmokeTest
version: 1.0.0
main: io.github.beduality.TerracottaSmokeTest
api-version: "1.20"
description: Dummy plugin for Terracotta smoke testing
author: Terracotta
"""


@pytest.fixture(scope="session")
def dummy_jar() -> Path:
    """Create a minimal Paper plugin JAR for smoke testing."""

    build_dir = Path(__file__).parent / "build"
    build_dir.mkdir(exist_ok=True)

    jar_path = build_dir / "my-plugin-1.0.0.jar"

    with zipfile.ZipFile(jar_path, "w", zipfile.ZIP_DEFLATED) as jar:
        jar.writestr("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n")
        jar.writestr("plugin.yml", PLUGIN_YML)

    yield jar_path

    jar_path.unlink(missing_ok=True)
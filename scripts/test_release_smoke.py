#!/usr/bin/env python3
"""Pytest-based smoke tests for a Terracotta release.

Run the full automated suite for a released version:

    uv run pytest scripts/test_release_smoke.py --release-version 0.1.2

Add optional end-to-end checks:

    uv run pytest scripts/test_release_smoke.py --release-version 0.1.2 \
        --build-from-tag --gradle-e2e --sdk-e2e

The optional checks require a local Gradle installation and JDK 21.
"""

import json
import re
import shutil
import subprocess
import tempfile
import zipfile
from pathlib import Path
from urllib.request import Request, urlopen
from urllib.error import HTTPError

import pytest

REPO = "beduality/terracotta"
GROUP = "io.github.beduality"
MARKER_GROUP = "io.github.beduality.terracotta"
ARTIFACTS = ["terracotta-core", "terracotta-gradle-plugin", "terracotta-provider-modrinth"]


def pytest_addoption(parser):
    parser.addoption("--release-version", required=True, help="Version to smoke-test, e.g. 0.1.2")
    parser.addoption("--build-from-tag", action="store_true", help="Build the project from the release tag")
    parser.addoption("--gradle-e2e", action="store_true", help="Run Gradle plugin end-to-end test")
    parser.addoption("--sdk-e2e", action="store_true", help="Run SDK end-to-end test")


@pytest.fixture(scope="module")
def release_version(request):
    return request.config.getoption("--release-version")


@pytest.fixture(scope="module")
def tag(release_version):
    return f"v{release_version}"


def _run(cmd, cwd=None, check=True, timeout=120):
    return subprocess.run(cmd, cwd=cwd, capture_output=True, text=True, check=check, timeout=timeout)


def _http_code(url):
    try:
        resp = urlopen(Request(url, method="HEAD"), timeout=30)
        return resp.getcode()
    except HTTPError as e:
        return e.code
    except Exception as e:
        return f"ERR: {e}"


def test_github_release_exists(release_version, tag):
    result = _run(
        ["gh", "release", "view", tag, "--json", "tagName,name,url,assets,targetCommitish,publishedAt"],
        check=False,
    )
    assert result.returncode == 0, f"GitHub release {tag} not found: {result.stderr}"
    data = json.loads(result.stdout)
    assert data["tagName"] == tag
    assert data["name"] == f"Release {tag}"

    expected_assets = {
        f"{a}-{release_version}{ext}"
        for a in ARTIFACTS
        for ext in [".jar", "-sources.jar", "-javadoc.jar"]
    }
    actual_assets = {a["name"] for a in data.get("assets", [])}
    missing = expected_assets - actual_assets
    assert not missing, f"Missing GitHub release assets: {sorted(missing)}"


def test_maven_central_artifacts(release_version):
    failures = []
    for artifact in ARTIFACTS:
        base = (
            f"https://repo1.maven.org/maven2/{GROUP.replace('.', '/')}/"
            f"{artifact}/{release_version}/{artifact}-{release_version}"
        )
        for ext in [".pom", ".jar", ".jar.asc", "-sources.jar", "-javadoc.jar", ".pom.asc", ".module"]:
            url = f"{base}{ext}"
            code = _http_code(url)
            if code != 200:
                failures.append(f"{artifact}{ext} -> {code}")

    marker = "io.github.beduality.terracotta.gradle.plugin"
    base = (
        f"https://repo1.maven.org/maven2/{MARKER_GROUP.replace('.', '/')}/"
        f"{marker}/{release_version}/{marker}-{release_version}"
    )
    for ext in [".pom", ".pom.asc"]:
        url = f"{base}{ext}"
        code = _http_code(url)
        if code != 200:
            failures.append(f"plugin-marker{ext} -> {code}")

    assert not failures, f"Maven Central failures: {failures}"


def test_jar_contents(release_version, tmp_path_factory):
    tmp = tmp_path_factory.mktemp("terracotta-smoke")
    expected = {
        "terracotta-core": ["io/github/beduality/terracotta/core/diff/DiffEngine.class"],
        "terracotta-gradle-plugin": [
            "META-INF/gradle-plugins/io.github.beduality.terracotta.properties",
            "io/github/beduality/terracotta/gradle/TerracottaPlugin.class",
            "io/github/beduality/terracotta/gradle/TerracottaPlanTask.class",
            "io/github/beduality/terracotta/gradle/TerracottaApplyTask.class",
        ],
        "terracotta-provider-modrinth": [
            "io/github/beduality/terracotta/provider/modrinth/ModrinthStateProvider.class",
            "io/github/beduality/terracotta/provider/modrinth/ModrinthRegistryProvider.class",
            "io/github/beduality/terracotta/provider/modrinth/ModrinthProviderFactory.class",
        ],
    }

    for artifact in ARTIFACTS:
        url = (
            f"https://repo1.maven.org/maven2/{GROUP.replace('.', '/')}/"
            f"{artifact}/{release_version}/{artifact}-{release_version}.jar"
        )
        jar_path = tmp / f"{artifact}-{release_version}.jar"
        with urlopen(url, timeout=60) as resp:
            jar_path.write_bytes(resp.read())

        with zipfile.ZipFile(jar_path) as zf:
            names = set(zf.namelist())

        for expected_path in expected[artifact]:
            assert expected_path in names, f"{artifact} missing {expected_path}"


def test_public_sites(release_version):
    urls = [
        "https://beduality.github.io/terracotta/",
        f"https://central.sonatype.com/artifact/{GROUP}/terracotta-core/{release_version}",
        f"https://central.sonatype.com/artifact/{GROUP}/terracotta-gradle-plugin/{release_version}",
        f"https://central.sonatype.com/artifact/{GROUP}/terracotta-provider-modrinth/{release_version}",
        f"https://central.sonatype.com/artifact/{MARKER_GROUP}/io.github.beduality.terracotta.gradle.plugin/{release_version}",
    ]
    failures = []
    for url in urls:
        code = _http_code(url)
        if code != 200:
            failures.append(f"{code} {url}")
    assert not failures, f"Public site failures: {failures}"


def test_github_release_body_matches_changelog(release_version, tag):
    """Verify the GitHub release body reflects the non-empty changelog section for this version."""
    repo_root = Path(__file__).resolve().parents[1]
    _run(["git", "fetch", "origin", "tag", tag], cwd=repo_root, check=False)

    changelog = _run(["git", "show", f"{tag}:CHANGELOG.md"], cwd=repo_root, check=False)
    assert changelog.returncode == 0, f"Could not read CHANGELOG.md at {tag}: {changelog.stderr}"

    section_match = re.search(
        rf"## \[{re.escape(release_version)}\].*?(?=\n## \[|\Z)",
        changelog.stdout,
        re.DOTALL,
    )
    assert section_match, f"CHANGELOG missing ## [{release_version}] section"
    section_body = section_match.group(0).split("\n", 1)[1].strip()
    assert section_body, f"CHANGELOG ## [{release_version}] section is empty"

    release = _run(
        ["gh", "release", "view", tag, "--json", "body"],
        check=False,
    )
    assert release.returncode == 0, f"Could not view release {tag}: {release.stderr}"
    body = json.loads(release.stdout).get("body", "")

    # Pick a few non-trivial lines from the changelog section and ensure at least one
    # appears in the release body.
    content_lines = [
        line.strip()
        for line in section_body.splitlines()
        if line.strip() and not line.strip().startswith("#")
    ][:5]
    assert content_lines, f"No usable content lines in CHANGELOG ## [{release_version}]"
    matches = [line for line in content_lines if line in body]
    assert matches, (
        f"GitHub release body does not reflect CHANGELOG ## [{release_version}] content. "
        f"Expected lines like: {content_lines[:3]}"
    )


def test_source_tree_version_strings(release_version, tag):
    """Verify version strings in files at the release tag."""
    repo_root = Path(__file__).resolve().parents[1]
    _run(["git", "fetch", "origin", "tag", tag], cwd=repo_root, check=False)

    files_to_check = {
        f"{tag}:gradle.properties": f"version = {release_version}",
        f"{tag}:pyproject.toml": f'version = "{release_version}"',
        f"{tag}:CHANGELOG.md": f"## [{release_version}]",
        f"{tag}:docs/index.md": f'version "{release_version}"',
    }

    failures = []
    for ref_path, expected in files_to_check.items():
        result = _run(["git", "show", ref_path], cwd=repo_root, check=False)
        if result.returncode != 0:
            failures.append(f"could not read {ref_path}: {result.stderr.strip()}")
        elif expected not in result.stdout:
            failures.append(f"{ref_path} missing expected string {expected!r}")

    assert not failures, f"Source tree version check failures: {failures}"


def test_gpg_signature_files(release_version, tmp_path_factory):
    """Verify Maven Central signature files are present and non-empty.
    If gpg is installed, also try to verify a sample signature."""
    tmp = tmp_path_factory.mktemp("terracotta-smoke-sigs")
    failures = []
    sample = None

    for artifact in ARTIFACTS:
        for ext in [".pom.asc", ".jar.asc"]:
            url = (
                f"https://repo1.maven.org/maven2/{GROUP.replace('.', '/')}/"
                f"{artifact}/{release_version}/{artifact}-{release_version}{ext}"
            )
            path = tmp / f"{artifact}-{release_version}{ext}"
            try:
                with urlopen(url, timeout=30) as resp:
                    data = resp.read()
            except Exception as e:
                failures.append(f"{artifact}{ext} download failed: {e}")
                continue
            if len(data) < 100:
                failures.append(f"{artifact}{ext} looks empty ({len(data)} bytes)")
            if ext == ".jar.asc" and sample is None:
                sample = path
                path.write_bytes(data)

    marker = "io.github.beduality.terracotta.gradle.plugin"
    url = (
        f"https://repo1.maven.org/maven2/{MARKER_GROUP.replace('.', '/')}/"
        f"{marker}/{release_version}/{marker}-{release_version}.pom.asc"
    )
    try:
        with urlopen(url, timeout=30) as resp:
            data = resp.read()
        if len(data) < 100:
            failures.append(f"plugin-marker.pom.asc looks empty ({len(data)} bytes)")
    except Exception as e:
        failures.append(f"plugin-marker.pom.asc download failed: {e}")

    assert not failures, f"Signature file failures: {failures}"

    if sample and shutil.which("gpg"):
        # We cannot verify without the public key, but we can at least confirm
        # gpg recognises the file as an OpenPGP signature.
        result = _run(["gpg", "--list-packets", str(sample)], check=False)
        assert "signature" in result.stdout.lower() or "signature" in result.stderr.lower(), (
            f"gpg did not recognise {sample.name} as a signature"
        )


def test_javadoc_jars_not_empty(release_version, tmp_path_factory):
    """Verify javadoc JARs contain more than just a manifest."""
    tmp = tmp_path_factory.mktemp("terracotta-smoke-javadoc")
    failures = []

    for artifact in ARTIFACTS:
        url = (
            f"https://repo1.maven.org/maven2/{GROUP.replace('.', '/')}/"
            f"{artifact}/{release_version}/{artifact}-{release_version}-javadoc.jar"
        )
        path = tmp / f"{artifact}-{release_version}-javadoc.jar"
        try:
            with urlopen(url, timeout=30) as resp:
                path.write_bytes(resp.read())
        except Exception as e:
            failures.append(f"{artifact}-javadoc.jar download failed: {e}")
            continue

        with zipfile.ZipFile(path) as zf:
            names = set(zf.namelist())
        # Remove the manifest and top-level dir from the count.
        content = [n for n in names if n not in ("META-INF/", "META-INF/MANIFEST.MF")]
        if not content:
            failures.append(f"{artifact}-javadoc.jar is empty (only manifest)")

    assert not failures, f"Javadoc JAR failures: {failures}"


def test_docs_sub_pages(release_version):
    """Verify key docs pages return 200.

    MkDocs Material + mike deploys content under the version directory with a
    `content/` prefix because the repo keeps source docs under `docs/content/`.
    """
    paths = [
        f"https://beduality.github.io/terracotta/{release_version}/content/sdk/reference/installation/",
        f"https://beduality.github.io/terracotta/{release_version}/content/gradle-plugin/tutorials/getting-started/",
        f"https://beduality.github.io/terracotta/{release_version}/content/repo/how-to-guides/releasing/",
    ]
    failures = []
    for url in paths:
        code = _http_code(url)
        if code != 200:
            failures.append(f"{code} {url}")
    assert not failures, f"Docs sub-page failures: {failures}"


def test_docs_version_reflected(release_version):
    """Verify the versioned docs homepage HTML mentions the release version."""
    try:
        with urlopen(
            f"https://beduality.github.io/terracotta/{release_version}/", timeout=30
        ) as resp:
            html = resp.read().decode("utf-8", errors="ignore")
    except Exception as e:
        pytest.fail(f"Could not fetch docs homepage: {e}")
    assert release_version in html, f"Docs homepage does not mention version {release_version}"


@pytest.mark.skipif("not config.getoption('--build-from-tag')")
def test_build_from_tag(release_version, tag):
    repo_root = Path(__file__).resolve().parents[1]
    _run(["git", "fetch", "origin", "tag", tag], cwd=repo_root, check=False)
    _run(["git", "checkout", tag], cwd=repo_root)
    try:
        result = _run(["./gradlew", "spotlessCheck", "build", "--no-daemon"], cwd=repo_root, timeout=300)
        assert result.returncode == 0, f"Build from tag failed:\n{result.stdout}\n{result.stderr}"
    finally:
        _run(["git", "checkout", "-"], cwd=repo_root, check=False)


@pytest.mark.skipif("not config.getoption('--gradle-e2e')")
def test_gradle_plugin_e2e(release_version, tmp_path_factory):
    tmp = tmp_path_factory.mktemp("terracotta-plugin-smoke")
    settings = tmp / "settings.gradle.kts"
    settings.write_text(f'rootProject.name = "terracotta-plugin-smoke-{release_version}"\n')

    build = tmp / "build.gradle.kts"
    build.write_text(
        f'''plugins {{
    java
    id("io.github.beduality.terracotta") version "{release_version}"
}}

repositories {{
    mavenCentral()
}}

dependencies {{
    runtimeOnly("{GROUP}:terracotta-provider-modrinth:{release_version}")
}}

terracotta {{
    artifactFile.set(layout.projectDirectory.file("dummy-artifact.jar"))
    providers {{
        create("modrinth") {{
            projectId.set("dummy-project-id")
        }}
    }}
    name.set("Smoke Test")
    summary.set("Smoke test project")
    description.set("Testing the {release_version} release.")
    license.set("MIT")
    tags.set(listOf("test"))
    gameVersions.set(listOf("1.20.1"))
    loaders.set(listOf("paper"))
    environment.set(io.github.beduality.terracotta.core.model.TerracottaEnvironment.SERVER_ONLY)
}}
'''
    )
    (tmp / "dummy-artifact.jar").write_bytes(b"")

    result = _run(["gradle", "tasks", "--all"], cwd=tmp, check=False, timeout=300)
    output = result.stdout + result.stderr
    assert result.returncode == 0, f"Gradle plugin E2E failed:\n{output}"

    expected_tasks = {"terracottaPlan", "terracottaApply", "terracottaPlanModrinth", "terracottaApplyModrinth"}
    missing = expected_tasks - {t for t in expected_tasks if t in output}
    assert not missing, f"Missing terracotta tasks: {sorted(missing)}"


@pytest.mark.skipif("not config.getoption('--sdk-e2e')")
def test_sdk_e2e(release_version, tmp_path_factory):
    tmp = tmp_path_factory.mktemp("terracotta-sdk-smoke")
    (tmp / "src" / "main" / "kotlin").mkdir(parents=True)

    settings = tmp / "settings.gradle.kts"
    settings.write_text(f'rootProject.name = "terracotta-sdk-smoke-{release_version}"\n')

    build = tmp / "build.gradle.kts"
    build.write_text(
        f'''plugins {{
    kotlin("jvm") version "2.3.21"
}}

repositories {{
    mavenCentral()
}}

java {{
    toolchain {{
        languageVersion.set(JavaLanguageVersion.of(21))
    }}
}}

dependencies {{
    implementation("{GROUP}:terracotta-core:{release_version}")
    implementation("{GROUP}:terracotta-provider-modrinth:{release_version}")
}}
'''
    )

    src = tmp / "src" / "main" / "kotlin" / "Smoke.kt"
    src.write_text(
        '''import io.github.beduality.terracotta.core.model.TerracottaEnvironment

fun main() {
    val env = TerracottaEnvironment.SERVER_ONLY
    println("Resolved environment $env")
}
'''
    )

    result = _run(["gradle", "build", "--stacktrace"], cwd=tmp, timeout=300)
    output = result.stdout + result.stderr
    assert result.returncode == 0, f"SDK E2E build failed:\n{output}"

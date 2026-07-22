#!/usr/bin/env python3
"""Per-module selective release script for Terracotta.

Releases are independent per publishable module. A change under
``modules/<module>/`` only triggers a release for that module. Downstream
modules are built and tested as a compatibility gate but are not released
unless they also changed.
"""

import os
import re
import sys
import subprocess
import zipfile
from datetime import date
from pathlib import Path
from typing import Optional

import semver

# Ensure the repository root is on the path so ``scripts`` can be imported as a
# namespace package regardless of how this file is executed.
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from cyclopts import App
from rich.console import Console
import questionary

from scripts.deployments import generate_deployment_entry, append_deployment

console = Console()
app = App(
    usage=(
        "release.py release [OPTIONS]\n"
        "       release.py trigger [OPTIONS]\n"
        "       release.py extract-release-notes <module> <version> [OPTIONS]\n"
        "       release.py rollback <module> <version>\n"
        "       release.py monitor [RUN_ID]"
    )
)


PUBLISHABLE_MODULES = [
    "terracotta-core",
    "terracotta-state-filesystem",
    "terracotta-provider-modrinth",
    "terracotta-provider-hangar",
    "terracotta-gradle-plugin",
]


MODULE_INFO: dict[str, dict] = {
    "terracotta-core": {
        "path": "modules/terracotta-core",
        "tag_prefix": "terracotta-core-v",
        "human_name": "Terracotta Core",
        "published_name": "terracotta-core",
        "downstream": [
            "terracotta-state-filesystem",
            "terracotta-provider-modrinth",
            "terracotta-provider-hangar",
            "terracotta-gradle-plugin",
        ],
    },
    "terracotta-state-filesystem": {
        "path": "modules/terracotta-state-filesystem",
        "tag_prefix": "terracotta-state-filesystem-v",
        "human_name": "Terracotta State Filesystem",
        "published_name": "terracotta-state-filesystem",
        "downstream": [
            "terracotta-gradle-plugin",
        ],
    },
    "terracotta-provider-modrinth": {
        "path": "modules/terracotta-provider-modrinth",
        "tag_prefix": "terracotta-provider-modrinth-v",
        "human_name": "Terracotta Modrinth Provider",
        "published_name": "terracotta-provider-modrinth",
        "downstream": [],
    },
    "terracotta-provider-hangar": {
        "path": "modules/terracotta-provider-hangar",
        "tag_prefix": "terracotta-provider-hangar-v",
        "human_name": "Terracotta Hangar Provider",
        "published_name": "terracotta-provider-hangar",
        "downstream": [],
    },
    "terracotta-gradle-plugin": {
        "path": "modules/terracotta-gradle-plugin",
        "tag_prefix": "terracotta-gradle-plugin-v",
        "human_name": "Terracotta Gradle Plugin",
        "published_name": "terracotta-gradle-plugin",
        "downstream": [],
    },
}


# ---------------------------------------------------------------------------
# Version helpers
# ---------------------------------------------------------------------------

def get_module_version(module: str) -> str:
    path = Path(MODULE_INFO[module]["path"]) / "gradle.properties"
    if not path.exists():
        raise FileNotFoundError(f"{path} not found")
    content = path.read_text()
    match = re.search(r"^version\s*=\s*([^\s]+)", content, re.MULTILINE)
    if not match:
        raise ValueError(f"Could not find version in {path}")
    return match.group(1).strip()


def set_module_version(module: str, version: str):
    path = Path(MODULE_INFO[module]["path"]) / "gradle.properties"
    content = path.read_text()
    new_content = re.sub(
        r"^version\s*=\s*.*$",
        f"version = {version}",
        content,
        flags=re.MULTILINE,
    )
    path.write_text(new_content)
    console.print(f"[green]✔[/green] Updated {path} to {version}")


def bump_version(current: str, bump_type: str) -> str:
    bump_type_clean = bump_type.strip().lower()

    if re.match(r"^\d+\.\d+\.\d+$", bump_type_clean):
        return bump_type_clean

    ver = semver.Version.parse(current)
    prerelease = ver.prerelease

    if bump_type_clean == "major":
        if ver.major == 0:
            ver = ver.bump_minor()
        else:
            ver = ver.bump_major()
    elif bump_type_clean == "minor":
        ver = ver.bump_minor()
    elif bump_type_clean == "patch":
        ver = ver.bump_patch()
    else:
        raise ValueError(
            f"Invalid bump type: '{bump_type}'. "
            f"Choose 'major', 'minor', 'patch' or a specific version X.Y.Z"
        )

    if prerelease:
        ver = ver.replace(prerelease=prerelease)

    return str(ver)


def validate_next_version(current: str, new_version: str):
    """Validate that *new_version* is a legal next release from *current*.

    Allowed transitions:
    - Next patch, next minor, or next major (per ``bump_version`` rules).
    - Pre-release variants of those (e.g. ``0.9.0-beta.1`` from ``0.8.0``).
    - Stable release from a pre-release (e.g. ``0.8.0`` from ``0.8.0-beta.1``).

    Rejects downgrades, same-version releases, and version jumps.
    """
    cur = semver.Version.parse(current)
    new = semver.Version.parse(new_version)

    if new == cur:
        raise ValueError(
            f"Version {new_version} is already released (current: {current})"
        )

    if new < cur:
        raise ValueError(
            f"Version {new_version} is a downgrade from {current}"
        )

    valid_next = {
        semver.Version.parse(bump_version(current, "patch")),
        semver.Version.parse(bump_version(current, "minor")),
        semver.Version.parse(bump_version(current, "major")),
    }

    if cur.prerelease:
        valid_next.add(cur.replace(prerelease=None))

    new_base = new.replace(prerelease=None)
    if not any(new_base == v.replace(prerelease=None) for v in valid_next):
        allowed = sorted(
            str(v.replace(prerelease=None)) for v in valid_next
        )
        raise ValueError(
            f"Version {new_version} is not a valid next bump from {current}. "
            f"Allowed: {', '.join(allowed)}"
        )


def determine_bump_from_commits(commits: list[str]) -> str:
    bump_type = "patch"
    for subject in commits:
        subject = subject.strip()
        if re.match(r"^[a-z]+(\([^)]+\))?!:", subject):
            bump_type = "major"
            break
        if subject.lower().startswith("breaking change"):
            bump_type = "major"
            break
        if re.match(r"^feat(\([^)]+\))?:", subject) and bump_type != "major":
            bump_type = "minor"
    return bump_type


# ---------------------------------------------------------------------------
# Git / change detection
# ---------------------------------------------------------------------------

def run_command(cmd: list[str], env: Optional[dict] = None, check: bool = True):
    console.print(f"[bold blue]Running:[/bold blue] {' '.join(cmd)}")
    result = subprocess.run(cmd, env=env)
    if check and result.returncode != 0:
        raise subprocess.CalledProcessError(result.returncode, cmd)


def run_git(args: list[str], check: bool = True) -> subprocess.CompletedProcess:
    return subprocess.run(["git", *args], capture_output=True, text=True, check=check)


def get_module_last_tag(module: str) -> Optional[str]:
    prefix = MODULE_INFO[module]["tag_prefix"]
    result = run_git(["describe", "--tags", f"--match={prefix}*", "--abbrev=0"], check=False)
    if result.returncode == 0:
        return result.stdout.strip()
    return None


def get_changed_files(since: str) -> list[str]:
    result = run_git(["diff", f"{since}..HEAD", "--name-only"])
    if result.returncode != 0:
        return []
    return [line for line in result.stdout.splitlines() if line]


def get_all_files(path: str) -> list[str]:
    result = run_git(["ls-files", path])
    if result.returncode != 0:
        return []
    return [line for line in result.stdout.splitlines() if line]


def detect_changed_modules(since_ref: Optional[str] = None) -> dict[str, Optional[str]]:
    """Return {module: from_tag} for modules with changes since their last tag.

    ``from_tag`` is ``None`` when the module has never been released.
    """
    changed: dict[str, Optional[str]] = {}
    for module in PUBLISHABLE_MODULES:
        last_tag = get_module_last_tag(module)
        module_path = MODULE_INFO[module]["path"]

        if last_tag is None:
            files = get_all_files(module_path)
            if files:
                changed[module] = None
            continue

        files = get_changed_files(last_tag)
        for f in files:
            if f.startswith(module_path + "/"):
                changed[module] = last_tag
                break

    if since_ref:
        # Filter to modules that also changed since the provided ref
        ref_files = get_changed_files(since_ref)
        ref_changed = set()
        for f in ref_files:
            for module in PUBLISHABLE_MODULES:
                if f.startswith(MODULE_INFO[module]["path"] + "/"):
                    ref_changed.add(module)
        changed = {m: t for m, t in changed.items() if m in ref_changed}

    return changed


def get_module_commits(module: str, since_tag: Optional[str]) -> list[str]:
    module_path = MODULE_INFO[module]["path"]
    if since_tag:
        result = run_git(
            ["log", f"{since_tag}..HEAD", "--pretty=%s", "--", module_path]
        )
    else:
        result = run_git(["log", "--pretty=%s", "--", module_path])
    if result.returncode != 0:
        return []
    return [line.strip() for line in result.stdout.splitlines() if line.strip()]


# ---------------------------------------------------------------------------
# Changelog
# ---------------------------------------------------------------------------

def _module_changelog_path(module: str) -> Path:
    return Path(MODULE_INFO[module]["path"]) / "CHANGELOG.md"


def _unreleased_section(content: str) -> tuple[int, int, str]:
    match = re.search(
        r"(## \[Unreleased\])\n(.*?)(?=\n## \[|\Z)",
        content,
        re.DOTALL,
    )
    if not match:
        raise ValueError("Could not find '## [Unreleased]' section in changelog")
    return match.start(2), match.end(2), match.group(2).rstrip()


def extract_module_unreleased_notes(module: str) -> Optional[str]:
    path = _module_changelog_path(module)
    if not path.exists():
        return None
    content = path.read_text()
    _, _, unreleased_body = _unreleased_section(content)
    notes = unreleased_body.strip()
    return notes if notes else None


def update_changelog(module_versions: dict[str, str]):
    """Promote each released module's unreleased notes to a versioned section.

    Each module has its own ``CHANGELOG.md`` under ``modules/<module>/``.
    The root ``CHANGELOG.md`` is not modified for module-specific entries.
    """
    today = date.today().isoformat()
    promoted = 0

    for module in sorted(module_versions):
        version = module_versions[module]
        path = _module_changelog_path(module)
        if not path.exists():
            console.print(
                f"[yellow]Warning: {path} not found; "
                f"skipping changelog promotion for {module}[/yellow]"
            )
            continue

        content = path.read_text()
        section_start, section_end, unreleased_body = _unreleased_section(content)

        notes = unreleased_body.strip()
        if not notes:
            console.print(
                f"[yellow]Warning: no unreleased notes for {module}; "
                f"skipping changelog promotion[/yellow]"
            )
            continue

        tag = f"{MODULE_INFO[module]['tag_prefix']}{version}"
        new_section = f"## [{tag}] - {today}\n\n{notes}\n\n"
        new_content = (
            content[:section_start]
            + "\n"
            + new_section
            + content[section_end:]
        )
        path.write_text(new_content)
        promoted += 1

    if promoted:
        console.print(f"[green]✔[/green] Updated {promoted} module changelog(s)")
    else:
        console.print("[dim]No changelog sections to promote[/dim]")


def _extract_release_notes_from_changelog(module: str, version: str) -> str:
    path = _module_changelog_path(module)
    if not path.exists():
        raise FileNotFoundError(f"{path} not found")
    content = path.read_text()
    tag = f"{MODULE_INFO[module]['tag_prefix']}{version}"
    pattern = re.compile(rf"## \[{re.escape(tag)}\].*?(?=\n## \[|\Z)", re.DOTALL)
    match = pattern.search(content)
    if not match:
        raise ValueError(f"{path} is missing a section for {tag}")
    body = match.group(0).split("\n", 1)[1].strip()
    if not body:
        raise ValueError(f"{path} section for {tag} is empty")
    return body


# ---------------------------------------------------------------------------
# Docs snippets
# ---------------------------------------------------------------------------

def _docs_markdown_paths() -> list[Path]:
    paths = [Path("docs/index.md")]
    paths.extend(Path("docs/content").rglob("*.md"))
    return paths


def update_docs_version_snippets(version: str):
    """Update Gradle plugin version snippets in docs."""
    pattern = re.compile(
        r'(id\("io\.github\.beduality\.terracotta"\)\s+version\s+")(\d+\.\d+\.\d+)(")'
    )
    updated = []
    for path in _docs_markdown_paths():
        content = path.read_text()
        new_content = pattern.sub(rf"\g<1>{version}\g<3>", content)
        if new_content != content:
            path.write_text(new_content)
            updated.append(path)
    if updated:
        console.print(
            f"[green]✔[/green] Updated docs version snippets ({len(updated)} files)"
        )
    else:
        console.print("[dim]Docs version snippets already up to date[/dim]")


def validate_docs_version_snippets(version: str):
    pattern = re.compile(
        r'id\("io\.github\.beduality\.terracotta"\)\s+version\s+"(\d+\.\d+\.\d+)"'
    )
    mismatches = []
    for path in _docs_markdown_paths():
        for match in pattern.finditer(path.read_text()):
            if match.group(1) != version:
                mismatches.append(f"{path}:{match.start() + 1}")
    if mismatches:
        raise ValueError(
            f"Docs version snippets do not match {version}: {', '.join(mismatches)}"
        )
    console.print("[green]✔[/green] Docs version snippets validated")


# ---------------------------------------------------------------------------
# Build / publish
# ---------------------------------------------------------------------------

def _java_env() -> dict:
    env = os.environ.copy()
    java_home = os.environ.get("JAVA_HOME", "/usr/lib/jvm/java-21-openjdk")
    env["JAVA_HOME"] = java_home
    return env


def _gradle_project_path(module: str) -> str:
    return f":{module}"


def build_modules(modules: list[str], core_version: Optional[str] = None, dry_run: bool = False):
    """Build and test the requested modules plus their downstream dependents."""
    affected = set(modules)
    for module in list(modules):
        for downstream in MODULE_INFO[module].get("downstream", []):
            affected.add(downstream)

    targets = sorted(affected)
    project_paths = [_gradle_project_path(m) for m in targets]

    console.print(
        f"\n[bold]Building and testing modules: {', '.join(targets)}...[/bold]"
    )

    if dry_run:
        console.print("[dim](dry-run) skipping Gradle build[/dim]")
        return

    env = _java_env()
    extra_props = []
    if core_version:
        extra_props.append(f"-PterracottaCoreReleaseVersion={core_version}")
    run_command(
        ["./gradlew", *project_paths, *extra_props, "spotlessCheck", "check", "build", "--no-daemon"],
        env=env,
    )


def build_javadoc_jars(modules: list[str], core_version: Optional[str] = None, dry_run: bool = False):
    project_paths = [_gradle_project_path(m) for m in modules]
    if dry_run:
        console.print("[dim](dry-run) skipping javadoc JAR build[/dim]")
        return
    env = _java_env()
    extra_props = []
    if core_version:
        extra_props.append(f"-PterracottaCoreReleaseVersion={core_version}")
    run_command(["./gradlew", *project_paths, *extra_props, "javadocJar", "--no-daemon"], env=env)


def validate_javadoc_jars(module_versions: dict[str, str]):
    empty = []
    for module, version in module_versions.items():
        jar = Path(MODULE_INFO[module]["path"]) / f"build/libs/{module}-{version}-javadoc.jar"
        if not jar.exists():
            raise FileNotFoundError(f"Missing javadoc JAR: {jar}")
        if jar.stat().st_size < 1024:
            empty.append(f"{jar.name} ({jar.stat().st_size} bytes)")
            continue
        with zipfile.ZipFile(jar) as zf:
            names = zf.namelist()
        content = [n for n in names if n not in ("META-INF/", "META-INF/MANIFEST.MF")]
        if not content:
            empty.append(f"{jar.name} (only manifest)")
    if empty:
        raise ValueError(f"Javadoc JARs are empty: {', '.join(empty)}")
    console.print("[green]✔[/green] Javadoc JARs validated")


def publish_module_to_central(module: str, version: str, dry_run: bool = False):
    project_path = _gradle_project_path(module)

    extra_props = []
    if module != "terracotta-core":
        # Providers and the Gradle plugin must reference the published core artifact
        # in their POM rather than the local project dependency.
        core_version = get_module_version("terracotta-core")
        extra_props.append(f"-PterracottaCoreReleaseVersion={core_version}")

    if dry_run:
        console.print(
            f"[dim](dry-run) skipping Maven Central publish for {module} {version}[/dim]"
        )
        return

    env = _java_env()
    run_command(
        [
            "./gradlew",
            f"{project_path}:validatePublishing",
            *extra_props,
            "--no-daemon",
        ],
        env=env,
    )
    run_command(
        [
            "./gradlew",
            f"{project_path}:publishToCentral",
            *extra_props,
            "--no-daemon",
        ],
        env=env,
    )
    console.print(f"[green]✔[/green] Published {module} {version} to Maven Central")


def create_github_release(module: str, version: str, dry_run: bool = False):
    tag = f"{MODULE_INFO[module]['tag_prefix']}{version}"
    title = f"{MODULE_INFO[module]['human_name']} {tag}"
    notes = _extract_release_notes_from_changelog(module, version)

    jar_dir = Path(MODULE_INFO[module]["path"]) / "build/libs"
    assets = [str(p) for p in jar_dir.glob("*") if p.is_file()]

    if dry_run:
        console.print(
            f"[dim](dry-run) skipping GitHub Release for {tag}[/dim]"
        )
        return

    if not assets:
        raise FileNotFoundError(f"No release assets found in {jar_dir}")

    # Use the GitHub CLI so this works both locally and in CI.
    cmd = [
        "gh",
        "release",
        "create",
        tag,
        "--title",
        title,
        "--notes",
        notes,
        *assets,
    ]
    run_command(cmd)
    console.print(f"[green]✔[/green] Created GitHub Release {tag}")


# ---------------------------------------------------------------------------
# Git operations
# ---------------------------------------------------------------------------

def rebase_onto_origin(branch: str):
    run_command(["git", "fetch", "origin"])
    ancestor_check = subprocess.run(
        ["git", "merge-base", "--is-ancestor", f"origin/{branch}", "HEAD"],
        capture_output=True,
    )
    if ancestor_check.returncode != 0:
        console.print(
            f"[yellow]origin/{branch} moved ahead during the build; "
            f"rebasing the release commit...[/yellow]"
        )
        try:
            run_command(["git", "rebase", f"origin/{branch}"])
        except subprocess.CalledProcessError:
            subprocess.run(["git", "rebase", "--abort"], check=False)
            raise


def current_branch() -> str:
    result = run_git(["rev-parse", "--abbrev-ref", "HEAD"])
    return result.stdout.strip() or "main"


# ---------------------------------------------------------------------------
# Deployment manifest
# ---------------------------------------------------------------------------

def update_deployment_manifest(module: str, version: str, is_release: bool = False):
    from datetime import datetime, timezone
    from scripts.deployments import parse_changelog_section, extract_summary

    changelog_path = _module_changelog_path(module)
    if not changelog_path.exists():
        console.print(
            f"[yellow]Warning: {changelog_path} not found; "
            f"skipping deployment manifest update[/yellow]"
        )
        return
    content = changelog_path.read_text(encoding="utf-8")
    tag = f"{MODULE_INFO[module]['tag_prefix']}{version}"
    section_body = parse_changelog_section(content, tag)
    if not section_body:
        console.print(
            f"[yellow]Warning: no changelog section for {tag}; "
            f"skipping deployment manifest update[/yellow]"
        )
        return

    now_str = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    entry = generate_deployment_entry(
        version, now_str, section_body, is_release=is_release
    )
    canonical = MODULE_INFO[module]["published_name"].removeprefix("terracotta-")
    entry["modules"] = [canonical]
    added = append_deployment(entry)
    action = "Added" if added else "Updated"
    console.print(
        f"[green]✔[/green] {action} deployment entry for {tag} in deployments.json"
    )


# ---------------------------------------------------------------------------
# Wizard
# ---------------------------------------------------------------------------

def prompt_bump(module: str, current_version: str, yes: bool) -> tuple[str, Optional[str]]:
    if yes:
        return "auto", None

    console.print(f"\n[bold yellow]--- {module} ---[/bold yellow]")

    last_tag = get_module_last_tag(module)
    commits = get_module_commits(module, last_tag)
    auto_bump = determine_bump_from_commits(commits)
    auto_version = bump_version(current_version, auto_bump)

    console.print(
        f"Current: [bold cyan]{current_version}[/bold cyan] | "
        f"Detected bump: [bold magenta]{auto_bump}[/bold magenta] "
        f"→ [bold green]{auto_version}[/bold green]"
    )

    choice = questionary.select(
        "Select bump type",
        choices=[
            questionary.Choice(f"auto ({auto_bump} → {auto_version})", value="auto"),
            "patch",
            "minor",
            "major",
            "custom",
        ],
        default="auto",
    ).ask()

    if choice is None:
        console.print("[yellow]Release aborted.[/yellow]")
        sys.exit(0)

    if choice == "custom":
        custom_version = questionary.text("Enter custom version (e.g., 0.5.0)").ask()
        if custom_version is None:
            console.print("[yellow]Release aborted.[/yellow]")
            sys.exit(0)
        if not re.match(r"^\d+\.\d+\.\d+.*$", custom_version):
            console.print(
                f"[bold red]Invalid version format: '{custom_version}'. "
                f"Expected format: X.Y.Z[/bold red]"
            )
            sys.exit(1)
        return "custom", custom_version

    return choice, None


# ---------------------------------------------------------------------------
# Commands
# ---------------------------------------------------------------------------

@app.command
def release(
    bump: Optional[str] = None,
    modules: Optional[str] = None,
    dry_run: bool = False,
    publish: bool = False,
    push: bool = True,
    yes: bool = False,
    since: Optional[str] = None,
):
    """Run a per-module release.

    Parameters
    ----------
    bump : str, optional
        Bump strategy applied to all changed modules: 'auto', 'patch', 'minor',
        'major', or a specific version string.
    modules : str, optional
        Comma-separated list of modules to release, ignoring change detection.
    dry_run : bool
        Compute changes and versions but do not publish, tag, or push.
    publish : bool
        Publish changed modules to Maven Central.
    push : bool
        Commit, tag, and push changes to git.
    yes : bool
        Skip all interactive prompts.
    since : str, optional
        Git ref to use as the change-detection baseline (default is each
        module's last release tag).
    """
    if modules:
        module_list = [m.strip() for m in modules.split(",")]
        invalid = [m for m in module_list if m not in PUBLISHABLE_MODULES]
        if invalid:
            raise ValueError(f"Unknown modules: {', '.join(invalid)}")
        changed = {m: get_module_last_tag(m) for m in module_list}
    else:
        changed = detect_changed_modules(since_ref=since)

    if not changed:
        console.print("[yellow]No modules changed since their last release.[/yellow]")
        sys.exit(0)

    console.print(f"Modules to release: {', '.join(changed)}")

    module_versions: dict[str, str] = {}
    for module in changed:
        current = get_module_version(module)
        last_tag = changed[module]

        if bump and bump.strip().lower() != "auto":
            strategy = bump.strip().lower()
            new_version = bump_version(current, strategy)
        elif bump and bump.strip().lower() == "auto":
            commits = get_module_commits(module, last_tag)
            detected = determine_bump_from_commits(commits)
            new_version = bump_version(current, detected)
        else:
            strategy, custom = prompt_bump(module, current, yes)
            if strategy == "custom":
                new_version = bump_version(current, custom)
            elif strategy == "auto":
                commits = get_module_commits(module, last_tag)
                detected = determine_bump_from_commits(commits)
                new_version = bump_version(current, detected)
            else:
                new_version = bump_version(current, strategy)

        validate_next_version(current, new_version)
        module_versions[module] = new_version
        console.print(
            f"{module}: [bold cyan]{current}[/bold cyan] "
            f"→ [bold green]{new_version}[/bold green]"
        )

    core_version = module_versions.get("terracotta-core")
    if core_version is None:
        core_version = get_module_version("terracotta-core")

    if dry_run:
        console.print(
            "\n[dim](dry-run) no files will be modified, no builds run, no artifacts published[/dim]"
        )
        sys.exit(0)

    if not yes and not questionary.confirm("Proceed with release?").ask():
        console.print("[yellow]Release aborted.[/yellow]")
        sys.exit(0)

    actions_taken: list[str] = []

    try:
        # 1. Update versions and changelog
        console.print("\n[bold]1. Updating versions and changelog...[/bold]")
        actions_taken.append("files_modified")
        for module, version in module_versions.items():
            set_module_version(module, version)
        update_changelog(module_versions)

        # Only update docs plugin snippets when the Gradle plugin itself changes
        if "terracotta-gradle-plugin" in module_versions:
            update_docs_version_snippets(module_versions["terracotta-gradle-plugin"])
            validate_docs_version_snippets(module_versions["terracotta-gradle-plugin"])

        # 2. Update deployment manifest entries
        console.print("\n[bold]2. Updating deployment manifest...[/bold]")
        for module, version in module_versions.items():
            update_deployment_manifest(module, version, is_release=False)

        # 3. Build and test changed modules + downstream dependents
        console.print("\n[bold]3. Compatibility build and test...[/bold]")
        build_modules(list(module_versions.keys()), core_version=core_version, dry_run=dry_run)

        # 4. Build and validate javadoc JARs
        console.print("\n[bold]4. Building and validating javadoc JARs...[/bold]")
        build_javadoc_jars(list(module_versions.keys()), core_version=core_version, dry_run=dry_run)
        validate_javadoc_jars(module_versions)

        # 5. Commit and tag
        branch = current_branch()
        if push:
            console.print("\n[bold]5. Committing and tagging...[/bold]")
            version_files = [
                MODULE_INFO[m]["path"] + "/gradle.properties"
                for m in module_versions
            ]
            changelog_files = [
                MODULE_INFO[m]["path"] + "/CHANGELOG.md"
                for m in module_versions
            ]
            run_command(
                ["git", "add", "deployments.json", *version_files, *changelog_files]
                + (
                    ["docs/index.md", "docs/content"]
                    if "terracotta-gradle-plugin" in module_versions
                    else []
                )
            )
            tags = ", ".join(
                f"{MODULE_INFO[m]['tag_prefix']}{v}" for m, v in module_versions.items()
            )
            run_command(
                ["git", "commit", "-m", f"chore: release {tags}"]
            )
            actions_taken.append("committed")

            rebase_onto_origin(branch)

            for module, version in module_versions.items():
                tag = f"{MODULE_INFO[module]['tag_prefix']}{version}"
                run_command(["git", "tag", tag])
            actions_taken.append("tagged")

        # 6. Publish to Maven Central
        if publish:
            console.print("\n[bold]6. Publishing to Maven Central...[/bold]")
            for module, version in module_versions.items():
                publish_module_to_central(module, version, dry_run=dry_run)

        # 7. Create GitHub Releases
        console.print("\n[bold]7. Creating GitHub Releases...[/bold]")
        for module, version in module_versions.items():
            create_github_release(module, version, dry_run=dry_run)

        # 8. Push commit and tags
        if push:
            console.print("\n[bold]8. Pushing branch and tags...[/bold]")
            actions_taken.append("pushed")
            run_command(["git", "push", "origin", branch, "--tags"])
            console.print(
                f"\n[bold green]Successfully released {', '.join(module_versions)}![/bold green]"
            )

    except (FileNotFoundError, ValueError, subprocess.CalledProcessError, OSError) as e:
        console.print(f"\n[bold red]Error occurred during release: {e}[/bold red]")
        console.print("[bold yellow]Initiating automatic rollback...[/bold yellow]")
        _rollback_release(module_versions, actions_taken, branch if push else "main")
        sys.exit(1)


def _rollback_release(
    module_versions: dict[str, str],
    actions_taken: list[str],
    branch: str,
):
    rollback_failed = False

    if "pushed" in actions_taken:
        for module, version in module_versions.items():
            tag = f"{MODULE_INFO[module]['tag_prefix']}{version}"
            try:
                run_command(["git", "push", "origin", "--delete", tag])
            except subprocess.CalledProcessError as e:
                console.print(f"[red]Failed to delete remote tag {tag}: {e}[/red]")
                rollback_failed = True

    if "tagged" in actions_taken:
        for module, version in module_versions.items():
            tag = f"{MODULE_INFO[module]['tag_prefix']}{version}"
            try:
                run_command(["git", "tag", "-d", tag])
            except subprocess.CalledProcessError as e:
                console.print(f"[red]Failed to delete local tag {tag}: {e}[/red]")
                rollback_failed = True

    if "committed" in actions_taken:
        try:
            run_command(["git", "reset", "--hard", "HEAD~1"])
        except subprocess.CalledProcessError as e:
            console.print(f"[red]Failed to reset commit: {e}[/red]")
            rollback_failed = True

    if "files_modified" in actions_taken and "committed" not in actions_taken:
        try:
            restore_paths = ["deployments.json"]
            for module in module_versions:
                restore_paths.append(MODULE_INFO[module]["path"] + "/gradle.properties")
                restore_paths.append(MODULE_INFO[module]["path"] + "/CHANGELOG.md")
            restore_paths.extend(["docs/index.md", "docs/content"])
            run_command(["git", "restore", *restore_paths])
        except subprocess.CalledProcessError as e:
            console.print(f"[red]Failed to restore modified files: {e}[/red]")
            rollback_failed = True

    if rollback_failed:
        console.print(
            "\n[bold red]Automatic rollback was only partially successful. "
            "Please complete any manual steps listed above.[/bold red]"
        )
    else:
        console.print(
            "\n[bold green]Automatic rollback completed successfully.[/bold green]"
        )


@app.command
def extract_release_notes(
    module: str,
    version: str,
    output: Optional[str] = None,
):
    """Extract the changelog release notes for a module version.

    Parameters
    ----------
    module : str
        Module name, e.g. 'terracotta-core'.
    version : str
        Module version without the tag prefix.
    output : str, optional
        Path to write the notes to instead of printing.
    """
    if module not in PUBLISHABLE_MODULES:
        raise ValueError(f"Unknown module: {module}")
    notes = _extract_release_notes_from_changelog(module, version)
    if output:
        Path(output).write_text(notes)
        console.print(f"[green]✔[/green] Wrote release notes to {output}")
    else:
        print(notes)


@app.command
def rollback(module: str, release_version: str, yes: bool = False):
    """Rollback a per-module release.

    Parameters
    ----------
    module : str
        Module to rollback, e.g. 'terracotta-core'.
    release_version : str
        Version to rollback.
    yes : bool
        Skip confirmation prompts.
    """
    if module not in PUBLISHABLE_MODULES:
        raise ValueError(f"Unknown module: {module}")

    tag = f"{MODULE_INFO[module]['tag_prefix']}{release_version}"
    console.print(f"[bold yellow]Initiating rollback for {tag}...[/bold yellow]")

    if not yes and not questionary.confirm(f"Delete remote tag {tag}?").ask():
        return
    try:
        run_command(["git", "push", "origin", "--delete", tag])
        console.print(f"[green]✔[/green] Deleted remote tag {tag}")
    except subprocess.CalledProcessError as e:
        console.print(f"[yellow]Could not delete remote tag: {e}[/yellow]")

    if not yes and not questionary.confirm(f"Delete local tag {tag}?").ask():
        return
    try:
        run_command(["git", "tag", "-d", tag])
        console.print(f"[green]✔[/green] Deleted local tag {tag}")
    except subprocess.CalledProcessError as e:
        console.print(f"[yellow]Could not delete local tag: {e}[/yellow]")

    try:
        res = run_git(["log", "-1", "--pretty=%s"])
        commit_msg = res.stdout.strip()
        expected_msg = f"chore: release {tag}"
        if commit_msg == expected_msg:
            if yes or questionary.confirm(
                f"Found release commit '{commit_msg}'. Reset this commit?"
            ).ask():
                run_command(["git", "reset", "HEAD~1"])
                console.print("[green]✔[/green] Reset the last git commit.")
    except subprocess.CalledProcessError as e:
        console.print(f"[yellow]Could not inspect git commit: {e}[/yellow]")


@app.command
def trigger(
    bump: Optional[str] = None,
    modules: Optional[str] = None,
    yes: bool = False,
):
    """Trigger the Release workflow on GitHub Actions from the local CLI.

    Parameters
    ----------
    bump : str, optional
        Bump strategy for changed modules ('auto', 'patch', 'minor', 'major',
        or a specific version).
    modules : str, optional
        Comma-separated modules to release.
    yes : bool
        Skip the confirmation prompt.
    """
    try:
        subprocess.run(["gh", "auth", "status"], capture_output=True, check=True)
    except (subprocess.CalledProcessError, FileNotFoundError):
        console.print("[bold red]GitHub CLI (`gh`) is required and authenticated.[/bold red]")
        sys.exit(1)

    branch = current_branch()
    if branch != "main":
        console.print(f"[yellow]Warning: you are on branch '{branch}', not 'main'.[/yellow]")

    cmd = ["gh", "workflow", "run", "release.yml", "--ref", branch]
    if bump:
        cmd.extend(["-f", f"bump={bump}"])
    if modules:
        cmd.extend(["-f", f"modules={modules}"])

    proceed = yes or questionary.confirm(
        f"Trigger Release workflow on '{branch}'?"
    ).ask()
    if not proceed:
        console.print("[yellow]Release aborted.[/yellow]")
        sys.exit(0)

    run_command(cmd)
    console.print("[bold green]Successfully triggered Release workflow.[/bold green]")


@app.command
def abort(run_id: Optional[str] = None, yes: bool = False):
    """Cancel a Release workflow run."""
    try:
        subprocess.run(["gh", "auth", "status"], capture_output=True, check=True)
    except (subprocess.CalledProcessError, FileNotFoundError):
        console.print("[bold red]GitHub CLI (`gh`) is required and authenticated.[/bold red]")
        sys.exit(1)

    if not run_id:
        result = run_git(
            [
                "run",
                "list",
                "--workflow=release.yml",
                "--json",
                "databaseId,status",
                "--jq",
                '.[0] | select(.status != "completed") | .databaseId',
            ]
        )
        run_id = result.stdout.strip()

    if not run_id:
        console.print("[yellow]No active release.yml runs found.[/yellow]")
        sys.exit(0)

    if not yes and not questionary.confirm(f"Cancel release.yml run {run_id}?").ask():
        console.print("[yellow]Cancellation aborted.[/yellow]")
        sys.exit(0)

    run_command(["gh", "run", "cancel", run_id])
    console.print(f"[bold green]✔[/bold green] Cancelled release.yml run {run_id}")


@app.command
def monitor(run_id: Optional[str] = None):
    """Monitor a Release workflow run in real time."""
    try:
        subprocess.run(["gh", "auth", "status"], capture_output=True, check=True)
    except (subprocess.CalledProcessError, FileNotFoundError):
        console.print("[bold red]GitHub CLI (`gh`) is required and authenticated.[/bold red]")
        sys.exit(1)

    if not run_id:
        result = run_git(
            ["run", "list", "--workflow=release.yml", "--json", "databaseId", "--jq", ".[0].databaseId"]
        )
        run_id = result.stdout.strip()

    if not run_id:
        console.print("[yellow]No recent release.yml runs found.[/yellow]")
        sys.exit(0)

    console.print(f"[dim]Monitoring run {run_id}...[/dim]")
    run_command(["gh", "run", "watch", run_id])


if __name__ == "__main__":
    app()

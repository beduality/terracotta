#!/usr/bin/env python3
import os
import re
import sys
import subprocess
import zipfile
from datetime import date
from pathlib import Path
from cyclopts import App
from rich.console import Console
import questionary

console = Console()
app = App(
    usage="release.py [BUMP] [OPTIONS]\n       release.py trigger [OPTIONS]\n       release.py monitor [RUN_ID]\n       release.py rollback <version>"
)

def get_current_version() -> str:
    path = Path("gradle.properties")
    if not path.exists():
        raise FileNotFoundError("gradle.properties not found")
    content = path.read_text()
    match = re.search(r"^version\s*=\s*([^\s]+)", content, re.MULTILINE)
    if not match:
        raise ValueError("Could not find version in gradle.properties")
    return match.group(1).strip()

def get_next_version(current: str) -> tuple[str, str]:
    """Determine the next version by inspecting conventional commits since the last tag.

    Returns a tuple of (bump_type, next_version) where bump_type is one of
    'major', 'minor', or 'patch'. Falls back to 'patch' if no commits are found
    or none match a known type.
    """
    try:
        # Find the most recent tag so we only look at commits since then
        tag_result = subprocess.run(
            ["git", "describe", "--tags", "--abbrev=0"],
            capture_output=True, text=True
        )
        if tag_result.returncode == 0:
            last_tag = tag_result.stdout.strip()
            log_range = f"{last_tag}..HEAD"
        else:
            # No tags yet — inspect the entire history
            log_range = "HEAD"

        log_result = subprocess.run(
            ["git", "log", log_range, "--pretty=%s"],
            capture_output=True, text=True, check=True
        )
        subjects = log_result.stdout.strip().splitlines()
    except Exception:
        subjects = []

    bump_type = "patch"
    for subject in subjects:
        subject = subject.strip()
        # Breaking change via '!' suffix or BREAKING CHANGE in body/footer
        if re.match(r"^[a-z]+(\([^)]+\))?!:", subject):
            bump_type = "major"
            break
        if subject.lower().startswith("breaking change"):
            bump_type = "major"
            break
        # New feature
        if re.match(r"^feat(\([^)]+\))?:", subject) and bump_type != "major":
            bump_type = "minor"

    return bump_type, bump_version(current, bump_type)


def bump_version(current: str, bump_type: str) -> str:
    bump_type_clean = bump_type.strip().lower()

    # Check if the bump_type is a specific custom version
    if re.match(r"^\d+\.\d+\.\d+$", bump_type_clean):
        return bump_type_clean

    match = re.match(r"^(\d+)\.(\d+)\.(\d+)(.*)$", current)
    if not match:
        raise ValueError(f"Current version '{current}' is not a valid semver")

    major, minor, patch, suffix = match.groups()
    major, minor, patch = int(major), int(minor), int(patch)

    if bump_type_clean == "major":
        major += 1
        minor = 0
        patch = 0
    elif bump_type_clean == "minor":
        minor += 1
        patch = 0
    elif bump_type_clean == "patch":
        patch += 1
    else:
        raise ValueError(
            f"Invalid bump type: '{bump_type}'. Choose 'major', 'minor', 'patch' or a specific version X.Y.Z"
        )

    return f"{major}.{minor}.{patch}{suffix}"

def prompt_bump(current_version: str, yes: bool = False) -> tuple[str, str | None]:
    """Interactively prompt for the release bump strategy.

    Returns a tuple of (strategy, custom_version). strategy is one of 'auto',
    'patch', 'minor', 'major', or 'custom'. custom_version is only set when
    strategy is 'custom'.
    """
    if yes:
        return "auto", None

    console.print("\n[bold yellow]--- Wizard Mode ---[/bold yellow]")

    auto_bump, auto_version = get_next_version(current_version)
    console.print(
        f"Detected bump from commits: [bold magenta]{auto_bump}[/bold magenta] "
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

    # Handle ctrl+c
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
                f"[bold red]Invalid version format: '{custom_version}'. Expected format: X.Y.Z[/bold red]"
            )
            sys.exit(1)
        return "custom", custom_version

    return choice, None

def update_gradle_properties(new_version: str):
    path = Path("gradle.properties")
    content = path.read_text()
    new_content = re.sub(
        r"^version\s*=\s*.*$", f"version = {new_version}", content, flags=re.MULTILINE
    )
    path.write_text(new_content)
    console.print("[green]✔[/green] Updated gradle.properties")

def update_pyproject_toml(new_version: str):
    path = Path("pyproject.toml")
    content = path.read_text()
    new_content = re.sub(
        r"^version\s*=\s*\".*\"$", f"version = \"{new_version}\"", content, flags=re.MULTILINE
    )
    path.write_text(new_content)
    console.print("[green]✔[/green] Updated pyproject.toml")

def update_changelog(new_version: str):
    path = Path("CHANGELOG.md")
    content = path.read_text()
    today = date.today().isoformat()

    if "## [Unreleased]" not in content:
        raise ValueError("Could not find '## [Unreleased]' section in CHANGELOG.md")

    # Find the Unreleased section and its body, stopping at the next ## [ header.
    match = re.search(
        r"(## \[Unreleased\])\n(.*?)(?=\n## \[|\Z)",
        content,
        re.DOTALL,
    )
    if not match:
        raise ValueError("Could not parse '## [Unreleased]' section in CHANGELOG.md")

    unreleased_body = match.group(2).rstrip()
    new_header = f"## [{new_version}] - {today}"

    # Replace the Unreleased section with an empty one and place the new release
    # section (with the old Unreleased body) right after it.
    new_section = f"## [Unreleased]\n\n{new_header}\n{unreleased_body}"
    new_content = content[: match.start()] + new_section + content[match.end() :]
    path.write_text(new_content)
    console.print("[green]✔[/green] Updated CHANGELOG.md")


def _docs_markdown_paths() -> list[Path]:
    paths = [Path("docs/index.md")]
    paths.extend(Path("docs/content").rglob("*.md"))
    return paths


def update_docs_version_snippets(new_version: str):
    pattern = re.compile(
        r'(id\("io\.github\.beduality\.terracotta"\)\s+version\s+")(\d+\.\d+\.\d+)(")'
    )
    updated = []
    for path in _docs_markdown_paths():
        content = path.read_text()
        new_content = pattern.sub(rf"\g<1>{new_version}\g<3>", content)
        if new_content != content:
            path.write_text(new_content)
            updated.append(path)
    if updated:
        console.print(f"[green]✔[/green] Updated docs version snippets ({len(updated)} files)")
    else:
        console.print("[dim]Docs version snippets already up to date[/dim]")


def validate_docs_version_snippets(new_version: str):
    pattern = re.compile(
        r'id\("io\.github\.beduality\.terracotta"\)\s+version\s+"(\d+\.\d+\.\d+)"'
    )
    mismatches = []
    for path in _docs_markdown_paths():
        for match in pattern.finditer(path.read_text()):
            if match.group(1) != new_version:
                mismatches.append(f"{path}:{match.start() + 1}")
    if mismatches:
        raise ValueError(
            f"Docs version snippets do not match {new_version}: {', '.join(mismatches)}"
        )
    console.print("[green]✔[/green] Docs version snippets validated")


def validate_changelog_release_section(new_version: str):
    path = Path("CHANGELOG.md")
    content = path.read_text()
    match = re.search(
        rf"## \[{re.escape(new_version)}\].*?(?=\n## \[|\Z)",
        content,
        re.DOTALL,
    )
    if not match:
        raise ValueError(f"CHANGELOG.md is missing a ## [{new_version}] section")
    body = match.group(0).split("\n", 1)[1].strip()
    content_lines = [
        line for line in body.splitlines()
        if line.strip() and not line.strip().startswith("#")
    ]
    if not content_lines:
        raise ValueError(
            f"CHANGELOG.md ## [{new_version}] section is empty. "
            "Add release notes before cutting the release."
        )
    console.print("[green]✔[/green] CHANGELOG.md release section validated")


@app.command
def extract_release_notes(
    version: str,
    output: str = None,
):
    """Extract the changelog release notes for the given version.

    Reads CHANGELOG.md and prints the body of the ## [version] section. If
    --output is provided, the notes are written to that file instead.

    Parameters
    ----------
    version : str
        The release version (e.g. '0.4.0').
    output : str, optional
        Path to a file where the notes should be written.
    """
    path = Path("CHANGELOG.md")
    if not path.exists():
        raise FileNotFoundError("CHANGELOG.md not found")
    content = path.read_text()
    match = re.search(
        rf"## \[{re.escape(version)}\].*?(?=\n## \[|\Z)",
        content,
        re.DOTALL,
    )
    if not match:
        raise ValueError(f"CHANGELOG.md is missing a ## [{version}] section")
    body = match.group(0).split("\n", 1)[1].strip()
    if not body:
        raise ValueError(f"CHANGELOG.md ## [{version}] section is empty")
    if output:
        Path(output).write_text(body)
        console.print(f"[green]✔[/green] Wrote release notes to {output}")
    else:
        print(body)


def validate_javadoc_jars(new_version: str):
    empty = []
    for module in [
        "terracotta-core",
        "terracotta-gradle-plugin",
        "terracotta-provider-modrinth",
    ]:
        jar = Path(f"modules/{module}/build/libs/{module}-{new_version}-javadoc.jar")
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

def run_command(cmd: list[str], env: dict = None):
    console.print(f"[bold blue]Running:[/bold blue] {' '.join(cmd)}")
    result = subprocess.run(cmd, env=env)
    if result.returncode != 0:
        raise subprocess.CalledProcessError(result.returncode, cmd)

@app.command
def rollback(release_version: str):
    """Rollback a release by deleting local and remote tags, and resetting the release commit.

    Parameters
    ----------
    release_version : str
        The version to rollback (e.g., '0.4.0').
    """
    version_tag = f"v{release_version}"
    console.print(f"[bold yellow]Initiating rollback for release {version_tag}...[/bold yellow]")

    # 1. Delete remote tag
    if questionary.confirm(f"Delete remote tag {version_tag}?", default=True).ask():
        try:
            run_command(["git", "push", "origin", "--delete", version_tag])
            console.print(f"[green]✔[/green] Deleted remote tag {version_tag}")
        except Exception as e:
            console.print(f"[yellow]Could not delete remote tag (might not exist): {e}[/yellow]")

    # 2. Delete local tag
    if questionary.confirm(f"Delete local tag {version_tag}?", default=True).ask():
        try:
            run_command(["git", "tag", "-d", version_tag])
            console.print(f"[green]✔[/green] Deleted local tag {version_tag}")
        except Exception as e:
            console.print(f"[yellow]Could not delete local tag (might not exist): {e}[/yellow]")

    # 3. Check for release commit
    try:
        res = subprocess.run(["git", "log", "-1", "--pretty=%s"], capture_output=True, text=True, check=True)
        commit_msg = res.stdout.strip()
        expected_msg = f"chore: release version {release_version}"
        if commit_msg == expected_msg:
            if questionary.confirm(f"Found release commit '{commit_msg}'. Reset this commit and keep modified files?", default=True).ask():
                run_command(["git", "reset", "HEAD~1"])
                console.print("[green]✔[/green] Reset the last git commit.")
    except Exception as e:
        console.print(f"[yellow]Could not inspect git commit or reset: {e}[/yellow]")

@app.command
def trigger(
    bump: str = None,
    version: str = None,
    yes: bool = False,
):
    """Trigger the Release workflow on GitHub Actions from the local CLI.

    This is an alternative to opening the GitHub web UI. The workflow runs on
    GitHub's infrastructure, bumps versions, builds, publishes to Maven Central,
    creates the GitHub release, and deploys documentation.

    Parameters
    ----------
    bump : str, optional
        The type of version bump ('auto', 'patch', 'minor', 'major', 'custom')
        or a specific version string (e.g., '1.2.3'). If omitted, starts wizard
        mode.
    version : str, optional
        Required when bump is 'custom' or when passing a specific version
        string directly.
    yes : bool, optional
        Skip all interactive prompts. Defaults to 'auto' bump strategy.
    """
    try:
        current_version = get_current_version()
    except Exception as e:
        console.print(f"[bold red]Error reading current version:[/bold red] {e}")
        sys.exit(1)

    console.print(f"Current version: [bold cyan]{current_version}[/bold cyan]")

    # Verify the GitHub CLI is available and authenticated
    try:
        subprocess.run(["gh", "auth", "status"], capture_output=True, check=True)
    except (subprocess.CalledProcessError, FileNotFoundError):
        console.print("[bold red]GitHub CLI (`gh`) is required and must be authenticated.[/bold red]")
        console.print("[dim]Install from https://cli.github.com/ and run `gh auth login`.[/dim]")
        sys.exit(1)

    if bump is None:
        strategy, custom_version = prompt_bump(current_version, yes)
    else:
        bump_clean = bump.strip().lower()
        if re.match(r"^\d+\.\d+\.\d+.*$", bump):
            strategy = "custom"
            custom_version = bump
        elif bump_clean in {"auto", "patch", "minor", "major", "custom"}:
            strategy = bump_clean
            custom_version = version
        else:
            console.print(
                f"[bold red]Invalid bump strategy: '{bump}'. "
                f"Choose 'auto', 'patch', 'minor', 'major', 'custom', or a specific version.[/bold red]"
            )
            sys.exit(1)

        if strategy == "custom" and not custom_version:
            console.print("[bold red]--version is required when --bump is 'custom'.[/bold red]")
            sys.exit(1)

    # Determine the branch to trigger the workflow on
    try:
        branch_result = subprocess.run(
            ["git", "rev-parse", "--abbrev-ref", "HEAD"],
            capture_output=True, text=True, check=True
        )
        current_branch = branch_result.stdout.strip()
    except Exception as e:
        console.print(f"[bold red]Could not determine current git branch:[/bold red] {e}")
        sys.exit(1)

    if current_branch != "main":
        console.print(
            f"[yellow]Warning: you are on branch '{current_branch}', not 'main'.[/yellow]"
        )

    cmd = [
        "gh", "workflow", "run", "release.yml",
        "--ref", current_branch,
        "-f", f"bump={strategy}",
    ]
    if strategy == "custom":
        cmd.extend(["-f", f"version={custom_version}"])

    proceed = yes or questionary.confirm(
        f"Trigger Release workflow on '{current_branch}' with bump '{strategy}'?"
    ).ask()
    if not proceed:
        console.print("[yellow]Release aborted.[/yellow]")
        sys.exit(0)

    console.print(f"[bold blue]Running:[/bold blue] {' '.join(cmd)}")
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        console.print(result.stderr or result.stdout)
        raise subprocess.CalledProcessError(result.returncode, cmd)
    if result.stdout:
        console.print(result.stdout)
    if result.stderr:
        console.print(result.stderr)

    console.print(
        f"[bold green]Successfully triggered Release workflow on {current_branch}.[/bold green]"
    )

    # Extract the run ID from the GitHub CLI output so we can offer live monitoring.
    run_id = None
    output = str(result.stdout or "") + str(result.stderr or "")
    for line in output.splitlines():
        match = re.search(r"/actions/runs/(\d+)", line)
        if match:
            run_id = match.group(1)
            break
        match = re.search(r"gh run view (\d+)", line)
        if match:
            run_id = match.group(1)
            break

    if run_id and not yes:
        if questionary.confirm(f"Monitor run {run_id} now?").ask():
            monitor(run_id=run_id)

@app.command
def monitor(run_id: str = None):
    """Monitor a Release workflow run in real time.

    Streams the live status of the most recent release.yml run, or the run
    specified by run_id, until it completes.

    Parameters
    ----------
    run_id : str, optional
        The GitHub Actions run ID to watch. If omitted, the latest release.yml
        run is used.
    """
    try:
        subprocess.run(["gh", "auth", "status"], capture_output=True, check=True)
    except (subprocess.CalledProcessError, FileNotFoundError):
        console.print("[bold red]GitHub CLI (`gh`) is required and must be authenticated.[/bold red]")
        sys.exit(1)

    if not run_id:
        try:
            result = subprocess.run(
                ["gh", "run", "list", "--workflow=release.yml", "--json", "databaseId", "--jq", ".[0].databaseId"],
                capture_output=True, text=True, check=True
            )
            run_id = result.stdout.strip()
        except subprocess.CalledProcessError as e:
            console.print(f"[bold red]Could not find a recent release.yml run:[/bold red] {e.stderr or e}")
            sys.exit(1)

    if not run_id:
        console.print("[yellow]No recent release.yml runs found.[/yellow]")
        sys.exit(0)

    console.print(f"[dim]Monitoring run {run_id}...[/dim]")
    run_command(["gh", "run", "watch", run_id])

@app.default
def main(
    bump: str = None,
    dry_run: bool = True,
    push: bool = True,
    yes: bool = False,
    publish: bool = False,
):
    """Automate the release process by bumping versions, updating changelogs, verifying,
    optionally publishing, and tagging.

    Parameters
    ----------
    bump : str, optional
        The type of version bump ('major', 'minor', 'patch', 'auto') or a specific version string.
        If omitted, starts wizard mode.
    dry_run : bool, optional
        Run the build/verification task before publishing.
    push : bool, optional
        Commit, tag, and push changes to git.
    yes : bool, optional
        Skip all interactive prompts and use defaults. Useful for CI.
    publish : bool, optional
        Publish to Maven Central after verification.
    """
    try:
        current_version = get_current_version()
    except Exception as e:
        console.print(f"[bold red]Error reading current version:[/bold red] {e}")
        sys.exit(1)

    console.print(f"Current version: [bold cyan]{current_version}[/bold cyan]")

    # Wizard Mode
    if bump is None:
        strategy, custom_version = prompt_bump(current_version, yes)
        if strategy == "auto":
            bump, new_version = get_next_version(current_version)
        elif strategy == "custom":
            bump = custom_version
            try:
                new_version = bump_version(current_version, custom_version)
            except Exception as e:
                console.print(f"[bold red]Error calculating new version:[/bold red] {e}")
                sys.exit(1)
        else:
            bump = strategy
            try:
                new_version = bump_version(current_version, strategy)
            except Exception as e:
                console.print(f"[bold red]Error calculating new version:[/bold red] {e}")
                sys.exit(1)
    elif bump.strip().lower() == "auto":
        bump, new_version = get_next_version(current_version)
    else:
        try:
            new_version = bump_version(current_version, bump)
        except Exception as e:
            console.print(f"[bold red]Error calculating new version:[/bold red] {e}")
            sys.exit(1)

    console.print(f"Target release version: [bold green]{new_version}[/bold green]")

    proceed = yes or questionary.confirm("Do you want to proceed with the release?").ask()
    if not proceed:
        console.print("[yellow]Release aborted.[/yellow]")
        sys.exit(0)

    actions_taken = []
    try:
        # 1. Update Version Numbers, Changelog, and Docs
        console.print("\n[bold]1. Updating version numbers, changelog, and docs...[/bold]")
        actions_taken.append("files_modified")
        update_gradle_properties(new_version)
        update_pyproject_toml(new_version)
        update_changelog(new_version)
        update_docs_version_snippets(new_version)
        validate_docs_version_snippets(new_version)
        validate_changelog_release_section(new_version)

        # 2. Run uv lock to update lock file
        console.print("\n[bold]2. Updating uv.lock...[/bold]")
        run_command(["uv", "lock"])

        # 3. Build javadoc JARs and validate release artifacts before publishing
        console.print("\n[bold]3. Building and validating javadoc JARs...[/bold]")
        env = os.environ.copy()
        java_home = os.environ.get("JAVA_HOME", "/usr/lib/jvm/java-21-openjdk")
        env["JAVA_HOME"] = java_home
        run_command(
            ["./gradlew", "javadocJar", "--no-daemon"],
            env=env,
        )
        validate_javadoc_jars(new_version)

        # 4. Dry-Run Verification
        if dry_run:
            console.print("\n[bold]4. Dry-Run Verification...[/bold]")
            # Skip prompt in automated or CI mode
            should_verify = bump is not None or yes or questionary.confirm("Do you want to run dry-run build verification?", default=True).ask()
            if should_verify:
                run_command(
                    [
                        "./gradlew",
                        "spotlessCheck",
                        "build",
                        "--no-daemon",
                    ],
                    env=env,
                )

        # 5. Commit and tag locally
        if push:
            console.print("\n[bold]5. Git commit and tag...[/bold]")
            # Detect current branch
            try:
                branch_result = subprocess.run(
                    ["git", "rev-parse", "--abbrev-ref", "HEAD"],
                    capture_output=True, text=True, check=True
                )
                current_branch = branch_result.stdout.strip()
            except Exception:
                current_branch = "main"  # fallback to main

            should_commit = yes or questionary.confirm(f"Commit, tag as v{new_version}, and push to remote?", default=True).ask()
            if not should_commit:
                console.print("[yellow]Release aborted.[/yellow]")
                sys.exit(0)

            run_command(["git", "add", "gradle.properties", "pyproject.toml", "CHANGELOG.md", "uv.lock", "docs/index.md", "docs/content"])

            run_command(["git", "commit", "-m", f"chore: release version {new_version}"])
            actions_taken.append("committed")

            run_command(["git", "tag", f"v{new_version}"])
            actions_taken.append("tagged")

        # 6. Publish to Maven Central
        if publish:
            console.print("\n[bold]6. Publishing to Maven Central...[/bold]")
            env = os.environ.copy()
            java_home = os.environ.get("JAVA_HOME", "/usr/lib/jvm/java-21-openjdk")
            env["JAVA_HOME"] = java_home
            run_command(["./gradlew", "validatePublishing", "--no-daemon"], env=env)
            run_command(["./gradlew", "publishToCentral", "--no-daemon"], env=env)

        # 7. Push commit and tag
        if push:
            console.print("\n[bold]7. Pushing branch and tag...[/bold]")
            actions_taken.append("pushed")
            run_command(["git", "push", "origin", current_branch, "--tags"])

            console.print(f"\n[bold green]Successfully released v{new_version}![/bold green]")

    except (FileNotFoundError, ValueError, subprocess.CalledProcessError, OSError) as e:
        console.print(f"\n[bold red]Error occurred during release process: {e}[/bold red]")
        console.print("[bold yellow]Initiating automatic rollback...[/bold yellow]")

        rollback_failed = False

        # Rollback in reverse order: remote tag -> local tag -> commit -> files
        if "pushed" in actions_taken:
            try:
                subprocess.run(["git", "push", "origin", "--delete", f"v{new_version}"], check=True)
                console.print("[green]✔[/green] Rolled back remote tag")
            except Exception as re_err:
                console.print(f"[red]Failed to delete remote tag: {re_err}[/red]")
                console.print(f"[yellow]Manual Step Needed: Run 'git push origin --delete v{new_version}'[/yellow]")
                rollback_failed = True

        if "tagged" in actions_taken:
            try:
                subprocess.run(["git", "tag", "-d", f"v{new_version}"], check=True)
                console.print("[green]✔[/green] Rolled back local tag")
            except Exception as re_err:
                console.print(f"[red]Failed to delete local tag: {re_err}[/red]")
                console.print(f"[yellow]Manual Step Needed: Run 'git tag -d v{new_version}'[/yellow]")
                rollback_failed = True

        if "committed" in actions_taken:
            try:
                subprocess.run(["git", "reset", "--hard", "HEAD~1"], check=True)
                console.print("[green]✔[/green] Reset commit")
            except Exception as re_err:
                console.print(f"[red]Failed to reset commit: {re_err}[/red]")
                console.print(f"[yellow]Manual Step Needed: Run 'git reset --hard HEAD~1'[/yellow]")
                rollback_failed = True

        if "files_modified" in actions_taken and "committed" not in actions_taken:
            try:
                subprocess.run(
                    ["git", "restore", "gradle.properties", "pyproject.toml", "CHANGELOG.md", "docs/index.md", "docs/content"],
                    check=True
                )
                # Try to restore uv.lock if it exists in git
                try:
                    subprocess.run(["git", "restore", "uv.lock"], check=True)
                except Exception:
                    # uv.lock might not be tracked, that's okay
                    pass
                console.print("[green]✔[/green] Restored modified files")
            except Exception as re_err:
                console.print(f"[red]Failed to restore modified files: {re_err}[/red]")
                console.print(f"[yellow]Manual Step Needed: Run 'git restore gradle.properties pyproject.toml CHANGELOG.md docs/index.md docs/content'[/yellow]")
                rollback_failed = True

        if rollback_failed:
            console.print("\n[bold red]Automatic rollback was only partially successful. Please complete the manual step(s) listed above.[/bold red]")
        else:
            console.print("\n[bold green]Automatic rollback completed successfully. Repository has been restored to its pre-release state.[/bold green]")

        sys.exit(1)

if __name__ == "__main__":
    app()

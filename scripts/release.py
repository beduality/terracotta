#!/usr/bin/env python3
import os
import re
import sys
import subprocess
from datetime import date
from pathlib import Path
from cyclopts import App
from rich.console import Console
import questionary

console = Console()
app = App(
    usage="release.py [BUMP] [OPTIONS]\n       release.py rollback <version>"
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
    
    new_header = f"## [{new_version}] - {today}"
    new_content = content.replace("## [Unreleased]", new_header, 1)
    path.write_text(new_content)
    console.print("[green]✔[/green] Updated CHANGELOG.md")

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

@app.default
def main(
    bump: str = None,
    dry_run: bool = True,
    push: bool = True,
):
    """Automate the release process by bumping versions, updating changelogs, dry-running publications, and tagging.

    Parameters
    ----------
    bump : str, optional
        The type of version bump ('major', 'minor', 'patch') or a specific version string.
        If omitted, starts wizard mode.
    dry_run : bool, optional
        Run the dry-run publish verification task.
    push : bool, optional
        Commit, tag, and push changes to git.
    """
    try:
        current_version = get_current_version()
    except Exception as e:
        console.print(f"[bold red]Error reading current version:[/bold red] {e}")
        sys.exit(1)
        
    console.print(f"Current version: [bold cyan]{current_version}[/bold cyan]")
    
    # Wizard Mode
    if bump is None:
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

        if choice == "auto":
            bump = auto_bump
        elif choice == "custom":
            bump = questionary.text("Enter custom version (e.g., 0.5.0)").ask()
            if bump is None:
                console.print("[yellow]Release aborted.[/yellow]")
                sys.exit(0)
        else:
            bump = choice
            
    try:
        new_version = bump_version(current_version, bump)
    except Exception as e:
        console.print(f"[bold red]Error calculating new version:[/bold red] {e}")
        sys.exit(1)
        
    console.print(f"Target release version: [bold green]{new_version}[/bold green]")
    
    proceed = questionary.confirm("Do you want to proceed with the release?").ask()
    if not proceed:
        console.print("[yellow]Release aborted.[/yellow]")
        sys.exit(0)
        
    actions_taken = []
    try:
        # 1. Update Version Numbers and Changelog
        console.print("\n[bold]1. Updating version numbers & changelog...[/bold]")
        update_gradle_properties(new_version)
        update_pyproject_toml(new_version)
        update_changelog(new_version)
        actions_taken.append("files_modified")
            
        # 2. Run uv lock to update lock file
        console.print("\n[bold]2. Updating uv.lock...[/bold]")
        run_command(["uv", "lock"])
        
        # 3. Dry-Run Verification
        if dry_run:
            console.print("\n[bold]3. Dry-Run Verification...[/bold]")
            if questionary.confirm("Do you want to run dry-run publication verification?", default=True).ask():
                env = os.environ.copy()
                env["DRY_RUN"] = "true"
                env["JAVA_HOME"] = "/usr/lib/jvm/java-21-openjdk"
                run_command(
                    [
                        "./gradlew",
                        "publishPluginPublicationToHangar",
                        "modrinth",
                        "--no-daemon",
                    ],
                    env=env,
                )
                
        # 4. Commit and Push Tag
        if push:
            console.print("\n[bold]4. Git Tag & Push...[/bold]")
            if questionary.confirm(f"Commit, tag as v{new_version}, and push to remote?", default=True).ask():
                run_command(["git", "add", "gradle.properties", "pyproject.toml", "CHANGELOG.md", "uv.lock"])
                
                run_command(["git", "commit", "-m", f"chore: release version {new_version}"])
                actions_taken.append("committed")
                
                run_command(["git", "tag", f"v{new_version}"])
                actions_taken.append("tagged")
                
                run_command(["git", "push", "origin", "main", "--tags"])
                actions_taken.append("pushed")
                
                console.print(f"\n[bold green]Successfully released v{new_version}![/bold green]")
                
    except Exception as e:
        console.print(f"\n[bold red]Error occurred during release process: {e}[/bold red]")
        console.print("[bold yellow]Initiating automatic rollback...[/bold yellow]")
        
        rollback_failed = False
        
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
                subprocess.run(["git", "reset", "HEAD~1"], check=True)
                console.print("[green]✔[/green] Reset commit")
            except Exception as re_err:
                console.print(f"[red]Failed to reset commit: {re_err}[/red]")
                console.print("[yellow]Manual Step Needed: Run 'git reset HEAD~1'[/yellow]")
                rollback_failed = True
                
        if "files_modified" in actions_taken and "committed" not in actions_taken:
            try:
                subprocess.run(
                    ["git", "checkout", "gradle.properties", "pyproject.toml", "CHANGELOG.md", "uv.lock"],
                    check=True
                )
                console.print("[green]✔[/green] Restored modified files")
            except Exception as re_err:
                console.print(f"[red]Failed to restore modified files: {re_err}[/red]")
                console.print("[yellow]Manual Step Needed: Run 'git restore gradle.properties pyproject.toml CHANGELOG.md uv.lock'[/yellow]")
                rollback_failed = True
                
        if rollback_failed:
            console.print("\n[bold red]Automatic rollback was only partially successful. Please complete the manual step(s) listed above.[/bold red]")
        else:
            console.print("\n[bold green]Automatic rollback completed successfully. Repository has been restored to its pre-release state.[/bold green]")
            
        sys.exit(1)

if __name__ == "__main__":
    app()

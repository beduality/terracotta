#!/usr/bin/env python3
"""Load secrets from .env into Pulumi config, auto-detecting which secrets
are referenced by the Pulumi GitHub infra module (App.kt)."""

import base64
import os
import re
import subprocess
import sys
from pathlib import Path


def find_env_file() -> Path:
    """Locate the .env file relative to the project root."""
    candidates = [
        Path(".env"),
        Path("../../.env"),
    ]
    for candidate in candidates:
        if candidate.exists():
            return candidate
    print("Error: No .env file found")
    sys.exit(1)


def load_env(env_file: Path) -> dict[str, str]:
    """Parse a .env file into a dict, ignoring comments and blank lines."""
    env = {}
    lines = env_file.read_text().splitlines()
    i = 0
    while i < len(lines):
        line = lines[i].strip()
        if not line or line.startswith("#"):
            i += 1
            continue
        if "=" not in line:
            i += 1
            continue
        key, _, value = line.partition("=")
        key = key.strip()
        value = value.strip()
        # Handle multiline quoted values
        if value.startswith('"') and not value.endswith('"'):
            # Collect lines until closing quote
            parts = [value[1:]]  # strip opening quote
            i += 1
            while i < len(lines):
                if lines[i].endswith('"'):
                    parts.append(lines[i][:-1])  # strip closing quote
                    break
                parts.append(lines[i])
                i += 1
            value = "\n".join(parts)
        elif value.startswith('"') and value.endswith('"'):
            value = value[1:-1]
        elif value.startswith("'") and value.endswith("'"):
            value = value[1:-1]
        env[key] = value
        i += 1
    return env


def detect_secrets_from_app_kt() -> list[str]:
    """Extract the secrets list from the Pulumi App.kt source."""
    app_kt = Path(__file__).parent.parent / "modules" / "terracotta-github" / "src" / "main" / "kotlin" / "io" / "github" / "beduality" / "terracotta" / "github" / "App.kt"

    if not app_kt.exists():
        print(f"Warning: Could not find {app_kt}, falling back to empty list")
        return []

    content = app_kt.read_text()

    # Match the listOf(...) block assigned to `val secrets`
    match = re.search(
        r'val\s+secrets\s*=\s*listOf\s*\((.*?)\)',
        content,
        re.DOTALL,
    )
    if not match:
        print("Warning: Could not parse secrets list from App.kt")
        return []

    # Extract quoted strings from the matched block
    return re.findall(r'"([A-Z_]+)"', match.group(1))


def to_camel_case(name: str) -> str:
    """Convert UPPER_SNAKE_CASE to camelCase (e.g. SONATYPE_USERNAME -> sonatypeUsername)."""
    parts = name.lower().split("_")
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def decode_signing_key(value: str) -> str:
    """If the signing key looks base64-encoded, decode it."""
    if "BEGIN PGP" in value:
        return value
    try:
        decoded = base64.b64decode(value).decode("utf-8")
        print("Decoded base64 SIGNING_KEY")
        return decoded
    except Exception:
        return value


def get_pulumi_project_dir() -> Path:
    """Get the path to the Pulumi project directory."""
    return Path(__file__).parent.parent / "modules" / "terracotta-github"


def main():
    env_file = find_env_file()
    print(f"Loading variables from {env_file}...")
    env = load_env(env_file)

    secrets = detect_secrets_from_app_kt()
    if not secrets:
        print("No secrets detected from App.kt")
        sys.exit(1)

    print(f"Detected secrets: {', '.join(secrets)}")

    pulumi_dir = get_pulumi_project_dir()
    if not (pulumi_dir / "Pulumi.yaml").exists():
        print(f"Error: Pulumi.yaml not found in {pulumi_dir}")
        sys.exit(1)

    for secret_name in secrets:
        config_key = to_camel_case(secret_name)
        value = env.get(secret_name) or os.environ.get(secret_name)

        if not value:
            print(f"Warning: {secret_name} is not set (skipping)")
            continue

        if secret_name == "SIGNING_KEY":
            value = decode_signing_key(value)

        result = subprocess.run(
            ["pulumi", "config", "set", "--secret", config_key, "--", value],
            capture_output=True,
            text=True,
            cwd=pulumi_dir,
        )

        if result.returncode == 0:
            print(f"Successfully set secret: {config_key}")
        else:
            print(f"Error setting {config_key}: {result.stderr.strip()}")
            sys.exit(1)


if __name__ == "__main__":
    main()

import re
import os
import subprocess
from pathlib import Path

root = Path(__file__).resolve().parent.parent

def get_versions():
    try:
        result = subprocess.run(
            ["git", "tag"],
            cwd=root,
            capture_output=True,
            text=True,
            check=True
        )
        tags = [t.strip() for t in result.stdout.splitlines() if t.strip()]
    except Exception:
        tags = []

    semver_tags = [t for t in tags if re.match(r"^v\d+\.\d+\.\d+$", t)]
    
    # Sort tags semantically: e.g. v0.1.0 < v0.2.0 < v0.10.0
    def semver_key(tag):
        return [int(c) for c in tag[1:].split('.')]
    
    semver_tags.sort(key=semver_key)

    versions_dict = {}
    for i, tag in enumerate(semver_tags):
        version_name = tag[1:]
        is_latest = (i == len(semver_tags) - 1)
        versions_dict[tag] = (version_name, "latest" if is_latest else None)

    versions_dict["main"] = ("unreleased", "Unreleased")
    return versions_dict

versions = get_versions()

def run(cmd, env=None):
    print(f"Running: {' '.join(cmd)}")
    subprocess.run(cmd, cwd=root, env=env, check=True)

def main():
    try:
        for tag, (version_name, alias) in versions.items():
            print(f"\n==================== PROCESSING {tag} ====================")
            run(["git", "reset", "--hard"])
            run(["git", "checkout", tag])
            run(["git", "clean", "-fdx", "-e", ".venv", "-e", ".gradle", "-e", ".env"])
            run(["git", "checkout", "main", "--", "mkdocs.yml", "pyproject.toml", "uv.lock", "docs/hooks/"])
            
            # Build KDocs
            env = os.environ.copy()
            env["JAVA_HOME"] = "/usr/lib/jvm/java-21-openjdk"
            run(["./gradlew", "clean", "dokkaHtmlMultiModule", "--no-daemon"], env=env)
            
            # Deploy with mike
            if tag == "main":
                run(["uv", "run", "mike", "deploy", "-t", "Unreleased", "unreleased"])
            else:
                if alias:
                    run(["uv", "run", "mike", "deploy", version_name, alias])
                else:
                    run(["uv", "run", "mike", "deploy", version_name])
                    
        run(["uv", "run", "mike", "set-default", "latest"])
        
    finally:
        run(["git", "reset", "--hard"])
        run(["git", "checkout", "main"])

if __name__ == "__main__":
    main()

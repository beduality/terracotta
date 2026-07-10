import os
import re
import shutil
from pathlib import Path

def get_current_version():
    properties_path = Path("gradle.properties")
    if not properties_path.exists():
        return None
    content = properties_path.read_text()
    match = re.search(r"^version\s*=\s*([^\s]+)", content, re.MULTILINE)
    if match:
        return match.group(1).strip()
    return None

def on_pre_build(config):
    source = 'CHANGELOG.md'
    target = os.path.join(config['docs_dir'], 'changes.md')
    
    if not os.path.exists(source):
        print(f"Warning: {source} not found.")
        return
        
    changelog_content = Path(source).read_text()
    version = get_current_version()
    
    # Check if we are being built by mike (mike sets MIKE_DOCS_VERSION)
    mike_version = os.environ.get("MIKE_DOCS_VERSION")
    
    target_header = None
    if mike_version == "unreleased":
        target_header = "## [Unreleased]"
        if target_header not in changelog_content:
            fallback_content = (
                "# Changes\n\n"
                "## Unreleased\n\n"
                "No unreleased changes have been documented yet for the next release.\n"
            )
            Path(target).write_text(fallback_content)
            print(f"No Unreleased section found. Wrote fallback to {target}")
            return
    else:
        v = mike_version if mike_version else version
        target_header = f"## [{v}]"
        
    if target_header not in changelog_content:
        # Fallback to current version in gradle.properties
        target_header = f"## [{version}]"
        
    if target_header not in changelog_content:
        # Fallback to full changelog if version header still not found
        shutil.copy(source, target)
        print(f"Copied full CHANGELOG.md to {target} (version header {target_header} not found)")
        return
        
    # Extract only this version's section
    start_idx = changelog_content.find(target_header)
    remaining = changelog_content[start_idx + len(target_header):]
    next_heading_match = re.search(r"\n##\s+\[", remaining)
    
    if next_heading_match:
        end_idx = start_idx + len(target_header) + next_heading_match.start()
        section = changelog_content[start_idx:end_idx].strip()
    else:
        section = changelog_content[start_idx:].strip()
        
    # Write to target changes.md
    Path(target).write_text(f"# Changes\n\n{section}\n")
    print(f"Extracted {target_header} changes and wrote to {target}")

import json
import re
from pathlib import Path

from jinja2 import Environment, FileSystemLoader


def define_env(env):
    """Defines macros and hooks for the documentation."""

    @env.macro
    def reading_time(text: str) -> str:
        """Estimate reading time for the given text."""
        words = len(text.split())
        minutes = max(1, round(words / 200))
        return f"{minutes} min read"

    @env.macro
    def deployments():
        """Render the Last Changes page from an external Jinja2 template."""
        entries = _load_deployments()
        all_modules = _discover_modules()

        discovered_ids = {mid for mid, _, _ in all_modules}
        data_module_ids = set()
        for entry in entries:
            data_module_ids.update(entry.get("modules", []))
        for mid in sorted(data_module_ids - discovered_ids):
            label = MODULE_LABELS.get(mid, mid.replace("-", " ").title())
            icon = MODULE_ICONS.get(mid)
            all_modules.append((mid, label, icon))

        module_icons = {
            mid: _load_icon_svg(icon) for mid, _, icon in all_modules if icon
        }
        module_labels = {mid: label for mid, label, _ in all_modules}

        templates_dir = Path(__file__).parent / "templates"
        jinja_env = Environment(
            loader=FileSystemLoader(str(templates_dir)),
            autoescape=False,
        )
        template = jinja_env.get_template("deployments.j2")
        return template.render(
            deployments_json=json.dumps(entries, ensure_ascii=False),
            all_modules=all_modules,
            module_icons=module_icons,
            module_labels=module_labels,
            module_icons_json=json.dumps(module_icons, ensure_ascii=False),
        )


def on_post_page_macros(env):
    """Compute reading time after macros are rendered and expose it in page meta."""
    text = env.page.markdown or ""
    words = len(text.split())
    minutes = max(1, round(words / 200))
    env.page.meta["reading_time"] = f"{minutes} min read"


def _load_deployments():
    path = Path("deployments.json")
    if not path.exists():
        return []
    data = json.loads(path.read_text(encoding="utf-8"))
    deployments = data.get("deployments", [])
    for entry in deployments:
        modules = entry.get("modules", [])
        entry["modules"] = [MODULE_ALIASES.get(m, m) for m in modules]
    return deployments


MODULE_ALIASES = {
    "release-pipeline": "repo",
}

MODULE_LABELS = {
    "github": "GitHub",
    "modrinth": "Modrinth",
    "hangar": "Hangar",
    "repo": "Repository",
    "docs": "Docs",
}

MODULE_ICONS = {
    "core": "material/engine",
    "github": "fontawesome/brands/github",
    "modrinth": "brands/modrinth",
    "hangar": "brands/hangar",
    "gradle-plugin": "brands/gradle",
    "state-filesystem": "material/file",
    "repo": "material/source-repository",
    "docs": "material/book",
}


def _load_icon_svg(icon_path: str) -> dict | None:
    """Load an SVG icon's inner content and viewBox from the Material theme or overrides."""
    if not icon_path:
        return None
    candidates = [
        Path("docs/overrides/.icons") / f"{icon_path}.svg",
        Path(".venv/lib/python3.13/site-packages/material/templates/.icons") / f"{icon_path}.svg",
    ]
    for path in candidates:
        if path.exists():
            svg = path.read_text(encoding="utf-8")
            vb_match = re.search(r'viewBox="([^"]+)"', svg)
            viewBox = vb_match.group(1) if vb_match else "0 0 24 24"
            start = svg.find(">") + 1
            end = svg.rfind("</svg>")
            return {"inner": svg[start:end].strip(), "viewBox": viewBox}
    return None


def _discover_modules():
    """Scan modules/ for terracotta-* directories and return (id, label, icon) triples."""
    modules = []
    seen = set()
    modules_dir = Path("modules")
    if modules_dir.is_dir():
        for child in sorted(modules_dir.iterdir()):
            if not child.is_dir() or not child.name.startswith("terracotta-"):
                continue
            canonical = child.name.removeprefix("terracotta-")
            if canonical.startswith("provider-"):
                canonical = canonical.removeprefix("provider-")
            if canonical in seen:
                continue
            seen.add(canonical)
            label = MODULE_LABELS.get(canonical, canonical.replace("-", " ").title())
            icon = MODULE_ICONS.get(canonical)
            modules.append((canonical, label, icon))
    return modules

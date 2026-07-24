import json
import unittest
from pathlib import Path

from jinja2 import Environment, FileSystemLoader

TEMPLATES_DIR = Path(__file__).resolve().parent.parent / "docs" / "templates"

SAMPLE_ENTRIES = [
    {
        "version": "0.8.0",
        "createdAt": "2026-07-13T02:48:13Z",
        "title": "Stable Gallery Identity",
        "summary": "Gallery items now use persisted stable keys.",
        "modules": ["core", "modrinth"],
        "isRelease": False,
    },
    {
        "createdAt": "2026-07-22T00:00:00Z",
        "title": "Docs Site Overhaul",
        "summary": "Reworked the Last Changes page.",
        "modules": ["docs"],
        "isRelease": False,
    },
]

SAMPLE_MODULES = [
    ("core", "Core", "material/engine"),
    ("modrinth", "Modrinth", "brands/modrinth"),
    ("docs", "Docs", "material/book"),
]

SAMPLE_ICONS = {
    "core": {"inner": "<path d='core'/>", "viewBox": "0 0 24 24"},
    "modrinth": {"inner": "<path d='modrinth'/>", "viewBox": "0 0 24 24"},
    "docs": {"inner": "<path d='docs'/>", "viewBox": "0 0 24 24"},
}

SAMPLE_LABELS = {mid: label for mid, label, _ in SAMPLE_MODULES}


def render_template() -> str:
    env = Environment(loader=FileSystemLoader(str(TEMPLATES_DIR)), autoescape=False)
    template = env.get_template("deployments.j2")
    return template.render(
        deployments_json=json.dumps(SAMPLE_ENTRIES, ensure_ascii=False),
        all_modules=SAMPLE_MODULES,
        module_icons=SAMPLE_ICONS,
        module_labels=SAMPLE_LABELS,
        module_icons_json=json.dumps(SAMPLE_ICONS, ensure_ascii=False),
    )


class TestCardLinkStructure(unittest.TestCase):
    """Verify the whole deployment card is clickable via a stretched link."""

    def setUp(self):
        self.html = render_template()

    def test_card_has_stretched_link_class(self):
        self.assertIn("mdx-card-link", self.html)

    def test_stretched_link_binds_release_url(self):
        self.assertIn(
            "https://github.com/beduality/terracotta/releases/tag/v'+d.version",
            self.html,
        )

    def test_stretched_link_shown_only_when_version_exists(self):
        self.assertIn('x-show="d.version"', self.html)

    def test_version_badge_is_not_anchor(self):
        self.assertNotIn('<a x-show="d.version" class="mdx-version"', self.html)

    def test_module_links_are_separate_anchors(self):
        self.assertIn('class="mdx-link"', self.html)
        self.assertIn("modrinth.com", self.html)

    def test_module_links_have_elevated_zindex_class(self):
        self.assertIn("mdx-link-above", self.html)


if __name__ == "__main__":
    unittest.main()

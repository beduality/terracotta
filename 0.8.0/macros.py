import re


def define_env(env):
    """Defines macros and hooks for the documentation."""

    @env.macro
    def reading_time(text: str) -> str:
        """Estimate reading time for the given text."""
        words = len(text.split())
        minutes = max(1, round(words / 200))
        return f"{minutes} min read"


def on_post_page_macros(env):
    """Compute reading time after macros are rendered and expose it in page meta."""
    text = env.page.markdown or ""
    words = len(text.split())
    minutes = max(1, round(words / 200))
    env.page.meta["reading_time"] = f"{minutes} min read"

package io.github.beduality.terracotta.core.model.projectfile.convention

/**
 * Terracotta's default README convention.
 *
 * The full trimmed content is the description, and the first non-empty,
 * non-heading paragraph is the summary.
 *
 * @see [Conventions reference](https://beduality.github.io/terracotta/content/core/reference/conventions.html)
 * @see [Add a project-file convention guide](https://beduality.github.io/terracotta/content/core/how-to-guides/add-a-new-project-file-convention.html)
 */
object TerracottaReadmeConvention : ReadmeConvention {
    /** Resolves metadata. */
    override fun resolve(id: String): ReadmeConvention? = if (id.equals("terracotta", ignoreCase = true)) this else null

    override fun extractDescription(content: String): String? = content.trim().takeIf { it.isNotEmpty() }

    override fun extractSummary(content: String): String? {
        val paragraph =
            content
                .replace(Regex("""^#{1,6}\s.*$""", RegexOption.MULTILINE), "")
                .trim()
                .lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .firstOrNull()
        return paragraph?.takeIf { it.isNotEmpty() }
    }
}

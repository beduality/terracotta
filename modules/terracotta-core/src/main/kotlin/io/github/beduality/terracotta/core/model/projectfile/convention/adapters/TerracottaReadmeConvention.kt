package io.github.beduality.terracotta.core.model.projectfile.convention

/**
 * Terracotta's default README convention.
 *
 * The full trimmed content is the description, and the first non-empty,
 * non-heading paragraph is the summary.
 */
object TerracottaReadmeConvention : ReadmeConvention {
    override fun resolve(id: String): ReadmeConvention? =
        if (id.equals("terracotta", ignoreCase = true)) this else null

    override fun extractDescription(content: String): String? =
        content.trim().takeIf { it.isNotEmpty() }

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

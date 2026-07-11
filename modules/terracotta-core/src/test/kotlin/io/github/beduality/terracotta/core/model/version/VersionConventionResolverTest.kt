package io.github.beduality.terracotta.core.model.version

import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class VersionConventionResolverTest {
    @Test
    fun `resolve defaults to SemverVersionConvention`() {
        assertSame(SemverVersionConvention, VersionConventionResolver.versionConvention(null))
    }

    @Test
    fun `resolve returns SemverVersionConvention for semver`() {
        assertSame(SemverVersionConvention, VersionConventionResolver.versionConvention("semver"))
    }

    @Test
    fun `resolve is case insensitive`() {
        assertSame(SemverVersionConvention, VersionConventionResolver.versionConvention("SeMvEr"))
    }

    @Test
    fun `resolve throws for unknown convention`() {
        assertThrows<IllegalArgumentException> {
            VersionConventionResolver.versionConvention("unknown")
        }
    }
}

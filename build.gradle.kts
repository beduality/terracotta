plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.dokka)
    alias(libs.plugins.nexus.publish)
}

allprojects {
    group = "io.github.beduality"
    version = "0.1.0"
}

subprojects {
    apply(plugin = "org.jetbrains.dokka")
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username.set(System.getenv("OSSRH_USERNAME") ?: findProperty("ossrhUsername")?.toString())
            password.set(System.getenv("OSSRH_PASSWORD") ?: findProperty("ossrhPassword")?.toString())
        }
    }
}

spotless {
    kotlinGradle {
        target("*.gradle.kts", "**/*.gradle.kts")
        ktlint()
    }
}

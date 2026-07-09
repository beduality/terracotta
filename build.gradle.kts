plugins {
    alias(libs.plugins.spotless)
    alias(libs.plugins.dokka)
}

allprojects {
    group = "io.github.beduality"
    version = "0.1.0"
}

subprojects {
    apply(plugin = "org.jetbrains.dokka")
}

spotless {
    kotlinGradle {
        target("*.gradle.kts", "**/*.gradle.kts")
        ktlint()
    }
}

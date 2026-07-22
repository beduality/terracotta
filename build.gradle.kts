import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.dokka)
    jacoco
}

allprojects {
    group = "io.github.beduality"
}

val jacocoVersion = libs.versions.jacoco.get()

subprojects {
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "jacoco")

    tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
        dokkaSourceSets.configureEach {
            jdkVersion.set(17)
        }
    }

    // The Central Portal publisher plugin auto-creates an empty javadocJar for
    // Kotlin projects. Wire it to the Dokka Javadoc output so the published
    // -javadoc.jar is meaningful.
    tasks.withType<Jar>().matching { it.name == "javadocJar" }.configureEach {
        val dokkaJavadoc = tasks.named<org.jetbrains.dokka.gradle.DokkaTask>("dokkaJavadoc")
        from(dokkaJavadoc.flatMap { it.outputDirectory })
        dependsOn(dokkaJavadoc)
    }

    jacoco {
        toolVersion = jacocoVersion
    }

    tasks.withType<Test> {
        finalizedBy(tasks.withType<JacocoReport>())
    }

    tasks.withType<JacocoReport> {
        dependsOn(tasks.withType<Test>())
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }
}

spotless {
    kotlinGradle {
        target("*.gradle.kts", "**/*.gradle.kts")
        ktlint()
    }
}

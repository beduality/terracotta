import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.tasks.testing.Test
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
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

    plugins.withType<MavenPublishPlugin> {
        apply(plugin = "signing")

        configure<SigningExtension> {
            val signingKey = System.getenv("SIGNING_KEY")
            val signingPassword = System.getenv("SIGNING_PASSWORD")
            if (!signingKey.isNullOrBlank()) {
                useInMemoryPgpKeys(signingKey, signingPassword)
                sign(the<PublishingExtension>().publications)
            }
        }

        tasks.withType<PublishToMavenRepository>().configureEach {
            dependsOn(tasks.withType<Sign>())
        }
    }
}

spotless {
    kotlinGradle {
        target("*.gradle.kts", "**/*.gradle.kts")
        ktlint()
    }
}

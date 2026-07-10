import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.dokka)
    alias(libs.plugins.central.portal.publisher)
    `maven-publish`
    jacoco
}

allprojects {
    group = "io.github.beduality"
    version = "0.1.0"
}

val jacocoVersion = libs.versions.jacoco.get()

subprojects {
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "jacoco")

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

centralPublisher {
    credentials {
        username = System.getenv("SONATYPE_USERNAME") ?: findProperty("sonatypeUsername")?.toString() ?: "unset"
        password = System.getenv("SONATYPE_PASSWORD") ?: findProperty("sonatypePassword")?.toString() ?: "unset"
    }

    projectInfo {
        name = "Terracotta"
        description = "Configuration sync and deployment tool for Minecraft servers."
        url = "https://github.com/beduality/terracotta"

        license {
            name = "MIT License"
            url = "https://opensource.org/licenses/MIT"
        }

        developer {
            id = "beduality"
            name = "Block-Entity Duality"
        }

        scm {
            url = "https://github.com/beduality/terracotta"
            connection = "scm:git:git://github.com/beduality/terracotta.git"
            developerConnection = "scm:git:ssh://github.com/beduality/terracotta.git"
        }
    }

    publishing {
        autoPublish = true
        aggregation = true
    }
}

spotless {
    kotlinGradle {
        target("*.gradle.kts", "**/*.gradle.kts")
        ktlint()
    }
}

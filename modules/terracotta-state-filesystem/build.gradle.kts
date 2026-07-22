plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.spotless)
    alias(libs.plugins.central.portal.publisher)
    `maven-publish`
}

fun terracottaCoreDep(): Any {
    val releaseVersion = project.findProperty("terracottaCoreReleaseVersion")?.toString()
    return if (!releaseVersion.isNullOrBlank()) {
        "io.github.beduality:terracotta-core:$releaseVersion"
    } else {
        project(":terracotta-core")
    }
}

dependencies {
    implementation(terracottaCoreDep())
    implementation(libs.kotlin.stdlib)
    implementation(libs.snakeyaml)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint()
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

centralPublisher {
    credentials {
        username = System.getenv("SONATYPE_USERNAME") ?: findProperty("sonatypeUsername")?.toString() ?: "unset"
        password = System.getenv("SONATYPE_PASSWORD") ?: findProperty("sonatypePassword")?.toString() ?: "unset"
    }

    projectInfo {
        name = "Terracotta State Filesystem"
        description = "Filesystem backend for Terracotta state persistence."
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
        aggregation = false
    }
}

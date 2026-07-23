plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.spotless)
    alias(libs.plugins.central.portal.publisher)
    `java-gradle-plugin`
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
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.slf4j.api)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(project(":terracotta-state-filesystem"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

gradlePlugin {
    plugins {
        create("terracotta") {
            id = "io.github.beduality.terracotta"
            implementationClass = "io.github.beduality.terracotta.gradle.TerracottaPlugin"
        }
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

// Ensure all publications (including gradle plugin marker) have required POM metadata
publishing {
    publications.withType<MavenPublication> {
        pom {
            name.set("Terracotta Gradle Plugin")
            description.set("Gradle plugin for Terracotta.")
            url.set("https://github.com/beduality/terracotta")
            licenses {
                license {
                    name.set("MIT License")
                    url.set("https://opensource.org/licenses/MIT")
                }
            }
            developers {
                developer {
                    id.set("beduality")
                    name.set("Block-Entity Duality")
                }
            }
            scm {
                url.set("https://github.com/beduality/terracotta")
                connection.set("scm:git:git://github.com/beduality/terracotta.git")
                developerConnection.set("scm:git:ssh://github.com/beduality/terracotta.git")
            }
        }
    }
}

centralPublisher {
    credentials {
        username = System.getenv("SONATYPE_USERNAME") ?: findProperty("sonatypeUsername")?.toString() ?: "unset"
        password = System.getenv("SONATYPE_PASSWORD") ?: findProperty("sonatypePassword")?.toString() ?: "unset"
    }

    projectInfo {
        name = "Terracotta Gradle Plugin"
        description = "Gradle plugin for Terracotta."
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

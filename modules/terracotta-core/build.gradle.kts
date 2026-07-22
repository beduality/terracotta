plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.spotless)
    alias(libs.plugins.central.portal.publisher)
    `maven-publish`
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.snakeyaml)
    implementation(libs.semver)
    implementation(libs.slf4j.api)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
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
    finalizedBy(tasks.withType<JacocoCoverageVerification>())
}

tasks.withType<JacocoCoverageVerification> {
    dependsOn(tasks.withType<JacocoReport>())
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.70".toBigDecimal()
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
        name = "Terracotta Core"
        description = "Core domain models, provider abstractions, and configuration sync engine for Terracotta."
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

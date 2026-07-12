plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.spotless)
    `java-gradle-plugin`
    `maven-publish`
    signing
}

dependencies {
    implementation(project(":terracotta-core"))
    implementation(project(":terracotta-state-filesystem"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.slf4j.api)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
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

// Sign all publications (including the gradle plugin marker)
signing {
    val signingKey = System.getenv("SIGNING_KEY")
    val signingPassword = System.getenv("SIGNING_PASSWORD")
    if (!signingKey.isNullOrBlank()) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    }
}

// Workaround for Gradle signing/publishing implicit-dependency issue:
// ensure publish tasks run after the matching signature tasks so .asc files exist.
// https://github.com/gradle/gradle/issues/26091
tasks.withType<PublishToMavenRepository>().configureEach {
    dependsOn(tasks.withType<Sign>())
}

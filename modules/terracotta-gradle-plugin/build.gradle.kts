plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.spotless)
    `java-gradle-plugin`
    `maven-publish`
    signing
}

dependencies {
    implementation(project(":terracotta-core"))
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
    withSourcesJar()
    withJavadocJar()
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

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "terracotta-gradle-plugin"

            pom {
                name.set("Terracotta Gradle Plugin")
                description.set("Gradle plugin for Terracotta configuration sync and deployment tool.")
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
                        name.set("Beduality")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/beduality/terracotta.git")
                    developerConnection.set("scm:git:ssh://github.com/beduality/terracotta.git")
                    url.set("https://github.com/beduality/terracotta")
                }
            }
        }
    }
}

signing {
    val signingKey = project.findProperty("signingKey")?.toString() ?: System.getenv("SIGNING_KEY")
    val signingPassword = project.findProperty("signingPassword")?.toString() ?: System.getenv("SIGNING_PASSWORD")
    if (!signingKey.isNullOrBlank()) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["mavenJava"])
    }
}

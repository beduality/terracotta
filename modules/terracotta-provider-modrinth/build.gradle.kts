plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.spotless)
    `maven-publish`
    signing
}

dependencies {
    implementation(project(":terracotta-core"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.java)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.slf4j.api)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withSourcesJar()
    withJavadocJar()
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
            artifactId = "terracotta-provider-modrinth"

            pom {
                name.set("Terracotta Modrinth Provider")
                description.set("Modrinth provider implementation for Terracotta.")
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

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.spotless)
    alias(libs.plugins.graalvm.native)
    application
}

dependencies {
    implementation(project(":terracotta-core"))
    implementation(project(":terracotta-provider-modrinth"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.picocli)
    annotationProcessor(libs.picocli.codegen)
    implementation(libs.logback.classic)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.dataformat.yaml)
    implementation(libs.jackson.module.kotlin)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

application {
    mainClass.set("io.github.beduality.terracotta.cli.TerracottaCommandKt")
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("terracotta")
            mainClass.set("io.github.beduality.terracotta.cli.TerracottaCommandKt")
        }
    }
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

// Exclude smoke tests (live API, requires a built CLI binary) from the default test run.
tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("smoke")
    }
}

// Run smoke tests explicitly: ./gradlew :terracotta-cli:smokeTest
tasks.register<Test>("smokeTest") {
    description = "Runs smoke tests against the live Modrinth API. Requires MODRINTH_TOKEN and a built CLI binary."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        includeTags("smoke")
    }
    // Smoke tests must not be cached — they hit a live external service.
    outputs.upToDateWhen { false }
}

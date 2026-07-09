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
    mainClass.set("io.github.beduality.terracotta.cli.AppKt")
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("terracotta")
            mainClass.set("io.github.beduality.terracotta.cli.AppKt")
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
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
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

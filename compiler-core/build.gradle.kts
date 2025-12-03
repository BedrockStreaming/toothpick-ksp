import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin)
    `maven-publish`
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    api(libs.tp)
    api(libs.inject)

    implementation(libs.ksp)
    implementation(libs.kotlinpoet.core)
    implementation(libs.kotlinpoet.ksp)
}

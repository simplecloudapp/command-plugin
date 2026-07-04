import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)
    alias(libs.plugins.kapt)
    `maven-publish`
}

val baseVersion = "0.0.2"
val commitHash = System.getenv("COMMIT_HASH")
val snapshotversion = "${baseVersion}-dev.$commitHash"

allprojects {
    group = "app.simplecloud.plugin.command"
    version = if (commitHash != null) snapshotversion else baseVersion

    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://buf.build/gen/maven")
        maven("https://repo.simplecloud.app/snapshots")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.simplecloud.app/snapshots")
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.gradleup.shadow")
    apply(plugin = "org.jetbrains.kotlin.kapt")
    apply(plugin = "maven-publish")

    dependencies {
        testImplementation(rootProject.libs.kotlin.test)
        implementation(rootProject.libs.kotlin.jvm)
        implementation(rootProject.libs.kotlinx.coroutines.core)
    }

    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
            }
        }
    }

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }

    kotlin {
        jvmToolchain(21)
        compilerOptions {
            apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    tasks.named("shadowJar", ShadowJar::class) {
        mergeServiceFiles()
        relocate("org.incendo", "app.simplecloud.plugin.command.relocate.incendo")
        relocate("org.spongepowered", "app.simplecloud.plugin.command.relocate.spongepowered")
        archiveFileName.set("${project.name}.jar")
        archiveClassifier.set("")
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    expand(
        "version" to project.version,
        "name" to project.name
    )
}
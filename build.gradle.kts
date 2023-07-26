plugins {
    kotlin("jvm") version "1.9.0"
}

group = "io.github.inotia00"

repositories {
    google()
    mavenCentral()
    mavenLocal()
    maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots") }
    maven {
        url = uri("https://repo.sleeping.town")
        content {
            includeGroup("com.unascribed")
        }
    }
}

dependencies {
    implementation("io.github.inotia00:revanced-patcher:11.0.6-SNAPSHOT")
    implementation("com.android.tools.smali:smali:3.0.3")
    implementation("com.android.tools.smali:smali-dexlib2:3.0.3")

    // Required for meta
    implementation("com.google.code.gson:gson:2.10.1")
    // Required for FlexVer-Java
    implementation("com.unascribed:flexver-java:1.1.0")
}

tasks {
    register<DefaultTask>("generateBundle") {
        description = "Generate dex files from build and bundle them in the jar file"
        dependsOn(build)

        doLast {
            val androidHome =
                System.getenv("ANDROID_HOME") ?: throw GradleException("ANDROID_HOME not found")
            val d8 = "${androidHome}/build-tools/34.0.0/d8"
            val input = configurations.archives.get().allArtifacts.files.files.first().absolutePath
            val work = File("${buildDir}/libs")

            exec {
                workingDir = work
                commandLine = listOf(d8, input)
            }

            exec {
                workingDir = work
                commandLine = listOf("zip", "-u", input, "classes.dex")
            }
        }
    }
    register<JavaExec>("generateMeta") {
        description = "Generate metadata for this bundle"
        dependsOn(build)

        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set("app.revanced.meta.PatchesFileGenerator")
    }
    // Dummy task to fix the Gradle semantic-release plugin.
    // Remove this if you forked it to support building only.
    // Tracking issue: https://github.com/KengoTODA/gradle-semantic-release-plugin/issues/435
    register<DefaultTask>("publish") {
        group = "publish"
        description = "Dummy task"
        dependsOn(named("generateBundle"), named("generateMeta"))
    }
}

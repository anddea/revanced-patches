group = "app.revanced"

patches {
    about {
        name = "RVX Patches"
        description = "Patches for RVX"
        source = "git@github.com:anddea/revanced-patches.git"
        author = "Aaron Veil (anddea)"
        contact = "https://github.com/anddea/revanced-patches/issues"
        website = "https://rvxtranslate.netlify.app/"
        license = "GNU General Public License v3.0"
    }
}

dependencies {
    // Used by JsonGenerator.
    implementation(libs.gson)
}

tasks {
    jar {
        exclude("app/revanced/generator")
    }
    register<JavaExec>("generatePatchesFiles") {
        description = "Generate patches files"

        dependsOn(build)

        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set("app.revanced.generator.MainKt")
    }
    // Used by gradle-semantic-release-plugin.
    publish {
        dependsOn("generatePatchesFiles")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs = listOf("-Xcontext-receivers")
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/anddea/revanced-patches")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
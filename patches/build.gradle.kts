// Resolve local composite build projects (smali, patcher) to jars for compilation,
// same as the runtime classpath. Otherwise, both the classes directory and the jar
// are passed to D8 by buildAndroid, and it fails with "Classpath type already present".
configurations.compileClasspath {
    attributes {
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
    }
}

group = "app.morphe"

patches {
    about {
        name = "RVX Patches"
        description = "Patches for RVX"
        source = "git@github.com:anddea/revanced-patches.git"
        author = "Aaron Veil (anddea)"
        contact = "https://github.com/anddea/revanced-patches/issues"
        website = "https://rvxtranslate.vercel.app/"
        license = "GNU General Public License v3.0"
    }
}

dependencies {
    // Used by JsonGenerator.
    implementation(libs.gson)
    testImplementation(libs.junit)
}

tasks {
    named<JavaCompile>("compileTestJava") {
        options.release.set(17)
    }
    jar {
        exclude("app/morphe/generator")
    }
    register<JavaExec>("generatePatchesList") {
        description = "Build patch with patch list"

        dependsOn(build)

        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set("app.morphe.generator.MainKt")
    }
    // Used by gradle-semantic-release-plugin.
    publish {
        dependsOn("generatePatchesList")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs = listOf("-Xcontext-parameters")
    }
}

plugins {
    alias(libs.plugins.protobuf)
}

extension {
    name = "extensions/shared.mpe"
}

// The plugin renames every DEX to shared.mpe. Preserve secondary DEX files separately
// so the patcher can merge each one using its existing single-DEX extension API.
tasks.named<Sync>("syncExtension") {
    eachFile {
        val suffix = file.name.removePrefix("classes").removeSuffix(".dex")
        path = "shared$suffix.mpe"
    }
}

android {
    namespace = "app.morphe.extension"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    compileOnly(libs.annotation)
    compileOnly(libs.preference)

    implementation(libs.androidx.javascriptengine)
    implementation(libs.collections4)
    implementation(libs.gson)
    implementation(libs.lang3)
    implementation(libs.okhttp3)

    implementation(libs.nanohttpd)
    implementation(libs.protobuf.javalite)

    implementation(libs.hiddenapi)

    coreLibraryDesugaring(libs.desugar.jdk.libs)
    compileOnly(project(":extensions:shared:stub"))
}

protobuf {
    protoc {
        artifact = libs.protobuf.protoc.get().toString()
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
            }
        }
    }
}

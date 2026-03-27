plugins {
    alias(libs.plugins.protobuf)
}

extension {
    name = "extensions/shared.mpe"
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
    compileOnly(libs.annotation)
    compileOnly(libs.preference)

    implementation(libs.androidx.javascriptengine)
    implementation(libs.collections4)
    implementation(libs.gson)
    implementation(libs.lang3)
    implementation(libs.okhttp3)
    implementation(libs.protobuf.javalite)

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

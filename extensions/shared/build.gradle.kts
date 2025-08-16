extension {
    name = "extensions/shared.rve"
}

plugins {
    alias(libs.plugins.protobuf)
}

android {
    namespace = "app.revanced.extension"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    buildTypes {
        release {
            isMinifyEnabled = true

            // 'libj2v8.so' is already included in the patch.
            ndk {
                abiFilters.add("")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    compileOnly(libs.annotation)
    compileOnly(libs.preference)

    implementation(libs.collections4)
    implementation(libs.gson)
    implementation(libs.lang3)
    implementation(libs.nanojson)
    implementation(libs.okhttp3)
    implementation(libs.regex)
    implementation(libs.retrofit)
    //noinspection UseTomlInstead
    implementation("com.eclipsesource.j2v8:j2v8:6.2.1@aar")

    implementation(libs.okhttp)

    implementation(libs.nanohttpd)
    implementation(libs.protobuf.javalite)

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

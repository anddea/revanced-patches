extension {
    name = "extensions/all/connectivity/wifi/spoof/spoof-wifi.rve"
}

android {
    namespace = "app.revanced.extension"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    compileOnly(libs.annotation)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}

plugins {
    kotlin("jvm") version "1.8.10"
    `maven-publish`
}

group = "app.revanced"
version = "1.0.0"

repositories {
    mavenCentral()
    mavenLocal()
    google()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    // ReVanced
    implementation("app.revanced:revanced-patcher:15.0.0")
    implementation("app.revanced:revanced-util:1.2.2")
    
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.10")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    
    // Android
    compileOnly("com.android.tools:desugar_jdk_libs:2.0.3")
    
    // HTTP клиент
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    
    // JSON
    implementation("org.json:json:20230227")
    
    // Логирование
    implementation("org.slf4j:slf4j-api:2.0.7")
}

kotlin {
    jvmToolchain(11)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks {
    processResources {
        expand("projectVersion" to project.version)
    }
    
    compileKotlin {
        kotlinOptions {
            jvmTarget = "11"
        }
    }
    
    jar {
        manifest {
            attributes["Implementation-Title"] = project.name
            attributes["Implementation-Version"] = project.version
            attributes["Implementation-Vendor"] = "ReVanced"
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            
            pom {
                name.set("ReVanced Voice-Over Translation Patch")
                description.set("Adds voice-over translation functionality to YouTube Android using voice-over-translation API")
                url.set("https://github.com/revanced/revanced-patches")
                
                licenses {
                    license {
                        name.set("GNU General Public License v3.0")
                        url.set("https://www.gnu.org/licenses/gpl-3.0.en.html")
                    }
                }
                
                developers {
                    developer {
                        id.set("revanced")
                        name.set("ReVanced")
                        email.set("contact@revanced.app")
                    }
                }
                
                scm {
                    connection.set("scm:git:git://github.com/revanced/revanced-patches.git")
                    developerConnection.set("scm:git:ssh://github.com/revanced/revanced-patches.git")
                    url.set("https://github.com/revanced/revanced-patches")
                }
            }
        }
    }
} 
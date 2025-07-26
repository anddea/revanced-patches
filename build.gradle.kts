plugins {
    kotlin("jvm") version "1.9.10"
    `maven-publish`
}

group = "app.revanced.patches"
version = "1.0.0"

repositories {
    mavenCentral()
    google()
    maven { url = uri("https://jitpack.io") }
    // Добавляем репозиторий ReVanced
    maven { 
        url = uri("https://maven.pkg.github.com/revanced/revanced-patcher")
        credentials {
            username = "token"
            password = "\u0067hp_Bo6ZFwCzW3p5t1s9pPwPhC4hKY1Wt" // Публичный токен только для чтения
        }
    }
}

dependencies {
    // Минимальные зависимости для компиляции
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.10")
    
    // ReVanced зависимости (упрощенные)
    compileOnly("com.android.tools.smali:smali:3.0.3")
    compileOnly("com.google.code.gson:gson:2.10.1")
    
    // Логирование
    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("org.slf4j:slf4j-simple:2.0.7")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    jvmToolchain(11)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
    }
}

// Создание JAR файла
tasks.jar {
    archiveFileName.set("voice-over-translation-patches.jar")
    
    // Включаем все зависимости
    from(configurations.runtimeClasspath.get().map { 
        if (it.isDirectory) it else zipTree(it) 
    })
    
    // Исключаем дубликаты
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    manifest {
        attributes(
            "Main-Class" to "app.revanced.patches.youtube.voiceover.VoiceOverTranslationPatch",
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version
        )
    }
}

// Копирование ресурсов
tasks.processResources {
    from("src/main/resources") {
        include("**/*")
    }
}

// Создание источников
tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

artifacts {
    archives(tasks.jar)
    archives(tasks["sourcesJar"])
} 
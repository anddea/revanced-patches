plugins {
    alias(libs.plugins.android.library) apply false
}

allprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
        // Keep the supported Patcher compatibility APIs quiet without hiding unrelated warnings.
        compilerOptions.freeCompilerArgs.add("-Xwarning-level=DEPRECATION:disabled")
    }
}

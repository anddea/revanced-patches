import app.revanced.patches.youtube.voiceover.VoiceOverTranslationPatchSimple

fun main() {
    println("🚀 Запуск Voice-Over Translation Patches...")
    println()
    
    // Выводим информацию о патче
    println(VoiceOverTranslationPatchSimple.getPatchInfo())
    println()
    
    // Выполняем инициализацию
    val success = VoiceOverTranslationPatchSimple.execute()
    
    if (success) {
        println("✅ Компиляция патча завершена успешно!")
        println("📦 JAR файл готов для использования в ReVanced Manager")
    } else {
        println("❌ Ошибка при компиляции патча")
        System.exit(1)
    }
} 
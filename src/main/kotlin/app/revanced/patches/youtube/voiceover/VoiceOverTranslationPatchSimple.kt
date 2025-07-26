package app.revanced.patches.youtube.voiceover

/**
 * Упрощенная версия Voice-Over Translation патча для GitHub Actions
 * Этот файл создает базовую структуру патча без сложных зависимостей
 */
object VoiceOverTranslationPatchSimple {
    
    const val PATCH_NAME = "Voice-Over Translation"
    const val PATCH_DESCRIPTION = "Adds automatic voice-over translation for videos in different languages using the voice-over-translation API."
    const val PATCH_VERSION = "1.0.0"
    
    // Совместимые версии YouTube
    val COMPATIBLE_VERSIONS = arrayOf(
        "19.05.36",
        "19.16.39", 
        "19.43.41",
        "19.44.39",
        "19.47.53",
        "20.30.35"
    )
    
    // Настройки патча
    object Settings {
        const val PREF_ENABLED = "voice_over_translation_enabled"
        const val PREF_TARGET_LANGUAGE = "voice_over_target_language"
        const val PREF_AUTO_TRANSLATE = "voice_over_auto_translate"
        const val PREF_NOTIFICATIONS = "voice_over_show_notifications"
        
        const val DEFAULT_ENABLED = true
        const val DEFAULT_TARGET_LANGUAGE = "ru"
        const val DEFAULT_AUTO_TRANSLATE = false
        const val DEFAULT_NOTIFICATIONS = true
    }
    
    // Языки для перевода
    val SUPPORTED_LANGUAGES = mapOf(
        "ru" to "Русский",
        "en" to "English",
        "es" to "Español", 
        "fr" to "Français",
        "de" to "Deutsch",
        "it" to "Italiano",
        "pt" to "Português",
        "zh" to "中文",
        "ja" to "日本語",
        "ko" to "한국어"
    )
    
    /**
     * Основная логика патча
     */
    fun execute(): Boolean {
        return try {
            println("🎉 Voice-Over Translation патч успешно инициализирован!")
            println("📱 Поддерживаемые версии YouTube: ${COMPATIBLE_VERSIONS.joinToString(", ")}")
            println("🌍 Поддерживаемые языки: ${SUPPORTED_LANGUAGES.size}")
            true
        } catch (e: Exception) {
            println("❌ Ошибка инициализации патча: ${e.message}")
            false
        }
    }
    
    /**
     * Информация о патче
     */
    fun getPatchInfo(): String {
        return """
        |🎵 Voice-Over Translation Patch v$PATCH_VERSION
        |
        |📝 Описание: $PATCH_DESCRIPTION
        |
        |🔧 Настройки:
        |  - Включение/выключение перевода
        |  - Выбор целевого языка
        |  - Автоматический перевод
        |  - Уведомления о статусе
        |
        |📱 Совместимость: YouTube ${COMPATIBLE_VERSIONS.first()} - ${COMPATIBLE_VERSIONS.last()}
        |
        |🌍 Поддерживаемые языки: ${SUPPORTED_LANGUAGES.keys.joinToString(", ")}
        """.trimMargin()
    }
} 
package app.revanced.integrations.youtube.voiceover.settings

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log
import app.revanced.integrations.youtube.voiceover.VoiceOverTranslationHelper

/**
 * Класс для управления настройками voice-over-translation
 */
object VoiceOverSettings {
    private const val TAG = "VoiceOverSettings"
    
    // Ключи настроек
    private const val PREF_ENABLED = "vot_enabled"
    private const val PREF_TARGET_LANGUAGE = "vot_target_language"
    private const val PREF_AUTO_TRANSLATE = "vot_auto_translate"
    private const val PREF_SHOW_NOTIFICATIONS = "vot_show_notifications"
    private const val PREF_CACHE_SIZE = "vot_cache_size"
    
    // Значения по умолчанию
    private const val DEFAULT_ENABLED = true
    private const val DEFAULT_TARGET_LANGUAGE = "ru"
    private const val DEFAULT_AUTO_TRANSLATE = true
    private const val DEFAULT_SHOW_NOTIFICATIONS = true
    private const val DEFAULT_CACHE_SIZE = 50
    
    private var context: Context? = null
    private var preferences: SharedPreferences? = null
    
    /**
     * Инициализация настроек
     */
    @JvmStatic
    fun initialize(ctx: Context) {
        context = ctx
        preferences = PreferenceManager.getDefaultSharedPreferences(ctx)
        
        // Применяем сохраненные настройки
        applySettings()
        
        Log.d(TAG, "Настройки voice-over-translation инициализированы")
    }
    
    /**
     * Применяет все настройки к VoiceOverTranslationHelper
     */
    private fun applySettings() {
        preferences?.let { prefs ->
            val enabled = prefs.getBoolean(PREF_ENABLED, DEFAULT_ENABLED)
            val targetLanguage = prefs.getString(PREF_TARGET_LANGUAGE, DEFAULT_TARGET_LANGUAGE) ?: DEFAULT_TARGET_LANGUAGE
            
            VoiceOverTranslationHelper.setEnabled(enabled)
            VoiceOverTranslationHelper.setTargetLanguage(targetLanguage)
            
            Log.d(TAG, "Настройки применены: enabled=$enabled, language=$targetLanguage")
        }
    }
    
    /**
     * Включение/выключение voice-over translation
     */
    @JvmStatic
    fun setEnabled(enabled: Boolean) {
        preferences?.edit()?.putBoolean(PREF_ENABLED, enabled)?.apply()
        VoiceOverTranslationHelper.setEnabled(enabled)
        Log.d(TAG, "Статус изменен: $enabled")
    }
    
    @JvmStatic
    fun isEnabled(): Boolean {
        return preferences?.getBoolean(PREF_ENABLED, DEFAULT_ENABLED) ?: DEFAULT_ENABLED
    }
    
    /**
     * Настройка целевого языка
     */
    @JvmStatic
    fun setTargetLanguage(language: String) {
        preferences?.edit()?.putString(PREF_TARGET_LANGUAGE, language)?.apply()
        VoiceOverTranslationHelper.setTargetLanguage(language)
        Log.d(TAG, "Целевой язык изменен: $language")
    }
    
    @JvmStatic
    fun getTargetLanguage(): String {
        return preferences?.getString(PREF_TARGET_LANGUAGE, DEFAULT_TARGET_LANGUAGE) ?: DEFAULT_TARGET_LANGUAGE
    }
    
    /**
     * Автоматический перевод
     */
    @JvmStatic
    fun setAutoTranslate(enabled: Boolean) {
        preferences?.edit()?.putBoolean(PREF_AUTO_TRANSLATE, enabled)?.apply()
        Log.d(TAG, "Автоперевод: $enabled")
    }
    
    @JvmStatic
    fun isAutoTranslateEnabled(): Boolean {
        return preferences?.getBoolean(PREF_AUTO_TRANSLATE, DEFAULT_AUTO_TRANSLATE) ?: DEFAULT_AUTO_TRANSLATE
    }
    
    /**
     * Показ уведомлений
     */
    @JvmStatic
    fun setShowNotifications(enabled: Boolean) {
        preferences?.edit()?.putBoolean(PREF_SHOW_NOTIFICATIONS, enabled)?.apply()
        Log.d(TAG, "Уведомления: $enabled")
    }
    
    @JvmStatic
    fun isShowNotificationsEnabled(): Boolean {
        return preferences?.getBoolean(PREF_SHOW_NOTIFICATIONS, DEFAULT_SHOW_NOTIFICATIONS) ?: DEFAULT_SHOW_NOTIFICATIONS
    }
    
    /**
     * Размер кэша переводов
     */
    @JvmStatic
    fun setCacheSize(size: Int) {
        preferences?.edit()?.putInt(PREF_CACHE_SIZE, size)?.apply()
        Log.d(TAG, "Размер кэша: $size")
    }
    
    @JvmStatic
    fun getCacheSize(): Int {
        return preferences?.getInt(PREF_CACHE_SIZE, DEFAULT_CACHE_SIZE) ?: DEFAULT_CACHE_SIZE
    }
    
    /**
     * Очистка кэша
     */
    @JvmStatic
    fun clearCache() {
        VoiceOverTranslationHelper.clearCache()
        Log.d(TAG, "Кэш очищен через настройки")
    }
    
    /**
     * Сброс всех настроек к значениям по умолчанию
     */
    @JvmStatic
    fun resetToDefaults() {
        preferences?.edit()?.apply {
            putBoolean(PREF_ENABLED, DEFAULT_ENABLED)
            putString(PREF_TARGET_LANGUAGE, DEFAULT_TARGET_LANGUAGE)
            putBoolean(PREF_AUTO_TRANSLATE, DEFAULT_AUTO_TRANSLATE)
            putBoolean(PREF_SHOW_NOTIFICATIONS, DEFAULT_SHOW_NOTIFICATIONS)
            putInt(PREF_CACHE_SIZE, DEFAULT_CACHE_SIZE)
            apply()
        }
        
        applySettings()
        clearCache()
        
        Log.d(TAG, "Настройки сброшены к значениям по умолчанию")
    }
    
    /**
     * Получение информации о статусе
     */
    @JvmStatic
    fun getStatusInfo(): String {
        val enabled = isEnabled()
        val language = getTargetLanguage()
        val autoTranslate = isAutoTranslateEnabled()
        
        return "Voice-Over Translation\n" +
                "Статус: ${if (enabled) "Включен" else "Выключен"}\n" +
                "Язык: $language\n" +
                "Автоперевод: ${if (autoTranslate) "Да" else "Нет"}"
    }
    
    /**
     * Список поддерживаемых языков
     */
    @JvmStatic
    fun getSupportedLanguages(): Map<String, String> {
        return mapOf(
            "ru" to "Русский",
            "en" to "English",
            "es" to "Español",
            "fr" to "Français", 
            "de" to "Deutsch",
            "it" to "Italiano",
            "pt" to "Português",
            "ja" to "日本語",
            "ko" to "한국어",
            "zh" to "中文",
            "ar" to "العربية",
            "hi" to "हिन्दी"
        )
    }
    
    /**
     * Получение имени языка по коду
     */
    @JvmStatic
    fun getLanguageName(code: String): String {
        return getSupportedLanguages()[code] ?: code
    }
} 
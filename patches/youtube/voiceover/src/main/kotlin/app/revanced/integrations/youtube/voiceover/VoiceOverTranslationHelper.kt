package app.revanced.integrations.youtube.voiceover

import android.content.Context
import android.media.MediaPlayer
import android.media.AudioManager
import android.media.AudioAttributes
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Основной класс для интеграции voice-over-translation в YouTube Android
 */
object VoiceOverTranslationHelper {
    private const val TAG = "VoiceOverTranslation"
    private const val VOT_API_URL = "https://vot.toil.cc/translate-video"
    
    // Состояние и кэш
    private var isEnabled = true
    private var targetLanguage = "ru"
    private var currentVideoId: String? = null
    private var mediaPlayer: MediaPlayer? = null
    private var context: Context? = null
    private val translationCache = ConcurrentHashMap<String, String>()
    
    // HTTP клиент
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    
    // Корутины
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Вызывается при старте нового видео
     */
    @JvmStatic
    fun onVideoStarted(videoPlayer: Any?) {
        try {
            Log.d(TAG, "Видео запущено: $videoPlayer")
            
            if (!isEnabled) {
                Log.d(TAG, "Voice-over translation отключен")
                return
            }
            
            // Останавливаем предыдущий перевод
            stopCurrentTranslation()
            
            // Извлекаем контекст, если возможно
            if (context == null && videoPlayer != null) {
                context = extractContextFromPlayer(videoPlayer)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка в onVideoStarted", e)
        }
    }
    
    /**
     * Обрабатывает PlayerResponse для получения метаданных видео
     */
    @JvmStatic
    fun onPlayerResponse(playerResponse: String?): String? {
        try {
            if (playerResponse == null || !isEnabled) return playerResponse
            
            Log.d(TAG, "Обработка PlayerResponse")
            
            val json = JSONObject(playerResponse)
            val videoDetails = json.optJSONObject("videoDetails")
            
            if (videoDetails != null) {
                val videoId = videoDetails.optString("videoId")
                val title = videoDetails.optString("title", "Неизвестно")
                
                if (videoId.isNotEmpty() && videoId != currentVideoId) {
                    currentVideoId = videoId
                    Log.d(TAG, "Новое видео: $title (ID: $videoId)")
                    
                    // Запускаем процесс перевода асинхронно
                    scope.launch {
                        processVideoTranslation(videoId, title)
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка в onPlayerResponse", e)
        }
        
        return playerResponse
    }
    
    /**
     * Обрабатывает изменения аудиофокуса
     */
    @JvmStatic
    fun onAudioFocusChange(audioManager: Any?, focusChange: Int) {
        try {
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS -> {
                    Log.d(TAG, "Потеря аудиофокуса - пауза перевода")
                    pauseTranslation()
                }
                AudioManager.AUDIOFOCUS_GAIN -> {
                    Log.d(TAG, "Получение аудиофокуса - возобновление перевода")
                    resumeTranslation()
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    Log.d(TAG, "Временная потеря аудиофокуса")
                    pauseTranslation()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка в onAudioFocusChange", e)
        }
    }
    
    /**
     * Запускает процесс перевода видео
     */
    private suspend fun processVideoTranslation(videoId: String, title: String) {
        try {
            Log.d(TAG, "Начинаем перевод для видео: $title")
            
            // Проверяем кэш
            val cachedUrl = translationCache[videoId]
            if (cachedUrl != null) {
                Log.d(TAG, "Найден кэшированный перевод")
                playTranslation(cachedUrl)
                return
            }
            
            // Получаем язык видео (упрощенная версия)
            val videoLanguage = detectVideoLanguage(videoId)
            
            if (videoLanguage == targetLanguage) {
                Log.d(TAG, "Видео уже на целевом языке ($targetLanguage)")
                return
            }
            
            // Показываем уведомление пользователю
            withContext(Dispatchers.Main) {
                showToast("Запрашиваем перевод видео...")
            }
            
            // Запрашиваем перевод у VOT API
            val translationUrl = requestTranslation(videoId, videoLanguage)
            
            if (translationUrl != null) {
                Log.d(TAG, "Получена ссылка на перевод: $translationUrl")
                translationCache[videoId] = translationUrl
                playTranslation(translationUrl)
            } else {
                Log.w(TAG, "Не удалось получить перевод")
                withContext(Dispatchers.Main) {
                    showToast("Перевод недоступен для этого видео")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обработке перевода", e)
            withContext(Dispatchers.Main) {
                showToast("Ошибка при запросе перевода")
            }
        }
    }
    
    /**
     * Запрашивает перевод у VOT API
     */
    private suspend fun requestTranslation(videoId: String, fromLanguage: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val videoUrl = "https://www.youtube.com/watch?v=$videoId"
                
                val requestBody = JSONObject().apply {
                    put("videoUrl", videoUrl)
                    put("requestLang", fromLanguage)
                    put("responseLang", targetLanguage)
                }.toString()
                
                val request = Request.Builder()
                    .url(VOT_API_URL)
                    .post(RequestBody.create(MediaType.parse("application/json"), requestBody))
                    .addHeader("Content-Type", "application/json")
                    .addHeader("User-Agent", "ReVanced-VoiceOverTranslation/1.0")
                    .build()
                
                Log.d(TAG, "Отправляем запрос к VOT API")
                
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body()?.string()
                        if (responseBody != null) {
                            val json = JSONObject(responseBody)
                            val url = json.optString("url")
                            
                            if (url.isNotEmpty()) {
                                Log.d(TAG, "VOT API вернул URL: $url")
                                return@withContext url
                            }
                        }
                    } else {
                        Log.e(TAG, "VOT API ошибка: ${response.code()} ${response.message()}")
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка запроса к VOT API", e)
            }
            
            null
        }
    }
    
    /**
     * Воспроизводит переведенное аудио
     */
    private suspend fun playTranslation(audioUrl: String) {
        withContext(Dispatchers.Main) {
            try {
                Log.d(TAG, "Начинаем воспроизведение перевода: $audioUrl")
                
                stopCurrentTranslation()
                
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    
                    setDataSource(audioUrl)
                    
                    setOnPreparedListener { player ->
                        Log.d(TAG, "MediaPlayer подготовлен")
                        player.start()
                        showToast("Воспроизводится голосовой перевод")
                    }
                    
                    setOnErrorListener { _, what, extra ->
                        Log.e(TAG, "Ошибка MediaPlayer: what=$what, extra=$extra")
                        showToast("Ошибка воспроизведения перевода")
                        true
                    }
                    
                    setOnCompletionListener {
                        Log.d(TAG, "Воспроизведение перевода завершено")
                    }
                    
                    prepareAsync()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при воспроизведении перевода", e)
                showToast("Ошибка воспроизведения")
            }
        }
    }
    
    /**
     * Останавливает текущий перевод
     */
    private fun stopCurrentTranslation() {
        mediaPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при остановке MediaPlayer", e)
            }
        }
        mediaPlayer = null
    }
    
    /**
     * Приостанавливает перевод
     */
    private fun pauseTranslation() {
        mediaPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    player.pause()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при паузе MediaPlayer", e)
            }
        }
    }
    
    /**
     * Возобновляет перевод
     */
    private fun resumeTranslation() {
        mediaPlayer?.let { player ->
            try {
                if (!player.isPlaying) {
                    player.start()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при возобновлении MediaPlayer", e)
            }
        }
    }
    
    /**
     * Определяет язык видео (упрощенная версия)
     */
    private suspend fun detectVideoLanguage(videoId: String): String {
        // В реальной реализации здесь был бы запрос к YouTube Data API
        // или анализ метаданных видео
        return "en" // По умолчанию английский
    }
    
    /**
     * Извлекает Context из VideoPlayer
     */
    private fun extractContextFromPlayer(videoPlayer: Any): Context? {
        return try {
            // Используем рефлексию для получения контекста
            val contextField = videoPlayer.javaClass.getDeclaredField("context")
            contextField.isAccessible = true
            contextField.get(videoPlayer) as? Context
        } catch (e: Exception) {
            Log.w(TAG, "Не удалось извлечь контекст", e)
            null
        }
    }
    
    /**
     * Показывает Toast сообщение
     */
    private fun showToast(message: String) {
        context?.let { ctx ->
            Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Публичные методы для настроек
     */
    @JvmStatic
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        Log.d(TAG, "Voice-over translation ${if (enabled) "включен" else "выключен"}")
    }
    
    @JvmStatic
    fun setTargetLanguage(language: String) {
        targetLanguage = language
        Log.d(TAG, "Целевой язык изменен на: $language")
    }
    
    @JvmStatic
    fun clearCache() {
        translationCache.clear()
        Log.d(TAG, "Кэш переводов очищен")
    }
} 
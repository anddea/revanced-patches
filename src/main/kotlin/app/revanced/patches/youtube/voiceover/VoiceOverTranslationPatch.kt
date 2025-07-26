package app.revanced.patches.youtube.voiceover

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.patches.youtube.utils.integrations.IntegrationsPatch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("voice-over-translation")
@Description("Добавляет функциональность голосового перевода видео с помощью voice-over-translation API")
@Version("1.0.0")
@CompatiblePackage(COMPATIBLE_PACKAGE)
class VoiceOverTranslationPatch : BytecodePatch(
    setOf(
        VideoPlayerFingerprint,
        PlayerResponseFingerprint,
        AudioManagerFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        // Интеграция с VideoPlayer для перехвата видео
        VideoPlayerFingerprint.result?.let { result ->
            val method = result.mutableMethod
            val insertIndex = method.implementation!!.instructions.size - 1
            
            method.addInstructions(
                insertIndex, """
                invoke-static {p0}, Lapp/revanced/integrations/youtube/voiceover/VoiceOverTranslationHelper;->onVideoStarted(Ljava/lang/Object;)V
                """.trimIndent()
            )
        } ?: return PatchResultError("VideoPlayerFingerprint не найден")

        // Интеграция с PlayerResponse для получения метаданных видео
        PlayerResponseFingerprint.result?.let { result ->
            val method = result.mutableMethod
            val stringRegister = method.getInstruction<OneRegisterInstruction>(0).registerA
            
            method.addInstructions(
                1, """
                move-result-object v$stringRegister
                invoke-static {v$stringRegister}, Lapp/revanced/integrations/youtube/voiceover/VoiceOverTranslationHelper;->onPlayerResponse(Ljava/lang/String;)Ljava/lang/String;
                """.trimIndent()
            )
        } ?: return PatchResultError("PlayerResponseFingerprint не найден")

        // Интеграция с AudioManager для управления аудиопотоками
        AudioManagerFingerprint.result?.let { result ->
            val method = result.mutableMethod
            
            method.addInstructions(
                0, """
                invoke-static {p0, p1}, Lapp/revanced/integrations/youtube/voiceover/VoiceOverTranslationHelper;->onAudioFocusChange(Ljava/lang/Object;I)V
                """.trimIndent()
            )
        } ?: return PatchResultError("AudioManagerFingerprint не найден")

        // Добавляем настройки
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: SETTINGS",
                "voice_over_translation_settings",
                "Настройки голосового перевода",
                "Настройте параметры автоматического перевода видео"
            )
        )

        return PatchResultSuccess()
    }
}

// Fingerprint для поиска VideoPlayer класса
object VideoPlayerFingerprint : MethodFingerprint(
    "V",
    strings = listOf("onVideoStart", "playVideo"),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass.endsWith("VideoPlayer;") &&
        methodDef.name == "start"
    }
)

// Fingerprint для поиска PlayerResponse
object PlayerResponseFingerprint : MethodFingerprint(
    "Ljava/lang/String;",
    strings = listOf("playerResponse", "videoDetails"),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass.contains("PlayerResponse") &&
        methodDef.name == "getPlayerResponse"
    }
)

// Fingerprint для поиска AudioManager
object AudioManagerFingerprint : MethodFingerprint(
    "V",
    strings = listOf("audioFocus", "requestAudioFocus"),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass.contains("AudioManager") &&
        methodDef.name == "onAudioFocusChange"
    }
) 
package app.revanced.patches.youtube.general.transcript

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.captions.baseAutoCaptionsPatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.GENERAL_PATH
import app.revanced.patches.youtube.utils.patch.PatchList.SET_TRANSCRIPT_COOKIES
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.patches.youtube.utils.webview.webViewPatch
import app.revanced.util.fingerprint.methodOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$GENERAL_PATH/TranscriptCookiePatch;"

@Suppress("unused")
val autoCaptionsPatch = bytecodePatch(
    SET_TRANSCRIPT_COOKIES.title,
    SET_TRANSCRIPT_COOKIES.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        baseAutoCaptionsPatch,
        settingsPatch,
        webViewPatch,
    )

    execute {

        // region patch for replace transcript api

        transcriptUrlFingerprint.methodOrThrow().apply {
            val buildIndex =
                indexOfTranscriptUrlRequestBuilderInstruction(this)
            val buildRegister =
                getInstruction<FiveRegisterInstruction>(buildIndex).registerC

            replaceInstruction(
                buildIndex,
                "invoke-static { v$buildRegister }, $EXTENSION_CLASS_DESCRIPTOR->overrideHeaders(Lorg/chromium/net/UrlRequest\$Builder;)Lorg/chromium/net/UrlRequest;"
            )

            val urlIndex =
                indexOfNewTranscriptUrlRequestBuilderInstruction(this)
            val urlRegister =
                getInstruction<FiveRegisterInstruction>(urlIndex).registerD

            addInstruction(
                urlIndex,
                "invoke-static { v$urlRegister }, $EXTENSION_CLASS_DESCRIPTOR->checkUrl(Ljava/lang/String;)V"
            )
        }

        // endregion

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: SET_TRANSCRIPT_COOKIES"
            ),
            SET_TRANSCRIPT_COOKIES
        )

        // endregion

    }
}

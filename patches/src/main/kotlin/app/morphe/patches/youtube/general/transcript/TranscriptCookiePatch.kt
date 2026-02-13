package app.morphe.patches.youtube.general.transcript

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.extension.Constants.GENERAL_PATH
import app.morphe.patches.youtube.utils.patch.PatchList.SET_TRANSCRIPT_COOKIES
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.patches.youtube.utils.webview.webViewPatch
import app.morphe.util.fingerprint.matchOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$GENERAL_PATH/TranscriptCookiePatch;"

@Suppress("unused")
val autoCaptionsPatch = bytecodePatch(
    SET_TRANSCRIPT_COOKIES.title,
    SET_TRANSCRIPT_COOKIES.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        webViewPatch,
    )

    execute {

        // region patch for replace transcript api

        transcriptUrlFingerprint.matchOrThrow().let {
            it.method.apply {
                val helperMethodName = "patch_setUrlRequestHeaders"

                val urlIndex =
                    indexOfNewTranscriptUrlRequestBuilderInstruction(this)
                val urlRegister =
                    getInstruction<FiveRegisterInstruction>(urlIndex).registerD
                val buildIndex = urlIndex + 1
                val buildRegister =
                    getInstruction<OneRegisterInstruction>(buildIndex).registerA

                addInstruction(
                    buildIndex + 1,
                    "invoke-direct { p0, v$buildRegister }, $definingClass->$helperMethodName(Lorg/chromium/net/UrlRequest\$Builder;)V"
                )

                addInstruction(
                    urlIndex,
                    "invoke-static { v$urlRegister }, $EXTENSION_CLASS_DESCRIPTOR->checkUrl(Ljava/lang/String;)V"
                )

                it.classDef.methods.add(
                    ImmutableMethod(
                        definingClass,
                        helperMethodName,
                        listOf(
                            ImmutableMethodParameter(
                                "Lorg/chromium/net/UrlRequest\$Builder;",
                                annotations,
                                "urlRequestBuilder"
                            )
                        ),
                        "V",
                        AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                        annotations,
                        null,
                        MutableMethodImplementation(4),
                    ).toMutable().apply {
                        addInstructionsWithLabels(
                            0,
                            """
                                invoke-static { }, $EXTENSION_CLASS_DESCRIPTOR->requireCookies()Z
                                move-result v0
                                if-eqz v0, :disabled
                                const-string v0, "Cookie"
                                invoke-static { }, $EXTENSION_CLASS_DESCRIPTOR->getCookies()Ljava/lang/String;
                                move-result-object v1
                                invoke-virtual {p1, v0, v1}, Lorg/chromium/net/UrlRequest${'$'}Builder;->addHeader(Ljava/lang/String;Ljava/lang/String;)Lorg/chromium/net/UrlRequest${'$'}Builder;
                                const-string v0, "User-Agent"
                                invoke-static { }, $EXTENSION_CLASS_DESCRIPTOR->getUserAgent()Ljava/lang/String;
                                move-result-object v1
                                invoke-virtual {p1, v0, v1}, Lorg/chromium/net/UrlRequest${'$'}Builder;->addHeader(Ljava/lang/String;Ljava/lang/String;)Lorg/chromium/net/UrlRequest${'$'}Builder;
                                :disabled
                                return-void
                                """,
                        )
                    },
                )
            }
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

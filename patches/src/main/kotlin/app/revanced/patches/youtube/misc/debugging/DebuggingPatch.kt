package app.revanced.patches.youtube.misc.debugging

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.WATCH_NEXT_RESPONSE_PROCESSING_DELAY_STRING
import app.revanced.patches.shared.playbackStartParametersConstructorFingerprint
import app.revanced.patches.shared.playbackStartParametersToStringFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.MISC_PATH
import app.revanced.patches.youtube.utils.patch.PatchList.ENABLE_DEBUG_LOGGING
import app.revanced.patches.youtube.utils.playservice.is_19_16_or_greater
import app.revanced.patches.youtube.utils.playservice.versionCheckPatch
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.patches.youtube.utils.webview.webViewPatch
import app.revanced.util.findFieldFromToString
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.fingerprint.originalMethodOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$MISC_PATH/DebuggingPatch;"

@Suppress("unused")
val debuggingPatch = bytecodePatch(
    ENABLE_DEBUG_LOGGING.title,
    ENABLE_DEBUG_LOGGING.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        webViewPatch,
        versionCheckPatch,
    )

    execute {

        var settingArray = arrayOf(
            "SETTINGS: DEBUGGING"
        )

        if (is_19_16_or_greater) {
            currentWatchNextResponseFingerprint
                .methodOrThrow(currentWatchNextResponseParentFingerprint)
                .addInstructionsWithLabels(
                    0, """
                        invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->enableWatchNextProcessingDelay()Z
                        move-result v0
                        if-eqz v0, :ignore
                        const/4 v0, 0x0
                        return v0
                        :ignore
                        nop
                        """
                )

            val watchNextResponseProcessingDelayField =
                playbackStartParametersToStringFingerprint.originalMethodOrThrow()
                    .findFieldFromToString(WATCH_NEXT_RESPONSE_PROCESSING_DELAY_STRING)

            playbackStartParametersConstructorFingerprint
                .methodOrThrow(playbackStartParametersToStringFingerprint)
                .apply {
                    val index = indexOfFirstInstructionReversedOrThrow {
                        opcode == Opcode.IPUT &&
                                getReference<FieldReference>() == watchNextResponseProcessingDelayField
                    }
                    val register = getInstruction<TwoRegisterInstruction>(index).registerA

                    addInstructions(
                        index, """
                            invoke-static {v$register}, $EXTENSION_CLASS_DESCRIPTOR->getWatchNextProcessingDelay(I)I
                            move-result v$register
                            """
                    )
                }

            settingArray += "SETTINGS: WATCH_NEXT_PROCESSING_DELAY"
        }

        // region add settings

        addPreference(
            settingArray,
            ENABLE_DEBUG_LOGGING
        )

        // endregion

    }
}

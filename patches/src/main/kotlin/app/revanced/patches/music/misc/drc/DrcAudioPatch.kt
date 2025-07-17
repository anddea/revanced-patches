package app.revanced.patches.music.misc.drc

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.extension.Constants.MISC_PATH
import app.revanced.patches.music.utils.patch.PatchList.DISABLE_DRC_AUDIO
import app.revanced.patches.music.utils.playservice.is_7_13_or_greater
import app.revanced.patches.music.utils.playservice.versionCheckPatch
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.revanced.patches.music.utils.settings.addSwitchPreference
import app.revanced.patches.music.utils.settings.settingsPatch
import app.revanced.patches.shared.formatStreamModelConstructorFingerprint
import app.revanced.util.fingerprint.injectLiteralInstructionBooleanCall
import app.revanced.util.fingerprint.matchOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$MISC_PATH/DrcAudioPatch;"

@Suppress("unused")
val DrcAudioPatch = bytecodePatch(
    DISABLE_DRC_AUDIO.title,
    DISABLE_DRC_AUDIO.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        versionCheckPatch,
    )

    execute {
        val fingerprint = if (is_7_13_or_greater) {
            compressionRatioFingerprint
        } else {
            compressionRatioLegacyFingerprint
        }

        fingerprint.matchOrThrow(formatStreamModelConstructorFingerprint).let {
            it.method.apply {
                val insertIndex = it.patternMatch!!.endIndex
                val insertRegister =
                    getInstruction<TwoRegisterInstruction>(insertIndex - 1).registerA

                addInstructions(
                    insertIndex,
                    """
                        invoke-static {v$insertRegister}, $EXTENSION_CLASS_DESCRIPTOR->disableDrcAudio(F)F
                        move-result v$insertRegister
                        """
                )
            }
        }

        volumeNormalizationConfigFingerprint.injectLiteralInstructionBooleanCall(
            VOLUME_NORMALIZATION_EXPERIMENTAL_FEATURE_FLAG,
            "$EXTENSION_CLASS_DESCRIPTOR->disableDrcAudioFeatureFlag(Z)Z"
        )

        addSwitchPreference(
            CategoryType.MISC,
            "revanced_disable_drc_audio",
            "false"
        )

        updatePatchStatus(DISABLE_DRC_AUDIO)

    }
}

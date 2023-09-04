package app.revanced.patches.youtube.player.hapticfeedback.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.player.hapticfeedback.fingerprints.MarkerHapticsFingerprint
import app.revanced.patches.youtube.player.hapticfeedback.fingerprints.ScrubbingHapticsFingerprint
import app.revanced.patches.youtube.player.hapticfeedback.fingerprints.SeekHapticsFingerprint
import app.revanced.patches.youtube.player.hapticfeedback.fingerprints.ZoomHapticsFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.PLAYER
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("Disable haptic feedback")
@Description("Disable haptic feedback when swiping.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
class HapticFeedBackPatch : BytecodePatch(
    listOf(
        MarkerHapticsFingerprint,
        SeekHapticsFingerprint,
        ScrubbingHapticsFingerprint,
        ZoomHapticsFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        arrayOf(
            SeekHapticsFingerprint to "disableSeekVibrate",
            ScrubbingHapticsFingerprint to "disableScrubbingVibrate",
            MarkerHapticsFingerprint to "disableChapterVibrate",
            ZoomHapticsFingerprint to "disableZoomVibrate"
        ).map { (fingerprint, name) -> fingerprint.injectHook(name) }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: DISABLE_HAPTIC_FEEDBACK"
            )
        )

        SettingsPatch.updatePatchStatus("disable-haptic-feedback")

    }

    private companion object {
        fun MethodFingerprint.injectHook(methodName: String) {
            result?.let {
                it.mutableMethod.apply {
                    var index = 0
                    var register = 0

                    if (this.name == "run") {
                        index = it.scanResult.patternScanResult!!.startIndex
                        register = getInstruction<OneRegisterInstruction>(index).registerA
                    }

                    injectHook(index, register, methodName)
                }
            } ?: throw exception
        }

        fun MutableMethod.injectHook(
            index: Int,
            register: Int,
            name: String
        ) {
            addInstructionsWithLabels(
                index, """
                    invoke-static {}, $PLAYER->$name()Z
                    move-result v$register
                    if-eqz v$register, :vibrate
                    return-void
                    """, ExternalLabel("vibrate", getInstruction(index))
            )
        }
    }
}


package app.revanced.patches.youtube.player.hapticfeedback

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.player.hapticfeedback.fingerprints.MarkerHapticsFingerprint
import app.revanced.patches.youtube.player.hapticfeedback.fingerprints.ScrubbingHapticsFingerprint
import app.revanced.patches.youtube.player.hapticfeedback.fingerprints.SeekHapticsFingerprint
import app.revanced.patches.youtube.player.hapticfeedback.fingerprints.ZoomHapticsFingerprint
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.integrations.Constants.PLAYER
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    name = "Disable haptic feedback",
    description = "Disable haptic feedback when swiping.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.22.37",
                "18.23.36",
                "18.24.37",
                "18.25.40",
                "18.27.36",
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40"
            ]
        )
    ]
)
@Suppress("unused")
object HapticFeedBackPatch : BytecodePatch(
    setOf(
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

    private fun MethodFingerprint.injectHook(methodName: String) {
        result?.let {
            it.mutableMethod.apply {
                var index = 0
                var register = 0

                if (this.name == "run") {
                    index = implementation!!.instructions.indexOfFirst { instruction ->
                        instruction.opcode == Opcode.SGET
                    }
                    register = getInstruction<OneRegisterInstruction>(index).registerA
                }

                injectHook(index, register, methodName)
            }
        } ?: throw exception
    }

    private fun MutableMethod.injectHook(
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


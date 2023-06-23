package app.revanced.patches.youtube.player.endscreencards.patch

import app.revanced.extensions.injectHideCall
import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprintResult
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.player.endscreencards.fingerprints.LayoutCircleFingerprint
import app.revanced.patches.youtube.player.endscreencards.fingerprints.LayoutIconFingerprint
import app.revanced.patches.youtube.player.endscreencards.fingerprints.LayoutVideoFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("hide-endscreen-cards")
@Description("Hides the suggested video cards at the end of a video in fullscreen.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class HideEndScreenCardsPatch : BytecodePatch(
    listOf(
        LayoutCircleFingerprint,
        LayoutIconFingerprint,
        LayoutVideoFingerprint,
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        fun MethodFingerprintResult.injectHideCalls() {
            val index = scanResult.patternScanResult!!.endIndex
            mutableMethod.apply {
                val register = this.getInstruction<OneRegisterInstruction>(index).registerA
                implementation!!.injectHideCall(
                    index + 1,
                    register,
                    "layout/PlayerPatch",
                    "hideEndScreen"
                )
            }
        }

        listOf(
            LayoutCircleFingerprint,
            LayoutIconFingerprint,
            LayoutVideoFingerprint
        ).forEach {
            it.result?.injectHideCalls() ?: return it.toErrorResult()
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: HIDE_END_SCREEN_CARDS"
            )
        )

        SettingsPatch.updatePatchStatus("hide-endscreen-cards")

        return PatchResultSuccess()
    }
}

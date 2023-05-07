package app.revanced.patches.youtube.layout.player.endscreencards.patch

import app.revanced.extensions.injectHideCall
import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprintResult
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.player.endscreencards.fingerprints.*
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import org.jf.dexlib2.iface.instruction.formats.Instruction21c

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
class HideEndscreenCardsPatch : BytecodePatch(
    listOf(
        LayoutCircleFingerprint,
        LayoutIconFingerprint,
        LayoutVideoFingerprint,
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        fun MethodFingerprintResult.injectHideCalls() {
            val index = this.scanResult.patternScanResult!!.endIndex
            with (this.mutableMethod) {
                val register = (this.instruction(index) as Instruction21c).registerA
                this.implementation!!.injectHideCall(index + 1, register, "layout/PlayerPatch", "hideEndscreen")
            }
        }
        
        listOf(
            LayoutCircleFingerprint,
            LayoutIconFingerprint,
            LayoutVideoFingerprint
        ).forEach {
            it.result?.injectHideCalls() ?: return it.toErrorResult()
        }

        /*
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

package app.revanced.patches.youtube.layout.fullscreen.flimstripoverlay.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.fullscreen.flimstripoverlay.fingerprints.ScrubbingLabelFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.FULLSCREEN
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction

@Patch
@Name("hide-filmstrip-overlay")
@Description("Hide flimstrip overlay on swipe controls.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class HideFilmstripOverlayPatch : BytecodePatch(
    listOf(ScrubbingLabelFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        val result = ScrubbingLabelFingerprint.result?: return ScrubbingLabelFingerprint.toErrorResult()

        result.mutableMethod.hook(result.scanResult.patternScanResult!!.endIndex - 1)

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: FULLSCREEN_SETTINGS",
                "SETTINGS: HIDE_FILMSTRIP_OVERLAY"
            )
        )

        SettingsPatch.updatePatchStatus("hide-filmstrip-overlay")

        return PatchResultSuccess()
    }
    private companion object {
        fun MutableMethod.hook(index: Int) {
            val targetInstruction = getInstruction<TwoRegisterInstruction>(index)
            val fieldReference = getInstruction<ReferenceInstruction>(index).reference
            replaceInstruction(
                index,
                "invoke-static {v${targetInstruction.registerA}}, $FULLSCREEN->hideFilmstripOverlay(Z)Z"
            )

            addInstructions(
                index + 1, """
                    move-result v${targetInstruction.registerA}
                    iput-boolean v${targetInstruction.registerA}, v${targetInstruction.registerB}, $fieldReference
                    """
            )
        }
    }
}

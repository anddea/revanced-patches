package app.revanced.patches.youtube.fullscreen.compactcontrolsoverlay.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.fingerprints.YouTubeControlsOverlayFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.FULLSCREEN
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("enable-compact-controls-overlay")
@Description("Enable compact control overlay.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class CompactControlsOverlayPatch : BytecodePatch(
    listOf(YouTubeControlsOverlayFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        YouTubeControlsOverlayFingerprint.result?.let {
            with(
                context
                    .toMethodWalker(it.method)
                    .nextMethod(it.scanResult.patternScanResult!!.startIndex, true)
                    .getMethod() as MutableMethod
            ) {
                val insertIndex = implementation!!.instructions.size - 1
                val targetRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex,
                    """
                        invoke-static {v$targetRegister}, $FULLSCREEN->enableCompactControlsOverlay(Z)Z
                        move-result v$targetRegister
                    """
                )
            }
        } ?: return YouTubeControlsOverlayFingerprint.toErrorResult()

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: FULLSCREEN_SETTINGS",
                "SETTINGS: ENABLE_COMPACT_CONTROLS_OVERLAY"
            )
        )

        SettingsPatch.updatePatchStatus("enable-compact-controls-overlay")

        return PatchResultSuccess()
    }
}

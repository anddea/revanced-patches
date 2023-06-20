package app.revanced.patches.youtube.layout.tooltip.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.layout.tooltip.fingerprints.TooltipContentViewFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch

@Patch
@Name("hide-tooltip-content")
@Description("Hides the tooltip box that appears on first install.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class TooltipContentViewPatch : BytecodePatch(
    listOf(TooltipContentViewFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        TooltipContentViewFingerprint.result?.mutableMethod?.addInstruction(
            0,
            "return-void"
        ) ?: return TooltipContentViewFingerprint.toErrorResult()

        /**
         * Add settings
         */
        SettingsPatch.updatePatchStatus("hide-tooltip-content")

        return PatchResultSuccess()
    }
}

package app.revanced.patches.music.general.tooltip.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.general.tooltip.fingerprints.TooltipContentViewFingerprint
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.resourceid.patch.SharedResourceIdPatch

@Patch
@Name("Hide tooltip content")
@Description("Hides the tooltip box that appears on first install.")
@DependsOn([SharedResourceIdPatch::class])
@MusicCompatibility
class TooltipContentViewPatch : BytecodePatch(
    listOf(TooltipContentViewFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        TooltipContentViewFingerprint.result?.mutableMethod?.addInstruction(
            0,
            "return-void"
        ) ?: throw TooltipContentViewFingerprint.exception

    }
}

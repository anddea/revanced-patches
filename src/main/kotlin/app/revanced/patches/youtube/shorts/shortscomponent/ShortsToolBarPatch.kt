package app.revanced.patches.youtube.shorts.shortscomponent

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.shorts.shortscomponent.fingerprints.ToolBarBannerFingerprint
import app.revanced.patches.youtube.utils.fingerprints.ToolBarPatchFingerprint
import app.revanced.util.integrations.Constants.SHORTS

object ShortsToolBarPatch : BytecodePatch(
    setOf(
        ToolBarBannerFingerprint,
        ToolBarPatchFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {
        ToolBarBannerFingerprint.result?.let {
            val targetMethod = context
                .toMethodWalker(it.method)
                .nextMethod(it.scanResult.patternScanResult!!.endIndex, true)
                .getMethod() as MutableMethod

            targetMethod.apply {
                addInstructionsWithLabels(
                    0,
                    """
                        invoke-static {}, $SHORTS->hideShortsToolBarBanner()Z
                        move-result v0
                        if-nez v0, :hide
                        """,
                    ExternalLabel("hide", getInstruction(implementation!!.instructions.size - 1))
                )
            }
        } ?: throw ToolBarBannerFingerprint.exception

        ToolBarPatchFingerprint.result?.let {
            it.mutableMethod.apply {
                addInstruction(
                    0,
                    "invoke-static {p0, p1}, $SHORTS->hideShortsToolBarButton(Ljava/lang/String;Landroid/view/View;)V"
                )
            }
        } ?: throw ToolBarPatchFingerprint.exception
    }
}

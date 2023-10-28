package app.revanced.patches.youtube.shorts.shortscomponent

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.shorts.shortscomponent.fingerprints.ShortsCommentFingerprint
import app.revanced.patches.youtube.shorts.shortscomponent.fingerprints.ToolBarBannerFingerprint
import app.revanced.patches.youtube.utils.fingerprints.SetToolBarPaddingFingerprint
import app.revanced.patches.youtube.utils.navbarindex.NavBarIndexHookPatch
import app.revanced.patches.youtube.utils.navbarindex.NavBarIndexHookPatch.injectIndex
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ToolBarPaddingHome
import app.revanced.patches.youtube.utils.toolbar.ToolBarHookPatch
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.integrations.Constants.SHORTS
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Patch(
    dependencies =
    [
        NavBarIndexHookPatch::class,
        ToolBarHookPatch::class
    ]
)
object ShortsToolBarPatch : BytecodePatch(
    setOf(
        SetToolBarPaddingFingerprint,
        ShortsCommentFingerprint,
        ToolBarBannerFingerprint
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

        ToolBarHookPatch.injectCall("$SHORTS->hideShortsToolBarButton")

        SetToolBarPaddingFingerprint.result?.let {
            val targetMethod = context
                .toMethodWalker(it.method)
                .nextMethod(it.mutableMethod.getWideLiteralIndex(ToolBarPaddingHome) + 3, true)
                .getMethod() as MutableMethod

            targetMethod.apply {
                val targetParameter = getInstruction<ReferenceInstruction>(0).reference
                if (!targetParameter.toString().endsWith("Landroid/support/v7/widget/Toolbar;"))
                    throw PatchException("Method signature parameter did not match: $targetParameter")
                val targetRegister = getInstruction<TwoRegisterInstruction>(0).registerA

                addInstruction(
                    1,
                    "invoke-static {v$targetRegister}, $SHORTS->hideShortsToolBar(Landroid/support/v7/widget/Toolbar;)V"
                )
            }
        } ?: throw SetToolBarPaddingFingerprint.exception

        ShortsCommentFingerprint.injectIndex(1)
    }
}

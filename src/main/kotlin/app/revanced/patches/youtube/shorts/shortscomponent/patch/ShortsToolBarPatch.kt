package app.revanced.patches.youtube.shorts.shortscomponent.patch

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.shorts.shortscomponent.fingerprints.ShortsCommentFingerprint
import app.revanced.patches.youtube.utils.fingerprints.SetToolBarPaddingFingerprint
import app.revanced.patches.youtube.utils.navbarindex.patch.NavBarIndexHookPatch.Companion.injectIndex
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.ToolBarPaddingHome
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.integrations.Constants.SHORTS
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

class ShortsToolBarPatch : BytecodePatch(
    listOf(
        SetToolBarPaddingFingerprint,
        ShortsCommentFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {
        SetToolBarPaddingFingerprint.result?.let {
            val targetIndex = it.mutableMethod.getWideLiteralIndex(ToolBarPaddingHome) + 3
            (context.toMethodWalker(it.method)
                .nextMethod(targetIndex, true)
                .getMethod() as MutableMethod
                    ).apply {
                    val targetParameter = getInstruction<ReferenceInstruction>(0).reference
                    if (!targetParameter.toString().endsWith("Landroid/support/v7/widget/Toolbar;"))
                        throw PatchException("Method signature parameter did not match: $targetParameter")
                    val targetRegister = getInstruction<TwoRegisterInstruction>(0).registerA

                    addInstruction(
                        1,
                        "invoke-static {v$targetRegister}, $SHORTS->hideShortsPlayerToolBar(Landroid/support/v7/widget/Toolbar;)V"
                    )
                }

        } ?: throw SetToolBarPaddingFingerprint.exception

        ShortsCommentFingerprint.injectIndex(1)

    }
}

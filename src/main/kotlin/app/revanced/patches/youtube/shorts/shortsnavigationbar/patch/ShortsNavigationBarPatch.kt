package app.revanced.patches.youtube.shorts.shortsnavigationbar.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.shorts.shortsnavigationbar.fingerprints.BottomNavigationBarFingerprint
import app.revanced.patches.youtube.shorts.shortsnavigationbar.fingerprints.RenderBottomNavigationBarFingerprint
import app.revanced.patches.youtube.shorts.shortsnavigationbar.fingerprints.SetPivotBarFingerprint
import app.revanced.patches.youtube.utils.fingerprints.PivotBarCreateButtonViewFingerprint
import app.revanced.util.integrations.Constants.SHORTS
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

class ShortsNavigationBarPatch : BytecodePatch(
    listOf(
        BottomNavigationBarFingerprint,
        PivotBarCreateButtonViewFingerprint,
        RenderBottomNavigationBarFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        PivotBarCreateButtonViewFingerprint.result?.let { parentResult ->
            SetPivotBarFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.let {
                it.mutableMethod.apply {
                    val startIndex = it.scanResult.patternScanResult!!.startIndex
                    val register = getInstruction<OneRegisterInstruction>(startIndex).registerA

                    addInstruction(
                        startIndex + 1,
                        "sput-object v$register, $SHORTS->pivotBar:Ljava/lang/Object;"
                    )
                }
            } ?: return SetPivotBarFingerprint.toErrorResult()
        } ?: return PivotBarCreateButtonViewFingerprint.toErrorResult()

        RenderBottomNavigationBarFingerprint.result?.let {
            (context
                .toMethodWalker(it.method)
                .nextMethod(it.scanResult.patternScanResult!!.endIndex, true)
                .getMethod() as MutableMethod
                    ).apply {
                    addInstruction(
                        0,
                        "invoke-static {}, $SHORTS->hideShortsPlayerNavigationBar()V"
                    )
                }
        } ?: return RenderBottomNavigationBarFingerprint.toErrorResult()

        BottomNavigationBarFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.endIndex
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$insertRegister}, $SHORTS->hideShortsPlayerNavigationBar(Landroid/view/View;)Landroid/view/View;
                        move-result-object v$insertRegister
                        """
                )
            }
        } ?: return BottomNavigationBarFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}
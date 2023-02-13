package app.revanced.patches.youtube.layout.general.pivotbar.createbutton.bytecode.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.fingerprints.PivotBarCreateButtonViewFingerprint
import app.revanced.patches.youtube.layout.general.pivotbar.createbutton.bytecode.fingerprints.PivotBarFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourcdIdPatch
import app.revanced.util.integrations.Constants.GENERAL_LAYOUT
import app.revanced.util.pivotbar.InjectionUtils.REGISTER_TEMPLATE_REPLACEMENT
import app.revanced.util.pivotbar.InjectionUtils.injectHook
import org.jf.dexlib2.dexbacked.reference.DexBackedMethodReference
import org.jf.dexlib2.iface.instruction.ReferenceInstruction

@Name("hide-create-button-bytecode-patch")
@DependsOn([SharedResourcdIdPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class CreateButtonRemoverBytecodePatch : BytecodePatch(
    listOf(
        PivotBarCreateButtonViewFingerprint,
        PivotBarFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        /*
         * Resolve fingerprints
         */

        PivotBarFingerprint.result?.let {
            val startIndex = it.scanResult.patternScanResult!!.startIndex
            val pivotBarInstructions = it.mutableMethod.implementation!!.instructions
            createRef = (pivotBarInstructions.elementAt(startIndex) as ReferenceInstruction).reference as DexBackedMethodReference
        } ?: return PivotBarFingerprint.toErrorResult()

        PivotBarCreateButtonViewFingerprint.result?.let { result ->
            with (result.mutableMethod){
                val createButtonInstructions = implementation!!.instructions
                createButtonInstructions.filter { instruction ->
                    val fieldReference = (instruction as? ReferenceInstruction)?.reference as? DexBackedMethodReference
                    fieldReference?.let { it.definingClass == createRef.definingClass && it.name == createRef.name } == true
                }.forEach { instruction ->
                    if (foundIndex == 0) {
                        foundIndex++
                        return@forEach
                    }

                    /*
                    * Inject hooks
                    */

                    injectHook(hook, createButtonInstructions.indexOf(instruction) + 2)

                    return PatchResultSuccess()
                }
                return PivotBarCreateButtonViewFingerprint.toErrorResult()
            }
        } ?: return PivotBarCreateButtonViewFingerprint.toErrorResult()
    }

    private companion object {
        const val hook =
            "invoke-static { v$REGISTER_TEMPLATE_REPLACEMENT }, $GENERAL_LAYOUT" +
            "->" +
            "hideCreateButton(Landroid/view/View;)V"

        lateinit var createRef: DexBackedMethodReference

        var foundIndex: Int = 0
    }
}

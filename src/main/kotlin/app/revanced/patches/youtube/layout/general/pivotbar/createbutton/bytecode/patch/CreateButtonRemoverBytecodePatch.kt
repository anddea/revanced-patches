package app.revanced.patches.youtube.layout.general.pivotbar.createbutton.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.youtube.layout.general.pivotbar.createbutton.bytecode.fingerprints.PivotBarCreateButtonViewFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourcdIdPatch
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.util.integrations.Constants.GENERAL_LAYOUT
import app.revanced.shared.util.pivotbar.InjectionUtils.injectHook
import app.revanced.shared.util.pivotbar.InjectionUtils.REGISTER_TEMPLATE_REPLACEMENT
import org.jf.dexlib2.dexbacked.reference.DexBackedMethodReference
import org.jf.dexlib2.iface.instruction.ReferenceInstruction

@Name("hide-create-button-bytecode-patch")
@DependsOn([SharedResourcdIdPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class CreateButtonRemoverBytecodePatch : BytecodePatch(
    listOf(PivotBarCreateButtonViewFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        /*
         * Resolve fingerprints
         */

        val createButtonResult = PivotBarCreateButtonViewFingerprint.result ?: return PatchResultError("PivotBarCreateButtonViewFingerprint failed")
        val createButtonMethod = createButtonResult.mutableMethod
        val createButtonInstructions = createButtonMethod.implementation!!.instructions

        createButtonInstructions.filter { instruction ->
            val fieldReference = (instruction as? ReferenceInstruction)?.reference as? DexBackedMethodReference
            fieldReference?.let { it.definingClass == "Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotBar;" && it.name == "c" } == true
        }.forEach { instruction ->
            if (!isSeondary) {
                isSeondary = true;
                return@forEach
            }

            insertIndex = createButtonInstructions.indexOf(instruction) + 2

            /*
            * Inject hooks
            */

            createButtonMethod.injectHook(hook, insertIndex)

            return PatchResultSuccess()
        }

        return PatchResultError("Could not find the method to hook.")
    }

    internal companion object {
        const val hook =
            "invoke-static { v$REGISTER_TEMPLATE_REPLACEMENT }, $GENERAL_LAYOUT" +
            "->" +
            "hideCreateButton(Landroid/view/View;)V"

        private var insertIndex: Int = 0
        private var isSeondary: Boolean = false
    }
}

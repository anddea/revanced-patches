package app.revanced.patches.youtube.utils.viewgroup

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.utils.fingerprints.AccountMenuParentFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.viewgroup.fingerprints.SetViewGroupMarginFingerprint
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

@Patch(
    description = "Hook YouTube to use ViewGroup.MarginLayoutParams in the integration.",
    dependencies = [SharedResourceIdPatch::class],
)
object ViewGroupMarginLayoutParamsHookPatch : BytecodePatch(
    setOf(AccountMenuParentFingerprint)
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$UTILS_PATH/ViewGroupMarginLayoutParamsPatch;"

    override fun execute(context: BytecodeContext) {

        val method = context.findClass(INTEGRATIONS_CLASS_DESCRIPTOR)?.mutableClass?.methods?.first { method ->
            method.name == "hideViewGroupByMarginLayoutParams"
        } ?: throw PatchException("Could not find hideViewGroupByMarginLayoutParams method")

        SetViewGroupMarginFingerprint.resolve(
            context,
            AccountMenuParentFingerprint.resultOrThrow().classDef
        )
        SetViewGroupMarginFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val setViewGroupMarginIndex = it.scanResult.patternScanResult!!.startIndex
                val setViewGroupMarginReference =
                    getInstruction<ReferenceInstruction>(setViewGroupMarginIndex).reference

                method.addInstructions(
                    0, """
                        const/4 v0, 0x0
                        invoke-static {p0, v0, v0}, $setViewGroupMarginReference
                        """
                )
            }
        }
    }
}
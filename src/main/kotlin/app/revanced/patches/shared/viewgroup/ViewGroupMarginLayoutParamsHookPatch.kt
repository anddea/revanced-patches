package app.revanced.patches.shared.viewgroup

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.shared.viewgroup.fingerprints.ViewGroupMarginFingerprint
import app.revanced.patches.shared.viewgroup.fingerprints.ViewGroupMarginParentFingerprint
import app.revanced.patches.shared.integrations.Constants.INTEGRATIONS_UTILS_CLASS_DESCRIPTOR
import app.revanced.util.resultOrThrow

@Patch(
    description = "Hook YouTube or YouTube Music to use ViewGroup.MarginLayoutParams in the integration.",
)
object ViewGroupMarginLayoutParamsHookPatch : BytecodePatch(
    setOf(ViewGroupMarginParentFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        val method =
            context.findClass(INTEGRATIONS_UTILS_CLASS_DESCRIPTOR)?.mutableClass?.methods?.first { method ->
                method.name == "hideViewGroupByMarginLayoutParams"
            } ?: throw PatchException("Could not find hideViewGroupByMarginLayoutParams method")

        ViewGroupMarginFingerprint.resolve(
            context,
            ViewGroupMarginParentFingerprint.resultOrThrow().classDef
        )
        ViewGroupMarginFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val setViewGroupMarginCall = "$definingClass->$name(Landroid/view/View;II)V"

                method.addInstructions(
                    0, """
                        const/4 v0, 0x0
                        invoke-static {p0, v0, v0}, $setViewGroupMarginCall
                        """
                )
            }
        }
    }
}
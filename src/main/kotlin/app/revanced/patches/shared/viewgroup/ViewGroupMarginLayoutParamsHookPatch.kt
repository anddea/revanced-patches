package app.revanced.patches.shared.viewgroup

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.shared.integrations.Constants.INTEGRATIONS_UTILS_CLASS_DESCRIPTOR
import app.revanced.patches.shared.viewgroup.fingerprints.ViewGroupMarginFingerprint
import app.revanced.patches.shared.viewgroup.fingerprints.ViewGroupMarginParentFingerprint
import app.revanced.util.alsoResolve
import app.revanced.util.findMethodOrThrow

@Patch(
    description = "Hook YouTube or YouTube Music to use ViewGroup.MarginLayoutParams in the integration.",
)
object ViewGroupMarginLayoutParamsHookPatch : BytecodePatch(
    setOf(ViewGroupMarginParentFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        val setViewGroupMarginCall = with(
            ViewGroupMarginFingerprint.alsoResolve(
                context, ViewGroupMarginParentFingerprint
            ).mutableMethod
        ) {
            "$definingClass->$name(Landroid/view/View;II)V"
        }

        context.findMethodOrThrow(INTEGRATIONS_UTILS_CLASS_DESCRIPTOR) {
            name == "hideViewGroupByMarginLayoutParams"
        }.addInstructions(
            0, """
                const/4 v0, 0x0
                invoke-static {p0, v0, v0}, $setViewGroupMarginCall
                """
        )
    }
}
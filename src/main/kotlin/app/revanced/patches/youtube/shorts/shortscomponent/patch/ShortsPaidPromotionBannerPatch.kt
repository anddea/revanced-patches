package app.revanced.patches.youtube.shorts.shortscomponent.patch

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.shorts.shortscomponent.fingerprints.ShortsPaidPromotionFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.ReelPlayerBadge
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.ReelPlayerBadge2
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.integrations.Constants.SHORTS
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

class ShortsPaidPromotionBannerPatch : BytecodePatch(
    listOf(ShortsPaidPromotionFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        ShortsPaidPromotionFingerprint.result?.let {
            it.mutableMethod.apply {
                val primaryIndex = getWideLiteralIndex(ReelPlayerBadge) + 3
                val secondaryIndex = getWideLiteralIndex(ReelPlayerBadge2) + 3

                if (primaryIndex > secondaryIndex) {
                    insertHook(primaryIndex)
                    insertHook(secondaryIndex)
                } else {
                    insertHook(secondaryIndex)
                    insertHook(primaryIndex)
                }
            }
        } ?: throw ShortsPaidPromotionFingerprint.exception

    }

    private companion object {
        fun MutableMethod.insertHook(insertIndex: Int) {
            val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

            addInstructions(
                insertIndex + 1, """
                    invoke-static {v$insertRegister}, $SHORTS->hideShortsPlayerPaidPromotionBanner(Landroid/view/ViewStub;)Landroid/view/ViewStub;
                    move-result-object v$insertRegister
                    """
            )
        }
    }
}

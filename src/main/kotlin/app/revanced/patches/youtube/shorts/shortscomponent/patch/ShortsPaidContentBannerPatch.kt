package app.revanced.patches.youtube.shorts.shortscomponent.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.shorts.shortscomponent.fingerprints.ShortsPaidContentFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.ReelPlayerBadge
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.ReelPlayerBadge2
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.integrations.Constants.SHORTS
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Name("hide-shorts-paid-content")
@YouTubeCompatibility
@Version("0.0.1")
class ShortsPaidContentBannerPatch : BytecodePatch(
    listOf(ShortsPaidContentFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {
        ShortsPaidContentFingerprint.result?.let {
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
        } ?: return ShortsPaidContentFingerprint.toErrorResult()

        return PatchResultSuccess()
    }

    private companion object {
        fun MutableMethod.insertHook(insertIndex: Int) {
            val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

            addInstructions(
                insertIndex + 1, """
                    invoke-static {v$insertRegister}, $SHORTS->hideShortsPlayerPaidContent(Landroid/view/ViewStub;)Landroid/view/ViewStub;
                    move-result-object v$insertRegister
                    """
            )
        }
    }
}

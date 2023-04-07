package app.revanced.patches.youtube.layout.general.shortscomponent.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.general.shortscomponent.fingerprints.ShortsPaidContentFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.util.integrations.Constants
import app.revanced.util.integrations.Constants.GENERAL
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.WideLiteralInstruction

@Name("hide-shorts-paid-content")
@DependsOn([SharedResourceIdPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class ShortsPaidContentBannerPatch : BytecodePatch(
    listOf(ShortsPaidContentFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {
        ShortsPaidContentFingerprint.result?.mutableMethod?.let { method ->
            with (method.implementation!!.instructions) {
                val primaryIndex = this.indexOfFirst {
                    (it as? WideLiteralInstruction)?.wideLiteral == SharedResourceIdPatch.reelPlayerBadgeLabelId
                } + 3

                val secondaryIndex = this.indexOfFirst {
                    (it as? WideLiteralInstruction)?.wideLiteral == SharedResourceIdPatch.reelPlayerBadge2LabelId
                } + 3

                if (primaryIndex > secondaryIndex) {
                    method.insertHook(primaryIndex)
                    method.insertHook(secondaryIndex)
                } else {
                    method.insertHook(secondaryIndex)
                    method.insertHook(primaryIndex)
                }
            }
        } ?: return ShortsPaidContentFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
    private companion object {
        fun MutableMethod.insertHook(insertIndex: Int) {
            val insertRegister = (instruction(insertIndex) as OneRegisterInstruction).registerA
            addInstructions(
                insertIndex + 1, """
                    invoke-static {v$insertRegister}, $GENERAL->hideShortsPlayerPaidContent(Landroid/view/ViewStub;)Landroid/view/ViewStub;
                    move-result-object v$insertRegister
                """
            )
        }
    }
}

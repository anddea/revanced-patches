package app.revanced.patches.youtube.shorts.shortscomponent.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.youtube.shorts.shortscomponent.fingerprints.ShortsSubscriptionsFingerprint
import app.revanced.patches.youtube.shorts.shortscomponent.fingerprints.ShortsSubscriptionsTabletFingerprint
import app.revanced.patches.youtube.shorts.shortscomponent.fingerprints.ShortsSubscriptionsTabletParentFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.ReelPlayerFooter
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.ReelPlayerPausedStateButton
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.integrations.Constants.SHORTS
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

class ShortsSubscriptionsButtonPatch : BytecodePatch(
    listOf(
        ShortsSubscriptionsFingerprint,
        ShortsSubscriptionsTabletParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        ShortsSubscriptionsFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = getWideLiteralIndex(ReelPlayerPausedStateButton) + 2
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex + 1,
                    "invoke-static {v$insertRegister}, $SHORTS->hideShortsPlayerSubscriptionsButton(Landroid/view/View;)V"
                )
            }
        } ?: return ShortsSubscriptionsFingerprint.toErrorResult()

        ShortsSubscriptionsTabletParentFingerprint.result?.let { parentResult ->
            parentResult.mutableMethod.apply {
                val targetIndex = getWideLiteralIndex(ReelPlayerFooter) - 1
                if (getInstruction(targetIndex).opcode != Opcode.IPUT) return ShortsSubscriptionsTabletFingerprint.toErrorResult()
                subscriptionFieldReference =
                    (getInstruction<ReferenceInstruction>(targetIndex)).reference as FieldReference
            }

            ShortsSubscriptionsTabletFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.result?.mutableMethod?.let {
                with(it.implementation!!.instructions) {
                    filter { instruction ->
                        val fieldReference =
                            (instruction as? ReferenceInstruction)?.reference as? FieldReference
                        instruction.opcode == Opcode.IGET && fieldReference == subscriptionFieldReference
                    }.forEach { instruction ->
                        val insertIndex = indexOf(instruction) + 1
                        val register = (instruction as TwoRegisterInstruction).registerA

                        it.addInstructions(
                            insertIndex, """
                                invoke-static {v$register}, $SHORTS->hideShortsPlayerSubscriptionsButton(I)I
                                move-result v$register
                                """
                        )
                    }
                }
            } ?: return ShortsSubscriptionsTabletFingerprint.toErrorResult()
        } ?: return ShortsSubscriptionsTabletParentFingerprint.toErrorResult()

        return PatchResultSuccess()
    }

    private companion object {
        private lateinit var subscriptionFieldReference: FieldReference
    }
}

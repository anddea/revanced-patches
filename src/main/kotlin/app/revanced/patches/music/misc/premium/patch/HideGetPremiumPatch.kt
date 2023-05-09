package app.revanced.patches.music.misc.premium.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.music.misc.premium.fingerprints.AccountMenuFooterFingerprint
import app.revanced.patches.music.misc.premium.fingerprints.HideGetPremiumFingerprint
import app.revanced.patches.music.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.music.misc.resourceid.patch.SharedResourceIdPatch.Companion.privacyTosFooterId
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.util.bytecode.getWideLiteralIndex
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction
import org.jf.dexlib2.iface.reference.Reference

@Patch
@Name("hide-get-premium")
@Description("Removes all \"Get Premium\" evidences from the avatar menu.")
@DependsOn([SharedResourceIdPatch::class])
@YouTubeMusicCompatibility
@Version("0.0.1")
class HideGetPremiumPatch : BytecodePatch(
    listOf(
        AccountMenuFooterFingerprint,
        HideGetPremiumFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        HideGetPremiumFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.startIndex
                val register = instruction<TwoRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex + 1,
                    "const/4 v$register, 0x0"
                )
            }
        } ?: return HideGetPremiumFingerprint.toErrorResult()


        AccountMenuFooterFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = getWideLiteralIndex(privacyTosFooterId) + 4
                targetReference = instruction<ReferenceInstruction>(targetIndex + 1).reference

                with (context
                    .toMethodWalker(this)
                    .nextMethod(targetIndex, true)
                    .getMethod() as MutableMethod
                ) {
                    this.implementation!!.instructions.apply {
                        for ((index, instruction) in withIndex()) {
                            if (instruction.opcode != Opcode.IGET_OBJECT) continue

                            if (instruction<ReferenceInstruction>(index).reference == targetReference) {
                                val targetRegister = instruction<OneRegisterInstruction>(index + 2).registerA

                                addInstruction(
                                    index,
                                    "const/16 v$targetRegister, 0x8"
                                )

                                break
                            }
                        }
                    }
                }
            }
        } ?: return AccountMenuFooterFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
    private companion object{
        lateinit var targetReference: Reference
    }
}

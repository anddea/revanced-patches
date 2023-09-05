package app.revanced.patches.music.misc.premium.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.music.misc.premium.fingerprints.AccountMenuFooterFingerprint
import app.revanced.patches.music.misc.premium.fingerprints.HideGetPremiumFingerprint
import app.revanced.patches.music.misc.premium.fingerprints.MembershipSettingsFingerprint
import app.revanced.patches.music.misc.premium.fingerprints.MembershipSettingsParentFingerprint
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.music.utils.resourceid.patch.SharedResourceIdPatch.Companion.PrivacyTosFooter
import app.revanced.util.bytecode.getWideLiteralIndex
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.Reference

@Patch
@Name("Hide get premium")
@Description("Hides \"Get Premium\" label from the account menu or settings.")
@DependsOn([SharedResourceIdPatch::class])
@MusicCompatibility
class HideGetPremiumPatch : BytecodePatch(
    listOf(
        AccountMenuFooterFingerprint,
        HideGetPremiumFingerprint,
        MembershipSettingsParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        HideGetPremiumFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.startIndex
                val register = getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex + 1,
                    "const/4 v$register, 0x0"
                )
            }
        } ?: throw HideGetPremiumFingerprint.exception


        AccountMenuFooterFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = getWideLiteralIndex(PrivacyTosFooter) + 4
                targetReference = getInstruction<ReferenceInstruction>(targetIndex + 1).reference

                with(
                    context
                        .toMethodWalker(this)
                        .nextMethod(targetIndex, true)
                        .getMethod() as MutableMethod
                ) {
                    this.implementation!!.instructions.apply {
                        for ((index, instruction) in withIndex()) {
                            if (instruction.opcode != Opcode.IGET_OBJECT) continue

                            if (getInstruction<ReferenceInstruction>(index).reference == targetReference) {
                                val targetRegister =
                                    getInstruction<OneRegisterInstruction>(index + 2).registerA

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
        } ?: throw AccountMenuFooterFingerprint.exception

        MembershipSettingsParentFingerprint.result?.let { parentResult ->
            MembershipSettingsFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.result?.let {
                it.mutableMethod.apply {
                    addInstructions(
                        0, """
                            const/4 v0, 0x0
                            return-object v0
                            """
                    )
                }
            } ?: throw MembershipSettingsFingerprint.exception
        } ?: throw MembershipSettingsParentFingerprint.exception

    }

    private companion object {
        lateinit var targetReference: Reference
    }
}

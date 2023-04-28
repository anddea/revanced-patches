package app.revanced.patches.music.misc.premium.patch

import app.revanced.extensions.findMutableMethodOf
import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.music.misc.premium.fingerprints.HideGetPremiumFingerprint
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.patches.shared.patch.mapping.ResourceMappingPatch
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction
import org.jf.dexlib2.iface.instruction.formats.Instruction31i
import org.jf.dexlib2.iface.reference.FieldReference

@Patch
@Name("hide-get-premium")
@Description("Removes all \"Get Premium\" evidences from the avatar menu.")
@DependsOn([ResourceMappingPatch::class])
@YouTubeMusicCompatibility
@Version("0.0.1")
class HideGetPremiumPatch : BytecodePatch(
    listOf(
        HideGetPremiumFingerprint
    )
) {
    // list of resource names to get the id of
    private val resourceIds = arrayOf(
        "id" to "privacy_tos_footer"
    ).map { (type, name) ->
        ResourceMappingPatch
            .resourceMappings
            .single { it.type == type && it.name == name }.id
    }

    override fun execute(context: BytecodeContext): PatchResult {

        HideGetPremiumFingerprint.result?.let {
            with (it.mutableMethod) {
                val insertIndex = it.scanResult.patternScanResult!!.startIndex
                val register = (implementation!!.instructions[insertIndex] as TwoRegisterInstruction).registerA

                addInstruction(
                    insertIndex + 1,
                    "const/4 v$register, 0x0"
                )
            }
        } ?: return HideGetPremiumFingerprint.toErrorResult()

        context.classes.forEach { classDef ->
            classDef.methods.forEach { method ->
                with(method.implementation) {
                    this?.instructions?.forEachIndexed { index, instruction ->
                        when (instruction.opcode) {
                            Opcode.CONST -> {
                                when ((instruction as Instruction31i).wideLiteral) {
                                    resourceIds[0] -> {
                                        val viewIndex = index + 5
                                        val viewInstruction = instructions.elementAt(viewIndex)
                                        if (viewInstruction.opcode != Opcode.IGET_OBJECT) return@forEachIndexed

                                        val mutableMethod = context.proxy(classDef).mutableClass.findMutableMethodOf(method)

                                        val viewReference = (viewInstruction as? ReferenceInstruction)?.reference as? FieldReference

                                        with (context
                                            .toMethodWalker(mutableMethod)
                                            .nextMethod(viewIndex - 1, true)
                                            .getMethod() as MutableMethod
                                        ) {
                                            val viewInstructions = implementation!!.instructions

                                            for ((targetIndex, targetInstruction) in viewInstructions.withIndex()) {
                                                if (targetInstruction.opcode != Opcode.IGET_OBJECT) continue

                                                val indexReference = (targetInstruction as ReferenceInstruction).reference as FieldReference

                                                if (indexReference == viewReference) {
                                                    val targetRegister = (viewInstructions.elementAt(targetIndex + 2) as OneRegisterInstruction).registerA
                                                    addInstruction(
                                                        targetIndex,
                                                        "const/16 v$targetRegister, 0x8"
                                                    )
                                                    break
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            else -> return@forEachIndexed
                        }
                    }
                }
            }
        }

        return PatchResultSuccess()
    }
}

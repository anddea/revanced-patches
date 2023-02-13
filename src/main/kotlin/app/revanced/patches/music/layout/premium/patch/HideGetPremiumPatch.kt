package app.revanced.patches.music.layout.premium.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.layout.premium.fingerprints.HideGetPremiumFingerprint
import app.revanced.patches.music.misc.integrations.patch.MusicIntegrationsPatch
import app.revanced.shared.annotation.YouTubeMusicCompatibility
import app.revanced.shared.extensions.findMutableMethodOf
import app.revanced.shared.extensions.toErrorResult
import app.revanced.shared.patches.mapping.ResourceMappingPatch
import app.revanced.shared.util.integrations.Constants.INTEGRATIONS_PATH
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction
import org.jf.dexlib2.iface.instruction.formats.Instruction22c
import org.jf.dexlib2.iface.instruction.formats.Instruction31i

@Patch
@Name("hide-get-premium")
@Description("Removes all \"Get Premium\" evidences from the avatar menu.")
@DependsOn([MusicIntegrationsPatch::class])
@YouTubeMusicCompatibility
@Version("0.0.1")
class HideGetPremiumPatch : BytecodePatch(
    listOf(
        HideGetPremiumFingerprint
    )
) {
    // list of resource names to get the id of
    private val resourceIds = arrayOf(
        "unlimited_panel"
    ).map { name ->
        ResourceMappingPatch.resourceMappings.single { it.name == name }.id
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
                                        val insertIndex = index + 3
                                        val iPutInstruction = instructions.elementAt(insertIndex)
                                        if (iPutInstruction.opcode != Opcode.IPUT_OBJECT) return@forEachIndexed

                                        val mutableMethod = context.proxy(classDef).mutableClass.findMutableMethodOf(method)

                                        val viewRegister = (iPutInstruction as Instruction22c).registerA

                                        mutableMethod.addInstruction(
                                            insertIndex,
                                            "invoke-static {v$viewRegister}, $INTEGRATIONS_PATH/adremover/AdRemoverAPI;->HideViewWithLayout1dp(Landroid/view/View;)V"
                                        )
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

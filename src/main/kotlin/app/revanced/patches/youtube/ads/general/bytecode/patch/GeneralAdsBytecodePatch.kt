package app.revanced.patches.youtube.ads.general.bytecode.patch

import app.revanced.extensions.findMutableMethodOf
import app.revanced.extensions.injectHideCall
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.util.bytecode.BytecodeHelper.updatePatchStatus
import org.jf.dexlib2.iface.instruction.formats.Instruction31i
import org.jf.dexlib2.iface.instruction.formats.Instruction35c

@Name("hide-general-ads-bytecode-patch")
@DependsOn([SharedResourceIdPatch::class])
@Version("0.0.1")
class GeneralAdsBytecodePatch : BytecodePatch() {
    override fun execute(context: BytecodeContext): PatchResult {
        context.classes.forEach { classDef ->
            classDef.methods.forEach { method ->
                with(method.implementation) {
                    this?.instructions?.forEachIndexed { index, instruction ->
                        if (instruction.opcode != org.jf.dexlib2.Opcode.CONST)
                            return@forEachIndexed
                        // Instruction to store the id adAttribution into a register
                        if ((instruction as Instruction31i).wideLiteral != SharedResourceIdPatch.adAttributionLabelId)
                            return@forEachIndexed

                        val insertIndex = index + 1

                        // Call to get the view with the id adAttribution
                        with(instructions.elementAt(insertIndex)) {
                            if (opcode != org.jf.dexlib2.Opcode.INVOKE_VIRTUAL)
                                return@forEachIndexed

                            // Hide the view
                            val viewRegister = (this as Instruction35c).registerC
                            context.proxy(classDef)
                                .mutableClass
                                .findMutableMethodOf(method)
                                .implementation!!.injectHideCall(insertIndex, viewRegister, "ads/GeneralAdsPatch", "hideAdAttributionView")
                        }
                    }
                }
            }
        }
        context.updatePatchStatus("GeneralAds")

        return PatchResultSuccess()
    }
}

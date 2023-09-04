package app.revanced.patches.youtube.ads.general.bytecode.patch

import app.revanced.extensions.findMutableMethodOf
import app.revanced.extensions.injectHideCall
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.AdAttribution
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.bytecode.isWideLiteralExists
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c

@DependsOn([SharedResourceIdPatch::class])
@Suppress("LABEL_NAME_CLASH")
class GeneralAdsBytecodePatch : BytecodePatch() {
    override fun execute(context: BytecodeContext) {
        context.classes.forEach { classDef ->
            classDef.methods.forEach { method ->
                if (!method.isWideLiteralExists(AdAttribution))
                    return@forEach

                context.proxy(classDef)
                    .mutableClass
                    .findMutableMethodOf(method)
                    .apply {
                        val insertIndex = getWideLiteralIndex(AdAttribution) + 1
                        if (getInstruction(insertIndex).opcode != Opcode.INVOKE_VIRTUAL)
                            return@forEach

                        val viewRegister = getInstruction<Instruction35c>(insertIndex).registerC

                        this.implementation!!.injectHideCall(
                            insertIndex,
                            viewRegister,
                            "ads/AdsFilter",
                            "hideAdAttributionView"
                        )
                    }
            }
        }

    }
}

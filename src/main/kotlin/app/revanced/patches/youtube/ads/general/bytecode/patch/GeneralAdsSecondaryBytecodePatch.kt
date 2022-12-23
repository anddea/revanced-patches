package app.revanced.patches.youtube.ads.general.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.extensions.findMutableMethodOf
import app.revanced.shared.extensions.injectHideCall
import app.revanced.shared.patches.mapping.ResourceMappingPatch
import org.jf.dexlib2.iface.instruction.formats.*
import org.jf.dexlib2.Opcode

@Name("hide-general-ads-secondary-bytecode-patch")
@DependsOn([ResourceMappingPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class GeneralAdsSecondaryBytecodePatch : BytecodePatch() {
    private val resourceIds = arrayOf(
        "ad_attribution",
        "horizontal_card_list",
        "album_card"
    ).map { name ->
        ResourceMappingPatch.resourceMappings.single { it.name == name }.id
    }

    override fun execute(context: BytecodeContext): PatchResult {
        context.classes.forEach { classDef ->
            classDef.methods.forEach { method ->
                with(method.implementation) {
                    this?.instructions?.forEachIndexed { index, instruction ->
                        when (instruction.opcode) {
                            Opcode.CONST -> {
                                when ((instruction as Instruction31i).wideLiteral) {
                                    resourceIds[0] -> { // general ads
                                        val insertIndex = index + 1
                                        val invokeInstruction = instructions.elementAt(insertIndex)
                                        if (invokeInstruction.opcode != Opcode.INVOKE_VIRTUAL) return@forEachIndexed

                                        val mutableMethod = context.proxy(classDef).mutableClass.findMutableMethodOf(method)

                                        val viewRegister = (invokeInstruction as Instruction35c).registerC
                                        mutableMethod.implementation!!.injectHideCall(insertIndex, viewRegister, "ads/GeneralAdsPatch", "hideAdAttributionView")
                                    }

                                    resourceIds[1] -> { // breaking news
                                        val insertIndex = index + 4
                                        val invokeInstruction = instructions.elementAt(insertIndex)
                                        if (invokeInstruction.opcode != Opcode.CHECK_CAST) return@forEachIndexed

                                        val mutableMethod = context.proxy(classDef).mutableClass.findMutableMethodOf(method)

                                        val viewRegister = (invokeInstruction as Instruction21c).registerA
                                        mutableMethod.implementation!!.injectHideCall(insertIndex, viewRegister, "ads/GeneralAdsPatch", "hideBreakingNewsShelf")
                                    }

                                    resourceIds[2] -> { // album cards
                                        val insertIndex = index + 4
                                        val invokeInstruction = instructions.elementAt(insertIndex)
                                        if (invokeInstruction.opcode != Opcode.CHECK_CAST) return@forEachIndexed

                                        val mutableMethod = context.proxy(classDef).mutableClass.findMutableMethodOf(method)

                                        val viewRegister = (invokeInstruction as Instruction21c).registerA
                                        mutableMethod.implementation!!.injectHideCall(insertIndex, viewRegister, "ads/GeneralAdsPatch", "hideAlbumCards")
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

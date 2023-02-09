package app.revanced.patches.youtube.ads.general.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.extensions.findMutableMethodOf
import app.revanced.shared.extensions.injectHideCall
import app.revanced.shared.extensions.toResult
import app.revanced.shared.patches.mapping.ResourceMappingPatch
import app.revanced.shared.util.integrations.Constants.ADS_PATH
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.instruction.formats.*

@Name("hide-general-ads-secondary-bytecode-patch")
@DependsOn([ResourceMappingPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class GeneralAdsSecondaryBytecodePatch : BytecodePatch() {
    private val resourceIds = arrayOf(
        "id" to "ad_attribution",
        "layout" to "horizontal_card_list",
        "layout" to "album_card",
        "id" to "reel_player_badge",
        "id" to "reel_player_badge2",
        "id" to "reel_player_info_panel"
    ).map { (type, name) ->
        ResourceMappingPatch
            .resourceMappings
            .single { it.type == type && it.name == name }.id
    }
    private var patchSuccessArray = Array(resourceIds.size) {false}

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
                                        patchSuccessArray[0] = true;
                                    }

                                    resourceIds[1] -> { // breaking news
                                        val insertIndex = index + 4
                                        val invokeInstruction = instructions.elementAt(insertIndex)
                                        if (invokeInstruction.opcode != Opcode.CHECK_CAST) return@forEachIndexed

                                        val mutableMethod = context.proxy(classDef).mutableClass.findMutableMethodOf(method)

                                        val viewRegister = (invokeInstruction as Instruction21c).registerA
                                        mutableMethod.implementation!!.injectHideCall(insertIndex, viewRegister, "ads/GeneralAdsPatch", "hideBreakingNewsShelf")
                                        patchSuccessArray[1] = true;
                                    }

                                    resourceIds[2] -> { // album cards
                                        val insertIndex = index + 4
                                        val invokeInstruction = instructions.elementAt(insertIndex)
                                        if (invokeInstruction.opcode != Opcode.CHECK_CAST) return@forEachIndexed

                                        val mutableMethod = context.proxy(classDef).mutableClass.findMutableMethodOf(method)

                                        val viewRegister = (invokeInstruction as Instruction21c).registerA
                                        mutableMethod.implementation!!.injectHideCall(insertIndex, viewRegister, "ads/GeneralAdsPatch", "hideAlbumCards")
                                        patchSuccessArray[2] = true;
                                    }

                                    resourceIds[3], resourceIds[4] -> { // paid content banner
                                        val insertIndex = index + 3
                                        val invokeInstruction = instructions.elementAt(insertIndex)
                                        if (invokeInstruction.opcode != Opcode.CHECK_CAST) return@forEachIndexed

                                        val mutableMethod = context.proxy(classDef).mutableClass.findMutableMethodOf(method)
                                        val dummyRegister = (instructions.elementAt(index) as Instruction31i).registerA
                                        val viewRegister = (invokeInstruction as Instruction21c).registerA

                                        mutableMethod.addInjectCall(insertIndex, dummyRegister, viewRegister, "hidePaidContentBanner")

                                        patchSuccessArray[3] = true;
                                        patchSuccessArray[4] = true;
                                    }

                                    resourceIds[5] -> { // info panel
                                        val insertIndex = index + 3
                                        val invokeInstruction = instructions.elementAt(insertIndex)
                                        if (invokeInstruction.opcode != Opcode.CHECK_CAST) return@forEachIndexed

                                        val mutableMethod = context.proxy(classDef).mutableClass.findMutableMethodOf(method)
                                        val dummyRegister = (instructions.elementAt(index) as Instruction31i).registerA
                                        val viewRegister = (invokeInstruction as Instruction21c).registerA

                                        mutableMethod.addInjectCall(insertIndex + 7, dummyRegister, viewRegister, "hideInfoPanel")

                                        patchSuccessArray[5] = true;
                                    }
                                }
                            }
                            else -> return@forEachIndexed
                        }
                    }
                }
            }
        }
        return toResult(patchSuccessArray.indexOf(false))
    }

    private fun MutableMethod.addInjectCall(
	    index: Int,
	    dummyRegister: Int,
	    viewRegister: Int,
	    method: String
    ) {
        addInstructions(
            index + 1, """
                invoke-static {}, $ADS_PATH/GeneralAdsPatch;->$method()Z
                move-result v$dummyRegister
                if-eqz v$dummyRegister, :shown
                const v$viewRegister, 0x0
            """, listOf(ExternalLabel("shown", this.instruction(index + 1)))
        )
    }
}

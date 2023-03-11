package app.revanced.patches.youtube.layout.general.shortscomponent.patch

import app.revanced.extensions.findMutableMethodOf
import app.revanced.extensions.injectHideCall
import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.patch.mapping.ResourceMappingPatch
import app.revanced.patches.youtube.layout.general.shortscomponent.fingerprints.*
import app.revanced.patches.youtube.misc.litho.patch.LithoFilterPatch
import app.revanced.patches.youtube.misc.playertype.patch.PlayerTypeHookPatch
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.bytecode.BytecodeHelper.updatePatchStatus
import app.revanced.util.integrations.Constants.GENERAL_LAYOUT
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction
import org.jf.dexlib2.iface.instruction.WideLiteralInstruction
import org.jf.dexlib2.iface.instruction.formats.Instruction21c
import org.jf.dexlib2.iface.instruction.formats.Instruction31i
import org.jf.dexlib2.iface.reference.FieldReference

@Patch
@Name("hide-shorts-component")
@Description("Hides other Shorts components.")
@DependsOn(
    [
        LithoFilterPatch::class,
        PlayerTypeHookPatch::class,
        ResourceMappingPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class ShortsComponentPatch : BytecodePatch(
    listOf(SubscriptionsButtonTabletParentFingerprint)
) {

    // list of resource names to get the id of
    private val resourceIds = arrayOf(
        "ic_right_comment_32c",
        "reel_dyn_remix",
        "reel_player_paused_state_buttons"
    ).map { name ->
        ResourceMappingPatch.resourceMappings.single { it.name == name }.id
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
                                    resourceIds[0] -> { // shorts player comment
                                        val insertIndex = index - 2
                                        val invokeInstruction = instructions.elementAt(insertIndex)
                                        if (invokeInstruction.opcode != Opcode.CONST_HIGH16) return@forEachIndexed

                                        val mutableMethod = context.proxy(classDef).mutableClass.findMutableMethodOf(method)

                                        val viewRegister = (instructions.elementAt(index + 3) as OneRegisterInstruction).registerA
                                        mutableMethod.implementation!!.injectHideCall(index + 4, viewRegister, "layout/GeneralLayoutPatch", "hideShortsPlayerCommentsButton")

                                        patchSuccessArray[0] = true
                                    }

                                    resourceIds[1] -> { // shorts player remix
                                        val insertIndex = index - 2
                                        val invokeInstruction = instructions.elementAt(insertIndex)
                                        if (invokeInstruction.opcode != Opcode.CHECK_CAST) return@forEachIndexed

                                        val mutableMethod = context.proxy(classDef).mutableClass.findMutableMethodOf(method)

                                        val viewRegister = (invokeInstruction as Instruction21c).registerA
                                        mutableMethod.implementation!!.injectHideCall(index - 1, viewRegister, "layout/GeneralLayoutPatch", "hideShortsPlayerRemixButton")

                                        patchSuccessArray[1] = true
                                    }

                                    resourceIds[2] -> { // shorts player subscriptions banner
                                        val insertIndex = index + 3
                                        val invokeInstruction = instructions.elementAt(insertIndex)
                                        if (invokeInstruction.opcode != Opcode.CHECK_CAST) return@forEachIndexed

                                        val mutableMethod = context.proxy(classDef).mutableClass.findMutableMethodOf(method)

                                        val viewRegister = (invokeInstruction as Instruction21c).registerA
                                        mutableMethod.implementation!!.injectHideCall(insertIndex, viewRegister, "layout/GeneralLayoutPatch", "hideShortsPlayerSubscriptionsButton")

                                        patchSuccessArray[2] = true
                                    }
                                }
                            }
                            else -> return@forEachIndexed
                        }
                    }
                }
            }
        }

        SubscriptionsButtonTabletParentFingerprint.result?.let { parentResult ->
            with (parentResult.mutableMethod.implementation!!.instructions) {
                val targetIndex = this.indexOfFirst {
                    (it as? WideLiteralInstruction)?.wideLiteral == SharedResourceIdPatch.reelPlayerFooterLabelId
                } - 1
                if (elementAt(targetIndex).opcode.ordinal != Opcode.IPUT.ordinal) return SubscriptionsButtonTabletFingerprint.toErrorResult()
                subscriptionFieldReference = (elementAt(targetIndex) as ReferenceInstruction).reference as FieldReference
            }
            SubscriptionsButtonTabletFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.mutableMethod?.let {
                with (it.implementation!!.instructions) {
                    filter { instruction ->
                        val fieldReference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
                        instruction.opcode.ordinal == Opcode.IGET.ordinal && fieldReference == subscriptionFieldReference
                    }.forEach { instruction ->
                        val insertIndex = indexOf(instruction) + 1
                        val register = (instruction as TwoRegisterInstruction).registerA

                        it.addInstructions(
                            insertIndex,"""
                                invoke-static {v$register}, $GENERAL_LAYOUT->hideShortsPlayerSubscriptionsButton(I)I
                                move-result v$register
                                """
                        )
                    }
                }
            } ?: return SubscriptionsButtonTabletFingerprint.toErrorResult()
        } ?: return SubscriptionsButtonTabletParentFingerprint.toErrorResult()

        val errorIndex: Int = patchSuccessArray.indexOf(false)

        if (errorIndex == -1) {
            context.updatePatchStatus("ShortsComponent")

            /*
             * Add settings
             */
            SettingsPatch.addPreference(
                arrayOf(
                    "PREFERENCE: GENERAL_LAYOUT_SETTINGS",
                    "SETTINGS: SHORTS_COMPONENT.PARENT",
                    "SETTINGS: SHORTS_COMPONENT_PARENT.A",
                    "SETTINGS: SHORTS_COMPONENT_PARENT.B",
                    "SETTINGS: HIDE_SHORTS_COMPONENTS",
                    "SETTINGS: HIDE_SHORTS_SHELF"
                )
            )

            SettingsPatch.updatePatchStatus("hide-shorts-component")

            return PatchResultSuccess()
        } else
            return PatchResultError("Instruction not found: $errorIndex")
    }
    private companion object {
        private lateinit var subscriptionFieldReference: FieldReference
    }
}

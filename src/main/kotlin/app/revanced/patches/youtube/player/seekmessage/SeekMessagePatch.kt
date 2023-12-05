package app.revanced.patches.youtube.player.seekmessage

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.player.seekmessage.fingerprints.SeekEduContainerFingerprint
import app.revanced.patches.youtube.player.seekmessage.fingerprints.SeekEduUndoOverlayFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.PLAYER
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.SeekUndoEduOverlayStub
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.exception
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Patch(
    name = "Hide seek message",
    description = "Hides the 'Slide left or right to seek' or 'Release to cancel' message container.",
    dependencies = [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.25.40",
                "18.27.36",
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40",
                "18.34.38",
                "18.35.36",
                "18.36.39",
                "18.37.36",
                "18.38.44",
                "18.39.41",
                "18.40.34",
                "18.41.39",
                "18.42.41",
                "18.43.45",
                "18.44.41",
                "18.45.43",
                "18.46.43"
            ]
        )
    ]
)
@Suppress("unused")
object SeekMessagePatch : BytecodePatch(
    setOf(
        SeekEduContainerFingerprint,
        SeekEduUndoOverlayFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        SeekEduContainerFingerprint.result?.let {
            it.mutableMethod.apply {
                addInstructionsWithLabels(
                    0, """
                        invoke-static {}, $PLAYER->hideSeekMessage()Z
                        move-result v0
                        if-eqz v0, :default
                        return-void
                        """, ExternalLabel("default", getInstruction(0))
                )
            }
        } ?: throw SeekEduContainerFingerprint.exception

        /**
         * Added in YouTube v18.29.xx~
         */
        SeekEduUndoOverlayFingerprint.result?.let {
            it.mutableMethod.apply {
                val seekUndoCalls = implementation!!.instructions.withIndex()
                    .filter { instruction ->
                        (instruction.value as? WideLiteralInstruction)?.wideLiteral == SeekUndoEduOverlayStub
                    }
                val insertIndex = seekUndoCalls.elementAt(seekUndoCalls.size - 1).index
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                for (index in insertIndex until implementation!!.instructions.size) {
                    val targetInstruction = getInstruction(index)
                    if (targetInstruction.opcode != Opcode.INVOKE_VIRTUAL)
                        continue

                    if (((targetInstruction as Instruction35c).reference as MethodReference).name != "setOnClickListener")
                        continue

                    // Force close occurs only in YouTube v18.36.xx unless we add this.
                    if (SettingsPatch.is1836)
                        addComponent(insertIndex, index - 1)

                    addInstructionsWithLabels(
                        insertIndex, fixComponent + """
                            invoke-static {}, $PLAYER->hideSeekUndoMessage()Z
                            move-result v$insertRegister
                            if-nez v$insertRegister, :default
                            """, ExternalLabel("default", getInstruction(index + 1))
                    )

                    /**
                     * Add settings
                     */
                    SettingsPatch.addPreference(
                        arrayOf(
                            "PREFERENCE: PLAYER_SETTINGS",
                            "SETTINGS: HIDE_SEEK_UNDO_MESSAGE"
                        )
                    )

                    break
                }
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: HIDE_SEEK_MESSAGE"
            )
        )

        SettingsPatch.updatePatchStatus("Hide seek message")

    }

    private var fixComponent: String = ""

    private fun MutableMethod.addComponent(
        startIndex: Int,
        endIndex: Int
    ) {
        val fixRegister =
            getInstruction<FiveRegisterInstruction>(endIndex).registerE

        for (index in endIndex downTo startIndex) {
            val opcode = getInstruction(index).opcode
            if (opcode != Opcode.CONST_16)
                continue

            val register = getInstruction<OneRegisterInstruction>(index).registerA

            if (register != fixRegister)
                continue

            val fixValue = getInstruction<WideLiteralInstruction>(index).wideLiteral.toInt()

            fixComponent = "const/16 v$fixRegister, $fixValue"

            break
        }
    }
}

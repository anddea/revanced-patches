package app.revanced.patches.youtube.player.seekmessage

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.player.seekmessage.fingerprints.SeekEduContainerFingerprint
import app.revanced.patches.youtube.player.seekmessage.fingerprints.SeekEduUndoOverlayFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.SeekUndoEduOverlayStub
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.integrations.Constants.PLAYER
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
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
                "18.24.37",
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
                "18.39.41"
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
                val insertIndex = getWideLiteralIndex(SeekUndoEduOverlayStub)
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                for (index in insertIndex until implementation!!.instructions.size) {
                    val targetInstruction = getInstruction(index)
                    if (targetInstruction.opcode != Opcode.INVOKE_VIRTUAL)
                        continue

                    if (((targetInstruction as Instruction35c).reference as MethodReference).name != "setOnClickListener")
                        continue

                    addInstructionsWithLabels(
                        insertIndex, """
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
}

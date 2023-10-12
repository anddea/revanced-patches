package app.revanced.patches.music.utils.playerlayouthook

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patches.music.utils.playerlayouthook.fingerprints.NewPlayerLayoutFingerprint
import app.revanced.util.integrations.Constants.MUSIC_PLAYER
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

object PlayerLayoutHookPatch : BytecodePatch(
    setOf(NewPlayerLayoutFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        NewPlayerLayoutFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = implementation!!.instructions.size - 1
                val targetRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {}, $MUSIC_PLAYER->enableNewPlayerLayout()Z
                        move-result v$targetRegister
                        """
                )
            }
        } ?: throw NewPlayerLayoutFingerprint.exception

    }
}
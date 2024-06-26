package app.revanced.patches.music.utils.fix.header

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.layout.header.ChangeHeaderPatch
import app.revanced.patches.music.utils.fix.header.fingerprints.HeaderSwitchConfigFingerprint
import app.revanced.util.getTargetIndexOrThrow
import app.revanced.util.getWideLiteralInstructionIndex
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    description = "Fix the issues where new headers are used."
)
object RestoreOldHeaderPatch : BytecodePatch(
    setOf(HeaderSwitchConfigFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        /**
         * New Header has been added from YouTube Music v7.04.51.
         *
         * The new header's file names are  'action_bar_logo_ringo2.png' and 'ytm_logo_ringo2.png'.
         * The only difference between the existing header and the new header is the dimensions of the image.
         *
         * The affected patch is [ChangeHeaderPatch].
         *
         * TODO: Add a new header image file to [ChangeHeaderPatch] later.
         */
        HeaderSwitchConfigFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex =
                    getTargetIndexOrThrow(
                        getWideLiteralInstructionIndex(45617851),
                        Opcode.MOVE_RESULT
                    )
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "const/4 v$targetRegister, 0x0"
                )
            }
        }

    }
}

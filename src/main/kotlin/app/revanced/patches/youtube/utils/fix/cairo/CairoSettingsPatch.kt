package app.revanced.patches.youtube.utils.fix.cairo

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.misc.backgroundplayback.BackgroundPlaybackPatch
import app.revanced.patches.youtube.utils.fix.cairo.fingerprints.CarioFragmentConfigFingerprint
import app.revanced.util.getTargetIndexOrThrow
import app.revanced.util.getWideLiteralInstructionIndex
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    description = "Fixes issues where Cairo Fragment is applied."
)
object CairoSettingsPatch : BytecodePatch(
    setOf(CarioFragmentConfigFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        /**
         * Cairo Fragment was added since YouTube v19.04.38.
         * Disable this for the following reasons:
         * 1. [BackgroundPlaybackPatch] does not activate the Minimized playback setting of Cairo Fragment.
         * 2. Some patches implemented in RVX do not yet support Cairo Fragments.
         *
         * See <a href="https://github.com/inotia00/ReVanced_Extended/issues/2099">ReVanced_Extended#2099</a>
         * or <a href="https://github.com/qnblackcat/uYouPlus/issues/1468">uYouPlus#1468</a>
         * for screenshots of the Cairo Fragment.
         */
        CarioFragmentConfigFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex =
                    getTargetIndexOrThrow(
                        getWideLiteralInstructionIndex(45532100),
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

package app.revanced.patches.music.utils.fix.clientspoof.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.music.utils.fix.clientspoof.fingerprints.UserAgentHeaderBuilderFingerprint
import app.revanced.patches.music.utils.microg.shared.Constants.MUSIC_PACKAGE_NAME
import org.jf.dexlib2.iface.instruction.FiveRegisterInstruction

class ClientSpoofMusicPatch : BytecodePatch(
    listOf(UserAgentHeaderBuilderFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        UserAgentHeaderBuilderFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.endIndex - 1
                val packageNameRegister =
                    getInstruction<FiveRegisterInstruction>(insertIndex).registerD

                addInstruction(
                    insertIndex,
                    "const-string v$packageNameRegister, \"$MUSIC_PACKAGE_NAME\""
                )
            }
        } ?: return UserAgentHeaderBuilderFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}

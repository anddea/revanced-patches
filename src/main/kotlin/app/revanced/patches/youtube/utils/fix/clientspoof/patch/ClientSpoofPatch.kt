package app.revanced.patches.youtube.utils.fix.clientspoof.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.fix.clientspoof.fingerprints.UserAgentHeaderBuilderFingerprint
import app.revanced.patches.youtube.utils.microg.shared.Constants.PACKAGE_NAME
import org.jf.dexlib2.iface.instruction.FiveRegisterInstruction

@Name("client-spoof")
@Description("Spoofs a patched client to allow playback.")
@YouTubeCompatibility
@Version("0.0.1")
class ClientSpoofPatch : BytecodePatch(
    listOf(UserAgentHeaderBuilderFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        UserAgentHeaderBuilderFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.endIndex
                val packageNameRegister =
                    getInstruction<FiveRegisterInstruction>(insertIndex).registerD

                addInstruction(
                    insertIndex,
                    "const-string v$packageNameRegister, \"$PACKAGE_NAME\""
                )
            }
        } ?: return UserAgentHeaderBuilderFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}

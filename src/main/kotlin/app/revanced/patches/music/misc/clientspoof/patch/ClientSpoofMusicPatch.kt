package app.revanced.patches.music.misc.clientspoof.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.music.misc.clientspoof.fingerprints.UserAgentHeaderBuilderFingerprint
import app.revanced.patches.music.misc.microg.shared.Constants.MUSIC_PACKAGE_NAME
import app.revanced.shared.annotation.YouTubeMusicCompatibility
import app.revanced.shared.extensions.toErrorResult
import org.jf.dexlib2.iface.instruction.FiveRegisterInstruction

@Patch
@Name("client-spoof-music")
@Description("Spoofs the YouTube Music client.")
@YouTubeMusicCompatibility
@Version("0.0.1")
class ClientSpoofMusicPatch : BytecodePatch(
    listOf(UserAgentHeaderBuilderFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        UserAgentHeaderBuilderFingerprint.result?.let {
            with (it.mutableMethod) {
                val insertIndex = it.scanResult.patternScanResult!!.endIndex - 1
                val packageNameRegister = (instruction(insertIndex) as FiveRegisterInstruction).registerD
                addInstruction(insertIndex, "const-string v$packageNameRegister, \"$MUSIC_PACKAGE_NAME\"")
            }
        } ?: return UserAgentHeaderBuilderFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}

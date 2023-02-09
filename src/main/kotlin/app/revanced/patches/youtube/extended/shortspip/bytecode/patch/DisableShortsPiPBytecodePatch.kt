package app.revanced.patches.youtube.extended.shortspip.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.youtube.extended.shortspip.bytecode.fingerprints.DisableShortsPiPFingerprint
import app.revanced.patches.youtube.misc.playertype.patch.PlayerTypeHookPatch
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.extensions.toErrorResult
import app.revanced.shared.util.integrations.Constants.EXTENDED_PATH
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction

@Name("disable-shorts-pip-bytecode-patch")
@DependsOn(
    [
        PlayerTypeHookPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class DisableShortsPiPBytecodePatch : BytecodePatch(
    listOf(
        DisableShortsPiPFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        DisableShortsPiPFingerprint.result?.let {
            with (it.mutableMethod) {
                val endIndex = it.scanResult.patternScanResult!!.endIndex
                val register = (implementation!!.instructions[endIndex] as TwoRegisterInstruction).registerA
                this.addInstructions(
                    endIndex + 1, """
                        invoke-static {v$register}, $EXTENDED_PATH/DisableShortsPiPPatch;->disableShortsPlayerPiP(Z)Z
                        move-result v$register
                    """
                )
            }
        } ?: return DisableShortsPiPFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}
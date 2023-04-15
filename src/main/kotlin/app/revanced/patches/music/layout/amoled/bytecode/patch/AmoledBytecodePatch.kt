package app.revanced.patches.music.layout.amoled.bytecode.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.patches.music.layout.amoled.bytecode.fingerprints.LithoThemeFingerprint
import app.revanced.util.integrations.Constants.UTILS_PATH
import org.jf.dexlib2.iface.instruction.formats.Instruction35c

@Name("amoled-bytecode-patch")
@YouTubeMusicCompatibility
@Version("0.0.1")
class AmoledBytecodePatch : BytecodePatch(
    listOf(
        LithoThemeFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        LithoThemeFingerprint.result?.let {
            with (it.mutableMethod) {
                val insertIndex = it.scanResult.patternScanResult!!.endIndex - 1
                val register = (implementation!!.instructions[insertIndex] as Instruction35c).registerD

                addInstructions(
                    insertIndex, """
                        invoke-static { v$register }, $UTILS_PATH/LithoThemePatch;->applyLithoTheme(I)I
                        move-result v$register
                    """
                )
            }
        } ?: return LithoThemeFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}

package app.revanced.patches.youtube.misc.theme.bytecode.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.misc.theme.bytecode.fingerprints.LithoThemeFingerprint
import app.revanced.patches.youtube.misc.theme.resource.patch.GeneralThemeResourcePatch
import app.revanced.util.integrations.Constants.UTILS_PATH
import org.jf.dexlib2.iface.instruction.formats.Instruction35c

@Name("general-theme")
@DependsOn([GeneralThemeResourcePatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class GeneralThemePatch : BytecodePatch(
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

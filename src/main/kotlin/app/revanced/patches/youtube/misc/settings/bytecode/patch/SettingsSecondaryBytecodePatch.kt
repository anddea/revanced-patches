package app.revanced.patches.youtube.misc.settings.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourcdIdPatch
import app.revanced.patches.youtube.misc.settings.bytecode.fingerprints.ThemeSetterSystemFingerprint
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.util.integrations.Constants.INTEGRATIONS_PATH
import org.jf.dexlib2.iface.instruction.formats.Instruction31i
import org.jf.dexlib2.Opcode

@Name("settings-secondary-bytecode-patch")
@DependsOn([SharedResourcdIdPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class SettingsSecondaryBytecodePatch : BytecodePatch(
    listOf(ThemeSetterSystemFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {
        val ThemeHelper = "$INTEGRATIONS_PATH/utils/ThemeHelper;"

        // apply the current theme of the settings page
        with(ThemeSetterSystemFingerprint.result!!) {
            with(mutableMethod) {
                val call = "invoke-static {v0}, $ThemeHelper->setTheme(Ljava/lang/Object;)V"

                addInstruction(
                    scanResult.patternScanResult!!.startIndex,
                    call
                )

                addInstruction(
                    mutableMethod.implementation!!.instructions.size - 1,
                    call
                )
            }
        }

        return PatchResultSuccess()
    }
}
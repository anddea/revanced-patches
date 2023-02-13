package app.revanced.patches.youtube.misc.externalbrowser.bytecode.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.misc.externalbrowser.bytecode.fingerprints.*
import app.revanced.util.integrations.Constants.MISC_PATH
import org.jf.dexlib2.iface.instruction.formats.Instruction21c

@Name("enable-external-browser-bytecode-patch")
@YouTubeCompatibility
@Version("0.0.1")
class ExternalBrowserBytecodePatch : BytecodePatch(
    listOf(
        ExternalBrowserPrimaryFingerprint,
        ExternalBrowserSecondaryFingerprint,
        ExternalBrowserTertiaryFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        arrayOf(
            ExternalBrowserPrimaryFingerprint,
            ExternalBrowserSecondaryFingerprint,
            ExternalBrowserTertiaryFingerprint
        ).forEach {
            val result = it.result?: return it.toErrorResult()
            val endIndex = result.scanResult.patternScanResult!!.endIndex
            with (result.mutableMethod) {
                val register = (implementation!!.instructions[endIndex] as Instruction21c).registerA
                addInstructions(
                    endIndex + 1, """
                        invoke-static {v$register}, $MISC_PATH/ExternalBrowserPatch;->enableExternalBrowser(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$register
                    """
                )
            }
        }

        return PatchResultSuccess()
    }
}
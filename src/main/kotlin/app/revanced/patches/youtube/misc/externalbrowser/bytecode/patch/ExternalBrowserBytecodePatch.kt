package app.revanced.patches.youtube.misc.externalbrowser.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.youtube.misc.externalbrowser.bytecode.fingerprints.*
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.util.integrations.Constants.MISC_PATH
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
        ).forEach { fingerprint ->
            val result = fingerprint.result!!
            val method = result.mutableMethod
            val endIndex = result.scanResult.patternScanResult!!.endIndex
            val register = (method.implementation!!.instructions[endIndex] as Instruction21c).registerA

            method.addInstructions(
                    endIndex + 1, """
                    invoke-static {v$register}, $MISC_PATH/ExternalBrowserPatch;->enableExternalBrowser(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v$register
                """
            )
        }
        return PatchResultSuccess()
    }
}
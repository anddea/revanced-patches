package app.revanced.patches.youtube.misc.openlinksdirectly.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.youtube.misc.openlinksdirectly.bytecode.fingerprints.*
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.util.integrations.Constants.MISC_PATH
import org.jf.dexlib2.iface.instruction.Instruction
import org.jf.dexlib2.iface.instruction.formats.Instruction11x
import org.jf.dexlib2.iface.instruction.formats.Instruction35c

@Name("enable-open-links-directly-bytecode-patch")
@YouTubeCompatibility
@Version("0.0.1")
class OpenLinksDirectlyBytecodePatch : BytecodePatch(
    listOf(
        OpenLinksDirectlyFingerprintPrimary,
        OpenLinksDirectlyFingerprintSecondary
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        OpenLinksDirectlyFingerprintPrimary.hookUriParser(true)
        OpenLinksDirectlyFingerprintSecondary.hookUriParser(false)

        return PatchResultSuccess()
    }
}

fun MethodFingerprint.hookUriParser(isPrimaryFingerprint: Boolean) {
    fun getTargetRegister(instruction: Instruction): Int {
        if (isPrimaryFingerprint) return (instruction as Instruction35c).registerC
        return (instruction as Instruction11x).registerA
    }
    with(this.result!!) {
        val startIndex = scanResult.patternScanResult!!.startIndex
        val instruction = method.implementation!!.instructions.elementAt(startIndex + 1)
        val insertIndex = if (isPrimaryFingerprint) 1 else 2
        val targetRegister = getTargetRegister(instruction)

        mutableMethod.addInstructions(
            startIndex + insertIndex, """
               invoke-static {v$targetRegister}, $MISC_PATH/OpenLinksDirectlyPatch;->enableBypassRedirect(Ljava/lang/String;)Ljava/lang/String;
               move-result-object v$targetRegister
            """
        )
    }
}
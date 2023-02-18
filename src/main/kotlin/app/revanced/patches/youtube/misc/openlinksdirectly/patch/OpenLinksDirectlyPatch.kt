package app.revanced.patches.youtube.misc.openlinksdirectly.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprintResult
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.misc.openlinksdirectly.fingerprints.*
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.MISC_PATH
import org.jf.dexlib2.iface.instruction.Instruction
import org.jf.dexlib2.iface.instruction.formats.Instruction11x
import org.jf.dexlib2.iface.instruction.formats.Instruction35c

@Patch
@Name("enable-open-links-directly")
@Description("Skips over redirection URLs to external links.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class OpenLinksDirectlyPatch : BytecodePatch(
    listOf(
        OpenLinksDirectlyFingerprintPrimary,
        OpenLinksDirectlyFingerprintSecondary
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        arrayOf(
            OpenLinksDirectlyFingerprintPrimary to true,
            OpenLinksDirectlyFingerprintSecondary to false
        ).map { (fingerprint, boolean) ->
            fingerprint.result?.hookUriParser(boolean) ?: return fingerprint.toErrorResult()
        }

        /*
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: ENABLE_OPEN_LINKS_DIRECTLY"
            )
        )

        SettingsPatch.updatePatchStatus("enable-open-links-directly")

        return PatchResultSuccess()
    }
}

fun MethodFingerprintResult.hookUriParser(isPrimaryFingerprint: Boolean) {
    fun getTargetRegister(instruction: Instruction): Int {
        if (isPrimaryFingerprint) return (instruction as Instruction35c).registerC
        return (instruction as Instruction11x).registerA
    }
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
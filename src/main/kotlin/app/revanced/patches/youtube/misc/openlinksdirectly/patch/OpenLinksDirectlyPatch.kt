package app.revanced.patches.youtube.misc.openlinksdirectly.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.misc.openlinksdirectly.fingerprints.OpenLinksDirectlyFingerprintPrimary
import app.revanced.patches.youtube.misc.openlinksdirectly.fingerprints.OpenLinksDirectlyFingerprintSecondary
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.MISC_PATH
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
            OpenLinksDirectlyFingerprintPrimary,
            OpenLinksDirectlyFingerprintSecondary
        ).forEach { fingerprint ->
            fingerprint.result?.let {
                it.mutableMethod.apply {
                    val insertIndex = it.scanResult.patternScanResult!!.startIndex
                    val register = getInstruction<Instruction35c>(insertIndex).registerC

                    replaceInstruction(
                        insertIndex,
                        "invoke-static {v$register}, $MISC_PATH/OpenLinksDirectlyPatch;->enableBypassRedirect(Ljava/lang/String;)Landroid/net/Uri;"
                    )
                }
            } ?: return fingerprint.toErrorResult()
        }

        /**
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

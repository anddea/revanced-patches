package app.revanced.patches.reddit.misc.tracking.url

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.reddit.misc.tracking.url.fingerprints.ShareLinkFormatterFingerprint
import app.revanced.patches.reddit.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.reddit.utils.integrations.Constants.PATCHES_PATH
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.updateSettingsStatus
import app.revanced.patches.reddit.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow

@Suppress("unused")
object SanitizeUrlQueryPatch : BaseBytecodePatch(
    name = "Sanitize sharing links",
    description = "Adds an option to remove tracking query parameters from URLs when sharing links.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(ShareLinkFormatterFingerprint)
) {
    private const val SANITIZE_METHOD_DESCRIPTOR =
        "$PATCHES_PATH/SanitizeUrlQueryPatch;->stripQueryParameters()Z"

    override fun execute(context: BytecodeContext) {
        ShareLinkFormatterFingerprint.resultOrThrow().let { result ->
            result.mutableMethod.apply {
                addInstructionsWithLabels(
                    0,
                    """
                        invoke-static {}, $SANITIZE_METHOD_DESCRIPTOR
                        move-result v0
                        if-eqz v0, :off
                        return-object p0
                        """, ExternalLabel("off", getInstruction(0))
                )
            }
        }

        updateSettingsStatus("enableSanitizeUrlQuery")

    }
}

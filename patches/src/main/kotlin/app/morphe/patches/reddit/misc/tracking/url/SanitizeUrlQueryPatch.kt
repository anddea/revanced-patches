package app.morphe.patches.reddit.misc.tracking.url

import app.morphe.patches.reddit.utils.extension.Constants
import app.morphe.patches.reddit.utils.patch.PatchList
import app.morphe.patches.reddit.utils.settings.settingsPatch
import app.morphe.patches.reddit.utils.settings.updatePatchStatus
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.util.fingerprint.methodOrThrow

private const val SANITIZE_METHOD_DESCRIPTOR =
    "${Constants.PATCHES_PATH}/SanitizeUrlQueryPatch;->stripQueryParameters()Z"

@Suppress("unused")
val sanitizeUrlQueryPatch = bytecodePatch(
    PatchList.SANITIZE_SHARING_LINKS.title,
    PatchList.SANITIZE_SHARING_LINKS.summary,
) {
    compatibleWith(app.morphe.patches.reddit.utils.compatibility.Constants.COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {
        shareLinkFormatterFingerprint.methodOrThrow().apply {
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

        updatePatchStatus(
            "enableSanitizeUrlQuery",
            PatchList.SANITIZE_SHARING_LINKS
        )
    }
}

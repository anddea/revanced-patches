package app.revanced.patches.music.misc.tracking

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.misc.tracking.fingerprints.ShareLinkFormatterFingerprint
import app.revanced.patches.music.utils.integrations.Constants.MISC_PATH
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.shared.fingerprints.tracking.CopyTextEndpointFingerprint
import app.revanced.patches.shared.patch.tracking.AbstractSanitizeUrlQueryPatch

@Patch(
    name = "Sanitize sharing links",
    description = "Removes tracking query parameters from the URLs when sharing links.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [CompatiblePackage("com.google.android.apps.youtube.music")]
)
@Suppress("unused")
object SanitizeUrlQueryPatch : AbstractSanitizeUrlQueryPatch(
    "$MISC_PATH/SanitizeUrlQueryPatch;"
) {
    override fun execute(context: BytecodeContext) {
        super.execute(context)

        SettingsPatch.addMusicPreference(
            CategoryType.MISC,
            "revanced_sanitize_sharing_links",
            "true"
        )

    }
}

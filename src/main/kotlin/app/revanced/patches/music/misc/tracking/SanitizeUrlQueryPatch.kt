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
    description = "Adds an option to remove tracking query parameters from URLs when sharing links.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "6.21.52",
                "6.22.52",
                "6.23.56",
                "6.25.53",
                "6.26.51",
                "6.27.54",
                "6.28.53",
                "6.29.58",
                "6.31.55",
                "6.33.52"
            ]
        )
    ]
)
@Suppress("unused")
object SanitizeUrlQueryPatch : AbstractSanitizeUrlQueryPatch(
    "$MISC_PATH/SanitizeUrlQueryPatch;",
    listOf(
        CopyTextEndpointFingerprint,
        ShareLinkFormatterFingerprint
    ),
    null
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

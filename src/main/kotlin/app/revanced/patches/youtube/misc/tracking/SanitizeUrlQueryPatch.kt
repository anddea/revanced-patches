package app.revanced.patches.youtube.misc.tracking

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.shared.patch.tracking.AbstractSanitizeUrlQueryPatch
import app.revanced.patches.youtube.misc.tracking.fingerprints.ShareLinkFormatterFingerprint
import app.revanced.patches.youtube.misc.tracking.fingerprints.SystemShareLinkFormatterFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.MISC_PATH
import app.revanced.patches.youtube.utils.settings.SettingsPatch

@Patch(
    name = "Sanitize sharing links",
    description = "Removes tracking query parameters from the URLs when sharing links.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.25.40",
                "18.27.36",
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40",
                "18.34.38",
                "18.35.36",
                "18.36.39",
                "18.37.36",
                "18.38.44",
                "18.39.41",
                "18.40.34",
                "18.41.39",
                "18.42.41",
                "18.43.45",
                "18.44.41",
                "18.45.43"
            ]
        )
    ]
)
@Suppress("unused")
object SanitizeUrlQueryPatch : AbstractSanitizeUrlQueryPatch(
    "$MISC_PATH/SanitizeUrlQueryPatch;",
    setOf(
        ShareLinkFormatterFingerprint,
        SystemShareLinkFormatterFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {
        super.execute(context)

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: SANITIZE_SHARING_LINKS"
            )
        )

        SettingsPatch.updatePatchStatus("Sanitize sharing links")
    }
}

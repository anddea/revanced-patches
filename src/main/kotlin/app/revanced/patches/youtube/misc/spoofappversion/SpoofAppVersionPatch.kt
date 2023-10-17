package app.revanced.patches.youtube.misc.spoofappversion

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.shared.patch.versionspoof.AbstractVersionSpoofPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch.contexts
import app.revanced.util.integrations.Constants.MISC_PATH
import app.revanced.util.resources.ResourceUtils.copyXmlNode

@Patch(
    name = "Spoof app version",
    description = "Spoof the YouTube client version.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.24.37",
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
                "18.37.36"
            ]
        )
    ]
)
@Suppress("unused")
object SpoofAppVersionPatch : AbstractVersionSpoofPatch(
    "$MISC_PATH/SpoofAppVersionPatch;->getVersionOverride(Ljava/lang/String;)Ljava/lang/String;"
) {
    override fun execute(context: BytecodeContext) {
        super.execute(context)

        /**
         * Copy arrays
         */
        contexts.copyXmlNode("youtube/spoofappversion/host", "values/arrays.xml", "resources")

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: SPOOF_APP_VERSION"
            )
        )

        SettingsPatch.updatePatchStatus("Spoof app version")

    }
}
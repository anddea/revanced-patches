package app.revanced.patches.youtube.misc.spoofappversion.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.patch.versionspoof.AbstractVersionSpoofPatch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch.Companion.contexts
import app.revanced.util.integrations.Constants.MISC_PATH
import app.revanced.util.resources.ResourceUtils.copyXmlNode

@Patch
@Name("Spoof app version")
@Description("Tricks YouTube into thinking, you are running an older version of the app. One of the side effects also includes restoring the old UI.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
class SpoofAppVersionPatch : AbstractVersionSpoofPatch(
    "$MISC_PATH/VersionOverridePatch;->getVersionOverride(Ljava/lang/String;)Ljava/lang/String;"
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

        SettingsPatch.updatePatchStatus("spoof-app-version")

    }
}
package app.revanced.patches.youtube.misc.spoofappversion.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.patch.versionspoof.GeneralVersionSpoofPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.MISC_PATH
import app.revanced.util.resources.ResourceUtils.copyXmlNode

@Patch
@Name("spoof-app-version")
@Description("Tricks YouTube into thinking, you are running an older version of the app. One of the side effects also includes restoring the old UI.")
@DependsOn(
    [
        GeneralVersionSpoofPatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class SpoofAppVersionPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        GeneralVersionSpoofPatch.injectSpoof("$MISC_PATH/VersionOverridePatch;->getVersionOverride(Ljava/lang/String;)Ljava/lang/String;")

        /**
         * Copy arrays
         */
        context.copyXmlNode("youtube/spoofappversion/host", "values/arrays.xml", "resources")

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: SPOOF_APP_VERSION"
            )
        )

        SettingsPatch.updatePatchStatus("spoof-app-version")

        return PatchResultSuccess()
    }
}
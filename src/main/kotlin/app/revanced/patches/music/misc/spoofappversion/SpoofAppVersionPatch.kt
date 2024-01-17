package app.revanced.patches.music.misc.spoofappversion

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.general.oldstylelibraryshelf.OldStyleLibraryShelfPatch
import app.revanced.patches.music.utils.integrations.Constants.MISC_PATH
import app.revanced.patches.music.utils.intenthook.IntentHookPatch
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.music.utils.settings.SettingsPatch.contexts
import app.revanced.patches.shared.patch.versionspoof.AbstractVersionSpoofPatch
import app.revanced.util.copyXmlNode

@Patch(
    name = "Spoof app version",
    description = "Adds options to spoof the YouTube Music client version. " + 
            "This can remove the radio mode restriction in Canadian regions or disable real-time lyrics.",
    dependencies = [
        IntentHookPatch::class,
        OldStyleLibraryShelfPatch::class,
        SettingsPatch::class
    ],
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
object SpoofAppVersionPatch : AbstractVersionSpoofPatch(
    "$MISC_PATH/SpoofAppVersionPatch;->getVersionOverride(Ljava/lang/String;)Ljava/lang/String;"
) {
    override fun execute(context: BytecodeContext) {
        super.execute(context)

        /**
         * Copy arrays
         */
        contexts.copyXmlNode("music/spoofappversion/host", "values/arrays.xml", "resources")

        SettingsPatch.addMusicPreference(
            CategoryType.MISC,
            "revanced_spoof_app_version",
            "false"
        )
        SettingsPatch.addMusicPreferenceWithIntent(
            CategoryType.MISC,
            "revanced_spoof_app_version_target",
            "revanced_spoof_app_version"
        )

    }
}
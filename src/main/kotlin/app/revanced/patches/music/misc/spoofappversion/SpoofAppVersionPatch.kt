package app.revanced.patches.music.misc.spoofappversion

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.general.oldstylelibraryshelf.OldStyleLibraryShelfPatch
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.shared.patch.versionspoof.AbstractVersionSpoofPatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_MISC_PATH

@Patch(
    name = "Spoof app version",
    description = "Spoof the YouTube Music client version.",
    dependencies = [
        OldStyleLibraryShelfPatch::class,
        SettingsPatch::class
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "6.15.52",
                "6.20.51",
                "6.21.51",
                "6.22.51"
            ]
        )
    ]
)
@Suppress("unused")
object SpoofAppVersionPatch : AbstractVersionSpoofPatch(
    "$MUSIC_MISC_PATH/SpoofAppVersionPatch;->getVersionOverride(Ljava/lang/String;)Ljava/lang/String;"
) {
    override fun execute(context: BytecodeContext) {
        super.execute(context)

        SettingsPatch.addMusicPreference(
            CategoryType.MISC,
            "revanced_spoof_app_version",
            "false"
        )

    }
}
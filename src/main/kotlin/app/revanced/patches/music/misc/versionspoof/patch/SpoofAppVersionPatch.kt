package app.revanced.patches.music.misc.versionspoof.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.general.oldstylelibraryshelf.patch.OldStyleLibraryShelfPatch
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.patches.shared.patch.versionspoof.AbstractVersionSpoofPatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_MISC_PATH

@Patch
@Name("Spoof app version")
@Description("Spoof the YouTube Music client version.")
@DependsOn(
    [
        OldStyleLibraryShelfPatch::class,
        SettingsPatch::class
    ]
)
@MusicCompatibility
class SpoofAppVersionPatch : AbstractVersionSpoofPatch(
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
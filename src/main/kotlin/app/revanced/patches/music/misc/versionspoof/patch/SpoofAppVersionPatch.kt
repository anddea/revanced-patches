package app.revanced.patches.music.misc.versionspoof.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.utils.settings.resource.patch.MusicSettingsPatch
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.patches.shared.patch.versionspoof.AbstractVersionSpoofPatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_MISC_PATH

@Patch
@Name("spoof-app-version")
@Description("Spoof the YouTube Music client version.")
@DependsOn([MusicSettingsPatch::class])
@YouTubeMusicCompatibility
@Version("0.0.1")
class SpoofAppVersionPatch : AbstractVersionSpoofPatch(
    "$MUSIC_MISC_PATH/SpoofAppVersionPatch;->getVersionOverride(Ljava/lang/String;)Ljava/lang/String;"
) {
    override fun execute(context: BytecodeContext): PatchResult {

        MusicSettingsPatch.addMusicPreference(CategoryType.MISC, "revanced_spoof_app_version", "false")

        return PatchResultSuccess()
    }
}
package app.revanced.patches.music.layout.landscapemode.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.layout.landscapemode.fingerprints.TabletIdentifierFingerprint
import app.revanced.patches.music.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.music.misc.settings.resource.patch.MusicSettingsPatch
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_LAYOUT

@Patch
@Name("enable-landscape-mode")
@Description("Enables entry into landscape mode by screen rotation on the phone.")
@DependsOn(
    [
        MusicSettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeMusicCompatibility
@Version("0.0.1")
class LandScapeModePatch : BytecodePatch(
    listOf(TabletIdentifierFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {
        TabletIdentifierFingerprint.result?.let {
            it.mutableMethod.addInstructions(
                it.scanResult.patternScanResult!!.endIndex + 1, """
                    invoke-static {p0}, $MUSIC_LAYOUT->enableLandScapeMode(Z)Z
                    move-result p0
                    """
            )
        } ?: return TabletIdentifierFingerprint.toErrorResult()

        MusicSettingsPatch.addMusicPreference(CategoryType.LAYOUT, "revanced_enable_landscape_mode", "true")

        return PatchResultSuccess()
    }
}

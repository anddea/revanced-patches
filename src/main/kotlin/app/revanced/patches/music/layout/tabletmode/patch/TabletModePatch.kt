package app.revanced.patches.music.layout.tabletmode.patch

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
import app.revanced.patches.music.layout.tabletmode.fingerprints.TabletLayoutFingerprint
import app.revanced.patches.music.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.music.misc.settings.patch.MusicSettingsPatch
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.util.integrations.Constants.MUSIC_SETTINGS_PATH

@Patch
@Name("enable-tablet-mode")
@Description("Enable landscape mode on phone.")
@DependsOn(
    [
        MusicSettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeMusicCompatibility
@Version("0.0.1")
class TabletModePatch : BytecodePatch(
    listOf(
        TabletLayoutFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        TabletLayoutFingerprint.result?.let {
            it.mutableMethod.addInstructions(
                it.scanResult.patternScanResult!!.endIndex + 1, """
                invoke-static {p0}, $MUSIC_SETTINGS_PATH->enableTabletMode(Z)Z
                move-result p0
            """
                )
        } ?: return TabletLayoutFingerprint.toErrorResult()

        MusicSettingsPatch.addMusicPreference("navigation", "revanced_enable_tablet_mode", "true")

        return PatchResultSuccess()
    }
}

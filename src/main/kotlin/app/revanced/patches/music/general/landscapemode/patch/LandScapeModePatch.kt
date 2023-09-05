package app.revanced.patches.music.general.landscapemode.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.general.landscapemode.fingerprints.TabletIdentifierFingerprint
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_GENERAL

@Patch
@Name("Enable landscape mode")
@Description("Enables entry into landscape mode by screen rotation on the phone.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@MusicCompatibility
class LandScapeModePatch : BytecodePatch(
    listOf(TabletIdentifierFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        TabletIdentifierFingerprint.result?.let {
            it.mutableMethod.addInstructions(
                it.scanResult.patternScanResult!!.endIndex + 1, """
                    invoke-static {p0}, $MUSIC_GENERAL->enableLandScapeMode(Z)Z
                    move-result p0
                    """
            )
        } ?: throw TabletIdentifierFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.GENERAL,
            "revanced_enable_landscape_mode",
            "true"
        )

    }
}

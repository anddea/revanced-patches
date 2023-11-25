package app.revanced.patches.music.general.landscapemode

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.general.landscapemode.fingerprints.TabletIdentifierFingerprint
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_GENERAL

@Patch(
    name = "Enable landscape mode",
    description = "Enables entry into landscape mode by screen rotation on the phone.",
    dependencies = [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "6.21.52",
                "6.27.54",
                "6.28.52"
            ]
        )
    ]
)
@Suppress("unused")
object LandScapeModePatch : BytecodePatch(
    setOf(TabletIdentifierFingerprint)
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

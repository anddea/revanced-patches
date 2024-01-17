package app.revanced.patches.music.misc.codecs

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.utils.integrations.Constants.MISC_PATH
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.shared.patch.opus.AbstractOpusCodecsPatch

@Patch(
    name = "Enable opus codec",
    description = "Adds an option use the opus audio codec instead of the mp4a audio codec.",
    dependencies = [SettingsPatch::class],
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
object CodecsUnlockPatch : AbstractOpusCodecsPatch(
    "$MISC_PATH/OpusCodecPatch;->enableOpusCodec()Z"
) {
    override fun execute(context: BytecodeContext) {
        super.execute(context)

        SettingsPatch.addMusicPreference(
            CategoryType.MISC,
            "revanced_enable_opus_codec",
            "true"
        )

    }
}

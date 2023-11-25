package app.revanced.patches.music.misc.codecs

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.shared.patch.opus.AbstractOpusCodecsPatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_MISC_PATH

@Patch(
    name = "Enable opus codec",
    description = "Enable opus codec when playing audio.",
    dependencies = [SettingsPatch::class],
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
object CodecsUnlockPatch : AbstractOpusCodecsPatch(
    "$MUSIC_MISC_PATH/OpusCodecPatch;->enableOpusCodec()Z"
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

package app.revanced.patches.youtube.misc.codec.audio

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.shared.patch.opus.AbstractOpusCodecsPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.integrations.Constants.MISC_PATH

@Patch(
    name = "Force OPUS codec",
    description = "Forces the OPUS codec for audios.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.25.40",
                "18.27.36",
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40",
                "18.34.38",
                "18.35.36",
                "18.36.39",
                "18.37.36",
                "18.38.44",
                "18.39.41",
                "18.40.34",
                "18.41.39",
                "18.42.41",
                "18.43.45",
                "18.44.41",
                "18.45.43"
            ]
        )
    ]
)
@Suppress("unused")
object ForceOpusCodecPatch : AbstractOpusCodecsPatch(
    "$MISC_PATH/CodecOverridePatch;->shouldForceOpus()Z"
) {
    override fun execute(context: BytecodeContext) {
        super.execute(context)

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: EXPERIMENTAL_FLAGS",
                "SETTINGS: ENABLE_OPUS_CODEC"
            )
        )

        SettingsPatch.updatePatchStatus("Force OPUS codec")

    }
}

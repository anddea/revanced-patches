package app.revanced.patches.youtube.misc.forceopus.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.patch.opus.AbstractOpusCodecsPatch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.MISC_PATH

@Patch
@Name("Force OPUS codec")
@Description("Forces the OPUS codec for audios.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
class ForceOpusCodecPatch : AbstractOpusCodecsPatch(
    "$MISC_PATH/CodecOverridePatch;->shouldForceOpus()Z"
) {
    override fun execute(context: BytecodeContext) {
        super.execute(context)

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: ENABLE_OPUS_CODEC"
            )
        )

        SettingsPatch.updatePatchStatus("force-opus-codec")

    }
}

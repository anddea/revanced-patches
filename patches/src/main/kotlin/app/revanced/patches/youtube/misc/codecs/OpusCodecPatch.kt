package app.revanced.patches.youtube.misc.codecs

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.opus.baseOpusCodecsPatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.MISC_PATH
import app.revanced.patches.youtube.utils.patch.PatchList.ENABLE_OPUS_CODEC
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch

@Suppress("unused")
val opusCodecPatch = bytecodePatch(
    ENABLE_OPUS_CODEC.title,
    ENABLE_OPUS_CODEC.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        baseOpusCodecsPatch(
            "$MISC_PATH/OpusCodecPatch;->enableOpusCodec()Z"
        ),
        settingsPatch,
    )

    execute {

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_CATEGORY: MISC_EXPERIMENTAL_FLAGS",
                "SETTINGS: ENABLE_OPUS_CODEC"
            ),
            ENABLE_OPUS_CODEC
        )

        // endregion

    }
}

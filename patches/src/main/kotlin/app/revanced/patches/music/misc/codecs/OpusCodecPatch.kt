package app.revanced.patches.music.misc.codecs

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.extension.Constants.MISC_PATH
import app.revanced.patches.music.utils.patch.PatchList.ENABLE_OPUS_CODEC
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.revanced.patches.music.utils.settings.addSwitchPreference
import app.revanced.patches.music.utils.settings.settingsPatch
import app.revanced.patches.shared.opus.baseOpusCodecsPatch

@Suppress("unused")
val opusCodecPatch = resourcePatch(
    ENABLE_OPUS_CODEC.title,
    ENABLE_OPUS_CODEC.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        baseOpusCodecsPatch(
            "$MISC_PATH/OpusCodecPatch;->enableOpusCodec()Z"
        ),
        settingsPatch
    )

    execute {
        addSwitchPreference(
            CategoryType.MISC,
            "revanced_enable_opus_codec",
            "false"
        )

        updatePatchStatus(ENABLE_OPUS_CODEC)

    }
}

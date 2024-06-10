package app.revanced.patches.youtube.misc.codecs

import app.revanced.patches.youtube.utils.integrations.Constants.MISC_PATH
import app.revanced.patches.shared.opus.BaseOpusCodecsPatch

object OpusCodecBytecodePatch : BaseOpusCodecsPatch(
    "$MISC_PATH/OpusCodecPatch;->enableOpusCodec()Z"
)

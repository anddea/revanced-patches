package app.revanced.patches.music.misc.codecs

import app.revanced.patches.music.utils.integrations.Constants.MISC_PATH
import app.revanced.patches.shared.opus.BaseOpusCodecsPatch

object ForceOpusCodecBytecodePatch : BaseOpusCodecsPatch(
    "$MISC_PATH/OpusCodecPatch;->enableOpusCodec()Z"
)

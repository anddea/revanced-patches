package app.revanced.patches.youtube.video.playback

import app.revanced.patches.shared.customspeed.BaseCustomPlaybackSpeedPatch
import app.revanced.patches.youtube.utils.integrations.Constants.VIDEO_PATH

object CustomPlaybackSpeedPatch : BaseCustomPlaybackSpeedPatch(
    "$VIDEO_PATH/CustomPlaybackSpeedPatch;",
    8.0f
)

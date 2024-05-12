package app.revanced.patches.music.video.playback

import app.revanced.patches.music.utils.integrations.Constants.VIDEO_PATH
import app.revanced.patches.shared.customspeed.BaseCustomPlaybackSpeedPatch

object CustomPlaybackSpeedPatch : BaseCustomPlaybackSpeedPatch(
    "$VIDEO_PATH/CustomPlaybackSpeedPatch;",
    5.0f
)

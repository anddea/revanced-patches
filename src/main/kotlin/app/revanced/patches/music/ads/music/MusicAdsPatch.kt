package app.revanced.patches.music.ads.music

import app.revanced.patches.shared.patch.ads.AbstractAdsPatch
import app.revanced.util.integrations.Constants.MUSIC_ADS_PATH

object MusicAdsPatch : AbstractAdsPatch(
    "$MUSIC_ADS_PATH/MusicAdsPatch;->hideMusicAds()Z"
)

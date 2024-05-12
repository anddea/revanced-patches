package app.revanced.patches.youtube.ads.general

import app.revanced.patches.shared.ads.BaseAdsPatch
import app.revanced.patches.youtube.utils.integrations.Constants.ADS_CLASS_DESCRIPTOR

object VideoAdsPatch : BaseAdsPatch(
    ADS_CLASS_DESCRIPTOR,
    "hideVideoAds"
)

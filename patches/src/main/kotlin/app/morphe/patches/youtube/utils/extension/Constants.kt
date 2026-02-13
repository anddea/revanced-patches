package app.morphe.patches.youtube.utils.extension

@Suppress("MemberVisibilityCanBePrivate")
internal object Constants {
    const val EXTENSION_PATH = "Lapp/morphe/extension/youtube"
    const val SHARED_PATH = "$EXTENSION_PATH/shared"
    const val PATCHES_PATH = "$EXTENSION_PATH/patches"

    const val ADS_PATH = "$PATCHES_PATH/ads"
    const val ALTERNATIVE_THUMBNAILS_PATH = "$PATCHES_PATH/alternativethumbnails"
    const val COMPONENTS_PATH = "$PATCHES_PATH/components"
    const val FEED_PATH = "$PATCHES_PATH/feed"
    const val GENERAL_PATH = "$PATCHES_PATH/general"
    const val MISC_PATH = "$PATCHES_PATH/misc"
    const val OVERLAY_BUTTONS_PATH = "$PATCHES_PATH/overlaybutton"
    const val PLAYER_PATH = "$PATCHES_PATH/player"
    const val SHORTS_PATH = "$PATCHES_PATH/shorts"
    const val SPANS_PATH = "$PATCHES_PATH/spans"
    const val SPOOF_PATH = "$PATCHES_PATH/spoof"
    const val SWIPE_PATH = "$PATCHES_PATH/swipe"
    const val UTILS_PATH = "$PATCHES_PATH/utils"
    const val VIDEO_PATH = "$PATCHES_PATH/video"

    const val ADS_CLASS_DESCRIPTOR = "$ADS_PATH/AdsPatch;"
    const val ALTERNATIVE_THUMBNAILS_CLASS_DESCRIPTOR =
        "$ALTERNATIVE_THUMBNAILS_PATH/AlternativeThumbnailsPatch;"
    const val FEED_CLASS_DESCRIPTOR = "$FEED_PATH/FeedPatch;"
    const val GENERAL_CLASS_DESCRIPTOR = "$GENERAL_PATH/GeneralPatch;"
    const val PLAYER_CLASS_DESCRIPTOR = "$PLAYER_PATH/PlayerPatch;"
    const val SHORTS_CLASS_DESCRIPTOR = "$SHORTS_PATH/ShortsPatch;"

    const val PATCH_STATUS_CLASS_DESCRIPTOR = "$UTILS_PATH/PatchStatus;"
}
package app.revanced.patches.music.layout.doubletapbackground

import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.shared.patch.doubletapbackground.AbstractDoubleTapOverlayBackgroundPatch

@Patch(
    name = "Hide double tap overlay filter",
    description = "Hides the double tap dark filter layer.",
    compatiblePackages = [CompatiblePackage("com.google.android.apps.youtube.music")],
    use = false
)
@Suppress("unused")
object DoubleTapOverlayBackgroundPatch : AbstractDoubleTapOverlayBackgroundPatch(
    arrayOf("quick_seek_overlay.xml", "music_controls_overlay.xml"),
    arrayOf("tap_bloom_view", "dark_background", "player_control_screen")
)
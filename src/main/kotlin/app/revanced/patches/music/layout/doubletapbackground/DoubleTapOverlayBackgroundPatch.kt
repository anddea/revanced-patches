package app.revanced.patches.music.layout.doubletapbackground

import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.shared.patch.doubletapbackground.AbstractDoubleTapOverlayBackgroundPatch

@Patch(
    name = "Hide double tap overlay filter",
    description = "Removes the dark overlay when double-tapping to seek.",
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "6.21.52",
                "6.22.52",
                "6.23.56",
                "6.25.53",
                "6.26.51",
                "6.27.54",
                "6.28.53",
                "6.29.58",
                "6.31.55",
                "6.33.52"
            ]
        )
    ],
    use = false
)
@Suppress("unused")
object DoubleTapOverlayBackgroundPatch : AbstractDoubleTapOverlayBackgroundPatch(
    arrayOf("quick_seek_overlay.xml", "music_controls_overlay.xml"),
    arrayOf("tap_bloom_view", "dark_background", "player_control_screen")
)
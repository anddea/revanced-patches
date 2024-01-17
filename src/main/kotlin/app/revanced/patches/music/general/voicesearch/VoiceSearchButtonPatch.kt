package app.revanced.patches.music.general.voicesearch

import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.shared.patch.voicesearch.AbstractVoiceSearchButtonPatch

@Patch(
    name = "Hide voice search button",
    description = "Hides the voice search button in the search bar.",
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
object VoiceSearchButtonPatch : AbstractVoiceSearchButtonPatch(
    arrayOf("search_toolbar_view.xml"),
    arrayOf("height", "width")
)

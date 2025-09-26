package app.revanced.patches.youtube.player.overlaybuttons

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources

val geminiButton = resourcePatch {
    compatibleWith(COMPATIBLE_PACKAGE)

    execute {
        arrayOf(
            "xxxhdpi",
            "xxhdpi",
            "xhdpi",
            "hdpi",
            "mdpi"
        ).forEach { dpi ->
            copyResources(
                "youtube/overlaybuttons/rounded",
                ResourceGroup(
                    "drawable-$dpi",
                    "revanced_gemini_button.png"
                )
            )
        }
    }
}

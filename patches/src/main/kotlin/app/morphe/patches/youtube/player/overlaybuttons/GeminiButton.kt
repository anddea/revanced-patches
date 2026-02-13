package app.morphe.patches.youtube.player.overlaybuttons

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.util.ResourceGroup
import app.morphe.util.copyResources

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

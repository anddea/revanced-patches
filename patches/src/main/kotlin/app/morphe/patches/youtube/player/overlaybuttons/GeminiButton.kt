/*
 * Copyright (C) 2025 anddea
 *
 * This file is part of https://github.com/anddea/revanced-patches/.
 *
 * The original author: https://github.com/anddea.
 *
 * IMPORTANT: This file is the proprietary work of https://github.com/anddea.
 * Any modifications, derivatives, or substantial rewrites of this file
 * must retain this copyright notice and the original author attribution
 * in the source code and version control history.
 */

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

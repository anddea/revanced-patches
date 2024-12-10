package app.revanced.patches.shared.returnyoutubeusername

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.extension.Constants.PATCHES_PATH
import app.revanced.patches.shared.textcomponent.hookSpannableString
import app.revanced.patches.shared.textcomponent.hookTextComponent
import app.revanced.patches.shared.textcomponent.textComponentPatch

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$PATCHES_PATH/ReturnYouTubeUsernamePatch;"

val baseReturnYouTubeUsernamePatch = bytecodePatch(
    description = "baseReturnYouTubeUsernamePatch"
) {
    dependsOn(textComponentPatch)

    execute {
        hookSpannableString(EXTENSION_CLASS_DESCRIPTOR, "preFetchLithoText")
        hookTextComponent(EXTENSION_CLASS_DESCRIPTOR)
    }
}


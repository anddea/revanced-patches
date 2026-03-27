package app.morphe.patches.shared.returnyoutubeusername

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.extension.Constants.PATCHES_PATH
import app.morphe.patches.shared.textcomponent.hookSpannableString
import app.morphe.patches.shared.textcomponent.hookTextComponent
import app.morphe.patches.shared.textcomponent.textComponentPatch

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


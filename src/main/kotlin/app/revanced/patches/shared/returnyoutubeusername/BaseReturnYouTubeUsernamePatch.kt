package app.revanced.patches.shared.returnyoutubeusername

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.shared.integrations.Constants.PATCHES_PATH
import app.revanced.patches.shared.textcomponent.TextComponentPatch

@Patch(dependencies = [TextComponentPatch::class])
object BaseReturnYouTubeUsernamePatch : BytecodePatch(emptySet()) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$PATCHES_PATH/ReturnYouTubeUsernamePatch;"

    override fun execute(context: BytecodeContext) {
        TextComponentPatch.hookSpannableString(INTEGRATIONS_CLASS_DESCRIPTOR, "preFetchLithoText")
        TextComponentPatch.hookTextComponent(INTEGRATIONS_CLASS_DESCRIPTOR)
    }
}


package app.revanced.patches.youtube.alternative.thumbnails

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.imageurlhook.CronetImageUrlHookPatch
import app.revanced.patches.youtube.utils.integrations.Constants.ALTERNATIVE_THUMBNAILS_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.navigation.NavigationBarHookPatch
import app.revanced.patches.youtube.utils.playertype.PlayerTypeHookPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseBytecodePatch

@Suppress("unused")
object AlternativeThumbnailsPatch : BaseBytecodePatch(
    name = "Alternative thumbnails",
    description = "Adds options to replace video thumbnails using the DeArrow API or image captures from the video.",
    dependencies = setOf(
        CronetImageUrlHookPatch::class,
        NavigationBarHookPatch::class,
        PlayerTypeHookPatch::class,
        SettingsPatch::class,
    ),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    override fun execute(context: BytecodeContext) {
        CronetImageUrlHookPatch.addImageUrlHook(ALTERNATIVE_THUMBNAILS_CLASS_DESCRIPTOR)
        CronetImageUrlHookPatch.addImageUrlSuccessCallbackHook(
            ALTERNATIVE_THUMBNAILS_CLASS_DESCRIPTOR
        )
        CronetImageUrlHookPatch.addImageUrlErrorCallbackHook(ALTERNATIVE_THUMBNAILS_CLASS_DESCRIPTOR)

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: ALTERNATIVE_THUMBNAILS",
                "SETTINGS: ALTERNATIVE_THUMBNAILS"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }
}

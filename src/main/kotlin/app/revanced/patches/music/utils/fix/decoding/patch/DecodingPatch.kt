package app.revanced.patches.music.utils.fix.decoding.patch

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch

class DecodingPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        /**
         * For some reason, Androlib is incorrectly decoding some resources of YT Music
         */
        arrayOf(
            "res/layout/fullscreen_loading_spinner.xml",
            "res/layout/notification_media_cancel_action.xml"
        ).forEach { xmlPath ->
            context[xmlPath].apply {
                writeText(
                    readText()
                        .replace(
                            "@android:drawable/emulator_circular_window_overlay",
                            "@android:drawable/screen_background_dark_transparent"
                        ).replace(
                            "@android:drawable/ab_share_pack_material",
                            "@android:drawable/ic_menu_close_clear_cancel"
                        )
                )
            }
        }

        return PatchResultSuccess()
    }
}
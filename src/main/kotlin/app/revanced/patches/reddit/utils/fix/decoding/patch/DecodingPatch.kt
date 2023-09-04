package app.revanced.patches.reddit.utils.fix.decoding.patch

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch

class DecodingPatch : ResourcePatch {
    override fun execute(context: ResourceContext) {

        arrayOf(
            "res/layout/notification_media_cancel_action.xml",
            "res/values/styles.xml"
        ).forEach { xmlPath ->
            context[xmlPath].apply {
                writeText(
                    readText()
                        .replace(
                            "@android:drawable/cling_button",
                            "@android:drawable/sym_def_app_icon"
                        ).replace(
                            "@android:drawable/ab_share_pack_material",
                            "@android:drawable/ic_menu_close_clear_cancel"
                        )
                )
            }
        }

    }
}
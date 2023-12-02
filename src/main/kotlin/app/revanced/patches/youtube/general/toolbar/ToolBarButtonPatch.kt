package app.revanced.patches.youtube.general.toolbar

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.patches.youtube.utils.toolbar.ToolBarHookPatch
import app.revanced.util.integrations.Constants.GENERAL

@Patch(
    name = "Hide toolbar button",
    description = "Hide the button in the toolbar.",
    dependencies = [
        SettingsPatch::class,
        ToolBarHookPatch::class
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.25.40",
                "18.27.36",
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40",
                "18.34.38",
                "18.35.36",
                "18.36.39",
                "18.37.36",
                "18.38.44",
                "18.39.41",
                "18.40.34",
                "18.41.39",
                "18.42.41",
                "18.43.45",
                "18.44.41",
                "18.45.43",
                "18.46.43"
            ]
        )
    ]
)
@Suppress("unused")
object ToolBarButtonPatch : BytecodePatch() {
    override fun execute(context: BytecodeContext) {

        ToolBarHookPatch.injectCall("$GENERAL->hideToolBarButton")

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: HIDE_TOOLBAR_BUTTON"
            )
        )

        SettingsPatch.updatePatchStatus("Hide toolbar button")

    }
}

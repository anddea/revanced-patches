package app.revanced.patches.youtube.general.toolbar

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.utils.fingerprints.ToolBarPatchFingerprint
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
                "18.24.37",
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
                "18.37.36"
            ]
        )
    ]
)
@Suppress("unused")
object ToolBarButtonPatch : BytecodePatch(
    setOf(ToolBarPatchFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        ToolBarPatchFingerprint.result?.let {
            it.mutableMethod.apply {
                addInstruction(
                    0,
                    "invoke-static {p0, p1}, $GENERAL->hideToolBarButton(Ljava/lang/String;Landroid/view/View;)V"
                )
            }
        } ?: throw ToolBarPatchFingerprint.exception

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

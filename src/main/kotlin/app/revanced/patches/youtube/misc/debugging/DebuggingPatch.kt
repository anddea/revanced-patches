package app.revanced.patches.youtube.misc.debugging

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.utils.settings.SettingsPatch

@Patch(
    name = "Enable debug logging",
    description = "Adds debugging options.",
    dependencies = [SettingsPatch::class],
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
    ],
    use = false
)
@Suppress("unused")
object DebuggingPatch : ResourcePatch() {
    override fun execute(context: ResourceContext) {

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: ENABLE_DEBUG_LOGGING"
            )
        )

        SettingsPatch.updatePatchStatus("enable-debug-logging")


    }
}

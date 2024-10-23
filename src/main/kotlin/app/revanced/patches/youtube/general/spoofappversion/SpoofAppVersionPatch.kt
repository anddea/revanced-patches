package app.revanced.patches.youtube.general.spoofappversion

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.settings.SettingsBytecodePatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.appendAppVersion
import app.revanced.util.findMethodOrThrow
import app.revanced.util.patch.BaseResourcePatch

@Suppress("unused")
object SpoofAppVersionPatch : BaseResourcePatch(
    name = "Spoof app version",
    description = "Adds options to spoof the YouTube client version. " +
            "This can be used to restore old UI elements and features.",
    dependencies = setOf(
        SettingsPatch::class,
        SettingsBytecodePatch::class,
        SpoofAppVersionBytecodePatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    override fun execute(context: ResourceContext) {

        if (SettingsPatch.upward1834) {
            context.appendAppVersion("18.33.40")
            if (SettingsPatch.upward1839) {
                context.appendAppVersion("18.38.45")
                if (SettingsPatch.upward1849) {
                    context.appendAppVersion("18.48.39")
                    if (SettingsPatch.upward1915) {
                        context.appendAppVersion("19.13.37")

                        SettingsBytecodePatch.contexts.findMethodOrThrow(
                            PATCH_STATUS_CLASS_DESCRIPTOR
                        ) {
                            name == "SpoofAppVersionDefaultString"
                        }.replaceInstruction(
                            0,
                            "const-string v0, \"19.13.37\""
                        )
                    }
                }
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "PREFERENCE_CATEGORY: GENERAL_EXPERIMENTAL_FLAGS",
                "SETTINGS: SPOOF_APP_VERSION"
            )
        )
        SettingsPatch.updatePatchStatus(this)
    }
}
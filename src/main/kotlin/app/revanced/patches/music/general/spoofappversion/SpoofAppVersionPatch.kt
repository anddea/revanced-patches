package app.revanced.patches.music.general.spoofappversion

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patches.music.general.oldstylelibraryshelf.OldStyleLibraryShelfPatch
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsBytecodePatch
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.appendAppVersion
import app.revanced.util.findMethodOrThrow
import app.revanced.util.patch.BaseResourcePatch

@Suppress("unused")
object SpoofAppVersionPatch : BaseResourcePatch(
    name = "Spoof app version",
    description = "Adds options to spoof the YouTube Music client version. " +
            "This can remove the radio mode restriction in Canadian regions or disable real-time lyrics.",
    dependencies = setOf(
        OldStyleLibraryShelfPatch::class,
        SettingsPatch::class,
        SpoofAppVersionBytecodePatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    override fun execute(context: ResourceContext) {

        if (SettingsPatch.upward0718) {
            context.appendAppVersion("7.16.53")

            SettingsBytecodePatch.contexts.findMethodOrThrow(GENERAL_CLASS_DESCRIPTOR) {
                name == "getSpoofAppVersionDefault"
            }.replaceInstruction(
                0,
                "const-string v0, \"7.16.53\""
            )
        }

        SettingsPatch.addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_spoof_app_version",
            "false"
        )
        SettingsPatch.addPreferenceWithIntent(
            CategoryType.GENERAL,
            "revanced_spoof_app_version_target",
            "revanced_spoof_app_version"
        )

    }
}
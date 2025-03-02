package app.revanced.patches.shared.settingmenu

import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.extension.Constants.PATCHES_PATH
import app.revanced.util.findMethodOrThrow
import app.revanced.util.fingerprint.methodCall

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$PATCHES_PATH/BaseSettingsMenuPatch;"

val settingsMenuPatch = bytecodePatch(
    description = "settingsMenuPatch",
) {
    execute {
        val findPreferenceMethodCall = findPreferenceFingerprint.methodCall()
        val removePreferenceMethodCall = removePreferenceFingerprint.methodCall()

        findMethodOrThrow(EXTENSION_CLASS_DESCRIPTOR) {
            name == "removePreference"
        }.addInstructionsWithLabels(
            0, """
                invoke-virtual {p0, p1}, $findPreferenceMethodCall
                move-result-object v0
                if-eqz v0, :ignore
                invoke-virtual {p0, v0}, $removePreferenceMethodCall
                :ignore
                return-void
                """
        )
    }
}


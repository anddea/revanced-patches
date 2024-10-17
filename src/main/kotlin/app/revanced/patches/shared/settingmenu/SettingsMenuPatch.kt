package app.revanced.patches.shared.settingmenu

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.shared.integrations.Constants.PATCHES_PATH
import app.revanced.patches.shared.settingmenu.fingerprints.FindPreferenceFingerprint
import app.revanced.patches.shared.settingmenu.fingerprints.RemovePreferenceFingerprint
import app.revanced.util.findMethodOrThrow
import app.revanced.util.getMethodCall

@Patch(
    description = "Hide the settings menu for YouTube or YouTube Music.",
)
object SettingsMenuPatch : BytecodePatch(
    setOf(
        FindPreferenceFingerprint,
        RemovePreferenceFingerprint
    )
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$PATCHES_PATH/BaseSettingsMenuPatch;"

    override fun execute(context: BytecodeContext) {

        val findPreferenceMethodCall = FindPreferenceFingerprint.getMethodCall()
        val removePreferenceMethodCall = RemovePreferenceFingerprint.getMethodCall()

        context.findMethodOrThrow(INTEGRATIONS_CLASS_DESCRIPTOR) {
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
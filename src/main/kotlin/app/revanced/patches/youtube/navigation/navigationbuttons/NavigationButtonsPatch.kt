package app.revanced.patches.youtube.navigation.navigationbuttons

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.navigation.navigationbuttons.fingerprints.ANDROID_AUTOMOTIVE_STRING
import app.revanced.patches.youtube.navigation.navigationbuttons.fingerprints.AddCreateButtonViewFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.NAVIGATION_PATH
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.exception
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    name = "Hide navigation buttons",
    description = "Adds options to hide and change navigation buttons (such as the Shorts button).",
    dependencies = [
        NavigationBarHookPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class
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
                "18.46.45",
                "18.48.39",
                "18.49.37",
                "19.01.34",
                "19.02.39",
                "19.03.36",
                "19.04.38",
                "19.05.36",
                "19.06.39",
                "19.07.40",
                "19.08.36",
                "19.09.38",
                "19.10.39",
                "19.11.43",
                "19.12.41",
                "19.13.37",
                "19.14.43"
            ]
        )
    ]
)
@Suppress("unused")
object NavigationButtonsPatch : BytecodePatch(
    setOf(
        AddCreateButtonViewFingerprint
    )
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$NAVIGATION_PATH/NavigationPatch;"

    override fun execute(context: BytecodeContext) {
        /**
         * Switch create button with notifications button
         */
        AddCreateButtonViewFingerprint.result?.let {
            it.mutableMethod.apply {
                val stringIndex = it.scanResult.stringsScanResult!!.matches.find { match ->
                    match.string == ANDROID_AUTOMOTIVE_STRING
                }!!.index

                val conditionalCheckIndex = stringIndex - 1
                val conditionRegister =
                    getInstruction<OneRegisterInstruction>(conditionalCheckIndex).registerA

                addInstructions(
                    conditionalCheckIndex,
                    """
                        invoke-static { }, $INTEGRATIONS_CLASS_DESCRIPTOR->switchCreateWithNotificationButton()Z
                        move-result v$conditionRegister
                    """,
                )
            }
        } ?: throw AddCreateButtonViewFingerprint.exception

        // Hook navigation button created, in order to hide them.
        NavigationBarHookPatch.hookNavigationButtonCreated(INTEGRATIONS_CLASS_DESCRIPTOR)

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: NAVIGATION_SETTINGS",
                "SETTINGS: HIDE_NAVIGATION_BUTTONS"
            )
        )

        SettingsPatch.updatePatchStatus("Hide navigation buttons")
    }
}

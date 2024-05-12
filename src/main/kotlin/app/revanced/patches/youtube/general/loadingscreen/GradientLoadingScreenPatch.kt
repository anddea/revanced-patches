package app.revanced.patches.youtube.general.loadingscreen

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patches.youtube.general.loadingscreen.fingerprints.GradientLoadingScreenPrimaryFingerprint
import app.revanced.patches.youtube.general.loadingscreen.fingerprints.GradientLoadingScreenSecondaryFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.literalInstructionBooleanHook
import app.revanced.util.patch.BaseBytecodePatch

@Suppress("unused")
object GradientLoadingScreenPatch : BaseBytecodePatch(
    name = "Enable gradient loading screen",
    description = "Adds an option to enable gradient loading screen.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        GradientLoadingScreenPrimaryFingerprint,
        GradientLoadingScreenSecondaryFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        /**
         * YouTube v18.29.38 ~
         */
        mapOf(
            GradientLoadingScreenPrimaryFingerprint to 45412406,
            GradientLoadingScreenSecondaryFingerprint to 45418917
        ).forEach { (fingerprint, literal) ->
            fingerprint.literalInstructionBooleanHook(literal, "$GENERAL_CLASS_DESCRIPTOR->enableGradientLoadingScreen()Z")
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: ENABLE_GRADIENT_LOADING_SCREEN"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }
}

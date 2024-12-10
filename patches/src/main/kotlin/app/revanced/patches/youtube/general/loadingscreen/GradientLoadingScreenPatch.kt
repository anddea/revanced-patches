package app.revanced.patches.youtube.general.loadingscreen

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.patch.PatchList.ENABLE_GRADIENT_LOADING_SCREEN
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.util.fingerprint.injectLiteralInstructionBooleanCall

@Suppress("unused")
val gradientLoadingScreenPatch = bytecodePatch(
    ENABLE_GRADIENT_LOADING_SCREEN.title,
    ENABLE_GRADIENT_LOADING_SCREEN.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {

        mapOf(
            gradientLoadingScreenPrimaryFingerprint to 45412406L,
            gradientLoadingScreenSecondaryFingerprint to 45418917L
        ).forEach { (fingerprint, literal) ->
            fingerprint.injectLiteralInstructionBooleanCall(
                literal,
                "$GENERAL_CLASS_DESCRIPTOR->enableGradientLoadingScreen()Z"
            )
        }

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: ENABLE_GRADIENT_LOADING_SCREEN"
            ),
            ENABLE_GRADIENT_LOADING_SCREEN
        )

        // endregion

    }
}

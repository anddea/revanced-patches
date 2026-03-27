package app.morphe.patches.youtube.general.signintotvpopup

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.patch.PatchList.DISABLE_SIGN_IN_TO_TV_POPUP
import app.morphe.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.fingerprint.methodOrThrow

@Suppress("unused")
val signInToTvPopupPatch = bytecodePatch(
    DISABLE_SIGN_IN_TO_TV_POPUP.title,
    DISABLE_SIGN_IN_TO_TV_POPUP.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        sharedResourceIdPatch,
    )

    execute {

        signInToTvPopupFingerprint.methodOrThrow().addInstructionsWithLabels(
            0, """
                invoke-static { }, $GENERAL_CLASS_DESCRIPTOR->disableSignInToTvPopup()Z
                move-result v0
                if-eqz v0, :allow_sign_in_popup
                const/4 v0, 0x0
                return v0
                :allow_sign_in_popup
                nop
                """
        )

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: DISABLE_SIGN_IN_TO_TV_POPUP"
            ),
            DISABLE_SIGN_IN_TO_TV_POPUP
        )

        // endregion

    }
}

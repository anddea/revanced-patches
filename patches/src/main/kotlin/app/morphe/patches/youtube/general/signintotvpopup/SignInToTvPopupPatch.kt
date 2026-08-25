/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.general.signintotvpopup

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.mapping.resourceMappingPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.patch.PatchList.DISABLE_SIGN_IN_TO_TV_POPUP
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.findFreeRegister
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val signInToTvPopupPatch = bytecodePatch(
    DISABLE_SIGN_IN_TO_TV_POPUP.title,
    DISABLE_SIGN_IN_TO_TV_POPUP.summary,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE)

    dependsOn(
        settingsPatch,
        resourceMappingPatch,
    )

    execute {
        SignInToTVPopupFingerprint.let {
            it.method.apply {
                val index = it.instructionMatches.last().index
                val register = getInstruction<OneRegisterInstruction>(
                    index
                ).registerA
                val free = findFreeRegister(
                    index, register
                )
                val className = definingClass
                val dismissMethodName = SignInToTVPopupDismissFingerprint.method.name

                addInstructionsWithLabels(
                    index,
                    """
                        invoke-static { }, $GENERAL_CLASS_DESCRIPTOR->disableSignInToTvPopup()Z
                        move-result v$free
                        if-eqz v$free, :allow_sign_in_popup
                        invoke-virtual { p0 }, $className->$dismissMethodName()V
                        :allow_sign_in_popup
                        nop
                    """
                )
            }
        }

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

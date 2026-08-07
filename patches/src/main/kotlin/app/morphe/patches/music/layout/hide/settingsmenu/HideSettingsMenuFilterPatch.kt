/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/2029
 *
 * Portions of this file are modified by anddea:
 * Copyright (C) 2026 anddea
 * https://github.com/anddea/revanced-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.music.layout.hide.settingsmenu

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.music.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE_MUSIC
import app.morphe.patches.music.utils.extension.sharedExtensionPatch
import app.morphe.patches.music.utils.patch.PatchList.SETTINGS_MENU_FILTER
import app.morphe.patches.music.utils.settings.CategoryType
import app.morphe.patches.music.utils.settings.ResourceUtils.addCustomPreference
import app.morphe.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.morphe.patches.music.utils.settings.settingsPatch
import app.morphe.patches.shared.misc.settingsmenu.HIDE_MATCHING_METHOD
import app.morphe.patches.shared.misc.settingsmenu.SETTINGS_MENU_FILTER_CLASS
import app.morphe.patches.shared.misc.settingsmenu.injectHideMatchingHelper
import app.morphe.patches.shared.misc.settingsmenu.injectSettingsMenuFilterHook
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_CLASS =
    "Lapp/morphe/extension/music/patches/SettingsMenuFilterPatch;"

private const val SETTINGS_HEADERS_FRAGMENT_CLASS =
    "Lcom/google/android/apps/youtube/music/settings/fragment/SettingsHeadersFragment;"

@Suppress("unused")
val hideSettingsMenuFilterPatch = bytecodePatch(
    SETTINGS_MENU_FILTER.title,
    SETTINGS_MENU_FILTER.summary,
) {
    dependsOn(
        settingsPatch,
        sharedExtensionPatch
    )

    compatibleWith(COMPATIBILITY_YOUTUBE_MUSIC)

    execute {
        addCustomPreference(
            CategoryType.GENERAL.value,
            "morphe_settings_menu_filter_screen",
            "app.morphe.extension.shared.patches.SettingsMenuFilterPickerPreference",
            "",
            true,
            "revanced_change_start_page"
        )

        updatePatchStatus(SETTINGS_MENU_FILTER)

        injectSettingsMenuFilterHook(EXTENSION_CLASS)
        injectHideMatchingHelper()

        // Recover the fragment via peer.fragment using the register holding the peer object.
        SettingsHeadersOnCreatePreferencesFingerprint.let {
            val matchInstruction = it.instructionMatches.first().instruction
            val fragmentField = matchInstruction.getReference<FieldReference>()!!
            val peerRegister = (matchInstruction as TwoRegisterInstruction).registerB

            it.method.apply {
                val insertIndex = implementation!!.instructions.size - 1

                addInstructionsWithLabels(
                    insertIndex,
                    """
                        iget-object v0, v$peerRegister, $fragmentField
                        invoke-virtual { v0 }, $SETTINGS_HEADERS_FRAGMENT_CLASS->getPreferenceScreen()Landroidx/preference/PreferenceScreen;
                        move-result-object v0
                        if-eqz v0, :ignore

                        invoke-static { }, $EXTENSION_CLASS->getNeedles()[Ljava/lang/String;
                        move-result-object v1
                        if-eqz v1, :ignore

                        invoke-static { }, $SETTINGS_MENU_FILTER_CLASS->beginCapture()V

                        const/4 v2, 0x0
                        invoke-virtual { v0, v1, v2 }, $HIDE_MATCHING_METHOD

                        invoke-static { }, $SETTINGS_MENU_FILTER_CLASS->endCapture()V

                        :ignore
                        nop
                    """
                )
            }
        }
    }
}

/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/2029
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.youtube.layout.hide.settingsmenu

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.misc.settingsmenu.HIDE_MATCHING_METHOD
import app.morphe.patches.shared.misc.settingsmenu.SETTINGS_MENU_FILTER_CLASS
import app.morphe.patches.shared.misc.settingsmenu.injectHideMatchingHelper
import app.morphe.patches.shared.misc.settingsmenu.injectSettingsMenuFilterHook
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.patch.PatchList.SETTINGS_MENU_FILTER
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.findFreeRegister
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

private const val EXTENSION_CLASS =
    "Lapp/morphe/extension/youtube/patches/SettingsMenuFilterPatch;"

@Suppress("unused")
val hideSettingsMenuFilterPatch = bytecodePatch(
    SETTINGS_MENU_FILTER.title,
    SETTINGS_MENU_FILTER.summary,
) {
    dependsOn(
        settingsPatch,
        sharedExtensionPatch
    )

    compatibleWith(COMPATIBILITY_YOUTUBE)

    execute {
        addPreference(
            arrayOf(
                "SETTINGS: HIDE_SETTINGS_MENU_FILTER"
            ),
            SETTINGS_MENU_FILTER
        )

        injectSettingsMenuFilterHook(EXTENSION_CLASS)
        injectHideMatchingHelper()

        // Reuse the method's own getPreferenceScreen call; fragmentRegister is dead after it.
        PreferenceScreenSyntheticFingerprint.let {
            it.method.apply {
                val getPreferenceScreenIndex = it.instructionMatches[1].index
                val fragmentRegister =
                    getInstruction<FiveRegisterInstruction>(getPreferenceScreenIndex).registerC
                val getPreferenceScreenReference =
                    getInstruction<ReferenceInstruction>(getPreferenceScreenIndex).reference

                // Insert after the settings-name block added by settingsPatch. The
                // fragment register is reused for the needles array below, so no
                // later code may need it as the settings fragment receiver.
                val insertIndex = it.method.implementation!!.instructions.lastIndex
                val screenRegister = findFreeRegister(insertIndex, fragmentRegister)
                val nullRegister = findFreeRegister(insertIndex, fragmentRegister, screenRegister)

                addInstructionsAtControlFlowLabel(
                    insertIndex,
                    """
                        invoke-virtual { v$fragmentRegister }, $getPreferenceScreenReference
                        move-result-object v$screenRegister
                        if-eqz v$screenRegister, :ignore

                        invoke-static { }, $EXTENSION_CLASS->getNeedles()[Ljava/lang/String;
                        move-result-object v$fragmentRegister
                        if-eqz v$fragmentRegister, :ignore

                        invoke-static { }, $SETTINGS_MENU_FILTER_CLASS->beginCapture()V

                        const/4 v$nullRegister, 0x0
                        invoke-virtual { v$screenRegister, v$fragmentRegister, v$nullRegister }, $HIDE_MATCHING_METHOD

                        invoke-static { }, $SETTINGS_MENU_FILTER_CLASS->endCapture()V

                        :ignore
                        nop
                    """
                )
            }
        }
    }
}

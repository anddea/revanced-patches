/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/2029
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.youtube.layout.hide.settingsmenu

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
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

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

        PreferenceScreenSyntheticFingerprint.let {
            val fragmentField = it.classDef.fields.first { field ->
                field.type.startsWith("L")
            }

            it.method.apply {
                val getPreferenceScreenReference =
                    it.instructionMatches[1].instruction.getReference<MethodReference>()!!

                val insertIndex = it.instructionMatches.last().index
                val fragmentRegister = findFreeRegister(insertIndex)
                val screenRegister = findFreeRegister(insertIndex, fragmentRegister)
                val needlesRegister = findFreeRegister(insertIndex, fragmentRegister, screenRegister)
                val nullRegister = findFreeRegister(insertIndex, fragmentRegister, screenRegister, needlesRegister)

                addInstructionsAtControlFlowLabel(
                    insertIndex,
                    """
                        iget-object v$fragmentRegister, p0, ${it.classDef.type}->${fragmentField.name}:${fragmentField.type}
                        if-eqz v$fragmentRegister, :ignore

                        invoke-virtual { v$fragmentRegister }, $getPreferenceScreenReference
                        move-result-object v$screenRegister
                        if-eqz v$screenRegister, :ignore

                        invoke-static { }, $EXTENSION_CLASS->getNeedles()[Ljava/lang/String;
                        move-result-object v$needlesRegister
                        if-eqz v$needlesRegister, :ignore

                        invoke-static { }, $SETTINGS_MENU_FILTER_CLASS->beginCapture()V

                        const/4 v$nullRegister, 0x0
                        invoke-virtual { v$screenRegister, v$needlesRegister, v$nullRegister }, $HIDE_MATCHING_METHOD

                        invoke-static { }, $SETTINGS_MENU_FILTER_CLASS->endCapture()V

                        :ignore
                        nop
                    """
                )
            }
        }
    }
}

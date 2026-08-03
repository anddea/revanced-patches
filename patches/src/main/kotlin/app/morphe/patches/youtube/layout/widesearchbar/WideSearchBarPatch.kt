/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/2221
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.youtube.layout.widesearchbar

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.mapping.resourceMappingPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.patch.PatchList.WIDE_SEARCH_BAR
import app.morphe.patches.youtube.utils.playservice.is_20_31_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.patches.youtube.utils.toolbar.hookToolBarWithImageView
import app.morphe.patches.youtube.utils.toolbar.toolBarHookPatch
import app.morphe.util.addInstructionsAtControlFlowLabel
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS = "Lapp/morphe/extension/youtube/patches/WideSearchBarPatch;"

@Suppress("unused")
val wideSearchBarPatch = bytecodePatch(
    name = WIDE_SEARCH_BAR.title,
    description = WIDE_SEARCH_BAR.summary,
) {
    dependsOn(
        sharedExtensionPatch,
        resourceMappingPatch,
        sharedResourceIdPatch,
        settingsPatch,
        versionCheckPatch,
        toolBarHookPatch
    )

    compatibleWith(COMPATIBILITY_YOUTUBE)

    execute {
        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: ENABLE_WIDE_SEARCH_BAR"
            ),
            WIDE_SEARCH_BAR
        )

        if (!is_20_31_or_greater) {
            applyLegacyWideSearchBar()
            return@execute
        }

        hookToolBarWithImageView("$EXTENSION_CLASS->setSearchButtonView")

        ActionbarRingoViewFingerprint.apply {
            arrayOf(
                instructionMatches[5],
                instructionMatches[3],
                instructionMatches[1]
            ).forEach { match ->
                val index = match.index
                val register = match.getInstruction<OneRegisterInstruction>().registerA

                method.addInstructionsAtControlFlowLabel(
                    index,
                    "invoke-static { v$register }, $EXTENSION_CLASS->" +
                            "initializeWideSearchbar(Landroid/view/View;)V"
                )
            }
        }
    }
}

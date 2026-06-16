/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.misc.share

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.litho.addLithoFilter
import app.morphe.patches.shared.litho.lithoFilterPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import app.morphe.patches.youtube.utils.extension.Constants.PATCHES_PATH
import app.morphe.patches.youtube.utils.patch.PatchList.CHANGE_SHARE_SHEET
import app.morphe.patches.youtube.utils.recyclerview.recyclerViewTreeObserverHook
import app.morphe.patches.youtube.utils.recyclerview.recyclerViewTreeObserverPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$PATCHES_PATH/OpenSystemShareSheetPatch;"

private const val FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/SystemShareSheetFilter;"

@Suppress("unused")
val shareSheetPatch = bytecodePatch(
    CHANGE_SHARE_SHEET.title,
    CHANGE_SHARE_SHEET.summary,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE)

    dependsOn(
        settingsPatch,
        lithoFilterPatch,
        recyclerViewTreeObserverPatch,
    )

    execute {

        // Detects that the Share sheet panel has been invoked.
        recyclerViewTreeObserverHook("$EXTENSION_CLASS_DESCRIPTOR->onFlyoutMenuCreate(Landroid/support/v7/widget/RecyclerView;)V")

        // Remove the app list from the Share sheet panel on YouTube.
        QueryIntentListFingerprint.method.addInstructions(
            0, """
                invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->openSystemShareSheetEnabled()Z
                move-result v0
                if-eqz v0, :ignore
                new-instance v0, Ljava/util/ArrayList;
                invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V
                return-object v0
                :ignore
                nop
                """
        )

        addLithoFilter(FILTER_CLASS_DESCRIPTOR)

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_CATEGORY: MISC_EXPERIMENTAL_FLAGS",
                "SETTINGS: CHANGE_SHARE_SHEET"
            ),
            CHANGE_SHARE_SHEET
        )

        // endregion

    }
}

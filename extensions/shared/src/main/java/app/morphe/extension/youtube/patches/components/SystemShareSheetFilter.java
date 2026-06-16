/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to Morphe contributions.
 */

package app.morphe.extension.youtube.patches.components;

import app.morphe.extension.shared.patches.components.Filter;
import app.morphe.extension.shared.patches.components.StringFilterGroup;
import app.morphe.extension.youtube.patches.OpenSystemShareSheetPatch;
import app.morphe.extension.youtube.settings.Settings;

/**
 * LithoFilter for {@link OpenSystemShareSheetPatch}.
 */
public final class SystemShareSheetFilter extends Filter {
    public static volatile boolean isShareSheetVisible;

    public SystemShareSheetFilter() {
        addPathCallbacks(
                new StringFilterGroup(
                        Settings.CHANGE_SHARE_SHEET,
                        "share_sheet_container."
                )
        );
    }

    @Override
    public boolean isFiltered(String path, String identifier, String allValue, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        isShareSheetVisible = true;
        return false;
    }
}

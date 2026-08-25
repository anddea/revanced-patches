/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.extension.youtube.patches.components;

import android.net.Uri;
import android.view.View;
import app.morphe.extension.shared.patches.components.Filter;
import app.morphe.extension.shared.patches.components.StringFilterGroup;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class LayoutComponentsFilter extends Filter {
    private static final String ACCOUNT_HEADER_PATH = "account_header.";
    private static final String HANDLE_PATH = "|CellType|ContainerType|ContainerType|ContainerType|TextType|";

    public LayoutComponentsFilter() {
        addIdentifierCallbacks(
                new StringFilterGroup(
                        Settings.HIDE_VISUAL_SPACER,
                        "cell_divider"
                )
        );

        addPathCallbacks(
                new StringFilterGroup(
                        Settings.HIDE_HANDLE,
                        ACCOUNT_HEADER_PATH
                )
        );
    }

    @Override
    public boolean isFiltered(String path, String identifier, String allValue, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        return contentType != FilterContentType.PATH || (contentIndex == 0 && path.contains(HANDLE_PATH));
    }

    /**
     * Injection point.
     */
    public static void hideSyncButton(View view) {
        Utils.hideViewBy0dpUnderCondition(Settings.HIDE_SYNC_BUTTON, view);
    }

    /**
     * Hides a search term thumbnail and prevents the image loader from populating it.
     *
     * @param view The thumbnail view supplied to the image loader.
     * @param uri The thumbnail URI supplied to the image loader.
     * @return {@code null} when thumbnails are hidden; otherwise the original URI.
     */
    public static Uri hideSearchTermThumbnails(View view, Uri uri) {
        if (Settings.HIDE_SEARCH_TERM_THUMBNAIL.get()) {
            if (view != null) {
                Utils.hideViewByLayoutParams(view);
            }
            return null;
        }
        return uri;
    }
}

/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.extension.music.patches.components;

import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.Nullable;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.ResourceType;
import app.morphe.extension.shared.utils.ResourceUtils;
import app.morphe.extension.music.settings.Settings;
import app.morphe.extension.shared.patches.components.Filter;
import app.morphe.extension.shared.patches.components.StringFilterGroup;

@SuppressWarnings("unused")
public final class LayoutComponentsFilter extends Filter {

    /**
     * Resource id of the search box, resolved on first use.
     */
    private static int searchEditTextId;

    public LayoutComponentsFilter() {

        final StringFilterGroup buttonShelf = new StringFilterGroup(
                Settings.HIDE_BUTTON_SHELF,
                "entry_point_button_shelf."
        );

        final StringFilterGroup carouselShelf = new StringFilterGroup(
                Settings.HIDE_CAROUSEL_SHELF,
                "music_grid_item_carousel."
        );

        final StringFilterGroup playlistCardShelf = new StringFilterGroup(
                Settings.HIDE_PLAYLIST_CARD_SHELF,
                "music_container_card_shelf."
        );

        final StringFilterGroup sampleShelf = new StringFilterGroup(
                Settings.HIDE_SAMPLE_SHELF,
                "immersive_card_shelf."
        );

        final StringFilterGroup yourDownloadsShelf = new StringFilterGroup(
                Settings.HIDE_YOUR_DOWNLOADS_SHELF,
                "music_downloads_shelf.e"
        );

        addPathCallbacks(yourDownloadsShelf);

        addIdentifierCallbacks(
                buttonShelf,
                carouselShelf,
                playlistCardShelf,
                sampleShelf
        );
    }
    /**
     * Injection point.
     * Called before the search suggestions response is shown.
     *
     * @param suggestionsList The search suggestions list.
     * @return If shelves that only hold suggested searches ('You may also like') should be removed.
     */
    public static boolean hideYouMayAlsoLikeSection(@Nullable View suggestionsList) {
        if (!Settings.HIDE_YOU_MAY_ALSO_LIKE_SECTION.get()) {
            return false;
        }

        try {
            // The section is only shown before a query is typed.
            // Suggestions for a typed query are never changed.
            if (suggestionsList != null) {
                if (searchEditTextId == 0) {
                    searchEditTextId = ResourceUtils.getIdentifier(ResourceType.ID, "search_edit_text");
                }

                View searchEditText = suggestionsList.getRootView().findViewById(searchEditTextId);
                if (searchEditText instanceof TextView textView) {
                    CharSequence query = textView.getText();
                    return TextUtils.isEmpty(textView.getText());
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "hideYouMayAlsoLikeSection failure", ex);
        }

        return true;
    }
}

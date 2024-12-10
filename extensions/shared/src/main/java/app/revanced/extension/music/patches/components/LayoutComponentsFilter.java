package app.revanced.extension.music.patches.components;

import app.revanced.extension.music.settings.Settings;
import app.revanced.extension.shared.patches.components.Filter;
import app.revanced.extension.shared.patches.components.StringFilterGroup;

@SuppressWarnings("unused")
public final class LayoutComponentsFilter extends Filter {

    public LayoutComponentsFilter() {

        final StringFilterGroup buttonShelf = new StringFilterGroup(
                Settings.HIDE_BUTTON_SHELF,
                "entry_point_button_shelf.eml"
        );

        final StringFilterGroup carouselShelf = new StringFilterGroup(
                Settings.HIDE_CAROUSEL_SHELF,
                "music_grid_item_carousel.eml"
        );

        final StringFilterGroup playlistCardShelf = new StringFilterGroup(
                Settings.HIDE_PLAYLIST_CARD_SHELF,
                "music_container_card_shelf.eml"
        );

        final StringFilterGroup sampleShelf = new StringFilterGroup(
                Settings.HIDE_SAMPLE_SHELF,
                "immersive_card_shelf.eml"
        );

        addIdentifierCallbacks(
                buttonShelf,
                carouselShelf,
                playlistCardShelf,
                sampleShelf
        );
    }
}

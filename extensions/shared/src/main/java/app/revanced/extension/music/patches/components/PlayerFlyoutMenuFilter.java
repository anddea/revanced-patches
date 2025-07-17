package app.revanced.extension.music.patches.components;

import androidx.annotation.Nullable;

import app.revanced.extension.music.settings.Settings;
import app.revanced.extension.shared.patches.components.ByteArrayFilterGroup;
import app.revanced.extension.shared.patches.components.Filter;
import app.revanced.extension.shared.patches.components.StringFilterGroup;

@SuppressWarnings("unused")
public final class PlayerFlyoutMenuFilter extends Filter {
    private final StringFilterGroup listItem;
    private final ByteArrayFilterGroup downloadButton;

    public PlayerFlyoutMenuFilter() {
        addIdentifierCallbacks(
                new StringFilterGroup(
                        Settings.HIDE_FLYOUT_MENU_3_COLUMN_COMPONENT,
                        "music_highlight_menu_item_carousel.eml",
                        "tile_button_carousel.eml"
                )

        );

        listItem = new StringFilterGroup(
                Settings.HIDE_FLYOUT_MENU_DOWNLOAD,
                "list_item.eml"
        );

        downloadButton = new ByteArrayFilterGroup(
                Settings.HIDE_FLYOUT_MENU_DOWNLOAD,
                "yt_outline_download"
        );

        addPathCallbacks(listItem);
    }

    @Override
    public boolean isFiltered(String path, @Nullable String identifier, String allValue, byte[] protobufBufferArray,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (matchedGroup == listItem) {
            if (contentIndex == 0 && downloadButton.check(protobufBufferArray).isFiltered()) {
                return super.isFiltered(path, identifier, allValue, protobufBufferArray, matchedGroup, contentType, contentIndex);
            }
            return false;
        }

        return super.isFiltered(path, identifier, allValue, protobufBufferArray, matchedGroup, contentType, contentIndex);
    }
}

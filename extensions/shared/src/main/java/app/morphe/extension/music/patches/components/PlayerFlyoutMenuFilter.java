package app.morphe.extension.music.patches.components;

import static app.morphe.extension.shared.utils.ResourceUtils.getString;

import app.morphe.extension.music.settings.Settings;
import app.morphe.extension.shared.patches.components.ByteArrayFilterGroup;
import app.morphe.extension.shared.patches.components.ByteArrayFilterGroupList;
import app.morphe.extension.shared.patches.components.Filter;
import app.morphe.extension.shared.patches.components.StringFilterGroup;

@SuppressWarnings("unused")
public final class PlayerFlyoutMenuFilter extends Filter {
    private final StringFilterGroup listItem;
    private final ByteArrayFilterGroupList bufferGroupList = new ByteArrayFilterGroupList();

    public PlayerFlyoutMenuFilter() {
        addIdentifierCallbacks(
                new StringFilterGroup(
                        Settings.HIDE_FLYOUT_MENU_3_COLUMN_COMPONENT,
                        "music_highlight_menu_item_carousel.",
                        "tile_button_carousel."
                )
        );

        listItem = new StringFilterGroup(
                Settings.HIDE_FLYOUT_MENU_DOWNLOAD,
                "list_item."
        );

        bufferGroupList.addAll(
                new ByteArrayFilterGroup(
                        Settings.HIDE_FLYOUT_MENU_DOWNLOAD,
                        "yt_fill_downloaded",
                        "yt_outline_download",
                        "yt_outline_experimental_download"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_FLYOUT_MENU_TASTE_MATCH,
                        "yt_outline_circles_overlap"
                )
        );

        addPathCallbacks(listItem);
    }

    @Override
    public boolean isFiltered(String path, String identifier, String allValue, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (matchedGroup == listItem) {
            if (contentIndex == 0 && bufferGroupList.check(buffer).isFiltered()) {
                return true;
            }
            if (identifier != null && !identifier.isEmpty()) {
                if (Settings.HIDE_FLYOUT_MENU_DOWNLOAD.get()) {
                    String dlSongs = getString("action_add_to_offline_songs");
                    String dlPlaylist = getString("action_add_playlist_to_offline");
                    String rmSongs = getString("action_remove_from_offline_songs");
                    String rmPlaylist = getString("action_remove_playlist_from_offline");
                    if (identifier.endsWith("|" + dlSongs)
                            || identifier.endsWith("|" + dlPlaylist)
                            || identifier.endsWith("|" + rmSongs)
                            || identifier.endsWith("|" + rmPlaylist)) {
                        return true;
                    }
                }
                if (Settings.HIDE_FLYOUT_MENU_REMOVE_FROM_LIBRARY.get()) {
                    String label = getString("accessibility_undo_add_to_library");
                    if (identifier.endsWith("|" + label)) {
                        return true;
                    }
                }
                if (Settings.HIDE_FLYOUT_MENU_SAVE_EPISODE_FOR_LATER_SAVE_TO_LIBRARY.get()) {
                    String label = getString("add_to_library_a11y_text");
                    return identifier.endsWith("|" + label);
                }
            }
            return false;
        }

        return true;
    }

    @Override
    public boolean isFiltered(Object contextSource, String identifier, String accessibility, String path, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        return isFiltered(path, accessibility, "", buffer, matchedGroup, contentType, contentIndex);
    }

    @Override
    public boolean useModernFilterDataInLegacyBridge() {
        return true;
    }
}

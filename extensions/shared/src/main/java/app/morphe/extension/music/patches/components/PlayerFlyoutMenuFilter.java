/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - inotia00 (https://github.com/inotia00)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Additional Terms & Attribution Requirements
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Source Credit Preservation (Section 7(b)): This specific copyright notice
 *    and the list of original authors above must be preserved in any copy
 *    or derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin & Modification Marking (Section 7(c)): Modified versions must be
 *    clearly marked as such (e.g., by adding a "Modified by" line or a new
 *    copyright notice) and must not be misrepresented as the original work.
 *
 * 3. Version Control Attribution (Section 7(b)): Any ports or substantial
 *    modifications must retain historical authorship credit in version control
 *    systems (e.g., Git), listing original author(s) appropriately and
 *    modifiers as committers or co-authors.
 *
 * 4. User Interface Attribution (Section 7(b)): Any works containing or
 *    derived from this material must maintain a visible credit or
 *    acknowledgment to the original author(s) within the application's
 *    user interface (e.g., in an "About" or "Credits" section).
 */

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

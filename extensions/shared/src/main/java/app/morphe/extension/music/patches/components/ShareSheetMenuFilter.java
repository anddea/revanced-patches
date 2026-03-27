package app.morphe.extension.music.patches.components;

import app.morphe.extension.music.patches.misc.ShareSheetPatch;
import app.morphe.extension.music.settings.Settings;
import app.morphe.extension.shared.patches.components.Filter;
import app.morphe.extension.shared.patches.components.StringFilterGroup;

/**
 * LithoFilter for {@link ShareSheetPatch}.
 */
public final class ShareSheetMenuFilter extends Filter {
    // Must be volatile or synchronized, as litho filtering runs off main thread and this field is then access from the main thread.
    public static volatile boolean isShareSheetMenuVisible;

    public ShareSheetMenuFilter() {
        addIdentifierCallbacks(
                new StringFilterGroup(
                        Settings.CHANGE_SHARE_SHEET,
                        "share_sheet_container."
                )
        );
    }

    @Override
    public boolean isFiltered(String path, String identifier, String allValue, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        isShareSheetMenuVisible = true;

        return false;
    }
}

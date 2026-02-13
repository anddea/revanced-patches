package app.morphe.extension.youtube.patches.components;

import app.morphe.extension.shared.patches.components.Filter;
import app.morphe.extension.shared.patches.components.StringFilterGroup;
import app.morphe.extension.youtube.patches.misc.ShareSheetPatch;
import app.morphe.extension.youtube.settings.Settings;

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

package app.revanced.extension.youtube.patches.components;

import app.revanced.extension.shared.patches.components.Filter;
import app.revanced.extension.shared.patches.components.StringFilterGroup;
import app.revanced.extension.youtube.patches.video.AdvancedVideoQualityMenuPatch;
import app.revanced.extension.youtube.settings.Settings;

/**
 * LithoFilter for {@link AdvancedVideoQualityMenuPatch}.
 */
public final class VideoQualityMenuFilter extends Filter {
    // Must be volatile or synchronized, as litho filtering runs off main thread and this field is then access from the main thread.
    public static volatile boolean isVideoQualityMenuVisible;

    public VideoQualityMenuFilter() {
        addPathCallbacks(
                new StringFilterGroup(
                        Settings.ADVANCED_VIDEO_QUALITY_MENU,
                        "quick_quality_sheet_content.eml-js"
                )
        );
    }

    @Override
    public boolean isFiltered(String path, String identifier, String allValue, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        isVideoQualityMenuVisible = true;

        return false;
    }
}

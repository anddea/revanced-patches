package app.revanced.extension.youtube.patches.components;

import androidx.annotation.Nullable;

import app.revanced.extension.shared.patches.components.Filter;
import app.revanced.extension.shared.patches.components.StringFilterGroup;
import app.revanced.extension.youtube.patches.video.CustomPlaybackSpeedPatch;
import app.revanced.extension.youtube.settings.Settings;

/**
 * Abuse LithoFilter for {@link CustomPlaybackSpeedPatch}.
 */
public final class PlaybackSpeedMenuFilter extends Filter {
    /**
     * Old litho based speed selection menu.
     */
    public static volatile boolean isOldPlaybackSpeedMenuVisible;

    /**
     * 0.05x speed selection menu.
     */
    public static volatile boolean isPlaybackRateSelectorMenuVisible;

    private final StringFilterGroup oldPlaybackMenuGroup;

    public PlaybackSpeedMenuFilter() {
        // 0.05x litho speed menu.
        final StringFilterGroup playbackRateSelectorGroup = new StringFilterGroup(
                Settings.ENABLE_CUSTOM_PLAYBACK_SPEED,
                "playback_rate_selector_menu_sheet.eml-js"
        );

        // Old litho based speed menu.
        oldPlaybackMenuGroup = new StringFilterGroup(
                Settings.ENABLE_CUSTOM_PLAYBACK_SPEED,
                "playback_speed_sheet_content.eml-js");

        addPathCallbacks(playbackRateSelectorGroup, oldPlaybackMenuGroup);
    }

    @Override
    public boolean isFiltered(String path, @Nullable String identifier, String allValue, byte[] protobufBufferArray,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (matchedGroup == oldPlaybackMenuGroup) {
            isOldPlaybackSpeedMenuVisible = true;
        } else {
            isPlaybackRateSelectorMenuVisible = true;
        }

        return false;
    }
}

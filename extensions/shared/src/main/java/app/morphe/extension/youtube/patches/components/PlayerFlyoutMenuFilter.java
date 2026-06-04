package app.morphe.extension.youtube.patches.components;

import static app.morphe.extension.youtube.utils.ExtendedUtils.IS_20_31_OR_GREATER;

import app.morphe.extension.shared.patches.components.ByteArrayFilterGroup;
import app.morphe.extension.shared.patches.components.ByteArrayFilterGroupList;
import app.morphe.extension.shared.patches.components.Filter;
import app.morphe.extension.shared.patches.components.StringFilterGroup;
import app.morphe.extension.shared.utils.StringTrieSearch;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.PlayerType;

@SuppressWarnings("unused")
public final class PlayerFlyoutMenuFilter extends Filter {
    private final ByteArrayFilterGroupList flyoutFilterGroupList = new ByteArrayFilterGroupList();

    private final ByteArrayFilterGroup flyoutLoopVideoButton;
    private final ByteArrayFilterGroup qualityMenuButton;
    private final ByteArrayFilterGroup byteArrayException;
    private final StringTrieSearch pathBuilderException = new StringTrieSearch();
    private final StringFilterGroup playerFlyoutMenu;
    private final StringFilterGroup divider;
    private final StringFilterGroup qualityFooter;

    public PlayerFlyoutMenuFilter() {
        flyoutLoopVideoButton = new ByteArrayFilterGroup(
                null,
                "yt_outline_arrow_repeat_1_",
                "yt_outline_experimental_repeat1_",
                "yt_outline_experimental_play_circle_black_"
        );

        qualityMenuButton = new ByteArrayFilterGroup(
                null,
                "yt_outline_adjust_",
                "yt_outline_experimental_adjust_"
        );

        byteArrayException = new ByteArrayFilterGroup(
                null,
                "quality_sheet"
        );
        pathBuilderException.addPattern(
                "bottom_sheet_list_option.e"
        );

        divider = new StringFilterGroup(
                null,
                "|divider.e"
        );

        qualityFooter = new StringFilterGroup(
                Settings.HIDE_PLAYER_FLYOUT_MENU_QUALITY_FOOTER,
                "quality_sheet_footer.e"
        );

        playerFlyoutMenu = new StringFilterGroup(
                null,
                "overflow_menu_item.e"
        );

        // Using pathFilterGroupList due to new flyout panel(A/B)
        addPathCallbacks(
                divider,
                qualityFooter,
                playerFlyoutMenu
        );

        flyoutFilterGroupList.addAll(
                new ByteArrayFilterGroup(
                        Settings.HIDE_PLAYER_FLYOUT_MENU_AMBIENT,
                        "yt_outline_experimental_ambient_mode_",
                        "yt_outline_screen_light_"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_PLAYER_FLYOUT_MENU_AUDIO_TRACK,
                        "yt_outline_experimental_person_",
                        "yt_outline_person_"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_PLAYER_FLYOUT_MENU_CAPTIONS,
                        "closed_caption_",
                        "yt_outline_experimental_closed_captions_"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_PLAYER_FLYOUT_MENU_HELP,
                        "yt_outline_experimental_help_circle_",
                        "yt_outline_question_circle_"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_PLAYER_FLYOUT_MENU_LOCK_SCREEN,
                        "yt_outline_experimental_lock_",
                        "yt_outline_lock_"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_PLAYER_FLYOUT_MENU_MORE,
                        "yt_outline_info_circle_"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_PLAYER_FLYOUT_MENU_PIP,
                        "yt_fill_picture_in_picture_",
                        "yt_outline_picture_in_picture_"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_PLAYER_FLYOUT_MENU_PLAYBACK_SPEED,
                        "yt_outline_play_arrow_half_circle_",
                        "yt_outline_experimental_play_circle_half_dashed_"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_PLAYER_FLYOUT_MENU_ADDITIONAL_SETTINGS,
                        "yt_outline_experimental_gear_",
                        "yt_outline_gear_"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_PLAYER_FLYOUT_MENU_REPORT,
                        "yt_outline_experimental_flag_",
                        "yt_outline_flag_"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_PLAYER_FLYOUT_MENU_STABLE_VOLUME,
                        "yt_fill_experimental_stable_volume_",
                        "yt_outline_experimental_stable_volume_",
                        "volume_stable_"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_PLAYER_FLYOUT_MENU_SLEEP_TIMER,
                        "yt_outline_experimental_sleep_timer_",
                        "yt_outline_moon_z_"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_PLAYER_FLYOUT_MENU_STATS_FOR_NERDS,
                        "yt_outline_statistics_graph_"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_PLAYER_FLYOUT_MENU_WATCH_IN_VR,
                        "yt_outline_experimental_vr_",
                        "yt_outline_vr_"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_PLAYER_FLYOUT_MENU_YT_MUSIC,
                        "yt_outline_experimental_youtube_music_",
                        "yt_outline_youtube_music_"
                )
        );
    }

    @Override
    public boolean isFiltered(String path, String identifier, String allValue, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (matchedGroup == playerFlyoutMenu) {
            // Overflow menu is always the start of the path.
            if (contentIndex != 0) {
                return false;
            }

            // Shorts also use this player flyout panel
            if (PlayerType.getCurrent().isNoneOrHidden()) {
                return false;
            }

            if (IS_20_31_OR_GREATER && pathBuilderException.matches(path)) {
                return false;
            }

            if (Settings.HIDE_PLAYER_FLYOUT_MENU_QUALITY.get()
                    && qualityMenuButton.check(buffer).isFiltered()) {
                return true;
            }

            if (byteArrayException.check(buffer).isFiltered()) {
                return false;
            }

            if (Settings.HIDE_PLAYER_FLYOUT_MENU_LOOP.get() && flyoutLoopVideoButton.check(buffer).isFiltered()) {
                return true;
            }

            return flyoutFilterGroupList.check(buffer).isFiltered();
        } else if (matchedGroup == qualityFooter) {
            return true;
        } else if (matchedGroup == divider) {
            if (path.contains("captions_sheet_content.")) {
                return Settings.HIDE_PLAYER_FLYOUT_MENU_CAPTIONS_FOOTER.get();
            }

            if (path.contains("quick_quality_sheet_content.")
                    || path.contains("quality_sheet_content.")) {
                return Settings.HIDE_PLAYER_FLYOUT_MENU_QUALITY_FOOTER.get();
            }

            return path.contains("overflow_menu_item.e");
        }

        return false;
    }
}

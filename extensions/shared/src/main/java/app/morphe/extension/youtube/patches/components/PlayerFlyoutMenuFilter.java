/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.extension.youtube.patches.components;

import static app.morphe.extension.youtube.utils.ExtendedUtils.IS_20_31_OR_GREATER;

import app.morphe.extension.shared.patches.components.ByteArrayFilterGroup;
import app.morphe.extension.shared.patches.components.ByteArrayFilterGroupList;
import app.morphe.extension.shared.patches.components.Filter;
import app.morphe.extension.shared.patches.components.StringFilterGroup;
import app.morphe.extension.shared.utils.StringTrieSearch;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.PlayerType;
import app.morphe.extension.youtube.shared.ShortsPlayerState;

@SuppressWarnings("unused")
public final class PlayerFlyoutMenuFilter extends Filter {
    private static boolean topFlyoutMenuVisible;
    public static boolean getTopFlyoutMenuVisible() {
        return topFlyoutMenuVisible;
    }
    public static void resetTopFlyoutMenuVisible() {
        topFlyoutMenuVisible = false;
    }

    private final ByteArrayFilterGroupList flyoutFilterGroupList = new ByteArrayFilterGroupList();

    private final ByteArrayFilterGroup videoPlayerSettingsQualityButton = new ByteArrayFilterGroup(
            null,
            "quality_sheet_header",
            "quick_quality_sheet_content"
    );
    private final ByteArrayFilterGroup shortsPlayerSettingsCaptionsButton = new ByteArrayFilterGroup(
            null,
            "closed_captions"
    );
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
                "yt_outline_experimental_play_circle_black_",
                // 21.x: Base64-encoded Protobuf entity key fragment for "_video_loop_entity"
                "X3ZpZGVvX2xvb3BfZW50aXR5"
        );

        qualityMenuButton = new ByteArrayFilterGroup(
                null,
                "yt_outline_adjust_",
                "yt_outline_experimental_adjust_",
                "quick_quality_sheet_content",
                "quality_sheet_header"
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
                        new String[]{
                                "yt_outline_screen_light_",
                                "yt_outline_experimental_ambient_mode_"
                        },
                        // 21.x: Ambient mode toggle command (field 456628506),
                        // when the icon has no name in the buffer.
                        new byte[]{(byte) 0xD2, (byte) 0x91, (byte) 0xF3, (byte) 0xCD, 0x0D}
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_PLAYER_FLYOUT_MENU_AUDIO_TRACK,
                        "yt_outline_person_",
                        "yt_outline_experimental_person_",
                        "audio_track_sheet_content"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_PLAYER_FLYOUT_MENU_CAPTIONS,
                        "closed_caption_",
                        "yt_outline_experimental_closed_captions_",
                        "captions_sheet_content"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_PLAYER_FLYOUT_MENU_HELP,
                        "yt_outline_question_circle_",
                        "yt_outline_experimental_help_circle_",
                        "yt_android_watch"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_PLAYER_FLYOUT_MENU_LOCK_SCREEN,
                        "yt_outline_lock_",
                        "yt_outline_experimental_lock_",
                        // 21.x: Base64-encoded Protobuf entity key fragment for "lock_mode_state_entity_key"
                        "bG9ja19tb2RlX3N0YXRlX2VudGl0eV9rZXkg"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_PLAYER_FLYOUT_ON_THE_GO,
                        "yt_outline_headset_",
                        "yt_outline_experimental_headset_",
                        // 21.x: Premium upsell parameters of the item, when the icon has no name
                        // in the buffer. Each Premium feature has its own parameters.
                        "igkCCA8%3D"
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
                        "yt_outline_experimental_play_circle_half_dashed_",
                        "playback_rate_selector_menu_sheet"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_PLAYER_FLYOUT_MENU_ADDITIONAL_SETTINGS,
                        new String[]{
                                "yt_outline_gear_",
                                "yt_outline_experimental_gear_"
                        },
                        // 21.x: Command that opens the submenu (field 469660771),
                        // when the icon has no name in the buffer.
                        new byte[]{(byte) 0x9A, (byte) 0xC6, (byte) 0xCE, (byte) 0xFF, 0x0D}
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_PLAYER_FLYOUT_MENU_REPORT,
                        "yt_outline_experimental_flag_",
                        "yt_outline_flag_"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_PLAYER_FLYOUT_MENU_STABLE_VOLUME,
                        new String[]{
                                "volume_stable_",
                                "yt_fill_experimental_stable_volume_",
                                "yt_outline_experimental_stable_volume_"
                        },
                        // 21.x: Stable volume toggle command (field 449823757),
                        // when the icon has no name in the buffer.
                        new byte[]{(byte) 0xEA, (byte) 0xC0, (byte) 0xF8, (byte) 0xB3, 0x0D}
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_PLAYER_FLYOUT_MENU_SLEEP_TIMER,
                        "yt_outline_moon_z_",
                        "yt_outline_experimental_sleep_timer_",
                        "sleep_timer_sheet_content"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_PLAYER_FLYOUT_MENU_STATS_FOR_NERDS,
                        "yt_outline_statistics_graph_"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_PLAYER_FLYOUT_MENU_WATCH_IN_VR,
                        "yt_outline_vr_",
                        "yt_outline_experimental_vr_"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_PLAYER_FLYOUT_MENU_YT_MUSIC,
                        "yt_outline_youtube_music_",
                        "yt_outline_experimental_youtube_music_",
                        // 21.x: Link that opens the playing video in YouTube Music,
                        // when the icon has no name in the buffer.
                        "vnd.youtube.music://music.youtube.com/watch?v="
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

            // Verify that the open flyout menu is the first one and not the 'others'
            // one, by checking the filtering of its first button (quality menu).
            // The path already starts with the overflow menu item, while the buffer
            // does not always contain it (20.21 keeps it in a separate buffer).
            boolean videoPlayerFlyout = PlayerType.getCurrent().isMaximizedOrFullscreen()
                    && videoPlayerSettingsQualityButton.check(buffer).isFiltered();
            boolean shortsPlayerFlyout = ShortsPlayerState.getCurrent().isOpen()
                    && shortsPlayerSettingsCaptionsButton.check(buffer).isFiltered();
            if (videoPlayerFlyout || shortsPlayerFlyout) {
                topFlyoutMenuVisible = true;
            }

            // Shorts also use this player flyout panel
            if (PlayerType.getCurrent().isNoneOrHidden() || ShortsPlayerState.getCurrent().isOpen()) {
                return Settings.HIDE_PLAYER_FLYOUT_MENU_CAPTIONS.get()
                        && shortsPlayerSettingsCaptionsButton.check(buffer).isFiltered();
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

/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/1881
 * https://github.com/MorpheApp/morphe-patches/pull/4698
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.extension.music.patches;

import android.app.Activity;
import android.net.Uri;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.google.protobuf.MessageLite;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import app.morphe.extension.music.patches.components.PlaylistPageFilter;
import app.morphe.extension.music.patches.components.SongMenuFilter;
import app.morphe.extension.music.settings.Settings;
import app.morphe.extension.music.shared.VideoInformation;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.ResourceUtils;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.shared.innertube.Offline.OfflineCommand;
import app.morphe.extension.shared.patches.BaseInAppDownloadsPatch;

import app.morphe.extension.shared.patches.components.ByteArrayFilterGroup;
import app.morphe.extension.shared.patches.components.ContextInterface;


/**
 * Morphe's native command handling, adapted to the local native-download switch.
 * When disabled, the existing flyout and action bar hooks handle downloads instead.
 */
@SuppressWarnings("unused")
public final class DownloadsPatch extends BaseInAppDownloadsPatch {

    /** Only enable native routing when the native hooks were installed for this target. */
    public static boolean isEnabled() {
        return isPatchIncluded() && Settings.NATIVE_DOWNLOADS.get();
    }

    private static boolean isPatchIncluded() {
        return false; // Modified during patching on supported versions.
    }

    private static final String ELEMENTS_SENDER_VIEW =
            "com.google.android.libraries.youtube.rendering.elements.sender_view";
    private static final int IGNORE_DOUBLE_CLICK_DURATION_MS = 1000;

    /**
     * Command that does nothing, which replaces a command that was handled.
     */
    private static final byte[] EMPTY_COMMAND = new byte[0];

    private static volatile String downloadButtonLabel = "";

    /**
     * Fields of the encoded parameters of the offline upsell. The parameters of a song hold its video ID,
     * and the parameters of an album or a playlist hold its id in a nested message.
     */
    private static final int UPSELL_PARAMETERS_VIDEO_ID = 4;
    private static final int UPSELL_PARAMETERS_PLAYLIST = 5;
    private static final int UPSELL_PLAYLIST_ITEM = 10;
    private static final int UPSELL_PLAYLIST_ID = 3;

    /**
     * Liked music, user and editorial playlists, mixes, album playlists and album pages,
     * with or without the 'VL' prefix of a playlist page.
     */
    public static final Pattern PLAYLIST_ID = Pattern.compile(
            "(VL)?(LM|PL(?:[A-Za-z0-9_-]{11}|[A-Za-z0-9_-]{16,})"
                    + "|OLAK5uy_[A-Za-z0-9_-]{11,}|RD[A-Za-z0-9_-]{10,})|MPRE[A-Za-z0-9_-]{8,}");

    private static volatile long lastFlyoutDownloadTime;
    private static volatile long lastPlaylistDownloadTime;
    private static volatile long lastMainPlayerDownloadTime;

    /**
     * Menu Download action of accounts offered Premium, which opens the Premium sheet
     * and has no video ID.
     */
    private static final ByteArrayFilterGroup PREMIUM_UPSELL_COMMAND =
            new ByteArrayFilterGroup(null, "premium_upsell");

    /**
     * Opening the menu of an item resolves a command that holds the whole menu, so it also
     * holds the Download action of the menu without being that action.
     */
    private static final ByteArrayFilterGroup MENU_COMMAND =
            new ByteArrayFilterGroup(null, "panel_header.", "tile_button_carousel.");

    /**
     * Injection point.
     * Usually is called off the main thread, when a litho text is created and its style runs
     * are applied.
     */
    public static CharSequence onLithoTextLoaded(Object context, CharSequence original) {
        try {
            // Such as the text of an icon only button.
            //noinspection SizeReplaceableByIsEmpty
            if (original.length() == 0) return original;

            if (!(context instanceof ContextInterface conversionContext)) return original;
            StringBuilder path = conversionContext.patch_getPathBuilder();
            if (isFlyoutMenuOverridden()
                    && downloadButtonLabel.isEmpty()
                    && path.indexOf("music_download_button.") >= 0) {
                downloadButtonLabel = original.toString();
                Logger.printDebug(() -> "Found download button label: " + downloadButtonLabel);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "onLithoTextLoaded failure", ex);
        }
        return original;
    }

    /**
     * In app downloads and the external downloader always replace the Download button.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isActionButtonOverridden() {
        return isEnabled();
    }

    /**
     * The download mode replaces the Download menu, and the external downloader
     * can replace it in the other modes too.
     */
    public static boolean isFlyoutMenuOverridden() {
        return isEnabled();
    }

    /**
     * Injection point.
     * <p>
     * If the external downloader replaces the Download menu, the menus open it instead,
     * so the upsell is kept.
     */
    @Nullable
    public static byte[] replaceOfflineUpsell(@Nullable MessageLite command, @Nullable Object response) {
        return BaseInAppDownloadsPatch.replaceOfflineUpsell(command, response);
    }

    /**
     * Injection point.
     * <p>
     * Called for every command the app resolves, on or off the main thread.
     * Replaces the Premium upsell of the Download buttons and menus with a command that saves the song,
     * album or playlist with the offline downloads of the app, or opens the external downloader.
     *
     * @param command The command that is resolved.
     * @param map     The context of the command, which holds the view that was tapped.
     * @return The command to resolve instead, an empty array if the command was handled,
     *         or null to keep the original.
     */
    @Nullable
    public static byte[] replaceDownloadCommand(
            @Nullable MessageLite command,
            @Nullable Map<Object, Object> map
    ) {
        try {
            if (!isFlyoutMenuOverridden() || command == null) {
                return null;
            }

            byte[] commandBytes = command.toByteArray();
            if (commandBytes.length == 0) {
                return null;
            }

            final boolean insideDialog = map != null && isViewInsideDialog(map.get(ELEMENTS_SENDER_VIEW));
            OfflineCommand offlineCommand = OfflineCommand.parseFrom(commandBytes);

            // The app resolves each command of a list on its own. Only the upsell of the list
            // is replaced, so the other commands still run, such as the one that closes the menu.
            final boolean premiumUpsell = PREMIUM_UPSELL_COMMAND.check(commandBytes).isFiltered()
                    && !MENU_COMMAND.check(commandBytes).isFiltered()
                    && !offlineCommand.hasCommandExecutorContainer();

            // Menu Download of accounts offered Premium.
            if (insideDialog && premiumUpsell) {
                String videoId = SongMenuFilter.getMenuVideoId();
                if (videoId != null && !videoId.isEmpty()) {
                    return menuVideoDownload(videoId);
                }

                String playlistId = PlaylistPageFilter.getMenuPlaylistId();
                if (playlistId == null || playlistId.isEmpty()) {
                    playlistId = PlaylistPageFilter.getPagePlaylistId();
                }
                return playlistDownload(playlistId, true);
            }

            // Song menus in search results, playlists and the queue share the player's Download label.
            // Only the player action bar button should use the playing song's ID.
            if (!insideDialog && isPlayerDownloadButton(map)) {
                if (!isActionButtonOverridden()) {
                    return null;
                }

                String videoId = VideoInformation.getVideoId();
                if (videoId.isEmpty()) {
                    return null;
                }

                final long now = System.currentTimeMillis();
                if (now - lastMainPlayerDownloadTime < IGNORE_DOUBLE_CLICK_DURATION_MS) {
                    return EMPTY_COMMAND;
                }
                lastMainPlayerDownloadTime = now;

                return videoDownload(videoId);
            }

            // Download button of a playlist or album page of accounts offered Premium.
            // Only a tap can be the page button: commands the app resolves itself have no sender view.
            if (!insideDialog && premiumUpsell
                    && map != null
                    && map.get(ELEMENTS_SENDER_VIEW) instanceof View senderView
                    && !isViewInsideActionBar(senderView)) {
                return playlistDownload(PlaylistPageFilter.getPagePlaylistId(), false);
            }

            // The 'ypc/get_offline_upsell' endpoint, which holds the video ID of a song, or encoded parameters.
            if (offlineCommand.hasOfflineUpsellEndpoint()) {
                String value = offlineCommand.getOfflineUpsellEndpoint().getVideoId();
                if (value.isEmpty()) {
                    return null;
                }
                if (isVideoId(value)) {
                    return insideDialog
                            ? menuVideoDownload(value)
                            : videoDownload(value);
                }

                ProtoMessage parameters = new ProtoMessage(Base64.decode(Uri.decode(value), Base64.URL_SAFE));
                String videoId = getOfflineUpsellVideoId(parameters);
                if (videoId != null) {
                    return insideDialog
                            ? menuVideoDownload(videoId)
                            : videoDownload(videoId);
                }

                String playlistId = parameters.messagePath(UPSELL_PARAMETERS_PLAYLIST, UPSELL_PLAYLIST_ITEM)
                        .stringField(UPSELL_PLAYLIST_ID);
                if (!PLAYLIST_ID.matcher(playlistId).matches()) {
                    playlistId = insideDialog
                            ? PlaylistPageFilter.getMenuPlaylistId()
                            : PlaylistPageFilter.getPagePlaylistId();
                }
                return playlistDownload(playlistId, insideDialog);
            }

            // The endpoints of the songs and playlists the account is allowed to save, such as
            // podcast episodes and Liked music. In-app downloads keep them and only unlock their
            // offlineability, so the quality picker of the app shows the formats of the server.
            // Removing or refreshing a saved song or playlist is left to the app.
        } catch (Exception ex) {
            Logger.printException(() -> "replaceDownloadCommand failure", ex);
        }
        return null;
    }

    /**
     * A quick second tap before the menu closes can repeat the request.
     * The timer is separate from the player button, so a menu download of another song is not blocked.
     */
    @Nullable
    private static byte[] menuVideoDownload(String videoId) {
        final long now = System.currentTimeMillis();
        if (now - lastFlyoutDownloadTime < IGNORE_DOUBLE_CLICK_DURATION_MS) {
            return EMPTY_COMMAND;
        }
        lastFlyoutDownloadTime = now;

        Logger.printDebug(() -> "Menu download of " + videoId);
        return videoDownload(videoId);
    }

    /**
     * @return A command that saves the song in-app, an empty command if the external downloader
     *         was opened, or null if the download mode is the default.
     */
    @Nullable
    private static byte[] videoDownload(String videoId) {
        if (isInAppDownloadMode()) {
            return saveVideoCommand(videoId, List.of()).toByteArray();
        }
        return null;
    }

    /**
     * Saves a playlist or an album in-app, or hands its page to the external downloader,
     * as the download mode says.
     *
     * @return The command to resolve instead, an empty command if the external downloader was opened,
     *         or null to keep the original.
     */
    @Nullable
    private static byte[] playlistDownload(@Nullable String playlistId, boolean insideDialog) {
        if (playlistId == null || !PLAYLIST_ID.matcher(playlistId).matches()) {
            return null;
        }
        if (!insideDialog && !isActionButtonOverridden()) {
            return null;
        }

        // A single tap can resolve the command twice.
        final long now = System.currentTimeMillis();
        if (now - lastPlaylistDownloadTime < IGNORE_DOUBLE_CLICK_DURATION_MS) {
            return EMPTY_COMMAND;
        }
        lastPlaylistDownloadTime = now;

        Logger.printDebug(() -> "Playlist download of " + playlistId);
        if (isInAppDownloadMode()) {
            return savePlaylistCommand(playlistId, List.of())
                    .toByteArray();
        }
        return null;
    }

    public static boolean isVideoId(String value) {
        return value != null && value.matches("[A-Za-z0-9_-]{11}");
    }

    /**
     * @param parameters The encoded parameters of the offline upsell endpoint.
     * @return The video ID, or null if the parameters are of an album or a playlist.
     */
    @Nullable
    private static String getOfflineUpsellVideoId(ProtoMessage parameters) {
        try {
            String videoId = parameters.stringField(UPSELL_PARAMETERS_VIDEO_ID);
            return isVideoId(videoId) ? videoId : null;
        } catch (IllegalArgumentException ex) {
            // The parameters of an album or a playlist hold other fields at this number.
            Logger.printDebug(() -> "Offline upsell parameters are not those of a song", ex);
            return null;
        }
    }

    /**
     * Determines if the clicked view is inside a Dialog/BottomSheet by comparing
     * its Window root to the main Activity's Window root.
     */
    private static boolean isViewInsideDialog(@Nullable Object viewObj) {
        if (viewObj instanceof View view) {
            View buttonRoot = view.getRootView();

            Activity activity = Utils.getActivity();
            if (activity != null) {
                View activityRoot = activity.getWindow().getDecorView();
                return buttonRoot != activityRoot;
            }
        }
        return false;
    }

    /**
     * The label is the localized download text litho gave us, so no hard coded language is needed.
     */
    private static boolean isPlayerDownloadButton(@Nullable Map<Object, Object> map) {
        if (map == null || downloadButtonLabel.isEmpty()
                || !(map.get(ELEMENTS_SENDER_VIEW) instanceof ViewGroup senderViewGroup)) {
            return false;
        }
        CharSequence description = senderViewGroup.getContentDescription();
        //noinspection SizeReplaceableByIsEmpty
        if (description == null || description.length() == 0) return false;

        String value = description.toString().toLowerCase(Locale.ROOT);
        String label = downloadButtonLabel.toLowerCase(Locale.ROOT);
        if (!value.contains(label) && !label.contains(value)) return false;

        return isViewInsideActionBar(senderViewGroup);
    }

    private static boolean isViewInsideActionBar(View view) {
        int actionBarId = ResourceUtils.getIdIdentifier("video_action_bar");
        while (view.getParent() instanceof View parent) {
            if (parent.getId() == actionBarId) return true;
            view = parent;
        }
        return false;
    }
}

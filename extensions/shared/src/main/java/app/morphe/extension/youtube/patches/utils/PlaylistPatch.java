package app.morphe.extension.youtube.patches.utils;

import static app.morphe.extension.shared.utils.StringRef.str;
import static app.morphe.extension.shared.utils.Utils.runOnMainThreadDelayed;
import static app.morphe.extension.youtube.shared.RootView.getContext;
import static app.morphe.extension.youtube.utils.VideoUtils.launchVideoExternalDownloader;
import static app.morphe.extension.youtube.utils.VideoUtils.openPlaylist;
import static app.morphe.extension.youtube.utils.VideoUtils.reloadVideo;

import android.content.Context;
import android.view.KeyEvent;
import android.widget.LinearLayout;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

import app.morphe.extension.shared.innertube.utils.AuthUtils;
import app.morphe.extension.shared.ui.CustomDialog;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.ResourceUtils;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.patches.utils.requests.CreatePlaylistRequest;
import app.morphe.extension.youtube.patches.utils.requests.DeletePlaylistRequest;
import app.morphe.extension.youtube.patches.utils.requests.EditPlaylistRequest;
import app.morphe.extension.youtube.patches.utils.requests.GetPlaylistsRequest;
import app.morphe.extension.youtube.patches.utils.requests.SavePlaylistRequest;
import app.morphe.extension.youtube.patches.utils.requests.VideoDetailsRequest;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.PlayerType;
import app.morphe.extension.youtube.shared.VideoInformation;
import app.morphe.extension.youtube.utils.ExtendedUtils;
import app.morphe.extension.youtube.utils.GeminiManager;
import kotlin.Pair;

// TODO: Implement sync queue and clean up code.
@SuppressWarnings({"unused", "StaticFieldLeak"})
public class PlaylistPatch {
    private static final boolean QUEUE_MANAGER =
            Settings.OVERLAY_BUTTON_EXTERNAL_DOWNLOADER_QUEUE_MANAGER.get()
                    || Settings.OVERRIDE_VIDEO_DOWNLOAD_BUTTON_QUEUE_MANAGER.get();
    private static final long DELAY_MILLISECONDS = 1500L;

    private static volatile String playlistId = "";
    private static volatile String videoId = "";

    private static String checkFailedAuth;
    private static String checkFailedPlaylistId;
    private static String checkFailedQueue;
    private static String checkFailedVideoId;
    private static String checkFailedGeneric;

    private static String fetchFailedAdd;
    private static String fetchFailedCreate;
    private static String fetchFailedDelete;
    private static String fetchFailedRemove;
    private static String fetchFailedSave;
    private static String fetchFailedVideoIndormation;

    private static String fetchSucceededAdd;
    private static String fetchSucceededCreate;
    private static String fetchSucceededDelete;
    private static String fetchSucceededRemove;
    private static String fetchSucceededSave;

    static {
        Context mContext = Utils.getContext();
        if (mContext != null && mContext.getResources() != null) {
            checkFailedAuth = str("revanced_queue_manager_check_failed_auth");
            checkFailedPlaylistId = str("revanced_queue_manager_check_failed_playlist_id");
            checkFailedQueue = str("revanced_queue_manager_check_failed_queue");
            checkFailedVideoId = str("revanced_queue_manager_check_failed_video_id");
            checkFailedGeneric = str("revanced_queue_manager_check_failed_generic");

            fetchFailedAdd = str("revanced_queue_manager_fetch_failed_add");
            fetchFailedCreate = str("revanced_queue_manager_fetch_failed_create");
            fetchFailedDelete = str("revanced_queue_manager_fetch_failed_delete");
            fetchFailedRemove = str("revanced_queue_manager_fetch_failed_remove");
            fetchFailedSave = str("revanced_queue_manager_fetch_failed_save");
            fetchFailedVideoIndormation =
                    str("revanced_queue_manager_fetch_failed_video_information");

            fetchSucceededAdd = str("revanced_queue_manager_fetch_succeeded_add");
            fetchSucceededCreate = str("revanced_queue_manager_fetch_succeeded_create");
            fetchSucceededDelete = str("revanced_queue_manager_fetch_succeeded_delete");
            fetchSucceededRemove = str("revanced_queue_manager_fetch_succeeded_remove");
            fetchSucceededSave = str("revanced_queue_manager_fetch_succeeded_save");
        }
    }

    @GuardedBy("itself")
    private static final BidiMap<String, String> lastVideoIds = new DualHashBidiMap<>();

    /**
     * Injection point.
     */
    public static boolean onKeyLongPress(int keyCode) {
        if (!QUEUE_MANAGER || keyCode != KeyEvent.KEYCODE_BACK) {
            return false;
        }
        if (getContext() == null) {
            handleCheckError(checkFailedQueue);
            return false;
        }
        prepareDialogBuilder("");
        return true;
    }

    /**
     * Injection point.
     */
    public static void removeFromQueue(@Nullable String setVideoId) {
        if (StringUtils.isNotEmpty(setVideoId)) {
            synchronized (lastVideoIds) {
                String videoId = lastVideoIds.inverseBidiMap().get(setVideoId);
                if (videoId != null) {
                    lastVideoIds.remove(videoId, setVideoId);
                    EditPlaylistRequest.clearVideoId(videoId);
                    Logger.printDebug(() -> "Video removed by YouTube flyout menu: " + videoId);
                }
            }
        }
    }

    /**
     * Invoked by extension.
     */
    public static void prepareDialogBuilder(@NonNull String currentVideoId) {
        if (AuthUtils.isNotLoggedIn()) {
            handleCheckError(checkFailedAuth);
            return;
        }
        if (currentVideoId.isEmpty()) {
            buildBottomSheetDialog(QueueManager.noVideoIdQueueEntries);
        } else {
            videoId = currentVideoId;
            synchronized (lastVideoIds) {
                QueueManager[] customActionsEntries;
                boolean canReload = PlayerType.getCurrent().isMaximizedOrFullscreen() &&
                        lastVideoIds.get(VideoInformation.getVideoId()) != null;
                if (playlistId.isEmpty() || lastVideoIds.get(currentVideoId) == null) {
                    if (canReload) {
                        customActionsEntries = QueueManager.addToQueueWithReloadEntries;
                    } else {
                        customActionsEntries = QueueManager.addToQueueEntries;
                    }
                } else {
                    if (canReload) {
                        customActionsEntries = QueueManager.removeFromQueueWithReloadEntries;
                    } else {
                        customActionsEntries = QueueManager.removeFromQueueEntries;
                    }
                }

                buildBottomSheetDialog(customActionsEntries);
            }
        }
    }

    private static void buildBottomSheetDialog(QueueManager[] queueManagerEntries) {
        Context mContext = getContext();
        LinearLayout mainLayout = ExtendedUtils.prepareMainLayout(mContext);
        Map<LinearLayout, Runnable> actionsMap = new LinkedHashMap<>(queueManagerEntries.length);

        for (QueueManager queueManager : queueManagerEntries) {
            String title = queueManager.label;
            int iconId = queueManager.drawableId;
            Runnable action = queueManager.onClickAction;
            LinearLayout itemLayout = ExtendedUtils.createItemLayout(mContext, title, iconId);
            actionsMap.putIfAbsent(itemLayout, action);
            mainLayout.addView(itemLayout);
        }

        ExtendedUtils.showBottomSheetDialog(mContext, mainLayout, actionsMap);
    }

    private static void fetchQueue(boolean remove, boolean openPlaylist,
                                   boolean openVideo, boolean reload) {
        try {
            String currentPlaylistId = playlistId;
            String currentVideoId = videoId;
            synchronized (lastVideoIds) {
                if (currentPlaylistId.isEmpty()) { // Queue is empty, create new playlist.
                    CreatePlaylistRequest.fetchRequestIfNeeded(currentVideoId, AuthUtils.getRequestHeader());
                    runOnMainThreadDelayed(() -> {
                        CreatePlaylistRequest request = CreatePlaylistRequest.getRequestForVideoId(currentVideoId);
                        if (request != null) {
                            Pair<String, String> playlistIds = request.getPlaylistId();
                            if (playlistIds != null) {
                                String createdPlaylistId = playlistIds.getFirst();
                                String setVideoId = playlistIds.getSecond();
                                if (createdPlaylistId != null && setVideoId != null) {
                                    playlistId = createdPlaylistId;
                                    lastVideoIds.putIfAbsent(currentVideoId, setVideoId);
                                    showToast(fetchSucceededCreate);
                                    Logger.printDebug(() -> "Queue successfully created, playlistId: " + createdPlaylistId + ", setVideoId: " + setVideoId);
                                    if (openPlaylist) {
                                        openQueue(currentVideoId, openVideo, reload);
                                    }
                                    return;
                                }
                            }
                        }
                        showToast(fetchFailedCreate);
                    }, DELAY_MILLISECONDS);
                } else { // Queue is not empty, add or remove video.
                    String setVideoId = lastVideoIds.get(currentVideoId);
                    EditPlaylistRequest.fetchRequestIfNeeded(currentVideoId, currentPlaylistId, setVideoId, AuthUtils.getRequestHeader());

                    runOnMainThreadDelayed(() -> {
                        EditPlaylistRequest request = EditPlaylistRequest.getRequestForVideoId(currentVideoId);
                        if (request != null) {
                            String fetchedSetVideoId = request.getResult();
                            Logger.printDebug(() -> "fetchedSetVideoId: " + fetchedSetVideoId);
                            if (remove) { // Remove from queue.
                                if ("".equals(fetchedSetVideoId)) {
                                    lastVideoIds.remove(currentVideoId, setVideoId);
                                    EditPlaylistRequest.clearVideoId(currentVideoId);
                                    showToast(fetchSucceededRemove);
                                    if (openPlaylist) {
                                        openQueue(currentVideoId, openVideo, reload);
                                    }
                                    return;
                                }
                                showToast(fetchFailedRemove);
                            } else { // Add to queue.
                                if (fetchedSetVideoId != null && !fetchedSetVideoId.isEmpty()) {
                                    lastVideoIds.putIfAbsent(currentVideoId, fetchedSetVideoId);
                                    EditPlaylistRequest.clearVideoId(currentVideoId);
                                    showToast(fetchSucceededAdd);
                                    Logger.printDebug(() -> "Video successfully added, setVideoId: " + fetchedSetVideoId);
                                    if (openPlaylist) {
                                        openQueue(currentVideoId, openVideo, reload);
                                    }
                                    return;
                                }
                                showToast(fetchFailedAdd);
                            }
                        }
                    }, DELAY_MILLISECONDS);
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "fetchQueue failure", ex);
        }
    }

    private static void summarizeVideo() {
        Context mContext = getContext();
        if (mContext == null) {
            handleCheckError(checkFailedQueue);
            return;
        }
        String currentVideoId = videoId;
        if (StringUtils.isEmpty(currentVideoId)) {
            handleCheckError(checkFailedVideoId);
            return;
        }

        String videoUrl = "https://www.youtube.com/watch?v=" + currentVideoId;
        GeminiManager.getInstance().startSummarization(mContext, videoUrl);
    }

    private static void fetchVideoDetails() {
        try {
            String currentVideoId = videoId;
            VideoDetailsRequest.fetchRequestIfNeeded(currentVideoId, AuthUtils.getRequestHeader());
            runOnMainThreadDelayed(() -> {
                VideoDetailsRequest request = VideoDetailsRequest.getRequestForVideoId(currentVideoId);
                if (request != null) {
                    String message = request.getMessage();
                    if (message != null) {
                        Context mContext = getContext();
                        CustomDialog.create(
                                // context
                                mContext,
                                // title
                                str("revanced_queue_manager_video_information"),
                                // message
                                message,
                                // editText
                                null,
                                // okButtonText
                                null,
                                // onOkClick
                                () -> {
                                },
                                // onCancelClick
                                null,
                                // neutralButtonText
                                str("revanced_queue_manager_video_information_copy"),
                                // onNeutralClick
                                () -> Utils.setClipboard(
                                        message,
                                        str("revanced_share_copy_video_information_success")
                                ),
                                // dismissDialogOnNeutralClick
                                true
                        ).first.show();
                        return;
                    }
                }
                showToast(fetchFailedVideoIndormation);
            }, DELAY_MILLISECONDS);
        } catch (Exception ex) {
            Logger.printException(() -> "fetchVideoDetails failure", ex);
        }
    }

    private static void saveToPlaylist() {
        String currentPlaylistId = playlistId;
        if (currentPlaylistId.isEmpty()) {
            handleCheckError(checkFailedQueue);
            return;
        }
        try {
            GetPlaylistsRequest.fetchRequestIfNeeded(currentPlaylistId, AuthUtils.getRequestHeader());
            runOnMainThreadDelayed(() -> {
                GetPlaylistsRequest request = GetPlaylistsRequest.getRequestForPlaylistId(currentPlaylistId);
                if (request != null) {
                    Context mContext = getContext();
                    Pair<String, String>[] playlists = request.getPlaylists();
                    if (playlists != null) {
                        LinearLayout mainLayout = ExtendedUtils.prepareMainLayout(mContext);
                        Map<LinearLayout, Runnable> actionsMap = new LinkedHashMap<>(playlists.length);
                        int libraryIconId = QueueManager.SAVE_QUEUE.drawableId;

                        for (Pair<String, String> playlist : playlists) {
                            String playlistId = playlist.getFirst();
                            String title = playlist.getSecond();
                            Runnable action = () -> saveToPlaylist(playlistId, title);
                            LinearLayout itemLayout = ExtendedUtils.createItemLayout(mContext, title, libraryIconId);
                            actionsMap.putIfAbsent(itemLayout, action);
                            mainLayout.addView(itemLayout);
                        }

                        ExtendedUtils.showBottomSheetDialog(mContext, mainLayout, actionsMap);
                        GetPlaylistsRequest.clear();
                    }
                }
            }, DELAY_MILLISECONDS);
        } catch (Exception ex) {
            Logger.printException(() -> "saveToPlaylist failure", ex);
        }
    }

    private static void saveToPlaylist(@Nullable String libraryId, @Nullable String libraryTitle) {
        try {
            if (StringUtils.isEmpty(libraryId)) {
                handleCheckError(checkFailedPlaylistId);
                return;
            }
            SavePlaylistRequest.fetchRequestIfNeeded(playlistId, libraryId, AuthUtils.getRequestHeader());

            runOnMainThreadDelayed(() -> {
                SavePlaylistRequest request = SavePlaylistRequest.getRequestForLibraryId(libraryId);
                if (request != null) {
                    Boolean result = request.getResult();
                    if (BooleanUtils.isTrue(result)) {
                        showToast(String.format(fetchSucceededSave, libraryTitle));
                        SavePlaylistRequest.clear();
                        return;
                    }
                    showToast(fetchFailedSave);
                }
            }, DELAY_MILLISECONDS);
        } catch (Exception ex) {
            Logger.printException(() -> "saveToPlaylist failure", ex);
        }
    }

    private static void removeQueue() {
        String currentPlaylistId = playlistId;
        if (currentPlaylistId.isEmpty()) {
            handleCheckError(checkFailedQueue);
            return;
        }
        try {
            DeletePlaylistRequest.fetchRequestIfNeeded(currentPlaylistId, AuthUtils.getRequestHeader());
            runOnMainThreadDelayed(() -> {
                DeletePlaylistRequest request = DeletePlaylistRequest.getRequestForPlaylistId(currentPlaylistId);
                if (request != null) {
                    Boolean result = request.getResult();
                    if (BooleanUtils.isTrue(result)) {
                        playlistId = "";
                        synchronized (lastVideoIds) {
                            lastVideoIds.clear();
                        }
                        CreatePlaylistRequest.clear();
                        DeletePlaylistRequest.clear();
                        EditPlaylistRequest.clear();
                        GetPlaylistsRequest.clear();
                        SavePlaylistRequest.clear();
                        showToast(fetchSucceededDelete);
                        return;
                    }
                }
                showToast(fetchFailedDelete);
            }, DELAY_MILLISECONDS);
        } catch (Exception ex) {
            Logger.printException(() -> "removeQueue failure", ex);
        }
    }

    private static void downloadVideo() {
        String currentVideoId = videoId;
        launchVideoExternalDownloader(currentVideoId);
    }

    private static void openQueue() {
        openQueue("", false, false);
    }

    private static void openQueue(String currentVideoId, boolean openVideo, boolean reload) {
        String currentPlaylistId = playlistId;
        if (currentPlaylistId.isEmpty()) {
            handleCheckError(checkFailedQueue);
            return;
        }
        if (openVideo) {
            if (StringUtils.isEmpty(currentVideoId)) {
                handleCheckError(checkFailedVideoId);
                return;
            }
            // Open a video from a playlist
            if (reload) {
                // Since the Queue is not automatically synced, a 'reload' action has been added as a workaround.
                // The 'reload' action simply closes the video and reopens it.
                // It is important to close the video, otherwise the Queue will not be updated.
                reloadVideo(VideoInformation.getVideoId(), currentPlaylistId);
            } else {
                openPlaylist(currentPlaylistId, currentVideoId);
            }
        } else {
            // Open a playlist
            openPlaylist(currentPlaylistId);
        }
    }

    private static void handleCheckError(String reason) {
        showToast(String.format(checkFailedGeneric, reason));
    }

    private static void showToast(String reason) {
        Utils.showToastShort(reason);
    }

    private enum QueueManager {
        ADD_TO_QUEUE(
                "revanced_queue_manager_add_to_queue",
                "yt_outline_list_add_black_24",
                () -> fetchQueue(false, false, false, false)
        ),
        ADD_TO_QUEUE_AND_OPEN_QUEUE(
                "revanced_queue_manager_add_to_queue_and_open_queue",
                "yt_outline_list_add_black_24",
                () -> fetchQueue(false, true, false, false)
        ),
        ADD_TO_QUEUE_AND_PLAY_VIDEO(
                "revanced_queue_manager_add_to_queue_and_play_video",
                "yt_outline_list_play_arrow_black_24",
                () -> fetchQueue(false, true, true, false)
        ),
        ADD_TO_QUEUE_AND_RELOAD_VIDEO(
                "revanced_queue_manager_add_to_queue_and_reload_video",
                "yt_outline_arrow_circle_black_24",
                () -> fetchQueue(false, true, true, true)
        ),
        REMOVE_FROM_QUEUE(
                "revanced_queue_manager_remove_from_queue",
                "yt_outline_trash_can_black_24",
                () -> fetchQueue(true, false, false, false)
        ),
        REMOVE_FROM_QUEUE_AND_OPEN_QUEUE(
                "revanced_queue_manager_remove_from_queue_and_open_queue",
                "yt_outline_trash_can_black_24",
                () -> fetchQueue(true, true, false, false)
        ),
        REMOVE_FROM_QUEUE_AND_RELOAD_VIDEO(
                "revanced_queue_manager_remove_from_queue_and_reload_video",
                "yt_outline_arrow_circle_black_24",
                () -> fetchQueue(true, true, true, true)
        ),
        OPEN_QUEUE(
                "revanced_queue_manager_open_queue",
                "yt_outline_list_view_black_24",
                PlaylistPatch::openQueue
        ),
        // For some reason, the 'playlist/delete' endpoint is unavailable.
        REMOVE_QUEUE(
                "revanced_queue_manager_remove_queue",
                "yt_outline_slash_circle_left_black_24",
                PlaylistPatch::removeQueue
        ),
        SAVE_QUEUE(
                "revanced_queue_manager_save_queue",
                "yt_outline_bookmark_black_24",
                PlaylistPatch::saveToPlaylist
        ),
        SUMMARIZE_VIDEO(
                "revanced_overlay_button_gemini_summarize",
                "revanced_gemini_button",
                PlaylistPatch::summarizeVideo
        ),
        SHOW_ORIGINAL_VIDEO_INFORMATION(
                "revanced_queue_manager_show_original_video_information",
                "quantum_gm_ic_g_translate_black_24",
                PlaylistPatch::fetchVideoDetails
        ),
        EXTERNAL_DOWNLOADER(
                "revanced_queue_manager_external_downloader",
                "yt_outline_download_black_24",
                PlaylistPatch::downloadVideo
        );

        public final int drawableId;

        @NonNull
        public final String label;

        @NonNull
        public final Runnable onClickAction;

        QueueManager(@NonNull String label, @NonNull String icon, @NonNull Runnable onClickAction) {
            this.drawableId = ResourceUtils.getDrawableIdentifier(icon);
            this.label = ResourceUtils.getString(label);
            this.onClickAction = onClickAction;
        }

        public static final QueueManager[] addToQueueEntries = {
                ADD_TO_QUEUE,
                ADD_TO_QUEUE_AND_OPEN_QUEUE,
                ADD_TO_QUEUE_AND_PLAY_VIDEO,
                OPEN_QUEUE,
                //REMOVE_QUEUE,
                EXTERNAL_DOWNLOADER,
                SAVE_QUEUE,
                SUMMARIZE_VIDEO,
                SHOW_ORIGINAL_VIDEO_INFORMATION,
        };

        public static final QueueManager[] addToQueueWithReloadEntries = {
                ADD_TO_QUEUE,
                ADD_TO_QUEUE_AND_OPEN_QUEUE,
                ADD_TO_QUEUE_AND_PLAY_VIDEO,
                ADD_TO_QUEUE_AND_RELOAD_VIDEO,
                OPEN_QUEUE,
                //REMOVE_QUEUE,
                EXTERNAL_DOWNLOADER,
                SAVE_QUEUE,
                SUMMARIZE_VIDEO,
                SHOW_ORIGINAL_VIDEO_INFORMATION,
        };

        public static final QueueManager[] removeFromQueueEntries = {
                REMOVE_FROM_QUEUE,
                REMOVE_FROM_QUEUE_AND_OPEN_QUEUE,
                OPEN_QUEUE,
                //REMOVE_QUEUE,
                EXTERNAL_DOWNLOADER,
                SAVE_QUEUE,
                SUMMARIZE_VIDEO,
                SHOW_ORIGINAL_VIDEO_INFORMATION,
        };

        public static final QueueManager[] removeFromQueueWithReloadEntries = {
                REMOVE_FROM_QUEUE,
                REMOVE_FROM_QUEUE_AND_OPEN_QUEUE,
                REMOVE_FROM_QUEUE_AND_RELOAD_VIDEO,
                OPEN_QUEUE,
                //REMOVE_QUEUE,
                EXTERNAL_DOWNLOADER,
                SAVE_QUEUE,
                SUMMARIZE_VIDEO,
                SHOW_ORIGINAL_VIDEO_INFORMATION,
        };

        public static final QueueManager[] noVideoIdQueueEntries = {
                OPEN_QUEUE,
                //REMOVE_QUEUE,
                SAVE_QUEUE,
        };
    }
}

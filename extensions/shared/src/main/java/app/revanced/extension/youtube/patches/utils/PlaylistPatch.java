package app.revanced.extension.youtube.patches.utils;

import static app.revanced.extension.shared.utils.StringRef.str;

import android.content.Context;
import android.view.KeyEvent;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;

import com.google.android.libraries.youtube.rendering.ui.pivotbar.PivotBar;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.ResourceUtils;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.patches.utils.requests.CreatePlaylistRequest;
import app.revanced.extension.youtube.patches.utils.requests.DeletePlaylistRequest;
import app.revanced.extension.youtube.patches.utils.requests.EditPlaylistRequest;
import app.revanced.extension.youtube.patches.utils.requests.GetPlaylistsRequest;
import app.revanced.extension.youtube.patches.utils.requests.SavePlaylistRequest;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.utils.ExtendedUtils;
import app.revanced.extension.youtube.utils.VideoUtils;
import kotlin.Pair;

// TODO: Implement sync queue and clean up code.
@SuppressWarnings({"unused", "StaticFieldLeak"})
public class PlaylistPatch extends VideoUtils {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String[] REQUEST_HEADER_KEYS = {
            AUTHORIZATION_HEADER,
            "X-GOOG-API-FORMAT-VERSION",
            "X-Goog-Visitor-Id"
    };
    private static final boolean QUEUE_MANAGER =
            Settings.OVERLAY_BUTTON_EXTERNAL_DOWNLOADER_QUEUE_MANAGER.get()
                    || Settings.OVERRIDE_VIDEO_DOWNLOAD_BUTTON_QUEUE_MANAGER.get();

    private static Context mContext;
    private static volatile String authorization = "";
    public static volatile String dataSyncId = "";
    public static volatile boolean isIncognito = false;
    private static volatile Map<String, String> requestHeader;
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
        if (mContext == null) {
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
                }
            }
        }
    }

    /**
     * Injection point.
     */
    public static void setPivotBar(PivotBar view) {
        if (QUEUE_MANAGER) {
            mContext = view.getContext();
        }
    }

    /**
     * Injection point.
     */
    public static void setRequestHeaders(String url, Map<String, String> requestHeaders) {
        if (QUEUE_MANAGER) {
            try {
                // Save requestHeaders whenever an account is switched.
                String auth = requestHeaders.get(AUTHORIZATION_HEADER);
                if (auth == null || authorization.equals(auth)) {
                    return;
                }
                for (String key : REQUEST_HEADER_KEYS) {
                    if (requestHeaders.get(key) == null) {
                        return;
                    }
                }
                authorization = auth;
                requestHeader = requestHeaders;
            } catch (Exception ex) {
                Logger.printException(() -> "setRequestHeaders failure", ex);
            }
        }
    }

    /**
     * Invoked by extension.
     */
    public static void setContext(Context context) {
        mContext = context;
    }

    /**
     * Invoked by extension.
     */
    public static void prepareDialogBuilder(@NonNull String currentVideoId) {
        if (authorization.isEmpty() || (dataSyncId.isEmpty() && isIncognito)) {
            handleCheckError(checkFailedAuth);
            return;
        }
        if (currentVideoId.isEmpty()) {
            buildBottomSheetDialog(QueueManager.noVideoIdQueueEntries);
        } else {
            videoId = currentVideoId;
            synchronized (lastVideoIds) {
                QueueManager[] customActionsEntries = playlistId.isEmpty() || lastVideoIds.get(currentVideoId) == null
                        ? QueueManager.addToQueueEntries
                        : QueueManager.removeFromQueueEntries;

                buildBottomSheetDialog(customActionsEntries);
            }
        }
    }

    private static void buildBottomSheetDialog(QueueManager[] queueManagerEntries) {
        ScrollView mScrollView = new ScrollView(mContext);
        LinearLayout mLinearLayout = new LinearLayout(mContext);
        mLinearLayout.setOrientation(LinearLayout.VERTICAL);
        mLinearLayout.setPadding(0, 0, 0, 0);

        Map<LinearLayout, Runnable> actionsMap = new LinkedHashMap<>(queueManagerEntries.length);

        for (QueueManager queueManager : queueManagerEntries) {
            String title = queueManager.label;
            int iconId = queueManager.drawableId;
            Runnable action = queueManager.onClickAction;
            LinearLayout itemLayout = ExtendedUtils.createItemLayout(mContext, title, iconId);
            actionsMap.putIfAbsent(itemLayout, action);
            mLinearLayout.addView(itemLayout);
        }

        mScrollView.addView(mLinearLayout);

        ExtendedUtils.showBottomSheetDialog(mContext, mScrollView, actionsMap);
    }

    private static void fetchQueue(boolean remove, boolean openPlaylist, boolean openVideo) {
        try {
            String currentPlaylistId = playlistId;
            String currentVideoId = videoId;
            synchronized (lastVideoIds) {
                if (currentPlaylistId.isEmpty()) { // Queue is empty, create new playlist.
                    CreatePlaylistRequest.fetchRequestIfNeeded(currentVideoId, requestHeader, dataSyncId);
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
                                        openQueue(currentVideoId, openVideo);
                                    }
                                    return;
                                }
                            }
                        }
                        showToast(fetchFailedCreate);
                    }, 1000);
                } else { // Queue is not empty, add or remove video.
                    String setVideoId = lastVideoIds.get(currentVideoId);
                    EditPlaylistRequest.fetchRequestIfNeeded(currentVideoId, currentPlaylistId, setVideoId, requestHeader, dataSyncId);

                    runOnMainThreadDelayed(() -> {
                        EditPlaylistRequest request = EditPlaylistRequest.getRequestForVideoId(currentVideoId);
                        if (request != null) {
                            String fetchedSetVideoId = request.getResult();
                            Logger.printDebug(() -> "fetchedSetVideoId: " + fetchedSetVideoId);
                            if (remove) { // Remove from queue.
                                if (StringUtils.isEmpty(fetchedSetVideoId)) {
                                    lastVideoIds.remove(currentVideoId, setVideoId);
                                    showToast(fetchSucceededRemove);
                                    if (openPlaylist) {
                                        openQueue(currentVideoId, openVideo);
                                    }
                                    return;
                                }
                                showToast(fetchFailedRemove);
                            } else { // Add to queue.
                                if (StringUtils.isNotEmpty(fetchedSetVideoId)) {
                                    lastVideoIds.putIfAbsent(currentVideoId, fetchedSetVideoId);
                                    showToast(fetchSucceededAdd);
                                    Logger.printDebug(() -> "Video successfully added, setVideoId: " + fetchedSetVideoId);
                                    if (openPlaylist) {
                                        openQueue(currentVideoId, openVideo);
                                    }
                                    return;
                                }
                                showToast(fetchFailedAdd);
                            }
                        }
                    }, 1000);
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "fetchQueue failure", ex);
        }
    }

    private static void saveToPlaylist() {
        String currentPlaylistId = playlistId;
        if (currentPlaylistId.isEmpty()) {
            handleCheckError(checkFailedQueue);
            return;
        }
        try {
            GetPlaylistsRequest.fetchRequestIfNeeded(currentPlaylistId, requestHeader, dataSyncId);
            runOnMainThreadDelayed(() -> {
                GetPlaylistsRequest request = GetPlaylistsRequest.getRequestForPlaylistId(currentPlaylistId);
                if (request != null) {
                    Pair<String, String>[] playlists = request.getPlaylists();
                    if (playlists != null) {
                        ScrollView mScrollView = new ScrollView(mContext);
                        LinearLayout mLinearLayout = new LinearLayout(mContext);
                        mLinearLayout.setOrientation(LinearLayout.VERTICAL);
                        mLinearLayout.setPadding(0, 0, 0, 0);

                        Map<LinearLayout, Runnable> actionsMap = new LinkedHashMap<>(playlists.length);

                        int libraryIconId = QueueManager.SAVE_QUEUE.drawableId;

                        for (Pair<String, String> playlist : playlists) {
                            String playlistId = playlist.getFirst();
                            String title = playlist.getSecond();
                            Runnable action = () -> saveToPlaylist(playlistId, title);
                            LinearLayout itemLayout = ExtendedUtils.createItemLayout(mContext, title, libraryIconId);
                            actionsMap.putIfAbsent(itemLayout, action);
                            mLinearLayout.addView(itemLayout);
                        }

                        mScrollView.addView(mLinearLayout);

                        ExtendedUtils.showBottomSheetDialog(mContext, mScrollView, actionsMap);
                        GetPlaylistsRequest.clear();
                    }
                }
            }, 1000);
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
            SavePlaylistRequest.fetchRequestIfNeeded(playlistId, libraryId, requestHeader, dataSyncId);

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
            }, 1000);
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
            DeletePlaylistRequest.fetchRequestIfNeeded(currentPlaylistId, requestHeader, dataSyncId);
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
            }, 1000);
        } catch (Exception ex) {
            Logger.printException(() -> "removeQueue failure", ex);
        }
    }

    private static void downloadVideo() {
        String currentVideoId = videoId;
        launchVideoExternalDownloader(currentVideoId);
    }

    private static void openQueue() {
        openQueue("", false);
    }

    private static void openQueue(String currentVideoId, boolean openVideo) {
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
            openPlaylist(currentPlaylistId, currentVideoId);
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
                () -> fetchQueue(false, false, false)
        ),
        ADD_TO_QUEUE_AND_OPEN_QUEUE(
                "revanced_queue_manager_add_to_queue_and_open_queue",
                "yt_outline_list_add_black_24",
                () -> fetchQueue(false, true, false)
        ),
        ADD_TO_QUEUE_AND_PLAY_VIDEO(
                "revanced_queue_manager_add_to_queue_and_play_video",
                "yt_outline_list_play_arrow_black_24",
                () -> fetchQueue(false, true, true)
        ),
        REMOVE_FROM_QUEUE(
                "revanced_queue_manager_remove_from_queue",
                "yt_outline_trash_can_black_24",
                () -> fetchQueue(true, false, false)
        ),
        REMOVE_FROM_QUEUE_AND_OPEN_QUEUE(
                "revanced_queue_manager_remove_from_queue_and_open_queue",
                "yt_outline_trash_can_black_24",
                () -> fetchQueue(true, true, false)
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
        };

        public static final QueueManager[] removeFromQueueEntries = {
                REMOVE_FROM_QUEUE,
                REMOVE_FROM_QUEUE_AND_OPEN_QUEUE,
                OPEN_QUEUE,
                //REMOVE_QUEUE,
                EXTERNAL_DOWNLOADER,
                SAVE_QUEUE,
        };

        public static final QueueManager[] noVideoIdQueueEntries = {
                OPEN_QUEUE,
                //REMOVE_QUEUE,
                SAVE_QUEUE,
        };
    }
}

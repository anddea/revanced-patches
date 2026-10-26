/*
 * Copyright (C) 2026 lavinhoque33
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Licensed under the GNU General Public License v3.0.
 * Written by lavinhoque33, 2026-10-10.
 */

package app.morphe.extension.youtube.patches.general.offline;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.service.notification.StatusBarNotification;

import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

/**
 * Injection points of the two stock foreground services that the offline feature borrows.
 * <p>
 * Patched root installs cannot register new manifest components, so offline playback runs in
 * YouTube's {@code BackgroundPlayerService} (foregroundServiceType mediaPlayback) and downloads
 * in {@code OfflineKeepAliveService} (foregroundServiceType dataSync, only used by Premium).
 * <p>
 * Both services stay usable by YouTube itself: an instance that YouTube created, bound or
 * started is marked as attached, and then YouTube's own lifecycle code keeps running. Only an
 * instance that exists purely for the offline feature skips YouTube's code.
 */
@SuppressWarnings("unused")
public final class OfflineServiceHooks {
    static final String BACKGROUND_SERVICE =
            "com.google.android.libraries.youtube.player.background.service.BackgroundPlayerService";
    static final String KEEP_ALIVE_SERVICE =
            "com.google.android.libraries.youtube.offline.transfer.service.OfflineKeepAliveService";

    private static final String ACTION_PREFIX = "app.morphe.youtube.offline.";
    static final String ACTION_PLAYBACK_START = ACTION_PREFIX + "PLAYBACK_START";
    static final String ACTION_PLAYBACK_TOGGLE = ACTION_PREFIX + "PLAYBACK_TOGGLE";
    static final String ACTION_PLAYBACK_NEXT = ACTION_PREFIX + "PLAYBACK_NEXT";
    static final String ACTION_PLAYBACK_PREVIOUS = ACTION_PREFIX + "PLAYBACK_PREVIOUS";
    static final String ACTION_PLAYBACK_CLOSE = ACTION_PREFIX + "PLAYBACK_CLOSE";
    static final String ACTION_DOWNLOAD_START = ACTION_PREFIX + "DOWNLOAD_START";
    static final String ACTION_DOWNLOAD_CANCEL = ACTION_PREFIX + "DOWNLOAD_CANCEL";

    private static final class ServiceState {
        final String name;
        final int foregroundType;
        /** Set right before this feature starts the service, so onCreate knows who it is for. */
        volatile boolean claimed;
        /** YouTube created, bound or started the current instance. */
        volatile boolean youTubeAttached;
        volatile boolean foreground;
        volatile int lastStartId;
        WeakReference<Service> service = new WeakReference<>(null);

        ServiceState(String name, int foregroundType) {
            this.name = name;
            this.foregroundType = foregroundType;
        }
    }

    private static final ServiceState playback = new ServiceState("BackgroundPlayerService",
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK : 0);
    private static final ServiceState keepAlive = new ServiceState("OfflineKeepAliveService",
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC : 0);

    private OfflineServiceHooks() {
    }

    private static boolean isOurs(@Nullable Intent intent) {
        String action = intent == null ? null : intent.getAction();
        return action != null && action.startsWith(ACTION_PREFIX);
    }

    // region shared lifecycle

    private static boolean onCreate(ServiceState state, Service service) {
        state.service = new WeakReference<>(service);
        final boolean claimed = state.claimed;
        state.youTubeAttached = !claimed;
        Logger.printInfo(() -> state.name + " created " + (claimed ? "for offline use" : "by YouTube"));
        return claimed;
    }

    private static void markAttached(ServiceState state, String reason) {
        if (!state.youTubeAttached) {
            Logger.printInfo(() -> state.name + " attached by YouTube (" + reason + ")");
        }
        state.youTubeAttached = true;
    }

    /** @return If YouTube's own onDestroy code must be skipped. */
    private static boolean onDestroy(ServiceState state, Service service) {
        final boolean skipYouTube = !state.youTubeAttached;
        Logger.printInfo(() -> state.name + " destroyed, offline only: " + skipYouTube);
        state.service = new WeakReference<>(null);
        state.foreground = false;
        state.youTubeAttached = false;
        state.claimed = false;
        return skipYouTube;
    }

    private static void start(ServiceState state, String serviceClass, String action) {
        Context context = Utils.getContext();
        Intent intent = new Intent(action).setClassName(context, serviceClass);
        state.claimed = state.service.get() == null;
        try {
            context.startForegroundService(intent);
        } catch (Exception ex) {
            state.claimed = false;
            Logger.printException(() -> "Could not start " + state.name, ex);
        }
    }

    private static void startForeground(ServiceState state, Service service, int id, Notification notification) {
        try {
            if (state.foregroundType != 0) {
                service.startForeground(id, notification, state.foregroundType);
            } else {
                service.startForeground(id, notification);
            }
            state.foreground = true;
        } catch (Exception ex) {
            // Android 12+ refuses to promote a service from the background. The notification is
            // still shown, and the next update from the foreground promotes it again.
            Logger.printInfo(() -> "Could not promote " + state.name + " to the foreground", ex);
            OfflineNotifications.notify(service, id, notification);
        }
    }

    private static void stop(ServiceState state, int notificationId) {
        Service service = state.service.get();
        if (service != null) {
            try {
                if (state.youTubeAttached) {
                    // YouTube still uses the instance. Detach instead of removing, in case the
                    // foreground notification currently is YouTube's own.
                    service.stopForeground(Service.STOP_FOREGROUND_DETACH);
                } else {
                    service.stopForeground(Service.STOP_FOREGROUND_REMOVE);
                }
                // Only stops if no newer start request (for example YouTube's) arrived since ours.
                // A bound instance stays alive until YouTube unbinds.
                service.stopSelfResult(state.lastStartId);
            } catch (Exception ex) {
                Logger.printException(() -> "Could not stop " + state.name, ex);
            }
        }
        state.foreground = false;
        state.claimed = false;
        OfflineNotifications.cancel(Utils.getContext(), notificationId);
    }

    // endregion

    // region BackgroundPlayerService

    /**
     * Injection point. Called after the Hilt injection in onCreate.
     *
     * @return True to skip YouTube's own setup, which would register the instance with
     * YouTube's background-playback notification controller.
     */
    public static boolean onPlaybackServiceCreate(Service service) {
        return onCreate(playback, service);
    }

    /**
     * Injection point.
     *
     * @return True if the command was for offline playback and YouTube's code must not run.
     */
    public static boolean onPlaybackServiceStartCommand(Service service, @Nullable Intent intent, int startId) {
        if (!isOurs(intent)) {
            markAttached(playback, "start command");
            return false;
        }
        playback.service = new WeakReference<>(service);
        playback.claimed = false;
        playback.lastStartId = startId;
        final String action = intent.getAction();
        Logger.printDebug(() -> "Playback command: " + action);
        try {
            if (ACTION_PLAYBACK_START.equals(action)) {
                // startForeground must be called for every startForegroundService, even if the
                // session ended in the meantime, or the app is killed.
                Notification notification = OfflinePlayer.buildNotification(service);
                startForeground(playback, service, OfflineNotifications.PLAYBACK_ID,
                        notification != null ? notification : OfflineNotifications.buildDownloadProgress(service, null, 0));
                if (notification == null) stopPlaybackForeground();
            } else if (ACTION_PLAYBACK_TOGGLE.equals(action)) {
                OfflinePlayer.togglePlayPause();
            } else if (ACTION_PLAYBACK_NEXT.equals(action)) {
                OfflinePlayer.skip(1);
            } else if (ACTION_PLAYBACK_PREVIOUS.equals(action)) {
                OfflinePlayer.skip(-1);
            } else if (ACTION_PLAYBACK_CLOSE.equals(action)) {
                OfflinePlayer.close();
            }
        } catch (Exception ex) {
            Logger.printException(() -> "Playback command failed: " + action, ex);
        }
        return true;
    }

    /**
     * Injection point. YouTube binds the service for its own background playback.
     */
    public static void onPlaybackServiceBind(Service service) {
        playback.service = new WeakReference<>(service);
        markAttached(playback, "bind");
    }

    /**
     * Injection point.
     *
     * @return True to skip YouTube's own handling.
     */
    public static boolean onPlaybackServiceTaskRemoved(Service service) {
        if (OfflinePlayer.isSessionActive() && !OfflinePlayer.isPlaying()) {
            Logger.printInfo(() -> "Task removed while offline playback is paused, closing");
            OfflinePlayer.close();
        }
        return !playback.youTubeAttached;
    }

    /**
     * Injection point.
     *
     * @return True to skip YouTube's own teardown, because YouTube never set the instance up.
     */
    public static boolean onPlaybackServiceDestroy(Service service) {
        final boolean skipYouTube = onDestroy(playback, service);
        if (OfflinePlayer.isSessionActive()) {
            // YouTube stopped the shared instance (it stops every service it registered) while
            // offline playback still runs. Start it again; this succeeds while the app is visible.
            Utils.runOnMainThreadDelayed(() -> {
                if (OfflinePlayer.isSessionActive() && playback.service.get() == null) {
                    Logger.printInfo(() -> "Restarting the playback service for offline playback");
                    startPlaybackForeground();
                }
            }, 500);
        }
        return skipYouTube;
    }

    /** Must be called while the app is in the foreground (Android 12+ restriction). */
    static void startPlaybackForeground() {
        Service service = playback.service.get();
        if (service != null && playback.foreground) {
            updatePlaybackNotification();
            return;
        }
        start(playback, BACKGROUND_SERVICE, ACTION_PLAYBACK_START);
    }

    static void updatePlaybackNotification() {
        Service service = playback.service.get();
        if (service == null) return;
        Notification notification = OfflinePlayer.buildNotification(service);
        if (notification == null) return;
        // startForeground again rather than notify: it re-promotes the instance if YouTube demoted it.
        startForeground(playback, service, OfflineNotifications.PLAYBACK_ID, notification);
    }

    /**
     * On an instance that YouTube also uses, YouTube's notification controller removes the
     * foreground notification (stopForeground, or startForeground with its own id) when its own
     * player reacts to losing audio focus. Posts the offline notification again if it is gone.
     */
    static void ensurePlaybackNotification() {
        Service service = playback.service.get();
        if (service == null || !OfflinePlayer.isSessionActive()) return;
        NotificationManager manager = OfflineNotifications.manager(service);
        if (manager == null) return;
        try {
            for (StatusBarNotification active : manager.getActiveNotifications()) {
                if (active.getId() == OfflineNotifications.PLAYBACK_ID) return;
            }
        } catch (Exception ex) {
            Logger.printException(() -> "Could not read the active notifications", ex);
            return;
        }
        Logger.printInfo(() -> "Offline playback notification was removed, posting it again");
        updatePlaybackNotification();
    }

    static void stopPlaybackForeground() {
        stop(playback, OfflineNotifications.PLAYBACK_ID);
    }

    // endregion

    // region OfflineKeepAliveService

    /**
     * Injection point. Called before YouTube's startForegroundIfApplicable() in onCreate.
     *
     * @return True to skip YouTube's foreground notification.
     */
    public static boolean onKeepAliveServiceCreate(Service service) {
        return onCreate(keepAlive, service);
    }

    /**
     * Injection point.
     *
     * @return True if the command was for a download and YouTube's code must not run.
     */
    public static boolean onKeepAliveServiceStartCommand(Service service, @Nullable Intent intent, int startId) {
        if (!isOurs(intent)) {
            markAttached(keepAlive, "start command");
            return false;
        }
        keepAlive.service = new WeakReference<>(service);
        keepAlive.claimed = false;
        keepAlive.lastStartId = startId;
        final String action = intent.getAction();
        try {
            if (ACTION_DOWNLOAD_START.equals(action)) {
                startForeground(keepAlive, service, OfflineNotifications.DOWNLOAD_PROGRESS_ID,
                        OfflineDownloader.buildProgressNotification(service));
                if (!OfflineDownloader.hasWork()) stopDownloadForeground();
            } else if (ACTION_DOWNLOAD_CANCEL.equals(action)) {
                String videoId = intent.getStringExtra(OfflineScreens.EXTRA_VIDEO_ID);
                if (videoId != null) OfflineDownloader.cancel(videoId);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "Download command failed: " + action, ex);
        }
        return true;
    }

    /**
     * Injection point. Android 15 limits dataSync foreground services to 6 hours a day and calls
     * this when the limit is reached; the service must stop within seconds.
     *
     * @return True to skip YouTube's own handling.
     */
    public static boolean onKeepAliveServiceTimeout(Service service) {
        if (OfflineDownloader.hasWork()) {
            Logger.printInfo(() -> "Download foreground time limit reached, cancelling downloads");
            OfflineDownloader.cancelAll(true);
        }
        stopDownloadForeground();
        return !keepAlive.youTubeAttached;
    }

    /**
     * Injection point.
     *
     * @return True to skip YouTube's own teardown.
     */
    public static boolean onKeepAliveServiceDestroy(Service service) {
        return onDestroy(keepAlive, service);
    }

    /** Must be called while the app is in the foreground (Android 12+ restriction). */
    static void startDownloadForeground() {
        if (keepAlive.service.get() != null && keepAlive.foreground) {
            updateDownloadNotification();
            return;
        }
        start(keepAlive, KEEP_ALIVE_SERVICE, ACTION_DOWNLOAD_START);
    }

    static void updateDownloadNotification() {
        Service service = keepAlive.service.get();
        if (service == null || !keepAlive.foreground) return;
        OfflineNotifications.notify(service, OfflineNotifications.DOWNLOAD_PROGRESS_ID,
                OfflineDownloader.buildProgressNotification(service));
    }

    static void stopDownloadForeground() {
        stop(keepAlive, OfflineNotifications.DOWNLOAD_PROGRESS_ID);
    }

    // endregion
}

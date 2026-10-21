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

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.protobuf.MessageLite;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.WeakHashMap;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.settings.Settings;

/**
 * Hosts the offline library and player screens in the hijacked {@code LicenseActivity}, the
 * same activity the RVX settings use. The screen is chosen by the intent data string.
 */
@SuppressWarnings("unused")
public final class OfflineScreens {
    public static final String LIBRARY_INTENT = "rvx_offline_library_intent";
    public static final String PLAYER_INTENT = "rvx_offline_player_intent";
    static final String EXTRA_VIDEO_ID = "rvx_offline_video_id";

    private static final String LICENSE_ACTIVITY = "com.google.android.libraries.social.licenses.LicenseActivity";
    private static final String DOWNLOADS_BROWSE_ID = "FEdownloads";
    private static final byte[] DOWNLOADS_BROWSE_ID_BYTES = DOWNLOADS_BROWSE_ID.getBytes(StandardCharsets.UTF_8);

    // field numbers of the commands in YouTube's innertube
    // protocol. browseEndpoint carries the browse id as field 1; the Downloads page endpoint is the
    // command of the "Downloads" row in the You tab's library list.
    private static final int BROWSE_ENDPOINT_FIELD = 48687626;
    private static final int DOWNLOADS_PAGE_ENDPOINT_FIELD = 167315449;

    static final String SORT_NEWEST = "NEWEST";
    static final String SORT_OLDEST = "OLDEST";
    static final String SORT_TITLE = "TITLE";
    static final String SORT_SIZE = "SIZE";

    /** A screen living in one LicenseActivity instance. */
    interface Screen {
        /** @return True if the back press was handled and the activity must stay. */
        boolean onBackPressed();

        void onResume();

        void onPause();

        void onDestroy();
    }

    private static final WeakHashMap<Activity, Screen> screens = new WeakHashMap<>();
    private static boolean lifecycleCallbacksRegistered;

    private OfflineScreens() {
    }

    // region intents

    static Intent libraryIntent(Context context) {
        return hostIntent(context, LIBRARY_INTENT);
    }

    static Intent playerIntent(Context context, @Nullable String videoId) {
        Intent intent = hostIntent(context, PLAYER_INTENT);
        if (videoId != null) intent.putExtra(EXTRA_VIDEO_ID, videoId);
        return intent;
    }

    private static Intent hostIntent(Context context, String key) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setData(Uri.parse(key));
        intent.setClassName(context.getPackageName(), LICENSE_ACTIVITY);
        return intent;
    }

    private static void start(@Nullable Context context, Intent intent) {
        if (context == null) context = Utils.getActivity();
        if (context == null) context = Utils.getContext();
        if (!(context instanceof Activity)) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (Exception ex) {
            Logger.printException(() -> "Could not open the offline screen", ex);
        }
    }

    public static void openLibrary(@Nullable Context context) {
        Context base = context != null ? context : Utils.getContext();
        start(context, libraryIntent(base));
    }

    static void openPlayer(@Nullable Context context, @Nullable String videoId) {
        Context base = context != null ? context : Utils.getContext();
        start(context, playerIntent(base, videoId));
    }

    // endregion

    // region injection points

    /**
     * Injection point, from {@code YouTubeActivityHook.initialize}.
     *
     * @return True if the activity hosts an offline screen.
     */
    public static boolean initialize(Activity activity) {
        String data = activity.getIntent().getDataString();
        final boolean library = LIBRARY_INTENT.equals(data);
        final boolean player = PLAYER_INTENT.equals(data);
        if (!library && !player) return false;

        try {
            registerLifecycleCallbacks(activity);
            Screen screen = library ? new OfflineLibraryScreen(activity) : new OfflinePlayerScreen(activity);
            screens.put(activity, screen);
            Logger.printInfo(() -> "Showing offline " + (library ? "library" : "player"));
        } catch (Exception ex) {
            Logger.printException(() -> "Could not show the offline screen", ex);
        }
        return true;
    }

    /**
     * Injection point, from the patched {@code LicenseActivity.finish()}.
     *
     * @return True if the screen handled the back press and the activity must stay.
     */
    public static boolean handleBackPress(Activity activity) {
        Screen screen = screens.get(activity);
        return screen != null && screen.onBackPressed();
    }

    /**
     * @return If the activity hosts an offline screen, whose back presses must not reach the
     * settings search.
     */
    public static boolean hostsScreen(Activity activity) {
        return screens.containsKey(activity);
    }

    /**
     * Injection point, at the start of YouTube's browse-endpoint command handler.
     * Opens the offline library instead of YouTube's Downloads page. The navigation is cancelled,
     * so YouTube stays on the current page and Back from the library returns there.
     *
     * @return True to cancel YouTube's navigation.
     */
    public static boolean openLibraryInsteadOfBrowse(@Nullable String browseId) {
        return DOWNLOADS_BROWSE_ID.equals(browseId) && openLibraryInsteadOfDownloadsPage();
    }

    // the You tab library row opens the Downloads page with a
    // dedicated endpoint, which never reaches the browse-endpoint handler. Every command is checked
    // where YouTube resolves it, so each entry to the Downloads page is replaced.
    /**
     * Injection point, at the start of YouTube's command resolver.
     * Opens the offline library instead of YouTube's Downloads page for the two commands that
     * navigate to it: a browse endpoint with the browse id "FEdownloads" and the Downloads page
     * endpoint (used by the library row of the You tab). The command is cancelled, so YouTube stays
     * on the current page and Back from the library returns there.
     *
     * @param command The command, a protobuf message.
     * @return True to cancel YouTube's command.
     */
    public static boolean openLibraryInsteadOfCommand(@Nullable Object command) {
        if (!(command instanceof MessageLite message)
                || !Settings.IN_APP_DOWNLOADER.get()
                || !Settings.REPLACE_DOWNLOADS_PAGE.get()) {
            return false;
        }
        try {
            return isDownloadsPageCommand(message.toByteArray()) && openLibraryInsteadOfDownloadsPage();
        } catch (Exception ex) {
            Logger.printException(() -> "Could not check the command for the Downloads page", ex);
            return false;
        }
    }

    private static boolean openLibraryInsteadOfDownloadsPage() {
        if (!Settings.IN_APP_DOWNLOADER.get() || !Settings.REPLACE_DOWNLOADS_PAGE.get()) return false;
        Logger.printInfo(() -> "Replacing the Downloads page with the offline library");
        Utils.runOnMainThread(() -> openLibrary(Utils.getActivity()));
        return true;
    }

    /**
     * Reads the top-level fields of a serialized command. The field numbers are part of YouTube's
     * protocol, so they do not change with the obfuscated class names of each app version.
     */
    static boolean isDownloadsPageCommand(byte[] command) {
        int[] position = {0};
        while (position[0] < command.length) {
            long tag = readVarint(command, position);
            int wireType = (int) (tag & 7);
            int field = (int) (tag >>> 3);
            if (wireType == 2) {
                int length = (int) readVarint(command, position);
                int start = position[0];
                if (length < 0 || start + length > command.length) return false;
                if (field == DOWNLOADS_PAGE_ENDPOINT_FIELD) return true;
                if (field == BROWSE_ENDPOINT_FIELD && browseIdEquals(command, start, start + length)) {
                    return true;
                }
                position[0] = start + length;
            } else if (wireType == 0) {
                readVarint(command, position);
            } else if (wireType == 1) {
                position[0] += 8;
            } else if (wireType == 5) {
                position[0] += 4;
            } else {
                return false;
            }
        }
        return false;
    }

    /** @return If field 1 (browseId) of the browse endpoint in [start, end) is "FEdownloads". */
    private static boolean browseIdEquals(byte[] data, int start, int end) {
        int[] position = {start};
        while (position[0] < end) {
            long tag = readVarint(data, position);
            int wireType = (int) (tag & 7);
            if (wireType != 2) return false;
            int length = (int) readVarint(data, position);
            int valueStart = position[0];
            if (length < 0 || valueStart + length > end) return false;
            if ((tag >>> 3) == 1) {
                if (length != DOWNLOADS_BROWSE_ID_BYTES.length) return false;
                for (int i = 0; i < length; i++) {
                    if (data[valueStart + i] != DOWNLOADS_BROWSE_ID_BYTES[i]) return false;
                }
                return true;
            }
            position[0] = valueStart + length;
        }
        return false;
    }

    private static long readVarint(byte[] data, int[] position) {
        long result = 0;
        for (int shift = 0; shift < 64 && position[0] < data.length; shift += 7) {
            byte b = data[position[0]++];
            result |= (long) (b & 0x7F) << shift;
            if (b >= 0) return result;
        }
        position[0] = data.length;
        return -1;
    }

    // endregion

    // region lifecycle

    private static void registerLifecycleCallbacks(Activity activity) {
        if (lifecycleCallbacksRegistered) return;
        lifecycleCallbacksRegistered = true;
        // LicenseActivity has no lifecycle methods left after the settings patch, so the
        // application callbacks forward them to the screen of that instance.
        activity.getApplication().registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                Screen screen = screens.get(activity);
                if (screen != null) screen.onResume();
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
                Screen screen = screens.get(activity);
                if (screen != null) screen.onPause();
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                Screen screen = screens.remove(activity);
                if (screen != null) screen.onDestroy();
            }
        });
    }

    // endregion

    // region library order

    /** @return The videos in the library display order of the chosen sort. */
    static List<OfflineVideo> sorted(List<OfflineVideo> videos) {
        List<OfflineVideo> result = new ArrayList<>(videos);
        Comparator<OfflineVideo> comparator = switch (Settings.OFFLINE_LIBRARY_SORT.get()) {
            case SORT_OLDEST -> Comparator.comparingLong(video -> video.downloadedAt);
            case SORT_TITLE -> Comparator.comparing(video -> video.title.toLowerCase(Locale.ROOT));
            case SORT_SIZE -> Comparator.comparingLong((OfflineVideo video) -> video.sizeBytes).reversed();
            default -> Comparator.comparingLong((OfflineVideo video) -> video.downloadedAt).reversed();
        };
        result.sort(comparator);
        return result;
    }

    static List<String> order() {
        List<String> ids = new ArrayList<>();
        for (OfflineVideo video : sorted(OfflineIndex.getAll())) ids.add(video.videoId);
        return ids;
    }

    // endregion
}

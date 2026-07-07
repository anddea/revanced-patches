/*
 * Copyright (C) 2024-2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - inotia00 (https://github.com/inotia00)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Additional Terms & Attribution Requirements
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Source Credit Preservation (Section 7(b)): This specific copyright notice
 *    and the list of original authors above must be preserved in any copy
 *    or derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin & Modification Marking (Section 7(c)): Modified versions must be
 *    clearly marked as such (e.g., by adding a "Modified by" line or a new
 *    copyright notice) and must not be misrepresented as the original work.
 *
 * 3. Version Control Attribution (Section 7(b)): Any ports or substantial
 *    modifications must retain historical authorship credit in version control
 *    systems (e.g., Git), listing original author(s) appropriately and
 *    modifiers as committers or co-authors.
 *
 * 4. User Interface Attribution (Section 7(b)): Any works containing or
 *    derived from this material must maintain a visible credit or
 *    acknowledgment to the original author(s) within the application's
 *    user interface (e.g., in an "About" or "Credits" section).
 */

/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to Morphe contributions.
 */

package app.morphe.extension.youtube.patches.general;

import static app.morphe.extension.youtube.utils.VideoUtils.launchPlaylistExternalDownloader;
import static app.morphe.extension.youtube.utils.VideoUtils.launchVideoExternalDownloader;

import android.app.Dialog;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Pair;
import android.view.View;
import android.view.ViewParent;

import androidx.annotation.Nullable;

import com.facebook.litho.ComponentHost;

import org.apache.commons.lang3.StringUtils;

import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.youtube.patches.utils.PlaylistPatch;
import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class DownloadActionsPatch {

    /**
     * Interface added to YouTube's flyout protocol-buffer holder during patching.
     */
    public interface ProtocolBufferFieldInterface {
        byte[] patch_getBuffer();
    }

    /**
     * Interface added to YouTube message types that expose a flyout video ID.
     */
    public interface FlyoutMenuVideoIdInterface {
        String patch_getVideoId();
    }

    private static final boolean OVERRIDE_PLAY_NEXT_IN_QUEUE =
            Settings.OVERRIDE_PLAY_NEXT_IN_QUEUE.get();

    private static final String QUEUE_BUTTON_NAME = "QUEUE_PLAY_NEXT";
    private static final List<byte[]> VIDEO_ID_PREFIXES_BYTES = List.of(
            // Can be i.ytimg.com, i2.ytimg.com, i3.ytimg.com, and so on.
            ".ytimg.com/vi/".getBytes(StandardCharsets.US_ASCII),
            "youtube.com/watch?v=".getBytes(StandardCharsets.US_ASCII)
    );
    private static final byte[] HORIZONTAL_SHELF_BYTES =
            "horizontal_shelf.e".getBytes(StandardCharsets.US_ASCII);

    private static WeakReference<View> senderViewRef = new WeakReference<>(null);
    private static final List<Pair<String, Integer>> visibleFlyoutButtons = new ArrayList<>();
    private static Dialog queueBottomSheetFlyout;
    private static String flyoutVideoId = "";
    private static String currentFlyoutButtonName = "";
    private static int currentFlyoutButtonIndex;

    private static final boolean OVERRIDE_PLAYLIST_DOWNLOAD_BUTTON =
            Settings.OVERRIDE_PLAYLIST_DOWNLOAD_BUTTON.get();

    private static final boolean OVERRIDE_VIDEO_DOWNLOAD_BUTTON =
            Settings.OVERRIDE_VIDEO_DOWNLOAD_BUTTON.get();

    private static final boolean OVERRIDE_VIDEO_DOWNLOAD_BUTTON_QUEUE_MANAGER =
            OVERRIDE_VIDEO_DOWNLOAD_BUTTON && Settings.OVERRIDE_VIDEO_DOWNLOAD_BUTTON_QUEUE_MANAGER.get();

    /**
     * Injection point. Tracks the native flyout lifecycle without replacing its dismiss listener.
     */
    public static void setQueueBottomSheetFlyout(@Nullable Dialog dialog) {
        if (dialog == null) {
            return;
        }

        queueBottomSheetFlyout = dialog;
        Handler visibilityHandler = new Handler(Looper.getMainLooper());
        visibilityHandler.post(new Runnable() {
            @Override
            public void run() {
                if (dialog.isShowing()) {
                    visibilityHandler.postDelayed(this, 100);
                } else {
                    flyoutVideoId = "";
                }
            }
        });
    }

    private static void dismissQueueBottomSheetFlyout() {
        if (queueBottomSheetFlyout != null) {
            queueBottomSheetFlyout.dismiss();
        }
    }

    /**
     * Injection point. Extracts the sender view and protocol-buffer holder for feed flyouts.
     */
    public static void extractFlyoutVideoId(@Nullable Map<?, ?> map) {
        if (map == null) {
            return;
        }

        senderViewRef = new WeakReference<>(
                (View) map.get("com.google.android.libraries.youtube.rendering.elements.sender_view")
        );
        extractFlyoutVideoId(map.get("com.google.android.libraries.youtube.innertube.endpoint.tag"));
    }

    /**
     * Injection point. Resolves the video ID from a typed message or its serialized buffer.
     */
    public static void extractFlyoutVideoId(@Nullable Object bufferObject) {
        try {
            if (bufferObject instanceof FlyoutMenuVideoIdInterface videoIdInterface) {
                String videoId = videoIdInterface.patch_getVideoId();
                if (videoId != null) {
                    flyoutVideoId = videoId;
                    visibleFlyoutButtons.clear();
                    Logger.printDebug(() -> "Found flyout videoId: " + videoId);
                }
                return;
            }

            if (!(bufferObject instanceof ProtocolBufferFieldInterface bufferInterface)) {
                return;
            }

            visibleFlyoutButtons.clear();
            byte[] flyoutBuffer = bufferInterface.patch_getBuffer();
            if (flyoutBuffer == null) {
                return;
            }

            if (indexOf(flyoutBuffer, HORIZONTAL_SHELF_BYTES) >= 0) {
                View senderView = senderViewRef.get();
                ViewParent parent = senderView == null ? null : senderView.getParent();
                while (parent != null) {
                    if (parent instanceof ComponentHost componentHost) {
                        CharSequence description = componentHost.getContentDescription();
                        if (description != null) {
                            flyoutBuffer = getTrimmedHorizontalShelfBuffer(
                                    flyoutBuffer,
                                    description.toString()
                            );
                        }
                    }
                    parent = parent.getParent();
                }
            }

            for (byte[] prefix : VIDEO_ID_PREFIXES_BYTES) {
                int index = indexOf(flyoutBuffer, prefix);
                if (index < 0) {
                    continue;
                }

                int videoIdStart = index + prefix.length;
                int videoIdEnd = videoIdStart + 11;
                if (videoIdEnd <= flyoutBuffer.length) {
                    flyoutVideoId = new String(
                            flyoutBuffer,
                            videoIdStart,
                            11,
                            StandardCharsets.US_ASCII
                    );
                    Logger.printDebug(() -> "Found flyout videoId: " + flyoutVideoId);
                    break;
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "extractFlyoutVideoId failure", ex);
        }
    }

    private static int indexOf(byte[] haystack, byte[] needle) {
        int needleLength = needle.length;
        for (int i = 0, lastIndex = haystack.length - needleLength; i <= lastIndex; i++) {
            boolean found = true;
            for (int j = 0; j < needleLength; j++) {
                if (haystack[i + j] != needle[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Narrows a horizontal-shelf buffer to the item whose title matches the sender view.
     */
    public static byte[] getTrimmedHorizontalShelfBuffer(byte[] buffer, String description) {
        if (buffer == null || description == null || description.isEmpty()) {
            return buffer;
        }

        String[] parts = description.split(" - ");
        String title = parts[0].toLowerCase(Locale.ROOT).replaceAll("[^a-zA-Z0-9\\s]", "");
        List<byte[]> words = new ArrayList<>();
        for (String word : title.split("\\s+")) {
            if (word.length() > 2) {
                words.add(word.getBytes(StandardCharsets.UTF_8));
            }
        }
        if (words.isEmpty()) {
            return buffer;
        }

        int bestIndex = -1;
        int maxScore = 0;
        int windowSize = 200;
        for (int i = 0; i <= buffer.length - windowSize; i += 20) {
            int score = 0;
            for (byte[] word : words) {
                int endLimit = i + windowSize - word.length;
                boolean found = false;
                for (int j = i; j <= endLimit; j++) {
                    int k = 0;
                    while (k < word.length) {
                        byte value = buffer[j + k];
                        byte normalized = value >= 65 && value <= 90 ? (byte) (value + 32) : value;
                        if (normalized != word[k]) {
                            break;
                        }
                        k++;
                    }
                    if (k == word.length) {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    score++;
                }
            }
            if (score > maxScore) {
                maxScore = score;
                bestIndex = i;
            }
        }

        int requiredScore = Math.max(1, (int) Math.ceil(words.size() * 0.4));
        return bestIndex >= 0 && maxScore >= requiredScore
                ? Arrays.copyOfRange(buffer, bestIndex, buffer.length)
                : buffer;
    }

    /**
     * Injection point. Records the native flyout entry order and enum names.
     */
    public static void setCurrentFlyoutButton(@Nullable Enum<?> buttonEnum,
                                              @Nullable CharSequence buttonText) {
        if (buttonEnum == null || buttonText == null || buttonText.toString().isEmpty()) {
            return;
        }

        currentFlyoutButtonName = buttonEnum.name();
        currentFlyoutButtonIndex++;
        visibleFlyoutButtons.add(new Pair<>(currentFlyoutButtonName, currentFlyoutButtonIndex));
    }

    /**
     * Injection point. Removes only the right-side native queue badge because the
     * left-side row icon still identifies the Play next entry.
     */
    public static Drawable hideQueueFlyoutBadge(@Nullable Drawable icon) {
        return OVERRIDE_PLAY_NEXT_IN_QUEUE && QUEUE_BUTTON_NAME.equals(currentFlyoutButtonName)
                ? null
                : icon;
    }

    /**
     * Injection point for flyout implementations backed by a Runnable.
     */
    public static Runnable replaceQueueButtonRunnable(Runnable original) {
        if (!OVERRIDE_PLAY_NEXT_IN_QUEUE || flyoutVideoId.isEmpty()) {
            return original;
        }

        String buttonName = currentFlyoutButtonName;
        return () -> {
            if (QUEUE_BUTTON_NAME.equals(buttonName)) {
                openQueueManagerForFlyoutVideo();
            } else if (original != null) {
                original.run();
            }
        };
    }

    /**
     * Injection point for YouTube 20.x flyout implementations backed by an item index.
     */
    public static boolean replaceQueueOnItemClick(int index) {
        if (!OVERRIDE_PLAY_NEXT_IN_QUEUE || flyoutVideoId.isEmpty()) {
            return false;
        }

        try {
            if (index >= 0 && index < visibleFlyoutButtons.size()
                    && QUEUE_BUTTON_NAME.equals(visibleFlyoutButtons.get(index).first)) {
                openQueueManagerForFlyoutVideo();
                return true;
            }
        } catch (Exception ex) {
            Logger.printException(() -> "replaceQueueOnItemClick failure", ex);
        }
        return false;
    }

    private static void openQueueManagerForFlyoutVideo() {
        Logger.printDebug(() -> "Opening queue manager with videoId: " + flyoutVideoId);
        PlaylistPatch.prepareDialogBuilder(flyoutVideoId);
        dismissQueueBottomSheetFlyout();
    }

    /**
     * Injection point.
     * <p>
     * Called from the in app download hook,
     * for both the player action button (below the video)
     * and the 'Download video' flyout option for feed videos.
     * <p>
     * Appears to always be called from the main thread.
     */
    public static boolean inAppVideoDownloadButtonOnClick(@Nullable Map<Object, Object> map, Object offlineVideoEndpointOuterClass,
                                                          @Nullable String videoId) {
        try {
            if (OVERRIDE_VIDEO_DOWNLOAD_BUTTON && StringUtils.isNotEmpty(videoId)) {
                if (OVERRIDE_VIDEO_DOWNLOAD_BUTTON_QUEUE_MANAGER) {
                    PlaylistPatch.prepareDialogBuilder(videoId);
                } else {
                    launchVideoExternalDownloader(videoId);
                }

                return true;
            }
        } catch (Exception ex) {
            Logger.printException(() -> "inAppVideoDownloadButtonOnClick failure", ex);
        }
        return false;
    }

    /**
     * Injection point.
     * <p>
     * Called from the in app playlist download hook.
     * <p>
     * Appears to always be called from the main thread.
     */
    public static String inAppPlaylistDownloadButtonOnClick(String playlistId) {
        try {
            if (OVERRIDE_PLAYLIST_DOWNLOAD_BUTTON && StringUtils.isNotEmpty(playlistId)) {
                launchPlaylistExternalDownloader(playlistId);
                return "";
            }
        } catch (Exception ex) {
            Logger.printException(() -> "inAppPlaylistDownloadButtonOnClick failure", ex);
        }
        return playlistId;
    }

    /**
     * Injection point.
     * <p>
     * Called from the 'Download playlist' flyout option.
     * <p>
     * Appears to always be called from the main thread.
     */
    public static boolean inAppPlaylistDownloadMenuOnClick(String playlistId) {
        try {
            if (OVERRIDE_PLAYLIST_DOWNLOAD_BUTTON && StringUtils.isNotEmpty(playlistId)) {
                launchPlaylistExternalDownloader(playlistId);
                return true;
            }
        } catch (Exception ex) {
            Logger.printException(() -> "inAppPlaylistDownloadMenuOnClick failure", ex);
        }
        return false;
    }

    /**
     * Injection point.
     */
    public static boolean overridePlaylistDownloadButtonVisibility(boolean original) {
        return OVERRIDE_PLAYLIST_DOWNLOAD_BUTTON || original;
    }

}

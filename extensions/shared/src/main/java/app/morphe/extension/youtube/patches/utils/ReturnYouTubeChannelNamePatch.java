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

package app.morphe.extension.youtube.patches.utils;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class ReturnYouTubeChannelNamePatch {

    private static final boolean REPLACE_CHANNEL_HANDLE = Settings.REPLACE_CHANNEL_HANDLE.get();
    /**
     * The last character of some handles is an official channel certification mark.
     * This was in the form of nonBreakSpaceCharacter before SpannableString was made.
     */
    private static final String NON_BREAK_SPACE_CHARACTER = "\u00A0";
    private volatile static String channelName = "";

    /**
     * Key: channelId, Value: channelName.
     */
    private static final Map<String, String> channelIdMap = Collections.synchronizedMap(
            new LinkedHashMap<>(20) {
                private static final int CACHE_LIMIT = 10;

                @Override
                protected boolean removeEldestEntry(Entry eldest) {
                    return size() > CACHE_LIMIT; // Evict the oldest entry if over the cache limit.
                }
            });

    /**
     * Key: handle, Value: channelName.
     */
    private static final Map<String, String> channelHandleMap = Collections.synchronizedMap(
            new LinkedHashMap<>(20) {
                private static final int CACHE_LIMIT = 10;

                @Override
                protected boolean removeEldestEntry(Entry eldest) {
                    return size() > CACHE_LIMIT; // Evict the oldest entry if over the cache limit.
                }
            });

    /**
     * This method is only invoked on Shorts and is updated whenever the user swipes up or down on the Shorts.
     */
    public static void newShortsVideoStarted(@NonNull String newlyLoadedChannelId, @NonNull String newlyLoadedChannelName,
                                             @NonNull String newlyLoadedVideoId, @NonNull String newlyLoadedVideoTitle,
                                             final long newlyLoadedVideoLength, boolean newlyLoadedLiveStreamValue) {
        if (channelIdMap.get(newlyLoadedChannelId) != null) {
            return;
        }
        if (channelIdMap.put(newlyLoadedChannelId, newlyLoadedChannelName) == null) {
            channelName = newlyLoadedChannelName;
            Logger.printDebug(() -> "New video started, ChannelId " + newlyLoadedChannelId + ", Channel Name: " + newlyLoadedChannelName);
        }
    }

    @NonNull
    public static String getCachedChannelName(@NonNull String channelId) {
        String cachedChannelName = channelIdMap.get(channelId);
        return cachedChannelName == null ? "" : cachedChannelName;
    }

    public static void setCachedChannelName(@NonNull String channelId, @NonNull String channelName) {
        if (channelId.isEmpty() || channelName.isEmpty() || channelIdMap.get(channelId) != null) {
            return;
        }
        channelIdMap.put(channelId, channelName);
    }

    /**
     * Injection point.
     */
    public static CharSequence onCharSequenceLoaded(@NonNull Object conversionContext,
                                                    @NonNull CharSequence charSequence) {
        try {
            if (!REPLACE_CHANNEL_HANDLE) {
                return charSequence;
            }
            final String conversionContextString = conversionContext.toString();
            if (!conversionContextString.contains("|reel_channel_bar_inner.")) {
                return charSequence;
            }
            final String originalString = charSequence.toString();
            if (!originalString.startsWith("@")) {
                return charSequence;
            }
            return getChannelName(originalString);
        } catch (Exception ex) {
            Logger.printException(() -> "onCharSequenceLoaded failed", ex);
        }
        return charSequence;
    }

    private static CharSequence getChannelName(@NonNull String handle) {
        final String trimmedHandle = handle.replaceAll(NON_BREAK_SPACE_CHARACTER, "");

        String cachedChannelName = channelHandleMap.get(trimmedHandle);
        if (cachedChannelName == null) {
            if (!channelName.isEmpty() && channelHandleMap.put(handle, channelName) == null) {
                Logger.printDebug(() -> "Set Handle from last fetched Channel Name, Handle: " + handle + ", Channel Name: " + channelName);
                cachedChannelName = channelName;
            } else {
                Logger.printDebug(() -> "Channel handle is not found: " + trimmedHandle);
                return handle;
            }
        }

        if (handle.contains(NON_BREAK_SPACE_CHARACTER)) {
            cachedChannelName += NON_BREAK_SPACE_CHARACTER;
        }
        String replacedChannelName = cachedChannelName;
        Logger.printDebug(() -> "Replace Handle " + handle + " to " + replacedChannelName);
        return replacedChannelName;
    }

    public synchronized static void setLastShortsChannelId(@NonNull String handle, @NonNull String channelId) {
        try {
            if (channelHandleMap.get(handle) != null) {
                return;
            }
            final String channelName = channelIdMap.get(channelId);
            if (channelName == null) {
                Logger.printDebug(() -> "Channel name is not found!");
                return;
            }
            if (channelHandleMap.put(handle, channelName) == null) {
                Logger.printDebug(() -> "Set Handle from Shorts, Handle: " + handle + ", Channel Name: " + channelName);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "setLastShortsChannelId failure ", ex);
        }
    }
}

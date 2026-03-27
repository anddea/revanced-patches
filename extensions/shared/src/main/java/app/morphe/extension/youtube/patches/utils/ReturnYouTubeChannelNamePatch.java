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
        if (!REPLACE_CHANNEL_HANDLE) {
            return;
        }
        if (channelIdMap.get(newlyLoadedChannelId) != null) {
            return;
        }
        if (channelIdMap.put(newlyLoadedChannelId, newlyLoadedChannelName) == null) {
            channelName = newlyLoadedChannelName;
            Logger.printDebug(() -> "New video started, ChannelId " + newlyLoadedChannelId + ", Channel Name: " + newlyLoadedChannelName);
        }
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

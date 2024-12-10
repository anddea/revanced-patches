package app.revanced.extension.shared.patches;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import android.text.SpannableString;
import android.text.Spanned;

import androidx.annotation.NonNull;

import app.revanced.extension.shared.returnyoutubeusername.requests.ChannelRequest;
import app.revanced.extension.shared.settings.BaseSettings;
import app.revanced.extension.shared.utils.Logger;

@SuppressWarnings("unused")
public class ReturnYouTubeUsernamePatch {
    private static final boolean RETURN_YOUTUBE_USERNAME_ENABLED = BaseSettings.RETURN_YOUTUBE_USERNAME_ENABLED.get();
    private static final Boolean RETURN_YOUTUBE_USERNAME_DISPLAY_FORMAT = BaseSettings.RETURN_YOUTUBE_USERNAME_DISPLAY_FORMAT.get().userNameFirst;
    private static final String YOUTUBE_API_KEY = BaseSettings.RETURN_YOUTUBE_USERNAME_YOUTUBE_DATA_API_V3_DEVELOPER_KEY.get();

    private static final String AUTHOR_BADGE_PATH = "|author_badge.eml|";
    private static volatile String lastFetchedHandle = "";

    /**
     * Injection point.
     *
     * @param original The original string before the SpannableString is built.
     */
    public static CharSequence preFetchLithoText(@NonNull Object conversionContext,
                                                 @NonNull CharSequence original) {
        onLithoTextLoaded(conversionContext, original, true);
        return original;
    }

    /**
     * Injection point.
     *
     * @param original The original string after the SpannableString is built.
     */
    @NonNull
    public static CharSequence onLithoTextLoaded(@NonNull Object conversionContext,
                                         @NonNull CharSequence original) {
        return onLithoTextLoaded(conversionContext, original, false);
    }

    @NonNull
    private static CharSequence onLithoTextLoaded(@NonNull Object conversionContext,
                                                 @NonNull CharSequence original,
                                                 boolean fetchNeeded) {
        try {
            if (!RETURN_YOUTUBE_USERNAME_ENABLED) {
                return original;
            }
            if (YOUTUBE_API_KEY.isEmpty()) {
                Logger.printDebug(() -> "API key is empty");
                return original;
            }
            // In comments, the path to YouTube Handle(@youtube) always includes [AUTHOR_BADGE_PATH].
            if (!conversionContext.toString().contains(AUTHOR_BADGE_PATH)) {
                return original;
            }
            String handle = original.toString();
            if (fetchNeeded && !handle.equals(lastFetchedHandle)) {
                lastFetchedHandle = handle;
                // Get the original username using YouTube Data API v3.
                ChannelRequest.fetchRequestIfNeeded(handle, YOUTUBE_API_KEY, RETURN_YOUTUBE_USERNAME_DISPLAY_FORMAT);
                return original;
            }
            // If the username is not in the cache, put it in the cache.
            ChannelRequest channelRequest = ChannelRequest.getRequestForHandle(handle);
            if (channelRequest == null) {
                Logger.printDebug(() -> "ChannelRequest is null, handle:" + handle);
                return original;
            }
            final String userName = channelRequest.getStream();
            if (userName == null) {
                Logger.printDebug(() -> "ChannelRequest Stream is null, handle:" + handle);
                return original;
            }
            final CharSequence copiedSpannableString = copySpannableString(original, userName);
            Logger.printDebug(() -> "Replaced: '" + original + "' with: '" + copiedSpannableString + "'");
            return copiedSpannableString;
        } catch (Exception ex) {
            Logger.printException(() -> "onLithoTextLoaded failure", ex);
        }
        return original;
    }

    private static CharSequence copySpannableString(CharSequence original, String userName) {
        if (original instanceof Spanned spanned) {
            SpannableString newString = new SpannableString(userName);
            Object[] spans = spanned.getSpans(0, spanned.length(), Object.class);
            for (Object span : spans) {
                int flags = spanned.getSpanFlags(span);
                newString.setSpan(span, 0, newString.length(), flags);
            }
            return newString;
        }
        return original;
    }

    public enum DisplayFormat {
        USERNAME_ONLY(null),
        USERNAME_HANDLE(TRUE),
        HANDLE_USERNAME(FALSE);

        final Boolean userNameFirst;

        DisplayFormat(Boolean userNameFirst) {
            this.userNameFirst = userNameFirst;
        }
    }
}

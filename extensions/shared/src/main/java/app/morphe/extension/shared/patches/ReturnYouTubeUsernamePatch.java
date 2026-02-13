package app.morphe.extension.shared.patches;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static app.morphe.extension.shared.utils.Utils.newSpanUsingStylingOfAnotherSpan;

import androidx.annotation.NonNull;

import app.morphe.extension.shared.returnyoutubeusername.requests.ChannelRequest;
import app.morphe.extension.shared.settings.BaseSettings;
import app.morphe.extension.shared.utils.Logger;

@SuppressWarnings("unused")
public class ReturnYouTubeUsernamePatch {
    private static final String YOUTUBE_API_KEY =
            BaseSettings.RETURN_YOUTUBE_USERNAME_YOUTUBE_DATA_API_V3_DEVELOPER_KEY.get();
    private static final boolean RETURN_YOUTUBE_USERNAME_ENABLED =
            BaseSettings.RETURN_YOUTUBE_USERNAME_ENABLED.get() && !YOUTUBE_API_KEY.isEmpty();
    private static final Boolean RETURN_YOUTUBE_USERNAME_DISPLAY_FORMAT =
            BaseSettings.RETURN_YOUTUBE_USERNAME_DISPLAY_FORMAT.get().userNameFirst;

    private static final String AUTHOR_BADGE_PATH = "|author_badge.";

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
            // In comments, the path to YouTube Handle(@youtube) always includes [AUTHOR_BADGE_PATH].
            if (!conversionContext.toString().contains(AUTHOR_BADGE_PATH)) {
                return original;
            }
            String handle = original.toString();
            if (fetchNeeded) {
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
            return newSpanUsingStylingOfAnotherSpan(original, userName);
        } catch (Exception ex) {
            Logger.printException(() -> "onLithoTextLoaded failure", ex);
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

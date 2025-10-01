package com.liskovsoft.googlecommon.common.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class YouTubeHelper {
    /**
     * Generate a content playback nonce (also called {@code cpn}), sent by YouTube clients in
     * playback requests (and also for some clients, in the player request body).
     *
     * @return a content playback nonce string
     */
    @NonNull
    public static String generateContentPlaybackNonce() {
        return RandomStringFromAlphabetGenerator.generate(16);
    }

    @NonNull
    public static String generateContentPlaybackNonce(@NonNull String cpn) {
        return cpn.length() == 16
                ? cpn
                : generateContentPlaybackNonce();
    }

    /**
     * Try to generate a {@code t} parameter, sent by mobile clients as a query of the player
     * request.
     *
     * <p>
     * Some researches needs to be done to know how this parameter, unique at each request, is
     * generated.
     * </p>
     *
     * @return a 12 characters string to try to reproduce the {@code} parameter
     */
    @NonNull
    public static String generateTParameter() {
        return RandomStringFromAlphabetGenerator.generate(12);
    }

    @NonNull
    public static String generateTParameter(@Nullable String tParameter) {
        return tParameter != null && tParameter.length() == 12
                ? tParameter
                : generateTParameter();
    }
}

package com.liskovsoft.youtubeapi.app.potokennp2.misc;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * The result of a supported/successful {@code poToken} extraction request by a
 * {@link PoTokenProvider}.
 */
public final class PoTokenResult {
    @NonNull
    public final String videoId;

    /**
     * The visitor data associated with a {@code poToken}.
     */
    @NonNull
    public final String visitorData;

    /**
     * The {@code poToken} of a player request, a Protobuf object encoded as a base 64 string.
     */
    @NonNull
    public final String playerRequestPoToken;

    /**
     * The {@code poToken} to be appended to streaming URLs, a Protobuf object encoded as a base
     * 64 string.
     *
     * <p>
     * It may be required on some clients such as HTML5 ones and may also differ from the player
     * request {@code poToken}.
     * </p>
     */
    @Nullable
    public final String streamingDataPoToken;

    /**
     * Construct a {@link PoTokenResult} instance.
     *
     * @param videoId              see {@link #videoId}
     * @param visitorData          see {@link #visitorData}
     * @param playerRequestPoToken see {@link #playerRequestPoToken}
     * @param streamingDataPoToken see {@link #streamingDataPoToken}
     * @throws NullPointerException if a non-null parameter is null
     */
    public PoTokenResult(@NonNull final String videoId,
                         @NonNull final String visitorData,
                         @NonNull final String playerRequestPoToken,
                         @Nullable final String streamingDataPoToken) {
        this.videoId = videoId;
        this.visitorData = visitorData;
        this.playerRequestPoToken = playerRequestPoToken;
        this.streamingDataPoToken = streamingDataPoToken;
    }
}

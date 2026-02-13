package app.morphe.extension.youtube.shared;

import androidx.annotation.NonNull;

public enum PlaylistIdPrefix {
    /**
     * To check all available prefixes,
     * See <a href="https://github.com/RobertWesner/YouTube-Play-All/blob/main/documentation/available-lists.md">this document</a>.
     */
    ALL_CONTENTS_WITH_TIME_ASCENDING("UL", false),
    ALL_CONTENTS_WITH_TIME_DESCENDING("UU", true),
    ALL_CONTENTS_WITH_POPULAR_DESCENDING("PU", true),
    VIDEOS_ONLY_WITH_TIME_DESCENDING("UULF", true),
    VIDEOS_ONLY_WITH_POPULAR_DESCENDING("UULP", true),
    SHORTS_ONLY_WITH_TIME_DESCENDING("UUSH", true),
    SHORTS_ONLY_WITH_POPULAR_DESCENDING("UUPS", true),
    LIVESTREAMS_ONLY_WITH_TIME_DESCENDING("UULV", true),
    LIVESTREAMS_ONLY_WITH_POPULAR_DESCENDING("UUPV", true),
    ALL_MEMBERSHIPS_CONTENTS("UUMO", true),
    MEMBERSHIPS_VIDEOS_ONLY("UUMF", true),
    MEMBERSHIPS_SHORTS_ONLY("UUMS", true),
    MEMBERSHIPS_LIVESTREAMS_ONLY("UUMV", true);

    /**
     * Prefix of playlist id.
     */
    @NonNull
    public final String prefixId;

    /**
     * Whether to use channelId.
     */
    public final boolean useChannelId;

    PlaylistIdPrefix(@NonNull String prefixId, boolean useChannelId) {
        this.prefixId = prefixId;
        this.useChannelId = useChannelId;
    }
}

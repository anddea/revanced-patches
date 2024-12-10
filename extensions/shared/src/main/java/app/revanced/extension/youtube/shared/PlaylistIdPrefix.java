package app.revanced.extension.youtube.shared;

import androidx.annotation.NonNull;

public enum PlaylistIdPrefix {
    /**
     * To check all available prefixes,
     * See <a href="https://github.com/RobertWesner/YouTube-Play-All/blob/main/documentation/available-lists.md">this document</a>.
     */
    ALL_CONTENTS_WITH_TIME_ASCENDING("UL"),
    ALL_CONTENTS_WITH_TIME_DESCENDING("UU"),
    ALL_CONTENTS_WITH_POPULAR_DESCENDING("PU"),
    VIDEOS_ONLY_WITH_TIME_DESCENDING("UULF"),
    VIDEOS_ONLY_WITH_POPULAR_DESCENDING("UULP"),
    SHORTS_ONLY_WITH_TIME_DESCENDING("UUSH"),
    SHORTS_ONLY_WITH_POPULAR_DESCENDING("UUPS"),
    LIVESTREAMS_ONLY_WITH_TIME_DESCENDING("UULV"),
    LIVESTREAMS_ONLY_WITH_POPULAR_DESCENDING("UUPV"),
    ALL_MEMBERSHIPS_CONTENTS("UUMO"),
    MEMBERSHIPS_VIDEOS_ONLY("UUMF"),
    MEMBERSHIPS_SHORTS_ONLY("UUMS"),
    MEMBERSHIPS_LIVESTREAMS_ONLY("UUMV");

    /**
     * Prefix of playlist id.
     */
    @NonNull
    public final String prefixId;

    PlaylistIdPrefix(@NonNull String prefixId) {
        this.prefixId = prefixId;
    }
}

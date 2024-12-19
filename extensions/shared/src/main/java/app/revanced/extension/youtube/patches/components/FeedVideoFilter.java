package app.revanced.extension.youtube.patches.components;

import androidx.annotation.Nullable;

import app.revanced.extension.shared.patches.components.ByteArrayFilterGroup;
import app.revanced.extension.shared.patches.components.ByteArrayFilterGroupList;
import app.revanced.extension.shared.patches.components.Filter;
import app.revanced.extension.shared.patches.components.StringFilterGroup;
import app.revanced.extension.shared.patches.components.StringFilterGroupList;
import app.revanced.extension.shared.utils.StringTrieSearch;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.shared.RootView;

@SuppressWarnings("unused")
public final class FeedVideoFilter extends Filter {
    private static final String CONVERSATION_CONTEXT_FEED_IDENTIFIER =
            "horizontalCollectionSwipeProtector=null";
    private static final String ENDORSEMENT_FOOTER_PATH = "endorsement_header_footer";

    private static final StringTrieSearch feedOnlyVideoPattern = new StringTrieSearch();
    // In search results, vertical video with shorts labels mostly include videos with gray descriptions.
    // Filters without check process.
    private final StringFilterGroup inlineShorts;
    // Used for home, related videos, subscriptions, and search results.
    private final StringFilterGroup videoLockup = new StringFilterGroup(
            null,
            "video_lockup_with_attachment.eml"
    );
    private final ByteArrayFilterGroupList feedAndDrawerGroupList = new ByteArrayFilterGroupList();
    private final ByteArrayFilterGroupList feedOnlyGroupList = new ByteArrayFilterGroupList();
    private final StringFilterGroupList videoLockupFilterGroup = new StringFilterGroupList();
    private static final ByteArrayFilterGroup relatedVideo =
            new ByteArrayFilterGroup(
                    Settings.HIDE_RELATED_VIDEOS,
                    "relatedH"
            );

    public FeedVideoFilter() {
        feedOnlyVideoPattern.addPattern(CONVERSATION_CONTEXT_FEED_IDENTIFIER);

        inlineShorts = new StringFilterGroup(
                Settings.HIDE_RECOMMENDED_VIDEO,
                "inline_shorts.eml" // vertical video with shorts label
        );

        addIdentifierCallbacks(inlineShorts);

        addPathCallbacks(videoLockup);

        feedAndDrawerGroupList.addAll(
                new ByteArrayFilterGroup(
                        Settings.HIDE_RECOMMENDED_VIDEO,
                        ENDORSEMENT_FOOTER_PATH, // videos with gray descriptions
                        "high-ptsZ" // videos for membership only
                )
        );

        feedOnlyGroupList.addAll(
                new ByteArrayFilterGroup(
                        Settings.HIDE_LOW_VIEWS_VIDEO,
                        "g-highZ"  // videos with less than 1000 views
                )
        );

        videoLockupFilterGroup.addAll(
                new StringFilterGroup(
                        Settings.HIDE_RECOMMENDED_VIDEO,
                        ENDORSEMENT_FOOTER_PATH
                )
        );
    }

    @Override
    public boolean isFiltered(String path, @Nullable String identifier, String allValue, byte[] protobufBufferArray,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (matchedGroup == inlineShorts) {
            if (RootView.isSearchBarActive()) {
                return super.isFiltered(path, identifier, allValue, protobufBufferArray, matchedGroup, contentType, contentIndex);
            }
            return false;
        } else if (matchedGroup == videoLockup) {
            if (relatedVideo.check(protobufBufferArray).isFiltered()) {
                return super.isFiltered(path, identifier, allValue, protobufBufferArray, matchedGroup, contentType, contentIndex);
            }
            if (feedOnlyVideoPattern.matches(allValue)) {
                if (feedOnlyGroupList.check(protobufBufferArray).isFiltered()) {
                    return super.isFiltered(path, identifier, allValue, protobufBufferArray, matchedGroup, contentType, contentIndex);
                } else if (videoLockupFilterGroup.check(allValue).isFiltered()) {
                    return super.isFiltered(path, identifier, allValue, protobufBufferArray, matchedGroup, contentType, contentIndex);
                }
            } else {
                if (feedAndDrawerGroupList.check(protobufBufferArray).isFiltered()) {
                    return super.isFiltered(path, identifier, allValue, protobufBufferArray, matchedGroup, contentType, contentIndex);
                }
            }
        }
        return false;
    }
}

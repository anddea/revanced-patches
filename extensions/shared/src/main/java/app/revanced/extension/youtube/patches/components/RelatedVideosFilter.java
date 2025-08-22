package app.revanced.extension.youtube.patches.components;

import androidx.annotation.Nullable;

import app.revanced.extension.shared.patches.components.ByteArrayFilterGroup;
import app.revanced.extension.shared.patches.components.Filter;
import app.revanced.extension.shared.patches.components.StringFilterGroup;
import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class RelatedVideosFilter extends Filter {
    private final ByteArrayFilterGroup relatedVideo =
            new ByteArrayFilterGroup(
                    null,
                    "relatedH"
            );

    public RelatedVideosFilter() {
        addPathCallbacks(
                new StringFilterGroup(
                        Settings.HIDE_RELATED_VIDEOS,
                        "video_lockup_with_attachment.eml"
                )
        );
    }

    @Override
    public boolean isFiltered(String path, @Nullable String identifier, String allValue, byte[] protobufBufferArray,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (!relatedVideo.check(protobufBufferArray).isFiltered()) {
            return false;
        }

        return super.isFiltered(path, identifier, allValue, protobufBufferArray, matchedGroup, contentType, contentIndex);
    }
}

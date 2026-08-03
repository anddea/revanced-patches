/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to Morphe contributions.
 */

package app.morphe.extension.youtube.patches.player;

import com.google.protobuf.MessageLite;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.youtube.innertube.NextResponseOuterClass.SecondaryContents;
import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class RelatedVideoPatch {
    private static final boolean HIDE_RELATED_VIDEOS = Settings.HIDE_RELATED_VIDEOS.get();
    private static final String COMMENTS = "comments";
    private static volatile boolean isFiltered;

    /**
     * Injection point that resets filtering state for each watch-next response.
     *
     * @return whether related videos should be hidden
     */
    public static boolean hideRelatedVideos() {
        if (HIDE_RELATED_VIDEOS) {
            isFiltered = false;
        }

        return HIDE_RELATED_VIDEOS;
    }

    /**
     * Identifies related-video item sections while preserving the comments section.
     *
     * @param messageLite watch-next secondary content
     * @return whether the item section contains related videos
     */
    public static boolean isRelatedItems(MessageLite messageLite) {
        try {
            SecondaryContents secondaryContents = SecondaryContents.parseFrom(messageLite.toByteArray());
            if (secondaryContents.hasItemSectionRenderer()) {
                String sectionIdentifier = secondaryContents
                        .getItemSectionRenderer()
                        .getSectionIdentifier();

                if (sectionIdentifier != null && sectionIdentifier.startsWith(COMMENTS)) {
                    return false;
                }

                isFiltered = true;
                return true;
            }
        } catch (Exception ex) {
            Logger.printException(() -> "Failed to parse ItemSectionRenderer", ex);
        }

        return false;
    }

    /**
     * Identifies tablet shelf renderers, which contain related videos.
     *
     * @param messageLite watch-next secondary content
     * @return whether the content is a shelf renderer
     */
    public static boolean isShelfRenderer(MessageLite messageLite) {
        try {
            SecondaryContents secondaryContents = SecondaryContents.parseFrom(messageLite.toByteArray());
            boolean hasShelfRenderer = secondaryContents.hasShelfRenderer();
            if (hasShelfRenderer) {
                isFiltered = true;
            }

            return hasShelfRenderer;
        } catch (Exception ex) {
            Logger.printException(() -> "Failed to parse ShelfRenderer", ex);
        }

        return false;
    }

    /**
     * @return whether the current response had related content removed
     */
    public static boolean isFiltered() {
        return isFiltered;
    }
}

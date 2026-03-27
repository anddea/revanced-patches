package app.morphe.extension.music.sponsorblock;

import static app.morphe.extension.shared.utils.StringRef.str;

import android.graphics.Canvas;
import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Objects;

import app.morphe.extension.music.settings.Settings;
import app.morphe.extension.music.shared.VideoInformation;
import app.morphe.extension.music.sponsorblock.objects.CategoryBehaviour;
import app.morphe.extension.music.sponsorblock.objects.SponsorSegment;
import app.morphe.extension.music.sponsorblock.requests.SBRequester;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

/**
 * Handles showing, scheduling, and skipping of all {@link SponsorSegment} for the current video.
 * <p>
 * Class is not thread safe. All methods must be called on the main thread unless otherwise specified.
 */
@SuppressWarnings("unused")
public class SegmentPlaybackController {
    @Nullable
    private static String currentVideoId;
    @Nullable
    private static SponsorSegment[] segments;
    /**
     * Currently playing (non-highlight) segment that user can manually skip.
     */
    @Nullable
    private static SponsorSegment segmentCurrentlyPlaying;
    /**
     * Currently playing manual skip segment that is scheduled to hide.
     * This will always be NULL or equal to {@link #segmentCurrentlyPlaying}.
     */
    @Nullable
    private static SponsorSegment scheduledHideSegment;
    /**
     * Upcoming segment that is scheduled to either autoskip or show the manual skip button.
     */
    @Nullable
    private static SponsorSegment scheduledUpcomingSegment;
    /**
     * System time (in milliseconds) of when to hide the skip button of {@link #segmentCurrentlyPlaying}.
     * Value is zero if playback is not inside a segment ({@link #segmentCurrentlyPlaying} is null),
     */
    private static long skipSegmentButtonEndTime;

    private static int sponsorBarAbsoluteLeft;
    private static int sponsorAbsoluteBarRight;
    private static int sponsorBarThickness = 7;
    private static SponsorSegment lastSegmentSkipped;
    private static long lastSegmentSkippedTime;
    private static int toastNumberOfSegmentsSkipped;
    @Nullable
    private static SponsorSegment toastSegmentSkipped;

    private static void setSegments(@NonNull SponsorSegment[] videoSegments) {
        Arrays.sort(videoSegments);
        segments = videoSegments;
    }

    /**
     * Clears all downloaded data.
     */
    private static void clearData() {
        SponsorBlockSettings.initialize();
        currentVideoId = null;
        segments = null;
        segmentCurrentlyPlaying = null;
        scheduledUpcomingSegment = null;
        scheduledHideSegment = null;
        skipSegmentButtonEndTime = 0;
        toastSegmentSkipped = null;
        toastNumberOfSegmentsSkipped = 0;
    }

    /**
     * Injection point.
     */
    public static void setVideoId(@NonNull String videoId) {
        try {
            if (Objects.equals(currentVideoId, videoId)) {
                return;
            }
            clearData();
            if (!Settings.SB_ENABLED.get()) {
                return;
            }
            if (!Utils.isNetworkConnected()) {
                Logger.printDebug(() -> "Network not connected, ignoring video");
                return;
            }

            currentVideoId = videoId;
            Logger.printDebug(() -> "setCurrentVideoId: " + videoId);

            Utils.runOnBackgroundThread(() -> {
                try {
                    executeDownloadSegments(videoId);
                } catch (Exception e) {
                    Logger.printException(() -> "Failed to download segments", e);
                }
            });
        } catch (Exception ex) {
            Logger.printException(() -> "setCurrentVideoId failure", ex);
        }
    }

    /**
     * Must be called off main thread
     */
    static void executeDownloadSegments(@NonNull String videoId) {
        Objects.requireNonNull(videoId);
        try {
            SponsorSegment[] segments = SBRequester.getSegments(videoId);

            Utils.runOnMainThread(() -> {
                if (!videoId.equals(currentVideoId)) {
                    // user changed videos before get segments network call could complete
                    Logger.printDebug(() -> "Ignoring segments for prior video: " + videoId);
                    return;
                }
                setSegments(segments);

                // check for any skips now, instead of waiting for the next update to setVideoTime()
                setVideoTime(VideoInformation.getVideoTime());
            });
        } catch (Exception ex) {
            Logger.printException(() -> "executeDownloadSegments failure", ex);
        }
    }

    /**
     * Injection point.
     * Updates SponsorBlock every 1000ms.
     * When changing videos, this is first called with value 0 and then the video is changed.
     */
    public static void setVideoTime(long millis) {
        try {
            if (!Settings.SB_ENABLED.get() || segments == null || segments.length == 0) {
                return;
            }
            Logger.printDebug(() -> "setVideoTime: " + millis);

            final float playbackSpeed = VideoInformation.getPlaybackSpeed();
            // Amount of time to look ahead for the next segment,
            // and the threshold to determine if a scheduled show/hide is at the correct video time when it's run.
            //
            // This value must be greater than largest time between calls to this method (1000ms),
            // and must be adjusted for the video speed.
            //
            // To debug the stale skip logic, set this to a very large value (5000 or more)
            // then try manually seeking just before playback reaches a segment skip.
            final long speedAdjustedTimeThreshold = (long) (playbackSpeed * 1200);
            final long startTimerLookAheadThreshold = millis + speedAdjustedTimeThreshold;

            SponsorSegment foundSegmentCurrentlyPlaying = null;
            SponsorSegment foundUpcomingSegment = null;

            for (final SponsorSegment segment : segments) {
                if (segment.category.behaviour == CategoryBehaviour.IGNORE) {
                    continue;
                }
                if (segment.end <= millis) {
                    continue; // past this segment
                }

                if (segment.start <= millis) {
                    // we are in the segment!
                    if (segment.shouldAutoSkip()) {
                        skipSegment(segment);
                        return; // must return, as skipping causes a recursive call back into this method
                    }

                    // first found segment, or it's an embedded segment and fully inside the outer segment
                    if (foundSegmentCurrentlyPlaying == null || foundSegmentCurrentlyPlaying.containsSegment(segment)) {
                        // If the found segment is not currently displayed, then do not show if the segment is nearly over.
                        // This check prevents the skip button text from rapidly changing when multiple segments end at nearly the same time.
                        // Also prevents showing the skip button if user seeks into the last 800ms of the segment.
                        final long minMillisOfSegmentRemainingThreshold = 800;
                        if (segmentCurrentlyPlaying == segment
                                || !segment.endIsNear(millis, minMillisOfSegmentRemainingThreshold)) {
                            foundSegmentCurrentlyPlaying = segment;
                        } else {
                            Logger.printDebug(() -> "Ignoring segment that ends very soon: " + segment);
                        }
                    }
                    // Keep iterating and looking. There may be an upcoming autoskip,
                    // or there may be another smaller segment nested inside this segment
                    continue;
                }

                // segment is upcoming
                if (startTimerLookAheadThreshold < segment.start) {
                    break; // segment is not close enough to schedule, and no segments after this are of interest
                }
                if (segment.shouldAutoSkip()) { // upcoming autoskip
                    foundUpcomingSegment = segment;
                    break; // must stop here
                }

                // upcoming manual skip

                // do not schedule upcoming segment, if it is not fully contained inside the current segment
                if ((foundSegmentCurrentlyPlaying == null || foundSegmentCurrentlyPlaying.containsSegment(segment))
                        // use the most inner upcoming segment
                        && (foundUpcomingSegment == null || foundUpcomingSegment.containsSegment(segment))) {

                    // Only schedule, if the segment start time is not near the end time of the current segment.
                    // This check is needed to prevent scheduled hide and show from clashing with each other.
                    // Instead the upcoming segment will be handled when the current segment scheduled hide calls back into this method.
                    final long minTimeBetweenStartEndOfSegments = 1000;
                    if (foundSegmentCurrentlyPlaying == null
                            || !foundSegmentCurrentlyPlaying.endIsNear(segment.start, minTimeBetweenStartEndOfSegments)) {
                        foundUpcomingSegment = segment;
                    } else {
                        Logger.printDebug(() -> "Not scheduling segment (start time is near end of current segment): " + segment);
                    }
                }
            }

            if (segmentCurrentlyPlaying != foundSegmentCurrentlyPlaying) {
                setSegmentCurrentlyPlaying(foundSegmentCurrentlyPlaying);
            } else if (foundSegmentCurrentlyPlaying != null
                    && skipSegmentButtonEndTime != 0 && skipSegmentButtonEndTime <= System.currentTimeMillis()) {
                Logger.printDebug(() -> "Auto hiding skip button for segment: " + segmentCurrentlyPlaying);
                skipSegmentButtonEndTime = 0;
            }

            // schedule a hide, only if the segment end is near
            final SponsorSegment segmentToHide =
                    (foundSegmentCurrentlyPlaying != null && foundSegmentCurrentlyPlaying.endIsNear(millis, speedAdjustedTimeThreshold))
                            ? foundSegmentCurrentlyPlaying
                            : null;

            if (scheduledHideSegment != segmentToHide) {
                if (segmentToHide == null) {
                    Logger.printDebug(() -> "Clearing scheduled hide: " + scheduledHideSegment);
                    scheduledHideSegment = null;
                } else {
                    scheduledHideSegment = segmentToHide;
                    Logger.printDebug(() -> "Scheduling hide segment: " + segmentToHide + " playbackSpeed: " + playbackSpeed);
                    final long delayUntilHide = (long) ((segmentToHide.end - millis) / playbackSpeed);
                    Utils.runOnMainThreadDelayed(() -> {
                        if (scheduledHideSegment != segmentToHide) {
                            Logger.printDebug(() -> "Ignoring old scheduled hide segment: " + segmentToHide);
                            return;
                        }
                        scheduledHideSegment = null;

                        final long videoTime = VideoInformation.getVideoTime();
                        if (!segmentToHide.endIsNear(videoTime, speedAdjustedTimeThreshold)) {
                            // current video time is not what's expected.  User paused playback
                            Logger.printDebug(() -> "Ignoring outdated scheduled hide: " + segmentToHide
                                    + " videoInformation time: " + videoTime);
                            return;
                        }
                        Logger.printDebug(() -> "Running scheduled hide segment: " + segmentToHide);
                        // Need more than just hide the skip button, as this may have been an embedded segment
                        // Instead call back into setVideoTime to check everything again.
                        // Should not use VideoInformation time as it is less accurate,
                        // but this scheduled handler was scheduled precisely so we can just use the segment end time
                        setSegmentCurrentlyPlaying(null);
                        setVideoTime(segmentToHide.end);
                    }, delayUntilHide);
                }
            }

            if (scheduledUpcomingSegment != foundUpcomingSegment) {
                if (foundUpcomingSegment == null) {
                    Logger.printDebug(() -> "Clearing scheduled segment: " + scheduledUpcomingSegment);
                    scheduledUpcomingSegment = null;
                } else {
                    scheduledUpcomingSegment = foundUpcomingSegment;
                    final SponsorSegment segmentToSkip = foundUpcomingSegment;

                    Logger.printDebug(() -> "Scheduling segment: " + segmentToSkip + " playbackSpeed: " + playbackSpeed);
                    final long delayUntilSkip = (long) ((segmentToSkip.start - millis) / playbackSpeed);
                    Utils.runOnMainThreadDelayed(() -> {
                        if (scheduledUpcomingSegment != segmentToSkip) {
                            Logger.printDebug(() -> "Ignoring old scheduled segment: " + segmentToSkip);
                            return;
                        }
                        scheduledUpcomingSegment = null;

                        final long videoTime = VideoInformation.getVideoTime();
                        if (!segmentToSkip.startIsNear(videoTime, speedAdjustedTimeThreshold)) {
                            // current video time is not what's expected.  User paused playback
                            Logger.printDebug(() -> "Ignoring outdated scheduled segment: " + segmentToSkip
                                    + " videoInformation time: " + videoTime);
                            return;
                        }
                        if (segmentToSkip.shouldAutoSkip()) {
                            Logger.printDebug(() -> "Running scheduled skip segment: " + segmentToSkip);
                            skipSegment(segmentToSkip);
                        } else {
                            Logger.printDebug(() -> "Running scheduled show segment: " + segmentToSkip);
                            setSegmentCurrentlyPlaying(segmentToSkip);
                        }
                    }, delayUntilSkip);
                }
            }
        } catch (Exception e) {
            Logger.printException(() -> "setVideoTime failure", e);
        }
    }

    private static void setSegmentCurrentlyPlaying(@Nullable SponsorSegment segment) {
        if (segment == null) {
            if (segmentCurrentlyPlaying != null)
                Logger.printDebug(() -> "Hiding segment: " + segmentCurrentlyPlaying);
            segmentCurrentlyPlaying = null;
            skipSegmentButtonEndTime = 0;
            return;
        }
        segmentCurrentlyPlaying = segment;
        skipSegmentButtonEndTime = 0;
        Logger.printDebug(() -> "Showing segment: " + segment);
    }

    private static void skipSegment(@NonNull SponsorSegment segmentToSkip) {
        try {
            // If trying to seek to end of the video, YouTube can seek just before of the actual end.
            // (especially if the video does not end on a whole second boundary).
            // This causes additional segment skip attempts, even though it cannot seek any closer to the desired time.
            // Check for and ignore repeated skip attempts of the same segment over a small time period.
            final long now = System.currentTimeMillis();
            final long minimumMillisecondsBetweenSkippingSameSegment = 500;
            if ((lastSegmentSkipped == segmentToSkip) && (now - lastSegmentSkippedTime < minimumMillisecondsBetweenSkippingSameSegment)) {
                Logger.printDebug(() -> "Ignoring skip segment request (already skipped as close as possible): " + segmentToSkip);
                return;
            }

            Logger.printDebug(() -> "Skipping segment: " + segmentToSkip);
            lastSegmentSkipped = segmentToSkip;
            lastSegmentSkippedTime = now;
            setSegmentCurrentlyPlaying(null);
            scheduledHideSegment = null;
            scheduledUpcomingSegment = null;

            // If the seek is successful, then the seek causes a recursive call back into this class.
            final boolean seekSuccessful = VideoInformation.seekTo(segmentToSkip.end);
            if (!seekSuccessful) {
                // can happen when switching videos and is normal
                Logger.printDebug(() -> "Could not skip segment (seek unsuccessful): " + segmentToSkip);
                return;
            }

            // check for any smaller embedded segments, and count those as autoskipped
            final boolean showSkipToast = Settings.SB_TOAST_ON_SKIP.get();
            for (final SponsorSegment otherSegment : Objects.requireNonNull(segments)) {
                if (segmentToSkip.end < otherSegment.start) {
                    break; // no other segments can be contained
                }
                if (otherSegment == segmentToSkip ||
                        segmentToSkip.containsSegment(otherSegment)) {
                    otherSegment.didAutoSkipped = true;
                    // Do not show a toast if the user is scrubbing thru a paused video.
                    // Cannot do this video state check in setTime or earlier in this method, as the video state may not be up to date.
                    // So instead, only hide toasts because all other skip logic done while paused causes no harm.
                    if (showSkipToast) {
                        showSkippedSegmentToast(otherSegment);
                    }
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "skipSegment failure", ex);
        }
    }

    private static void showSkippedSegmentToast(@NonNull SponsorSegment segment) {
        Utils.verifyOnMainThread();
        toastNumberOfSegmentsSkipped++;
        if (toastNumberOfSegmentsSkipped > 1) {
            return; // toast already scheduled
        }
        toastSegmentSkipped = segment;

        final long delayToToastMilliseconds = 250; // also the maximum time between skips to be considered skipping multiple segments
        Utils.runOnMainThreadDelayed(() -> {
            try {
                if (toastSegmentSkipped == null) { // video was changed just after skipping segment
                    Logger.printDebug(() -> "Ignoring old scheduled show toast");
                    return;
                }
                Utils.showToastShort(toastNumberOfSegmentsSkipped == 1
                        ? toastSegmentSkipped.getSkippedToastText()
                        : str("revanced_sb_skipped_multiple_segments"));
            } catch (Exception ex) {
                Logger.printException(() -> "showSkippedSegmentToast failure", ex);
            } finally {
                toastNumberOfSegmentsSkipped = 0;
                toastSegmentSkipped = null;
            }
        }, delayToToastMilliseconds);
    }

    /**
     * Injection point
     */
    public static void setSponsorBarRect(final Object self, final String fieldName) {
        if (Settings.SB_ENABLED.get()) {
            try {
                Field field = self.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                Rect rect = (Rect) Objects.requireNonNull(field.get(self));
                setSponsorBarAbsoluteLeft(rect);
                setSponsorBarAbsoluteRight(rect);
            } catch (Exception ex) {
                Logger.printException(() -> "setSponsorBarRect failure", ex);
            }
        }
    }

    private static void setSponsorBarAbsoluteLeft(Rect rect) {
        final int left = rect.left;
        if (sponsorBarAbsoluteLeft != left) {
            sponsorBarAbsoluteLeft = left;
        }
    }

    private static void setSponsorBarAbsoluteRight(Rect rect) {
        final int right = rect.right;
        if (sponsorAbsoluteBarRight != right) {
            sponsorAbsoluteBarRight = right;
        }
    }

    /**
     * Injection point
     */
    public static void setSponsorBarThickness(int thickness) {
        if (Settings.SB_ENABLED.get() && sponsorBarThickness != thickness) {
            sponsorBarThickness = (int) Math.round(thickness * 1.2);
        }
    }

    /**
     * Injection point.
     */
    public static void drawSponsorTimeBars(final Canvas canvas, final float posY) {
        try {
            if (!Settings.SB_ENABLED.get() || segments == null) return;
            final long videoLength = VideoInformation.getVideoLength();
            if (videoLength <= 0) return;

            final int thicknessDiv2 = sponsorBarThickness / 2; // rounds down
            final float top = posY - (sponsorBarThickness - thicknessDiv2);
            final float bottom = posY + thicknessDiv2;
            final float videoMillisecondsToPixels = (1f / videoLength) * (sponsorAbsoluteBarRight - sponsorBarAbsoluteLeft);
            final float leftPadding = sponsorBarAbsoluteLeft;

            for (SponsorSegment segment : segments) {
                final float left = leftPadding + segment.start * videoMillisecondsToPixels;
                final float right = leftPadding + segment.end * videoMillisecondsToPixels;
                canvas.drawRect(left, top, right, bottom, segment.category.paint);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "drawSponsorTimeBars failure", ex);
        }
    }

}

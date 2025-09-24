package app.revanced.extension.youtube.sponsorblock;

import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.youtube.shared.RootView.isAdProgressTextVisible;
import static app.revanced.extension.youtube.utils.VideoUtils.getFormattedTimeStamp;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.shared.PlayerControlsVisibility;
import app.revanced.extension.youtube.shared.PlayerType;
import app.revanced.extension.youtube.shared.VideoInformation;
import app.revanced.extension.youtube.shared.VideoState;
import app.revanced.extension.youtube.sponsorblock.objects.CategoryBehaviour;
import app.revanced.extension.youtube.sponsorblock.objects.SegmentCategory;
import app.revanced.extension.youtube.sponsorblock.objects.SponsorSegment;
import app.revanced.extension.youtube.sponsorblock.requests.SBRequester;
import app.revanced.extension.youtube.sponsorblock.ui.SponsorBlockViewController;
import app.revanced.extension.youtube.whitelist.Whitelist;

/**
 * Handles showing, scheduling, and skipping of all {@link SponsorSegment} for the current video.
 * <p>
 * Class is not thread safe. All methods must be called on the main thread unless otherwise specified.
 */
@SuppressWarnings("unused")
public class SegmentPlaybackController {
    /**
     * Length of time to show a skip button for a highlight segment,
     * or a regular segment if {@link Settings#SB_AUTO_HIDE_SKIP_BUTTON} is enabled.
     * <p>
     * Effectively this value is rounded up to the next second.
     */
    private static final long DURATION_TO_SHOW_SKIP_BUTTON = 3800;

    /**
     * Highlight segments have zero length as they are a point in time.
     * Draw them on screen using a fixed width bar.
     * Value is independent of device dpi.
     */
    private static final int HIGHLIGHT_SEGMENT_DRAW_BAR_WIDTH = 7;
    /**
     * Used to prevent re-showing a previously hidden skip button when exiting an embedded segment.
     * Only used when {@link Settings#SB_AUTO_HIDE_SKIP_BUTTON} is enabled.
     * <p>
     * A collection of segments that have automatically hidden the skip button for, and all segments in this list
     * contain the current video time.  Segment are removed when playback exits the segment.
     */
    private static final List<SponsorSegment> hiddenSkipSegmentsForCurrentVideoTime = new ArrayList<>();
    @NonNull
    private static String videoId = "";
    private static long videoLength = 0;

    @Nullable
    private static SponsorSegment[] segments;
    /**
     * Highlight segment, if one exists and the skip behavior is not set to {@link CategoryBehaviour#SHOW_IN_SEEKBAR}.
     */
    @Nullable
    private static SponsorSegment highlightSegment;
    /**
     * Because loading can take time, show the skip to highlight for a few seconds after the segments load.
     * This is the system time (in milliseconds) to no longer show the initial display skip to highlight.
     * Value will be zero if no highlight segment exists, or if the system time to show the highlight has passed.
     */
    private static long highlightSegmentInitialShowEndTime;
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
     * or if {@link Settings#SB_AUTO_HIDE_SKIP_BUTTON} is not enabled.
     */
    private static long skipSegmentButtonEndTime;

    @Nullable
    private static String timeWithoutSegments;

    private static int sponsorBarAbsoluteLeft;
    private static int sponsorAbsoluteBarRight;
    private static int sponsorBarThickness;
    private static SponsorSegment lastSegmentSkipped;
    private static long lastSegmentSkippedTime;
    private static int toastNumberOfSegmentsSkipped;
    @Nullable
    private static SponsorSegment toastSegmentSkipped;

    @Nullable
    static SponsorSegment[] getSegments() {
        return segments;
    }

    private static void setSegments(@NonNull SponsorSegment[] videoSegments) {
        Arrays.sort(videoSegments);
        segments = videoSegments;
        calculateTimeWithoutSegments();

        if (SegmentCategory.HIGHLIGHT.behaviour == CategoryBehaviour.SKIP_AUTOMATICALLY
                || SegmentCategory.HIGHLIGHT.behaviour == CategoryBehaviour.MANUAL_SKIP) {
            for (SponsorSegment segment : videoSegments) {
                if (segment.category == SegmentCategory.HIGHLIGHT) {
                    highlightSegment = segment;
                    return;
                }
            }
        }
        highlightSegment = null;
    }

    static void addUnsubmittedSegment(@NonNull SponsorSegment segment) {
        Objects.requireNonNull(segment);
        if (segments == null) {
            segments = new SponsorSegment[1];
        } else {
            segments = Arrays.copyOf(segments, segments.length + 1);
        }
        segments[segments.length - 1] = segment;
        setSegments(segments);
    }

    static void removeUnsubmittedSegments() {
        if (segments == null || segments.length == 0) {
            return;
        }
        List<SponsorSegment> replacement = new ArrayList<>();
        for (SponsorSegment segment : segments) {
            if (segment.category != SegmentCategory.UNSUBMITTED) {
                replacement.add(segment);
            }
        }
        if (replacement.size() != segments.length) {
            setSegments(replacement.toArray(new SponsorSegment[0]));
        }
    }

    public static boolean videoHasSegments() {
        return segments != null && segments.length > 0;
    }

    /**
     * Clears all downloaded data.
     */
    public static void clearData() {
        videoId = "";
        videoLength = 0;
        segments = null;
        highlightSegment = null;
        highlightSegmentInitialShowEndTime = 0;
        timeWithoutSegments = null;
        segmentCurrentlyPlaying = null;
        scheduledUpcomingSegment = null;
        scheduledHideSegment = null;
        skipSegmentButtonEndTime = 0;
        toastSegmentSkipped = null;
        toastNumberOfSegmentsSkipped = 0;
        hiddenSkipSegmentsForCurrentVideoTime.clear();
    }

    /**
     * Injection point.
     * Initializes SponsorBlock when the video player starts playing a new video.
     */
    public static void initialize() {
        try {
            Utils.verifyOnMainThread();
            SponsorBlockSettings.initialize();
            clearData();
            SponsorBlockViewController.hideAll();
            SponsorBlockUtils.clearUnsubmittedSegmentTimes();
            Logger.printDebug(() -> "Initialized SponsorBlock");
        } catch (Exception ex) {
            Logger.printException(() -> "Failed to initialize SponsorBlock", ex);
        }
    }

    /**
     * Injection point.
     */
    public static void newVideoStarted(@NonNull String newlyLoadedChannelId, @NonNull String newlyLoadedChannelName,
                                       @NonNull String newlyLoadedVideoId, @NonNull String newlyLoadedVideoTitle,
                                       final long newlyLoadedVideoLength, boolean newlyLoadedLiveStreamValue) {
        try {
            if (Objects.equals(videoId, newlyLoadedVideoId)) {
                return;
            }
            clearData();
            if (!Settings.SB_ENABLED.get()) {
                return;
            }
            if (PlayerType.getCurrent().isNoneOrHidden()) {
                Logger.printDebug(() -> "ignoring Short");
                return;
            }
            if (!Utils.isNetworkConnected()) {
                Logger.printDebug(() -> "Network not connected, ignoring video");
                return;
            }

            videoId = newlyLoadedVideoId;
            videoLength = newlyLoadedVideoLength;
            Logger.printDebug(() -> "newVideoStarted: " + newlyLoadedVideoId);

            if (Whitelist.isChannelWhitelistedSponsorBlock(newlyLoadedChannelId)) {
                return;
            }

            Utils.runOnBackgroundThread(() -> {
                try {
                    executeDownloadSegments(newlyLoadedVideoId);
                } catch (Exception e) {
                    Logger.printException(() -> "Failed to download segments", e);
                }
            });
        } catch (Exception ex) {
            Logger.printException(() -> "setCurrentVideoId failure", ex);
        }
    }

    /**
     * Id of the last video opened.  Includes Shorts.
     *
     * @return The id of the video, or an empty string if no videos have been opened yet.
     */
    @NonNull
    public static String getVideoId() {
        return videoId;
    }

    /**
     * Length of the current video playing. Includes Shorts.
     *
     * @return The length of the video in milliseconds.
     * If the video is not yet loaded, or if the video is playing in the background with no video visible,
     * then this returns zero.
     */
    public static long getVideoLength() {
        return videoLength;
    }

    /**
     * Must be called off main thread
     */
    static void executeDownloadSegments(@NonNull String newlyLoadedVideoId) {
        Objects.requireNonNull(newlyLoadedVideoId);
        Utils.verifyOffMainThread();
        try {
            SponsorSegment[] segments = SBRequester.getSegments(newlyLoadedVideoId);

            Utils.runOnMainThread(() -> {
                if (!newlyLoadedVideoId.equals(videoId)) {
                    // user changed videos before get segments network call could complete
                    Logger.printDebug(() -> "Ignoring segments for prior video: " + newlyLoadedVideoId);
                    return;
                }
                setSegments(segments);

                final long videoTime = VideoInformation.getVideoTime();
                if (highlightSegment != null) {
                    // If the current video time is before the highlight.
                    final long timeUntilHighlight = highlightSegment.start - videoTime;
                    if (timeUntilHighlight > 0) {
                        if (highlightSegment.shouldAutoSkip()) {
                            skipSegment(highlightSegment, false);
                            return;
                        }
                        highlightSegmentInitialShowEndTime = System.currentTimeMillis() + Math.min(
                                (long) (timeUntilHighlight / VideoInformation.getPlaybackSpeed()),
                                DURATION_TO_SHOW_SKIP_BUTTON);
                    }
                }

                // check for any skips now, instead of waiting for the next update to setVideoTime()
                setVideoTime(videoTime);
            });
        } catch (Exception ex) {
            Logger.printException(() -> "executeDownloadSegments failure", ex);
        }
    }

    /**
     * Injection point.
     * Updates SponsorBlock every 100ms.
     * When changing videos, this is first called with value 0 and then the video is changed.
     */
    public static void setVideoTime(long millis) {
        try {
            if (!Settings.SB_ENABLED.get()
                    || PlayerType.getCurrent().isNoneOrHidden() // Shorts playback.
                    || segments == null || segments.length == 0
                    || isAdProgressTextVisible()) {
                return;
            }
            Logger.printDebug(() -> "setVideoTime: " + getFormattedTimeStamp(millis));

            updateHiddenSegments(millis);

            final float playbackSpeed = VideoInformation.getPlaybackSpeed();
            // Amount of time to look ahead for the next segment,
            // and the threshold to determine if a scheduled show/hide is at the correct video time when it's run.
            //
            // This value must be greater than largest time between calls to this method (1000ms),
            // and must be adjusted for the video speed.
            //
            // To debug the stale skip logic, set this to a very large value (5000 or more)
            // then try manually seeking just before playback reaches a segment skip.
            final long speedAdjustedTimeThreshold = (long) (playbackSpeed * 1000);
            final long startTimerLookAheadThreshold = millis + speedAdjustedTimeThreshold;

            SponsorSegment foundSegmentCurrentlyPlaying = null;
            SponsorSegment foundUpcomingSegment = null;

            for (final SponsorSegment segment : segments) {
                if (segment.category.behaviour == CategoryBehaviour.SHOW_IN_SEEKBAR
                        || segment.category.behaviour == CategoryBehaviour.IGNORE
                        || segment.category == SegmentCategory.HIGHLIGHT) {
                    continue;
                }
                if (segment.end <= millis) {
                    continue; // past this segment
                }

                if (segment.start <= millis) {
                    // we are in the segment!
                    if (segment.shouldAutoSkip()) {
                        skipSegment(segment, false);
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

            if (highlightSegment != null) {
                if (millis < DURATION_TO_SHOW_SKIP_BUTTON || (highlightSegmentInitialShowEndTime != 0
                        && System.currentTimeMillis() < highlightSegmentInitialShowEndTime)) {
                    SponsorBlockViewController.showSkipHighlightButton(highlightSegment);
                } else {
                    highlightSegmentInitialShowEndTime = 0;
                    SponsorBlockViewController.hideSkipHighlightButton();
                }
            }

            if (segmentCurrentlyPlaying != foundSegmentCurrentlyPlaying) {
                setSegmentCurrentlyPlaying(foundSegmentCurrentlyPlaying);
            } else if (foundSegmentCurrentlyPlaying != null
                    && skipSegmentButtonEndTime != 0 && skipSegmentButtonEndTime <= System.currentTimeMillis()) {
                Logger.printDebug(() -> "Auto hiding skip button for segment: " + segmentCurrentlyPlaying);
                skipSegmentButtonEndTime = 0;
                hiddenSkipSegmentsForCurrentVideoTime.add(foundSegmentCurrentlyPlaying);
                SponsorBlockViewController.hideSkipSegmentButton();
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
                        if (VideoState.getCurrent() != VideoState.PLAYING) {
                            Logger.printDebug(() -> "Ignoring scheduled hide segment as video is paused: " + segmentToHide);
                            return;
                        }

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
                        if (VideoState.getCurrent() != VideoState.PLAYING) {
                            Logger.printDebug(() -> "Ignoring scheduled hide segment as video is paused: " + segmentToSkip);
                            return;
                        }

                        final long videoTime = VideoInformation.getVideoTime();
                        if (!segmentToSkip.startIsNear(videoTime, speedAdjustedTimeThreshold)) {
                            // current video time is not what's expected.  User paused playback
                            Logger.printDebug(() -> "Ignoring outdated scheduled segment: " + segmentToSkip
                                    + " videoInformation time: " + videoTime);
                            return;
                        }
                        if (segmentToSkip.shouldAutoSkip()) {
                            Logger.printDebug(() -> "Running scheduled skip segment: " + segmentToSkip);
                            skipSegment(segmentToSkip, false);
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

    /**
     * Removes all previously hidden segments that are not longer contained in the given video time.
     */
    private static void updateHiddenSegments(long currentVideoTime) {
        // If you want to maintain compatibility with RVX Android 6, use Iterator.
        hiddenSkipSegmentsForCurrentVideoTime.removeIf(segment -> {
            if (!segment.containsTime(currentVideoTime)) {
                Logger.printDebug(() -> "Resetting hide skip button: " + segment);
                return true;
            }
            return false;
        });
    }

    private static void setSegmentCurrentlyPlaying(@Nullable SponsorSegment segment) {
        if (segment == null) {
            if (segmentCurrentlyPlaying != null) {
                Logger.printDebug(() -> "Hiding segment: " + segmentCurrentlyPlaying);
            }
            segmentCurrentlyPlaying = null;
            skipSegmentButtonEndTime = 0;
            SponsorBlockViewController.hideSkipSegmentButton();
            return;
        }
        segmentCurrentlyPlaying = segment;
        skipSegmentButtonEndTime = 0;
        if (Settings.SB_AUTO_HIDE_SKIP_BUTTON.get()) {
            if (hiddenSkipSegmentsForCurrentVideoTime.contains(segment)) {
                // Playback exited a nested segment and the outer segment skip button was previously hidden.
                Logger.printDebug(() -> "Ignoring previously auto-hidden segment: " + segment);
                SponsorBlockViewController.hideSkipSegmentButton();
                return;
            }
            skipSegmentButtonEndTime = System.currentTimeMillis() + DURATION_TO_SHOW_SKIP_BUTTON;
        }
        Logger.printDebug(() -> "Showing segment: " + segment);
        SponsorBlockViewController.showSkipSegmentButton(segment);
    }

    /**
     * Injection point.
     */
    public static void setVisibility(boolean visible, boolean animated) {
        onPlayerControlsVisibilityChanged(visible, false);
    }

    /**
     * Injection point.
     */
    public static void setVisibilityImmediate(boolean visible) {
        onPlayerControlsVisibilityChanged(visible, true);
    }

    /**
     * Injection point.
     */
    public static void setVisibilityNegatedImmediate() {
        if (PlayerControlsVisibility.getCurrent() == PlayerControlsVisibility.PLAYER_CONTROLS_VISIBILITY_HIDDEN)
            onPlayerControlsVisibilityChanged(false, true);
    }

    /**
     * Handles changes in player control visibility and manages the skip segment button accordingly.
     *
     * <p>This method is called whenever the visibility state of the player controls changes.
     * If auto-hide is enabled and there is a currently playing sponsor segment, it will show
     * the skip segment button when the controls are visible and schedule recursive checks
     * to hide the button after a defined duration.</p>
     *
     * @param visible   if true, player controls are visible (The user touched the player when the player controls were invisible)
     * @param immediate if true, player controls are invisible (The user touched the player when the player controls were visible)
     */
    private static void onPlayerControlsVisibilityChanged(boolean visible, boolean immediate) {
        if (!Settings.SB_ENABLED.get()
                || !Settings.SB_AUTO_HIDE_SKIP_BUTTON.get()
                || segmentCurrentlyPlaying == null
                || isAdProgressTextVisible()
                // When the player button appears after the skip button is hidden
                || !hiddenSkipSegmentsForCurrentVideoTime.contains(segmentCurrentlyPlaying)) {
            return;
        }

        if (visible) {
            SponsorBlockViewController.showSkipSegmentButton(segmentCurrentlyPlaying);
            skipSegmentButtonEndTime = System.currentTimeMillis() + 2000; // Player buttons are hidden after 2000ms
            checkPlayerControlsVisibilityRecursive(segmentCurrentlyPlaying);
        } else if (immediate) {
            // Hide the skip segment button and reset the end time
            skipSegmentButtonEndTime = 0;
            SponsorBlockViewController.hideSkipSegmentButton();
        }
    }

    /**
     * Recursively checks whether the skip segment button should remain visible or be hidden.
     *
     * <p>This method continues checking at a fixed interval (500 milliseconds) if the button
     * should be hidden. The recursion stops if the current segment changes or the duration
     * to show the button has expired.</p>
     *
     * @param segment the sponsor segment associated with the current check
     */
    private static void checkPlayerControlsVisibilityRecursive(SponsorSegment segment) {
        if (skipSegmentButtonEndTime == 0
                // Stop recursion if the current segment has changed
                || segment != segmentCurrentlyPlaying) {
            return;
        }

        // Continue recursion if the button's visibility duration has not expired
        if (skipSegmentButtonEndTime > System.currentTimeMillis()) {
            Utils.runOnMainThreadDelayed(() -> checkPlayerControlsVisibilityRecursive(segment), 500);
        } else {
            // Hide the skip segment button and reset the end time
            skipSegmentButtonEndTime = 0;
            hiddenSkipSegmentsForCurrentVideoTime.add(segment);
            SponsorBlockViewController.hideSkipSegmentButton();
        }
    }

    private static void skipSegment(@NonNull SponsorSegment segmentToSkip, boolean userManuallySkipped) {
        try {
            SponsorBlockViewController.hideSkipHighlightButton();
            SponsorBlockViewController.hideSkipSegmentButton();

            final long now = System.currentTimeMillis();
            if (lastSegmentSkipped == segmentToSkip) {
                // If trying to seek to end of the video, YouTube can seek just before of the actual end.
                // (especially if the video does not end on a whole second boundary).
                // This causes additional segment skip attempts, even though it cannot seek any closer to the desired time.
                // Check for and ignore repeated skip attempts of the same segment over a small time period.
                final long minTimeBetweenSkippingSameSegment = Math.max(500, (long) (500 / VideoInformation.getPlaybackSpeed()));
                if (now - lastSegmentSkippedTime < minTimeBetweenSkippingSameSegment) {
                    Logger.printDebug(() -> "Ignoring skip segment request (already skipped as close as possible): " + segmentToSkip);
                    return;
                }
            }

            Logger.printDebug(() -> "Skipping segment: " + segmentToSkip);
            lastSegmentSkipped = segmentToSkip;
            lastSegmentSkippedTime = now;
            setSegmentCurrentlyPlaying(null);
            scheduledHideSegment = null;
            scheduledUpcomingSegment = null;
            if (segmentToSkip == highlightSegment) {
                highlightSegmentInitialShowEndTime = 0;
            }

            // If the seek is successful, then the seek causes a recursive call back into this class.
            final boolean seekSuccessful = VideoInformation.seekTo(segmentToSkip.end, getVideoLength());
            if (!seekSuccessful) {
                // can happen when switching videos and is normal
                Logger.printDebug(() -> "Could not skip segment (seek unsuccessful): " + segmentToSkip);
                return;
            }

            final boolean videoIsPaused = VideoState.getCurrent() == VideoState.PAUSED;
            if (!userManuallySkipped) {
                // check for any smaller embedded segments, and count those as autoskipped
                final boolean showSkipToast = Settings.SB_TOAST_ON_SKIP.get();
                for (SponsorSegment otherSegment : Objects.requireNonNull(segments)) {
                    if (otherSegment.end <= segmentToSkip.start) {
                        // Other segment does not overlap, and is before this skipped segment.
                        // This situation can only happen if a video is opened and adjusted to
                        // a later time in the video where earlier auto skip segments
                        // have not been encountered yet.
                        continue;
                    }
                    if (segmentToSkip.end <= otherSegment.start) {
                        break; // no other segments can be contained
                    }
                    if (otherSegment == segmentToSkip ||
                            (otherSegment.category != SegmentCategory.HIGHLIGHT && segmentToSkip.containsSegment(otherSegment))) {
                        otherSegment.didAutoSkipped = true;
                        // Do not show a toast if the user is scrubbing thru a paused video.
                        // Cannot do this video state check in setTime or earlier in this method, as the video state may not be up to date.
                        // So instead, only hide toasts because all other skip logic done while paused causes no harm.
                        if (showSkipToast && !videoIsPaused) {
                            showSkippedSegmentToast(otherSegment);
                        }
                    }
                }
            }

            if (segmentToSkip.category == SegmentCategory.UNSUBMITTED) {
                removeUnsubmittedSegments();
                SponsorBlockUtils.setNewSponsorSegmentPreviewed();
            } else if (!videoIsPaused) {
                SponsorBlockUtils.sendViewRequestAsync(segmentToSkip);
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
     * @param segment can be either a highlight or a regular manual skip segment.
     */
    public static void onSkipSegmentClicked(@NonNull SponsorSegment segment) {
        try {
            if (segment != highlightSegment && segment != segmentCurrentlyPlaying) {
                Logger.printException(() -> "error: segment not available to skip"); // should never happen
                SponsorBlockViewController.hideSkipSegmentButton();
                SponsorBlockViewController.hideSkipHighlightButton();
                return;
            }
            skipSegment(segment, true);
        } catch (Exception ex) {
            Logger.printException(() -> "onSkipSegmentClicked failure", ex);
        }
    }

    /**
     * Injection point
     */
    public static void setSponsorBarRect(final Object self) {
        if (Settings.SB_ENABLED.get()) {
            try {
                Field field = self.getClass().getDeclaredField("replaceMeWithsetSponsorBarRect");
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
            sponsorBarThickness = thickness;
        }
    }

    /**
     * Injection point.
     */
    public static String appendTimeWithoutSegments(String totalTime) {
        try {
            if (Settings.SB_ENABLED.get() && Settings.SB_VIDEO_LENGTH_WITHOUT_SEGMENTS.get()
                    && !StringUtils.isAnyEmpty(totalTime, timeWithoutSegments)
                    && !isAdProgressTextVisible()) {
                // Force LTR layout, to match the same LTR video time/length layout YouTube uses for all languages
                return "\u202D" + totalTime + timeWithoutSegments; // u202D = left to right override
            }
        } catch (Exception ex) {
            Logger.printException(() -> "appendTimeWithoutSegments failure", ex);
        }

        return totalTime;
    }

    @SuppressLint("DefaultLocale")
    private static void calculateTimeWithoutSegments() {
        if (!Settings.SB_VIDEO_LENGTH_WITHOUT_SEGMENTS.get() || videoLength <= 0
                || segments == null || segments.length == 0) {
            timeWithoutSegments = null;
            return;
        }

        boolean foundNonhighlightSegments = false;
        long timeWithoutSegmentsValue = videoLength;

        for (int i = 0, length = segments.length; i < length; i++) {
            SponsorSegment segment = segments[i];
            if (segment.category == SegmentCategory.HIGHLIGHT) {
                continue;
            }
            foundNonhighlightSegments = true;
            long start = segment.start;
            final long end = segment.end;
            // To prevent nested segments from incorrectly counting additional time,
            // check if the segment overlaps any earlier segments.
            for (int j = 0; j < i; j++) {
                start = Math.max(start, segments[j].end);
            }
            if (start < end) {
                timeWithoutSegmentsValue -= (end - start);
            }
        }

        if (!foundNonhighlightSegments) {
            timeWithoutSegments = null;
            return;
        }

        final long hours = timeWithoutSegmentsValue / 3600000;
        final long minutes = (timeWithoutSegmentsValue / 60000) % 60;
        final long seconds = (timeWithoutSegmentsValue / 1000) % 60;
        if (hours > 0) {
            timeWithoutSegments = String.format(Locale.ENGLISH, "\u2009(%d:%02d:%02d)", hours, minutes, seconds);
        } else {
            timeWithoutSegments = String.format(Locale.ENGLISH, "\u2009(%d:%02d)", minutes, seconds);
        }
    }

    /**
     * Actual screen pixel width to use for the highlight segment time bar.
     */
    private static final int highlightSegmentTimeBarScreenWidth
            = Utils.dipToPixels(HIGHLIGHT_SEGMENT_DRAW_BAR_WIDTH);

    /**
     * Injection point.
     */
    public static void drawSponsorTimeBars(final Canvas canvas, final float posY) {
        try {
            if (!Settings.SB_ENABLED.get()
                    || segments == null
                    || videoLength <= 0
                    || isAdProgressTextVisible()) {
                return;
            }

            final int thicknessDiv2 = sponsorBarThickness / 2; // rounds down
            final float top = posY - (sponsorBarThickness - thicknessDiv2);
            final float bottom = posY + thicknessDiv2;
            final float videoMillisecondsToPixels = (1f / videoLength) * (sponsorAbsoluteBarRight - sponsorBarAbsoluteLeft);
            final float leftPadding = sponsorBarAbsoluteLeft;

            for (SponsorSegment segment : segments) {
                final float left = leftPadding + segment.start * videoMillisecondsToPixels;
                final float right;
                if (segment.category == SegmentCategory.HIGHLIGHT) {
                    right = left + highlightSegmentTimeBarScreenWidth;
                } else {
                    right = leftPadding + segment.end * videoMillisecondsToPixels;
                }
                canvas.drawRect(left, top, right, bottom, segment.category.paint);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "drawSponsorTimeBars failure", ex);
        }
    }
}

package app.morphe.extension.music.sponsorblock.objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

import app.morphe.extension.music.shared.VideoInformation;

public class SponsorSegment implements Comparable<SponsorSegment> {
    @NonNull
    public final SegmentCategory category;
    /**
     * NULL if segment is unsubmitted
     */
    @Nullable
    public final String UUID;
    public final long start;
    public final long end;
    public final boolean isLocked;
    public boolean didAutoSkipped = false;

    public SponsorSegment(@NonNull SegmentCategory category, @Nullable String UUID, long start, long end, boolean isLocked) {
        this.category = category;
        this.UUID = UUID;
        this.start = start;
        this.end = end;
        this.isLocked = isLocked;
    }

    public boolean shouldAutoSkip() {
        return category.behaviour.skipAutomatically;
    }

    /**
     * @param nearThreshold threshold to declare the time parameter is near this segment. Must be a positive number
     */
    public boolean startIsNear(long videoTime, long nearThreshold) {
        return Math.abs(start - videoTime) <= nearThreshold;
    }

    /**
     * @param nearThreshold threshold to declare the time parameter is near this segment. Must be a positive number
     */
    public boolean endIsNear(long videoTime, long nearThreshold) {
        return Math.abs(end - videoTime) <= nearThreshold;
    }

    /**
     * @return if the segment is completely contained inside this segment
     */
    public boolean containsSegment(SponsorSegment other) {
        return start <= other.start && other.end <= end;
    }

    /**
     * @return the length of this segment, in milliseconds.  Always a positive number.
     */
    public long length() {
        return end - start;
    }

    /**
     * @return 'skipped segment' toast message
     */
    @NonNull
    public String getSkippedToastText() {
        return category.getSkippedToastText(start, VideoInformation.getVideoLength()).toString();
    }

    @Override
    public int compareTo(SponsorSegment o) {
        // If both segments start at the same time, then sort with the longer segment first.
        // This keeps the seekbar drawing correct since it draws the segments using the sorted order.
        return start == o.start ? Long.compare(o.length(), length()) : Long.compare(start, o.start);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SponsorSegment other)) return false;
        return Objects.equals(UUID, other.UUID)
                && category == other.category
                && start == other.start
                && end == other.end;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(UUID);
    }

    @NonNull
    @Override
    public String toString() {
        return "SponsorSegment{"
                + "category=" + category
                + ", start=" + start
                + ", end=" + end
                + '}';
    }
}

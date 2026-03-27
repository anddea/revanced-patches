package app.morphe.extension.shared.patches.components;

import app.morphe.extension.shared.settings.BooleanSetting;
import app.morphe.extension.shared.utils.ByteTrieSearch;
import app.morphe.extension.shared.utils.Logger;

/**
 * If you have more than 1 filter patterns, then all instances of
 * this class should filtered using {@link ByteArrayFilterGroupList#check(byte[])},
 * which uses a prefix tree to give better performance.
 */
@SuppressWarnings("unused")
public class ByteArrayFilterGroup extends FilterGroup<byte[]> {

    private volatile int[][] failurePatterns;

    // Modified implementation from https://stackoverflow.com/a/1507813
    private static int indexOf(final byte[] data, final byte[] pattern, final int[] failure) {
        // Finds the first occurrence of the pattern in the byte array using
        // KMP matching algorithm.
        int patternLength = pattern.length;
        for (int i = 0, j = 0, dataLength = data.length; i < dataLength; i++) {
            while (j > 0 && pattern[j] != data[i]) {
                j = failure[j - 1];
            }
            if (pattern[j] == data[i]) {
                j++;
            }
            if (j == patternLength) {
                return i - patternLength + 1;
            }
        }
        return -1;
    }

    private static int[] createFailurePattern(byte[] pattern) {
        // Computes the failure function using a boot-strapping process,
        // where the pattern is matched against itself.
        final int patternLength = pattern.length;
        final int[] failure = new int[patternLength];

        for (int i = 1, j = 0; i < patternLength; i++) {
            while (j > 0 && pattern[j] != pattern[i]) {
                j = failure[j - 1];
            }
            if (pattern[j] == pattern[i]) {
                j++;
            }
            failure[i] = j;
        }
        return failure;
    }

    public ByteArrayFilterGroup(BooleanSetting setting, byte[]... filters) {
        super(setting, filters);
    }

    /**
     * Converts the Strings into byte arrays. Used to search for text in binary data.
     */
    public ByteArrayFilterGroup(BooleanSetting setting, String... filters) {
        super(setting, ByteTrieSearch.convertStringsToBytes(filters));
    }

    private synchronized void buildFailurePatterns() {
        if (failurePatterns != null)
            return; // Thread race and another thread already initialized the search.
        Logger.printDebug(() -> "Building failure array for: " + this);
        int[][] failurePatterns = new int[filters.length][];
        int i = 0;
        for (byte[] pattern : filters) {
            failurePatterns[i++] = createFailurePattern(pattern);
        }
        this.failurePatterns = failurePatterns; // Must set after initialization finishes.
    }

    @Override
    public FilterGroupResult check(final byte[] bytes) {
        int matchedLength = 0;
        int matchedIndex = -1;
        if (isEnabled()) {
            int[][] failures = failurePatterns;
            if (failures == null) {
                buildFailurePatterns(); // Lazy load.
                failures = failurePatterns;
            }
            for (int i = 0, length = filters.length; i < length; i++) {
                byte[] filter = filters[i];
                matchedIndex = indexOf(bytes, filter, failures[i]);
                if (matchedIndex >= 0) {
                    matchedLength = filter.length;
                    break;
                }
            }
        }
        return new FilterGroupResult(setting, matchedIndex, matchedLength);
    }
}

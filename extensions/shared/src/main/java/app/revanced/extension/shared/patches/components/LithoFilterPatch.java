package app.revanced.extension.shared.patches.components;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.ByteBuffer;
import java.util.List;

import app.revanced.extension.shared.settings.BaseSettings;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.StringTrieSearch;

@SuppressWarnings("unused")
public final class LithoFilterPatch {
    /**
     * Simple wrapper to pass the litho parameters through the prefix search.
     */
    private static final class LithoFilterParameters {
        @Nullable
        final String identifier;
        final String path;
        final String allValue;
        final byte[] protoBuffer;

        LithoFilterParameters(String lithoPath, @Nullable String lithoIdentifier, String allValues, byte[] bufferArray) {
            this.path = lithoPath;
            this.identifier = lithoIdentifier;
            this.allValue = allValues;
            this.protoBuffer = bufferArray;
        }

        @NonNull
        @Override
        public String toString() {
            // Estimate the percentage of the buffer that are Strings.
            StringBuilder builder = new StringBuilder(Math.max(100, protoBuffer.length / 2));
            builder.append("\nID: ");
            builder.append(identifier);
            builder.append("\nPath: ");
            builder.append(path);
            if (BaseSettings.ENABLE_DEBUG_BUFFER_LOGGING.get()) {
                builder.append("\nBufferStrings: ");
                findAsciiStrings(builder, protoBuffer);
            }

            return builder.toString();
        }

        /**
         * Search through a byte array for all ASCII strings.
         */
        private static void findAsciiStrings(StringBuilder builder, byte[] buffer) {
            // Valid ASCII values (ignore control characters).
            final int minimumAscii = 32;  // 32 = space character
            final int maximumAscii = 126; // 127 = delete character
            final int minimumAsciiStringLength = 4; // Minimum length of an ASCII string to include.
            String delimitingCharacter = "❙"; // Non ascii character, to allow easier log filtering.

            final int length = buffer.length;
            int start = 0;
            int end = 0;
            while (end < length) {
                int value = buffer[end];
                if (value < minimumAscii || value > maximumAscii || end == length - 1) {
                    if (end - start >= minimumAsciiStringLength) {
                        for (int i = start; i < end; i++) {
                            builder.append((char) buffer[i]);
                        }
                        builder.append(delimitingCharacter);
                    }
                    start = end + 1;
                }
                end++;
            }
        }
    }

    private static final Filter[] filters = new Filter[]{
            new DummyFilter() // Replaced by patch.
    };

    private static final StringTrieSearch pathSearchTree = new StringTrieSearch();
    private static final StringTrieSearch identifierSearchTree = new StringTrieSearch();
    private static final StringTrieSearch allValueSearchTree = new StringTrieSearch();

    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    /**
     * Because litho filtering is multi-threaded and the buffer is passed in from a different injection point,
     * the buffer is saved to a ThreadLocal so each calling thread does not interfere with other threads.
     */
    private static final ThreadLocal<ByteBuffer> bufferThreadLocal = new ThreadLocal<>();

    static {
        for (Filter filter : filters) {
            filterUsingCallbacks(identifierSearchTree, filter,
                    filter.identifierCallbacks, Filter.FilterContentType.IDENTIFIER);
            filterUsingCallbacks(pathSearchTree, filter,
                    filter.pathCallbacks, Filter.FilterContentType.PATH);
            filterUsingCallbacks(allValueSearchTree, filter,
                    filter.allValueCallbacks, Filter.FilterContentType.ALLVALUE);
        }

        Logger.printDebug(() -> "Using: "
                + identifierSearchTree.numberOfPatterns() + " identifier filters"
                + " (" + identifierSearchTree.getEstimatedMemorySize() + " KB), "
                + pathSearchTree.numberOfPatterns() + " path filters"
                + " (" + pathSearchTree.getEstimatedMemorySize() + " KB)");
    }

    private static void filterUsingCallbacks(StringTrieSearch pathSearchTree,
                                             Filter filter, List<StringFilterGroup> groups,
                                             Filter.FilterContentType type) {
        for (StringFilterGroup group : groups) {
            if (!group.includeInSearch()) {
                continue;
            }
            for (String pattern : group.filters) {
                pathSearchTree.addPattern(pattern, (textSearched, matchedStartIndex, matchedLength, callbackParameter) -> {
                            if (!group.isEnabled()) return false;
                            LithoFilterParameters parameters = (LithoFilterParameters) callbackParameter;
                            return filter.isFiltered(parameters.path, parameters.identifier, parameters.allValue, parameters.protoBuffer,
                                    group, type, matchedStartIndex);
                        }
                );
            }
        }
    }

    /**
     * Injection point.  Called off the main thread.
     */
    public static void setProtoBuffer(@Nullable ByteBuffer protobufBuffer) {
        // Set the buffer to a thread local.  The buffer will remain in memory, even after the call to #filter completes.
        // This is intentional, as it appears the buffer can be set once and then filtered multiple times.
        // The buffer will be cleared from memory after a new buffer is set by the same thread,
        // or when the calling thread eventually dies.
        bufferThreadLocal.set(protobufBuffer);
    }

    /**
     * Injection point.  Called off the main thread, and commonly called by multiple threads at the same time.
     */
    public static boolean filter(@NonNull StringBuilder pathBuilder, @Nullable String identifier, @NonNull Object object) {
        try {
            if (pathBuilder.length() == 0) {
                return false;
            }

            ByteBuffer protobufBuffer = bufferThreadLocal.get();
            final byte[] bufferArray;
            // Potentially the buffer may have been null or never set up until now.
            // Use an empty buffer so the litho id or path filters still work correctly.
            if (protobufBuffer == null) {
                Logger.printDebug(() -> "Proto buffer is null, using an empty buffer array");
                bufferArray = EMPTY_BYTE_ARRAY;
            } else if (!protobufBuffer.hasArray()) {
                Logger.printDebug(() -> "Proto buffer does not have an array, using an empty buffer array");
                bufferArray = EMPTY_BYTE_ARRAY;
            } else {
                bufferArray = protobufBuffer.array();
            }

            LithoFilterParameters parameter = new LithoFilterParameters(pathBuilder.toString(), identifier,
                    object.toString(), bufferArray);
            Logger.printDebug(() -> "Searching " + parameter);

            if (parameter.identifier != null && identifierSearchTree.matches(parameter.identifier, parameter)) {
                return true;
            }

            if (pathSearchTree.matches(parameter.path, parameter)) {
                return true;
            }

            if (allValueSearchTree.matches(parameter.allValue, parameter)) {
                return true;
            }
        } catch (Exception ex) {
            Logger.printException(() -> "Litho filter failure", ex);
        }

        return false;
    }
}

/**
 * Placeholder for actual filters.
 */
final class DummyFilter extends Filter {
}
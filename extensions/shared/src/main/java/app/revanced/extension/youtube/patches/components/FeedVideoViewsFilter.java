package app.revanced.extension.youtube.patches.components;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.revanced.extension.shared.patches.components.Filter;
import app.revanced.extension.shared.patches.components.StringFilterGroup;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.shared.NavigationBar;
import app.revanced.extension.youtube.shared.RootView;

@SuppressWarnings("all")
public final class FeedVideoViewsFilter extends Filter {

    private static final String ARROW = " -> ";
    private static final String VIEWS = "views";
    private final StringFilterGroup feedVideoFilter = new StringFilterGroup(
            null,
            "video_lockup_with_attachment.eml"
    );
    private final String[] parts = Settings.HIDE_VIDEO_VIEW_COUNTS_MULTIPLIER.get().split("\\n");
    private Pattern viewCountPattern = null;

    public FeedVideoViewsFilter() {
        addPathCallbacks(feedVideoFilter);
    }

    private boolean hideFeedVideoViewsSettingIsActive() {
        final boolean hideHome = Settings.HIDE_VIDEO_BY_VIEW_COUNTS_HOME.get();
        final boolean hideSearch = Settings.HIDE_VIDEO_BY_VIEW_COUNTS_SEARCH.get();
        final boolean hideSubscriptions = Settings.HIDE_VIDEO_BY_VIEW_COUNTS_SUBSCRIPTIONS.get();

        if (!hideHome && !hideSearch && !hideSubscriptions) {
            return false;
        } else if (hideHome && hideSearch && hideSubscriptions) {
            return true;
        }

        // Must check player type first, as search bar can be active behind the player.
        if (RootView.isPlayerActive()) {
            // For now, consider the under video results the same as the home feed.
            return hideHome;
        }

        // Must check second, as search can be from any tab.
        if (RootView.isSearchBarActive()) {
            return hideSearch;
        }

        NavigationBar.NavigationButton selectedNavButton = NavigationBar.NavigationButton.getSelectedNavigationButton();
        if (selectedNavButton == null) {
            return hideHome; // Unknown tab, treat the same as home.
        } else if (selectedNavButton == NavigationBar.NavigationButton.HOME) {
            return hideHome;
        } else if (selectedNavButton == NavigationBar.NavigationButton.SUBSCRIPTIONS) {
            return hideSubscriptions;
        }
        // User is in the Library or Notifications tab.
        return false;
    }

    @Override
    public boolean isFiltered(String path, @Nullable String identifier, String allValue, byte[] protobufBufferArray,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (hideFeedVideoViewsSettingIsActive() && filterByViews(protobufBufferArray)) {
            return super.isFiltered(path, identifier, allValue, protobufBufferArray, matchedGroup, contentType, contentIndex);
        }
        return false;
    }

    /**
     * Hide videos based on views count
     */
    private synchronized boolean filterByViews(byte[] protobufBufferArray) {
        final String protobufString = new String(protobufBufferArray);
        final long lessThan = Settings.HIDE_VIDEO_VIEW_COUNTS_LESS_THAN.get();
        final long greaterThan = Settings.HIDE_VIDEO_VIEW_COUNTS_GREATER_THAN.get();

        if (viewCountPattern == null) {
            viewCountPattern = getViewCountPattern(parts);
        }

        final Matcher matcher = viewCountPattern.matcher(protobufString);
        if (matcher.find()) {
            String numString = Objects.requireNonNull(matcher.group(1));
            double num = parseNumber(numString);
            String multiplierKey = matcher.group(2);
            long multiplierValue = getMultiplierValue(parts, multiplierKey);
            boolean shouldFilter = num * multiplierValue < lessThan || num * multiplierValue > greaterThan;

            final boolean finalShouldFilter = shouldFilter;
            Logger.printDebug(() -> {
                StringBuilder builder = new StringBuilder();
                builder.append("FeedVideoViewsFilter: Should Filter: ").append(finalShouldFilter);
                builder.append("\n").append(num * multiplierValue).append(" < ").append(lessThan);
                builder.append(" || ").append(num * multiplierValue).append(" > ").append(greaterThan);
                builder.append("\nRegex pattern: ").append(viewCountPattern.pattern());
                builder.append("\nText: ").append(matcher.group(0));
                return builder.toString();
            });
            return shouldFilter;
        }
        return false;
    }

    private synchronized double parseNumber(String numString) {
        /**
         * Some languages have comma (,) as a decimal separator.
         * In order to detect those numbers as doubles in Java
         * we convert commas (,) to dots (.).
         * Unless we find a language that has commas used in
         * a different manner, it should work.
         */
        numString = numString.replace(",", ".");

        /**
         * Some languages have dot (.) as a kilo separator.
         * So we check with regex if there is a number with 3+
         * digits after dot (.), we replace it with nothing
         * to make Java understand the number as a whole.
         */
        if (numString.matches("\\d+\\.\\d{3,}")) {
            numString = numString.replace(".", "");
        }
        return Double.parseDouble(numString);
    }

    private synchronized Pattern getViewCountPattern(String[] parts) {
        // Regex: (\d+[.,]?\d*)\s?(K|M|B)?(\u200F\u202C)?\s*views
        StringBuilder prefixPatternBuilder = new StringBuilder("(\\d+[.,]?\\d*)\\s?(");
        StringBuilder suffixBuilder = getSuffixBuilder(parts, prefixPatternBuilder);

        prefixPatternBuilder.deleteCharAt(prefixPatternBuilder.length() - 1); // Remove the trailing |
        prefixPatternBuilder.append(")?(\\u200F\\u202C)?\\s*");
        prefixPatternBuilder.append(suffixBuilder.toString());

        Pattern pattern = Pattern.compile(prefixPatternBuilder.toString());
        Logger.printDebug(() -> "FeedVideoViewsFilter: pattern: " + pattern.pattern());
        return pattern;
    }


    @NonNull
    private synchronized StringBuilder getSuffixBuilder(String[] parts, StringBuilder prefixPatternBuilder) {
        StringBuilder suffixBuilder = new StringBuilder();

        for (String part : parts) {
            final String[] pair = part.split(ARROW);
            if (pair.length != 2) {
                Logger.printDebug(() -> "FeedVideoViewsFilter: Invalid multiplier setting: " + part);
                continue; // Skip invalid entries
            }
            final String pair0 = pair[0].trim();
            final String pair1 = pair[1].trim();

            if (!pair1.equals(VIEWS)) {
                prefixPatternBuilder.append(pair0).append("|");
            } else {
                suffixBuilder.append(pair0);
            }
        }
        return suffixBuilder;
    }

    private synchronized long getMultiplierValue(String[] parts, String multiplier) {
        if (multiplier == null || multiplier.isEmpty()) {
            return 1L;
        }
        for (String part : parts) {
            final String[] pair = part.split(ARROW);
            if (pair.length != 2) {
                Logger.printDebug(() -> "FeedVideoViewsFilter: Invalid multiplier setting: " + part);
                continue; // Skip invalid entries
            }
            final String pair0 = pair[0].trim();
            final String pair1 = pair[1].trim();


            if (pair0.equals(multiplier) && !pair1.equals(VIEWS)) {
                try {
                    return Long.parseLong(pair1.replaceAll("[^\\d]", ""));
                } catch (NumberFormatException e) {
                    Logger.printException(() -> "Error parsing multiplier value for " + multiplier + ": " + pair1, e);
                    return 1L; // Default value on error
                }
            }
        }
        return 1L; // Default value if not found
    }
}

package app.morphe.extension.youtube.patches.components;

import androidx.annotation.NonNull;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.morphe.extension.shared.patches.components.Filter;
import app.morphe.extension.shared.patches.components.StringFilterGroup;
import app.morphe.extension.shared.settings.StringSetting;
import app.morphe.extension.shared.utils.ResourceUtils;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.NavigationBar;
import app.morphe.extension.youtube.shared.RootView;

@SuppressWarnings("all")
public final class FeedVideoViewsFilter extends Filter {

    public FeedVideoViewsFilter() {

        addPathCallbacks(
                new StringFilterGroup(
                        null,
                        "video_lockup_with_attachment."
                )
        );
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
    public boolean isFiltered(String path, String identifier, String allValue, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        return hideFeedVideoViewsSettingIsActive() &&
                filterByViews(buffer);
    }

    private static final String ARROW = " -> ";
    private static final String VIEWS = "views";
    private static final StringSetting HIDE_VIDEO_VIEW_COUNTS_MULTIPLIER =
            Settings.HIDE_VIDEO_VIEW_COUNTS_MULTIPLIER;
    private static final String HIDE_VIDEO_VIEW_COUNTS_MULTIPLIER_DEFAULT_VALUE =
            "revanced_hide_video_view_counts_multiplier_default_value";
    private static final String[] parts;
    private Pattern[] viewCountPatterns = null;

    static {
        final String multiplierString = HIDE_VIDEO_VIEW_COUNTS_MULTIPLIER.get();
        if (multiplierString.equals(HIDE_VIDEO_VIEW_COUNTS_MULTIPLIER_DEFAULT_VALUE) &&
                ResourceUtils.getContext() != null) {
            String defaultValue = ResourceUtils.getString(HIDE_VIDEO_VIEW_COUNTS_MULTIPLIER_DEFAULT_VALUE);
            if (!multiplierString.equals(defaultValue)) {
                HIDE_VIDEO_VIEW_COUNTS_MULTIPLIER.save(defaultValue);
            }
        }
        parts = HIDE_VIDEO_VIEW_COUNTS_MULTIPLIER.get().split("\\n");
    }

    /**
     * Hide videos based on views count
     */
    private synchronized boolean filterByViews(byte[] buffer) {
        final String protobufString = new String(buffer);
        final long lessThan = Settings.HIDE_VIDEO_VIEW_COUNTS_LESS_THAN.get();
        final long greaterThan = Settings.HIDE_VIDEO_VIEW_COUNTS_GREATER_THAN.get();

        if (viewCountPatterns == null) {
            viewCountPatterns = getViewCountPatterns(parts);
        }

        for (Pattern pattern : viewCountPatterns) {
            final Matcher matcher = pattern.matcher(protobufString);
            if (matcher.find()) {
                String numString = Objects.requireNonNull(matcher.group(1));
                double num = parseNumber(numString);
                String multiplierKey = matcher.group(2);
                long multiplierValue = getMultiplierValue(parts, multiplierKey);
                return num * multiplierValue < lessThan || num * multiplierValue > greaterThan;
            }
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

    private synchronized Pattern[] getViewCountPatterns(String[] parts) {
        StringBuilder prefixPatternBuilder = new StringBuilder("(\\d+(?:[.,]\\d+)?)\\s?("); // LTR layout
        StringBuilder secondPatternBuilder = new StringBuilder(); // RTL layout
        StringBuilder suffixBuilder = getSuffixBuilder(parts, prefixPatternBuilder, secondPatternBuilder);

        prefixPatternBuilder.deleteCharAt(prefixPatternBuilder.length() - 1); // Remove the trailing |
        prefixPatternBuilder.append(")?\\s*");
        prefixPatternBuilder.append(suffixBuilder.length() > 0 ? suffixBuilder.toString() : VIEWS);

        secondPatternBuilder.deleteCharAt(secondPatternBuilder.length() - 1); // Remove the trailing |
        secondPatternBuilder.append(")?");

        final Pattern[] patterns = new Pattern[2];
        patterns[0] = Pattern.compile(prefixPatternBuilder.toString());
        patterns[1] = Pattern.compile(secondPatternBuilder.toString());

        return patterns;
    }

    @NonNull
    private synchronized StringBuilder getSuffixBuilder(String[] parts, StringBuilder prefixPatternBuilder, StringBuilder secondPatternBuilder) {
        StringBuilder suffixBuilder = new StringBuilder();

        for (String part : parts) {
            final String[] pair = part.split(ARROW);
            final String pair0 = pair[0].trim();
            final String pair1 = pair[1].trim();

            if (pair.length == 2 && !pair1.equals(VIEWS)) {
                prefixPatternBuilder.append(pair0).append("|");
            }

            if (pair.length == 2 && pair1.equals(VIEWS)) {
                suffixBuilder.append(pair0);
                secondPatternBuilder.append(pair0).append("\\s*").append(prefixPatternBuilder);
            }
        }
        return suffixBuilder;
    }

    private synchronized long getMultiplierValue(String[] parts, String multiplier) {
        for (String part : parts) {
            final String[] pair = part.split(ARROW);
            final String pair0 = pair[0].trim();
            final String pair1 = pair[1].trim();

            if (pair.length == 2 && pair0.equals(multiplier) && !pair1.equals(VIEWS)) {
                return Long.parseLong(pair[1].replaceAll("[^\\d]", ""));
            }
        }

        return 1L; // Default value if not found
    }
}

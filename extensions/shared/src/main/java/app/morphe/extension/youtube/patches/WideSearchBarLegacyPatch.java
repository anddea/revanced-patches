package app.morphe.extension.youtube.patches;

import android.view.View;
import android.widget.RelativeLayout;

import app.morphe.extension.shared.utils.ResourceUtils;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class WideSearchBarLegacyPatch {

    private static final boolean wideSearchbarEnabled = Settings.ENABLE_WIDE_SEARCH_BAR.get();
    // Loads the search bar deprecated by Google.
    private static final boolean wideSearchbarWithHeaderEnabled = Settings.ENABLE_WIDE_SEARCH_BAR_WITH_HEADER.get();
    private static final boolean wideSearchbarYouTabEnabled = Settings.ENABLE_WIDE_SEARCH_BAR_IN_YOU_TAB.get();

    public static boolean enableWideSearchBar(boolean original) {
        return wideSearchbarEnabled || original;
    }

    /**
     * Limitation: Premium header will not be applied for YouTube Premium users if the user uses the 'Wide search bar with header' option.
     * This is because it forces the deprecated search bar to be loaded.
     * As a solution to this limitation, 'Change YouTube header' patch is required.
     */
    public static boolean enableWideSearchBarWithHeader(boolean original) {
        if (!wideSearchbarEnabled)
            return original;
        else
            return wideSearchbarWithHeaderEnabled || original;
    }

    public static boolean enableWideSearchBarWithHeaderInverse(boolean original) {
        if (!wideSearchbarEnabled)
            return original;
        else
            return !wideSearchbarWithHeaderEnabled && original;
    }

    public static boolean enableWideSearchBarInYouTab(boolean original) {
        if (!wideSearchbarEnabled)
            return original;
        else
            return !wideSearchbarYouTabEnabled && original;
    }

    private static int searchBarId = 0;
    private static int youtubeTextId = 0;
    private static int searchBoxId = 0;
    private static int searchIconId = 0;

    public static void setWideSearchBarLayout(View view) {
        if (!wideSearchbarEnabled)
            return;

        if (searchBarId == 0) {
            searchBarId = ResourceUtils.getIdIdentifier("search_bar");
        }

        if (!(view.findViewById(searchBarId) instanceof RelativeLayout searchBarView))
            return;

        // When the deprecated search bar is loaded, two search bars overlap.
        // Manually hides another search bar.
        if (wideSearchbarWithHeaderEnabled) {
            if (youtubeTextId == 0) {
                youtubeTextId = ResourceUtils.getIdIdentifier("youtube_text");
                searchBoxId = ResourceUtils.getIdIdentifier("search_box");
                searchIconId = ResourceUtils.getIdIdentifier("search_icon");
            }
            final View searchIconView = searchBarView.findViewById(searchIconId);
            final View searchBoxView = searchBarView.findViewById(searchBoxId);
            final View textView = searchBarView.findViewById(youtubeTextId);
            if (textView != null) {
                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(0, 0);
                layoutParams.setMargins(0, 0, 0, 0);
                textView.setLayoutParams(layoutParams);
            }
            // The search icon in the deprecated search bar is clickable, but onClickListener is not assigned.
            // Assign onClickListener and disable the effect when clicked.
            if (searchIconView != null && searchBoxView != null) {
                searchIconView.setOnClickListener(view1 -> searchBoxView.callOnClick());
                searchIconView.getBackground().setAlpha(0);
            }
        } else {
            // This is the legacy method - Wide search bar without YouTube header.
            // Since the padding start is 0, it does not look good.
            // Add a padding start of 8.0 dip.
            final int paddingLeft = searchBarView.getPaddingLeft();
            final int paddingRight = searchBarView.getPaddingRight();
            final int paddingTop = searchBarView.getPaddingTop();
            final int paddingBottom = searchBarView.getPaddingBottom();
            final int paddingStart = Utils.dipToPixels(8f);

            // In RelativeLayout, paddingStart cannot be assigned programmatically.
            // Check RTL layout and set left padding or right padding.
            if (Utils.isRightToLeftLocale()) {
                searchBarView.setPadding(paddingLeft, paddingTop, paddingStart, paddingBottom);
            } else {
                searchBarView.setPadding(paddingStart, paddingTop, paddingRight, paddingBottom);
            }
        }
    }
}

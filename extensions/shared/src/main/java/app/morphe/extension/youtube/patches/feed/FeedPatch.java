package app.morphe.extension.youtube.patches.feed;

import static app.morphe.extension.shared.utils.Utils.hideViewBy0dpUnderCondition;
import static app.morphe.extension.shared.utils.Utils.hideViewUnderCondition;
import static app.morphe.extension.youtube.utils.ExtendedUtils.IS_20_10_OR_GREATER;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class FeedPatch {

    // region [Hide feed components] patch

    public static int hideCategoryBarInFeed(final int height) {
        return Settings.HIDE_CATEGORY_BAR_IN_FEED.get() ? 0 : height;
    }

    public static boolean hideCategoryBarInRelatedVideos() {
        return Settings.HIDE_CATEGORY_BAR_IN_RELATED_VIDEOS.get();
    }

    /**
     * Whether to enforce a minimum height for subviews of the category bar.
     * This experimental flag was added in YouTube 20.10+ and must be true to hide the category bar without leaving any gaps.
     * Even if the experimental flag is removed in the future, this value will remain true.
     */
    public static boolean hideCategoryBarInRelatedVideos(boolean original) {
        return hideCategoryBarInRelatedVideos() || original;
    }

    public static void hideCategoryBarInRelatedVideos(final View chipView) {
        if (hideCategoryBarInRelatedVideos()) {
            // If the minimum height is 0, we can hide the view with setVisibility instead of changing LayoutParams.
            // Minimum height is only available in YouTube 20.10+.
            // In earlier versions, hide the view using LayoutParams.
            if (IS_20_10_OR_GREATER) {
                chipView.setVisibility(View.GONE);

                // If you want to hide a View with LayoutParams, use the code enclosed in comments.

                /*
                chipView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                    try {
                        if (chipView instanceof ViewGroup viewGroup &&
                                viewGroup.getParent() instanceof ViewGroup parentView) {
                            if (parentView.getId() == ResourceUtils.getIdIdentifier("fullscreen_watch_next_chips") &&
                                    viewGroup.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                                Utils.hideViewGroupByMarginLayoutParams(viewGroup);
                            } else {
                                viewGroup.setVisibility(View.GONE);
                            }
                        }
                    } catch (Exception ex) {
                        Logger.printException(() -> "hideCategoryBarInRelatedVideos failed.", ex);
                    }
                });
                 */

            } else {
                Utils.hideViewByLayoutParams(chipView);
            }
        }
    }

    /**
     * The minimum height to apply to subviews in the category bar.
     * This method is only available in YouTube 20.10+.
     */
    public static int hideCategoryBarInRelatedVideos(final int height) {
        return hideCategoryBarInRelatedVideos() ? 0 : height;
    }

    public static int hideCategoryBarInSearch(final int height) {
        return Settings.HIDE_CATEGORY_BAR_IN_SEARCH.get() ? 0 : height;
    }

    /**
     * Rather than simply hiding the channel tab view, completely removes channel tab from list.
     * If a channel tab is removed from the list, users will not be able to open it by swiping.
     *
     * @param channelTabText Text to be assigned to channel tab, such as 'Shorts', 'Playlists', 'Community', 'Store'.
     *                       This text is hardcoded, so it follows the user's language.
     * @return Whether to remove the channel tab from the list.
     */
    public static boolean hideChannelTab(String channelTabText) {
        if (!Settings.HIDE_CHANNEL_TAB.get()) {
            return false;
        }
        if (channelTabText == null || channelTabText.isEmpty()) {
            return false;
        }

        String[] blockList = Settings.HIDE_CHANNEL_TAB_FILTER_STRINGS.get().split("\\n");

        for (String filter : blockList) {
            if (!filter.isEmpty() && channelTabText.equals(filter)) {
                return true;
            }
        }

        return false;
    }

    public static void hideBreakingNewsShelf(View view) {
        hideViewBy0dpUnderCondition(
                Settings.HIDE_CAROUSEL_SHELF_HOME.get(),
                view
        );
    }

    public static View hideCaptionsButton(View view) {
        return Settings.HIDE_FEED_CAPTIONS_BUTTON.get() ? null : view;
    }

    public static void hideCaptionsButtonContainer(View view) {
        hideViewUnderCondition(
                Settings.HIDE_FEED_CAPTIONS_BUTTON,
                view
        );
    }

    public static String hideFloatingButton(String fab) {
        return Settings.HIDE_FLOATING_BUTTON.get() ? null : fab;
    }

    public static void hideLatestVideosButton(View view) {
        hideViewUnderCondition(Settings.HIDE_LATEST_VIDEOS_BUTTON.get(), view);
    }

    public static boolean hideSubscriptionsChannelSection() {
        return Settings.HIDE_SUBSCRIBED_CHANNELS_BAR.get();
    }

    public static void hideSubscriptionsChannelSection(View view) {
        hideViewBy0dpUnderCondition(Settings.HIDE_SUBSCRIBED_CHANNELS_BAR, view);
    }

    private static FrameLayout.LayoutParams layoutParams;
    private static int minimumHeight = -1;
    private static int paddingLeft = 12;
    private static int paddingTop = 0;
    private static int paddingRight = 12;
    private static int paddingBottom = 0;

    /**
     * expandButtonContainer is used in channel profiles as well as search results.
     * We need to hide expandButtonContainer only in search results, not in channel profile.
     * <p>
     * If we hide expandButtonContainer with setVisibility, the empty space occupied by expandButtonContainer will still be left.
     * Therefore, we need to dynamically resize the View with LayoutParams.
     * <p>
     * Unlike other Views, expandButtonContainer cannot make a View invisible using the normal {@link Utils#hideViewByLayoutParams} method.
     * We should set the parent view's padding and MinimumHeight to 0 to completely hide the expandButtonContainer.
     *
     * @param parentView Parent view of expandButtonContainer.
     */
    public static void hideShowMoreButton(View parentView) {
        if (!Settings.HIDE_SHOW_MORE_BUTTON.get())
            return;

        if (!(parentView instanceof ViewGroup viewGroup))
            return;

        if (!(viewGroup.getChildAt(0) instanceof ViewGroup expandButtonContainer))
            return;

        if (layoutParams == null) {
            // We need to get the original LayoutParams and paddings applied to expandButtonContainer.
            // Theses are used to make the expandButtonContainer visible again.
            if (expandButtonContainer.getLayoutParams() instanceof FrameLayout.LayoutParams lp) {
                layoutParams = lp;
                paddingLeft = parentView.getPaddingLeft();
                paddingTop = parentView.getPaddingTop();
                paddingRight = parentView.getPaddingRight();
                paddingBottom = parentView.getPaddingBottom();
            }
        }

        // I'm not sure if 'Utils.runOnMainThreadDelayed' is absolutely necessary.
        Utils.runOnMainThreadDelayed(() -> {
                    // MinimumHeight is also needed to make expandButtonContainer visible again.
                    // Get original MinimumHeight.
                    if (minimumHeight == -1) {
                        minimumHeight = parentView.getMinimumHeight();
                    }

                    // In the search results, the child view structure of expandButtonContainer is as follows:
                    // expandButtonContainer
                    //   L TextView (first child view is SHOWN, 'Show more' text)
                    //   L ImageView (second child view is shown, dropdown arrow icon)

                    // In the channel profiles, the child view structure of expandButtonContainer is as follows:
                    // expandButtonContainer
                    //   L TextView (first child view is HIDDEN, 'Show more' text)
                    //   L ImageView (second child view is shown, dropdown arrow icon)

                    if (expandButtonContainer.getChildAt(0).getVisibility() != View.VISIBLE && layoutParams != null) {
                        // If the first child view (TextView) is HIDDEN, the channel profile is open.
                        // Restore parent view's padding and MinimumHeight to make them visible.
                        parentView.setMinimumHeight(minimumHeight);
                        parentView.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
                        expandButtonContainer.setLayoutParams(layoutParams);
                    } else {
                        // If the first child view (TextView) is SHOWN, the search results is open.
                        // Set the parent view's padding and MinimumHeight to 0 to completely hide the expandButtonContainer.
                        parentView.setMinimumHeight(0);
                        parentView.setPadding(0, 0, 0, 0);
                        expandButtonContainer.setLayoutParams(new FrameLayout.LayoutParams(0, 0));
                    }
                }, 0
        );
    }

    // endregion

    // region [Hide feed flyout menu] patch

    /**
     * hide feed flyout menu for phone
     *
     * @param menuTitleCharSequence menu title
     */
    @Nullable
    public static CharSequence hideFlyoutMenu(@Nullable CharSequence menuTitleCharSequence) {
        if (menuTitleCharSequence != null && Settings.HIDE_FEED_FLYOUT_MENU.get()) {
            String[] blockList = Settings.HIDE_FEED_FLYOUT_MENU_FILTER_STRINGS.get().split("\\n");
            String menuTitleString = menuTitleCharSequence.toString();

            for (String filter : blockList) {
                if (!filter.isEmpty()) {
                    if (Settings.HIDE_FEED_FLYOUT_MENU_FILTER_TYPE.get()) {
                        if (menuTitleString.contains(filter))
                            return null;
                    } else {
                        if (menuTitleString.equals(filter))
                            return null;
                    }
                }
            }
        }

        return menuTitleCharSequence;
    }

    /**
     * hide feed flyout panel for tablet
     *
     * @param menuTextView          flyout text view
     * @param menuTitleCharSequence raw text
     */
    public static void hideFlyoutMenu(TextView menuTextView, CharSequence menuTitleCharSequence) {
        if (menuTitleCharSequence == null || !Settings.HIDE_FEED_FLYOUT_MENU.get())
            return;

        if (!(menuTextView.getParent() instanceof View parentView))
            return;

        String[] blockList = Settings.HIDE_FEED_FLYOUT_MENU_FILTER_STRINGS.get().split("\\n");
        String menuTitleString = menuTitleCharSequence.toString();

        for (String filter : blockList) {
            if (menuTitleString.equals(filter) && !filter.isEmpty())
                Utils.hideViewByLayoutParams(parentView);
        }
    }

    // endregion

}

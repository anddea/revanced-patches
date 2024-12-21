package app.revanced.extension.youtube.patches.general;

import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.shared.utils.Utils.getChildView;
import static app.revanced.extension.shared.utils.Utils.hideViewByLayoutParams;
import static app.revanced.extension.shared.utils.Utils.hideViewGroupByMarginLayoutParams;
import static app.revanced.extension.shared.utils.Utils.hideViewUnderCondition;
import static app.revanced.extension.shared.utils.Utils.isSDKAbove;
import static app.revanced.extension.youtube.patches.utils.PatchStatus.ImageSearchButton;
import static app.revanced.extension.youtube.shared.NavigationBar.NavigationButton;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.apps.youtube.app.application.Shell_SettingsActivity;
import com.google.android.apps.youtube.app.settings.SettingsActivity;
import com.google.android.apps.youtube.app.settings.videoquality.VideoQualitySettingsActivity;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.ResourceUtils;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.utils.ThemeUtils;

@SuppressWarnings("unused")
public class GeneralPatch {

    // region [Disable auto audio tracks] patch

    private static final String DEFAULT_AUDIO_TRACKS_IDENTIFIER = "original";
    private static ArrayList<Object> formatStreamModelArray;

    /**
     * Find the stream format containing the parameter {@link GeneralPatch#DEFAULT_AUDIO_TRACKS_IDENTIFIER}, and save to the array.
     *
     * @param formatStreamModel stream format model including audio tracks.
     */
    public static void setFormatStreamModelArray(final Object formatStreamModel) {
        if (!Settings.DISABLE_AUTO_AUDIO_TRACKS.get()) {
            return;
        }

        // Ignoring, as the stream format model array has already been added.
        if (formatStreamModelArray != null) {
            return;
        }

        // Ignoring, as it is not an original audio track.
        if (!formatStreamModel.toString().contains(DEFAULT_AUDIO_TRACKS_IDENTIFIER)) {
            return;
        }

        // For some reason, when YouTube handles formatStreamModelArray,
        // it uses an array with duplicate values at the first and second indices.
        formatStreamModelArray = new ArrayList<>();
        formatStreamModelArray.add(formatStreamModel);
        formatStreamModelArray.add(formatStreamModel);
    }

    /**
     * Returns an array of stream format models containing the default audio tracks.
     *
     * @param localizedFormatStreamModelArray stream format model array consisting of audio tracks in the system's language.
     * @return stream format model array consisting of original audio tracks.
     */
    public static ArrayList<Object> getFormatStreamModelArray(final ArrayList<Object> localizedFormatStreamModelArray) {
        if (!Settings.DISABLE_AUTO_AUDIO_TRACKS.get()) {
            return localizedFormatStreamModelArray;
        }

        // Ignoring, as the stream format model array is empty.
        if (formatStreamModelArray == null || formatStreamModelArray.isEmpty()) {
            return localizedFormatStreamModelArray;
        }

        // Initialize the array before returning it.
        ArrayList<Object> defaultFormatStreamModelArray = formatStreamModelArray;
        formatStreamModelArray = null;
        return defaultFormatStreamModelArray;
    }

    // endregion

    // region [Disable splash animation] patch

    public static boolean disableSplashAnimation(boolean original) {
        try {
            return !Settings.DISABLE_SPLASH_ANIMATION.get() && original;
        } catch (Exception ex) {
            Logger.printException(() -> "Failed to load disableSplashAnimation", ex);
        }
        return original;
    }

    // endregion

    // region [Enable gradient loading screen] patch

    public static boolean enableGradientLoadingScreen() {
        return Settings.ENABLE_GRADIENT_LOADING_SCREEN.get();
    }

    // endregion

    // region [Hide layout components] patch

    private static String[] accountMenuBlockList;

    static {
        accountMenuBlockList = Settings.HIDE_ACCOUNT_MENU_FILTER_STRINGS.get().split("\\n");
        // Some settings should not be hidden.
        accountMenuBlockList = Arrays.stream(accountMenuBlockList)
                .filter(item -> !Objects.equals(item, str("settings")))
                .toArray(String[]::new);
    }

    /**
     * hide account menu in you tab
     *
     * @param menuTitleCharSequence menu title
     */
    public static void hideAccountList(View view, CharSequence menuTitleCharSequence) {
        if (!Settings.HIDE_ACCOUNT_MENU.get())
            return;
        if (menuTitleCharSequence == null)
            return;
        if (!(view.getParent().getParent().getParent() instanceof ViewGroup viewGroup))
            return;

        hideAccountMenu(viewGroup, menuTitleCharSequence.toString());
    }

    /**
     * hide account menu for tablet and old clients
     *
     * @param menuTitleCharSequence menu title
     */
    public static void hideAccountMenu(View view, CharSequence menuTitleCharSequence) {
        if (!Settings.HIDE_ACCOUNT_MENU.get())
            return;
        if (menuTitleCharSequence == null)
            return;
        if (!(view.getParent().getParent() instanceof ViewGroup viewGroup))
            return;

        hideAccountMenu(viewGroup, menuTitleCharSequence.toString());
    }

    private static void hideAccountMenu(ViewGroup viewGroup, String menuTitleString) {
        for (String filter : accountMenuBlockList) {
            if (!filter.isEmpty() && menuTitleString.equals(filter)) {
                if (viewGroup.getLayoutParams() instanceof MarginLayoutParams)
                    hideViewGroupByMarginLayoutParams(viewGroup);
                else
                    viewGroup.setLayoutParams(new LayoutParams(0, 0));
            }
        }
    }

    public static int hideHandle(int originalValue) {
        return Settings.HIDE_HANDLE.get() ? 8 : originalValue;
    }

    public static boolean hideFloatingMicrophone(boolean original) {
        return Settings.HIDE_FLOATING_MICROPHONE.get() || original;
    }

    public static boolean hideSnackBar() {
        return Settings.HIDE_SNACK_BAR.get();
    }

    // endregion

    // region [Hide navigation bar components] patch

    private static final int fillBellCairoBlack = ResourceUtils.getDrawableIdentifier("yt_fill_bell_cairo_black_24");

    private static final Map<NavigationButton, Boolean> shouldHideMap = new EnumMap<>(NavigationButton.class) {
        {
            put(NavigationButton.HOME, Settings.HIDE_NAVIGATION_HOME_BUTTON.get());
            put(NavigationButton.SHORTS, Settings.HIDE_NAVIGATION_SHORTS_BUTTON.get());
            put(NavigationButton.SUBSCRIPTIONS, Settings.HIDE_NAVIGATION_SUBSCRIPTIONS_BUTTON.get());
            put(NavigationButton.CREATE, Settings.HIDE_NAVIGATION_CREATE_BUTTON.get());
            put(NavigationButton.NOTIFICATIONS, Settings.HIDE_NAVIGATION_NOTIFICATIONS_BUTTON.get());
            put(NavigationButton.LIBRARY, Settings.HIDE_NAVIGATION_LIBRARY_BUTTON.get());
        }
    };

    public static boolean enableNarrowNavigationButton(boolean original) {
        return Settings.ENABLE_NARROW_NAVIGATION_BUTTONS.get() || original;
    }

    /**
     * @noinspection ALL
     */
    public static void setCairoNotificationFilledIcon(EnumMap enumMap, Enum tabActivityCairo) {
        if (fillBellCairoBlack != 0) {
            // It's very unlikely, but Google might fix this issue someday.
            // If so, [fillBellCairoBlack] might already be in enumMap.
            // That's why 'EnumMap.putIfAbsent()' is used instead of 'EnumMap.put()'.
            enumMap.putIfAbsent(tabActivityCairo, Integer.valueOf(fillBellCairoBlack));
        }
    }

    public static boolean switchCreateWithNotificationButton(boolean original) {
        return Settings.SWITCH_CREATE_WITH_NOTIFICATIONS_BUTTON.get() || original;
    }

    public static void navigationTabCreated(NavigationButton button, View tabView) {
        if (BooleanUtils.isTrue(shouldHideMap.get(button))) {
            tabView.setVisibility(View.GONE);
        }
    }

    public static void hideNavigationLabel(TextView view) {
        hideViewUnderCondition(Settings.HIDE_NAVIGATION_LABEL.get(), view);
    }

    public static void hideNavigationBar(View view) {
        hideViewUnderCondition(Settings.HIDE_NAVIGATION_BAR.get(), view);
    }

    public static boolean useTranslucentNavigationStatusBar(boolean original) {
        if (Settings.DISABLE_TRANSLUCENT_STATUS_BAR.get()) {
            return false;
        }

        return original;
    }

    private static final Boolean DISABLE_TRANSLUCENT_NAVIGATION_BAR_LIGHT
            = Settings.DISABLE_TRANSLUCENT_NAVIGATION_BAR_LIGHT.get();

    private static final Boolean DISABLE_TRANSLUCENT_NAVIGATION_BAR_DARK
            = Settings.DISABLE_TRANSLUCENT_NAVIGATION_BAR_DARK.get();

    public static boolean useTranslucentNavigationButtons(boolean original) {
        // Feature requires Android 13+
        if (!isSDKAbove(33)) {
            return original;
        }

        if (!DISABLE_TRANSLUCENT_NAVIGATION_BAR_DARK && !DISABLE_TRANSLUCENT_NAVIGATION_BAR_LIGHT) {
            return original;
        }

        if (DISABLE_TRANSLUCENT_NAVIGATION_BAR_DARK && DISABLE_TRANSLUCENT_NAVIGATION_BAR_LIGHT) {
            return false;
        }

        return Utils.isDarkModeEnabled()
                ? !DISABLE_TRANSLUCENT_NAVIGATION_BAR_DARK
                : !DISABLE_TRANSLUCENT_NAVIGATION_BAR_LIGHT;
    }

    // endregion

    // region [Remove viewer discretion dialog] patch

    /**
     * Injection point.
     * <p>
     * The {@link AlertDialog#getButton(int)} method must be used after {@link AlertDialog#show()} is called.
     * Otherwise {@link AlertDialog#getButton(int)} method will always return null.
     * <a href="https://stackoverflow.com/a/4604145"/>
     * <p>
     * That's why {@link AlertDialog#show()} is absolutely necessary.
     * Instead, use two tricks to hide Alertdialog.
     * <p>
     * 1. Change the size of AlertDialog to 0.
     * 2. Disable AlertDialog's background dim.
     * <p>
     * This way, AlertDialog will be completely hidden,
     * and {@link AlertDialog#getButton(int)} method can be used without issue.
     */
    public static void confirmDialog(final AlertDialog dialog) {
        if (!Settings.REMOVE_VIEWER_DISCRETION_DIALOG.get()) {
            return;
        }

        // This method is called after AlertDialog#show(),
        // So we need to hide the AlertDialog before pressing the possitive button.
        final Window window = dialog.getWindow();
        final Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (window != null && button != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.height = 0;
            params.width = 0;

            // Change the size of AlertDialog to 0.
            window.setAttributes(params);

            // Disable AlertDialog's background dim.
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            Utils.clickView(button);
        }
    }

    public static void confirmDialogAgeVerified(final AlertDialog dialog) {
        final Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (!button.getText().toString().equals(str("og_continue")))
            return;

        confirmDialog(dialog);
    }

    // endregion

    // region [Spoof app version] patch

    public static String getVersionOverride(String appVersion) {
        return Settings.SPOOF_APP_VERSION.get()
                ? Settings.SPOOF_APP_VERSION_TARGET.get()
                : appVersion;
    }

    // endregion

    // region [Toolbar components] patch

    private static final int generalHeaderAttributeId = ResourceUtils.getAttrIdentifier("ytWordmarkHeader");
    private static final int premiumHeaderAttributeId = ResourceUtils.getAttrIdentifier("ytPremiumWordmarkHeader");

    public static void setDrawerNavigationHeader(View lithoView) {
        final int headerAttributeId = getHeaderAttributeId();

        lithoView.getViewTreeObserver().addOnDrawListener(() -> {
            if (!(lithoView instanceof ViewGroup viewGroup))
                return;
            if (!(viewGroup.getChildAt(0) instanceof ImageView imageView))
                return;
            final Activity mActivity = Utils.getActivity();
            if (mActivity == null)
                return;
            imageView.setImageDrawable(getHeaderDrawable(mActivity, headerAttributeId));
        });
    }

    public static int getHeaderAttributeId() {
        return Settings.CHANGE_YOUTUBE_HEADER.get()
                ? premiumHeaderAttributeId
                : generalHeaderAttributeId;
    }

    public static boolean overridePremiumHeader() {
        return Settings.CHANGE_YOUTUBE_HEADER.get();
    }

    private static Drawable getHeaderDrawable(Activity mActivity, int resourceId) {
        // Rest of the implementation added by patch.
        return ResourceUtils.getDrawable("");
    }

    private static final int searchBarId = ResourceUtils.getIdIdentifier("search_bar");
    private static final int youtubeTextId = ResourceUtils.getIdIdentifier("youtube_text");
    private static final int searchBoxId = ResourceUtils.getIdIdentifier("search_box");
    private static final int searchIconId = ResourceUtils.getIdIdentifier("search_icon");

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

    public static void setWideSearchBarLayout(View view) {
        if (!wideSearchbarEnabled)
            return;
        if (!(view.findViewById(searchBarId) instanceof RelativeLayout searchBarView))
            return;

        // When the deprecated search bar is loaded, two search bars overlap.
        // Manually hides another search bar.
        if (wideSearchbarWithHeaderEnabled) {
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
            final int paddingStart = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, Utils.getResources().getDisplayMetrics());

            // In RelativeLayout, paddingStart cannot be assigned programmatically.
            // Check RTL layout and set left padding or right padding.
            if (Utils.isRightToLeftTextLayout()) {
                searchBarView.setPadding(paddingLeft, paddingTop, paddingStart, paddingBottom);
            } else {
                searchBarView.setPadding(paddingStart, paddingTop, paddingRight, paddingBottom);
            }
        }
    }

    public static boolean hideCastButton(boolean original) {
        return !Settings.HIDE_TOOLBAR_CAST_BUTTON.get() && original;
    }

    public static void hideCastButton(MenuItem menuItem) {
        if (!Settings.HIDE_TOOLBAR_CAST_BUTTON.get())
            return;

        menuItem.setVisible(false);
        menuItem.setEnabled(false);
    }

    public static void hideCreateButton(String enumString, View view) {
        if (!Settings.HIDE_TOOLBAR_CREATE_BUTTON.get())
            return;

        hideViewUnderCondition(isCreateButton(enumString), view);
    }

    public static void hideNotificationButton(String enumString, View view) {
        if (!Settings.HIDE_TOOLBAR_NOTIFICATION_BUTTON.get())
            return;

        hideViewUnderCondition(isNotificationButton(enumString), view);
    }

    public static boolean hideSearchTermThumbnail() {
        return Settings.HIDE_SEARCH_TERM_THUMBNAIL.get();
    }

    private static final boolean hideImageSearchButton = Settings.HIDE_IMAGE_SEARCH_BUTTON.get();
    private static final boolean hideVoiceSearchButton = Settings.HIDE_VOICE_SEARCH_BUTTON.get();

    /**
     * If the user does not hide the Image search button but only the Voice search button,
     * {@link View#setVisibility(int)} cannot be used on the Voice search button.
     * (This breaks the search bar layout.)
     * <p>
     * In this case, {@link Utils#hideViewByLayoutParams(View)} should be used.
     */
    private static final boolean showImageSearchButtonAndHideVoiceSearchButton = !hideImageSearchButton && hideVoiceSearchButton && ImageSearchButton();

    public static boolean hideImageSearchButton(boolean original) {
        return !hideImageSearchButton && original;
    }

    public static void hideVoiceSearchButton(View view) {
        if (showImageSearchButtonAndHideVoiceSearchButton) {
            hideViewByLayoutParams(view);
        } else {
            hideViewUnderCondition(hideVoiceSearchButton, view);
        }
    }

    public static void hideVoiceSearchButton(View view, int visibility) {
        if (showImageSearchButtonAndHideVoiceSearchButton) {
            view.setVisibility(visibility);
            hideViewByLayoutParams(view);
        } else {
            view.setVisibility(
                    hideVoiceSearchButton
                            ? View.GONE : visibility
            );
        }
    }

    /**
     * In ReVanced, image files are replaced to change the header,
     * Whereas in RVX, the header is changed programmatically.
     * There is an issue where the header is not changed in RVX when YouTube Doodles are hidden.
     * As a workaround, manually set the header when YouTube Doodles are hidden.
     */
    public static void hideYouTubeDoodles(ImageView imageView, Drawable drawable) {
        final Activity mActivity = Utils.getActivity();
        if (Settings.HIDE_YOUTUBE_DOODLES.get() && mActivity != null) {
            drawable = getHeaderDrawable(mActivity, getHeaderAttributeId());
        }
        imageView.setImageDrawable(drawable);
    }

    private static final int settingsDrawableId =
            ResourceUtils.getDrawableIdentifier("yt_outline_gear_black_24");

    public static int getCreateButtonDrawableId(int original) {
        return Settings.REPLACE_TOOLBAR_CREATE_BUTTON.get() &&
                settingsDrawableId != 0
                ? settingsDrawableId
                : original;
    }

    public static void replaceCreateButton(String enumString, View toolbarView) {
        if (!Settings.REPLACE_TOOLBAR_CREATE_BUTTON.get())
            return;
        // Check if the button is a create button.
        if (!isCreateButton(enumString))
            return;
        ImageView imageView = getChildView((ViewGroup) toolbarView, view -> view instanceof ImageView);
        if (imageView == null)
            return;

        // Overriding is possible only after OnClickListener is assigned to the create button.
        Utils.runOnMainThreadDelayed(() -> {
            if (Settings.REPLACE_TOOLBAR_CREATE_BUTTON_TYPE.get()) {
                imageView.setOnClickListener(GeneralPatch::openRVXSettings);
                imageView.setOnLongClickListener(button -> {
                    openYouTubeSettings(button);
                    return true;
                });
            } else {
                imageView.setOnClickListener(GeneralPatch::openYouTubeSettings);
                imageView.setOnLongClickListener(button -> {
                    openRVXSettings(button);
                    return true;
                });
            }
        }, 0);
    }

    private static void openYouTubeSettings(View view) {
        Context context = view.getContext();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setPackage(context.getPackageName());
        intent.setClass(context, Shell_SettingsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        context.startActivity(intent);
    }

    private static void openRVXSettings(View view) {
        Context context = view.getContext();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setPackage(context.getPackageName());
        intent.setData(Uri.parse("revanced_extended_settings_intent"));
        intent.setClass(context, VideoQualitySettingsActivity.class);
        context.startActivity(intent);
    }

    /**
     * The theme of {@link Shell_SettingsActivity} is dark theme.
     * Since this theme is hardcoded, we should manually specify the theme for the activity.
     * <p>
     * Since {@link Shell_SettingsActivity} only invokes {@link SettingsActivity}, finish activity after specifying a theme.
     *
     * @param base {@link Shell_SettingsActivity}
     */
    public static void setShellActivityTheme(Activity base) {
        if (!Settings.REPLACE_TOOLBAR_CREATE_BUTTON.get())
            return;

        base.setTheme(ThemeUtils.getThemeId());
        Utils.runOnMainThreadDelayed(base::finish, 0);
    }


    private static boolean isCreateButton(String enumString) {
        return StringUtils.equalsAny(
                enumString,
                "CREATION_ENTRY", // Create button for Phone layout
                "FAB_CAMERA" // Create button for Tablet layout
        );
    }

    private static boolean isNotificationButton(String enumString) {
        return StringUtils.equalsAny(
                enumString,
                "TAB_ACTIVITY", // Notification button
                "TAB_ACTIVITY_CAIRO" // Notification button (new layout)
        );
    }

    // endregion

}

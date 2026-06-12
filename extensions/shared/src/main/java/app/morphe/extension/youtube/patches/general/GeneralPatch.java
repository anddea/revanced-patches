/*
 * Copyright (C) 2024-2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - inotia00 (https://github.com/inotia00)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Additional Terms & Attribution Requirements
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Source Credit Preservation (Section 7(b)): This specific copyright notice
 *    and the list of original authors above must be preserved in any copy
 *    or derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin & Modification Marking (Section 7(c)): Modified versions must be
 *    clearly marked as such (e.g., by adding a "Modified by" line or a new
 *    copyright notice) and must not be misrepresented as the original work.
 *
 * 3. Version Control Attribution (Section 7(b)): Any ports or substantial
 *    modifications must retain historical authorship credit in version control
 *    systems (e.g., Git), listing original author(s) appropriately and
 *    modifiers as committers or co-authors.
 *
 * 4. User Interface Attribution (Section 7(b)): Any works containing or
 *    derived from this material must maintain a visible credit or
 *    acknowledgment to the original author(s) within the application's
 *    user interface (e.g., in an "About" or "Credits" section).
 */

package app.morphe.extension.youtube.patches.general;

import static app.morphe.extension.shared.utils.ResourceUtils.getXmlIdentifier;
import static app.morphe.extension.shared.utils.StringRef.str;
import static app.morphe.extension.shared.utils.Utils.getChildView;
import static app.morphe.extension.shared.utils.Utils.hideViewByLayoutParams;
import static app.morphe.extension.shared.utils.Utils.hideViewGroupByMarginLayoutParams;
import static app.morphe.extension.shared.utils.Utils.hideViewUnderCondition;
import static app.morphe.extension.youtube.patches.utils.PatchStatus.ImageSearchButton;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.InputType;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.apps.youtube.app.application.Shell_SettingsActivity;
import com.google.android.apps.youtube.app.settings.SettingsActivity;

import org.apache.commons.lang3.StringUtils;

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntSupplier;

import app.morphe.extension.shared.ui.CustomDialog;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.ResourceUtils;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.patches.utils.ReturnYouTubeChannelNamePatch;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.RootView;
import app.morphe.extension.youtube.shared.VideoInformation;
import app.morphe.extension.youtube.utils.ExtendedUtils;
import app.morphe.extension.youtube.utils.ThemeUtils;

@SuppressWarnings({"deprecation", "unused"})
public class GeneralPatch {

    private static final String OPEN_SEARCH_ACTION = "com.google.android.youtube.action.open.search";
    private static final String YOUTUBE_MAIN_ACTIVITY_CLASS_NAME =
            "com.google.android.apps.youtube.app.watchwhile.MainActivity";
    private static final int SEARCH_INTENT_FLAGS =
            Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP;
    private static final String CHANNEL_BROWSE_ID_PREFIX = "UC";
    private static final int YOUTUBE_VIDEO_ID_LENGTH = 11;
    private static final String CHANNEL_SEARCH_WEBVIEW_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/120.0.6099.230 Mobile Safari/537.36";
    private static final String CHANNEL_SEARCH_WEBVIEW_HOST = "m.youtube.com";
    private static final String CHANNEL_SEARCH_WEBVIEW_CONSENT_HOST = "consent.youtube.com";
    private static final String CHANNEL_SEARCH_WEBVIEW_BRIDGE_SCHEME = "revanced-channel-search";
    private static final String YOUTUBE_VIDEO_SCHEME_URL = "vnd.youtube://";
    private static final String CHANNEL_SEARCH_WEBVIEW_JAVASCRIPT = """
            (function() {
                function openInApp(event) {
                    var target = event.target;
                    while (target && target.tagName !== 'A') {
                        target = target.parentElement;
                    }
                    if (!target || !target.href) { return; }
            
                    var href = target.getAttribute('href') || '';
                    if (href.indexOf('/watch') < 0 && href.indexOf('/shorts/') < 0 && href.indexOf('/playlist') < 0) {
                        return;
                    }
            
                    event.preventDefault();
                    event.stopPropagation();
                    event.stopImmediatePropagation();
                    window.location.href = 'revanced-channel-search://open?url=' + encodeURIComponent(target.href);
                }
            
                function trim() {
                    var style = document.getElementById('revanced-channel-search-style');
                    if (!style) {
                        style = document.createElement('style');
                        style.id = 'revanced-channel-search-style';
                        style.textContent = 'ytm-mobile-topbar-renderer, ytm-pivot-bar-renderer, yt-page-header-renderer, .single-column-browse-results-tab-bar, #player-container-id, ytm-tabs-renderer, ytm-channel-header-renderer, ytm-browse-header-renderer, ytm-app-header, ytm-bottom-sheet-renderer { display: none !important; } html, body, ytm-app, .page-container, ytm-browse, ytm-single-column-browse-results-renderer { margin: 0 !important; padding: 0 !important; } body { overflow: auto !important; } .tab-content:not([tab-title="Search"]) { display: none !important; } .tab-content[tab-title="Search"] { display: block !important; margin: 0 !important; padding: 0 !important; } ytm-section-list-renderer { margin-top: 0 !important; }';
                        (document.head || document.documentElement).appendChild(style);
                    }
            
                    var search = document.querySelector('.tab-content[tab-title="Search"]');
                    if (search && search.parentElement) {
                        Array.prototype.forEach.call(search.parentElement.children, function(child) {
                            if (child !== search) {
                                child.style.display = 'none';
                            }
                        });
                    }
                }
            
                trim();
            
                if (!window.revancedChannelSearchClickBridge) {
                    window.addEventListener('click', openInApp, true);
                    window.addEventListener('auxclick', openInApp, true);
                    document.addEventListener('click', openInApp, true);
                    document.addEventListener('auxclick', openInApp, true);
                    window.revancedChannelSearchClickBridge = true;
                }
            
                if (!window.revancedChannelSearchObserver) {
                    window.revancedChannelSearchObserver = new MutationObserver(trim);
                    window.revancedChannelSearchObserver.observe(document.documentElement, { childList: true, subtree: true });
                }
            })();
            """;

    private static volatile String lastSearchInChannelQuery = "";
    private static volatile String pendingSearchInChannelVisibleQuery = "";
    private static volatile String pendingSearchInChannelRequestQuery = "";

    // region [Disable layout updates] patch

    private static final String[] REQUEST_HEADER_KEYS = {
            "X-Youtube-Cold-Config-Data",
            "X-Youtube-Cold-Hash-Data",
            "X-Youtube-Hot-Config-Data",
            "X-Youtube-Hot-Hash-Data"
    };

    private static final boolean DISABLE_LAYOUT_UPDATES =
            Settings.DISABLE_LAYOUT_UPDATES.get();

    /**
     * @param key   Keys to be added to the header of CronetBuilder.
     * @param value Values to be added to the header of CronetBuilder.
     * @return Empty value if setting is enabled.
     */
    public static String disableLayoutUpdates(String key, String value) {
        if (DISABLE_LAYOUT_UPDATES && StringUtils.equalsAny(key, REQUEST_HEADER_KEYS)) {
            Logger.printDebug(() -> "Blocking: " + key);
            return "";
        }

        return value;
    }

    // endregion

    // region [Disable sign in to TV popup] patch

    public static boolean disableSignInToTvPopup() {
        return Settings.DISABLE_SIGNIN_TO_TV_POPUP.get();
    }

    // endregion

    // region [Disable splash animation] patch

    public static boolean disableSplashAnimation(boolean original) {
        return !Settings.DISABLE_SPLASH_ANIMATION.get() && original;
    }

    public static int disableSplashAnimation(int i, int i2) {
        if (!Settings.DISABLE_SPLASH_ANIMATION.get() || i != i2) {
            return i;
        }
        return i - 1;
    }

    // endregion

    // region [Enable gradient loading screen] patch

    public static boolean enableGradientLoadingScreen() {
        return Settings.ENABLE_GRADIENT_LOADING_SCREEN.get();
    }

    // endregion

    // region [Hide layout components] patch

    public static boolean disableTranslucentStatusBar(boolean original) {
        return !Settings.DISABLE_TRANSLUCENT_STATUS_BAR.get() && original;
    }

    private static String[] accountMenuBlockList;

    private static String[] getAccountMenuBlockList(Context mContext) {
        if (accountMenuBlockList == null) {
            int settingsIdentifier = ResourceUtils.getIdentifier("settings", ResourceUtils.ResourceType.STRING, mContext);
            if (settingsIdentifier != 0) {
                String settings = mContext.getResources().getString(settingsIdentifier);
                accountMenuBlockList = Settings.HIDE_ACCOUNT_MENU_FILTER_STRINGS.get().split("\\n");
                // Some settings should not be hidden.
                accountMenuBlockList = Arrays.stream(accountMenuBlockList)
                        .filter(item -> !Objects.equals(item, settings))
                        .toArray(String[]::new);
            }
        }
        return Objects.requireNonNullElseGet(accountMenuBlockList, () -> Settings.HIDE_ACCOUNT_MENU_FILTER_STRINGS.get().split("\\n"));
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
        for (String filter : getAccountMenuBlockList(viewGroup.getContext())) {
            if (!filter.isEmpty()) {
                if (Settings.HIDE_ACCOUNT_MENU_FILTER_TYPE.get()) {
                    if (menuTitleString.contains(filter))
                        hideViewGroup(viewGroup);
                } else {
                    if (menuTitleString.equals(filter))
                        hideViewGroup(viewGroup);
                }
            }
        }
    }

    private static void hideViewGroup(ViewGroup viewGroup) {
        if (viewGroup.getLayoutParams() instanceof MarginLayoutParams)
            hideViewGroupByMarginLayoutParams(viewGroup);
        else
            viewGroup.setLayoutParams(new LayoutParams(0, 0));
    }

    public static int hideHandle(int originalValue) {
        return Settings.HIDE_HANDLE.get() ? 8 : originalValue;
    }

    public static boolean hideFloatingMicrophone(boolean original) {
        return Settings.HIDE_FLOATING_MICROPHONE.get() || original;
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
        // So we need to hide the AlertDialog before pressing the positive button.
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

    // region [Fix Hype button icon] patch

    public static boolean fixHypeButtonIconEnabled() {
        return Settings.FIX_HYPE_BUTTON_ICON.get();
    }

    public static String getWatchNextEndpointVersionOverride() {
        return "19.26.42";
    }

    // endregion

    // region [Spoof app version] patch

    private static int legacySettingsFragmentId;

    public static String getVersionOverride(String appVersion) {
        return Settings.SPOOF_APP_VERSION.get()
                ? Settings.SPOOF_APP_VERSION_TARGET.get()
                : appVersion;
    }

    /**
     * Injection point.
     */
    public static int useLegacySettingsFragment(int original) {
        return useLegacySettingsFragment(original, () -> {
            if (legacySettingsFragmentId == 0) {
                legacySettingsFragmentId = getXmlIdentifier("settings_fragment_legacy");
            }
            return legacySettingsFragmentId;
        });
    }

    static int useLegacySettingsFragment(int original, IntSupplier legacyFragmentIdSupplier) {
        if (Settings.RESTORE_OLD_SETTINGS_MENUS.get()
                || ExtendedUtils.isSpoofingToLessThan("19.35.36")) {
            final int legacyFragmentId = legacyFragmentIdSupplier.getAsInt();
            if (legacyFragmentId != 0) {
                return legacyFragmentId;
            }
        }

        return original;
    }

    // endregion

    // region [Toolbar components] patch

    private static int generalHeaderAttributeId = 0;
    private static int premiumHeaderAttributeId = 0;

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
        if (premiumHeaderAttributeId == 0) {
            generalHeaderAttributeId = ResourceUtils.getAttrIdentifier("ytWordmarkHeader");
            premiumHeaderAttributeId = ResourceUtils.getAttrIdentifier("ytPremiumWordmarkHeader");
        }

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

    public static void hideSearchButton(String enumString, View view) {
        if (!Settings.HIDE_TOOLBAR_SEARCH_BUTTON.get())
            return;

        hideViewUnderCondition(isSearchButton(enumString), view);
    }

    public static void hideSearchButton(MenuItem menuItem, int original) {
        menuItem.setShowAsAction(
                Settings.HIDE_TOOLBAR_SEARCH_BUTTON.get()
                        ? MenuItem.SHOW_AS_ACTION_NEVER
                        : original
        );
    }

    public static void openSearchInChannel(String enumString, View toolbarView) {
        if (!isSearchButton(enumString))
            return;

        ImageView imageView = getChildView((ViewGroup) toolbarView, view -> view instanceof ImageView);
        if (imageView == null)
            return;

        // YouTube assigns the original listener after creating the button.
        Utils.runOnMainThreadDelayed(() ->
                imageView.setOnClickListener(GeneralPatch::openSearchInChannel), 0);
    }

    public static String overrideSearchInChannelRequestQuery(String original) {
        String visibleQuery = pendingSearchInChannelVisibleQuery;
        String requestQuery = pendingSearchInChannelRequestQuery;

        if (StringUtils.isEmpty(requestQuery)) {
            return original;
        }

        if (StringUtils.isBlank(original)
                || StringUtils.equals(original, visibleQuery)
                || StringUtils.equals(original, requestQuery)) {
            clearPendingSearchInChannelQuery();
            return requestQuery;
        }

        clearPendingSearchInChannelQuery();
        return original;
    }

    private static void openSearchInChannel(View view) {
        String channelId = RootView.getBrowseId();
        if (!isChannelBrowseId(channelId)) {
            openSearchBar(view);
            return;
        }

        Context context = view.getContext();
        String channelName = getChannelName(channelId, view);
        EditText editText = new EditText(context);
        editText.setSingleLine(true);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        editText.setHint(str("revanced_search_in_channel_query_hint"));
        if (!StringUtils.isBlank(lastSearchInChannelQuery)) {
            editText.setText(lastSearchInChannelQuery);
            editText.setSelection(editText.length());
        }

        Pair<Dialog, LinearLayout> dialogPair = CustomDialog.create(
                context,
                str("revanced_search_in_channel_title"),
                null,
                editText,
                str("revanced_search_in_channel_search"),
                () -> {
                    String query = editText.getText().toString().trim();
                    if (!query.isEmpty()) {
                        cacheSearchInChannelQuery(query);
                        openSearchInChannel(context, channelId, channelName, query);
                    }
                },
                () -> {
                },
                str("revanced_search_in_channel_search_youtube"),
                () -> {
                    String query = editText.getText().toString().trim();
                    if (query.isEmpty()) {
                        openSearchBar(view);
                    } else {
                        cacheSearchInChannelQuery(query);
                        openSearch(context, query);
                    }
                },
                true,
                true
        );

        Dialog dialog = dialogPair.first;
        dialog.setOnShowListener(dialogInterface -> {
            editText.requestFocus();
            Window window = dialog.getWindow();
            if (window != null) {
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }
        });
        dialog.show();
    }

    private static boolean isChannelBrowseId(String browseId) {
        return browseId != null && browseId.startsWith(CHANNEL_BROWSE_ID_PREFIX);
    }

    private static void openSearchBar(View view) {
        Context context = view.getContext();
        clearPendingSearchInChannelQuery();
        Intent intent = new Intent(OPEN_SEARCH_ACTION);
        intent.setClassName(context.getPackageName(), YOUTUBE_MAIN_ACTIVITY_CLASS_NAME);
        intent.addFlags(SEARCH_INTENT_FLAGS);
        context.startActivity(intent);
    }

    private static void openSearchInChannel(Context context, String channelId, String channelName, String query) {
        if (Settings.SEARCH_IN_CHANNEL_USE_WEBVIEW.get()) {
            openSearchInChannelWebView(context, channelId, channelName, query);
            return;
        }

        openSearchInChannelNative(context, channelName, query);
    }

    private static void openSearchInChannelNative(Context context, String channelName, String query) {
        setPendingSearchInChannelQuery(query, getSearchInChannelNativeQuery(channelName, query));
        openSearch(context, query, true);
    }

    private static void openSearch(Context context, String query) {
        openSearch(context, query, false);
    }

    private static void openSearch(Context context, String query, boolean preservePendingSearchInChannelQuery) {
        if (!preservePendingSearchInChannelQuery) {
            clearPendingSearchInChannelQuery();
        }
        Intent intent = new Intent(Intent.ACTION_SEARCH);
        intent.setClassName(context.getPackageName(), YOUTUBE_MAIN_ACTIVITY_CLASS_NAME);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addFlags(SEARCH_INTENT_FLAGS);
        intent.putExtra(SearchManager.QUERY, query);
        context.startActivity(intent);
    }

    private static void cacheSearchInChannelQuery(String query) {
        lastSearchInChannelQuery = query;
    }

    private static void setPendingSearchInChannelQuery(String visibleQuery, String requestQuery) {
        pendingSearchInChannelVisibleQuery = visibleQuery;
        pendingSearchInChannelRequestQuery = requestQuery;
    }

    private static void clearPendingSearchInChannelQuery() {
        pendingSearchInChannelVisibleQuery = "";
        pendingSearchInChannelRequestQuery = "";
    }

    private static String getChannelName(String channelId) {
        String videoChannelId = VideoInformation.getChannelId();
        if (StringUtils.equals(channelId, videoChannelId)) {
            return VideoInformation.getChannelName();
        }

        return ReturnYouTubeChannelNamePatch.getCachedChannelName(channelId);
    }

    private static String getChannelName(String channelId, View anchorView) {
        String channelName = getChannelName(channelId);
        if (StringUtils.isNotBlank(channelName)) {
            return channelName;
        }

        channelName = getVisibleChannelName(anchorView);
        if (StringUtils.isNotBlank(channelName)) {
            ReturnYouTubeChannelNamePatch.setCachedChannelName(channelId, channelName);
        }

        return channelName;
    }

    private static String getVisibleChannelName(View anchorView) {
        try {
            Activity activity = Utils.getActivity();
            View rootView = activity == null
                    ? anchorView.getRootView()
                    : activity.findViewById(android.R.id.content);
            String[] bestText = {""};
            float[] bestTextSize = {0.0f};
            float minimumTextSize = anchorView.getResources().getDisplayMetrics().scaledDensity * 16.0f;
            findVisibleChannelName(rootView, minimumTextSize, bestText, bestTextSize);
            return bestText[0];
        } catch (Exception ex) {
            Logger.printException(() -> "getVisibleChannelName failed", ex);
            return "";
        }
    }

    private static void findVisibleChannelName(View view, float minimumTextSize, String[] bestText, float[] bestTextSize) {
        if (view == null || !view.isShown()) {
            return;
        }

        if (view instanceof TextView textView) {
            String text = StringUtils.trimToEmpty(textView.getText().toString());
            Rect visibleRect = new Rect();
            if (isVisibleChannelNameCandidate(text)
                    && textView.getTextSize() >= minimumTextSize
                    && textView.getTextSize() > bestTextSize[0]
                    && textView.getGlobalVisibleRect(visibleRect)) {
                bestText[0] = text;
                bestTextSize[0] = textView.getTextSize();
            }
        }

        if (view instanceof ViewGroup viewGroup) {
            for (int i = 0, childCount = viewGroup.getChildCount(); i < childCount; i++) {
                findVisibleChannelName(viewGroup.getChildAt(i), minimumTextSize, bestText, bestTextSize);
            }
        }
    }

    private static boolean isVisibleChannelNameCandidate(String text) {
        return StringUtils.isNotBlank(text)
                && text.length() <= 100
                && !StringUtils.contains(text, "\n")
                && !StringUtils.startsWith(text, "@")
                && !StringUtils.equalsAnyIgnoreCase(
                text,
                "Home",
                "Videos",
                "Shorts",
                "Live",
                "Playlists",
                "Community",
                "Channels",
                "About",
                "Subscribe",
                "Subscribed"
        );
    }

    private static String getSearchInChannelNativeQuery(String channelName, String query) {
        channelName = StringUtils.trimToEmpty(channelName);
        return StringUtils.isBlank(channelName) || StringUtils.containsIgnoreCase(query, channelName)
                ? query
                : query + " " + channelName;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private static void openSearchInChannelWebView(Context context, String channelId, String channelName, String query) {
        clearPendingSearchInChannelQuery();

        if (!Utils.isWebViewSupported()) {
            Utils.showToastLong(str("revanced_search_in_channel_webview_unavailable"));
            openSearchInChannelNative(context, channelName, query);
            return;
        }

        try {
            Dialog dialog = new Dialog(context);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

            android.widget.FrameLayout layout = new android.widget.FrameLayout(context);
            layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

            android.widget.ProgressBar progressBar = new android.widget.ProgressBar(context);
            android.widget.FrameLayout.LayoutParams progressParams = new android.widget.FrameLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            progressParams.gravity = android.view.Gravity.CENTER;
            progressBar.setLayoutParams(progressParams);

            String webViewUrl = getChannelSearchWebViewUrl(channelId, query);
            WebView webView = getWebView(context, dialog, progressBar);
            webView.setVisibility(View.INVISIBLE);

            layout.addView(webView);
            layout.addView(progressBar);

            dialog.setContentView(layout);
            dialog.setOnDismissListener(dialogInterface -> {
                webView.stopLoading();
                webView.destroy();
            });
            dialog.show();

            Window window = dialog.getWindow();
            if (window != null) {
                window.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }

            webView.loadUrl(webViewUrl);
        } catch (Exception ex) {
            Logger.printException(() -> "openSearchInChannelWebView failed", ex);
            openSearchInChannelNative(context, channelName, query);
        }
    }

    @SuppressLint("SetJavaScriptEnabled") // Required by YouTube mobile search. WebView is restricted below.
    @NonNull
    private static WebView getWebView(Context context, Dialog dialog, View progressBar) {
        WebView webView = new WebView(context);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setGeolocationEnabled(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setUserAgentString(CHANNEL_SEARCH_WEBVIEW_USER_AGENT);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleChannelSearchWebViewNavigation(context, dialog, request.getUrl().toString());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleChannelSearchWebViewNavigation(context, dialog, url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                view.setVisibility(View.VISIBLE);
                if (isChannelSearchWebViewSearchUrl(url)) {
                    view.evaluateJavascript(CHANNEL_SEARCH_WEBVIEW_JAVASCRIPT, null);
                }
            }
        });
        return webView;
    }

    private static boolean handleChannelSearchWebViewNavigation(Context context, Dialog dialog, String url) {
        try {
            Uri uri = Uri.parse(url);
            if (StringUtils.equalsIgnoreCase(uri.getScheme(), CHANNEL_SEARCH_WEBVIEW_BRIDGE_SCHEME)) {
                String targetUrl = uri.getQueryParameter("url");
                if (StringUtils.isNotBlank(targetUrl)) {
                    openYouTubeUrlInApp(context, dialog, targetUrl);
                }
                return true;
            }

            if (openYouTubeUrlInApp(context, dialog, url)) {
                return true;
            }

            return !isAllowedChannelSearchWebViewUrl(uri);
        } catch (Exception ex) {
            Logger.printException(() -> "handleChannelSearchWebViewNavigation failed", ex);
            return true;
        }
    }

    private static boolean isAllowedChannelSearchWebViewUrl(Uri uri) {
        return StringUtils.equalsIgnoreCase(uri.getScheme(), "https")
                && StringUtils.equalsAnyIgnoreCase(
                uri.getHost(),
                CHANNEL_SEARCH_WEBVIEW_HOST,
                CHANNEL_SEARCH_WEBVIEW_CONSENT_HOST,
                "www.youtube.com",
                "youtube.com"
        );
    }

    private static boolean isChannelSearchWebViewSearchUrl(String url) {
        try {
            Uri uri = Uri.parse(url);
            return StringUtils.equalsIgnoreCase(uri.getScheme(), "https")
                    && StringUtils.equalsIgnoreCase(uri.getHost(), CHANNEL_SEARCH_WEBVIEW_HOST)
                    && StringUtils.contains(uri.getPath(), "/channel/")
                    && StringUtils.endsWith(uri.getPath(), "/search");
        } catch (Exception ex) {
            Logger.printException(() -> "isChannelSearchWebViewSearchUrl failed", ex);
            return false;
        }
    }

    private static boolean openYouTubeUrlInApp(Context context, Dialog dialog, String url) {
        try {
            Uri uri = Uri.parse(url);
            String scheme = uri.getScheme();
            if (StringUtils.equalsAnyIgnoreCase(scheme, "vnd.youtube", "youtube")) {
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.setPackage(context.getPackageName());
                context.startActivity(intent);
                dialog.dismiss();
                return true;
            }

            String host = uri.getHost();
            String path = uri.getPath();
            if (!StringUtils.equalsAnyIgnoreCase(host, "m.youtube.com", "www.youtube.com", "youtube.com", "youtu.be")
                    || StringUtils.isBlank(path)) {
                return false;
            }
            if (!path.startsWith("/watch") && !path.startsWith("/shorts/") && !path.startsWith("/playlist")) {
                return false;
            }

            String videoId = getYouTubeVideoId(uri);
            Uri launchUri = StringUtils.isEmpty(videoId)
                    ? getYouTubeWebLaunchUri(uri)
                    : Uri.parse(YOUTUBE_VIDEO_SCHEME_URL + videoId);

            Intent intent = new Intent(Intent.ACTION_VIEW, launchUri);
            intent.setPackage(context.getPackageName());
            context.startActivity(intent);
            dialog.dismiss();
            return true;
        } catch (Exception ex) {
            Logger.printException(() -> "openYouTubeUrlInApp failed", ex);
            return false;
        }
    }

    private static Uri getYouTubeWebLaunchUri(Uri uri) {
        if (StringUtils.equalsIgnoreCase(uri.getHost(), "m.youtube.com")) {
            return uri.buildUpon().authority("www.youtube.com").build();
        }

        return uri;
    }

    private static String getYouTubeVideoId(Uri uri) {
        String host = uri.getHost();
        String path = uri.getPath();
        if (StringUtils.isBlank(path)) {
            return "";
        }

        if (StringUtils.equalsIgnoreCase(host, "youtu.be")) {
            return getValidVideoId(StringUtils.substringBefore(path.substring(1), "/"));
        }
        if (path.startsWith("/watch")) {
            return getValidVideoId(uri.getQueryParameter("v"));
        }
        if (path.startsWith("/shorts/")) {
            return getValidVideoId(StringUtils.substringBefore(path.substring("/shorts/".length()), "/"));
        }

        return "";
    }

    private static String getValidVideoId(String videoId) {
        return videoId != null && videoId.length() == YOUTUBE_VIDEO_ID_LENGTH
                ? videoId
                : "";
    }

    private static String getChannelSearchWebViewUrl(String channelId, String query) {
        return "https://" + CHANNEL_SEARCH_WEBVIEW_HOST + "/channel/" + channelId + "/search?query="
                + encodeUrl(query);
    }

    @SuppressWarnings("CharsetObjectCanBeUsed")
    private static String encodeUrl(String string) {
        try {
            return URLEncoder.encode(string, "UTF-8");
        } catch (Exception ex) {
            Logger.printException(() -> "encodeUrl failed", ex);
            return string;
        }
    }

    public static boolean hideSearchTermThumbnail() {
        return Settings.HIDE_SEARCH_TERM_THUMBNAIL.get();
    }

    public static boolean hideSearchTermThumbnail(boolean original) {
        return !hideSearchTermThumbnail() && original;
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
     * Injection point.
     * If the round search bar is enabled, the patch will not work.
     * Forcibly disable it.
     */
    public static boolean disableRoundSearchBar(boolean original) {
        return !Settings.HIDE_YOU_MAY_LIKE_SECTION.get() && original;
    }

    /**
     * Injection point.
     *
     * @param searchQuery Keywords entered in the search bar.
     * @return Whether the setting is enabled and the search query is empty.
     */
    public static boolean hideYouMayLikeSection(String searchQuery) {
        return Settings.HIDE_YOU_MAY_LIKE_SECTION.get()
                // The 'You may like' section is only visible when no search terms are entered.
                // To avoid unnecessary collection traversals, filtering is performed only when the searchQuery is empty.
                && StringUtils.isEmpty(searchQuery);
    }

    /**
     * Injection point.
     *
     * @param searchTerm This class contains information related to search terms.
     *                   The {@code toString()} method of this class overrides the search term.
     * @param endpoint   Endpoint related with the search term.
     *                   For search history, this value is:
     *                   '/complete/deleteitems?client=youtube-android-pb&delq=${searchTerm}&deltok=${token}'.
     *                   (If you long press on the search history,
     *                   you will see a dialog 'Remove from search history?')
     *                   For search suggestions, this value is null or empty.
     * @return Whether search term is a search history or not.
     */
    public static boolean isSearchHistory(Object searchTerm, String endpoint) {
        boolean isSearchHistory = endpoint != null && endpoint.contains("/delete");
        if (!isSearchHistory) {
            Logger.printDebug(() -> "Remove search suggestion: " + searchTerm);
        }
        return isSearchHistory;
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

    private static int settingsDrawableId = 0;
    private static int settingsCairoDrawableId = 0;

    public static int getCreateButtonDrawableId(int original) {
        if (!Settings.REPLACE_TOOLBAR_CREATE_BUTTON.get()) {
            return original;
        }

        if (settingsDrawableId == 0) {
            settingsDrawableId = ResourceUtils.getDrawableIdentifier("yt_outline_gear_black_24");
        }

        if (settingsDrawableId == 0) {
            return original;
        }

        // If the user has patched YouTube 19.26.42,
        // Or spoofed the app version to 19.26.42 or earlier.
        if (!ExtendedUtils.IS_19_28_OR_GREATER || ExtendedUtils.isSpoofingToLessThan("19.27.00")) {
            return settingsDrawableId;
        }

        if (settingsCairoDrawableId == 0) {
            settingsCairoDrawableId = ResourceUtils.getDrawableIdentifier("yt_outline_gear_cairo_black_24");
        }

        return settingsCairoDrawableId == 0
                ? settingsDrawableId
                : settingsCairoDrawableId;
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
        intent.setData(Uri.parse("revanced_settings_intent"));
        intent.setClassName(context.getPackageName(), "com.google.android.libraries.social.licenses.LicenseActivity");
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
                "TAB_ACTIVITY_CAIRO" // Notification button (New layout)
        );
    }

    private static boolean isSearchButton(String enumString) {
        return StringUtils.equalsAny(
                enumString,
                "SEARCH", // Search button
                "SEARCH_CAIRO", // Search button (New layout)
                "SEARCH_BOLD" // Search button (Shorts)
        );
    }

    // endregion

}

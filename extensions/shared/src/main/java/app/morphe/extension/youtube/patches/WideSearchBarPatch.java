/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/2221
 *
 * Portions of this file are modified by anddea:
 * Copyright (C) 2026 anddea
 * https://github.com/anddea/revanced-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.extension.youtube.patches;

import static app.morphe.extension.shared.utils.BaseThemeUtils.isDarkModeEnabled;
import static app.morphe.extension.shared.utils.ResourceUtils.getIdentifier;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.ResourceType;
import app.morphe.extension.shared.utils.ResourceUtils;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.NavigationBar;
import app.morphe.extension.youtube.shared.NavigationBar.NavigationButton;

@SuppressWarnings("unused")
public class WideSearchBarPatch {

    private static final boolean WIDE_SEARCHBAR_ENABLED = Settings.ENABLE_WIDE_SEARCH_BAR.get();
    private static final boolean WIDE_SEARCHBAR_WITH_HEADER_ENABLED =
            Settings.ENABLE_WIDE_SEARCH_BAR_WITH_HEADER.get();
    private static final boolean WIDE_SEARCHBAR_YOU_TAB_ENABLED =
            Settings.ENABLE_WIDE_SEARCH_BAR_IN_YOU_TAB.get();
    private static final int ID_YOUTUBE_LOGO = getIdentifier("youtube_logo", ResourceType.ID);
    private static final int ID_SEARCH_ICON =
            getIdentifier("revanced_settings_search_icon", ResourceType.DRAWABLE);
    private static final String SEARCH_HINT = ResourceUtils.getString("search_hint");
    private static final int DP115 = Utils.dipToPixels(115);

    private static WeakReference<View> searchButtonViewParentRef = new WeakReference<>(null);
    private static WeakReference<View> searchButtonViewRef = new WeakReference<>(null);

    static {
        // Change listener is needed to handle YT hardware back button handler
        // that runs out of order with UI update code.
        NavigationBar.addOnNavigationButtonChangedListener(activeButton ->
                hideSearchButton(searchButtonViewParentRef.get(), activeButton)
        );
    }

    /**
     * Injection point.
     */
    public static void setSearchButtonView(String enumName, View parentView, ImageView imageView) {
        if (WIDE_SEARCHBAR_ENABLED && NavigationButton.SEARCH.ytEnumNames.contains(enumName)) {
            searchButtonViewParentRef = new WeakReference<>(parentView);
            searchButtonViewRef = new WeakReference<>(imageView);
            hideSearchButton(parentView, NavigationButton.getSelectedNavigationButton());
        }
    }

    private static void hideSearchButton(@Nullable View searchParentView, @Nullable NavigationButton activeButton) {
        try {
            if (searchParentView == null) {
                return;
            }
            if (activeButton != NavigationButton.HOME &&
                    activeButton != NavigationButton.SUBSCRIPTIONS &&
                    activeButton != NavigationButton.NOTIFICATIONS &&
                    !(WIDE_SEARCHBAR_YOU_TAB_ENABLED && activeButton == NavigationButton.LIBRARY)) {
                return;
            }
            if (NavigationBar.isBackButtonVisible()) {
                return; // User has navigated into a channel page or other subpage.
            }

            searchParentView.setVisibility(View.GONE);
        } catch (Exception ex) {
            Logger.printException(() -> "hideSearchButton failure", ex);
        }
    }

    /**
     * Injection point.
     */
    public static void initializeWideSearchbar(View toolbar) {
        try {
            if (!WIDE_SEARCHBAR_ENABLED) {
                return;
            }

            if (!(toolbar instanceof ViewGroup toolbarViewGroup)) {
                return;
            }

            final boolean rightToLeftLocale = Utils.isRightToLeftLocale();
            final boolean darkModeEnabled = isDarkModeEnabled();
            final int textColor = Color.parseColor(darkModeEnabled
                    ? "#AAAAAA"
                    : "#606060");
            final int backgroundColor = Color.parseColor(darkModeEnabled
                    ? "#1A1A1A"
                    : "#F2F2F2");

            TextView wideSearchBox = new TextView(toolbarViewGroup.getContext());
            wideSearchBox.setPadding(Utils.dipToPixels(12), 0, Utils.dipToPixels(12), 0);
            wideSearchBox.setText(SEARCH_HINT);
            wideSearchBox.setTextSize(16);
            wideSearchBox.setTextColor(textColor);
            wideSearchBox.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            wideSearchBox.setSingleLine(true);
            wideSearchBox.setEllipsize(TextUtils.TruncateAt.END);
            wideSearchBox.setFocusable(false);
            wideSearchBox.setClickable(true);

            GradientDrawable searchBackground = new GradientDrawable();
            searchBackground.setShape(GradientDrawable.RECTANGLE);
            searchBackground.setCornerRadius(Utils.dipToPixels(24));
            searchBackground.setColor(backgroundColor);
            wideSearchBox.setBackground(searchBackground);

            Drawable searchIcon = wideSearchBox.getContext().getDrawable(ID_SEARCH_ICON);
            if (searchIcon != null) {
                searchIcon = searchIcon.mutate();
                searchIcon.setTint(textColor);

                if (rightToLeftLocale) {
                    wideSearchBox.setCompoundDrawablesWithIntrinsicBounds(null, null, searchIcon, null);
                } else {
                    wideSearchBox.setCompoundDrawablesWithIntrinsicBounds(searchIcon, null, null, null);
                }
                wideSearchBox.setCompoundDrawablePadding(Utils.dipToPixels(8));
            }

            View logoView = toolbarViewGroup.findViewById(ID_YOUTUBE_LOGO);
            if (!WIDE_SEARCHBAR_WITH_HEADER_ENABLED && logoView != null) {
                logoView.setVisibility(View.GONE);
            }
            final int sideMargin = Utils.dipToPixels(10);
            final int searchBarHeight = Utils.dipToPixels(32);

            ViewGroup.MarginLayoutParams currentViewGroupParams;
            if (toolbarViewGroup instanceof LinearLayout) {
                LinearLayout.LayoutParams linearParams = new LinearLayout.LayoutParams(
                        0, searchBarHeight
                );
                linearParams.weight = 1.0f;
                linearParams.gravity = Gravity.CENTER_VERTICAL;
                currentViewGroupParams = linearParams;
                currentViewGroupParams.setMargins(sideMargin, 0, sideMargin, 0);
            } else {
                currentViewGroupParams = new ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, searchBarHeight
                );

                int leftMargin = sideMargin;
                int rightMargin = sideMargin;

                if (WIDE_SEARCHBAR_WITH_HEADER_ENABLED && logoView != null) {
                    final int measuredWidth = logoView.getMeasuredWidth();
                    final int logoWidth = measuredWidth > 0 ? measuredWidth : DP115;
                    final int logoMargin = logoWidth + Utils.dipToPixels(16);

                    if (rightToLeftLocale) {
                        rightMargin = logoMargin;
                    } else {
                        leftMargin = logoMargin;
                    }
                }

                currentViewGroupParams.setMargins(leftMargin, 0, rightMargin, 0);

                if (toolbarViewGroup instanceof FrameLayout) {
                    FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
                            currentViewGroupParams);
                    frameParams.gravity = Gravity.CENTER_VERTICAL;
                    currentViewGroupParams = frameParams;
                }
            }
            wideSearchBox.setLayoutParams(currentViewGroupParams);

            wideSearchBox.setOnClickListener(view -> {
                View searchButtonView = searchButtonViewRef.get();
                if (searchButtonView != null) {
                    searchButtonView.callOnClick();
                } else {
                    Logger.printDebug(() -> "Using search intent");
                    Intent intent = new Intent();
                    intent.setAction("com.google.android.youtube.action.open.search");
                    Context context = Utils.getActivity();
                    intent.setPackage(context.getPackageName());
                    context.startActivity(intent);
                }
            });

            int targetIndex = toolbarViewGroup.getChildCount();
            if (logoView != null) {
                final int logoIndex = toolbarViewGroup.indexOfChild(logoView);
                if (logoIndex >= 0) {
                    targetIndex = logoIndex + 1;
                }
            }

            toolbarViewGroup.addView(wideSearchBox, targetIndex);
        } catch (Exception ex) {
            Logger.printException(() -> "initializeContainer failure", ex);
        }
    }
}

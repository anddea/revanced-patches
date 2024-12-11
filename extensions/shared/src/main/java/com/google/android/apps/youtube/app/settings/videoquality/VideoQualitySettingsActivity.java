package com.google.android.apps.youtube.app.settings.videoquality;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;
import android.widget.Toolbar;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.ResourceUtils;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.settings.preference.ReVancedPreferenceFragment;
import app.revanced.extension.youtube.utils.ThemeUtils;

@SuppressWarnings("deprecation")
public class VideoQualitySettingsActivity extends Activity {

    private static final String rvxSettingsLabel = ResourceUtils.getString("revanced_extended_settings_title");
    private static final String searchLabel = ResourceUtils.getString("revanced_extended_settings_search_title");
    private static WeakReference<SearchView> searchViewRef = new WeakReference<>(null);
    private ReVancedPreferenceFragment fragment;

    private final OnQueryTextListener onQueryTextListener = new OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            filterPreferences(query);
            return true;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            filterPreferences(newText);
            return true;
        }
    };

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Utils.getLocalizedContextAndSetResources(base));
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        try {
            // Set fragment theme
            setTheme(ThemeUtils.getThemeId());

            // Set content
            setContentView(ResourceUtils.getLayoutIdentifier("revanced_settings_with_toolbar"));

            String dataString = getIntent().getDataString();
            if (dataString == null) {
                Logger.printException(() -> "DataString is null");
                return;
            } else if (dataString.equals("revanced_extended_settings_intent")) {
                fragment = new ReVancedPreferenceFragment();
            } else {
                Logger.printException(() -> "Unknown setting: " + dataString);
                return;
            }

            // Set toolbar
            setToolbar();

            getFragmentManager()
                    .beginTransaction()
                    .replace(ResourceUtils.getIdIdentifier("revanced_settings_fragments"), fragment)
                    .commit();

            setSearchView();
        } catch (Exception ex) {
            Logger.printException(() -> "onCreate failure", ex);
        }
    }

    private void filterPreferences(String query) {
        if (fragment == null) return;
        fragment.filterPreferences(query);
    }

    private void setToolbar() {
        if (!(findViewById(ResourceUtils.getIdIdentifier("revanced_toolbar_parent")) instanceof ViewGroup toolBarParent))
            return;

        // Remove dummy toolbar.
        for (int i = 0; i < toolBarParent.getChildCount(); i++) {
            View view = toolBarParent.getChildAt(i);
            if (view != null) {
                toolBarParent.removeView(view);
            }
        }

        Toolbar toolbar = new Toolbar(toolBarParent.getContext());
        toolbar.setBackgroundColor(ThemeUtils.getToolbarBackgroundColor());
        toolbar.setNavigationIcon(ThemeUtils.getBackButtonDrawable());
        toolbar.setNavigationOnClickListener(view -> VideoQualitySettingsActivity.this.onBackPressed());
        toolbar.setTitle(rvxSettingsLabel);
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        toolbar.setTitleMarginStart(margin);
        toolbar.setTitleMarginEnd(margin);
        TextView toolbarTextView = Utils.getChildView(toolbar, view -> view instanceof TextView);
        if (toolbarTextView != null) {
            toolbarTextView.setTextColor(ThemeUtils.getForegroundColor());
        }
        toolBarParent.addView(toolbar, 0);
    }

    private void setSearchView() {
        SearchView searchView = findViewById(ResourceUtils.getIdIdentifier("search_view"));

        // region compose search hint

        // if the translation is missing the %s, then it
        // will use the default search hint for that language
        String finalSearchHint = String.format(searchLabel, rvxSettingsLabel);

        searchView.setQueryHint(finalSearchHint);

        // endregion

        // region set the font size

        try {
            // 'android.widget.SearchView' has been deprecated quite a long time ago
            // So access the SearchView's EditText via reflection
            Field field = searchView.getClass().getDeclaredField("mSearchSrcTextView");
            field.setAccessible(true);

            // Set the font size
            if (field.get(searchView) instanceof EditText searchEditText) {
                searchEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            }
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            Logger.printException(() -> "Reflection error accessing mSearchSrcTextView", ex);
        }

        // endregion

        // region SearchView dimensions

        // Get the current layout parameters of the SearchView
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) searchView.getLayoutParams();

        // Set the margins (in pixels)
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics()); // for example, 10dp
        layoutParams.setMargins(margin, layoutParams.topMargin, margin, layoutParams.bottomMargin);

        // Apply the layout parameters to the SearchView
        searchView.setLayoutParams(layoutParams);

        // endregion

        // region SearchView color

        searchView.setBackground(ThemeUtils.getSearchViewShape());

        // endregion

        // Set the listener for query text changes
        searchView.setOnQueryTextListener(onQueryTextListener);

        // Keep a weak reference to the SearchView
        searchViewRef = new WeakReference<>(searchView);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        SearchView searchView = searchViewRef.get();
        if (!hasFocus && searchView != null && searchView.getQuery().length() == 0) {
            searchView.clearFocus();
        }
    }
}

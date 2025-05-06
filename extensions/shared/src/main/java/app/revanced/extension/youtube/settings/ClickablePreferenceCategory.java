package app.revanced.extension.youtube.settings;

import android.content.Context;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.view.View;
import android.widget.TextView;

/**
 * A custom PreferenceCategory that can be clicked to navigate to a sub-screen
 * and features improved visual spacing to distinguish it from regular preferences.
 * <p>
 * Uses a top space drawable to create visual separation from the previous preference.
 */
@SuppressWarnings("deprecation")
public class ClickablePreferenceCategory extends PreferenceCategory {

    /**
     * Creates a new ClickablePreferenceCategory with optional navigation to a sub-screen
     *
     * @param fragment  The PreferenceFragment this category belongs to
     * @param subScreen The PreferenceScreen to navigate to when clicked, or null if no navigation
     */
    public ClickablePreferenceCategory(PreferenceFragment fragment, PreferenceScreen subScreen) {
        super(fragment.getContext());

        if (subScreen != null) {
            setOnPreferenceClickListener(p -> {
                fragment.setPreferenceScreen(subScreen);
                return true;
            });
        }
        setSelectable(true);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        Context context = getContext();
        float density = context.getResources().getDisplayMetrics().density;

        // Set padding on title TextView (16dp start/end, 8dp top/bottom)
        TextView titleView = view.findViewById(android.R.id.title);
        if (titleView != null) {
            int paddingStartEnd = (int) (16 * density);
            int paddingTopBottom = (int) (8 * density);
            titleView.setPadding(paddingStartEnd, paddingTopBottom, paddingStartEnd, paddingTopBottom);
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}

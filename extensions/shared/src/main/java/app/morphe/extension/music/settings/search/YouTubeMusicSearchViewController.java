package app.morphe.extension.music.settings.search;

import android.app.Activity;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.view.View;
import android.widget.Toolbar;

import app.morphe.extension.music.settings.preference.YouTubeMusicPreferenceFragment;
import app.morphe.extension.shared.settings.search.BaseSearchResultItem;
import app.morphe.extension.shared.settings.search.BaseSearchResultsAdapter;
import app.morphe.extension.shared.settings.search.BaseSearchViewController;

/**
 * YouTube Music-specific search view controller implementation.
 */
@SuppressWarnings("deprecation")
public class YouTubeMusicSearchViewController extends BaseSearchViewController {

    public static YouTubeMusicSearchViewController addSearchViewComponents(Activity activity, Toolbar toolbar,
                                                                           YouTubeMusicPreferenceFragment fragment) {
        return new YouTubeMusicSearchViewController(activity, toolbar, fragment);
    }

    private YouTubeMusicSearchViewController(Activity activity, Toolbar toolbar, YouTubeMusicPreferenceFragment fragment) {
        super(activity, toolbar, new PreferenceFragmentAdapter(fragment));
    }

    @Override
    protected BaseSearchResultsAdapter createSearchResultsAdapter() {
        return new YouTubeMusicSearchResultsAdapter(activity, filteredSearchItems, fragment, this);
    }

    @Override
    protected boolean isSpecialPreferenceGroup(Preference preference) {
        // YouTube Music doesn't have SponsorBlock, so no special groups.
        return false;
    }

    @Override
    protected void setupSpecialPreferenceListeners(BaseSearchResultItem item) {
        // YouTube Music doesn't have special preferences.
        // This method can be empty or handle music-specific preferences if any.
    }

    public static boolean handleBackPress(YouTubeMusicSearchViewController searchViewController) {
        if (searchViewController != null && searchViewController.isSearchActive()) {
            searchViewController.closeSearch();
            return false;
        }
        return true;
    }

    // Adapter to wrap YouTubeMusicPreferenceFragment to BasePreferenceFragment interface.
    private record PreferenceFragmentAdapter(
            YouTubeMusicPreferenceFragment fragment) implements BasePreferenceFragment {

        @Override
        public PreferenceScreen getPreferenceScreenForSearch() {
            return fragment.getPreferenceScreenForSearch();
        }

        @Override
        public View getView() {
            return fragment.getView();
        }

        @Override
        public Activity getActivity() {
            return fragment.getActivity();
        }
    }
}

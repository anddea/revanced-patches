package app.morphe.extension.music.settings.search;

import android.content.Context;
import android.preference.PreferenceScreen;

import java.util.List;

import app.morphe.extension.shared.settings.search.BaseSearchResultItem;
import app.morphe.extension.shared.settings.search.BaseSearchResultsAdapter;
import app.morphe.extension.shared.settings.search.BaseSearchViewController;

/**
 * YouTube Music-specific search results adapter.
 */
public class YouTubeMusicSearchResultsAdapter extends BaseSearchResultsAdapter {

    public YouTubeMusicSearchResultsAdapter(Context context, List<BaseSearchResultItem> items,
                                            BaseSearchViewController.BasePreferenceFragment fragment,
                                            BaseSearchViewController searchViewController) {
        super(context, items, fragment, searchViewController);
    }

    @Override
    protected PreferenceScreen getMainPreferenceScreen() {
        return fragment.getPreferenceScreenForSearch();
    }
}

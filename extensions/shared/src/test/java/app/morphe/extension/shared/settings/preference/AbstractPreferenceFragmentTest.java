package app.morphe.extension.shared.settings.preference;

import static org.junit.Assert.assertEquals;

import android.app.Activity;
import android.content.Context;
import android.preference.ListPreference;
import android.preference.PreferenceScreen;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import app.morphe.extension.shared.settings.Setting;
import app.morphe.extension.shared.settings.StringSetting;
import app.morphe.extension.shared.utils.Utils;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class AbstractPreferenceFragmentTest {
    @Before
    public void setUp() {
        Utils.setContext(RuntimeEnvironment.getApplication());
        Setting.preferences.clear();
    }

    @Test
    public void duplicateListPreferencesStayInSyncAfterOneChanges() {
        String key = "revanced_test_duplicate_list_" + System.nanoTime();
        StringSetting setting = new StringSetting(key, "default");
        TestPreferenceFragment fragment = new TestPreferenceFragment(setting);

        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        activity.getFragmentManager()
                .beginTransaction()
                .add(fragment, null)
                .commit();
        activity.getFragmentManager().executePendingTransactions();

        fragment.secondPreference.setValue("other");

        assertEquals("other", setting.get());
        assertEquals("other", fragment.firstPreference.getValue());
        assertEquals("other", fragment.secondPreference.getValue());
    }

    private static class TestPreferenceFragment extends AbstractPreferenceFragment {
        private final StringSetting setting;
        private ListPreference firstPreference;
        private ListPreference secondPreference;

        private TestPreferenceFragment(StringSetting setting) {
            this.setting = setting;
        }

        @Override
        protected void initialize() {
            Context context = getActivity();
            PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);
            setPreferenceScreen(screen);

            firstPreference = createListPreference(context);
            secondPreference = createListPreference(context);

            screen.addPreference(firstPreference);
            screen.addPreference(secondPreference);
        }

        private ListPreference createListPreference(Context context) {
            ListPreference preference = new ListPreference(context);
            preference.setKey(setting.key);
            preference.setEntries(new CharSequence[]{"Default", "Other"});
            preference.setEntryValues(new CharSequence[]{setting.defaultValue, "other"});
            return preference;
        }
    }
}

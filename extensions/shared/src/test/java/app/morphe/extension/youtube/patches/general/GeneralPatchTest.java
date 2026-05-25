package app.morphe.extension.youtube.patches.general;

import static org.junit.Assert.assertEquals;

import android.app.Application;
import android.content.pm.PackageInfo;

import app.morphe.extension.shared.settings.Setting;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.settings.Settings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28)
public class GeneralPatchTest {
    @Before
    public void setUp() throws ClassNotFoundException {
        Application application = RuntimeEnvironment.getApplication();
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = application.getPackageName();
        packageInfo.versionName = "20.05.46";
        Shadows.shadowOf(application.getPackageManager()).installPackage(packageInfo);

        Utils.setContext(application);
        Setting.preferences.clear();
        Class.forName(Settings.class.getName());
        Settings.RESTORE_OLD_SETTINGS_MENUS.resetToDefault();
    }

    @Test
    public void legacySettingsFragmentReturnsOriginalWhenRestoreOldSettingsMenusAreDisabled() {
        int originalResourceId = 12345;

        assertEquals(originalResourceId, GeneralPatch.useLegacySettingsFragment(originalResourceId, () -> 67890));
    }

    @Test
    public void legacySettingsFragmentUsesLegacyResourceWhenRestoreOldSettingsMenusAreEnabled() {
        Settings.RESTORE_OLD_SETTINGS_MENUS.save(true);
        int legacyResourceId = 67890;

        assertEquals(legacyResourceId, GeneralPatch.useLegacySettingsFragment(12345, () -> legacyResourceId));
    }

    @Test
    public void legacySettingsFragmentFallsBackToOriginalWhenLegacyResourceIsMissing() {
        Settings.RESTORE_OLD_SETTINGS_MENUS.save(true);
        int originalResourceId = 12345;

        assertEquals(originalResourceId, GeneralPatch.useLegacySettingsFragment(originalResourceId, () -> 0));
    }
}

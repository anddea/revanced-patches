package app.morphe.extension.youtube.settings;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import android.app.Application;
import android.content.pm.PackageInfo;

import app.morphe.extension.shared.settings.BooleanSetting;
import app.morphe.extension.shared.settings.Setting;
import app.morphe.extension.shared.utils.Utils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28)
public class YouTubeActivityHookTest {
    private static final String RESTORE_OLD_SETTINGS_MENUS_KEY = "revanced_restore_old_settings_menus";

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
    public void restoreOldSettingsMenusSettingIsRegistered() {
        BooleanSetting setting = (BooleanSetting) Setting.getSettingFromPath(RESTORE_OLD_SETTINGS_MENUS_KEY);
        assertNotNull(setting);

        assertFalse(setting.get());
    }
}

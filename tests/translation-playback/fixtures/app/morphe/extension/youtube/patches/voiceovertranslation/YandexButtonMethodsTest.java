package app.morphe.extension.youtube.patches.voiceovertranslation;

import static org.junit.Assert.*;
import app.morphe.extension.youtube.shared.VideoInformation;
import app.morphe.extension.youtube.settings.Settings;
import org.junit.*;

public class YandexButtonMethodsTest {
    @Before public void before() throws Exception {
        new YandexPauseMethodsTest().before();
        YandexButtonMethods.INSTANCE.setInstance(new PlayerControlButton(new View()));
        YandexButtonMethods.INSTANCE.setMenus(0);
    }
    @Test public void tapDuringWaitingCancelsInsteadOfOpeningMenu() {
        YandexPauseMethods.isTranslating.set(true);
        YandexButtonMethods.INSTANCE.click();
        assertFalse(YandexPauseMethods.isTranslationRequested());
        assertTrue(VideoInformation.playing);
        assertEquals(0, YandexButtonMethods.INSTANCE.getMenus());
        assertFalse(YandexButtonMethods.INSTANCE.getInstance().imageView().isActivated());
        YandexButtonMethods.INSTANCE.click();
        assertTrue(YandexPauseMethods.isTranslationRequested());
        assertTrue(YandexButtonMethods.INSTANCE.getInstance().imageView().isActivated());
    }
    @Test public void accessibilityAndIconIncludePendingRequestsEvenWithoutPause() {
        Settings.VOT_PAUSE_VIDEO_WHILE_PREPARING_TRANSLATION.value = false;
        for (boolean pending : new boolean[] {true, false}) {
            YandexPauseMethods.isTranslating.set(pending);
            YandexButtonMethods.INSTANCE.refresh();
            var node = YandexButtonMethods.INSTANCE.accessibility();
            assertEquals(pending, node.isChecked());
            assertTrue(node.isCheckable());
            assertTrue(node.getInitialized());
            assertEquals(ToggleButton.class.getName(), node.getClassName());
            assertEquals(pending, YandexButtonMethods.INSTANCE.getInstance().imageView().isActivated());
        }
    }
    @Test public void tappingSurvivesDetachedControl() {
        YandexButtonMethods.INSTANCE.setInstance(null);
        YandexButtonMethods.INSTANCE.click();
        assertFalse(YandexPauseMethods.isTranslationRequested());
        YandexButtonMethods.INSTANCE.setInstance(new PlayerControlButton(null));
        YandexButtonMethods.INSTANCE.click();
        assertTrue(YandexPauseMethods.isTranslationRequested());
        YandexButtonMethods.INSTANCE.refresh();
    }
}

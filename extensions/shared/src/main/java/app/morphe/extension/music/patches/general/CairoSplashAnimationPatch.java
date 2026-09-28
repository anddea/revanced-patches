package app.morphe.extension.music.patches.general;

import app.morphe.extension.music.settings.Settings;

@SuppressWarnings("unused")
public class CairoSplashAnimationPatch {

    public static boolean disableCairoSplashAnimation(boolean original) {
        return original
                && !Settings.DISABLE_CAIRO_SPLASH_ANIMATION.get()
                && "original".equals(Settings.CUSTOM_BRANDING_ICON.get());
    }
}

package app.morphe.extension.shared.patches;

import java.util.regex.Pattern;

import app.morphe.extension.shared.settings.BaseSettings;
import app.morphe.extension.shared.utils.Logger;

@SuppressWarnings("unused")
public final class BypassImageRegionRestrictionsPatch {

    private static final boolean BYPASS_IMAGE_REGION_RESTRICTIONS_ENABLED = BaseSettings.BYPASS_IMAGE_REGION_RESTRICTIONS.get();
    private static final String REPLACEMENT_IMAGE_DOMAIN = BaseSettings.BYPASS_IMAGE_REGION_RESTRICTIONS_DOMAIN.get();

    /**
     * YouTube static images domain.  Includes user and channel avatar images and community post images.
     */
    private static final Pattern YOUTUBE_STATIC_IMAGE_DOMAIN_PATTERN = Pattern.compile("(ap[1-2]|gm[1-4]|gz0|(cp|ci|gp|lh)[3-6]|sp[1-3]|yt[3-4]|(play|ccp)-lh)\\.(ggpht|googleusercontent)\\.com");

    public static String overrideImageURL(String originalUrl) {
        try {
            if (BYPASS_IMAGE_REGION_RESTRICTIONS_ENABLED) {
                final String replacement = YOUTUBE_STATIC_IMAGE_DOMAIN_PATTERN
                        .matcher(originalUrl).replaceFirst(REPLACEMENT_IMAGE_DOMAIN);
                if (!replacement.equals(originalUrl)) {
                    Logger.printDebug(() -> "Replaced: '" + originalUrl + "' with: '" + replacement + "'");
                }
                return replacement;
            }
        } catch (Exception ex) {
            Logger.printException(() -> "overrideImageURL failure", ex);
        }
        return originalUrl;
    }
}
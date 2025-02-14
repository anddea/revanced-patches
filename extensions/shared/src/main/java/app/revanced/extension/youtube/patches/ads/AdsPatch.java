package app.revanced.extension.youtube.patches.ads;

import static app.revanced.extension.shared.utils.Utils.hideViewBy0dpUnderCondition;

import android.view.View;

import java.util.List;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class AdsPatch {
    private static final boolean HIDE_END_SCREEN_STORE_BANNER =
            Settings.HIDE_END_SCREEN_STORE_BANNER.get();
    private static final boolean HIDE_GENERAL_ADS =
            Settings.HIDE_GENERAL_ADS.get();
    private static final boolean HIDE_GET_PREMIUM =
            Settings.HIDE_GET_PREMIUM.get();
    private static final boolean HIDE_VIDEO_ADS =
            Settings.HIDE_VIDEO_ADS.get();

    // https://encrypted-tbn0.gstatic.com/shopping?q=tbn
    private static final String STORE_BANNER_DOMAIN =
            "gstatic.com/shopping";

    /**
     * Injection point.
     * Hide the view, which shows ads in the homepage.
     *
     * @param view The view, which shows ads.
     */
    public static void hideAdAttributionView(View view) {
        hideViewBy0dpUnderCondition(HIDE_GENERAL_ADS, view);
    }

    /**
     * Injection point.
     *
     * @param elementsList List of components of the end screen container.
     * @param protobufList Component (ProtobufList).
     */
    public static void hideEndScreenStoreBanner(List<Object> elementsList, Object protobufList) {
        if (HIDE_END_SCREEN_STORE_BANNER &&
                protobufList.toString().contains(STORE_BANNER_DOMAIN)) {
            Logger.printDebug(() -> "Hiding store banner");
            return;
        }

        elementsList.add(protobufList);
    }

    /**
     * Injection point.
     */
    public static boolean hideGetPremium() {
        return HIDE_GET_PREMIUM;
    }

    /**
     * Injection point.
     */
    public static boolean hideVideoAds() {
        return !HIDE_VIDEO_ADS;
    }

    /**
     * Injection point.
     * <p>
     * Only used by old clients.
     * It is presumed to have been deprecated, and if it is confirmed that it is no longer used, remove it.
     */
    public static boolean hideVideoAds(boolean original) {
        return !HIDE_VIDEO_ADS && original;
    }

}

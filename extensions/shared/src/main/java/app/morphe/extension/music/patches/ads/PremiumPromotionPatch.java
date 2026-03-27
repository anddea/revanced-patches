package app.morphe.extension.music.patches.ads;

import static app.morphe.extension.shared.utils.StringRef.str;

import android.app.Dialog;
import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import app.morphe.extension.music.settings.Settings;
import app.morphe.extension.shared.settings.BaseSettings;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

@SuppressWarnings("unused")
public class PremiumPromotionPatch {
    private static final boolean HIDE_PREMIUM_PROMOTION =
            Settings.HIDE_PREMIUM_PROMOTION.get();

    public static void hidePremiumPromotionBottomSheet(View view) {
        if (HIDE_PREMIUM_PROMOTION) {
            view.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                try {
                    if (!(view instanceof ViewGroup viewGroup)) {
                        return;
                    }
                    if (!(viewGroup.getChildAt(0) instanceof ViewGroup mealBarLayoutRoot)) {
                        return;
                    }
                    if (!(mealBarLayoutRoot.getChildAt(0) instanceof LinearLayout linearLayout)) {
                        return;
                    }
                    if (!(linearLayout.getChildAt(0) instanceof ImageView imageView)) {
                        return;
                    }
                    if (imageView.getVisibility() == View.VISIBLE) {
                        view.setVisibility(View.GONE);
                    }
                } catch (Exception ex) {
                    Logger.printException(() -> "hidePremiumPromotionBottomSheet failure", ex);
                }
            });
        }
    }

    /**
     * YouTube Premium promotion dialog is shown under the following conditions:
     * 1. Patch YouTube Music 7.28.51 or later.
     * 2. Log in with a Non-Premium account.
     * 3. Change the Default client (Spoof client) to Android Music 4.27.53 or 5.29.53.
     * 4. Play music.
     * 5. Switch to the background.
     * 6. Turn off the screen and turn it back on.
     * 7. Switch to the foreground.
     * 8. YouTube Premium promotion dialog is shown.
     * <p>
     * In other words, if a dialog builder is called within 1000ms of the app being switched to the foreground,
     * it is very likely a YouTube Premium promotion dialog.
     */
    private static volatile boolean promotionDialogShown = false;
    private static long foregroundStartTime = -1L;

    /**
     * Injection point.
     */
    public static void onAppBackgrounded() {
        if (HIDE_PREMIUM_PROMOTION && !promotionDialogShown) {
            foregroundStartTime = 0L;
        }
    }

    /**
     * Injection point.
     */
    public static void onAppForegrounded() {
        if (HIDE_PREMIUM_PROMOTION && !promotionDialogShown && foregroundStartTime == 0L) {
            foregroundStartTime = System.currentTimeMillis();
        }
    }

    public static void hidePremiumPromotionDialog(Dialog dialog, View contentView) {
        if (HIDE_PREMIUM_PROMOTION && !promotionDialogShown) {
            final long foregroundTime = System.currentTimeMillis() - foregroundStartTime;
            if (foregroundTime < 1000L) {
                promotionDialogShown = true;
                dialog.setOnShowListener(DialogInterface::dismiss);
                if (BaseSettings.DEBUG.get()) {
                    Utils.showToastShort(str("revanced_hide_premium_promotion_closed_toast"));
                }
                return;
            }
        }
        dialog.setContentView(contentView);
    }
}
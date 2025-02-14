package app.revanced.extension.music.patches.ads;

import static app.revanced.extension.music.patches.general.GeneralPatch.disableDimBehind;
import static app.revanced.extension.shared.utils.StringRef.str;

import android.app.Dialog;
import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import app.revanced.extension.music.settings.Settings;
import app.revanced.extension.shared.settings.BaseSettings;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;

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

    public static void hidePremiumPromotionDialog(Dialog dialog, View contentView) {
        if (HIDE_PREMIUM_PROMOTION) {
            disableDimBehind(dialog.getWindow());
            dialog.setOnShowListener(DialogInterface::dismiss);
            if (BaseSettings.ENABLE_DEBUG_LOGGING.get()) {
                Utils.showToastShort(str("revanced_hide_premium_promotion_closed_toast"));
            }
        } else {
            dialog.setContentView(contentView);
        }
    }
}
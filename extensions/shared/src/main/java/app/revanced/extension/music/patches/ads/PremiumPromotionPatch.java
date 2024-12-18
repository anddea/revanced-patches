package app.revanced.extension.music.patches.ads;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import app.revanced.extension.music.settings.Settings;
import app.revanced.extension.shared.utils.Logger;

@SuppressWarnings("unused")
public class PremiumPromotionPatch {

    public static void hidePremiumPromotion(View view) {
        if (!Settings.HIDE_PREMIUM_PROMOTION.get())
            return;

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
                Logger.printException(() -> "hideGetPremium failure", ex);
            }
        });
    }
}
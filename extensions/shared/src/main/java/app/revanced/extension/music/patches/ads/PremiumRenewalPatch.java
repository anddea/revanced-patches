package app.revanced.extension.music.patches.ads;

import static app.revanced.extension.shared.utils.StringRef.str;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import app.revanced.extension.music.settings.Settings;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;

@SuppressWarnings("unused")
public class PremiumRenewalPatch {

    public static void hidePremiumRenewal(LinearLayout buttonContainerView) {
        if (!Settings.HIDE_PREMIUM_RENEWAL.get())
            return;

        buttonContainerView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            try {
                Utils.runOnMainThreadDelayed(() -> {
                            if (!(buttonContainerView.getChildAt(0) instanceof ViewGroup closeButtonParentView))
                                return;
                            if (!(closeButtonParentView.getChildAt(0) instanceof TextView closeButtonView))
                                return;
                            if (closeButtonView.getText().toString().equals(str("dialog_got_it_text")))
                                Utils.clickView(closeButtonView);
                            else
                                Utils.hideViewByLayoutParams((View) buttonContainerView.getParent());
                        }, 0
                );
            } catch (Exception ex) {
                Logger.printException(() -> "hidePremiumRenewal failure", ex);
            }
        });
    }
}

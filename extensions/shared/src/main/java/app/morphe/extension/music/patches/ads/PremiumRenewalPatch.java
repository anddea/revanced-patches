package app.morphe.extension.music.patches.ads;

import static app.morphe.extension.shared.utils.StringRef.str;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import app.morphe.extension.music.settings.Settings;
import app.morphe.extension.music.shared.NavigationBar;
import app.morphe.extension.shared.utils.Utils;

@SuppressWarnings("unused")
public class PremiumRenewalPatch {
    private static final String dialogGotItText =
            str("dialog_got_it_text");

    public static void hidePremiumRenewal(LinearLayout buttonContainerView) {
        if (!Settings.HIDE_PREMIUM_RENEWAL.get())
            return;

        buttonContainerView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (NavigationBar.getNavigationTabIndex() == 0) {
                // Always hide the banner when the navigation bar index is 0.
                hideParentViewByLayoutParams(buttonContainerView);
            } else {
                // This banner is exposed to the library as well as the home.
                // In this case, it is necessary to check whether the text of the button is 'Got it' or not.
                if (!(buttonContainerView.getChildAt(0) instanceof ViewGroup closeButtonParentView))
                    return;
                if (!(closeButtonParentView.getChildAt(0) instanceof TextView closeButtonView))
                    return;
                // If the text of the button is 'Got it', just click the button.
                // If not, tab sometimes becomes freezing.
                if (closeButtonView.getText().toString().equals(dialogGotItText)) {
                    Utils.clickView(closeButtonView);
                } else {
                    hideParentViewByLayoutParams(buttonContainerView);
                }
            }
        });
    }

    private static void hideParentViewByLayoutParams(View view) {
        if (view.getParent() instanceof View parentView) {
            Utils.hideViewByLayoutParams(parentView);
        }
    }
}

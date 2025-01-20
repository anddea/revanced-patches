package app.revanced.extension.reddit.patches;

import static app.revanced.extension.shared.utils.StringRef.str;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import app.revanced.extension.reddit.settings.Settings;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;

@SuppressWarnings("unused")
public class RemoveSubRedditDialogPatch {

    public static void confirmDialog(@NonNull TextView textView) {
        if (!Settings.REMOVE_NSFW_DIALOG.get())
            return;

        if (!textView.getText().toString().equals(str("nsfw_continue_non_anonymously")))
            return;

        clickViewDelayed(textView);
    }

    public static void dismissDialog(View cancelButtonView) {
        if (!Settings.REMOVE_NOTIFICATION_DIALOG.get())
            return;

        clickViewDelayed(cancelButtonView);
    }

    public static void dismissDialogV2(Object object) {
        if (!Settings.REMOVE_NOTIFICATION_DIALOG.get())
            return;

        Utils.runOnMainThreadDelayed(() -> {
            try {
                dismissRedditDialogV2(object);
            } catch (Exception ex) {
                Logger.printException(() -> "dismissDialogV2 failed", ex);
            }
        }, 0);
    }

    private static void dismissRedditDialogV2(Object object) {
    }

    private static void clickViewDelayed(View view) {
        Utils.runOnMainThreadDelayed(() -> {
            if (view != null) {
                view.setSoundEffectsEnabled(false);
                view.performClick();
            }
        }, 0);
    }
}

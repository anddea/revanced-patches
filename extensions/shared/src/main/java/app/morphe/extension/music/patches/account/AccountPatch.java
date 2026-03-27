package app.morphe.extension.music.patches.account;

import static app.morphe.extension.shared.utils.StringRef.str;
import static app.morphe.extension.shared.utils.Utils.isSDKAbove;

import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import app.morphe.extension.music.settings.Settings;

@SuppressWarnings("unused")
public class AccountPatch {

    private static String[] accountMenuBlockList;

    static {
        accountMenuBlockList = Settings.HIDE_ACCOUNT_MENU_FILTER_STRINGS.get().split("\\n");
        // Some settings should not be hidden.
        if (isSDKAbove(24)) {
            accountMenuBlockList = Arrays.stream(accountMenuBlockList)
                    .filter(item -> !Objects.equals(item, str("settings")))
                    .toArray(String[]::new);
        } else {
            List<String> tmp = new ArrayList<>(Arrays.asList(accountMenuBlockList));
            tmp.remove(str("settings")); // "Settings" should appear only once in the account menu
            accountMenuBlockList = tmp.toArray(new String[0]);
        }
    }

    public static void hideAccountMenu(CharSequence charSequence, View view) {
        if (!Settings.HIDE_ACCOUNT_MENU.get())
            return;

        if (charSequence == null) {
            if (Settings.HIDE_ACCOUNT_MENU_EMPTY_COMPONENT.get())
                view.setVisibility(View.GONE);

            return;
        }

        for (String filter : accountMenuBlockList) {
            if (!filter.isEmpty() && charSequence.toString().equals(filter))
                view.setVisibility(View.GONE);
        }
    }

    public static boolean hideHandle(boolean original) {
        return Settings.HIDE_HANDLE.get() || original;
    }

    public static void hideHandle(TextView textView, int visibility) {
        final int finalVisibility = Settings.HIDE_HANDLE.get()
                ? View.GONE
                : visibility;
        textView.setVisibility(finalVisibility);
    }

    public static int hideTermsContainer() {
        return Settings.HIDE_TERMS_CONTAINER.get() ? View.GONE : View.VISIBLE;
    }
}

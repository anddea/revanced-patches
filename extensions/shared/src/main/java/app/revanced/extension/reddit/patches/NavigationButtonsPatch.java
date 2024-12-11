package app.revanced.extension.reddit.patches;

import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import app.revanced.extension.reddit.settings.Settings;
import app.revanced.extension.shared.utils.Logger;

@SuppressWarnings("unused")
public final class NavigationButtonsPatch {

    public static List<?> hideNavigationButtons(List<?> list) {
        try {
            for (NavigationButton button : NavigationButton.values()) {
                if (button.enabled && list.size() > button.index) {
                    list.remove(button.index);
                }
            }
        } catch (Exception exception) {
            Logger.printException(() -> "Failed to remove button list", exception);
        }
        return list;
    }

    public static void hideNavigationButtons(ViewGroup viewGroup) {
        try {
            if (viewGroup == null) return;
            for (NavigationButton button : NavigationButton.values()) {
                if (button.enabled && viewGroup.getChildCount() > button.index) {
                    View view = viewGroup.getChildAt(button.index);
                    if (view != null) view.setVisibility(View.GONE);
                }
            }
        } catch (Exception exception) {
            Logger.printException(() -> "Failed to remove button view", exception);
        }
    }

    private enum NavigationButton {
        CHAT(Settings.HIDE_CHAT_BUTTON.get(), 3),
        CREATE(Settings.HIDE_CREATE_BUTTON.get(), 2),
        DISCOVER(Settings.HIDE_DISCOVER_BUTTON.get(), 1);
        private final boolean enabled;
        private final int index;

        NavigationButton(final boolean enabled, final int index) {
            this.enabled = enabled;
            this.index = index;
        }
    }
}

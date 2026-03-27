package app.morphe.extension.reddit.patches;

import android.view.View;
import android.view.ViewGroup;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import app.morphe.extension.reddit.settings.Settings;
import app.morphe.extension.shared.utils.Logger;

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

    public static Object[] hideNavigationButtons(Object[] array) {
        try {
            for (NavigationButton button : NavigationButton.values()) {
                if (button.enabled && array.length > button.index) {
                    Object buttonObject = array[button.index];
                    array = Arrays.stream(array)
                            .filter(item -> !Objects.equals(item, buttonObject))
                            .toArray(Object[]::new);
                }
            }
        } catch (Exception exception) {
            Logger.printException(() -> "Failed to remove button array", exception);
        }
        return array;
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

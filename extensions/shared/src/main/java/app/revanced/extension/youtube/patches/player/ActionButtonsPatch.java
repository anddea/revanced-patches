package app.revanced.extension.youtube.patches.player;

import androidx.annotation.Nullable;

import java.util.List;

import app.revanced.extension.shared.settings.BooleanSetting;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.shared.VideoInformation;

@SuppressWarnings("unused")
public class ActionButtonsPatch {

    public enum ActionButton {
        INDEX_7(Settings.HIDE_ACTION_BUTTON_INDEX_7, Settings.HIDE_ACTION_BUTTON_INDEX_LIVE_7, 7),
        INDEX_6(Settings.HIDE_ACTION_BUTTON_INDEX_6, Settings.HIDE_ACTION_BUTTON_INDEX_LIVE_6, 6),
        INDEX_5(Settings.HIDE_ACTION_BUTTON_INDEX_5, Settings.HIDE_ACTION_BUTTON_INDEX_LIVE_5, 5),
        INDEX_4(Settings.HIDE_ACTION_BUTTON_INDEX_4, Settings.HIDE_ACTION_BUTTON_INDEX_LIVE_4, 4),
        INDEX_3(Settings.HIDE_ACTION_BUTTON_INDEX_3, Settings.HIDE_ACTION_BUTTON_INDEX_LIVE_3, 3),
        INDEX_2(Settings.HIDE_ACTION_BUTTON_INDEX_2, Settings.HIDE_ACTION_BUTTON_INDEX_LIVE_2, 2),
        INDEX_1(Settings.HIDE_ACTION_BUTTON_INDEX_1, Settings.HIDE_ACTION_BUTTON_INDEX_LIVE_1, 1),
        INDEX_0(Settings.HIDE_ACTION_BUTTON_INDEX_0, Settings.HIDE_ACTION_BUTTON_INDEX_LIVE_0, 0);

        private final BooleanSetting generalSetting;
        private final BooleanSetting liveSetting;
        private final int index;

        ActionButton(final BooleanSetting generalSetting, final BooleanSetting liveSetting, final int index) {
            this.generalSetting = generalSetting;
            this.liveSetting = liveSetting;
            this.index = index;
        }
    }

    private static final String TARGET_COMPONENT_TYPE = "LazilyConvertedElement";
    private static final String VIDEO_ACTION_BAR_PATH_PREFIX = "video_action_bar.eml";

    public static List<Object> hideActionButtonByIndex(@Nullable List<Object> list, @Nullable String identifier) {
        try {
            if (identifier != null &&
                    identifier.startsWith(VIDEO_ACTION_BAR_PATH_PREFIX) &&
                    list != null &&
                    !list.isEmpty() &&
                    list.get(0).toString().equals(TARGET_COMPONENT_TYPE)
            ) {
                final int size = list.size();
                final boolean isLive = VideoInformation.getLiveStreamState();
                for (ActionButton button : ActionButton.values()) {
                    if (size > button.index && (isLive ? button.liveSetting.get() : button.generalSetting.get())) {
                        list.remove(button.index);
                    }
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "hideActionButtonByIndex failure", ex);
        }

        return list;
    }

}

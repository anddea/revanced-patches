package app.revanced.extension.music.patches.actionbar;

import static app.revanced.extension.shared.utils.Utils.hideViewBy0dpUnderCondition;
import static app.revanced.extension.shared.utils.Utils.hideViewUnderCondition;

import android.view.View;

import androidx.annotation.NonNull;

import app.revanced.extension.music.settings.Settings;
import app.revanced.extension.music.utils.VideoUtils;

@SuppressWarnings("unused")
public class ActionBarPatch {

    @NonNull
    private static String buttonType = "";

    public static boolean hideActionBarLabel() {
        return Settings.HIDE_ACTION_BUTTON_LABEL.get();
    }

    public static boolean hideActionButton() {
        for (ActionButton actionButton : ActionButton.values())
            if (actionButton.enabled && actionButton.name.equals(buttonType))
                return true;

        return false;
    }

    public static void hideLikeDislikeButton(View view) {
        final boolean enabled = Settings.HIDE_ACTION_BUTTON_LIKE_DISLIKE.get();
        hideViewUnderCondition(
                enabled,
                view
        );
        hideViewBy0dpUnderCondition(
                enabled,
                view
        );
    }

    public static void inAppDownloadButtonOnClick(View view) {
        if (!Settings.EXTERNAL_DOWNLOADER_ACTION_BUTTON.get()) {
            return;
        }

        if (buttonType.equals(ActionButton.DOWNLOAD.name))
            view.setOnClickListener(imageView -> VideoUtils.launchExternalDownloader());
    }

    public static void setButtonType(@NonNull Object obj) {
        final String buttonType = obj.toString();

        for (ActionButton actionButton : ActionButton.values())
            if (buttonType.contains(actionButton.identifier))
                setButtonType(actionButton.name);
    }

    public static void setButtonType(@NonNull String newButtonType) {
        buttonType = newButtonType;
    }

    public static void setButtonTypeDownload(int type) {
        if (type != 0)
            return;

        setButtonType(ActionButton.DOWNLOAD.name);
    }

    private enum ActionButton {
        ADD_TO_PLAYLIST("ACTION_BUTTON_ADD_TO_PLAYLIST", "69487224", Settings.HIDE_ACTION_BUTTON_ADD_TO_PLAYLIST.get()),
        COMMENT_DISABLED("ACTION_BUTTON_COMMENT", "76623563", Settings.HIDE_ACTION_BUTTON_COMMENT.get()),
        COMMENT_ENABLED("ACTION_BUTTON_COMMENT", "138681778", Settings.HIDE_ACTION_BUTTON_COMMENT.get()),
        DOWNLOAD("ACTION_BUTTON_DOWNLOAD", "73080600", Settings.HIDE_ACTION_BUTTON_DOWNLOAD.get()),
        RADIO("ACTION_BUTTON_RADIO", "48687757", Settings.HIDE_ACTION_BUTTON_RADIO.get()),
        SHARE("ACTION_BUTTON_SHARE", "90650344", Settings.HIDE_ACTION_BUTTON_SHARE.get());

        private final String name;
        private final String identifier;
        private final boolean enabled;

        ActionButton(String name, String identifier, boolean enabled) {
            this.name = name;
            this.identifier = identifier;
            this.enabled = enabled;
        }
    }
}

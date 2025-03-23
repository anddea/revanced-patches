package app.revanced.extension.music.patches.actionbar;

import static app.revanced.extension.shared.utils.Utils.hideViewBy0dpUnderCondition;
import static app.revanced.extension.shared.utils.Utils.hideViewUnderCondition;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.litho.ComponentHost;

import java.util.Map;

import app.revanced.extension.music.settings.Settings;
import app.revanced.extension.music.utils.VideoUtils;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.PackageUtils;

@SuppressWarnings("unused")
public class ActionBarPatch {
    private static final boolean CHANGE_ACTION_BAR_POSITION =
            Settings.CHANGE_ACTION_BAR_POSITION.get();
    private static final boolean HIDE_ACTION_BUTTON_LABEL =
            Settings.HIDE_ACTION_BUTTON_LABEL.get();
    private static final boolean HIDE_ACTION_BUTTON_LIKE_DISLIKE =
            Settings.HIDE_ACTION_BUTTON_LIKE_DISLIKE.get() || PackageUtils.getAppVersionName().compareTo("7.25.00") >= 0;
    private static final boolean EXTERNAL_DOWNLOADER_ACTION_BUTTON =
            Settings.EXTERNAL_DOWNLOADER_ACTION_BUTTON.get();
    private static final boolean SETTINGS_INITIALIZED =
            Settings.SETTINGS_INITIALIZED.get();
    private static final String ELEMENTS_SENDER_VIEW =
            "com.google.android.libraries.youtube.rendering.elements.sender_view";
    private static final String EXTERNAL_DOWNLOADER_LAUNCHED =
            "external_downloader_launched";
    private static String downloadButtonLabel = "";

    @NonNull
    private static String buttonType = "";

    public static boolean changeActionBarPosition(boolean original) {
        return SETTINGS_INITIALIZED
                ? CHANGE_ACTION_BAR_POSITION
                : original;
    }

    public static boolean hideActionBarLabel() {
        return HIDE_ACTION_BUTTON_LABEL;
    }

    public static boolean hideActionButton() {
        for (ActionButton actionButton : ActionButton.values())
            if (actionButton.enabled && actionButton.name.equals(buttonType))
                return true;

        return false;
    }

    public static void hideLikeDislikeButton(View view) {
        hideViewUnderCondition(
                HIDE_ACTION_BUTTON_LIKE_DISLIKE,
                view
        );
        hideViewBy0dpUnderCondition(
                HIDE_ACTION_BUTTON_LIKE_DISLIKE,
                view
        );
    }

    public static void inAppDownloadButtonOnClick(View view) {
        if (EXTERNAL_DOWNLOADER_ACTION_BUTTON &&
                buttonType.equals(ActionButton.DOWNLOAD.name)) {
            view.setOnClickListener(imageView -> VideoUtils.launchExternalDownloader());
        }
    }

    public static boolean inAppDownloadButtonOnClick(@Nullable Map<Object, Object> map) {
        try {
            if (EXTERNAL_DOWNLOADER_ACTION_BUTTON &&
                    !downloadButtonLabel.isEmpty() &&
                    map != null &&
                    map.get(ELEMENTS_SENDER_VIEW) instanceof ComponentHost componentHost &&
                    downloadButtonLabel.equals(componentHost.getContentDescription() + "")
            ) {
                if (!map.containsKey(EXTERNAL_DOWNLOADER_LAUNCHED)) {
                    map.put(EXTERNAL_DOWNLOADER_LAUNCHED, Boolean.TRUE);
                    VideoUtils.runOnMainThreadDelayed(VideoUtils::launchExternalDownloader, 0);
                }
                return true;
            }
        } catch (Exception ex) {
            Logger.printException(() -> "inAppDownloadButtonOnClick failed", ex);
        }

        return false;
    }

    public static CharSequence onLithoTextLoaded(@NonNull Object conversionContext,
                                                 @NonNull CharSequence original) {
        if (EXTERNAL_DOWNLOADER_ACTION_BUTTON &&
                downloadButtonLabel.isEmpty() &&
                conversionContext.toString().contains("music_download_button.eml")) {
            downloadButtonLabel = original.toString();
            Logger.printDebug(() -> "set download button label: " + original);
        }

        return original;
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

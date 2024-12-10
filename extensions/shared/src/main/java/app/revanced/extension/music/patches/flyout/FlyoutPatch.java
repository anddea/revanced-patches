package app.revanced.extension.music.patches.flyout;

import static app.revanced.extension.shared.utils.ResourceUtils.getIdentifier;
import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.shared.utils.Utils.clickView;
import static app.revanced.extension.shared.utils.Utils.runOnMainThreadDelayed;

import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

import app.revanced.extension.music.settings.Settings;
import app.revanced.extension.music.shared.VideoType;
import app.revanced.extension.music.utils.VideoUtils;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.ResourceUtils.ResourceType;

@SuppressWarnings("unused")
public class FlyoutPatch {

    public static int enableCompactDialog(int original) {
        if (!Settings.ENABLE_COMPACT_DIALOG.get())
            return original;

        return Math.max(original, 600);
    }

    public static boolean enableTrimSilence(boolean original) {
        if (!Settings.ENABLE_TRIM_SILENCE.get())
            return original;

        return VideoType.getCurrent().isPodCast() || original;
    }

    public static boolean enableTrimSilenceSwitch(boolean original) {
        if (!Settings.ENABLE_TRIM_SILENCE.get())
            return original;

        return VideoType.getCurrent().isPodCast() && original;
    }

    public static boolean hideComponents(@Nullable Enum<?> flyoutMenuEnum) {
        if (flyoutMenuEnum == null)
            return false;

        final String flyoutMenuName = flyoutMenuEnum.name();

        Logger.printDebug(() -> "flyoutMenu: " + flyoutMenuName);

        for (FlyoutPanelComponent component : FlyoutPanelComponent.values())
            if (component.name.equals(flyoutMenuName) && component.enabled)
                return true;

        return false;
    }

    public static void hideLikeDislikeContainer(View view) {
        if (!Settings.HIDE_FLYOUT_MENU_LIKE_DISLIKE.get())
            return;

        if (view.getParent() instanceof ViewGroup viewGroup) {
            viewGroup.removeView(view);
        }
    }

    private static volatile boolean lastMenuWasDismissQueue = false;

    private static WeakReference<View> touchOutSideViewRef = new WeakReference<>(null);

    public static void setTouchOutSideView(View touchOutSideView) {
        touchOutSideViewRef = new WeakReference<>(touchOutSideView);
    }

    public static void replaceComponents(@Nullable Enum<?> flyoutPanelEnum, @NonNull TextView textView, @NonNull ImageView imageView) {
        if (flyoutPanelEnum == null)
            return;

        final String enumString = flyoutPanelEnum.name();
        final boolean isDismissQue = enumString.equals("DISMISS_QUEUE");
        final boolean isReport = enumString.equals("FLAG");

        if (isDismissQue) {
            replaceDismissQueue(textView, imageView);
        } else if (isReport) {
            replaceReport(textView, imageView, lastMenuWasDismissQueue);
        }
        lastMenuWasDismissQueue = isDismissQue;
    }

    private static void replaceDismissQueue(@NonNull TextView textView, @NonNull ImageView imageView) {
        if (!Settings.REPLACE_FLYOUT_MENU_DISMISS_QUEUE.get())
            return;

        if (!(textView.getParent() instanceof ViewGroup clickAbleArea))
            return;

        runOnMainThreadDelayed(() -> {
                    textView.setText(str("revanced_replace_flyout_menu_dismiss_queue_watch_on_youtube_label"));
                    imageView.setImageResource(getIdentifier("yt_outline_youtube_logo_icon_vd_theme_24", ResourceType.DRAWABLE, clickAbleArea.getContext()));
                    clickAbleArea.setOnClickListener(viewGroup -> VideoUtils.openInYouTube());
                }, 0L
        );
    }

    private static final ColorFilter cf = new PorterDuffColorFilter(Color.parseColor("#ffffffff"), PorterDuff.Mode.SRC_ATOP);

    private static void replaceReport(@NonNull TextView textView, @NonNull ImageView imageView, boolean wasDismissQueue) {
        if (!Settings.REPLACE_FLYOUT_MENU_REPORT.get())
            return;

        if (Settings.REPLACE_FLYOUT_MENU_REPORT_ONLY_PLAYER.get() && !wasDismissQueue)
            return;

        if (!(textView.getParent() instanceof ViewGroup clickAbleArea))
            return;

        runOnMainThreadDelayed(() -> {
                    textView.setText(str("playback_rate_title"));
                    imageView.setImageResource(getIdentifier("yt_outline_play_arrow_half_circle_black_24", ResourceType.DRAWABLE, clickAbleArea.getContext()));
                    imageView.setColorFilter(cf);
                    clickAbleArea.setOnClickListener(view -> {
                        clickView(touchOutSideViewRef.get());
                        VideoUtils.showPlaybackSpeedFlyoutMenu();
                    });
                }, 0L
        );
    }

    private enum FlyoutPanelComponent {
        SAVE_EPISODE_FOR_LATER("BOOKMARK_BORDER", Settings.HIDE_FLYOUT_MENU_SAVE_EPISODE_FOR_LATER.get()),
        SHUFFLE_PLAY("SHUFFLE", Settings.HIDE_FLYOUT_MENU_SHUFFLE_PLAY.get()),
        RADIO("MIX", Settings.HIDE_FLYOUT_MENU_START_RADIO.get()),
        SUBSCRIBE("SUBSCRIBE", Settings.HIDE_FLYOUT_MENU_SUBSCRIBE.get()),
        EDIT_PLAYLIST("EDIT", Settings.HIDE_FLYOUT_MENU_EDIT_PLAYLIST.get()),
        DELETE_PLAYLIST("DELETE", Settings.HIDE_FLYOUT_MENU_DELETE_PLAYLIST.get()),
        PLAY_NEXT("QUEUE_PLAY_NEXT", Settings.HIDE_FLYOUT_MENU_PLAY_NEXT.get()),
        ADD_TO_QUEUE("QUEUE_MUSIC", Settings.HIDE_FLYOUT_MENU_ADD_TO_QUEUE.get()),
        SAVE_TO_LIBRARY("LIBRARY_ADD", Settings.HIDE_FLYOUT_MENU_SAVE_TO_LIBRARY.get()),
        REMOVE_FROM_LIBRARY("LIBRARY_REMOVE", Settings.HIDE_FLYOUT_MENU_REMOVE_FROM_LIBRARY.get()),
        REMOVE_FROM_PLAYLIST("REMOVE_FROM_PLAYLIST", Settings.HIDE_FLYOUT_MENU_REMOVE_FROM_PLAYLIST.get()),
        DOWNLOAD("OFFLINE_DOWNLOAD", Settings.HIDE_FLYOUT_MENU_DOWNLOAD.get()),
        SAVE_TO_PLAYLIST("ADD_TO_PLAYLIST", Settings.HIDE_FLYOUT_MENU_SAVE_TO_PLAYLIST.get()),
        GO_TO_EPISODE("INFO", Settings.HIDE_FLYOUT_MENU_GO_TO_EPISODE.get()),
        GO_TO_PODCAST("BROADCAST", Settings.HIDE_FLYOUT_MENU_GO_TO_PODCAST.get()),
        GO_TO_ALBUM("ALBUM", Settings.HIDE_FLYOUT_MENU_GO_TO_ALBUM.get()),
        GO_TO_ARTIST("ARTIST", Settings.HIDE_FLYOUT_MENU_GO_TO_ARTIST.get()),
        VIEW_SONG_CREDIT("PEOPLE_GROUP", Settings.HIDE_FLYOUT_MENU_VIEW_SONG_CREDIT.get()),
        SHARE("SHARE", Settings.HIDE_FLYOUT_MENU_SHARE.get()),
        DISMISS_QUEUE("DISMISS_QUEUE", Settings.HIDE_FLYOUT_MENU_DISMISS_QUEUE.get()),
        HELP("HELP_OUTLINE", Settings.HIDE_FLYOUT_MENU_HELP.get()),
        REPORT("FLAG", Settings.HIDE_FLYOUT_MENU_REPORT.get()),
        QUALITY("SETTINGS_MATERIAL", Settings.HIDE_FLYOUT_MENU_QUALITY.get()),
        CAPTIONS("CAPTIONS", Settings.HIDE_FLYOUT_MENU_CAPTIONS.get()),
        STATS_FOR_NERDS("PLANNER_REVIEW", Settings.HIDE_FLYOUT_MENU_STATS_FOR_NERDS.get()),
        SLEEP_TIMER("MOON_Z", Settings.HIDE_FLYOUT_MENU_SLEEP_TIMER.get());

        private final boolean enabled;
        private final String name;

        FlyoutPanelComponent(String name, boolean enabled) {
            this.enabled = enabled;
            this.name = name;
        }
    }
}

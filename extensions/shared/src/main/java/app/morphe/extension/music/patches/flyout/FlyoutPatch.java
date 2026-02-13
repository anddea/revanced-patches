package app.morphe.extension.music.patches.flyout;

import static app.morphe.extension.shared.utils.ResourceUtils.getIdentifier;
import static app.morphe.extension.shared.utils.StringRef.str;
import static app.morphe.extension.shared.utils.Utils.clickView;
import static app.morphe.extension.shared.utils.Utils.runOnMainThreadDelayed;

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

import app.morphe.extension.music.settings.Settings;
import app.morphe.extension.music.shared.VideoType;
import app.morphe.extension.music.utils.VideoUtils;
import app.morphe.extension.shared.settings.BooleanSetting;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.ResourceUtils.ResourceType;

@SuppressWarnings("unused")
public class FlyoutPatch {
    private static final BooleanSetting DISABLE_TRIM_SILENCE =
            Settings.DISABLE_TRIM_SILENCE;
    private static final BooleanSetting ENABLE_COMPACT_DIALOG =
            Settings.ENABLE_COMPACT_DIALOG;
    private static final BooleanSetting REPLACE_FLYOUT_MENU_DISMISS_QUEUE =
            Settings.REPLACE_FLYOUT_MENU_DISMISS_QUEUE;
    private static final BooleanSetting REPLACE_FLYOUT_MENU_REPORT =
            Settings.REPLACE_FLYOUT_MENU_REPORT;
    private static final BooleanSetting REPLACE_FLYOUT_MENU_REPORT_ONLY_PLAYER =
            Settings.REPLACE_FLYOUT_MENU_REPORT_ONLY_PLAYER;
    private static final boolean HIDE_FLYOUT_MENU_LIKE_DISLIKE =
            Settings.HIDE_FLYOUT_MENU_LIKE_DISLIKE.get();
    private static volatile boolean lastMenuWasDismissQueue = false;
    private static WeakReference<View> touchOutSideViewRef = new WeakReference<>(null);
    private static final ColorFilter cf = new PorterDuffColorFilter(Color.parseColor("#ffffffff"), PorterDuff.Mode.SRC_ATOP);

    public static boolean disableTrimSilence(boolean original) {
        return VideoType.getCurrent().isPodCast() && !DISABLE_TRIM_SILENCE.get();
    }

    public static boolean disableTrimSilenceSwitch(boolean original) {
        return VideoType.getCurrent().isPodCast() && !DISABLE_TRIM_SILENCE.get();
    }

    public static int enableCompactDialog(int original) {
        return ENABLE_COMPACT_DIALOG.get()
                ? Math.max(original, 600)
                : original;
    }

    public static boolean hideComponents(@Nullable Enum<?> flyoutMenuEnum) {
        if (flyoutMenuEnum != null) {
            final String flyoutMenuName = flyoutMenuEnum.name();
            Logger.printDebug(() -> "flyoutMenu loaded: " + flyoutMenuName);

            for (FlyoutPanelComponent component : FlyoutPanelComponent.values())
                if (component.name().equals(flyoutMenuName) && component.setting.get())
                    return true;
        }

        return false;
    }

    public static void hideLikeDislikeContainer(View view) {
        if (HIDE_FLYOUT_MENU_LIKE_DISLIKE &&
                view.getParent() instanceof ViewGroup viewGroup) {
            viewGroup.removeView(view);
        }
    }

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
        if (REPLACE_FLYOUT_MENU_DISMISS_QUEUE.get() &&
                textView.getParent() instanceof ViewGroup clickAbleArea) {
            runOnMainThreadDelayed(() -> {
                        textView.setText(str("revanced_replace_flyout_menu_dismiss_queue_watch_on_youtube_label"));
                        imageView.setImageResource(getIdentifier("yt_outline_youtube_logo_icon_vd_theme_24", ResourceType.DRAWABLE, clickAbleArea.getContext()));
                        clickAbleArea.setOnClickListener(view -> {
                            clickView(touchOutSideViewRef.get());
                            VideoUtils.openInYouTube();
                        });
                    }, 0L
            );
        }
    }

    private static void replaceReport(@NonNull TextView textView, @NonNull ImageView imageView,
                                      boolean wasDismissQueue) {
        if (REPLACE_FLYOUT_MENU_REPORT.get() &&
                (!REPLACE_FLYOUT_MENU_REPORT_ONLY_PLAYER.get() || wasDismissQueue) &&
                textView.getParent() instanceof ViewGroup clickAbleArea
        ) {
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
    }

    private enum FlyoutPanelComponent {
        ADD_TO_PLAYLIST(Settings.HIDE_FLYOUT_MENU_SAVE_TO_PLAYLIST),
        ALBUM(Settings.HIDE_FLYOUT_MENU_GO_TO_ALBUM),
        ARTIST(Settings.HIDE_FLYOUT_MENU_GO_TO_ARTIST),
        BOOKMARK_BORDER(Settings.HIDE_FLYOUT_MENU_SAVE_EPISODE_FOR_LATER_SAVE_TO_LIBRARY),
        BROADCAST(Settings.HIDE_FLYOUT_MENU_GO_TO_PODCAST),
        CAPTIONS(Settings.HIDE_FLYOUT_MENU_CAPTIONS),
        DELETE(Settings.HIDE_FLYOUT_MENU_DELETE_PLAYLIST),
        DISMISS_QUEUE(Settings.HIDE_FLYOUT_MENU_DISMISS_QUEUE),
        EDIT(Settings.HIDE_FLYOUT_MENU_EDIT_PLAYLIST),
        FLAG(Settings.HIDE_FLYOUT_MENU_REPORT),
        HELP_OUTLINE(Settings.HIDE_FLYOUT_MENU_HELP),
        HIDE(Settings.HIDE_FLYOUT_MENU_NOT_INTERESTED),
        INFO(Settings.HIDE_FLYOUT_MENU_GO_TO_EPISODE),
        KEEP(Settings.HIDE_FLYOUT_MENU_PIN_TO_SPEED_DIAL),
        KEEP_OFF(Settings.HIDE_FLYOUT_MENU_UNPIN_FROM_SPEED_DIAL),
        LIBRARY_ADD(Settings.HIDE_FLYOUT_MENU_SAVE_EPISODE_FOR_LATER_SAVE_TO_LIBRARY),
        LIBRARY_REMOVE(Settings.HIDE_FLYOUT_MENU_REMOVE_FROM_LIBRARY),
        MIX(Settings.HIDE_FLYOUT_MENU_START_RADIO),
        MOON_Z(Settings.HIDE_FLYOUT_MENU_SLEEP_TIMER),
        OFFLINE_DOWNLOAD(Settings.HIDE_FLYOUT_MENU_DOWNLOAD),
        PEOPLE_GROUP(Settings.HIDE_FLYOUT_MENU_VIEW_SONG_CREDIT),
        PLANNER_REVIEW(Settings.HIDE_FLYOUT_MENU_STATS_FOR_NERDS),
        QUEUE_MUSIC(Settings.HIDE_FLYOUT_MENU_ADD_TO_QUEUE),
        QUEUE_PLAY_NEXT(Settings.HIDE_FLYOUT_MENU_PLAY_NEXT),
        REMOVE_FROM_PLAYLIST(Settings.HIDE_FLYOUT_MENU_REMOVE_FROM_PLAYLIST),
        SETTINGS_MATERIAL(Settings.HIDE_FLYOUT_MENU_QUALITY),
        SHARE(Settings.HIDE_FLYOUT_MENU_SHARE),
        SHUFFLE(Settings.HIDE_FLYOUT_MENU_SHUFFLE_PLAY),
        SUBSCRIBE(Settings.HIDE_FLYOUT_MENU_SUBSCRIBE);

        private final BooleanSetting setting;

        FlyoutPanelComponent(BooleanSetting setting) {
            this.setting = setting;
        }
    }
}

/*
 * Copyright (C) 2026 lavinhoque33
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Licensed under the GNU General Public License v3.0.
 * Written by lavinhoque33, 2026-10-10.
 */

package app.morphe.extension.youtube.patches.general.offline;

import static app.morphe.extension.shared.utils.StringRef.str;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

/**
 * Video screen of the offline player. The player itself is {@link OfflinePlayer}; this screen
 * only shows its video and controls, so playback continues when the screen is left.
 */
@SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
final class OfflinePlayerScreen implements OfflineScreens.Screen, OfflinePlayer.Listener {
    private static final long CONTROLS_TIMEOUT_MS = 3000;
    private static final long PROGRESS_INTERVAL_MS = 500;
    private static final float[] SPEEDS = {0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f};

    private final Activity activity;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final FrameLayout root;
    private final SurfaceView surfaceView;
    private final View controls;
    private final TextView title;
    private final ImageButton playPause;
    private final ImageButton previous;
    private final ImageButton next;
    private final ImageButton speedButton;
    private final ImageButton fullscreenButton;
    private final SeekBar seekBar;
    private final TextView position;
    private final TextView duration;
    private final TextView seekHint;

    private boolean controlsVisible = true;
    private boolean userSeeking;
    private boolean resumed;
    private boolean destroyed;

    private final Runnable hideControls = () -> setControlsVisible(false);
    private final Runnable progressTicker = new Runnable() {
        @Override
        public void run() {
            updateProgress();
            if (resumed && !destroyed) handler.postDelayed(this, PROGRESS_INTERVAL_MS);
        }
    };

    OfflinePlayerScreen(Activity activity) {
        this.activity = activity;
        Window window = activity.getWindow();
        activity.setTheme(Utils.getResourceIdentifierOrThrow("Theme.YouTube.Settings.Dark", "style"));
        window.setStatusBarColor(Color.BLACK);
        window.setNavigationBarColor(Color.BLACK);

        root = new FrameLayout(activity);
        root.setBackgroundColor(Color.BLACK);

        surfaceView = new SurfaceView(activity);
        root.addView(surfaceView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                OfflinePlayer.attachSurface(holder.getSurface());
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                // Only the video stops; the audio continues in the background.
                OfflinePlayer.detachSurface(holder.getSurface());
            }
        });
        root.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (r - l != or - ol || b - t != ob - ot) fitVideo();
        });

        seekHint = text(18, Color.WHITE);
        GradientDrawable hintBackground = new GradientDrawable();
        hintBackground.setCornerRadius(Utils.dipToPixels(24));
        hintBackground.setColor(0x99000000);
        seekHint.setBackground(hintBackground);
        final int hintPadding = Utils.dipToPixels(12);
        seekHint.setPadding(hintPadding * 2, hintPadding, hintPadding * 2, hintPadding);
        seekHint.setVisibility(View.GONE);
        root.addView(seekHint, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        // region controls
        FrameLayout overlay = new FrameLayout(activity);
        overlay.setBackgroundColor(0x66000000);
        overlay.setFitsSystemWindows(true);
        controls = overlay;

        LinearLayout top = new LinearLayout(activity);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        ImageButton back = button(android.R.drawable.ic_menu_revert, str("revanced_settings_navigate_up"));
        back.setOnClickListener(v -> activity.finish());
        top.addView(back);
        title = text(16, Color.WHITE);
        title.setMaxLines(1);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        top.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        speedButton = button(android.R.drawable.ic_menu_manage, str("revanced_offline_player_speed"));
        speedButton.setOnClickListener(this::showSpeedMenu);
        top.addView(speedButton);
        overlay.addView(top, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP));

        LinearLayout center = new LinearLayout(activity);
        center.setOrientation(LinearLayout.HORIZONTAL);
        center.setGravity(Gravity.CENTER);
        previous = button(android.R.drawable.ic_media_previous, str("revanced_offline_previous"));
        previous.setOnClickListener(v -> {
            OfflinePlayer.skip(-1);
            scheduleHide();
        });
        playPause = button(android.R.drawable.ic_media_pause, str("revanced_offline_pause"));
        playPause.setScaleX(1.5f);
        playPause.setScaleY(1.5f);
        playPause.setOnClickListener(v -> {
            OfflinePlayer.togglePlayPause();
            scheduleHide();
        });
        next = button(android.R.drawable.ic_media_next, str("revanced_offline_next"));
        next.setOnClickListener(v -> {
            OfflinePlayer.skip(1);
            scheduleHide();
        });
        final int gap = Utils.dipToPixels(32);
        LinearLayout.LayoutParams centerParams = new LinearLayout.LayoutParams(Utils.dipToPixels(56), Utils.dipToPixels(56));
        centerParams.setMargins(gap, 0, gap, 0);
        center.addView(previous, new LinearLayout.LayoutParams(Utils.dipToPixels(56), Utils.dipToPixels(56)));
        center.addView(playPause, centerParams);
        center.addView(next, new LinearLayout.LayoutParams(Utils.dipToPixels(56), Utils.dipToPixels(56)));
        overlay.addView(center, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        LinearLayout bottom = new LinearLayout(activity);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        final int side = Utils.dipToPixels(12);
        bottom.setPadding(side, 0, 0, Utils.dipToPixels(4));
        position = text(13, Color.WHITE);
        bottom.addView(position);
        seekBar = new SeekBar(activity);
        seekBar.setMax(1000);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (fromUser) {
                    position.setText(OfflineLibraryScreen.formatDuration(progress * OfflinePlayer.getDuration() / 1000));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
                userSeeking = true;
                handler.removeCallbacks(hideControls);
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
                userSeeking = false;
                OfflinePlayer.seekTo(bar.getProgress() * OfflinePlayer.getDuration() / 1000);
                scheduleHide();
            }
        });
        bottom.addView(seekBar, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        duration = text(13, Color.WHITE);
        bottom.addView(duration);
        fullscreenButton = button(android.R.drawable.ic_menu_always_landscape_portrait,
                str("revanced_offline_player_fullscreen"));
        fullscreenButton.setOnClickListener(v -> toggleFullscreen());
        bottom.addView(fullscreenButton);
        overlay.addView(bottom, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
        root.addView(overlay, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        // endregion

        GestureDetector gestures = new GestureDetector(activity, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                setControlsVisible(!controlsVisible);
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                final boolean forward = e.getX() > root.getWidth() / 2f;
                if (forward) OfflinePlayer.seekForward();
                else OfflinePlayer.seekBackward();
                showSeekHint(forward ? "+10s" : "−10s");
                return true;
            }
        });
        // The overlay sits on top, so it receives the touches that miss its buttons.
        overlay.setOnTouchListener((v, event) -> gestures.onTouchEvent(event));

        activity.setContentView(root);
        applyFullscreen(isLandscape());

        if (!openRequestedVideo()) {
            activity.finish();
            return;
        }
        OfflinePlayer.addListener(this);
        onPlayerStateChanged();
        scheduleHide();
    }

    /** @return False if there is nothing to show. */
    private boolean openRequestedVideo() {
        String videoId = activity.getIntent().getStringExtra(OfflineScreens.EXTRA_VIDEO_ID);
        if (videoId == null) {
            // From the playback notification: show whatever plays.
            if (OfflinePlayer.isSessionActive()) return true;
            Utils.showToastShort(str("revanced_offline_nothing_playing"));
            return false;
        }
        OfflineVideo video = OfflineIndex.get(videoId);
        if (video == null) {
            Utils.showToastShort(str("revanced_offline_video_missing"));
            return false;
        }
        OfflinePlayer.play(video, OfflineScreens.order());
        return true;
    }

    // region views

    private TextView text(float sizeSp, int color) {
        TextView view = new TextView(activity);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        view.setTextColor(color);
        final int padding = Utils.dipToPixels(8);
        view.setPadding(padding, 0, padding, 0);
        return view;
    }

    private ImageButton button(int icon, String description) {
        ImageButton button = new ImageButton(activity);
        setIcon(button, icon);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setContentDescription(description);
        final int padding = Utils.dipToPixels(12);
        button.setPadding(padding, padding, padding, padding);
        button.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        return button;
    }

    private void setIcon(ImageButton button, int icon) {
        Drawable drawable = activity.getDrawable(icon);
        if (drawable != null) {
            drawable = drawable.mutate();
            drawable.setTint(Color.WHITE);
        }
        button.setImageDrawable(drawable);
    }

    private void showSeekHint(String text) {
        seekHint.setText(text);
        seekHint.setVisibility(View.VISIBLE);
        handler.removeCallbacks(hideSeekHint);
        handler.postDelayed(hideSeekHint, 700);
    }

    private final Runnable hideSeekHint = this::hideSeekHint;

    private void hideSeekHint() {
        seekHint.setVisibility(View.GONE);
    }

    private void setControlsVisible(boolean visible) {
        controlsVisible = visible;
        // The overlay itself stays visible: it has to receive the taps that show the controls again.
        ViewGroup group = (ViewGroup) controls;
        for (int i = 0; i < group.getChildCount(); i++) {
            group.getChildAt(i).setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
        controls.setBackgroundColor(visible ? 0x66000000 : Color.TRANSPARENT);
        if (visible) scheduleHide();
    }

    private void scheduleHide() {
        handler.removeCallbacks(hideControls);
        if (OfflinePlayer.isPlaying() && !userSeeking) {
            handler.postDelayed(hideControls, CONTROLS_TIMEOUT_MS);
        }
    }

    private void showSpeedMenu(View anchor) {
        PopupMenu menu = new PopupMenu(activity, anchor);
        final float current = OfflinePlayer.getSpeed();
        for (int i = 0; i < SPEEDS.length; i++) {
            final float speed = SPEEDS[i];
            String label = (speed == 1f ? str("revanced_offline_player_speed_normal")
                    : String.format(Locale.US, "%sx", trim(speed)));
            menu.getMenu().add(0, i, i, label).setCheckable(true).setChecked(speed == current);
        }
        menu.getMenu().setGroupCheckable(0, true, true);
        menu.setOnMenuItemClickListener(item -> {
            OfflinePlayer.setSpeed(SPEEDS[item.getItemId()]);
            return true;
        });
        menu.show();
        handler.removeCallbacks(hideControls);
    }

    private static String trim(float value) {
        String text = String.valueOf(value);
        return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
    }

    private void fitVideo() {
        final int videoWidth = OfflinePlayer.getVideoWidth();
        final int videoHeight = OfflinePlayer.getVideoHeight();
        final int width = root.getWidth();
        final int height = root.getHeight();
        if (videoWidth <= 0 || videoHeight <= 0 || width <= 0 || height <= 0) return;
        float scale = Math.min((float) width / videoWidth, (float) height / videoHeight);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) surfaceView.getLayoutParams();
        int newWidth = Math.round(videoWidth * scale);
        int newHeight = Math.round(videoHeight * scale);
        if (params.width != newWidth || params.height != newHeight) {
            params.width = newWidth;
            params.height = newHeight;
            params.gravity = Gravity.CENTER;
            surfaceView.setLayoutParams(params);
        }
    }

    private void updateProgress() {
        final long total = OfflinePlayer.getDuration();
        final long current = OfflinePlayer.getPosition();
        duration.setText(OfflineLibraryScreen.formatDuration(total));
        if (!userSeeking) {
            position.setText(OfflineLibraryScreen.formatDuration(current));
            seekBar.setProgress(total > 0 ? (int) (current * 1000 / total) : 0);
        }
    }

    // endregion

    // region fullscreen

    private boolean isLandscape() {
        return activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    private void toggleFullscreen() {
        // Rotating recreates the activity (LicenseActivity handles no configuration changes);
        // the new screen re-attaches its surface to the running player.
        final boolean toLandscape = !isLandscape();
        activity.setRequestedOrientation(toLandscape
                ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        applyFullscreen(toLandscape);
    }

    @SuppressWarnings("deprecation")
    private void applyFullscreen(boolean fullscreen) {
        Window window = activity.getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(!fullscreen);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                if (fullscreen) {
                    controller.hide(WindowInsets.Type.systemBars());
                    controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                } else {
                    controller.show(WindowInsets.Type.systemBars());
                }
            }
        } else {
            window.getDecorView().setSystemUiVisibility(fullscreen
                    ? View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    : View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    // endregion

    // region state

    @Override
    public void onPlayerStateChanged() {
        if (destroyed) return;
        OfflineVideo video = OfflinePlayer.getCurrent();
        if (video == null) {
            // The session was closed, for example from the notification.
            activity.finish();
            return;
        }
        title.setText(video.title);
        final boolean playing = OfflinePlayer.isPlaying();
        setIcon(playPause, playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
        playPause.setContentDescription(playing ? str("revanced_offline_pause") : str("revanced_offline_play"));
        previous.setAlpha(OfflinePlayer.hasPrevious() || OfflinePlayer.getPosition() > 3000 ? 1f : 0.4f);
        next.setEnabled(OfflinePlayer.hasNext());
        next.setAlpha(OfflinePlayer.hasNext() ? 1f : 0.4f);
        if (playing) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if (controlsVisible) scheduleHide();
        } else {
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            handler.removeCallbacks(hideControls);
            if (!controlsVisible) setControlsVisible(true);
        }
        fitVideo();
        updateProgress();
    }

    @Override
    public boolean onBackPressed() {
        if (!destroyed && OfflinePlayer.getCurrent() != null && isLandscape()
                && activity.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
            toggleFullscreen();
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        resumed = true;
        handler.removeCallbacks(progressTicker);
        handler.post(progressTicker);
        applyFullscreen(isLandscape());
    }

    @Override
    public void onPause() {
        resumed = false;
        handler.removeCallbacks(progressTicker);
    }

    @Override
    public void onDestroy() {
        destroyed = true;
        handler.removeCallbacksAndMessages(null);
        OfflinePlayer.removeListener(this);
        OfflinePlayer.detachSurface(surfaceView.getHolder().getSurface());
        Logger.printDebug(() -> "Offline player screen destroyed, playback continues: " + OfflinePlayer.isPlaying());
    }

    // endregion
}

package app.revanced.extension.youtube.patches.video;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.support.v7.widget.RecyclerView;
import android.view.*;
import android.view.animation.Animation;
import android.widget.*;
import androidx.annotation.NonNull;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.patches.components.PlaybackSpeedMenuFilter;
import app.revanced.extension.youtube.patches.player.SeekbarColorPatch;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.shared.PlayerType;
import app.revanced.extension.youtube.shared.VideoInformation;
import app.revanced.extension.youtube.utils.ThemeUtils;
import app.revanced.extension.youtube.utils.VideoUtils;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import java.lang.ref.WeakReference;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.function.Function;

import static app.revanced.extension.shared.utils.ResourceUtils.getString;
import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.shared.utils.Utils.dpToPx;

@SuppressWarnings({"deprecation", "unused"})
public class CustomPlaybackSpeedPatch {
    /**
     * Maximum playback speed, exclusive value.  Custom speeds must be less than this value.
     * <p>
     * Going over 8x does not increase the actual playback speed any higher,
     * and the UI selector starts flickering and acting weird.
     * Over 10x and the speeds show up out of order in the UI selector.
     */
    public static final float PLAYBACK_SPEED_MAXIMUM = 8;
    /**
     * Custom playback speeds.
     */
    public static final float[] customPlaybackSpeeds;
    private static final float PLAYBACK_SPEED_AUTO = Settings.DEFAULT_PLAYBACK_SPEED.defaultValue;
    private static final String[] defaultSpeedEntries;
    private static final String[] defaultSpeedEntryValues;
    /**
     * Scale used to convert user speed to {@link android.widget.ProgressBar#setProgress(int)}.
     */
    private static final float PROGRESS_BAR_VALUE_SCALE = 100;
    /**
     * Formats speeds to UI strings.
     */
    private static final NumberFormat speedFormatter = NumberFormat.getNumberInstance();
    /**
     * Minimum and maximum custom playback speeds of {@link #customPlaybackSpeeds}.
     */
    private static final float customPlaybackSpeedsMin, customPlaybackSpeedsMax;
    /**
     * Custom playback speeds.
     */
    private static float[] playbackSpeeds;
    private static String[] customSpeedEntries;
    private static String[] customSpeedEntryValues;
    private static String[] playbackSpeedEntries;
    private static String[] playbackSpeedEntryValues;
    /**
     * The last time the old playback menu was forcefully called.
     */
    private static long lastTimeOldPlaybackMenuInvoked;
    /**
     * Weak reference to the currently open dialog.
     */
    private static WeakReference<Dialog> currentDialog = new WeakReference<>(null);

    static {
        defaultSpeedEntries = new String[]{getString("quality_auto"), "0.25x", "0.5x", "0.75x", getString("revanced_playback_speed_normal"), "1.25x", "1.5x", "1.75x", "2.0x"};
        defaultSpeedEntryValues = new String[]{"-2.0", "0.25", "0.5", "0.75", "1.0", "1.25", "1.5", "1.75", "2.0"};

        // Cap at 2 decimals (rounds automatically).
        speedFormatter.setMaximumFractionDigits(2);

        customPlaybackSpeeds = loadCustomSpeeds();
        customPlaybackSpeedsMin = customPlaybackSpeeds[0];
        customPlaybackSpeedsMax = customPlaybackSpeeds[customPlaybackSpeeds.length - 1];
    }

    /**
     * Injection point.
     */
    public static float[] getArray(float[] original) {
        return isCustomPlaybackSpeedEnabled() ? playbackSpeeds : original;
    }

    /**
     * Injection point.
     */
    public static int getLength(int original) {
        return isCustomPlaybackSpeedEnabled() ? playbackSpeeds.length : original;
    }

    /**
     * Injection point.
     */
    public static int getSize(int original) {
        return isCustomPlaybackSpeedEnabled() ? 0 : original;
    }

    public static String[] getEntries() {
        return isCustomPlaybackSpeedEnabled()
                ? customSpeedEntries
                : defaultSpeedEntries;
    }

    public static String[] getEntryValues() {
        return isCustomPlaybackSpeedEnabled()
                ? customSpeedEntryValues
                : defaultSpeedEntryValues;
    }

    public static String[] getTrimmedEntries() {
        if (playbackSpeedEntries == null) {
            final String[] playbackSpeedWithAutoEntries = getEntries();
            playbackSpeedEntries = Arrays.copyOfRange(playbackSpeedWithAutoEntries, 1, playbackSpeedWithAutoEntries.length);
        }

        return playbackSpeedEntries;
    }

    public static String[] getTrimmedEntryValues() {
        if (playbackSpeedEntryValues == null) {
            final String[] playbackSpeedWithAutoEntryValues = getEntryValues();
            playbackSpeedEntryValues = Arrays.copyOfRange(playbackSpeedWithAutoEntryValues, 1, playbackSpeedWithAutoEntryValues.length);
        }

        return playbackSpeedEntryValues;
    }

    private static void resetCustomSpeeds(@NonNull String toastMessage) {
        Utils.showToastLong(toastMessage);
        Utils.showToastShort(str("revanced_extended_reset_to_default_toast"));
        Settings.CUSTOM_PLAYBACK_SPEEDS.resetToDefault();
    }

    private static void showInvalidCustomSpeedToast() {
        Utils.showToastLong(str("revanced_custom_playback_speeds_invalid", PLAYBACK_SPEED_MAXIMUM));
    }

    private static float[] loadCustomSpeeds() {
        try {
            // Automatically replace commas with periods,
            // if the user added speeds in a localized format.
            String[] speedStrings = Settings.CUSTOM_PLAYBACK_SPEEDS.get()
                    .replace(',', '.').split("\\s+");
            Arrays.sort(speedStrings);
            if (speedStrings.length == 0) {
                throw new IllegalArgumentException();
            }

            float[] speeds = new float[speedStrings.length];

            int i = 0;
            for (String speedString : speedStrings) {
                final float speedFloat = Float.parseFloat(speedString);
                if (speedFloat <= 0 || arrayContains(speeds, speedFloat)) {
                    throw new IllegalArgumentException();
                }

                if (speedFloat > PLAYBACK_SPEED_MAXIMUM) {
                    showInvalidCustomSpeedToast();
                    Settings.CUSTOM_PLAYBACK_SPEEDS.resetToDefault();
                    return loadCustomSpeeds();
                }

                speeds[i++] = speedFloat;
            }

            // Initialize customSpeedEntries and customSpeedEntryValues
            customSpeedEntries = new String[speeds.length + 1];
            customSpeedEntryValues = new String[speeds.length + 1];
            customSpeedEntries[0] = getString("quality_auto");
            customSpeedEntryValues[0] = "-2.0";

            for (i = 0; i < speeds.length; i++) {
                String speedString = String.valueOf(speeds[i]);
                customSpeedEntries[i + 1] = speeds[i] != 1.0f
                        ? speedString + "x"
                        : getString("revanced_playback_speed_normal");
                customSpeedEntryValues[i + 1] = speedString;
            }

            playbackSpeeds = speeds;
            return speeds;
        } catch (Exception ex) {
            Logger.printInfo(() -> "Parse error", ex);
            Utils.showToastShort(str("revanced_custom_playback_speeds_parse_exception"));
            Settings.CUSTOM_PLAYBACK_SPEEDS.resetToDefault();
            return loadCustomSpeeds();
        }
    }

    private static boolean arrayContains(float[] array, float value) {
        for (float arrayValue : array) {
            if (arrayValue == value) return true;
        }
        return false;
    }

    private static boolean isCustomPlaybackSpeedEnabled() {
        return Settings.ENABLE_CUSTOM_PLAYBACK_SPEED.get() && playbackSpeeds != null;
    }

    /**
     * Injection point.
     */
    public static void onFlyoutMenuCreate(RecyclerView recyclerView) {
        if (!Settings.ENABLE_CUSTOM_PLAYBACK_SPEED.get()) {
            return;
        }

        recyclerView.getViewTreeObserver().addOnDrawListener(() -> {
            try {
                if (PlaybackSpeedMenuFilter.isOldPlaybackSpeedMenuVisible) {
                    if (hideLithoMenuAndShowOldSpeedMenu(recyclerView, 8)) {
                        PlaybackSpeedMenuFilter.isOldPlaybackSpeedMenuVisible = false;
                    }
                    return;
                }
            } catch (Exception ex) {
                Logger.printException(() -> "isOldPlaybackSpeedMenuVisible failure", ex);
            }

            try {
                if (PlaybackSpeedMenuFilter.isPlaybackRateSelectorMenuVisible) {
                    if (hideLithoMenuAndShowOldSpeedMenu(recyclerView, 5)) {
                        PlaybackSpeedMenuFilter.isPlaybackRateSelectorMenuVisible = false;
                    }
                }
            } catch (Exception ex) {
                Logger.printException(() -> "isPlaybackRateSelectorMenuVisible failure", ex);
            }
        });
    }

    private static boolean hideLithoMenuAndShowOldSpeedMenu(RecyclerView recyclerView, int expectedChildCount) {
        if (recyclerView.getChildCount() == 0) {
            return false;
        }

        if (!(recyclerView.getChildAt(0) instanceof ViewGroup PlaybackSpeedParentView)) {
            return false;
        }

        if (PlaybackSpeedParentView.getChildCount() != expectedChildCount) {
            return false;
        }

        if (!(Utils.getParentView(recyclerView, 3) instanceof ViewGroup parentView3rd)) {
            return false;
        }

        if (!(parentView3rd.getParent() instanceof ViewGroup parentView4th)) {
            return false;
        }

        // Dismiss View [R.id.touch_outside] is the 1st ChildView of the 4th ParentView.
        // This only shows in phone layout.
        Utils.clickView(parentView4th.getChildAt(0));

        // In tablet layout there is no Dismiss View, instead we just hide all two parent views.
        parentView3rd.setVisibility(View.GONE);
        parentView4th.setVisibility(View.GONE);

        // Show custom playback speed menu (either dialog or modern bottom sheet).
        showCustomPlaybackSpeedMenu(recyclerView.getContext());

        return true;
    }

    /**
     * This method is sometimes used multiple times
     * To prevent this, ignore method reuse within 1 second.
     *
     * @param context Context for [playbackSpeedDialogListener]
     */
    private static void showCustomPlaybackSpeedMenu(@NonNull Context context) {
        final long now = System.currentTimeMillis();
        if (now - lastTimeOldPlaybackMenuInvoked < 1000) {
            return;
        }
        lastTimeOldPlaybackMenuInvoked = now;

        if (Settings.CUSTOM_PLAYBACK_SPEED_MENU_TYPE.get()) {
            // Open playback speed dialog (dialog builder)
            VideoUtils.showPlaybackSpeedDialog(context);
        } else {
            // Open modern bottom sheet-like menu
            showModernCustomPlaybackSpeedDialog(context);
        }
    }

    /**
     * Displays a modern custom dialog for adjusting video playback speed.
     * <p>
     * This method creates a dialog with a slider, plus/minus buttons, and preset speed buttons
     * to allow the user to modify the video playback speed. The dialog is styled with rounded
     * corners and themed colors, positioned at the bottom of the screen. The playback speed
     * can be adjusted in 0.05 increments using the slider or buttons, or set directly to preset
     * values. The dialog updates the displayed speed in real-time and applies changes to the
     * video playback. The dialog is dismissed if the player enters Picture-in-Picture (PiP) mode.
     */
    @SuppressLint({"SetTextI18n", "ClickableViewAccessibility"})
    public static void showModernCustomPlaybackSpeedDialog(Context context) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        currentDialog = new WeakReference<>(dialog);

        LinearLayout mainLayout = new LinearLayout(context);
        mainLayout.setOrientation(LinearLayout.VERTICAL);

        final int dip4 = dpToPx(4);
        final int dip5 = dpToPx(5);
        final int dip6 = dpToPx(6);
        final int dip8 = dpToPx(8);
        final int dip20 = dpToPx(20);
        final int dip32 = dpToPx(32);
        final int dip36 = dpToPx(36);
        final int dip40 = dpToPx(40);
        final int dip60 = dpToPx(60);
        final int swipeThreshold = dpToPx(100);

        mainLayout.setPadding(dip5, dip8, dip5, dip8);

        RoundRectShape roundRectShape = new RoundRectShape(createCornerRadii(12), null, null);
        ShapeDrawable background = new ShapeDrawable(roundRectShape);
        background.getPaint().setColor(ThemeUtils.getDialogBackgroundColor());
        mainLayout.setBackground(background);

        View handleBar = new View(context);
        ShapeDrawable handleBackground = new ShapeDrawable(new RoundRectShape(createCornerRadii(4), null, null));
        handleBackground.getPaint().setColor(getAdjustedBackgroundColor(true));
        handleBar.setBackground(handleBackground);
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(dip40, dip4);
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        handleParams.setMargins(0, 0, 0, dip20);
        handleBar.setLayoutParams(handleParams);
        mainLayout.addView(handleBar);

        // Display current playback speed.
        TextView currentSpeedText = new TextView(context);
        float currentSpeed = VideoInformation.getPlaybackSpeed();
        currentSpeedText.setText(formatSpeedStringX(currentSpeed, 0));
        currentSpeedText.setTextColor(ThemeUtils.getForegroundColor());
        currentSpeedText.setTextSize(16);
        currentSpeedText.setTypeface(Typeface.DEFAULT_BOLD);
        currentSpeedText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        textParams.setMargins(0, 0, 0, 0);
        currentSpeedText.setLayoutParams(textParams);
        mainLayout.addView(currentSpeedText);

        // Create horizontal layout for slider and +/- buttons.
        LinearLayout sliderLayout = new LinearLayout(context);
        sliderLayout.setOrientation(LinearLayout.HORIZONTAL);
        sliderLayout.setGravity(Gravity.CENTER_VERTICAL);
        sliderLayout.setPadding(dip5, dip5, dip5, dip5);

        // Create minus button.
        Button minusButton = new Button(context, null, 0);
        minusButton.setText("");
        ShapeDrawable minusBackground = new ShapeDrawable(new RoundRectShape(createCornerRadii(20), null, null));
        minusBackground.getPaint().setColor(getAdjustedBackgroundColor(false));
        minusButton.setBackground(minusBackground);
        OutlineSymbolDrawable minusDrawable = new OutlineSymbolDrawable(false);
        minusButton.setForeground(minusDrawable);
        LinearLayout.LayoutParams minusParams = new LinearLayout.LayoutParams(dip36, dip36);
        minusParams.setMargins(0, 0, dip5, 0);
        minusButton.setLayoutParams(minusParams);

        // Create slider for speed adjustment.
        SeekBar speedSlider = new SeekBar(context);
        speedSlider.setMax(speedToProgressValue(customPlaybackSpeedsMax));
        speedSlider.setProgress(speedToProgressValue(currentSpeed));
        speedSlider.getProgressDrawable().setColorFilter(ThemeUtils.getForegroundColor(), PorterDuff.Mode.SRC_IN);
        speedSlider.getThumb().setColorFilter(ThemeUtils.getForegroundColor(), PorterDuff.Mode.SRC_IN);
        LinearLayout.LayoutParams sliderParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        sliderParams.setMargins(dip5, 0, dip5, 0);
        speedSlider.setLayoutParams(sliderParams);

        // Create plus button.
        Button plusButton = new Button(context, null, 0);
        plusButton.setText("");
        ShapeDrawable plusBackground = new ShapeDrawable(new RoundRectShape(createCornerRadii(20), null, null));
        plusBackground.getPaint().setColor(getAdjustedBackgroundColor(false));
        plusButton.setBackground(plusBackground);
        OutlineSymbolDrawable plusDrawable = new OutlineSymbolDrawable(true);
        plusButton.setForeground(plusDrawable);
        LinearLayout.LayoutParams plusParams = new LinearLayout.LayoutParams(dip36, dip36);
        plusParams.setMargins(dip5, 0, 0, 0);
        plusButton.setLayoutParams(plusParams);

        sliderLayout.addView(minusButton);
        sliderLayout.addView(speedSlider);
        sliderLayout.addView(plusButton);

        LinearLayout.LayoutParams sliderLayoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sliderLayoutParams.setMargins(0, 0, 0, dip5);
        sliderLayout.setLayoutParams(sliderLayoutParams);
        mainLayout.addView(sliderLayout);

        Function<Float, Void> userSelectedSpeed = newSpeed -> {
            final float roundedSpeed = roundSpeedToNearestIncrement(newSpeed);
            if (VideoInformation.getPlaybackSpeed() == roundedSpeed) {
                return null;
            }
            VideoInformation.overridePlaybackSpeed(roundedSpeed);
            PlaybackSpeedPatch.userSelectedPlaybackSpeed(roundedSpeed);
            currentSpeedText.setText(formatSpeedStringX(roundedSpeed, 2));
            speedSlider.setProgress(speedToProgressValue(roundedSpeed));
            return null;
        };

        // Set listener for slider to update playback speed.
        speedSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    userSelectedSpeed.apply(customPlaybackSpeedsMin + (progress / PROGRESS_BAR_VALUE_SCALE));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        minusButton.setOnClickListener(v -> userSelectedSpeed.apply(VideoInformation.getPlaybackSpeed() - 0.05f));
        plusButton.setOnClickListener(v -> userSelectedSpeed.apply(VideoInformation.getPlaybackSpeed() + 0.05f));

        // Create GridLayout for preset speed buttons.
        GridLayout gridLayout = new GridLayout(context);
        gridLayout.setColumnCount(5);
        gridLayout.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        gridLayout.setRowCount((int) Math.ceil(customPlaybackSpeeds.length / 5.0));
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        gridParams.setMargins(0, 0, 0, 0);
        gridLayout.setLayoutParams(gridParams);

        // For all buttons show at least 1 zero in decimal (2 -> "2.0").
        speedFormatter.setMinimumFractionDigits(1);

        for (float speed : customPlaybackSpeeds) {
            FrameLayout buttonContainer = new FrameLayout(context);
            GridLayout.LayoutParams containerParams = new GridLayout.LayoutParams();
            containerParams.width = 0;
            containerParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
            containerParams.setMargins(dip5, 0, dip5, 0);
            containerParams.height = dip60;
            buttonContainer.setLayoutParams(containerParams);

            Button speedButton = new Button(context, null, 0);
            speedButton.setText(speedFormatter.format(speed));
            speedButton.setTextColor(ThemeUtils.getForegroundColor());
            speedButton.setTextSize(12);
            speedButton.setAllCaps(false);
            speedButton.setGravity(Gravity.CENTER);
            ShapeDrawable buttonBackground = new ShapeDrawable(new RoundRectShape(createCornerRadii(20), null, null));
            buttonBackground.getPaint().setColor(getAdjustedBackgroundColor(false));
            speedButton.setBackground(buttonBackground);
            speedButton.setPadding(dip5, dip5, dip5, dip5);
            FrameLayout.LayoutParams buttonParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, dip32, Gravity.CENTER);
            speedButton.setLayoutParams(buttonParams);
            buttonContainer.addView(speedButton);

            if (speed == 1.0f) {
                TextView normalLabel = new TextView(context);
                normalLabel.setText(str("normal_playback_rate_label"));
                normalLabel.setTextColor(ThemeUtils.getForegroundColor());
                normalLabel.setTextSize(10);
                normalLabel.setGravity(Gravity.CENTER);
                FrameLayout.LayoutParams labelParams = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
                labelParams.bottomMargin = 0;
                normalLabel.setLayoutParams(labelParams);
                buttonContainer.addView(normalLabel);
            }

            speedButton.setOnClickListener(v -> userSelectedSpeed.apply(speed));
            gridLayout.addView(buttonContainer);
        }

        mainLayout.addView(gridLayout);

        LinearLayout wrapperLayout = new LinearLayout(context);
        wrapperLayout.setOrientation(LinearLayout.VERTICAL);
        wrapperLayout.setPadding(dip8, 0, dip8, 0);
        wrapperLayout.addView(mainLayout);
        dialog.setContentView(wrapperLayout);

        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.BOTTOM;
            params.y = dip6;
            int portraitWidth = context.getResources().getDisplayMetrics().widthPixels;
            if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                portraitWidth = Math.min(portraitWidth, context.getResources().getDisplayMetrics().heightPixels);
            }
            params.width = portraitWidth;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(params);
            window.setBackgroundDrawable(null);
        }

        // Gesture handling for swipe-to-dismiss
        final float[] initialTouchY = {0};
        final View decorView = window != null ? window.getDecorView() : wrapperLayout;

        decorView.setOnTouchListener(new View.OnTouchListener() {
            private boolean isDragging = false;
            private float lastY = 0;
            private boolean isChildHandling = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Dispatch event to children first (e.g., buttons, SeekBar)
                boolean childConsumed = wrapperLayout.dispatchTouchEvent(event);

                // Check if a child is handling the event (e.g., SeekBar drag, button click)
                if (childConsumed && (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE)) {
                    isChildHandling = true;
                    return false;
                }

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        float x = event.getRawX();
                        float y = event.getRawY();
                        int[] location = new int[2];
                        mainLayout.getLocationOnScreen(location);
                        int dialogLeft = location[0];
                        int dialogTop = location[1];
                        int dialogRight = dialogLeft + mainLayout.getWidth();
                        int dialogBottom = dialogTop + mainLayout.getHeight();

                        // Check if touch is outside the dialog bounds
                        if (x < dialogLeft || x > dialogRight || y < dialogTop || y > dialogBottom) {
                            dialog.dismiss();
                            Logger.printDebug(() -> "Dialog dismissed due to touch outside");
                            return true;
                        }
                        isChildHandling = false;
                        initialTouchY[0] = event.getRawY();
                        lastY = event.getRawY();
                        isDragging = true;
                        Logger.printDebug(() -> "Swipe started, initial Y: " + initialTouchY[0]);
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if (!isDragging || isChildHandling) return false;
                        float currentY = event.getRawY();
                        float deltaY = currentY - lastY;
                        float newTranslationY = mainLayout.getTranslationY() + deltaY;
                        if (newTranslationY >= 0) {
                            mainLayout.setTranslationY(newTranslationY);
                            Logger.printDebug(() -> "Dragging, translationY: " + newTranslationY);
                        }
                        lastY = currentY;
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (!isDragging || isChildHandling) {
                            isChildHandling = false;
                            return false;
                        }
                        isDragging = false;
                        float finalTranslationY = mainLayout.getTranslationY();
                        Logger.printDebug(() -> "Swipe ended, final translationY: " + finalTranslationY);
                        if (finalTranslationY > swipeThreshold) {
                            // Dismiss with animation
                            mainLayout.animate()
                                    .translationY(mainLayout.getHeight())
                                    .setDuration(200)
                                    .setListener(new AnimatorListenerAdapter() {
                                        @Override
                                        public void onAnimationEnd(Animator animation) {
                                            dialog.dismiss();
                                            Logger.printDebug(() -> "Dialog dismissed via swipe");
                                        }
                                    })
                                    .start();
                        } else {
                            // Return to original position
                            mainLayout.animate()
                                    .translationY(0)
                                    .setDuration(200)
                                    .setListener(null)
                                    .start();
                            Logger.printDebug(() -> "Dialog returned to original position");
                        }
                        isChildHandling = false;
                        return true;
                }
                return false;
            }
        });

        Function1<PlayerType, Unit> playerTypeObserver = new Function1<>() {
            @Override
            public Unit invoke(PlayerType type) {
                Dialog current = currentDialog.get();
                if (current == null || !current.isShowing()) {
                    PlayerType.getOnChange().removeObserver(this);
                    Logger.printException(() -> "Removing player type listener as dialog is null or closed");
                } else if (type == PlayerType.WATCH_WHILE_PICTURE_IN_PICTURE) {
                    current.dismiss();
                    Logger.printDebug(() -> "Playback speed dialog dismissed due to PiP mode");
                }
                return Unit.INSTANCE;
            }
        };

        PlayerType.getOnChange().addObserver(playerTypeObserver);

        dialog.setOnDismissListener(d -> {
            PlayerType.getOnChange().removeObserver(playerTypeObserver);
            Logger.printDebug(() -> "PlayerType observer removed on dialog dismiss");
        });

        final int fadeDurationFast = Utils.getResourceInteger("fade_duration_fast");
        Animation slideInAnimation = Utils.getResourceAnimation("slide_in_bottom");
        slideInAnimation.setDuration(fadeDurationFast);
        mainLayout.startAnimation(slideInAnimation);

        dialog.show();
    }

    /**
     * Creates an array of corner radii for a rounded rectangle shape.
     *
     * @param dp The radius in density-independent pixels (dp) to apply to all corners.
     * @return An array of eight float values representing the corner radii
     * (top-left, top-right, bottom-right, bottom-left).
     */
    private static float[] createCornerRadii(float dp) {
        final float radius = dpToPx(dp);
        return new float[]{radius, radius, radius, radius, radius, radius, radius, radius};
    }

    /**
     * @param speed The playback speed value to format.
     * @return A string representation of the speed with 'x' (e.g. "1.25x" or "1.00x").
     */
    private static String formatSpeedStringX(float speed, int minimumFractionDigits) {
        speedFormatter.setMinimumFractionDigits(minimumFractionDigits);
        return speedFormatter.format(speed) + 'x';
    }

    /**
     * @return user speed converted to a value for {@link SeekBar#setProgress(int)}.
     */
    private static int speedToProgressValue(float speed) {
        return (int) ((speed - customPlaybackSpeedsMin) * PROGRESS_BAR_VALUE_SCALE);
    }

    /**
     * Rounds the given playback speed to the nearest 0.05 increment and ensures it is within valid bounds.
     *
     * @param speed The playback speed to round.
     * @return The rounded speed, constrained to the specified bounds.
     */
    private static float roundSpeedToNearestIncrement(float speed) {
        // Round to nearest 0.05 speed.
        final float roundedSpeed = Math.round(speed / 0.05f) * 0.05f;
        return SeekbarColorPatch.clamp(roundedSpeed, 0.05f, PLAYBACK_SPEED_MAXIMUM);
    }

    /**
     * Adjusts the background color based on the current theme.
     *
     * @param isHandleBar If true, applies a stronger darkening factor (0.9) for the handle bar in light theme;
     *                    if false, applies a standard darkening factor (0.95) for other elements in light theme.
     * @return A modified background color, lightened by 20% for dark themes or darkened by 5% (or 10% for handle bar)
     * for light themes to ensure visual contrast.
     */
    public static int getAdjustedBackgroundColor(boolean isHandleBar) {
        final int baseColor = ThemeUtils.getDialogBackgroundColor();
        float darkThemeFactor = isHandleBar ? 1.25f : 1.115f; // 1.25f for handleBar, 1.115f for others in dark theme.
        float lightThemeFactor = isHandleBar ? 0.9f : 0.95f; // 0.9f for handleBar, 0.95f for others in light theme.
        return ThemeUtils.isDarkTheme()
                ? ThemeUtils.adjustColorBrightness(baseColor, darkThemeFactor)  // Lighten for dark theme.
                : ThemeUtils.adjustColorBrightness(baseColor, lightThemeFactor); // Darken for light theme.
    }
}

/**
 * Custom Drawable for rendering outlined plus and minus symbols on buttons.
 */
class OutlineSymbolDrawable extends Drawable {
    private final boolean isPlus; // Determines if the symbol is a plus or minus.
    private final Paint paint;

    OutlineSymbolDrawable(boolean isPlus) {
        this.isPlus = isPlus;
        paint = new Paint(Paint.ANTI_ALIAS_FLAG); // Enable anti-aliasing for smooth rendering.
        paint.setColor(ThemeUtils.getForegroundColor());
        paint.setStyle(Paint.Style.STROKE); // Use stroke style for outline.
        paint.setStrokeWidth(dpToPx(1)); // 1dp stroke width.
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        final int width = bounds.width();
        final int height = bounds.height();
        final float centerX = width / 2f; // Center X coordinate.
        final float centerY = height / 2f; // Center Y coordinate.
        final float size = Math.min(width, height) * 0.25f; // Symbol size is 25% of button dimensions.

        // Draw horizontal line for both plus and minus symbols.
        canvas.drawLine(centerX - size, centerY, centerX + size, centerY, paint);
        if (isPlus) {
            // Draw vertical line for plus symbol.
            canvas.drawLine(centerX, centerY - size, centerX, centerY + size, paint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}

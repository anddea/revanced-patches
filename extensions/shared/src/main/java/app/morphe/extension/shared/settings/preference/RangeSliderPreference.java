/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Additional Terms & Attribution Requirements
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Source Credit Preservation (Section 7(b)): This specific copyright notice
 *    and the list of original authors above must be preserved in any copy
 *    or derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin & Modification Marking (Section 7(c)): Modified versions must be
 *    clearly marked as such (e.g., by adding a "Modified by" line or a new
 *    copyright notice) and must not be misrepresented as the original work.
 *
 * 3. Version Control Attribution (Section 7(b)): Any ports or substantial
 *    modifications must retain historical authorship credit in version
 *    control systems (e.g., Git), listing original authors appropriately and
 *    modifiers as committers or co-authors.
 *
 * 4. User Interface Attribution (Section 7(b)): Any works containing or
 *    derived from this material must maintain a visible credit or
 *    acknowledgment to the original author(s) within the application's user
 *    interface (e.g., in an "About" or "Credits" section).
 */

package app.morphe.extension.shared.settings.preference;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;

import app.morphe.extension.shared.settings.Setting;
import app.morphe.extension.shared.settings.StringSetting;
import app.morphe.extension.shared.ui.Dim;
import app.morphe.extension.shared.utils.Logger;

/**
 * Inline two-thumb slider for settings represented by two comma-separated decimal values.
 *
 * <p>The range is intentionally validated here as well as by the code that consumes the setting.
 * This means malformed imported values and manually entered values are reset to the setting's
 * declared default before they can be displayed by the preference.</p>
 */
@SuppressWarnings({"unused", "deprecation"})
public class RangeSliderPreference extends ResettableEditTextPreference {
    private static final String RANGE_WIDGET_TAG = "morphe_range_slider_widget";
    private static final String PRIMARY_COLOR_KEY = "revanced_custom_seekbar_color_primary";
    private static final String ACCENT_COLOR_KEY = "revanced_custom_seekbar_color_accent";

    private record RangeValue(float start, float end) {
    }

    private record RangeSliderViews(LinearLayout widget,
                                    RangeSeekBar slider,
                                    TextView minLabel,
                                    TextView maxLabel,
                                    TextView valueLabel) {
    }

    public RangeSliderPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public RangeSliderPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public RangeSliderPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RangeSliderPreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        setSelectable(true);
    }

    /** Requests a rebind after the shared slider-summary visibility setting changes. */
    public void refreshSummaryVisibility() {
        notifyChanged();
    }

    @Override
    protected void onClick() {
        showDialog(null);
    }

    @Override
    protected void showDialog(Bundle state) {
        final StringSetting setting = getRangeSetting();
        if (setting != null) {
            // ResettableEditTextPreference seeds its dialog from getText(). Keep it synchronized
            // with the value changed by either thumb before opening the existing text dialog.
            super.setText(setting.get());
        }
        super.showDialog(state);
    }

    @Override
    public void setText(String text) {
        final StringSetting setting = getRangeSetting();
        if (setting != null) {
            final RangeValue range = parseRange(text);
            if (range == null) {
                setting.resetToDefault();
                text = setting.defaultValue;
            } else {
                saveRange(setting, range);
                text = setting.get();
            }
        }

        super.setText(text);
        notifyChanged();
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        final StringSetting setting = getRangeSetting();
        if (setting == null) {
            Logger.printException(() -> "Missing range slider setting for: " + getKey());
            return;
        }

        final RangeValue currentValue = getRangeValue(setting);
        final View widgetFrame = view.findViewById(android.R.id.widget_frame);
        if (widgetFrame != null) {
            widgetFrame.setVisibility(View.GONE);
            if (widgetFrame instanceof ViewGroup widgetGroup) {
                widgetGroup.removeAllViews();
            }
        }

        final View titleView = view.findViewById(android.R.id.title);
        final View summaryView = view.findViewById(android.R.id.summary);
        if (titleView == null) {
            Logger.printException(() -> "Missing preference title view for setting: " + getKey());
            return;
        }

        final View.OnClickListener openManualDialog = ignored -> showDialog(null);
        titleView.setOnClickListener(isEnabled() ? openManualDialog : null);
        if (summaryView != null) {
            summaryView.setOnClickListener(isEnabled() ? openManualDialog : null);
        }

        final ViewGroup sliderContent = SliderPreference.prepareSliderContent(
                getContext(), titleView, summaryView, SliderPreference.areSummariesVisible());
        if (sliderContent == null) {
            Logger.printException(() -> "Missing preference content view for setting: " + getKey());
            return;
        }

        final RangeSliderViews rangeViews = createRangeSliderViews(getContext(), currentValue);
        final View oldSliderWidget = sliderContent.findViewWithTag(RANGE_WIDGET_TAG);
        if (oldSliderWidget != null) {
            sliderContent.removeView(oldSliderWidget);
        }
        sliderContent.addView(rangeViews.widget(), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        bindRangeControls(setting, rangeViews);
    }

    /** Binds this preference to a range slider used by the settings-search result row. */
    public void bindInlineSlider(@NonNull RangeSeekBar slider,
                                 @NonNull TextView minLabel,
                                 @NonNull TextView maxLabel,
                                 @NonNull TextView valueLabel) {
        final StringSetting setting = getRangeSetting();
        if (setting == null) {
            Logger.printException(() -> "Missing range slider setting for: " + getKey());
            return;
        }

        final RangeSliderViews rangeViews = new RangeSliderViews(
                null, slider, minLabel, maxLabel, valueLabel);
        bindRangeControls(setting, rangeViews);
    }

    private void bindRangeControls(StringSetting setting, RangeSliderViews rangeViews) {
        final RangeValue currentValue = getRangeValue(setting);
        rangeViews.minLabel().setText("0");
        rangeViews.maxLabel().setText("1");
        rangeViews.slider().setColors(
                resolveColor(PRIMARY_COLOR_KEY, resolveAccentColor()),
                resolveColor(ACCENT_COLOR_KEY, Color.WHITE)
        );
        rangeViews.slider().setPreciseSeekingChangedListener(null);
        rangeViews.slider().setTouchEndedListener(null);
        rangeViews.slider().resetPreciseSeeking();
        rangeViews.slider().setOnRangeChangeListener(null);
        rangeViews.slider().setRange(currentValue.start(), currentValue.end());
        updateRangeLabel(rangeViews.valueLabel(), currentValue);
        rangeViews.valueLabel().setOnClickListener(isEnabled() ? ignored -> showDialog(null) : null);
        rangeViews.minLabel().animate().cancel();
        rangeViews.maxLabel().animate().cancel();
        rangeViews.minLabel().setAlpha(1.0f);
        rangeViews.maxLabel().setAlpha(1.0f);
        rangeViews.slider().setPreciseSeekingChangedListener(preciseSeeking ->
                animateEndpointLabels(
                        rangeViews.minLabel(), rangeViews.maxLabel(), preciseSeeking
                ));

        final RangeValue[] initialValue = { null };
        final boolean[] changed = { false };
        final boolean[] interactionFinished = { false };
        rangeViews.slider().setOnRangeChangeListener(new RangeSeekBar.OnRangeChangeListener() {
            @Override
            public void onStartTrackingTouch(@NonNull RangeSeekBar slider) {
                initialValue[0] = getRangeValue(setting);
                changed[0] = false;
                interactionFinished[0] = false;
            }

            @Override
            public void onRangeChanged(@NonNull RangeSeekBar slider,
                                       float start,
                                       float end,
                                       boolean fromUser) {
                final RangeValue value = new RangeValue(start, end);
                updateRangeLabel(rangeViews.valueLabel(), value);
                if (fromUser) {
                    changed[0] = initialValue[0] != null && !initialValue[0].equals(value);
                    // Persist each step while the interaction guard prevents the shared
                    // preference listener from rebinding the slider or showing a dialog.
                    AbstractPreferenceFragment.setSliderInteractionInProgress(true);
                    try {
                        saveRange(setting, value);
                    } finally {
                        AbstractPreferenceFragment.setSliderInteractionInProgress(false);
                    }
                }
            }

        });
        rangeViews.slider().setTouchEndedListener(() -> {
            if (interactionFinished[0]) {
                return;
            }
            interactionFinished[0] = true;
            AbstractPreferenceFragment.setSliderInteractionInProgress(false);
            if (changed[0] && setting.rebootApp) {
                AbstractPreferenceFragment.showRestartDialog(getContext());
            }
            notifyChanged();
        });
        rangeViews.slider().setEnabled(isEnabled());
        rangeViews.valueLabel().setEnabled(isEnabled());
    }

    private static RangeSliderViews createRangeSliderViews(Context context, RangeValue currentValue) {
        final LinearLayout widget = new LinearLayout(context);
        widget.setOrientation(LinearLayout.VERTICAL);
        widget.setGravity(android.view.Gravity.CENTER_VERTICAL);
        widget.setPadding(0, Dim.dp4, 0, 0);
        widget.setClipChildren(false);
        widget.setClipToPadding(false);
        widget.setTag(RANGE_WIDGET_TAG);

        final LinearLayout endpointLabels = new LinearLayout(context);
        endpointLabels.setGravity(android.view.Gravity.CENTER_VERTICAL);

        final TextView minLabel = createLabel(context);
        minLabel.setText("0");
        endpointLabels.addView(minLabel);

        final Space spacer = new Space(context);
        endpointLabels.addView(spacer, new LinearLayout.LayoutParams(0, Dim.dp(1), 1f));

        final TextView maxLabel = createLabel(context);
        maxLabel.setText("1");
        maxLabel.setGravity(android.view.Gravity.END);
        endpointLabels.addView(maxLabel);

        widget.addView(endpointLabels, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        final RangeSeekBar slider = new RangeSeekBar(context);
        widget.addView(slider, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        final TextView valueLabel = createLabel(context);
        valueLabel.setGravity(android.view.Gravity.CENTER);
        valueLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        updateRangeLabel(valueLabel, currentValue);
        widget.addView(valueLabel, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        return new RangeSliderViews(widget, slider, minLabel, maxLabel, valueLabel);
    }

    private static TextView createLabel(Context context) {
        final TextView label = new TextView(context);
        label.setSingleLine(true);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        return label;
    }

    @Nullable
    private StringSetting getRangeSetting() {
        final String key = getKey();
        final Setting<?> setting = key == null ? null : Setting.getSettingFromPath(key);
        return setting instanceof StringSetting stringSetting ? stringSetting : null;
    }

    private static RangeValue getRangeValue(StringSetting setting) {
        RangeValue value = parseRange(setting.get());
        if (value == null) {
            setting.resetToDefault();
            value = parseRange(setting.defaultValue);
        }
        return value == null ? new RangeValue(0.0f, 1.0f) : value;
    }

    @Nullable
    private static RangeValue parseRange(@Nullable String text) {
        if (text == null) {
            return null;
        }

        final String[] values = text.split(",");
        if (values.length != 2) {
            return null;
        }

        try {
            final float start = Float.parseFloat(values[0].trim());
            final float end = Float.parseFloat(values[1].trim());
            if (!Float.isFinite(start) || !Float.isFinite(end)
                    || start < 0.0f || end > 1.0f || start > end) {
                return null;
            }
            return new RangeValue(start, end);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static void saveRange(StringSetting setting, RangeValue value) {
        final RangeValue defaultValue = parseRange(setting.defaultValue);
        if (defaultValue != null && defaultValue.equals(value)) {
            setting.resetToDefault();
        } else {
            setting.save(formatRange(value));
        }
    }

    private static String formatRange(RangeValue value) {
        return formatFraction(value.start()) + ", " + formatFraction(value.end());
    }

    private static String formatFraction(float value) {
        return BigDecimal.valueOf(value)
                .setScale(3, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    private static void updateRangeLabel(TextView valueLabel, RangeValue value) {
        valueLabel.setText(formatRange(value));
    }

    /** Fades range endpoints while precise seeking exposes the expanded tick rail. */
    private static void animateEndpointLabels(TextView minLabel,
                                               TextView maxLabel,
                                               boolean preciseSeeking) {
        final float targetAlpha = preciseSeeking ? 0.0f : 1.0f;
        minLabel.animate()
                .alpha(targetAlpha)
                .setDuration(180L)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        maxLabel.animate()
                .alpha(targetAlpha)
                .setDuration(180L)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    private int resolveAccentColor() {
        final TypedValue typedValue = new TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.colorAccent, typedValue, true);
        return typedValue.data;
    }

    private int resolveColor(String key, int fallback) {
        final Setting<?> setting = Setting.getSettingFromPath(key);
        if (setting == null) {
            return fallback;
        }
        try {
            return Color.parseColor(String.valueOf(setting.get()));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    /**
     * A small, dependency-free two-thumb slider. It is a View rather than a platform SeekBar
     * because Android's SeekBar has only one progress thumb and cannot represent an interval.
     */
    public static final class RangeSeekBar extends View {
        private static final int PROGRESS_STEPS = 1000;
        private static final float INACTIVE_TRACK_ALPHA = 0.30f;
        private static final float DRAGGING_TRACK_SCALE = 1.55f;
        private static final int PRECISE_TICK_SPACING = Dim.dp(24);
        private static final int PRECISE_LONG_TICK_HEIGHT = Dim.dp(12);
        private static final int PRECISE_SHORT_TICK_HEIGHT = Dim.dp(6);
        private static final float PRECISE_EDGE_GUTTER = 0.16f;
        private static final int PRECISE_EXTRA_HEIGHT = Dim.dp(12);
        private static final long PRECISE_SEEKING_DELAY = 1_000L;

        private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint gradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint thumbStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float thumbRadius = Dim.dp(9);
        private final Handler handler = new Handler(Looper.getMainLooper());

        private ValueAnimator trackAnimator;
        private ValueAnimator preciseAnimator;
        private float trackThicknessScale = 1.0f;
        private float preciseAnimationProgress;
        private float startValue = 0.0f;
        private float endValue = 1.0f;
        private int primaryColor = Color.WHITE;
        private int accentColor = Color.WHITE;
        private int activeThumb = -1;
        private int preciseWindowStart;
        private int preciseWindowEnd = PROGRESS_STEPS;
        private float touchDownX;
        private float touchDownY;
        private float lastTouchX;
        private float lastTouchY;
        private float preciseTouchDownX;
        private float preciseTouchDownY;
        private float lastPointerFraction;
        private float touchSlop;
        private boolean touchSlopExceeded;
        private boolean preciseHoldEligible;
        private boolean preciseSeeking;
        private boolean preciseMovementStarted;
        private boolean tracking;
        private int edgeScrollDirection;
        private final Runnable edgeScrollRunnable = this::scrollAtEdge;
        @Nullable
        private Runnable preciseSeekingRunnable;
        @Nullable
        private OnPreciseSeekingChangedListener preciseSeekingChangedListener;
        @Nullable
        private OnTouchEndedListener touchEndedListener;

        @FunctionalInterface
        public interface OnPreciseSeekingChangedListener {
            void onPreciseSeekingChanged(boolean preciseSeeking);
        }

        @FunctionalInterface
        public interface OnTouchEndedListener {
            void onTouchEnded();
        }
        @Nullable
        private OnRangeChangeListener listener;

        public RangeSeekBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
            initialize();
        }

        public RangeSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            initialize();
        }

        public RangeSeekBar(Context context, AttributeSet attrs) {
            super(context, attrs);
            initialize();
        }

        public RangeSeekBar(Context context) {
            super(context);
            initialize();
        }

        private void initialize() {
            setFocusable(true);
            setClickable(true);
            setWillNotDraw(false);
            thumbStrokePaint.setStyle(Paint.Style.STROKE);
            thumbStrokePaint.setStrokeWidth(Dim.dp(1));
            thumbStrokePaint.setColor(Color.WHITE);
            markerPaint.setStrokeWidth(Math.max(1.0f, Dim.dp(1)));
            markerPaint.setStrokeCap(Paint.Cap.ROUND);
            touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        }

        @Override
        public void setEnabled(boolean enabled) {
            if (!enabled) {
                resetPreciseSeeking();
            }
            super.setEnabled(enabled);
            // Both thumbs and the gradient are custom-drawn, so dim the complete view when the
            // parent setting makes this preference unavailable.
            setAlpha(enabled ? 1.0f : 0.5f);
            invalidate();
        }

        public void setColors(int primaryColor, int accentColor) {
            this.primaryColor = primaryColor;
            this.accentColor = accentColor;
            invalidate();
        }

        public void setRange(float startValue, float endValue) {
            this.startValue = clamp(startValue);
            this.endValue = clamp(endValue);
            if (this.startValue > this.endValue) {
                final float swap = this.startValue;
                this.startValue = this.endValue;
                this.endValue = swap;
            }
            invalidate();
        }

        public void setOnRangeChangeListener(@Nullable OnRangeChangeListener listener) {
            this.listener = listener;
        }

        /** Registers a listener for the long-press precise-seeking mode. */
        public void setPreciseSeekingChangedListener(
                @Nullable OnPreciseSeekingChangedListener listener) {
            preciseSeekingChangedListener = listener;
        }

        /** Registers a callback for the end of the range touch session. */
        public void setTouchEndedListener(@Nullable OnTouchEndedListener listener) {
            touchEndedListener = listener;
        }

        /** Cancels transient precise-seeking state before this view is rebound or disabled. */
        public void resetPreciseSeeking() {
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(false);
            }
            cancelPreciseSeekingRunnable();
            cancelEdgeScroll();
            if (preciseAnimator != null) {
                preciseAnimator.cancel();
                preciseAnimator = null;
            }
            if (preciseSeeking && preciseSeekingChangedListener != null) {
                preciseSeekingChangedListener.onPreciseSeekingChanged(false);
            }
            preciseSeeking = false;
            preciseAnimationProgress = 0.0f;
            preciseWindowStart = 0;
            preciseWindowEnd = PROGRESS_STEPS;
            activeThumb = -1;
            tracking = false;
            touchSlopExceeded = false;
            preciseHoldEligible = false;
            preciseMovementStarted = false;
            requestLayout();
            invalidate();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            final int width = MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED
                    ? Dim.dp(200) : MeasureSpec.getSize(widthMeasureSpec);
            final int baseHeight = Dim.dp(40);
            final int desiredHeight = baseHeight
                    + Math.round(PRECISE_EXTRA_HEIGHT * preciseAnimationProgress);
            final int height = resolveSize(desiredHeight, heightMeasureSpec);
            setMeasuredDimension(width, height);
        }

        @SuppressLint("DrawAllocation")
        @Override
        protected void onDraw(@NonNull Canvas canvas) {
            super.onDraw(canvas);

            final float centerY = getHeight() / 2.0f;
            final float thickness = Dim.dp(4) * trackThicknessScale
                    * (1.0f + 0.12f * preciseAnimationProgress);
            final float halfThickness = thickness / 2.0f;
            final float left = 0.0f;
            final float right = getWidth();
            final float startX = visualPositionForValue(startValue);
            final float endX = visualPositionForValue(endValue);

            drawPreciseMarkers(canvas, centerY);

            trackPaint.setColor(Color.argb(
                    Math.round(Color.alpha(primaryColor) * INACTIVE_TRACK_ALPHA),
                    Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor)
            ));
            canvas.drawRoundRect(
                    left, centerY - halfThickness, right, centerY + halfThickness,
                    halfThickness, halfThickness, trackPaint
            );

            if (endX > startX) {
                gradientPaint.setShader(new LinearGradient(
                        0.0f, 0.0f, Math.max(1, getWidth()), 0.0f,
                        primaryColor, accentColor, Shader.TileMode.CLAMP
                ));
                canvas.drawRoundRect(
                        startX, centerY - halfThickness, endX, centerY + halfThickness,
                        halfThickness, halfThickness, gradientPaint
                );
                gradientPaint.setShader(null);
            }

            thumbPaint.setColor(primaryColor);
            canvas.drawCircle(startX, centerY, thumbRadius, thumbPaint);
            canvas.drawCircle(startX, centerY, thumbRadius, thumbStrokePaint);
            thumbPaint.setColor(accentColor);
            canvas.drawCircle(endX, centerY, thumbRadius, thumbPaint);
            canvas.drawCircle(endX, centerY, thumbRadius, thumbStrokePaint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (!isEnabled()) {
                return false;
            }

            final int action = event.getActionMasked();
            switch (action) {
                case MotionEvent.ACTION_DOWN -> {
                    touchSlopExceeded = false;
                    if (preciseAnimationProgress > 0.0f) {
                        resetPreciseSeeking();
                    }

                    activeThumb = distanceToThumb(event.getX());
                    touchDownX = event.getX();
                    touchDownY = event.getY();
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    lastPointerFraction = pointerFractionForX(lastTouchX);
                    preciseHoldEligible = isThumbHit(lastTouchX);
                    tracking = true;
                    if (getParent() != null) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    setDragging(true);
                    if (listener != null) {
                        listener.onStartTrackingTouch(this);
                    }
                    updateFromPosition(preciseHoldEligible
                            ? positionForActiveThumb()
                            : event.getX());
                    if (preciseHoldEligible) {
                        schedulePreciseSeeking();
                    }
                    return true;
                }
                case MotionEvent.ACTION_MOVE -> {
                    final float movement = (float) Math.hypot(
                            event.getX() - lastTouchX,
                            event.getY() - lastTouchY
                    );
                    if (!touchSlopExceeded) {
                        touchSlopExceeded = Math.hypot(
                                event.getX() - touchDownX,
                                event.getY() - touchDownY
                        ) > touchSlop;
                    }
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    if (tracking) {
                        if (preciseSeeking) {
                            if (!preciseMovementStarted) {
                                preciseMovementStarted = Math.hypot(
                                        event.getX() - preciseTouchDownX,
                                        event.getY() - preciseTouchDownY
                                ) > touchSlop;
                            }
                            if (!preciseMovementStarted) {
                                // Entering precise mode is visual-only until the finger moves
                                // again, so a drag ending at the edge cannot keep seeking from
                                // touch jitter or edge auto-scrolling.
                                return true;
                            }
                            updatePreciseWindow(lastTouchX);
                            updatePreciseFromPosition(lastTouchX);
                        } else {
                            if (preciseHoldEligible && movement > 0.5f) {
                                schedulePreciseSeeking();
                            }
                            if (preciseHoldEligible && !touchSlopExceeded) {
                                return true;
                            }
                            updateFromPosition(lastTouchX);
                        }
                    }
                    return true;
                }
                case MotionEvent.ACTION_UP -> {
                    if (!touchSlopExceeded) {
                        touchSlopExceeded = Math.hypot(
                                event.getX() - touchDownX,
                                event.getY() - touchDownY
                        ) > touchSlop;
                    }
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    if (tracking) {
                        if (preciseSeeking) {
                            if (preciseMovementStarted) {
                                updatePreciseWindow(lastTouchX);
                                updatePreciseFromPosition(lastTouchX);
                            }
                            finishPreciseSeeking();
                        } else if (preciseHoldEligible && !touchSlopExceeded) {
                            updateFromPosition(positionForActiveThumb());
                        } else {
                            updateFromPosition(lastTouchX);
                        }
                        finishTracking();
                        performClick();
                    }
                    return true;
                }
                default -> {
                    // Finalize an interrupted stream without resetting the already-saved value.
                    if (tracking) {
                        if (preciseSeeking) {
                            finishPreciseSeeking();
                        }
                        finishTracking();
                    }
                    return true;
                }
            }
        }

        @Override
        public boolean performClick() {
            super.performClick();
            return true;
        }

        private int distanceToThumb(float x) {
            final float startDistance = Math.abs(x - positionForValue(startValue));
            final float endDistance = Math.abs(x - positionForValue(endValue));
            return startDistance <= endDistance ? 0 : 1;
        }

        private boolean isThumbHit(float x) {
            final float startDistance = Math.abs(x - positionForValue(startValue));
            final float endDistance = Math.abs(x - positionForValue(endValue));
            return Math.min(startDistance, endDistance) <= thumbRadius + touchSlop;
        }

        private float positionForActiveThumb() {
            return activeThumb == 0 ? positionForValue(startValue) : positionForValue(endValue);
        }

        private void updateFromPosition(float x) {
            final float value = snap(valueForPosition(x));
            final float oldStart = startValue;
            final float oldEnd = endValue;
            if (activeThumb == 0) {
                startValue = Math.min(value, endValue);
            } else {
                endValue = Math.max(value, startValue);
            }

            final boolean changed = oldStart != startValue || oldEnd != endValue;
            if (changed) {
                invalidate();
            }
            if (changed && listener != null) {
                listener.onRangeChanged(this, startValue, endValue, true);
            }
        }

        private void updatePreciseFromPosition(float x) {
            final int progress = preciseProgressForTouchX(x);
            final float value = progress / (float) PROGRESS_STEPS;
            final float oldStart = startValue;
            final float oldEnd = endValue;
            if (activeThumb == 0) {
                startValue = Math.min(value, endValue);
            } else {
                endValue = Math.max(value, startValue);
            }
            if (oldStart == startValue && oldEnd == endValue) {
                return;
            }
            invalidate();
            if (listener != null) {
                listener.onRangeChanged(this, startValue, endValue, true);
            }
        }

        private void finishTracking() {
            tracking = false;
            activeThumb = -1;
            preciseHoldEligible = false;
            cancelPreciseSeekingRunnable();
            cancelEdgeScroll();
            setDragging(false);
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(false);
            }
            if (touchEndedListener != null) {
                touchEndedListener.onTouchEnded();
            }
        }

        private void schedulePreciseSeeking() {
            if (!tracking || !preciseHoldEligible || preciseSeeking) {
                return;
            }
            cancelPreciseSeekingRunnable();
            preciseSeekingRunnable = () -> {
                if (tracking && isEnabled() && !preciseSeeking) {
                    startPreciseSeeking();
                }
            };
            handler.postDelayed(preciseSeekingRunnable, PRECISE_SEEKING_DELAY);
        }

        private void cancelPreciseSeekingRunnable() {
            if (preciseSeekingRunnable != null) {
                handler.removeCallbacks(preciseSeekingRunnable);
                preciseSeekingRunnable = null;
            }
        }

        private void cancelEdgeScroll() {
            handler.removeCallbacks(edgeScrollRunnable);
            edgeScrollDirection = 0;
        }

        private void startPreciseSeeking() {
            if (!tracking || activeThumb < 0) {
                return;
            }
            final int minimum = preciseMinimum();
            final int maximum = preciseMaximum();
            final int span = preciseWindowSpan(minimum, maximum);
            if (span <= 0) {
                return;
            }
            final int currentProgress = activeProgress();
            final int pointerOffset = preciseTickOffsetForX(lastTouchX, span);
            preciseWindowStart = Math.max(minimum, currentProgress - pointerOffset);
            preciseWindowEnd = Math.min(maximum, preciseWindowStart + span);
            preciseTouchDownX = lastTouchX;
            preciseTouchDownY = lastTouchY;
            preciseMovementStarted = false;
            preciseSeeking = true;
            animatePreciseSeeking(true);
            if (preciseSeekingChangedListener != null) {
                preciseSeekingChangedListener.onPreciseSeekingChanged(true);
            }
        }

        private void finishPreciseSeeking() {
            if (!preciseSeeking) {
                return;
            }
            preciseSeeking = false;
            preciseMovementStarted = false;
            cancelEdgeScroll();
            if (preciseSeekingChangedListener != null) {
                preciseSeekingChangedListener.onPreciseSeekingChanged(false);
            }
            animatePreciseSeeking(false);
        }

        private void animatePreciseSeeking(boolean entering) {
            if (preciseAnimator != null) {
                preciseAnimator.cancel();
            }
            preciseAnimator = ValueAnimator.ofFloat(
                    preciseAnimationProgress,
                    entering ? 1.0f : 0.0f
            );
            preciseAnimator.setDuration(entering ? 220L : 180L);
            preciseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            preciseAnimator.addUpdateListener(animation -> {
                preciseAnimationProgress = (float) animation.getAnimatedValue();
                requestLayout();
                invalidate();
                if (!preciseSeeking && preciseAnimationProgress <= 0.0f) {
                    preciseWindowStart = 0;
                    preciseWindowEnd = PROGRESS_STEPS;
                }
            });
            preciseAnimator.start();
        }

        private void updatePreciseWindow(float x) {
            if (!preciseSeeking || preciseWindowEnd <= preciseWindowStart) {
                lastPointerFraction = pointerFractionForX(x);
                return;
            }
            lastPointerFraction = pointerFractionForX(x);
            updateEdgeScrollState(lastPointerFraction);
        }

        private void updateEdgeScrollState(float pointerFraction) {
            final int minimum = preciseMinimum();
            final int maximum = preciseMaximum();
            int nextDirection = 0;
            if (pointerFraction >= 1.0f - PRECISE_EDGE_GUTTER
                    && preciseWindowEnd < maximum) {
                nextDirection = 1;
            } else if (pointerFraction <= PRECISE_EDGE_GUTTER
                    && preciseWindowStart > minimum) {
                nextDirection = -1;
            }
            if (nextDirection == edgeScrollDirection) {
                return;
            }
            cancelEdgeScroll();
            if (nextDirection != 0) {
                edgeScrollDirection = nextDirection;
                handler.postDelayed(edgeScrollRunnable, 50L);
            }
        }

        private void scrollAtEdge() {
            if (!preciseSeeking || !preciseMovementStarted || !tracking
                    || edgeScrollDirection == 0) {
                return;
            }
            final int minimum = preciseMinimum();
            final int maximum = preciseMaximum();
            final int span = preciseWindowEnd - preciseWindowStart;
            final int maximumStart = Math.max(minimum, maximum - span);
            final int oldStart = preciseWindowStart;
            preciseWindowStart = clamp(
                    preciseWindowStart + edgeScrollDirection,
                    minimum,
                    maximumStart
            );
            preciseWindowEnd = Math.min(maximum, preciseWindowStart + span);
            if (oldStart == preciseWindowStart) {
                cancelEdgeScroll();
                return;
            }
            updatePreciseFromPosition(lastTouchX);
            handler.postDelayed(edgeScrollRunnable, 50L);
        }

        private int activeProgress() {
            final float value = activeThumb == 0 ? startValue : endValue;
            return Math.round(value * PROGRESS_STEPS);
        }

        private int preciseMinimum() {
            return activeThumb == 0
                    ? 0
                    : Math.round(startValue * PROGRESS_STEPS);
        }

        private int preciseMaximum() {
            return activeThumb == 0
                    ? Math.round(endValue * PROGRESS_STEPS)
                    : PROGRESS_STEPS;
        }

        private int preciseWindowSpan(int minimum, int maximum) {
            final int allowedSteps = maximum - minimum;
            if (allowedSteps <= 0) {
                return 0;
            }
            final float safeWidth = getWidth() * (1.0f - 2.0f * PRECISE_EDGE_GUTTER);
            final int visibleSteps = Math.max(1, (int) Math.floor(
                    safeWidth / PRECISE_TICK_SPACING
            ));
            return Math.min(allowedSteps, visibleSteps);
        }

        private int preciseTickOffsetForX(float x, int span) {
            if (span <= 0) {
                return 0;
            }
            final float offset = (x - preciseContentLeft(span)) / PRECISE_TICK_SPACING;
            return clamp(Math.round(offset), 0, span);
        }

        private int preciseProgressForTouchX(float x) {
            final int span = preciseWindowEnd - preciseWindowStart;
            if (span <= 0) {
                return activeProgress();
            }
            final float offset = (x - preciseContentLeft(span)) / PRECISE_TICK_SPACING;
            return clamp(
                    preciseWindowStart + Math.round(offset),
                    preciseMinimum(),
                    preciseMaximum()
            );
        }

        private float visualPositionForValue(float value) {
            final float normalPosition = positionForValue(value);
            if (preciseAnimationProgress <= 0.0f) {
                return normalPosition;
            }
            final float precisePosition = precisePositionForProgress(
                    Math.round(clamp(value) * PROGRESS_STEPS)
            );
            return normalPosition + (precisePosition - normalPosition) * preciseAnimationProgress;
        }

        private float precisePositionForProgress(int progress) {
            final int span = preciseWindowEnd - preciseWindowStart;
            if (span <= 0) {
                return preciseContentLeft(0);
            }
            return preciseContentLeft(span)
                    + (clamp(progress, preciseWindowStart, preciseWindowEnd)
                    - preciseWindowStart) * PRECISE_TICK_SPACING;
        }

        private float preciseContentLeft(int span) {
            final float safeWidth = getWidth() * (1.0f - 2.0f * PRECISE_EDGE_GUTTER);
            final float safeLeft = getWidth() * PRECISE_EDGE_GUTTER;
            return safeLeft + Math.max(0.0f, (safeWidth - span * PRECISE_TICK_SPACING) / 2.0f);
        }

        private void drawPreciseMarkers(Canvas canvas, float centerY) {
            if (preciseAnimationProgress <= 0.0f) {
                return;
            }
            final int span = preciseWindowEnd - preciseWindowStart;
            if (span <= 0) {
                return;
            }
            markerPaint.setColor(Color.argb(
                    Math.round(Color.alpha(accentColor) * 0.65f * preciseAnimationProgress),
                    Color.red(accentColor),
                    Color.green(accentColor),
                    Color.blue(accentColor)
            ));
            for (int marker = preciseWindowStart; marker <= preciseWindowEnd; marker++) {
                final float tickHeight = (marker & 1) == 0
                        ? PRECISE_LONG_TICK_HEIGHT
                        : PRECISE_SHORT_TICK_HEIGHT;
                final float halfHeight = tickHeight * preciseAnimationProgress / 2.0f;
                final float x = precisePositionForProgress(marker);
                canvas.drawLine(
                        x, centerY - halfHeight,
                        x, centerY + halfHeight,
                        markerPaint
                );
            }
        }

        private float pointerFractionForX(float x) {
            if (getWidth() <= 0) {
                return 0.5f;
            }
            return clamp(x / getWidth());
        }

        private void setDragging(boolean dragging) {
            final float targetScale = dragging ? DRAGGING_TRACK_SCALE : 1.0f;
            if (trackAnimator != null) {
                trackAnimator.cancel();
            }
            trackAnimator = ValueAnimator.ofFloat(trackThicknessScale, targetScale);
            trackAnimator.setDuration(dragging ? 110L : 160L);
            trackAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            trackAnimator.addUpdateListener(animation -> {
                trackThicknessScale = (float) animation.getAnimatedValue();
                invalidate();
            });
            trackAnimator.start();
        }

        private float positionForValue(float value) {
            final float usableWidth = Math.max(0.0f, getWidth() - 2.0f * thumbRadius);
            return thumbRadius + clamp(value) * usableWidth;
        }

        private float valueForPosition(float x) {
            final float usableWidth = Math.max(1.0f, getWidth() - 2.0f * thumbRadius);
            return clamp((x - thumbRadius) / usableWidth);
        }

        private static float snap(float value) {
            return Math.round(clamp(value) * PROGRESS_STEPS) / (float) PROGRESS_STEPS;
        }

        private static float clamp(float value) {
            return Math.max(0.0f, Math.min(1.0f, value));
        }

        private static int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }

        /** Callback for range changes and the start of a touch interaction. */
        public interface OnRangeChangeListener {
            void onStartTrackingTouch(@NonNull RangeSeekBar slider);

            void onRangeChanged(@NonNull RangeSeekBar slider,
                                float start,
                                float end,
                                boolean fromUser);
        }
    }
}

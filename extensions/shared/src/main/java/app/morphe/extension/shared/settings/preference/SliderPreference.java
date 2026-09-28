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
 *    modifications must retain historical authorship credit in version control
 *    systems (e.g., Git), listing original author(s) appropriately and
 *    modifiers as committers or co-authors.
 *
 * 4. User Interface Attribution (Section 7(b)): Any works containing or
 *    derived from this material must maintain a visible credit or
 *    acknowledgment to the original author(s) within the application's
 *    user interface (e.g., in an "About" or "Credits" section).
 */

package app.morphe.extension.shared.settings.preference;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Space;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;

import app.morphe.extension.shared.settings.BaseSettings;
import app.morphe.extension.shared.settings.FloatSetting;
import app.morphe.extension.shared.settings.IntegerSetting;
import app.morphe.extension.shared.settings.LongSetting;
import app.morphe.extension.shared.settings.Setting;
import app.morphe.extension.shared.ui.Dim;
import app.morphe.extension.shared.utils.Logger;

/**
 * A numeric setting rendered as an inline slider.
 *
 * <p>The slider is created directly in the preference row, so it is visible without opening a
 * dialog. Tapping the row still uses {@link ResettableEditTextPreference}'s existing text-entry
 * dialog for precise manual input.</p>
 *
 * <p>Slider limits are declared by the numeric {@link Setting} itself. This keeps the default,
 * UI range, and import validation together in the setting declaration.</p>
 */
@SuppressWarnings({"unused", "deprecation"})
public class SliderPreference extends ResettableEditTextPreference {
    private static final int MAX_PROGRESS = 1_000_000_000;
    private static final String SLIDER_CONTENT_TAG = "morphe_slider_content";
    private static final String SLIDER_WIDGET_TAG = "morphe_slider_widget";

    private record SliderViews(LinearLayout widget,
                               SeekBar slider,
                               TextView minLabel,
                               TextView maxLabel,
                               TextView valueLabel) {
    }

    /**
     * SeekBar used by inline settings sliders. The track is drawn across the complete view width,
     * while the thumb uses horizontal padding so its outer edge aligns with the setting content.
     */
    @SuppressLint("AppCompatCustomView")
    public static final class SliderSeekBar extends SeekBar {
        private static final float INACTIVE_TRACK_ALPHA = 0.30f;
        private static final float DRAGGING_TRACK_SCALE = 1.55f;
        private static final int PRECISE_TICK_SPACING = Dim.dp(24);
        private static final int PRECISE_LONG_TICK_HEIGHT = Dim.dp(12);
        private static final int PRECISE_SHORT_TICK_HEIGHT = Dim.dp(6);
        private static final float PRECISE_EDGE_GUTTER = 0.16f;
        private static final int PRECISE_EXTRA_HEIGHT = Dim.dp(12);
        private static final long PRECISE_SEEKING_DELAY = 1_000L;

        private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Handler handler = new Handler(Looper.getMainLooper());
        private ValueAnimator trackAnimator;
        private ValueAnimator preciseAnimator;
        private float trackThicknessScale = 1.0f;
        private float preciseAnimationProgress;
        private int preciseWindowStart;
        private int preciseWindowEnd;
        private int preciseAnchorProgress;
        private int preciseCurrentOffset;
        private float touchDownX;
        private float touchDownY;
        private float lastTouchX;
        private float lastTouchY;
        private float preciseTouchDownX;
        private float preciseTouchDownY;
        private float lastPointerFraction;
        private float touchSlop;
        private int activeTrackColor = Color.WHITE;
        private int inactiveTrackColor = Color.argb(77, 255, 255, 255);
        private boolean firstTickLong = true;
        private boolean preciseProgressUpdate;
        private boolean touchActive;
        private boolean touchSlopExceeded;
        private boolean preciseHoldEligible;
        private boolean preciseSeeking;
        private boolean preciseMovementStarted;
        private int edgeScrollDirection;
        private final Runnable edgeScrollRunnable = this::scrollAtEdge;
        @Nullable
        private Runnable preciseSeekingRunnable;
        @Nullable
        private OnPreciseSeekingChangedListener preciseSeekingChangedListener;
        @Nullable
        private PreciseProgressMapper preciseProgressMapper;
        @Nullable
        private OnTouchStartedListener touchStartedListener;
        @Nullable
        private OnTouchEndedListener touchEndedListener;

        @FunctionalInterface
        public interface OnPreciseSeekingChangedListener {
            void onPreciseSeekingChanged(boolean preciseSeeking);
        }

        /** Maps a precise tick offset from the current value to SeekBar progress. */
        @FunctionalInterface
        public interface PreciseProgressMapper {
            int progressFor(int anchorProgress, int tickOffset);
        }

        @FunctionalInterface
        public interface OnTouchStartedListener {
            void onTouchStarted();
        }

        @FunctionalInterface
        public interface OnTouchEndedListener {
            void onTouchEnded();
        }

        public SliderSeekBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
            initialize();
        }

        public SliderSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            initialize();
        }

        public SliderSeekBar(Context context, AttributeSet attrs) {
            super(context, attrs);
            initialize();
        }

        public SliderSeekBar(Context context) {
            super(context);
            initialize();
        }

        private void initialize() {
            final int thumbSize = Dim.dp(18);
            final GradientDrawable thumb = new GradientDrawable();
            thumb.setShape(GradientDrawable.OVAL);
            thumb.setColor(Color.WHITE);
            thumb.setSize(thumbSize, thumbSize);
            setThumb(thumb);
            setThumbOffset(thumbSize / 2);
            setPadding(thumbSize / 2, getPaddingTop(), thumbSize / 2, getPaddingBottom());

            markerPaint.setStrokeWidth(Math.max(1.0f, Dim.dp(1)));
            markerPaint.setStrokeCap(Paint.Cap.ROUND);
            touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

            // The track is rendered in onDraw so it can use the complete view width and animate
            // its thickness independently of the native thumb.
            setProgressDrawable(new ColorDrawable(Color.TRANSPARENT));
            setBackground(null);
        }

        @Override
        public void setEnabled(boolean enabled) {
            if (!enabled) {
                resetPreciseSeeking();
            }
            super.setEnabled(enabled);
            // The track is custom-drawn, so the normal SeekBar disabled tint cannot make it
            // inactive. Dimming the complete view also dims the thumb consistently.
            setAlpha(enabled ? 1.0f : 0.5f);
            invalidate();
        }

        /** Registers a listener for the long-press precise-seeking mode. */
        public void setPreciseSeekingChangedListener(
                @Nullable OnPreciseSeekingChangedListener listener) {
            preciseSeekingChangedListener = listener;
        }

        /**
         * Sets the optional value-aware mapping used by precise seeking.
         *
         * <p>Normal dragging still uses the SeekBar's regular progress range. A mapper is only
         * consulted by the expanded precise tick rail, which allows logarithmic settings to move
         * between adjacent setting values instead of repeatedly rounding to the same value.</p>
         */
        public void setPreciseProgressMapper(@Nullable PreciseProgressMapper mapper) {
            preciseProgressMapper = mapper;
        }

        /**
         * Registers a callback for the beginning of the custom touch session.
         *
         * <p>The platform {@link SeekBar} may defer {@code onStartTrackingTouch} until movement
         * begins when it is hosted inside a scrolling parent. The custom callback keeps gesture
         * bookkeeping correct for a long press that enters precise seeking before that happens.
         */
        public void setTouchStartedListener(@Nullable OnTouchStartedListener listener) {
            touchStartedListener = listener;
        }

        /** Registers a callback for the end of the custom touch session. */
        public void setTouchEndedListener(@Nullable OnTouchEndedListener listener) {
            touchEndedListener = listener;
        }

        /** Cancels any transient precise-seeking state before this recycled view is rebound. */
        public void resetPreciseSeeking() {
            requestParentDisallowIntercept(false);
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
            preciseWindowEnd = 0;
            preciseAnchorProgress = 0;
            preciseCurrentOffset = 0;
            touchActive = false;
            touchSlopExceeded = false;
            preciseHoldEligible = false;
            preciseMovementStarted = false;
            requestLayout();
            invalidate();
        }

        /** Applies the active color and a subdued version for the inactive track. */
        public void setTrackColor(int color) {
            activeTrackColor = color;
            inactiveTrackColor = Color.argb(
                    Math.round(Color.alpha(color) * INACTIVE_TRACK_ALPHA),
                    Color.red(color),
                    Color.green(color),
                    Color.blue(color)
            );
            invalidate();
        }

        /** Sets which alternating tick height is used for the setting's first progress step. */
        public void setFirstTickLong(boolean firstTickLong) {
            this.firstTickLong = firstTickLong;
            invalidate();
        }

        /** Returns whether the current progress callback came from precise seeking. */
        public boolean isPreciseProgressUpdate() {
            return preciseProgressUpdate;
        }

        /** Animates the track thickness while the user is dragging the thumb. */
        public void setDragging(boolean dragging) {
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

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            if (MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY) {
                final int desiredHeight = getMeasuredHeight()
                        + Math.round(PRECISE_EXTRA_HEIGHT * preciseAnimationProgress);
                setMeasuredDimension(
                        getMeasuredWidth(),
                        resolveSize(desiredHeight, heightMeasureSpec)
                );
            }
        }

        @SuppressLint("ClickableViewAccessibility")
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

                    touchActive = true;
                    touchDownX = event.getX();
                    touchDownY = event.getY();
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    lastPointerFraction = pointerFractionForX(lastTouchX);
                    preciseHoldEligible = isThumbHit(lastTouchX);
                    if (touchStartedListener != null) {
                        touchStartedListener.onTouchStarted();
                    }
                    if (preciseHoldEligible) {
                        requestParentDisallowIntercept(true);
                    }
                    final MotionEvent adjustedEvent;
                    if (preciseHoldEligible) {
                        adjustedEvent = MotionEvent.obtain(event);
                        adjustedEvent.setLocation(
                                normalPositionForProgress(getProgress()),
                                event.getY()
                        );
                    } else {
                        adjustedEvent = event;
                    }
                    final boolean handled = super.onTouchEvent(adjustedEvent);
                    if (adjustedEvent != event) {
                        adjustedEvent.recycle();
                    }
                    if (handled && preciseHoldEligible) {
                        schedulePreciseSeeking();
                    }
                    return handled;
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
                    if (touchSlopExceeded) {
                        requestParentDisallowIntercept(true);
                    }
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    final boolean handled;
                    if (preciseSeeking) {
                        if (!preciseMovementStarted) {
                            preciseMovementStarted = Math.hypot(
                                    event.getX() - preciseTouchDownX,
                                    event.getY() - preciseTouchDownY
                            ) > touchSlop;
                        }
                        if (!preciseMovementStarted) {
                            // Entering precise mode is visual-only until the finger moves again.
                            // This prevents a drag that ends at the edge from continuing to seek
                            // because of normal touch jitter or edge auto-scrolling.
                            return true;
                        }
                        updatePreciseWindow(lastTouchX);
                        handled = dispatchPreciseTouchEvent(event);
                    } else {
                        // A long press is only valid while the finger is stationary. Re-arm the
                        // timer after every movement so a drag can enter precise mode only after
                        // it has actually stopped at a new value.
                        if (preciseHoldEligible && movement > 0.5f) {
                            schedulePreciseSeeking();
                        }
                        if (preciseHoldEligible && !touchSlopExceeded) {
                            // The native SeekBar maps ACTION_DOWN/MOVE directly to progress. Do
                            // not let finger jitter while waiting for the long-press timeout
                            // change the setting by one step.
                            return true;
                        }
                        handled = super.onTouchEvent(event);
                    }
                    return handled;
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
                    final boolean handled;
                    if (preciseSeeking) {
                        if (preciseMovementStarted) {
                            updatePreciseWindow(lastTouchX);
                        }
                        handled = dispatchPreciseTouchEvent(event, preciseMovementStarted);
                    } else if (preciseHoldEligible && !touchSlopExceeded) {
                        final MotionEvent adjustedEvent = MotionEvent.obtain(event);
                        adjustedEvent.setLocation(
                                normalPositionForProgress(getProgress()),
                                event.getY()
                        );
                        handled = super.onTouchEvent(adjustedEvent);
                        adjustedEvent.recycle();
                    } else {
                        handled = super.onTouchEvent(event);
                    }

                    finishTouchInteraction();
                    return handled;
                }
                default -> {
                    // Finalize an interrupted stream without sending the terminal event to
                    // SeekBar. The current value was already persisted by onProgressChanged.
                    finishTouchInteraction();
                    return true;
                }
            }
        }

        @Override
        protected void onDetachedFromWindow() {
            requestParentDisallowIntercept(false);
            resetPreciseSeeking();
            super.onDetachedFromWindow();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            final float thickness = Dim.dp(4) * trackThicknessScale
                    * (1.0f + 0.12f * preciseAnimationProgress);
            final float centerY = getHeight() / 2.0f;
            final float halfThickness = thickness / 2.0f;
            final float right = getWidth();
            final float thumbX = visualPositionForProgress(getProgress());

            drawPreciseMarkers(canvas, centerY);

            trackPaint.setColor(inactiveTrackColor);
            canvas.drawRoundRect(
                    0,
                    centerY - halfThickness,
                    right,
                    centerY + halfThickness,
                    halfThickness,
                    halfThickness,
                    trackPaint
            );

            trackPaint.setColor(activeTrackColor);
            canvas.drawRoundRect(
                    0,
                    centerY - halfThickness,
                    thumbX,
                    centerY + halfThickness,
                    halfThickness,
                    halfThickness,
                    trackPaint
            );

            final float normalThumbX = normalPositionForProgress(getProgress());
            canvas.save();
            canvas.translate(thumbX - normalThumbX, 0.0f);
            super.onDraw(canvas);
            canvas.restore();
        }

        private void schedulePreciseSeeking() {
            if (!touchActive || !preciseHoldEligible || preciseSeeking) {
                return;
            }
            cancelPreciseSeekingRunnable();
            preciseSeekingRunnable = () -> {
                if (touchActive && isEnabled() && !preciseSeeking) {
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

        private void notifyTouchEnded() {
            if (touchEndedListener != null) {
                touchEndedListener.onTouchEnded();
            }
        }

        /** Finishes either a normal release or a framework-interrupted touch stream. */
        private void finishTouchInteraction() {
            if (!touchActive) {
                return;
            }
            touchActive = false;
            preciseHoldEligible = false;
            requestParentDisallowIntercept(false);
            cancelPreciseSeekingRunnable();
            if (preciseSeeking) {
                finishPreciseSeeking();
            }
            notifyTouchEnded();
        }

        /** Keeps a thumb gesture inside this view while the finger moves vertically. */
        private void requestParentDisallowIntercept(boolean disallow) {
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(disallow);
            }
        }

        private void startPreciseSeeking() {
            final int maximum = getMax();
            if (maximum <= 0) {
                return;
            }

            final int span = preciseWindowSpan();
            final int currentProgress = getProgress();
            final int pointerOffset = preciseTickOffsetForX(lastTouchX, span);
            // Precise windows use logical tick offsets around the current value. This lets a
            // setting provide a value-aware progress mapping without changing normal dragging.
            preciseAnchorProgress = currentProgress;
            preciseCurrentOffset = 0;
            preciseWindowStart = -pointerOffset;
            preciseWindowEnd = preciseWindowStart + span;
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
                    preciseWindowEnd = 0;
                    preciseAnchorProgress = 0;
                    preciseCurrentOffset = 0;
                }
            });
            preciseAnimator.start();
        }

        private boolean dispatchPreciseTouchEvent(MotionEvent event) {
            return dispatchPreciseTouchEvent(event, true);
        }

        private boolean dispatchPreciseTouchEvent(MotionEvent event, boolean updateProgress) {
            if (updateProgress) {
                final int preciseProgress = preciseProgressForTouchX(event.getX());
                setPreciseProgress(preciseProgress);
            }
            if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                return true;
            }

            // Precise seeking already applied the value above. Sending ACTION_UP through the
            // native SeekBar would convert the rounded pixel position back into progress and can
            // change the selected value by one step on release. Cancel the native touch session
            // so it performs its tracking cleanup without recalculating progress.
            final MotionEvent adjustedEvent = MotionEvent.obtain(event);
            adjustedEvent.setAction(MotionEvent.ACTION_CANCEL);
            final boolean handled = super.onTouchEvent(adjustedEvent);
            adjustedEvent.recycle();
            return handled;
        }

        private void setPreciseProgress(int progress) {
            if (getProgress() == progress) {
                return;
            }
            preciseProgressUpdate = true;
            try {
                setProgress(progress);
            } finally {
                preciseProgressUpdate = false;
            }
        }

        private void updatePreciseWindow(float x) {
            if (getMax() <= 0 || preciseWindowEnd <= preciseWindowStart) {
                lastPointerFraction = pointerFractionForX(x);
                return;
            }

            final float pointerFraction = pointerFractionForX(x);
            lastPointerFraction = pointerFraction;
            updateEdgeScrollState(pointerFraction);
        }

        private void updateEdgeScrollState(float pointerFraction) {
            int nextDirection = 0;
            if (pointerFraction >= 1.0f - PRECISE_EDGE_GUTTER
                    && canScrollPreciseWindow(1)) {
                nextDirection = 1;
            } else if (pointerFraction <= PRECISE_EDGE_GUTTER
                    && canScrollPreciseWindow(-1)) {
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

        /** Continues panning while the finger rests in the safe edge gutter. */
        private void scrollAtEdge() {
            if (!preciseSeeking || !preciseMovementStarted || !touchActive
                    || edgeScrollDirection == 0) {
                return;
            }
            if (!canScrollPreciseWindow(edgeScrollDirection)) {
                cancelEdgeScroll();
                return;
            }

            final int oldStart = preciseWindowStart;
            preciseWindowStart += edgeScrollDirection;
            preciseWindowEnd += edgeScrollDirection;
            if (oldStart == preciseWindowStart) {
                cancelEdgeScroll();
                return;
            }

            final long now = SystemClock.uptimeMillis();
            final MotionEvent edgeEvent = MotionEvent.obtain(
                    now, now, MotionEvent.ACTION_MOVE, lastTouchX, lastTouchY, 0
            );
            dispatchPreciseTouchEvent(edgeEvent);
            edgeEvent.recycle();
            if (canScrollPreciseWindow(edgeScrollDirection)) {
                handler.postDelayed(edgeScrollRunnable, 50L);
            } else {
                cancelEdgeScroll();
            }
        }

        private void cancelEdgeScroll() {
            handler.removeCallbacks(edgeScrollRunnable);
            edgeScrollDirection = 0;
        }

        private int preciseProgressForTouchX(float x) {
            final int span = preciseWindowEnd - preciseWindowStart;
            if (span <= 0) {
                preciseCurrentOffset = preciseWindowStart;
                return preciseProgressAtOffset(preciseWindowStart);
            }
            final int tickOffset = preciseTickOffsetForX(x, span);
            preciseCurrentOffset = preciseWindowStart + tickOffset;
            return preciseProgressAtOffset(preciseCurrentOffset);
        }

        private int preciseTickOffsetForX(float x, int span) {
            final float offset = (x - preciseContentLeft(span)) / PRECISE_TICK_SPACING;
            return clamp(Math.round(offset), span);
        }

        private boolean isThumbHit(float x) {
            final float thumbRadius = Dim.dp(9);
            return Math.abs(x - normalPositionForProgress(getProgress()))
                    <= thumbRadius + touchSlop;
        }

        private float pointerFractionForX(float x) {
            final float left = getPaddingLeft();
            final float right = getWidth() - getPaddingRight();
            if (right <= left) {
                return 0.5f;
            }
            return clamp((x - left) / (right - left));
        }

        private float normalPositionForProgress(float progress) {
            return positionAtFraction(getMax() > 0 ? progress / getMax() : 0.0f);
        }

        private float precisePositionForProgress(float progress) {
            final int span = preciseWindowEnd - preciseWindowStart;
            if (span <= 0) {
                return positionAtFraction(0.5f);
            }
            return precisePositionForOffset(preciseCurrentOffset, span);
        }

        private float precisePositionForOffset(int offset, int span) {
            return preciseContentLeft(span)
                    + clamp(offset - preciseWindowStart, span) * PRECISE_TICK_SPACING;
        }

        /**
         * Calculates the fixed-pitch tick rail. The rail is centered inside the edge gutters when
         * the setting has fewer steps than fit on screen, and otherwise fills the safe viewport.
         */
        private float preciseContentLeft(int span) {
            final float trackLeft = getPaddingLeft();
            final float trackWidth = Math.max(
                    0.0f,
                    getWidth() - getPaddingLeft() - getPaddingRight()
            );
            final float safeWidth = trackWidth * (1.0f - 2.0f * PRECISE_EDGE_GUTTER);
            final float safeLeft = trackLeft + trackWidth * PRECISE_EDGE_GUTTER;
            return safeLeft + Math.max(0.0f, (safeWidth - span * PRECISE_TICK_SPACING) / 2.0f);
        }

        private int preciseWindowSpan() {
            final int maximum = getMax();
            if (maximum <= 0) {
                return 0;
            }

            final float trackWidth = Math.max(
                    0.0f,
                    getWidth() - getPaddingLeft() - getPaddingRight()
            );
            final float safeWidth = trackWidth * (1.0f - 2.0f * PRECISE_EDGE_GUTTER);
            final int visibleSteps = Math.max(1, (int) Math.floor(
                    safeWidth / PRECISE_TICK_SPACING
            ));
            return Math.min(maximum, visibleSteps);
        }

        private float visualPositionForProgress(float progress) {
            final float normalPosition = normalPositionForProgress(progress);
            if (preciseAnimationProgress <= 0.0f) {
                return normalPosition;
            }
            final float precisePosition = precisePositionForProgress(progress);
            return normalPosition + (precisePosition - normalPosition) * preciseAnimationProgress;
        }

        private float positionAtFraction(float fraction) {
            final float left = getPaddingLeft();
            final float width = Math.max(0.0f, getWidth() - getPaddingLeft() - getPaddingRight());
            return left + clamp(fraction) * width;
        }

        private void drawPreciseMarkers(Canvas canvas, float centerY) {
            if (preciseAnimationProgress <= 0.0f) {
                return;
            }

            final int span = preciseWindowEnd - preciseWindowStart;
            if (getMax() <= 0 || span <= 0) {
                return;
            }

            markerPaint.setColor(Color.argb(
                    Math.round(Color.alpha(activeTrackColor) * 0.65f * preciseAnimationProgress),
                    Color.red(activeTrackColor),
                    Color.green(activeTrackColor),
                    Color.blue(activeTrackColor)
            ));
            for (int marker = preciseWindowStart; marker <= preciseWindowEnd; marker++) {
                final boolean longTick = (((marker - preciseWindowStart) & 1) == 0)
                        == firstTickLong;
                final float tickHeight = longTick
                        ? PRECISE_LONG_TICK_HEIGHT
                        : PRECISE_SHORT_TICK_HEIGHT;
                final float halfHeight = tickHeight * preciseAnimationProgress / 2.0f;
                final float x = precisePositionForOffset(marker, span);
                canvas.drawLine(
                        x, centerY - halfHeight,
                        x, centerY + halfHeight,
                        markerPaint
                );
            }
        }

        private boolean canScrollPreciseWindow(int direction) {
            final int edgeOffset = direction > 0 ? preciseWindowEnd : preciseWindowStart;
            return preciseProgressAtOffset(edgeOffset + direction)
                    != preciseProgressAtOffset(edgeOffset);
        }

        private int preciseProgressAtOffset(int offset) {
            if (preciseProgressMapper != null) {
                return Math.max(
                        0,
                        Math.min(
                                getMax(),
                                preciseProgressMapper.progressFor(preciseAnchorProgress, offset)
                        )
                );
            }

            final long progress = (long) preciseAnchorProgress + offset;
            return (int) Math.max(0L, Math.min(getMax(), progress));
        }

        private static int clamp(int value, int max) {
            return Math.max(0, Math.min(max, value));
        }

        private static float clamp(float value) {
            return Math.max((float) 0.0, Math.min((float) 1.0, value));
        }
    }

    public SliderPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public SliderPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public SliderPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SliderPreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        setSelectable(true);
    }

    /**
     * Returns whether summaries should be visible below slider titles.
     *
     * <p>The setting is shared so the normal settings screen and settings search use the same
     * display preference.</p>
     */
    public static boolean areSummariesVisible() {
        return BaseSettings.SHOW_SLIDER_SUMMARIES.get();
    }

    /** Requests a rebind after the global slider-summary visibility setting changes. */
    public void refreshSummaryVisibility() {
        notifyChanged();
    }

    /**
     * Opens the existing manual-entry dialog when the preference row is tapped.
     *
     * <p>The slider itself is inline, so the row must keep the text-entry behavior that an
     * ordinary {@link ResettableEditTextPreference} provides.</p>
     */
    @Override
    protected void onClick() {
        showDialog(null);
    }

    @Override
    protected void showDialog(Bundle state) {
        final Setting<?> setting = getSliderSetting();
        if (getSliderConfig(setting) != null) {
            configureNumericInput(setting);
            // ResettableEditTextPreference uses getText() to seed its dialog. Keep that cached
            // value synchronized with slider changes before opening the existing dialog.
            super.setText(String.valueOf(setting.get()));
        }
        super.showDialog(state);
    }

    @Override
    public void setText(String text) {
        final Setting<?> setting = getSliderSetting();
        final Setting.SliderConfig config = getSliderConfig(setting);
        if (config != null) {
            final Number parsedValue = parseValue(setting, text);
            if (!config.contains(parsedValue)) {
                setting.resetToDefault();
                text = String.valueOf(setting.defaultValue);
            } else {
                saveValue(setting, parsedValue);
                text = String.valueOf(parsedValue);
            }
        }

        super.setText(text);
        notifyChanged();
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        final Setting<?> setting = getSliderSetting();
        final Setting.SliderConfig config = getSliderConfig(setting);
        if (config == null) {
            Logger.printException(() -> "Missing slider configuration for setting: " + getKey());
            return;
        }

        configureNumericInput(setting);

        final View widgetFrame = view.findViewById(android.R.id.widget_frame);
        if (widgetFrame != null) {
            // The default Preference layout puts this frame on the right. The slider belongs
            // below the title, so remove the right-side widget area entirely.
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

        final ViewGroup sliderContent = prepareSliderContent(
                getContext(), titleView, summaryView, areSummariesVisible());
        if (sliderContent == null) {
            Logger.printException(() -> "Missing preference content view for setting: " + getKey());
            return;
        }

        final SliderViews sliderViews = createSliderViews(
                getContext(),
                config,
                (Number) setting.get()
        );
        final View oldSliderWidget = sliderContent.findViewWithTag(SLIDER_WIDGET_TAG);
        if (oldSliderWidget != null) {
            sliderContent.removeView(oldSliderWidget);
        }
        sliderContent.addView(sliderViews.widget(), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        final SeekBar slider = sliderViews.slider();
        final TextView minLabel = sliderViews.minLabel();
        final TextView maxLabel = sliderViews.maxLabel();
        final TextView valueLabel = sliderViews.valueLabel();

        bindSliderControls(setting, config, slider, minLabel, maxLabel, valueLabel);
    }

    /**
     * Binds an inline slider in another settings surface, such as the settings search results.
     * The preference remains the single owner of value conversion, persistence, and dialogs.
     */
    public void bindInlineSlider(@NonNull SeekBar slider,
                                 @NonNull TextView minLabel,
                                 @NonNull TextView maxLabel,
                                 @NonNull TextView valueLabel) {
        final Setting<?> setting = getSliderSetting();
        final Setting.SliderConfig config = getSliderConfig(setting);
        if (config == null) {
            Logger.printException(() -> "Missing slider configuration for setting: " + getKey());
            return;
        }

        configureNumericInput(setting);
        bindSliderControls(setting, config, slider, minLabel, maxLabel, valueLabel);
    }

    private void bindSliderControls(Setting<?> setting,
                                    Setting.SliderConfig config,
                                    SeekBar slider,
                                    TextView minLabel,
                                    TextView maxLabel,
                                    TextView valueLabel) {
        final int accentColor = resolveAccentColor();
        slider.setProgressTintList(ColorStateList.valueOf(accentColor));
        slider.setThumbTintList(ColorStateList.valueOf(accentColor));
        if (slider instanceof SliderSeekBar sliderView) {
            // Search rows recycle their slider views. Clear the previous touch animation before
            // installing the labels belonging to the new preference item.
            sliderView.setPreciseSeekingChangedListener(null);
            sliderView.setPreciseProgressMapper(null);
            sliderView.setTouchStartedListener(null);
            sliderView.setTouchEndedListener(null);
            sliderView.resetPreciseSeeking();
            sliderView.setTrackColor(accentColor);
            sliderView.setFirstTickLong(firstPreciseTickIsLong(config));
        }
        minLabel.animate().cancel();
        maxLabel.animate().cancel();
        minLabel.setAlpha(1.0f);
        maxLabel.setAlpha(1.0f);
        expandSliderTrack(slider);
        slider.setMax(progressMax(config));
        slider.setOnSeekBarChangeListener(null);

        final Number currentValue = (Number) setting.get();
        slider.setProgress(progressFor(config, currentValue));
        updateLabels(minLabel, maxLabel, valueLabel, config, currentValue);
        final boolean enabled = isEnabled();
        slider.setEnabled(enabled);
        minLabel.setEnabled(enabled);
        maxLabel.setEnabled(enabled);
        valueLabel.setEnabled(enabled);
        valueLabel.setOnClickListener(enabled ? ignored -> showDialog(null) : null);
        if (slider instanceof SliderSeekBar sliderView) {
            if (config.logarithmic()) {
                sliderView.setPreciseProgressMapper((anchorProgress, tickOffset) ->
                        preciseProgressFor(config, anchorProgress, tickOffset));
            }
            sliderView.setPreciseSeekingChangedListener(preciseSeeking ->
                    animateEndpointLabels(minLabel, maxLabel, preciseSeeking));
        }
        final Number[] initialValue = { null };
        final boolean[] changed = { false };
        final boolean[] interactionFinished = { false };
        final boolean[] interactionStarted = { false };
        final Runnable initializeInteraction = () -> {
            initialValue[0] = (Number) setting.get();
            changed[0] = false;
            interactionFinished[0] = false;
            interactionStarted[0] = true;
        };
        if (slider instanceof SliderSeekBar sliderView) {
            // A long press can enter precise seeking before the platform SeekBar reports its
            // tracking start. Capture the baseline at ACTION_DOWN so release can still detect a
            // changed value and show the restart dialog.
            sliderView.setTouchStartedListener(initializeInteraction::run);
        }
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                final Number value = valueFromProgress(setting, config, progress);
                updateLabels(minLabel, maxLabel, valueLabel, config, value);
                final boolean userChange = fromUser
                        || (bar instanceof SliderSeekBar sliderView
                        && sliderView.isPreciseProgressUpdate());
                if (userChange) {
                    changed[0] = initialValue[0] != null
                            && !initialValue[0].equals(value);
                    // Persist each step while the interaction guard prevents the shared
                    // preference listener from rebinding the slider or showing a dialog.
                    AbstractPreferenceFragment.setSliderInteractionInProgress(true);
                    try {
                        saveValue(setting, value);
                    } finally {
                        AbstractPreferenceFragment.setSliderInteractionInProgress(false);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
                if (!interactionStarted[0]) {
                    initializeInteraction.run();
                }
                if (bar instanceof SliderSeekBar sliderView) {
                    sliderView.setDragging(true);
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
                if (bar instanceof SliderSeekBar sliderView) {
                    sliderView.setDragging(false);
                    return;
                }
                AbstractPreferenceFragment.setSliderInteractionInProgress(false);
                if (changed[0] && setting.rebootApp) {
                    AbstractPreferenceFragment.showRestartDialog(getContext());
                }
                notifyChanged();
                interactionStarted[0] = false;
            }
        });
        if (slider instanceof SliderSeekBar sliderView) {
            sliderView.setTouchEndedListener(() -> {
                if (interactionFinished[0]) {
                    return;
                }
                interactionFinished[0] = true;
                AbstractPreferenceFragment.setSliderInteractionInProgress(false);
                if (changed[0] && setting.rebootApp) {
                    AbstractPreferenceFragment.showRestartDialog(getContext());
                }
                notifyChanged();
                interactionStarted[0] = false;
            });
        }
    }

    /** Fades endpoint values while precise seeking exposes the larger tick-mark strip. */
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

    private static boolean firstPreciseTickIsLong(Setting.SliderConfig config) {
        final boolean unitStep = Math.abs(config.step() - 1.0) < 0.000001;
        final double roundedMinimum = Math.rint(config.min());
        final boolean integerMinimum = Math.abs(config.min() - roundedMinimum) < 0.000001;
        return !unitStep || !integerMinimum || ((long) roundedMinimum & 1L) == 0L;
    }

    /**
     * Maps precise ticks to adjacent values for logarithmic settings.
     *
     * <p>The normal slider deliberately uses a logarithmic progress mapping so a large range stays
     * usable. Its fixed progress ticks are too fine at the low end, however, where several million
     * progress units can round to the same configured value. Precise seeking follows the setting's
     * declared step instead and falls back to the nearest representable progress unit when the
     * configured step is smaller than the SeekBar's progress resolution.</p>
     */
    private static int preciseProgressFor(Setting.SliderConfig config,
                                          int anchorProgress,
                                          int tickOffset) {
        final int maximumProgress = progressMax(config);
        if (tickOffset == 0) {
            return Math.max(0, Math.min(maximumProgress, anchorProgress));
        }

        final double anchorValue = valueAtProgress(config, anchorProgress);
        final double targetValue = Math.max(
                config.min(),
                Math.min(config.max(), anchorValue + tickOffset * config.step())
        );
        final int mappedProgress = progressFor(config, targetValue);

        // Preserve the exact range endpoints. This also prevents edge scrolling through a long
        // run of progress positions that all represent the same minimum or maximum value.
        if (targetValue <= config.min() || targetValue >= config.max()) {
            return mappedProgress;
        }

        // At the high end of a large logarithmic range, multiple adjacent configured values can
        // share one progress unit. Keep precise seeking moving in the intended direction in that
        // case, while still using the logarithmic mapping whenever it has enough resolution.
        final long linearProgress = (long) anchorProgress + tickOffset;
        final int nearestProgress = (int) Math.max(
                0L,
                Math.min(maximumProgress, linearProgress)
        );
        return tickOffset < 0
                ? Math.min(mappedProgress, nearestProgress)
                : Math.max(mappedProgress, nearestProgress);
    }

    /**
     * Moves the title and summary into a vertical container so the slider can be placed directly
     * below the setting text instead of inside the default right-side widget frame. The original
     * row orientation and layout parameters are retained so an icon column stays beside this
     * content and naturally limits the slider width.
     */
    @Nullable
    static ViewGroup prepareSliderContent(Context context,
                                          View titleView,
                                          @Nullable View summaryView,
                                          boolean showSummary) {
        if (!(titleView.getParent() instanceof ViewGroup titleContainer)) {
            return null;
        }

        if (SLIDER_CONTENT_TAG.equals(titleContainer.getTag())) {
            if (summaryView != null) {
                summaryView.setVisibility(showSummary ? View.VISIBLE : View.GONE);
            }
            return titleContainer;
        }

        if (!(titleContainer.getParent() instanceof ViewGroup parent)) {
            return null;
        }

        final int childIndex = parent.indexOfChild(titleContainer);
        final ViewGroup.LayoutParams layoutParams = titleContainer.getLayoutParams();
        final LinearLayout content = getLinearLayout(context, titleContainer);

        while (titleContainer.getChildCount() > 0) {
            final View child = titleContainer.getChildAt(0);
            titleContainer.removeViewAt(0);
            if (child == summaryView) {
                child.setVisibility(showSummary ? View.VISIBLE : View.GONE);
            }
            content.addView(child, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
        }

        parent.removeView(titleContainer);
        parent.addView(content, childIndex, layoutParams);
        return content;
    }

    @NonNull
    private static LinearLayout getLinearLayout(Context context, ViewGroup titleContainer) {
        final LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_VERTICAL);
        content.setClipChildren(false);
        content.setClipToPadding(false);
        content.setPadding(
                titleContainer.getPaddingLeft(),
                titleContainer.getPaddingTop(),
                titleContainer.getPaddingRight(),
                Math.min(titleContainer.getPaddingBottom(), Dim.dp4)
        );
        content.setTag(SLIDER_CONTENT_TAG);
        if (titleContainer.getId() != View.NO_ID) {
            content.setId(titleContainer.getId());
        }
        return content;
    }

    /**
     * Builds the inline widget programmatically. This avoids depending on target-app resource IDs
     * for injected child views and lets the widget use the full measured width of the title
     * container without extending beyond the preference row.
     */
    private static SliderViews createSliderViews(Context context,
                                                  Setting.SliderConfig config,
                                                  Number currentValue) {
        final LinearLayout widget = new LinearLayout(context);
        widget.setOrientation(LinearLayout.VERTICAL);
        widget.setGravity(Gravity.CENTER_VERTICAL);
        widget.setPadding(0, Dim.dp4, 0, 0);
        widget.setClipChildren(false);
        widget.setClipToPadding(false);
        widget.setTag(SLIDER_WIDGET_TAG);

        final LinearLayout endpointLabels = new LinearLayout(context);
        endpointLabels.setGravity(Gravity.CENTER_VERTICAL);

        final TextView minLabel = createLabel(context);
        minLabel.setText(formatValue(config.min(), config));
        endpointLabels.addView(minLabel);

        final Space spacer = new Space(context);
        endpointLabels.addView(spacer, new LinearLayout.LayoutParams(0, Dim.dp(1), 1f));

        final TextView maxLabel = createLabel(context);
        maxLabel.setText(formatValue(config.max(), config));
        maxLabel.setGravity(Gravity.END);
        endpointLabels.addView(maxLabel);

        widget.addView(endpointLabels, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        // Keep the theme-provided horizontal padding so the thumb remains fully visible at both
        // endpoints. Extend only the SeekBar view by that padding so its track reaches the same
        // left and right edges as the endpoint labels without clipping the thumb.
        final SeekBar slider = new SliderSeekBar(context);
        final LinearLayout.LayoutParams sliderParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        widget.addView(slider, sliderParams);

        final TextView valueLabel = createLabel(context);
        valueLabel.setGravity(Gravity.CENTER);
        valueLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        widget.addView(valueLabel, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        valueLabel.setText(formatValue(currentValue.doubleValue(), config));
        return new SliderViews(widget, slider, minLabel, maxLabel, valueLabel);
    }

    /**
     * Compensates for the horizontal inset Android themes reserve for a SeekBar thumb. The view
     * keeps that inset for safe thumb drawing, while its negative margins make the actual track
     * line up with the full width of the surrounding slider content.
     */
    private static void expandSliderTrack(@NonNull SeekBar slider) {
        if (slider.getParent() instanceof ViewGroup parent) {
            parent.setClipChildren(false);
            parent.setClipToPadding(false);
        }

        if (slider.getLayoutParams() instanceof LinearLayout.LayoutParams params) {
            if (slider instanceof SliderSeekBar) {
                params.leftMargin = 0;
                params.rightMargin = 0;
            } else {
                params.leftMargin = -slider.getPaddingLeft();
                params.rightMargin = -slider.getPaddingRight();
            }
            slider.setLayoutParams(params);
        }
    }

    private static TextView createLabel(Context context) {
        final TextView label = new TextView(context);
        label.setSingleLine(true);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        return label;
    }

    @Nullable
    private Setting<?> getSliderSetting() {
        final String key = getKey();
        return key == null ? null : Setting.getSettingFromPath(key);
    }

    @Nullable
    private static Setting.SliderConfig getSliderConfig(@Nullable Setting<?> setting) {
        if (setting == null || setting.sliderConfig == null || !(setting.get() instanceof Number)) {
            return null;
        }
        return setting.sliderConfig;
    }

    private void configureNumericInput(Setting<?> setting) {
        final boolean decimal = setting instanceof FloatSetting;
        final int inputType = InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_FLAG_SIGNED
                | (decimal ? InputType.TYPE_NUMBER_FLAG_DECIMAL : 0);
        getEditText().setInputType(inputType);
    }

    private int resolveAccentColor() {
        final TypedValue typedValue = new TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.colorAccent, typedValue, true);
        return typedValue.data;
    }

    private static void updateLabels(@Nullable TextView minLabel,
                                     @Nullable TextView maxLabel,
                                     @Nullable TextView valueLabel,
                                     Setting.SliderConfig config,
                                     Number value) {
        if (minLabel != null) {
            minLabel.setText(formatValue(config.min(), config));
        }
        if (maxLabel != null) {
            maxLabel.setText(formatValue(config.max(), config));
        }
        if (valueLabel != null) {
            valueLabel.setText(formatValue(value.doubleValue(), config));
        }
    }

    private static int progressMax(Setting.SliderConfig config) {
        final double stepCount = (config.max() - config.min()) / config.step();
        return (int) Math.max(1, Math.min(MAX_PROGRESS, Math.round(stepCount)));
    }

    private static double valueAtProgress(Setting.SliderConfig config, int progress) {
        final int maximumProgress = progressMax(config);
        final double fraction = Math.max(0, Math.min(maximumProgress, progress))
                / (double) maximumProgress;
        final double rawValue;
        if (!config.logarithmic()) {
            rawValue = config.min() + fraction * (config.max() - config.min());
        } else if (config.min() == 0) {
            final double logarithmicScale = logarithmicScale(config);
            rawValue = logarithmicScale * Math.expm1(
                    fraction * Math.log1p(config.max() / logarithmicScale)
            );
        } else {
            rawValue = Math.exp(Math.log(config.min())
                    + fraction * (Math.log(config.max()) - Math.log(config.min())));
        }

        final double snappedValue = config.min()
                + Math.rint((rawValue - config.min()) / config.step()) * config.step();
        return Math.max(config.min(), Math.min(config.max(), snappedValue));
    }

    private static int progressFor(Setting.SliderConfig config, Number number) {
        final double value = number.doubleValue();
        final double fraction;
        double v = Math.max(config.min(), Math.min(config.max(), value));
        if (!config.logarithmic()) {
            fraction = (v - config.min())
                    / (config.max() - config.min());
        } else if (config.min() == 0) {
            final double logarithmicScale = logarithmicScale(config);
            fraction = Math.log1p(
                    Math.max(0, Math.min(config.max(), value)) / logarithmicScale
            ) / Math.log1p(config.max() / logarithmicScale);
        } else {
            fraction = (Math.log(v) - Math.log(config.min()))
                    / (Math.log(config.max()) - Math.log(config.min()));
        }
        return (int) Math.round(Math.max(0, Math.min(1, fraction)) * progressMax(config));
    }

    /**
     * Uses the declared step as the first useful logarithmic interval.
     *
     * <p>Without this scale, a range from zero to a very large maximum spends a disproportionate
     * amount of track length below the first configured step. For example, a 0..10^12 range puts
     * 1,000 at 25% of the track with {@code log1p(value)}. Scaling by the 1,000-step puts that
     * transition near the start while retaining logarithmic control over the rest of the range.</p>
     */
    private static double logarithmicScale(Setting.SliderConfig config) {
        return config.step();
    }

    private static Number valueFromProgress(Setting<?> setting,
                                             Setting.SliderConfig config,
                                             int progress) {
        final double value = valueAtProgress(config, progress);
        if (setting instanceof IntegerSetting) {
            return (int) Math.round(value);
        }
        if (setting instanceof LongSetting) {
            return Math.round(value);
        }
        return (float) value;
    }

    private static void saveValue(Setting<?> setting, Number value) {
        if (setting instanceof IntegerSetting integerSetting) {
            integerSetting.save(value.intValue());
        } else if (setting instanceof LongSetting longSetting) {
            longSetting.save(value.longValue());
        } else if (setting instanceof FloatSetting floatSetting) {
            floatSetting.save(value.floatValue());
        }
    }

    @Nullable
    private static Number parseValue(Setting<?> setting, String text) {
        if (text == null) {
            return null;
        }

        try {
            final String trimmedText = text.trim();
            if (setting instanceof IntegerSetting) {
                return Integer.valueOf(trimmedText);
            }
            if (setting instanceof LongSetting) {
                return Long.valueOf(trimmedText);
            }
            if (setting instanceof FloatSetting) {
                final float value = Float.parseFloat(trimmedText);
                return Float.isFinite(value) ? value : null;
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    private static String formatValue(double value, Setting.SliderConfig config) {
        final int decimals = displayDecimals(config.step());
        return BigDecimal.valueOf(value)
                .setScale(decimals, RoundingMode.HALF_UP)
                .toPlainString() + config.unit();
    }

    private static int displayDecimals(double step) {
        int decimals = 0;
        double scaledStep = step;
        while (decimals < 6 && Math.abs(scaledStep - Math.rint(scaledStep)) > 0.000001) {
            scaledStep *= 10;
            decimals++;
        }
        return decimals;
    }
}

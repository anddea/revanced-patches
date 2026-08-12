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

package app.morphe.extension.youtube.settings.preference;

import static app.morphe.extension.shared.utils.BaseThemeUtils.adjustColorBrightness;
import static app.morphe.extension.shared.utils.BaseThemeUtils.getAppBackgroundColor;
import static app.morphe.extension.shared.utils.BaseThemeUtils.getAppForegroundColor;
import static app.morphe.extension.shared.utils.BaseThemeUtils.isDarkModeEnabled;
import static app.morphe.extension.shared.utils.StringRef.str;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import app.morphe.extension.shared.settings.Setting;
import app.morphe.extension.shared.ui.Dim;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.settings.Settings;

/**
 * Draws a live map of the vertical or horizontal swipe zones in the settings screen.
 *
 * <p>The preference key selects the direction. Vertical previews show brightness, default, and
 * volume from left to right; horizontal previews show seek, default, and speed from top to bottom.
 * The preview follows the corresponding area slider, enabled controls, colors, and speed/seek
 * switch setting.</p>
 */
@SuppressWarnings({"unused", "deprecation"})
public final class SwipeZonePreviewPreference extends Preference {

    private static final String HORIZONTAL_PREVIEW_KEY = "revanced_swipe_horizontal_zone_preview";

    private ZoneView zoneView;

    private final SharedPreferences.OnSharedPreferenceChangeListener listener =
            (sharedPreferences, key) -> Utils.runOnMainThread(() -> {
                if (zoneView != null) {
                    zoneView.postInvalidate();
                }
            });

    public SwipeZonePreviewPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public SwipeZonePreviewPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public SwipeZonePreviewPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SwipeZonePreviewPreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        setSelectable(false);
        setPersistent(false);
    }

    private boolean isHorizontalPreview() {
        return HORIZONTAL_PREVIEW_KEY.equals(getKey());
    }

    @SuppressLint("MissingSuperCall")
    @Override
    protected View onCreateView(ViewGroup parent) {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(Dim.dp16, Dim.dp8, Dim.dp16, Dim.dp8);

        zoneView = new ZoneView(getContext(), isHorizontalPreview());
        layout.addView(zoneView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Dim.dp(132)));
        return layout;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        // PreferenceGroupAdapter may recycle the view created for the vertical preview for the
        // horizontal preview because both preferences use this class. Rebuild the child canvas
        // when the recycled view has the other orientation.
        if (view instanceof LinearLayout layout) {
            final boolean horizontal = isHorizontalPreview();
            ZoneView currentZoneView = null;
            if (layout.getChildCount() == 1 && layout.getChildAt(0) instanceof ZoneView candidate
                    && candidate.horizontal == horizontal) {
                currentZoneView = candidate;
            }

            if (currentZoneView == null) {
                layout.removeAllViews();
                currentZoneView = new ZoneView(getContext(), horizontal);
                layout.addView(currentZoneView, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, Dim.dp(132)));
            }

            zoneView = currentZoneView;
            zoneView.invalidate();
        }
    }

    @Override
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        super.onAttachedToHierarchy(preferenceManager);
        Setting.preferences.preferences.registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    protected void onPrepareForRemoval() {
        super.onPrepareForRemoval();
        Setting.preferences.preferences.unregisterOnSharedPreferenceChangeListener(listener);
    }

    @SuppressLint("ViewConstructor")
    private static final class ZoneView extends View {

        private static final int BRIGHTNESS_FALLBACK_COLOR = 0xFF42A5F5;
        private static final int VOLUME_FALLBACK_COLOR = 0xFF66BB6A;
        private static final int SEEK_FALLBACK_COLOR = 0xFFAB47BC;
        private static final int SPEED_FALLBACK_COLOR = 0xFFFFA726;

        private final boolean horizontal;
        private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint separatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint namePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint percentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF screenRect = new RectF();
        private final RectF zoneRect = new RectF();
        private final Path clipPath = new Path();

        @ColorInt
        private final int screenBackgroundColor;
        @ColorInt
        private final int edgeBackgroundColor;
        @ColorInt
        private final int foregroundColor;
        @ColorInt
        private final int dimTextColor;

        private final String brightnessLabel = str("revanced_swipe_zone_label_brightness");
        private final String volumeLabel = str("revanced_swipe_zone_label_volume");
        private final String seekLabel = str("revanced_swipe_zone_label_seek");
        private final String speedLabel = str("revanced_swipe_zone_label_speed");
        private final String defaultLabel = str("revanced_change_form_factor_entry_1");

        ZoneView(Context context, boolean horizontal) {
            super(context);
            this.horizontal = horizontal;

            screenBackgroundColor = getAppBackgroundColor();
            edgeBackgroundColor = adjustColorBrightness(
                    screenBackgroundColor,
                    isDarkModeEnabled() ? 0.90f : 0.97f
            );
            foregroundColor = getAppForegroundColor();
            dimTextColor = withAlpha(foregroundColor, 0x66);

            fillPaint.setStyle(Paint.Style.FILL);

            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(Dim.dp(1));
            borderPaint.setColor(withAlpha(foregroundColor, 0x55));

            separatorPaint.setStyle(Paint.Style.STROKE);
            separatorPaint.setStrokeWidth(Dim.dp(1));
            separatorPaint.setColor(withAlpha(foregroundColor, 0x33));

            namePaint.setTextAlign(Paint.Align.CENTER);
            namePaint.setTextSize(Dim.dp(11));

            percentPaint.setTextAlign(Paint.Align.CENTER);
            percentPaint.setTextSize(Dim.dp(10));
        }

        @Override
        protected void onDraw(@NonNull Canvas canvas) {
            super.onDraw(canvas);

            final float padH = Dim.dp4;
            final float padV = Dim.dp6;
            final float right = getWidth() - padH;
            final float bottom = getHeight() - padV;
            if (right <= padH || bottom <= padV) return;

            screenRect.set(padH, padV, right, bottom);
            final float radius = Dim.dp(5);

            clipPath.reset();
            clipPath.addRoundRect(screenRect, radius, radius, Path.Direction.CW);

            canvas.save();
            canvas.clipPath(clipPath);
            fillPaint.setColor(screenBackgroundColor);
            canvas.drawRect(screenRect, fillPaint);

            if (horizontal) {
                drawHorizontalZones(canvas);
            } else {
                drawVerticalZones(canvas);
            }

            canvas.restore();
            canvas.drawRoundRect(screenRect, radius, radius, borderPaint);
        }

        private void drawVerticalZones(Canvas canvas) {
            final float left = screenRect.left;
            final float top = screenRect.top;
            final float right = screenRect.right;
            final float bottom = screenRect.bottom;
            final float width = right - left;
            final float edgeWidth = Math.min(Dim.dp(12), width * 0.08f);
            final float effectiveLeft = left + edgeWidth;
            final float effectiveRight = right - edgeWidth;
            final float effectiveWidth = Math.max(0, effectiveRight - effectiveLeft);
            final int zonePercent = clamp(Settings.SWIPE_VERTICAL_ZONE.get());
            final float zoneWidth = effectiveWidth * zonePercent / 100f;
            final float defaultWidth = Math.max(0, effectiveWidth - 2f * zoneWidth);

            fillPaint.setColor(edgeBackgroundColor);
            canvas.drawRect(left, top, effectiveLeft, bottom, fillPaint);
            canvas.drawRect(effectiveRight, top, right, bottom, fillPaint);

            final int brightnessColor = previewColor(
                    Settings.SWIPE_OVERLAY_BRIGHTNESS_COLOR.get(), BRIGHTNESS_FALLBACK_COLOR);
            final int volumeColor = previewColor(
                    Settings.SWIPE_OVERLAY_VOLUME_COLOR.get(), VOLUME_FALLBACK_COLOR);

            zoneRect.set(effectiveLeft, top, effectiveLeft + zoneWidth, bottom);
            drawZone(canvas, zoneRect, brightnessColor, Settings.SWIPE_BRIGHTNESS.get());

            zoneRect.set(effectiveRight - zoneWidth, top, effectiveRight, bottom);
            drawZone(canvas, zoneRect, volumeColor, Settings.SWIPE_VOLUME.get());

            canvas.drawLine(effectiveLeft, top, effectiveLeft, bottom, separatorPaint);
            canvas.drawLine(effectiveLeft + zoneWidth, top,
                    effectiveLeft + zoneWidth, bottom, separatorPaint);
            canvas.drawLine(effectiveRight - zoneWidth, top,
                    effectiveRight - zoneWidth, bottom, separatorPaint);
            canvas.drawLine(effectiveRight, top, effectiveRight, bottom, separatorPaint);

            if (zoneWidth >= Dim.dp(30)) {
                zoneRect.set(effectiveLeft, top, effectiveLeft + zoneWidth, bottom);
                drawZoneLabel(canvas, zoneRect, brightnessLabel, zonePercent,
                        Settings.SWIPE_BRIGHTNESS.get());
                zoneRect.set(effectiveRight - zoneWidth, top, effectiveRight, bottom);
                drawZoneLabel(canvas, zoneRect, volumeLabel, zonePercent,
                        Settings.SWIPE_VOLUME.get());
            }

            if (defaultWidth >= Dim.dp(48)) {
                zoneRect.set(effectiveLeft + zoneWidth, top,
                        effectiveLeft + zoneWidth + defaultWidth, bottom);
                drawZoneLabel(canvas, zoneRect, defaultLabel, 100 - (2 * zonePercent), true);
            }
        }

        private void drawHorizontalZones(Canvas canvas) {
            final float left = screenRect.left;
            final float top = screenRect.top;
            final float right = screenRect.right;
            final float bottom = screenRect.bottom;
            final float height = bottom - top;
            final float sideWidth = Math.min(Dim.dp(18), (right - left) * 0.10f);
            final float effectiveLeft = left + sideWidth;
            final float effectiveRight = right - sideWidth;
            final int zonePercent = clamp(Settings.SWIPE_HORIZONTAL_ZONE.get());
            final float zoneHeight = height * zonePercent / 100f;
            final float defaultHeight = Math.max(0, height - 2f * zoneHeight);

            fillPaint.setColor(edgeBackgroundColor);
            canvas.drawRect(left, top, effectiveLeft, bottom, fillPaint);
            canvas.drawRect(effectiveRight, top, right, bottom, fillPaint);

            final int seekColor = previewColor(
                    Settings.SWIPE_OVERLAY_SEEK_COLOR.get(), SEEK_FALLBACK_COLOR);
            final int speedColor = previewColor(
                    Settings.SWIPE_OVERLAY_SPEED_COLOR.get(), SPEED_FALLBACK_COLOR);
            final boolean topIsSpeed = Settings.SWIPE_SWITCH_SPEED_AND_SEEK.get();

            zoneRect.set(effectiveLeft, top, effectiveRight, top + zoneHeight);
            drawZone(canvas, zoneRect, topIsSpeed ? speedColor : seekColor,
                    topIsSpeed ? Settings.SWIPE_SPEED.get() : Settings.SWIPE_SEEK.get());

            zoneRect.set(effectiveLeft, bottom - zoneHeight, effectiveRight, bottom);
            drawZone(canvas, zoneRect, topIsSpeed ? seekColor : speedColor,
                    topIsSpeed ? Settings.SWIPE_SEEK.get() : Settings.SWIPE_SPEED.get());

            canvas.drawLine(effectiveLeft, top, effectiveLeft, bottom, separatorPaint);
            canvas.drawLine(effectiveRight, top, effectiveRight, bottom, separatorPaint);
            canvas.drawLine(effectiveLeft, top + zoneHeight,
                    effectiveRight, top + zoneHeight, separatorPaint);
            canvas.drawLine(effectiveLeft, bottom - zoneHeight,
                    effectiveRight, bottom - zoneHeight, separatorPaint);

            if (zoneHeight >= Dim.dp(20) && effectiveRight - effectiveLeft >= Dim.dp(50)) {
                zoneRect.set(effectiveLeft, top, effectiveRight, top + zoneHeight);
                drawZoneLabel(canvas, zoneRect, topIsSpeed ? speedLabel : seekLabel, zonePercent,
                        topIsSpeed ? Settings.SWIPE_SPEED.get() : Settings.SWIPE_SEEK.get());
                zoneRect.set(effectiveLeft, bottom - zoneHeight, effectiveRight, bottom);
                drawZoneLabel(canvas, zoneRect, topIsSpeed ? seekLabel : speedLabel, zonePercent,
                        topIsSpeed ? Settings.SWIPE_SEEK.get() : Settings.SWIPE_SPEED.get());
            }

            if (defaultHeight >= Dim.dp(28) && effectiveRight - effectiveLeft >= Dim.dp(50)) {
                zoneRect.set(effectiveLeft, top + zoneHeight, effectiveRight, bottom - zoneHeight);
                drawZoneLabel(canvas, zoneRect, defaultLabel, 100 - (2 * zonePercent), true);
            }
        }

        private void drawZone(Canvas canvas, RectF rect, @ColorInt int color, boolean enabled) {
            fillPaint.setColor(enabled
                    ? withAlpha(color, 0x72)
                    : withAlpha(foregroundColor, 0x18));
            canvas.drawRect(rect, fillPaint);
        }

        private void drawZoneLabel(Canvas canvas, RectF rect, String label,
                                   int percent, boolean enabled) {
            final int textColor = enabled ? foregroundColor : dimTextColor;
            namePaint.setColor(textColor);
            percentPaint.setColor(textColor);
            final float centerX = rect.centerX();

            if (rect.height() >= Dim.dp(38)) {
                final float centerY = rect.centerY();
                canvas.drawText(label, centerX,
                        centerY - Dim.dp(3), namePaint);
                canvas.drawText(percent + "%", centerX,
                        centerY + Dim.dp(11), percentPaint);
            } else {
                final Paint.FontMetrics metrics = namePaint.getFontMetrics();
                final float baseline = rect.centerY() - (metrics.ascent + metrics.descent) / 2f;
                canvas.drawText(label, centerX, baseline, namePaint);
            }
        }

        @ColorInt
        private int previewColor(String value, @ColorInt int fallback) {
            try {
                final int parsed = Color.parseColor(value) | 0xFF000000;
                final float first = relativeLuminance(parsed);
                final float second = relativeLuminance(screenBackgroundColor);
                final float contrast = (Math.max(first, second) + 0.05f)
                        / (Math.min(first, second) + 0.05f);
                return contrast >= 1.5f ? parsed : fallback;
            } catch (Exception ignored) {
                return fallback;
            }
        }

        private static int clamp(int value) {
            return Math.max(0, Math.min(50, value));
        }

        private static float relativeLuminance(@ColorInt int color) {
            final float red = Color.red(color) / 255f;
            final float green = Color.green(color) / 255f;
            final float blue = Color.blue(color) / 255f;
            return 0.2126f * red + 0.7152f * green + 0.0722f * blue;
        }

        @ColorInt
        private static int withAlpha(@ColorInt int color, int alpha) {
            return (color & 0x00FFFFFF) | (alpha << 24);
        }
    }
}

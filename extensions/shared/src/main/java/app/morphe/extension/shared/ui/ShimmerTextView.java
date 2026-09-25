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

package app.morphe.extension.shared.ui;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Shader;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A text view with an explicitly controlled shimmer animation clipped to its glyphs.
 * Callers own the animation lifecycle through {@link #startShimmer()} and
 * {@link #stopShimmer()}; detaching the view always stops it to avoid retaining its context.
 */
@SuppressLint("AppCompatCustomView")
public final class ShimmerTextView extends TextView {
    @Nullable
    private ValueAnimator shimmerAnimator;
    @Nullable
    private LinearGradient shimmerGradient;
    private final Matrix shimmerMatrix = new Matrix();

    public ShimmerTextView(@NonNull Context context) {
        super(context);
    }

    public void startShimmer() {
        if (shimmerAnimator != null) {
            return;
        }
        shimmerAnimator = ValueAnimator.ofFloat(-1f, 2f);
        shimmerAnimator.setDuration(1300L);
        shimmerAnimator.setInterpolator(new LinearInterpolator());
        shimmerAnimator.setRepeatCount(ValueAnimator.INFINITE);
        shimmerAnimator.addUpdateListener(animation -> {
            if (shimmerGradient == null || getWidth() <= 0) {
                return;
            }
            float animatedFraction = (float) animation.getAnimatedValue();
            shimmerMatrix.setTranslate(getWidth() * animatedFraction, 0f);
            shimmerGradient.setLocalMatrix(shimmerMatrix);
            invalidate();
        });
        rebuildShader();
        shimmerAnimator.start();
    }

    public void stopShimmer() {
        if (shimmerAnimator != null) {
            shimmerAnimator.cancel();
            shimmerAnimator = null;
        }
        shimmerGradient = null;
        getPaint().setShader(null);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        rebuildShader();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopShimmer();
        super.onDetachedFromWindow();
    }

    private void rebuildShader() {
        if (getWidth() <= 0 || shimmerAnimator == null) {
            return;
        }

        int baseColor = getCurrentTextColor();
        shimmerGradient = new LinearGradient(
                -getWidth(),
                0f,
                0f,
                0f,
                new int[]{withAlpha(baseColor, 105), baseColor, withAlpha(baseColor, 105)},
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP
        );
        getPaint().setShader(shimmerGradient);
        invalidate();
    }

    @SuppressWarnings("SameParameterValue")
    private static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }
}

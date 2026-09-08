/*
 * Copyright (C) 2025-2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - Jav1x (https://github.com/Jav1x)
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

package app.morphe.extension.youtube.patches.overlaybutton

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.view.View
import app.morphe.extension.shared.utils.Logger
import app.morphe.extension.youtube.patches.utils.PatchStatus
import app.morphe.extension.youtube.patches.voiceovertranslation.VoiceOverTranslationPatch
import app.morphe.extension.youtube.settings.Settings
import app.morphe.extension.youtube.shared.PlayerControlButton
import app.morphe.extension.youtube.shared.RootView
import app.morphe.extension.youtube.shared.RootView.isAdProgressTextVisible
import app.morphe.extension.youtube.utils.VideoUtils

@Suppress("DEPRECATION", "unused")
object VoiceOverTranslationButton {
    private var instance: PlayerControlButton? = null
    private var buttonIcon: Drawable? = null
    private val updateWaitingIndicator = Runnable { refreshActivatedState() }

    /**
     * Injection point.
     */
    @JvmStatic
    fun initializeButton(controlsView: View) {
        try {
            instance?.imageView()?.removeCallbacks(updateWaitingIndicator)
            VoiceOverTranslationPatch.setOnTranslationStateChangeCallback { refreshActivatedState() }
            instance = PlayerControlButton(
                controlsViewGroup = controlsView,
                imageViewButtonId = "revanced_vot_button",
                buttonVisibility = { isButtonEnabled() },
                onClickListener = { view: View -> onClick(view) },
                onLongClickListener = { view: View -> onLongClick(view) },
            )
            buttonIcon = instance?.imageView()?.drawable
            instance?.imageView()?.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(view: View) = refreshActivatedState()

                override fun onViewDetachedFromWindow(view: View) {
                    view.removeCallbacks(updateWaitingIndicator)
                }
            })
            refreshActivatedState()
        } catch (ex: Exception) {
            Logger.printException({ "VoiceOverTranslationButton initializeButton failure" }, ex)
        }
    }

    /**
     * Injection point.
     */
    @JvmStatic
    fun setVisibilityNegatedImmediate() {
        instance?.setVisibilityNegatedImmediate()
    }

    /**
     * Injection point.
     */
    @JvmStatic
    fun setVisibilityImmediate(visible: Boolean) {
        instance?.setActivated()
        instance?.setVisibilityImmediate(visible)
    }

    /**
     * Injection point.
     */
    @JvmStatic
    fun setVisibility(visible: Boolean, animated: Boolean) {
        instance?.setActivated()
        instance?.setVisibility(visible, animated)
    }

    private fun isButtonEnabled(): Boolean {
        return Settings.VOT_ENABLED.get()
                && !isAdProgressTextVisible()
                && PatchStatus.VoiceOverTranslation()
    }

    private fun onClick(view: View) {
        if (VoiceOverTranslationPatch.isTranslationRequestInProgress()) {
            showMenu(view)
            return
        }
        VoiceOverTranslationPatch.toggleTranslation()
        instance?.imageView()?.isActivated = VoiceOverTranslationPatch.isTranslationActive()
    }

    private fun onLongClick(view: View): Boolean {
        return showMenu(view)
    }

    private fun showMenu(view: View): Boolean {
        val context = RootView.getContext() ?: view.context
        VideoUtils.showVotBottomSheetDialog(context)
        return true
    }

    private fun refreshActivatedState() {
        instance?.setActivated()
        val button = instance?.imageView() ?: return
        button.removeCallbacks(updateWaitingIndicator)
        val waiting = VoiceOverTranslationPatch.isTranslationRequestInProgress()
        if (waiting) {
            // Remove the drawable so its cached player-button shadow also disappears.
            if (button.drawable != null) {
                buttonIcon = button.drawable
                button.setImageDrawable(null)
            }
            if (button.foreground !is WaitingIndicator) {
                button.foreground = WaitingIndicator(button.resources.displayMetrics.density)
            }
            button.foreground.invalidateSelf()
            if (button.isAttachedToWindow) button.postDelayed(updateWaitingIndicator, 50)
        } else {
            if (button.drawable == null) button.setImageDrawable(buttonIcon)
            button.foreground = null
        }
    }

    /**
     * Uses the same deadline as the bottom sheet. An expired or missing estimate shows a
     * spinner and an ellipsis, rather than displaying a fabricated remaining time.
     * The foreground surrounds the whole button independently of its icon padding.
     */
    private class WaitingIndicator(private val density: Float) : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val arc = RectF()
        private val pill = RectF()
        private val textBounds = Rect()

        override fun draw(canvas: Canvas) {
            val cx = bounds.exactCenterX()
            val cy = bounds.exactCenterY()
            val radius = minOf(bounds.width(), bounds.height()) / 2f - 3 * density
            if (radius <= 0) return
            arc.set(cx - radius, cy - radius, cx + radius, cy + radius)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2 * density
            paint.strokeCap = Paint.Cap.ROUND
            paint.color = Color.rgb(255, 219, 77) // Matches the activated Yandex icon (#FFDB4D).
            paint.alpha = 48
            canvas.drawArc(arc, -90f, 360f, false, paint)
            paint.alpha = 255
            val progress = VoiceOverTranslationPatch.getTranslationRequestProgressFraction()
            if (progress >= 0) {
                canvas.drawArc(arc, -90f, 360f * progress, false, paint)
            } else {
                canvas.drawArc(arc, (SystemClock.uptimeMillis() % 1080) / 3f - 90f, 100f, false, paint)
            }

            val seconds = VoiceOverTranslationPatch.getTranslationRequestRemainingSeconds()
            val label = if (seconds > 0) seconds.toString() else "…"
            paint.style = Paint.Style.FILL
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 11 * density
            val maxTextWidth = (radius * 2 - 8 * density).coerceAtLeast(density)
            if (paint.measureText(label) > maxTextWidth) {
                paint.textSize *= maxTextWidth / paint.measureText(label)
            }
            val halfWidth = paint.measureText(label) / 2 + 3 * density
            val halfHeight = (paint.descent() - paint.ascent()) / 2 + 2 * density
            pill.set(cx - halfWidth, cy - halfHeight, cx + halfWidth, cy + halfHeight)
            paint.color = Color.argb(153, 0, 0, 0) // Same black pill as seekbar preview labels.
            canvas.drawRoundRect(pill, halfHeight, halfHeight, paint)
            paint.color = Color.WHITE
            val baseline = if (seconds > 0) {
                cy - (paint.ascent() + paint.descent()) / 2
            } else {
                // Center the visible dots, which sit below the font's vertical midpoint.
                paint.getTextBounds(label, 0, label.length, textBounds)
                cy - textBounds.exactCenterY()
            }
            canvas.drawText(label, cx, baseline, paint)
        }

        override fun setAlpha(alpha: Int) = Unit
        override fun setColorFilter(colorFilter: ColorFilter?) = Unit
        @Deprecated("Deprecated in Android")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }

    private fun PlayerControlButton.setActivated() {
        imageView()?.isActivated = VoiceOverTranslationPatch.isTranslationActive()
    }
}

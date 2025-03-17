package app.revanced.extension.youtube.swipecontrols.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.text.TextPaint
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.RelativeLayout
import app.revanced.extension.shared.utils.ResourceUtils
import app.revanced.extension.shared.utils.StringRef.str
import app.revanced.extension.shared.utils.Utils.getTimeStamp
import app.revanced.extension.youtube.shared.VideoInformation
import app.revanced.extension.youtube.swipecontrols.SwipeControlsConfigurationProvider
import app.revanced.extension.youtube.swipecontrols.misc.SwipeControlsOverlay
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/**
 * Main overlay layout for displaying volume, brightness, speed, and seek with both circular and horizontal progress bars.
 */
class SwipeControlsOverlayLayout(
    context: Context,
    private val config: SwipeControlsConfigurationProvider,
) : RelativeLayout(context), SwipeControlsOverlay {
    constructor(context: Context) : this(context, SwipeControlsConfigurationProvider(context))

    // Drawable icons for brightness, volume, speed, and seek
    private val autoBrightnessIcon: Drawable = getDrawable("revanced_ic_sc_brightness_auto")
    private val lowBrightnessIcon: Drawable = getDrawable("revanced_ic_sc_brightness_low")
    private val mediumBrightnessIcon: Drawable = getDrawable("revanced_ic_sc_brightness_medium")
    private val highBrightnessIcon: Drawable = getDrawable("revanced_ic_sc_brightness_high")
    private val fullBrightnessIcon: Drawable = getDrawable("revanced_ic_sc_brightness_full")
    private val mutedVolumeIcon: Drawable = getDrawable("revanced_ic_sc_volume_mute")
    private val lowVolumeIcon: Drawable = getDrawable("revanced_ic_sc_volume_low")
    private val normalVolumeIcon: Drawable = getDrawable("revanced_ic_sc_volume_normal")
    private val fullVolumeIcon: Drawable = getDrawable("revanced_ic_sc_volume_high")
    private val speedIcon: Drawable = getDrawable("revanced_ic_sc_speed")
    private val seekIcon: Drawable = getDrawable("revanced_ic_sc_seek")

    // Function to retrieve drawable resources by name
    private fun getDrawable(name: String): Drawable {
        val drawable = resources.getDrawable(
            ResourceUtils.getIdentifier(name, ResourceUtils.ResourceType.DRAWABLE, context),
            context.theme,
        )
        drawable.setTint(config.overlayTextColor)
        return drawable
    }

    // Initialize progress bars
    private val circularProgressView: CircularProgressView = CircularProgressView(
        context,
        config.overlayBackgroundOpacity,
        config.overlayShowOverlayMinimalStyle,
        config.overlayProgressColor,
        config.overlayFillBackgroundPaint,
        config.overlayTextColor
    ).apply {
        layoutParams = LayoutParams(300, 300).apply {
            addRule(CENTER_IN_PARENT, TRUE)
        }
        visibility = GONE
    }
    private val horizontalProgressView: HorizontalProgressView

    init {
        addView(circularProgressView)

        val screenWidth = resources.displayMetrics.widthPixels
        val maxWidth = (screenWidth * 4 / 5).toInt() // 80% of screen width max
        horizontalProgressView = HorizontalProgressView(
            context,
            config.overlayBackgroundOpacity,
            config.overlayShowOverlayMinimalStyle,
            config.overlayProgressColor,
            config.overlayFillBackgroundPaint,
            config.overlayTextColor
        ).apply {
            layoutParams = LayoutParams(maxWidth, 100).apply {
                addRule(CENTER_HORIZONTAL)
                topMargin = 40
            }
            visibility = GONE
        }
        addView(horizontalProgressView)
    }

    // Handler and callback for hiding progress bars
    private val feedbackHideHandler = Handler(Looper.getMainLooper())
    private val feedbackHideCallback = Runnable {
        circularProgressView.visibility = GONE
        horizontalProgressView.visibility = GONE
    }

    // Enum to represent the type of feedback
    private enum class FeedbackType {
        BRIGHTNESS, VOLUME, SPEED, SEEK
    }

    /**
     * Displays the progress bar with the appropriate value, icon, and type (brightness, volume, speed, or seek).
     */
    private fun showFeedbackView(value: String, progress: Int, max: Int, icon: Drawable, type: FeedbackType) {
        feedbackHideHandler.removeCallbacks(feedbackHideCallback)
        feedbackHideHandler.postDelayed(feedbackHideCallback, config.overlayShowTimeoutMillis)

        val isBrightness = type == FeedbackType.BRIGHTNESS
        val isSeek = type == FeedbackType.SEEK
        val viewToShow = if (config.isCircularProgressBar) circularProgressView else horizontalProgressView
        viewToShow.apply {
            setProgress(progress, max, value, isBrightness, isSeek)
            this.icon = icon
            visibility = VISIBLE
        }
    }

    override fun onVolumeChanged(newVolume: Int, maximumVolume: Int) {
        val volumePercentage = (newVolume.toFloat() / maximumVolume) * 100
        val icon = when {
            newVolume == 0 -> mutedVolumeIcon
            volumePercentage < 33 -> lowVolumeIcon
            volumePercentage < 66 -> normalVolumeIcon
            else -> fullVolumeIcon
        }
        showFeedbackView("$newVolume", newVolume, maximumVolume, icon, FeedbackType.VOLUME)
    }

    override fun onBrightnessChanged(brightness: Double) {
        if (config.shouldLowestValueEnableAutoBrightness && brightness <= 0) {
            showFeedbackView(
                str("revanced_swipe_lowest_value_auto_brightness_overlay_text"),
                0,
                100,
                autoBrightnessIcon,
                FeedbackType.BRIGHTNESS
            )
        } else {
            val brightnessValue = round(brightness).toInt()
            val icon = when {
                brightnessValue < 25 -> lowBrightnessIcon
                brightnessValue < 50 -> mediumBrightnessIcon
                brightnessValue < 75 -> highBrightnessIcon
                else -> fullBrightnessIcon
            }
            showFeedbackView("$brightnessValue%", brightnessValue, 100, icon, FeedbackType.BRIGHTNESS)
        }
    }

    override fun onSpeedChanged(speed: Float) {
        val displaySpeed = String.format(Locale.US, "%.2fx", speed)
        showFeedbackView(
            displaySpeed,
            (speed * 100).toInt(),
            800,
            speedIcon,
            FeedbackType.SPEED
        )
    }

    override fun onSeekChanged(seekAmount: Int) {
        val currentTime = VideoInformation.getVideoTime()
        val totalTime = VideoInformation.getVideoLength()
        val newTime = currentTime + seekAmount

        val text = "${getTimeStamp(newTime)} / ${getTimeStamp(totalTime)}"
        val progress = if (totalTime > 0) ((newTime.toFloat() / totalTime) * 100).toInt() else 0

        showFeedbackView(text, progress, 100, seekIcon, FeedbackType.SEEK)
    }

    // Begin swipe session
    override fun onEnterSwipeSession() {
        if (config.shouldEnableHapticFeedback) {
            @Suppress("DEPRECATION")
            performHapticFeedback(
                HapticFeedbackConstants.LONG_PRESS,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING,
            )
        }
    }
}

/**
 * Abstract base class for progress views with dynamic content sizing.
 */
abstract class AbstractProgressView(
    context: Context,
    overlayBackgroundOpacity: Int,
    protected val overlayShowOverlayMinimalStyle: Boolean,
    overlayProgressColor: Int,
    overlayFillBackgroundPaint: Int,
    protected val overlayTextColor: Int,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Combined paint creation function for both fill and stroke styles
    private fun createPaint(
        color: Int,
        style: Paint.Style = Paint.Style.FILL,
        strokeCap: Paint.Cap = Paint.Cap.BUTT,
        strokeWidth: Float = 0f
    ) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.style = style
        this.color = color
        this.strokeCap = strokeCap
        this.strokeWidth = strokeWidth
    }

    // Initialize paints
    val backgroundPaint = createPaint(overlayBackgroundOpacity, style = Paint.Style.FILL)
    val progressPaint =
        createPaint(overlayProgressColor, style = Paint.Style.STROKE, strokeCap = Paint.Cap.ROUND, strokeWidth = 20f)
    val fillBackgroundPaint = createPaint(overlayFillBackgroundPaint, style = Paint.Style.FILL)

    // Create text paint with advanced features
    val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = overlayTextColor
        textAlign = Paint.Align.CENTER
        textSize = 40f
    }

    // Rect for text measurement
    protected val textBounds = Rect()

    protected var progress = 0
    protected var maxProgress = 100
    protected var displayText: String = "0"
    protected var isBrightness = true
    protected var isSeek = false
    var icon: Drawable? = null

    /**
     * Set progress values and display text
     * @param value Current progress value
     * @param max Maximum progress value
     * @param text Text to display
     * @param isBrightnessMode Whether this is a brightness adjustment
     * @param isSeekMode Whether this is a seek operation (has special formatting)
     */
    open fun setProgress(value: Int, max: Int, text: String, isBrightnessMode: Boolean, isSeekMode: Boolean = false) {
        progress = value
        maxProgress = max
        displayText = text
        isBrightness = isBrightnessMode
        isSeek = isSeekMode
        invalidate()
    }

    /**
     * Measure text dimensions
     * @param text Text to measure
     * @param paint Paint to use for measurement
     * @return Width of the text
     */
    protected fun measureTextWidth(text: String, paint: Paint): Int {
        paint.getTextBounds(text, 0, text.length, textBounds)
        return textBounds.width()
    }

    override fun onDraw(canvas: Canvas) {
        // Base class implementation can be empty
    }
}

/**
 * Custom view for rendering a circular progress indicator with adaptive sizing based on content.
 */
@SuppressLint("ViewConstructor")
class CircularProgressView(
    context: Context,
    overlayBackgroundOpacity: Int,
    overlayShowOverlayMinimalStyle: Boolean,
    overlayProgressColor: Int,
    overlayFillBackgroundPaint: Int,
    overlayTextColor: Int,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractProgressView(
    context,
    overlayBackgroundOpacity,
    overlayShowOverlayMinimalStyle,
    overlayProgressColor,
    overlayFillBackgroundPaint,
    overlayTextColor,
    attrs,
    defStyleAttr
) {
    private val rectF = RectF()
    private val tempTextPaint = TextPaint(textPaint)

    // Reduced base font size for duration text
    private val durationFontSize = 30f

    // Circle size parameters for dynamic measuring
    private var circleSize = 300f
    private val minCircleSize = 300f
    private val maxCircleSize = 400f

    private val maxTextWidth: Float = 180f

    // Space between icon and text, centered together in full mode
    private val iconTextSpacing = 40f

    init {
        val strokeThickness = 20f
        progressPaint.strokeWidth = strokeThickness
        fillBackgroundPaint.strokeWidth = strokeThickness
        progressPaint.strokeCap = Paint.Cap.ROUND
        fillBackgroundPaint.strokeCap = Paint.Cap.BUTT
        progressPaint.style = Paint.Style.STROKE
        fillBackgroundPaint.style = Paint.Style.STROKE
    }

    /**
     * Override onMeasure to handle dynamic sizing for long durations.
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (isSeek && !overlayShowOverlayMinimalStyle) {
            // Start with base size and adjust based on text length.
            circleSize = minCircleSize
            if (displayText.length > 10) {
                // Increase circle size proportionally if the duration text is long.
                val textFactor = min((displayText.length - 10) * 10f, 100f)
                circleSize = min(minCircleSize + textFactor, maxCircleSize)
            }
            setMeasuredDimension(circleSize.toInt(), circleSize.toInt())
        } else {
            // Use a fixed size for non-seek modes.
            circleSize = minCircleSize
            setMeasuredDimension(minCircleSize.toInt(), minCircleSize.toInt())
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        val margin = 20f
        rectF.set(margin, margin, size - margin, size - margin)

        canvas.drawOval(rectF, fillBackgroundPaint)
        canvas.drawCircle(width / 2f, height / 2f, size / 3, backgroundPaint)
        val sweepAngle = (progress.toFloat() / maxProgress) * 360f
        canvas.drawArc(rectF, -90f, sweepAngle, false, progressPaint)

        if (overlayShowOverlayMinimalStyle) {
            icon?.let {
                val iconSize = 100
                val iconX = (width - iconSize) / 2f
                val iconY = (height - iconSize) / 2f
                it.setBounds(iconX.toInt(), iconY.toInt(), (iconX + iconSize).toInt(), (iconY + iconSize).toInt())
                it.draw(canvas)
            }
        } else {
            val centerX = width / 2f
            val centerY = height / 2f

            icon?.let {
                val iconSize = 80
                val iconHalfSize = iconSize / 2
                val iconX = centerX - iconHalfSize
                val iconY = centerY - iconTextSpacing
                it.setBounds(
                    iconX.toInt(),
                    (iconY - iconHalfSize).toInt(),
                    (iconX + iconSize).toInt(),
                    (iconY + iconHalfSize).toInt()
                )
                it.draw(canvas)
            }

            if (isSeek) {
                tempTextPaint.textSize = durationFontSize
                tempTextPaint.textAlign = Paint.Align.CENTER
                var textSize = durationFontSize
                while (measureTextWidth(displayText, tempTextPaint) > maxTextWidth && textSize > 16f) {
                    textSize -= 2f
                    tempTextPaint.textSize = textSize
                }
                canvas.drawText(
                    displayText,
                    centerX,
                    centerY + iconTextSpacing + tempTextPaint.textSize / 3,
                    tempTextPaint
                )
            } else {
                var textSize = 40f
                tempTextPaint.textSize = textSize
                tempTextPaint.textAlign = Paint.Align.CENTER
                while (measureTextWidth(displayText, tempTextPaint) > maxTextWidth && textSize > 20f) {
                    textSize -= 2f
                    tempTextPaint.textSize = textSize
                }
                canvas.drawText(
                    displayText,
                    centerX,
                    centerY + iconTextSpacing + tempTextPaint.textSize / 3,
                    tempTextPaint
                )
            }
        }
    }

    override fun setProgress(value: Int, max: Int, text: String, isBrightnessMode: Boolean, isSeekMode: Boolean) {
        super.setProgress(value, max, text, isBrightnessMode, isSeekMode)
        requestLayout()
    }
}

/**
 * Custom view for rendering a horizontal progress bar with dynamic width based on content.
 */
@SuppressLint("ViewConstructor")
class HorizontalProgressView(
    context: Context,
    overlayBackgroundOpacity: Int,
    overlayShowOverlayMinimalStyle: Boolean,
    overlayProgressColor: Int,
    overlayFillBackgroundPaint: Int,
    overlayTextColor: Int,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractProgressView(
    context,
    overlayBackgroundOpacity,
    overlayShowOverlayMinimalStyle,
    overlayProgressColor,
    overlayFillBackgroundPaint,
    overlayTextColor,
    attrs,
    defStyleAttr
) {
    private val iconSize = 60f
    private val progressBarHeight = 10f
    private val padding = 30f
    private var textWidth = 0f
    private val progressBarWidth: Float = resources.displayMetrics.widthPixels / 4f

    init {
        textPaint.textSize = 40f
        progressPaint.strokeWidth = 0f
        progressPaint.strokeCap = Paint.Cap.BUTT
        progressPaint.style = Paint.Style.FILL
        fillBackgroundPaint.style = Paint.Style.FILL
    }

    /**
     * Calculate required width based on content
     * @return Required width to display all elements
     */
    private fun calculateRequiredWidth(): Float {
        textWidth = measureTextWidth(displayText, textPaint).toFloat()

        return if (!overlayShowOverlayMinimalStyle) {
            padding + iconSize + padding + progressBarWidth + padding + textWidth + padding
        } else {
            padding + iconSize + padding + textWidth + padding
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val suggestedWidth = MeasureSpec.getSize(widthMeasureSpec)
        val suggestedHeight = MeasureSpec.getSize(heightMeasureSpec)

        val height = suggestedHeight
        val requiredWidth = calculateRequiredWidth().toInt()
        val width = min(max(100, requiredWidth), suggestedWidth)

        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        textWidth = measureTextWidth(displayText, textPaint).toFloat()

        val cornerRadius = viewHeight / 2

        val startX = padding
        val iconEndX = startX + iconSize

        val textStartX = (viewWidth - 1.5 * padding - textWidth).toFloat()

        canvas.drawRoundRect(
            0f, 0f, viewWidth, viewHeight,
            cornerRadius, cornerRadius, backgroundPaint
        )

        icon?.let {
            val iconY = viewHeight / 2 - iconSize / 2
            it.setBounds(
                startX.toInt(),
                iconY.toInt(),
                (startX + iconSize).toInt(),
                (iconY + iconSize).toInt()
            )
            it.draw(canvas)
        }

        val textY = viewHeight / 2 + textPaint.textSize / 3
        textPaint.textAlign = Paint.Align.LEFT

        if (overlayShowOverlayMinimalStyle) {
            canvas.drawText(displayText, textStartX, textY, textPaint)
        } else {
            val progressStartX = iconEndX + padding
            val progressEndX = textStartX - padding
            val progressWidth = progressEndX - progressStartX

            if (progressWidth > 50) {
                canvas.drawRoundRect(
                    progressStartX,
                    viewHeight / 2 - progressBarHeight / 2,
                    progressEndX,
                    viewHeight / 2 + progressBarHeight / 2,
                    progressBarHeight / 2,
                    progressBarHeight / 2,
                    fillBackgroundPaint
                )
                val progressValue = (progress.toFloat() / maxProgress) * progressWidth
                canvas.drawRoundRect(
                    progressStartX,
                    viewHeight / 2 - progressBarHeight / 2,
                    progressStartX + progressValue,
                    viewHeight / 2 + progressBarHeight / 2,
                    progressBarHeight / 2,
                    progressBarHeight / 2,
                    progressPaint
                )
            }
            canvas.drawText(displayText, textStartX, textY, textPaint)
        }
    }

    override fun setProgress(value: Int, max: Int, text: String, isBrightnessMode: Boolean, isSeekMode: Boolean) {
        super.setProgress(value, max, text, isBrightnessMode, isSeekMode)
        requestLayout()
    }
}

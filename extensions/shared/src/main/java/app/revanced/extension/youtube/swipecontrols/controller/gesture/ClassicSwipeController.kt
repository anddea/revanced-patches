package app.revanced.extension.youtube.swipecontrols.controller.gesture

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import app.revanced.extension.youtube.settings.Settings
import app.revanced.extension.youtube.shared.PlayerControlsVisibilityObserver
import app.revanced.extension.youtube.shared.PlayerControlsVisibilityObserverImpl
import app.revanced.extension.youtube.swipecontrols.SwipeControlsConfigurationProvider
import app.revanced.extension.youtube.swipecontrols.SwipeControlsHostActivity
import app.revanced.extension.youtube.swipecontrols.controller.gesture.core.BaseGestureController
import app.revanced.extension.youtube.swipecontrols.controller.gesture.core.SwipeDetector
import app.revanced.extension.youtube.swipecontrols.misc.contains
import app.revanced.extension.youtube.swipecontrols.misc.toPoint

/**
 * provides the classic swipe controls experience, as it was with 'XFenster'
 *
 * @param controller reference to the main swipe controller
 */
@Suppress("DEPRECATED_SMARTCAST_ON_DELEGATED_PROPERTY")
class ClassicSwipeController(
    private val controller: SwipeControlsHostActivity,
    private val config: SwipeControlsConfigurationProvider,
) : BaseGestureController(controller),
    PlayerControlsVisibilityObserver by PlayerControlsVisibilityObserverImpl(controller) {

    /**
     * the last event captured in [onDown]
     */
    private var lastOnDownEvent: MotionEvent? = null

    private val handler = Handler(Looper.getMainLooper())
    private var horizontalSwipeRunnable: Runnable? = null
    private var isHorizontalSwipeConfirmed = false
    private val horizontalSwipeDelayMs = Settings.SWIPE_SPEED_AND_SEEK_DELAY.get()

    override val shouldForceInterceptEvents: Boolean
        get() = currentSwipe == SwipeDetector.SwipeDirection.VERTICAL

    override fun isInSwipeZone(motionEvent: MotionEvent): Boolean {
        val inVolumeZone = if (controller.config.enableVolumeControls) {
            (motionEvent.toPoint() in controller.zones.volume)
        } else {
            false
        }
        val inBrightnessZone = if (controller.config.enableBrightnessControl) {
            (motionEvent.toPoint() in controller.zones.brightness)
        } else {
            false
        }

        return inVolumeZone || inBrightnessZone
    }

    private fun isInHorizontalSwipeZone(motionEvent: MotionEvent): Boolean {
        val inSpeedZone =
            controller.config.enableSpeedControl && (motionEvent.toPoint() in controller.zones.speed)
        val inSeekZone =
            controller.config.enableSeekControl && (motionEvent.toPoint() in controller.zones.seek)
        return inSpeedZone || inSeekZone
    }

    override fun shouldDropMotion(motionEvent: MotionEvent): Boolean {
        // ignore gestures with more than one pointer
        // when such a gesture is detected, dispatch the first event of the gesture to downstream
        if (motionEvent.pointerCount > 1) {
            // This is a multitouch gesture (like pinch-to-zoom), so cancel any pending swipe action.
            cancelHorizontalSwipeRunnable()
            isHorizontalSwipeConfirmed = false

            lastOnDownEvent?.let {
                controller.dispatchDownstreamTouchEvent(it)
                it.recycle()
            }
            lastOnDownEvent = null
            return true
        }

        // ignore gestures when player controls are visible
        return arePlayerControlsVisible
    }

    override fun onDown(motionEvent: MotionEvent): Boolean {
        cancelHorizontalSwipeRunnable()
        isHorizontalSwipeConfirmed = false

        // save the event for later
        lastOnDownEvent?.recycle()
        lastOnDownEvent = MotionEvent.obtain(motionEvent)

        // must be inside swipe zone
        return isInSwipeZone(motionEvent) || isInHorizontalSwipeZone(motionEvent)
    }

    override fun onUp(motionEvent: MotionEvent) {
        super.onUp(motionEvent)
        cancelHorizontalSwipeRunnable()
        isHorizontalSwipeConfirmed = false
    }

    override fun onSingleTapUp(motionEvent: MotionEvent): Boolean {
        MotionEvent.obtain(motionEvent).let {
            it.action = MotionEvent.ACTION_DOWN
            controller.dispatchDownstreamTouchEvent(it)
            it.recycle()
        }

        return false
    }

    override fun onDoubleTapEvent(motionEvent: MotionEvent): Boolean {
        MotionEvent.obtain(motionEvent).let {
            controller.dispatchDownstreamTouchEvent(it)
            it.recycle()
        }

        return super.onDoubleTapEvent(motionEvent)
    }

    override fun onLongPress(motionEvent: MotionEvent) {
        MotionEvent.obtain(motionEvent).let {
            controller.dispatchDownstreamTouchEvent(it)
            it.recycle()
        }

        super.onLongPress(motionEvent)
    }

    override fun onSwipe(
        from: MotionEvent,
        to: MotionEvent,
        distanceX: Double,
        distanceY: Double,
    ): Boolean {
        // cancel if locked
        if (!config.enableSwipeControlsLockMode && config.isScreenLocked) return false
        if (currentSwipe == SwipeDetector.SwipeDirection.VERTICAL) {
            cancelHorizontalSwipeRunnable()
            isHorizontalSwipeConfirmed = false

            return when (from.toPoint()) {
                in controller.zones.volume -> {
                    scrollVolume(distanceY)
                    true
                }

                in controller.zones.brightness -> {
                    scrollBrightness(distanceY)
                    true
                }
                else -> false
            }
        }
        else if (currentSwipe == SwipeDetector.SwipeDirection.HORIZONTAL) {
            if (isHorizontalSwipeConfirmed) {
                return processHorizontalSwipe(from, distanceX)
            }

            if (horizontalSwipeRunnable == null) {
                horizontalSwipeRunnable = Runnable {
                    isHorizontalSwipeConfirmed = true
                    processHorizontalSwipe(from, distanceX)
                }
                handler.postDelayed(horizontalSwipeRunnable!!, horizontalSwipeDelayMs)
            }
            return true
        }

        return false
    }

    private fun processHorizontalSwipe(from: MotionEvent, distanceX: Double): Boolean {
        return when (from.toPoint()) {
            in controller.zones.speed -> {
                if (config.enableSpeedControl) {
                    scrollSpeed(distanceX)
                    true
                } else false
            }
            in controller.zones.seek -> {
                if (config.enableSeekControl) {
                    scrollSeek(distanceX)
                    true
                } else false
            }
            else -> false
        }
    }

    private fun cancelHorizontalSwipeRunnable() {
        horizontalSwipeRunnable?.let {
            handler.removeCallbacks(it)
        }
        horizontalSwipeRunnable = null
    }
}

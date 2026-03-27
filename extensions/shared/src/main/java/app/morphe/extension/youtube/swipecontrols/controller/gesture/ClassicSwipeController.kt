package app.morphe.extension.youtube.swipecontrols.controller.gesture

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import app.morphe.extension.youtube.settings.Settings
import app.morphe.extension.youtube.shared.PlayerControlsVisibilityObserver
import app.morphe.extension.youtube.shared.PlayerControlsVisibilityObserverImpl
import app.morphe.extension.youtube.swipecontrols.SwipeControlsConfigurationProvider
import app.morphe.extension.youtube.swipecontrols.SwipeControlsHostActivity
import app.morphe.extension.youtube.swipecontrols.controller.gesture.core.BaseGestureController
import app.morphe.extension.youtube.swipecontrols.controller.gesture.core.SwipeDetector
import app.morphe.extension.youtube.swipecontrols.misc.contains
import app.morphe.extension.youtube.swipecontrols.misc.toPoint

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
    private var delayedSwipeRunnable: Runnable? = null
    private var isSwipeConfirmed = false
    private val swipeDelayMs = Settings.SWIPE_DELAY.get()

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
            cancelDelayedSwipe()
            isSwipeConfirmed = false

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
        cancelDelayedSwipe()
        isSwipeConfirmed = false

        // save the event for later
        lastOnDownEvent?.recycle()
        lastOnDownEvent = MotionEvent.obtain(motionEvent)

        // must be inside swipe zone
        return isInSwipeZone(motionEvent) || isInHorizontalSwipeZone(motionEvent)
    }

    override fun onUp(motionEvent: MotionEvent) {
        super.onUp(motionEvent)
        cancelDelayedSwipe()
        isSwipeConfirmed = false
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

        // Ensure the gesture starts in the valid zone.
        // If we swipe Vertically, but we are not in the Volume/Brightness zone (e.g. we are on the Description Panel,
        // Comments), we must return false immediately to allow the view hierarchy (ScrollView) to handle the touch.
        val validVertical = currentSwipe == SwipeDetector.SwipeDirection.VERTICAL && isInSwipeZone(from)
        val validHorizontal = currentSwipe == SwipeDetector.SwipeDirection.HORIZONTAL && isInHorizontalSwipeZone(from)

        if (!validVertical && !validHorizontal) {
            return false
        }

        // If the swipe is already confirmed, process immediately
        if (isSwipeConfirmed) {
            return if (currentSwipe == SwipeDetector.SwipeDirection.VERTICAL) {
                processVerticalSwipe(from, distanceY)
            } else {
                processHorizontalSwipe(from, distanceX)
            }
        }

        // If not confirmed, queue the runnable (if not already queued)
        if (delayedSwipeRunnable == null) {
            delayedSwipeRunnable = Runnable {
                isSwipeConfirmed = true
                // Execute the action that was pending
                if (currentSwipe == SwipeDetector.SwipeDirection.VERTICAL) {
                    processVerticalSwipe(from, distanceY)
                } else if (currentSwipe == SwipeDetector.SwipeDirection.HORIZONTAL) {
                    processHorizontalSwipe(from, distanceX)
                }
            }
            handler.postDelayed(delayedSwipeRunnable!!, swipeDelayMs)
        }

        // Return true to indicate we are handling (or waiting to handle) the gesture
        return true
    }

    // Logic for vertical swipes (Volume/Brightness)
    private fun processVerticalSwipe(from: MotionEvent, distanceY: Double): Boolean {
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

    // Logic for horizontal swipes (Seek/Speed)
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

    private fun cancelDelayedSwipe() {
        delayedSwipeRunnable?.let {
            handler.removeCallbacks(it)
        }
        delayedSwipeRunnable = null
    }
}

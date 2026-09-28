package app.morphe.extension.youtube.swipecontrols.controller.gesture

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import app.morphe.extension.youtube.settings.Settings
import app.morphe.extension.youtube.shared.PlayerControlsVisibilityObserver
import app.morphe.extension.youtube.shared.PlayerControlsVisibilityObserverImpl
import app.morphe.extension.youtube.swipecontrols.SwipeControlsConfigurationProvider
import app.morphe.extension.youtube.swipecontrols.SwipeControlsConfigurationProvider.SwipeZoneAction
import app.morphe.extension.youtube.swipecontrols.SwipeControlsHostActivity
import app.morphe.extension.youtube.swipecontrols.controller.gesture.core.BaseGestureController
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
        get() {
            val origin = lastOnDownEvent?.toPoint() ?: return false
            return swipeActionAt(origin, currentSwipe) != SwipeZoneAction.OFF
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

        // must be inside an active swipe zone
        return isInSwipeZone(motionEvent)
    }

    override fun onUp(motionEvent: MotionEvent) {
        super.onUp(motionEvent)
        cancelDelayedSwipe()
        isSwipeConfirmed = false
        lastOnDownEvent?.recycle()
        lastOnDownEvent = null
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
        // If the swipe did not start in an active zone, let the view hierarchy handle it.
        if (!shouldForceInterceptEvents) return false

        // If the swipe is already confirmed, process immediately.
        if (isSwipeConfirmed) {
            return applySwipeAction(from, distanceX, distanceY)
        }

        // If not confirmed, queue the runnable (if not already queued)
        if (delayedSwipeRunnable == null) {
            delayedSwipeRunnable = Runnable {
                isSwipeConfirmed = true
                // Execute the action that was pending.
                applySwipeAction(from, distanceX, distanceY)
            }
            handler.postDelayed(delayedSwipeRunnable!!, swipeDelayMs)
        }

        // Return true to indicate we are handling (or waiting to handle) the gesture
        return true
    }

    private fun cancelDelayedSwipe() {
        delayedSwipeRunnable?.let {
            handler.removeCallbacks(it)
        }
        delayedSwipeRunnable = null
    }
}

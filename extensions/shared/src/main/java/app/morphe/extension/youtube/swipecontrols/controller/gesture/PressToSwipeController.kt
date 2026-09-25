package app.morphe.extension.youtube.swipecontrols.controller.gesture

import android.view.MotionEvent
import app.morphe.extension.youtube.swipecontrols.SwipeControlsConfigurationProvider
import app.morphe.extension.youtube.swipecontrols.SwipeControlsConfigurationProvider.SwipeZoneAction
import app.morphe.extension.youtube.swipecontrols.SwipeControlsHostActivity
import app.morphe.extension.youtube.swipecontrols.controller.gesture.core.BaseGestureController
import app.morphe.extension.youtube.swipecontrols.misc.Point
import app.morphe.extension.youtube.swipecontrols.misc.toPoint

/**
 * provides the press-to-swipe (PtS) swipe controls experience
 *
 * @param controller reference to the main swipe controller
 */
class PressToSwipeController(
    private val controller: SwipeControlsHostActivity,
    private val config: SwipeControlsConfigurationProvider,
) : BaseGestureController(controller) {
    /**
     * monitors if the user is currently in a swipe session.
     */
    private var swipeSessionOrigin: Point? = null

    override val shouldForceInterceptEvents: Boolean
        get() {
            val origin = swipeSessionOrigin ?: return false
            return swipeActionAt(origin, currentSwipe) != SwipeZoneAction.OFF
        }

    override fun shouldDropMotion(motionEvent: MotionEvent): Boolean = false

    override fun onUp(motionEvent: MotionEvent) {
        super.onUp(motionEvent)
        swipeSessionOrigin = null
    }

    override fun onLongPress(motionEvent: MotionEvent) {
        // enter swipe session with feedback
        swipeSessionOrigin = if (isInSwipeZone(motionEvent)) motionEvent.toPoint() else null
        if (swipeSessionOrigin != null) {
            controller.overlay.onEnterSwipeSession()
        }

        // send GestureDetector a ACTION_CANCEL event so it will handle further events
        motionEvent.action = MotionEvent.ACTION_CANCEL
        detector.onTouchEvent(motionEvent)
    }

    override fun onSwipe(
        from: MotionEvent,
        to: MotionEvent,
        distanceX: Double,
        distanceY: Double,
    ): Boolean {
        // cancel if locked
        if (!config.enableSwipeControlsLockMode && config.isScreenLocked) return false
        if (swipeSessionOrigin == null) return false

        return applySwipeAction(from, distanceX, distanceY)
    }
}

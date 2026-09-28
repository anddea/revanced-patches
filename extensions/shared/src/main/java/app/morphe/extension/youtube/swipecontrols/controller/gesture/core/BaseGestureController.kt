package app.morphe.extension.youtube.swipecontrols.controller.gesture.core

import android.view.GestureDetector
import android.view.MotionEvent
import app.morphe.extension.youtube.swipecontrols.SwipeControlsConfigurationProvider.SwipeZoneAction
import app.morphe.extension.youtube.swipecontrols.SwipeControlsHostActivity
import app.morphe.extension.youtube.swipecontrols.misc.Point
import app.morphe.extension.youtube.swipecontrols.misc.contains
import app.morphe.extension.youtube.swipecontrols.misc.toPoint

/**
 * the common base of all [GestureController] classes.
 * handles most of the boilerplate code needed for gesture detection
 *
 * @param controller reference to the main swipe controller
 */
abstract class BaseGestureController(
    private val controller: SwipeControlsHostActivity,
) : GestureController,
    GestureDetector.SimpleOnGestureListener(),
    SwipeDetector by SwipeDetectorImpl(
        controller.config.swipeMagnitudeThreshold.toDouble(),
    ),
    VolumeAndBrightnessScroller by VolumeAndBrightnessScrollerImpl(
        controller.audio,
        controller.screen,
        controller.overlay,
        controller.config.volumeDistance,
        controller.config.brightnessDistance,
        controller.config.speedDistance,
        controller.config.seekDistance,
        controller.config.volumeSwipeSensitivity,
    ) {

    /**
     * the main gesture detector that powers everything
     */
    @Suppress("LeakingThis")
    protected val detector = GestureDetector(controller, this)

    /**
     * were downstream event canceled already? used in [onScroll]
     */
    private var didCancelDownstream = false

    override fun submitTouchEvent(motionEvent: MotionEvent): Boolean {
        // ignore if swipe is disabled
        if (!controller.config.enableSwipeControls) {
            return false
        }

        // create a copy of the event so we can modify it
        // without causing any issues downstream
        val me = MotionEvent.obtain(motionEvent)

        // check if we should drop this motion
        val dropped = shouldDropMotion(me)
        if (dropped) {
            me.action = MotionEvent.ACTION_CANCEL
        }

        // send the event to the detector
        // if we force intercept events, the event is always consumed
        val consumed = detector.onTouchEvent(me) || shouldForceInterceptEvents

        // evaluate swipe zone before recycling
        val inSwipeZone = isInSwipeZone(me)

        // invoke the custom onUp handler
        if (me.action == MotionEvent.ACTION_UP || me.action == MotionEvent.ACTION_CANCEL) {
            onUp(me)
        }

        // recycle the copy
        me.recycle()

        // do not consume dropped events
        // or events outside any swipe zone
        return !dropped && consumed && inSwipeZone
    }

    /**
     * custom handler for [MotionEvent.ACTION_UP] event, because GestureDetector doesn't offer that :|
     *
     * @param motionEvent the motion event
     */
    open fun onUp(motionEvent: MotionEvent) {
        didCancelDownstream = false
        resetSwipe()
        resetScroller()
    }

    override fun onScroll(
        from: MotionEvent?,
        to: MotionEvent,
        distanceX: Float,
        distanceY: Float,
    ): Boolean {
        if (from == null) {
            return false
        }

        // submit to swipe detector
        submitForSwipe(from, to, distanceX, distanceY)

        // call swipe callback if in a swipe
        return if (currentSwipe != SwipeDetector.SwipeDirection.NONE) {
            val consumed = onSwipe(
                from,
                to,
                distanceX.toDouble(),
                distanceY.toDouble(),
            )

            // if the swipe was consumed, cancel downstream events once
            if (consumed && !didCancelDownstream) {
                didCancelDownstream = true
                MotionEvent.obtain(from).let {
                    it.action = MotionEvent.ACTION_CANCEL
                    controller.dispatchDownstreamTouchEvent(it)
                    it.recycle()
                }
            }

            consumed
        } else {
            false
        }
    }

    /**
     * should [submitTouchEvent] force-intercept all touch events?
     */
    abstract val shouldForceInterceptEvents: Boolean

    /**
     * Checks if the provided motion event is in an active swipe zone for its direction.
     *
     * Before a direction is detected, all four configured edge zones are accepted. Once the
     * direction is known, only the zones belonging to that axis can consume the gesture.
     *
     * @param motionEvent the event to check.
     * @return whether the event is in an active swipe zone.
     */
    fun isInSwipeZone(motionEvent: MotionEvent): Boolean {
        val point = motionEvent.toPoint()
        return when (currentSwipe) {
            SwipeDetector.SwipeDirection.HORIZONTAL ->
                (controller.config.topZoneAction != SwipeZoneAction.OFF && point in controller.zones.top) ||
                    (controller.config.bottomZoneAction != SwipeZoneAction.OFF && point in controller.zones.bottom)

            SwipeDetector.SwipeDirection.VERTICAL ->
                (controller.config.leftZoneAction != SwipeZoneAction.OFF && point in controller.zones.left) ||
                    (controller.config.rightZoneAction != SwipeZoneAction.OFF && point in controller.zones.right)

            SwipeDetector.SwipeDirection.NONE ->
                (controller.config.leftZoneAction != SwipeZoneAction.OFF && point in controller.zones.left) ||
                    (controller.config.rightZoneAction != SwipeZoneAction.OFF && point in controller.zones.right) ||
                    (controller.config.topZoneAction != SwipeZoneAction.OFF && point in controller.zones.top) ||
                    (controller.config.bottomZoneAction != SwipeZoneAction.OFF && point in controller.zones.bottom)
        }
    }

    /**
     * Resolves the action assigned to the zone where a swipe started.
     *
     * @param origin where the swipe started.
     * @param direction the detected swipe direction.
     * @return the configured action, or [SwipeZoneAction.OFF] if the origin is not valid for the
     * direction.
     */
    protected fun swipeActionAt(origin: Point, direction: SwipeDetector.SwipeDirection): SwipeZoneAction =
        when (direction) {
            SwipeDetector.SwipeDirection.HORIZONTAL -> when (origin) {
                in controller.zones.top -> controller.config.topZoneAction
                in controller.zones.bottom -> controller.config.bottomZoneAction
                else -> SwipeZoneAction.OFF
            }

            SwipeDetector.SwipeDirection.VERTICAL -> when (origin) {
                in controller.zones.left -> controller.config.leftZoneAction
                in controller.zones.right -> controller.config.rightZoneAction
                else -> SwipeZoneAction.OFF
            }

            SwipeDetector.SwipeDirection.NONE -> SwipeZoneAction.OFF
        }

    /**
     * Applies the action assigned to the zone where a swipe started.
     *
     * @param from start event of the swipe.
     * @param distanceX horizontal swipe distance.
     * @param distanceY vertical swipe distance.
     * @return whether the event was consumed.
     */
    protected fun applySwipeAction(from: MotionEvent, distanceX: Double, distanceY: Double): Boolean {
        val direction = currentSwipe
        val action = swipeActionAt(from.toPoint(), direction)
        if (action == SwipeZoneAction.OFF) return false

        // Normalize the physical gesture first: upward and rightward swipes are positive. The
        // legacy speed and seek scrollers use the opposite sign from volume and brightness, so
        // invert their normalized distance to preserve their existing behavior in every zone.
        val normalizedDistance = if (direction == SwipeDetector.SwipeDirection.HORIZONTAL) {
            -distanceX
        } else {
            distanceY
        }
        val distance = when (action) {
            SwipeZoneAction.SPEED,
            SwipeZoneAction.SEEK -> -normalizedDistance
            else -> normalizedDistance
        }

        @Suppress("KotlinConstantConditions")
        return when (action) {
            SwipeZoneAction.VOLUME -> {
                scrollVolume(distance)
                true
            }

            SwipeZoneAction.BRIGHTNESS -> {
                scrollBrightness(distance)
                true
            }

            SwipeZoneAction.SPEED -> {
                scrollSpeed(distance)
                true
            }

            SwipeZoneAction.SEEK -> {
                scrollSeek(distance)
                true
            }

            SwipeZoneAction.OFF -> false
        }
    }

    /**
     * check if a touch event should be dropped.
     * when an event is dropped, the gesture detector received a [MotionEvent.ACTION_CANCEL] event and the event is not consumed
     *
     * @param motionEvent the event to check
     * @return should the event be dropped?
     */
    abstract fun shouldDropMotion(motionEvent: MotionEvent): Boolean

    /**
     * handler for swipe events, once a swipe is detected.
     * the direction of the swipe can be accessed in [currentSwipe]
     *
     * @param from start event of the swipe
     * @param to end event of the swipe
     * @param distanceX the horizontal distance of the swipe
     * @param distanceY the vertical distance of the swipe
     * @return was the event consumed?
     */
    abstract fun onSwipe(
        from: MotionEvent,
        to: MotionEvent,
        distanceX: Double,
        distanceY: Double,
    ): Boolean
}

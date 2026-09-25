package app.morphe.extension.youtube.swipecontrols.misc

import kotlin.math.abs
import kotlin.math.sign

/**
 * helper for scaling onScroll handler
 *
 * @param unitDistance absolute distance after which the callback is invoked
 * @param callback callback function for when unit distance is reached
 */
class ScrollDistanceHelper(
    unitDistance: Int,
    private val callback: (oldDistance: Double, newDistance: Double, direction: Int) -> Unit,
) {

    /**
     * Distance settings are converted from dp to pixels before reaching this helper. Very small
     * values can be truncated to zero, and corrupted values can be negative. Either case would
     * make the loop in [add] unable to make progress, so fall back to one pixel.
     */
    private val unitDistance = unitDistance.coerceAtLeast(1)

    /**
     * total distance scrolled
     */
    private var scrolledDistance: Double = 0.0

    /**
     * add a scrolled distance to the total.
     * if the [unitDistance] is reached, this function will also invoke the callback
     *
     * @param distance the distance to add
     */
    fun add(distance: Double) {
        // A non-finite delta would make the loop below run forever because subtracting a finite
        // unit from infinity never changes its magnitude.
        if (!distance.isFinite()) {
            reset()
            return
        }

        scrolledDistance += distance
        if (!scrolledDistance.isFinite()) {
            reset()
            return
        }

        // invoke the callback if we scrolled far enough
        while (abs(scrolledDistance) >= unitDistance) {
            val oldDistance = scrolledDistance
            subtractUnitDistance()
            callback.invoke(
                oldDistance,
                scrolledDistance,
                sign(scrolledDistance).toInt(),
            )
        }
    }

    /**
     * reset the distance scrolled to zero
     */
    fun reset() {
        scrolledDistance = 0.0
    }

    /**
     * subtract the [unitDistance] from the total [scrolledDistance]
     */
    private fun subtractUnitDistance() {
        scrolledDistance -= (unitDistance * sign(scrolledDistance))
    }
}

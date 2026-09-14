package app.morphe.extension.youtube.swipecontrols.controller

import android.app.Activity
import android.view.View
import app.morphe.extension.shared.utils.ResourceType
import app.morphe.extension.shared.utils.ResourceUtils.getIdentifier
import app.morphe.extension.shared.utils.Utils.dipToPixels
import app.morphe.extension.youtube.swipecontrols.SwipeControlsConfigurationProvider
import app.morphe.extension.youtube.swipecontrols.misc.Rectangle
import kotlin.math.max
import kotlin.math.min

/**
 * Y- Axis:
 * -------- 0
 *        ^
 * dead   | 40dp
 *        v
 * -------- yDeadTop
 *        ^
 * swipe  |
 *        v
 * -------- yDeadBtm
 *        ^
 * dead   | 80dp
 *        v
 * -------- screenHeight
 *
 * X- Axis:
 *  0    xLeftStart    xLeftEnd    xRightStart    xRightEnd   screenWidth
 *  |          |            |          |            |          |
 *  |   20dp   |  zoneWidth |  center  |  zoneWidth |   20dp   |
 *  | <------> |  <------>  | <------> |  <------>  | <------> |
 *  |   dead   | left zone  |  stock   | right zone |   dead   |
 *             | <--------------------------------> |
 *                              1/1
 *
 * Horizontal swipes use the top and bottom edges of the effective rectangle. The percentage
 * setting controls the height of each edge zone, leaving the center as the default/stock area.
 */
@Suppress("PrivatePropertyName")
class SwipeZonesController(
    private val config: SwipeControlsConfigurationProvider,
    private val host: Activity,
    private val fallbackScreenRect: () -> Rectangle,
) {
    /**
     * 20dp, in pixels
     */
    private val _20dp = dipToPixels(20f)

    /**
     * 40dp, in pixels
     */
    private val _40dp = dipToPixels(40f)

    /**
     * 80dp, in pixels
     */
    private val _80dp = dipToPixels(80f)

    /**
     * id for R.id.inset_controls_overlay_wrapper
     */
    private val sizeAdjustableOverlayId = getIdentifier("inset_controls_overlay_wrapper", ResourceType.ID, host)

    /**
     * current bounding rectangle of the player
     */
    private var playerRect: Rectangle? = null

    /**
     * rectangle of the area that is effectively usable for swipe controls
     */
    private val effectiveSwipeRect: Rectangle
        get() {
            maybeAttachPlayerBoundsListener()
            val p = if (playerRect != null) playerRect!! else fallbackScreenRect()
            return Rectangle(
                p.x + _20dp,
                p.y + _40dp,
                p.width - _20dp,
                p.height - _20dp - _80dp,
            )
        }

    /** The rectangle of the left control zone. */
    val left: Rectangle
        get() {
            val eRect = effectiveSwipeRect
            val zoneWidth = max(0, eRect.width * config.verticalSwipeZoneSize / 100)
            return Rectangle(
                eRect.left,
                eRect.top,
                zoneWidth,
                eRect.height,
            )
        }

    /** The rectangle of the right control zone. */
    val right: Rectangle
        get() {
            val eRect = effectiveSwipeRect
            val zoneWidth = max(0, eRect.width * config.verticalSwipeZoneSize / 100)
            return Rectangle(
                eRect.right - zoneWidth,
                eRect.top,
                zoneWidth,
                eRect.height,
            )
        }

    /**
     * Additional dead zone applied *inside* the `effectiveSwipeRect` for horizontal swipes
     * to avoid conflicting with system back gestures.
     */
    private val horizontalInnerDeadZone = _40dp // Use 40dp for inner horizontal dead zone

    /** The effective starting X-coordinate for horizontal swipe zones. */
    private val horizontalZoneEffectiveLeft get() = effectiveSwipeRect.left + horizontalInnerDeadZone

    /** The effective width available for horizontal swipe zones after applying inner dead zones. */
    private val horizontalZoneEffectiveWidth get() = max(0, effectiveSwipeRect.width - (horizontalInnerDeadZone * 2))

    /** The height for each horizontal zone, measured as a percentage of the effective height. */
    private val horizontalZoneHeight
        get() = max(0, effectiveSwipeRect.height * config.horizontalSwipeZoneSize / 100)
    private val topHorizontalZoneTop get() = effectiveSwipeRect.top
    private val bottomHorizontalZoneTop get() = effectiveSwipeRect.bottom - horizontalZoneHeight

    /** The rectangle of the top control zone. */
    val top: Rectangle
        get() {
            return Rectangle(
                horizontalZoneEffectiveLeft,
                topHorizontalZoneTop,
                horizontalZoneEffectiveWidth,
                horizontalZoneHeight
            )
        }

    /** The rectangle of the bottom control zone. */
    val bottom: Rectangle
        get() {
            return Rectangle(
                horizontalZoneEffectiveLeft,
                bottomHorizontalZoneTop,
                horizontalZoneEffectiveWidth,
                horizontalZoneHeight
            )
        }

    /**
     * try to attach a listener to the size adjustable player overlay and update the player rectangle.
     * once a listener is attached, this function does nothing
     */
    private fun maybeAttachPlayerBoundsListener() {
        if (playerRect != null) return
        host.findViewById<View>(sizeAdjustableOverlayId)?.let {
            onPlayerViewLayout(it)
            it.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                onPlayerViewLayout(it)
            }
        }
    }

    /**
     * update the player rectangle on size adjustable player overlay layout
     *
     * @param sizeAdjustableOverlay the player overlay
     */
    private fun onPlayerViewLayout(sizeAdjustableOverlay: View) {
        // The overlay shrinks with the visible player surface and excludes engagement panels.
        val playerWidthWithPadding = sizeAdjustableOverlay.width + (sizeAdjustableOverlay.x.toInt() * 2)
        playerRect = Rectangle(
            sizeAdjustableOverlay.x.toInt(),
            sizeAdjustableOverlay.y.toInt(),
            min(sizeAdjustableOverlay.width, playerWidthWithPadding),
            sizeAdjustableOverlay.height,
        )
    }
}

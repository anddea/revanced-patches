package app.revanced.extension.youtube.swipecontrols.controller

import android.app.Activity
import android.util.TypedValue
import android.view.ViewGroup
import app.revanced.extension.shared.utils.ResourceUtils.ResourceType
import app.revanced.extension.shared.utils.ResourceUtils.getIdentifier
import app.revanced.extension.youtube.settings.Settings
import app.revanced.extension.youtube.swipecontrols.misc.Rectangle
import app.revanced.extension.youtube.swipecontrols.misc.applyDimension
import app.revanced.extension.youtube.utils.ExtendedUtils.validateValue
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
 * X- Axis (Vertical Controls - Brightness/Volume):
 *  0    xBrigStart    xBrigEnd    xVolStart     xVolEnd   screenWidth
 *  |          |            |          |            |          |
 *  |   20dp   |  zoneWidth |  <-----> |  zoneWidth |   20dp   |
 *  | <------> |  <------>  |   Dead   |  <------>  | <------> |
 *  |   dead   | brightness |          |   volume   |   dead   |
 *             | <--------------------------------> |
 *                         effectiveSwipeRect.width
 *
 * X- Axis (Horizontal Controls - Seek/Speed):
 *  0        effectiveSwipeRect.left + 40dp        effectiveSwipeRect.right - 40dp       screenWidth
 *  |                 |                                            |                 |
 *  |   20dp + 40dp   | <------------ swipeable area ------------> |   40dp + 20dp   |
 *  | <-------------> |                                            | <-------------> |
 *  |      dead       | Seek/Speed (top half, based on setting)    |      dead       |
 *  |      dead       | Speed/Seek (bottom half, based on setting) |      dead       |
 *                    | <----------------------------------------> |
 *                                effectiveSwipeRect.width - (2 * 40dp)
 */
@Suppress("PrivatePropertyName")
class SwipeZonesController(
    private val host: Activity,
    private val fallbackScreenRect: () -> Rectangle,
) {

    private val overlayRectSize = validateValue(
        Settings.SWIPE_OVERLAY_RECT_SIZE,
        0,
        50,
        "revanced_swipe_overlay_rect_size_invalid_toast"
    )

    /**
     * Setting to control if `Seek` and `Speed` zones are swapped vertically
     */
    private val switchSpeedAndSeek = Settings.SWIPE_SWITCH_SPEED_AND_SEEK.get()

    /**
     * 20dp, in pixels
     */
    private val _20dp = 20.applyDimension(host, TypedValue.COMPLEX_UNIT_DIP)

    /**
     * 40dp, in pixels
     */
    private val _40dp = 40.applyDimension(host, TypedValue.COMPLEX_UNIT_DIP)

    /**
     * 80dp, in pixels
     */
    private val _80dp = 80.applyDimension(host, TypedValue.COMPLEX_UNIT_DIP)

    /**
     * id for R.id.player_view
     */
    private val playerViewId = getIdentifier("player_view", ResourceType.ID, host)

    /**
     * current bounding rectangle of the player
     */
    private var playerRect: Rectangle? = null

    /**
     * rectangle of the area that is effectively usable for swipe controls,
     * after applying the initial screen-edge dead zones.
     */
    private val effectiveSwipeRect: Rectangle
        get() {
            maybeAttachPlayerBoundsListener()
            val p = playerRect ?: fallbackScreenRect()
            val effectiveLeft = p.x + _20dp
            val effectiveTop = p.y + _40dp
            // Ensure width isn't negative if _20dp * 2 > p.width
            val effectiveWidth = max(0, p.width - (_20dp * 2))
            // Ensure height isn't negative if _40dp + _80dp > p.height
            val effectiveHeight = max(0, p.height - _40dp - _80dp)

            return Rectangle(
                effectiveLeft,
                effectiveTop,
                effectiveWidth,
                effectiveHeight,
            )
        }

    /**
     * the rectangle of the volume control zone (vertical swipe)
     */
    val volume: Rectangle
        get() {
            val effectiveRect = effectiveSwipeRect // Cache for performance
            val zoneWidth = effectiveRect.width * overlayRectSize / 100
            return Rectangle(
                effectiveRect.right - zoneWidth,
                effectiveRect.top,
                zoneWidth,
                effectiveRect.height,
            )
        }

    /**
     * the rectangle of the screen brightness control zone (vertical swipe)
     */
    val brightness: Rectangle
        get() {
            val effectiveRect = effectiveSwipeRect // Cache for performance
            val zoneWidth = effectiveRect.width * overlayRectSize / 100
            return Rectangle(
                effectiveRect.left,
                effectiveRect.top,
                zoneWidth,
                effectiveRect.height,
            )
        }

    private val horizontalDeadZone = _40dp
    private val horizontalZoneEffectiveLeft get() = effectiveSwipeRect.left + horizontalDeadZone
    private val horizontalZoneEffectiveWidth get() = max(0, effectiveSwipeRect.width - (horizontalDeadZone * 2))
    private val horizontalZoneHeight get() = max(0, effectiveSwipeRect.height / 2)
    private val topHorizontalZoneTop get() = effectiveSwipeRect.top
    private val bottomHorizontalZoneTop get() = effectiveSwipeRect.top + horizontalZoneHeight

    /**
     * the rectangle of the speed control zone (horizontal swipe).
     * Position (top/bottom half) depends on [Settings.SWIPE_SWITCH_SPEED_AND_SEEK].
     * Includes additional horizontal dead zones for gestures.
     */
    val speed: Rectangle
        get() {
            val zoneTop = if (switchSpeedAndSeek) {
                // If switched, speed is in the top half
                topHorizontalZoneTop
            } else {
                // Default, speed is in the bottom half
                bottomHorizontalZoneTop
            }

            return Rectangle(
                horizontalZoneEffectiveLeft,
                zoneTop,
                horizontalZoneEffectiveWidth,
                horizontalZoneHeight
            )
        }

    /**
     * the rectangle of the seek control zone (horizontal swipe).
     * Position (top/bottom half) depends on [Settings.SWIPE_SWITCH_SPEED_AND_SEEK].
     * Includes additional horizontal dead zones for gestures.
     */
    val seek: Rectangle
        get() {
            val zoneTop = if (switchSpeedAndSeek) {
                // If switched, seek is in the bottom half
                bottomHorizontalZoneTop
            } else {
                // Default, seek is in the top half
                topHorizontalZoneTop
            }

            return Rectangle(
                horizontalZoneEffectiveLeft,
                zoneTop,
                horizontalZoneEffectiveWidth,
                horizontalZoneHeight
            )
        }

    /**
     * try to attach a listener to the player_view and update the player rectangle.
     * once a listener is attached, this function does nothing
     */
    private fun maybeAttachPlayerBoundsListener() {
        if (playerRect != null) return
        host.findViewById<ViewGroup>(playerViewId)?.let {
            onPlayerViewLayout(it) // Get initial layout
            // Add listener for subsequent layout changes
            it.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                // Only update if bounds actually changed to avoid unnecessary recalculations
                if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
                    onPlayerViewLayout(it)
                }
            }
        }
    }

    /**
     * update the player rectangle on player_view layout
     *
     * @param playerView the player view
     */
    private fun onPlayerViewLayout(playerView: ViewGroup) {
        playerView.getChildAt(0)?.let { playerSurface ->
            // the player surface is centered in the player view
            // figure out the width of the surface including the padding (same on the left and right side)
            // and use that width for the player rectangle size
            // this automatically excludes any engagement panel from the rect
            val playerWidthWithPadding = playerSurface.width + (playerSurface.x.toInt() * 2)
            // Use the minimum of the view's width and the calculated surface width with padding
            // This handles cases where the surface + padding might theoretically exceed the view bounds
            val actualWidth = min(playerView.width, playerWidthWithPadding)
            // Ensure coordinates and dimensions are non-negative
            // Using playerView.left/top is more robust than playerView.x/y if the view is nested
            val viewX = max(0, playerView.left)
            val viewY = max(0, playerView.top)
            val viewHeight = max(0, playerView.height)

            playerRect = Rectangle(
                viewX,
                viewY,
                max(0, actualWidth), // Ensure width is not negative
                viewHeight,
            )
        } ?: run {
            // Fallback if playerSurface is not available (e.g., during initialization)
            // Use playerView bounds directly, applying max(0, ...)
            playerRect = Rectangle(
                max(0, playerView.left),
                max(0, playerView.top),
                max(0, playerView.width),
                max(0, playerView.height),
            )
        }
    }
}

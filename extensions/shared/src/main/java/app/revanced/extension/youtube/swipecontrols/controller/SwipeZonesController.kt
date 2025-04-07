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
 *  0    xBrigStart    xBrigEnd    xVolStart     xVolEnd   playerRect.width (dynamic)
 *  |          |            |          |            |          |
 *  |   20dp   |  zoneWidth |  <-----> |  zoneWidth |   20dp   |
 *  | <------> |  <------>  |   Dead   |  <------>  | <------> |
 *  |   dead   | brightness |          |   volume   |   dead   |
 *             | <--------------------------------> |
 *                         effectiveSwipeRect.width
 *
 * X- Axis (Horizontal Controls - Seek/Speed):
 *  playerRect.x + 20dp + 40dp        playerRect.right - 20dp - 40dp       playerRect.right
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
     * after applying the initial screen-edge dead zones (top, bottom, left, right).
     * This rectangle shrinks dynamically if the playerRect shrinks.
     */
    private val effectiveSwipeRect: Rectangle
        get() {
            maybeAttachPlayerBoundsListener()
            val p = playerRect ?: fallbackScreenRect()

            val effectiveLeft = p.x + _20dp
            val effectiveTop = p.y + _40dp
            // Ensure width isn't negative: Base Width - Left Dead Zone - Right Dead Zone
            val effectiveWidth = max(0, p.width - (_20dp * 2))
            // Ensure height isn't negative: Base Height - Top Dead Zone - Bottom Dead Zone
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

    /**
     * Additional dead zone applied *inside* the `effectiveSwipeRect` for horizontal swipes
     * to avoid conflicting with system back gestures.
     */
    private val horizontalInnerDeadZone = _40dp // Use 40dp for inner horizontal dead zone

    /** The effective starting X-coordinate for horizontal swipe zones (Seek/Speed). */
    private val horizontalZoneEffectiveLeft get() = effectiveSwipeRect.left + horizontalInnerDeadZone

    /** The effective width available for horizontal swipe zones after applying inner dead zones. */
    private val horizontalZoneEffectiveWidth get() = max(0, effectiveSwipeRect.width - (horizontalInnerDeadZone * 2))

    /** The height for each horizontal zone (top/bottom half). */
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
     * Tries to attach a listener to the player_view and update the player rectangle.
     * Once a listener is attached, this function does nothing more.
     * Uses a flag to prevent attaching multiple listeners.
     */
    private var listenerAttached = false
    private val listenerLock = Any() // Lock for thread safety when attaching listener

    private fun maybeAttachPlayerBoundsListener() {
        // Quick check without lock first
        if (listenerAttached) return

        synchronized(listenerLock) {
            // Double-check after acquiring lock
            if (listenerAttached) return

            host.findViewById<ViewGroup>(playerViewId)?.let { playerViewGroup ->
                onPlayerViewLayout(playerViewGroup) // Get initial layout immediately

                // Add listener for subsequent layout changes
                playerViewGroup.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
                    // Always recalculate playerRect based on the playerSurface's potential changes,
                    // even if the playerView's own bounds haven't changed. This handles internal panels correctly.
                    // We expect view to be the playerViewGroup we attached the listener to.
                    if (view is ViewGroup) {
                        onPlayerViewLayout(view)
                    }
                }
                listenerAttached = true
            }
        }
    }

    /**
     * Updates the `playerRect` based on the current layout of the `player_view` and its first child (`playerSurface`).
     * This calculation determines the actual available area for the video content, excluding overlays like description panels.
     *
     * @param playerView the player view
     */
    private fun onPlayerViewLayout(playerView: ViewGroup) {
        // Try to get the actual video surface view (usually the first child)
        playerView.getChildAt(0)?.let { playerSurface ->
            // Calculate the effective width occupied by the player surface, including any horizontal padding/margins
            // that center it within the playerView. This correctly accounts for side panels shrinking the surface.
            // playerSurface.x is the offset of the surface relative to its parent (playerView).
            // Assuming padding is equal on both sides if centered.
            val playerWidthWithPadding = playerSurface.width + (playerSurface.x.toInt() * 2)

            // The actual width used for the playerRect should be the *minimum* of the container's width
            // and the calculated surface width including padding. This prevents the rect from exceeding the container.
            val actualWidth = min(playerView.width, playerWidthWithPadding)

            // Use playerView's screen coordinates (left/top) for robustness with nested layouts.
            // Ensure all coordinates and dimensions are non-negative.
            val viewX = max(0, playerView.left)
            val viewY = max(0, playerView.top)
            val viewHeight = max(0, playerView.height)

            playerRect = Rectangle(
                viewX,
                viewY,
                max(0, actualWidth),
                viewHeight,
            )
        } ?: run {
            // Fallback: If playerSurface (child 0) isn't available (e.g., during initialization phases),
            // use the playerView's bounds directly as a less accurate estimate.
            playerRect = Rectangle(
                max(0, playerView.left),
                max(0, playerView.top),
                max(0, playerView.width),
                max(0, playerView.height),
            )
        }
    }
}

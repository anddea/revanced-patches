package app.morphe.extension.youtube.swipecontrols.controller

import android.app.Activity
import android.view.ViewGroup
import app.morphe.extension.shared.utils.ResourceUtils.ResourceType
import app.morphe.extension.shared.utils.ResourceUtils.getIdentifier
import app.morphe.extension.shared.utils.Utils.dipToPixels
import app.morphe.extension.youtube.settings.Settings
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
 *  0    xBrigStart    xBrigEnd    xVolStart     xVolEnd   screenWidth
 *  |          |            |          |            |          |
 *  |   20dp   |  zoneWidth |  others  |  zoneWidth |   20dp   |
 *  | <------> |  <------>  | <------> |  <------>  | <------> |
 *  |   dead   | brightness |   dead   |   volume   |   dead   |
 *             | <--------------------------------> |
 *                              1/1
 */
@Suppress("PrivatePropertyName")
class SwipeZonesController(
    private val config: SwipeControlsConfigurationProvider,
    private val host: Activity,
    private val fallbackScreenRect: () -> Rectangle,
) {
    /**
     * Setting to control if `Seek` and `Speed` zones are swapped vertically
     */
    private val switchSpeedAndSeek = Settings.SWIPE_SWITCH_SPEED_AND_SEEK.get()

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
     * id for R.id.player_view
     */
    private val playerViewId = getIdentifier("player_view", ResourceType.ID, host)

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

    /**
     * the rectangle of the volume control zone
     */
    val volume: Rectangle
        get() {
            val eRect = effectiveSwipeRect
            val zoneWidth = eRect.width * config.overlayRectSize / 100
            return Rectangle(
                eRect.right - zoneWidth,
                eRect.top,
                zoneWidth,
                eRect.height,
            )
        }

    /**
     * the rectangle of the screen brightness control zone
     */
    val brightness: Rectangle
        get() {
            val eRect = effectiveSwipeRect
            val zoneWidth = eRect.width * config.overlayRectSize / 100
            return Rectangle(
                eRect.left,
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
     * try to attach a listener to the player_view and update the player rectangle.
     * once a listener is attached, this function does nothing
     */
    private fun maybeAttachPlayerBoundsListener() {
        if (playerRect != null) return
        host.findViewById<ViewGroup>(playerViewId)?.let {
            onPlayerViewLayout(it)
            it.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                onPlayerViewLayout(it)
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
            playerRect = Rectangle(
                playerView.x.toInt(),
                playerView.y.toInt(),
                min(playerView.width, playerWidthWithPadding),
                playerView.height,
            )
        }
    }
}

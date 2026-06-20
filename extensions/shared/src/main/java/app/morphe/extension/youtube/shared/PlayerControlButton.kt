package app.morphe.extension.youtube.shared

import android.view.View
import android.widget.ImageView
import app.morphe.extension.shared.utils.Logger
import app.morphe.extension.shared.utils.ResourceUtils
import app.morphe.extension.shared.utils.Utils
import java.lang.ref.WeakReference


class PlayerControlButton(
    controlsViewGroup: View,
    imageViewButtonId: String,
    hasPlaceholder: Boolean = true,
    buttonVisibility: PlayerControlButtonVisibility,
    onClickListener: View.OnClickListener,
    onLongClickListener: View.OnLongClickListener? = null,
) {
    fun interface PlayerControlButtonVisibility {
        /**
         * @return If the button should be shown when the player overlay is visible.
         */
        fun shouldBeShown(): Boolean
    }

    private val buttonRef: WeakReference<ImageView?>

    private val placeholderExists: Boolean

    /**
     * Empty view with the same layout size as the button. Used to fill empty space while the
     * fade out animation runs. Without this the chapter titles overlapping the button when fading out.
     */
    private val placeHolderRef: WeakReference<View?>
    private val visibilityCheck: PlayerControlButtonVisibility
    private var isVisible: Boolean
    private var lastTimeSetVisible: Long = 0L

    init {
        val imageView =
            Utils.getChildViewByResourceName<ImageView>(controlsViewGroup, imageViewButtonId)
        imageView.visibility = View.GONE

        var tempPlaceholder: View? = null
        if (hasPlaceholder) {
            tempPlaceholder =
                Utils.getChildViewByResourceName(
                    controlsViewGroup,
                    "${imageViewButtonId}_placeholder"
                )
            tempPlaceholder.visibility = View.GONE
        }
        placeholderExists = hasPlaceholder
        placeHolderRef = WeakReference<View?>(tempPlaceholder)

        imageView.setOnClickListener(onClickListener)
        if (onLongClickListener != null) {
            imageView.setOnLongClickListener(onLongClickListener)
        }

        visibilityCheck = buttonVisibility
        buttonRef = WeakReference<ImageView?>(imageView)
        isVisible = false

        // Update the visibility after the player type changes.
        // This ensures that button animations are cleared and their states are updated correctly
        // when switching between states like minimized, maximized, or fullscreen, preventing
        // "stuck" animations or incorrect visibility.  Without this fix the issue is most noticeable
        // when maximizing type 3 miniplayer.
        PlayerType.onChange.addObserver { type: PlayerType ->
            playerTypeChanged(type)
        }
    }

    fun imageView() = buttonRef.get()

    fun setVisibilityNegatedImmediate() {
        try {
            Utils.verifyOnMainThread()
            if (PlayerControlsVisibility.current != PlayerControlsVisibility.PLAYER_CONTROLS_VISIBILITY_HIDDEN) {
                return
            }

            val shouldBeShown = visibilityCheck.shouldBeShown()
            if (!shouldBeShown) return
            val button = buttonRef.get() ?: return
            isVisible = false

            val placeholder = placeHolderRef.get()

            val animate = button.animate()
            animate.cancel()

            // If the overlay is tapped to display then immediately tapped to dismiss
            // before the fade in animation finishes, then the fade out animation is
            // the time between when the fade in started and now.
            val animationDuration =
                fadeInDuration.toLong().coerceAtMost(System.currentTimeMillis() - lastTimeSetVisible)
            if (animationDuration <= 0) {
                button.visibility = View.GONE
                placeholder?.visibility = View.VISIBLE
                return
            }

            animate.alpha(0f)
                .setDuration(animationDuration)
                .withEndAction {
                    button.visibility = View.GONE
                    placeholder?.visibility = View.VISIBLE
                }
                .start()
        } catch (ex: Exception) {
            Logger.printException({ "setVisibilityNegatedImmediate failure" }, ex)
        }
    }

    fun setVisibilityImmediate(visible: Boolean) {
        if (visible) {
            // Fix button flickering, by pushing this call to the back of
            // the main thread and letting other layout code run first.
            Utils.runOnMainThread { privateSetVisibility(visible = true, animated = false) }
        } else {
            privateSetVisibility(visible = false, animated = false)
        }
    }

    fun setVisibility(visible: Boolean, animated: Boolean) {
        // Ignore this call, otherwise with full screen thumbnails the buttons are visible while seeking.
        if (visible && !animated) return

        privateSetVisibility(visible, animated)
    }

    private fun privateSetVisibility(visible: Boolean, animated: Boolean) {
        try {
            if (isVisible == visible) return
            isVisible = visible

            val button = buttonRef.get() ?: return
            val placeholder = placeHolderRef.get()
            val shouldBeShown = visibilityCheck.shouldBeShown()

            if (visible) {
                lastTimeSetVisible = System.currentTimeMillis()
            }

            if (visible && shouldBeShown) {
                val animate = button.animate()
                animate.cancel()
                button.visibility = View.VISIBLE

                if (animated) {
                    button.alpha = 0f
                    animate.alpha(1f)
                        .setDuration(fadeInDuration.toLong())
                        .start()
                } else {
                    button.alpha = 1f
                }

                placeholder?.visibility = View.GONE
            } else {
                val animate = button.animate()
                animate.cancel()

                val placeholderVisibility = if (shouldBeShown) View.VISIBLE else View.GONE

                if (animated && button.visibility == View.VISIBLE) {
                    placeholder?.visibility = View.GONE
                    animate.alpha(0f)
                        .setDuration(fadeOutDuration.toLong())
                        .withEndAction {
                            button.visibility = View.GONE
                            placeholder?.visibility = placeholderVisibility
                        }
                        .start()
                } else {
                    button.visibility = View.GONE
                    placeholder?.visibility = placeholderVisibility
                }
            }
        } catch (ex: Exception) {
            Logger.printException({ "privateSetVisibility failure" }, ex)
        }
    }

    /**
     * Synchronizes the button state after the player state changes.
     */
    private fun playerTypeChanged(newType: PlayerType) {
        if (newType != PlayerType.WATCH_WHILE_MINIMIZED && !newType.isMaximizedOrFullscreen()) {
            return
        }

        val button = buttonRef.get() ?: return

        button.animate().cancel()
        val placeholder = placeHolderRef.get()

        if (visibilityCheck.shouldBeShown()) {
            if (isVisible) {
                button.visibility = View.VISIBLE
                button.alpha = 1f
                placeholder?.visibility = View.GONE
            } else {
                button.visibility = View.GONE
                placeholder?.visibility = View.VISIBLE
            }
        } else {
            button.visibility = View.GONE
            placeholder?.visibility = View.GONE
        }
    }


    fun hide() {
        if (!isVisible) return

        Utils.verifyOnMainThread()
        val view = buttonRef.get() ?: return
        view.animate().cancel()
        view.visibility = View.GONE

        val placeholder = placeHolderRef.get()
        placeholder?.visibility = View.GONE
        isVisible = false
    }

    companion object {
        private val fadeInDuration: Int = ResourceUtils.getInteger("fade_duration_fast")
        private val fadeOutDuration: Int = ResourceUtils.getInteger("fade_duration_scheduled")
    }
}

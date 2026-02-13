package app.morphe.extension.youtube.shared

import android.view.View
import android.view.animation.Animation
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

    init {
        val imageView =
            Utils.getChildViewByResourceName<ImageView>(controlsViewGroup, imageViewButtonId)
        imageView.visibility = View.GONE

        var tempPlaceholder: View? = null
        if (hasPlaceholder) {
            tempPlaceholder =
                Utils.getChildViewByResourceName<View>(
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
        // "stuck" animations or incorrect visibility.  Without this fix the issue is most noticable
        // when maximizing type 3 miniplayer.
        PlayerType.Companion.onChange.addObserver { type: PlayerType ->
            playerTypeChanged(type)
        }
    }

    fun imageView() = buttonRef.get()

    fun setVisibilityNegatedImmediate() {
        if (PlayerControlsVisibility.current != PlayerControlsVisibility.PLAYER_CONTROLS_VISIBILITY_HIDDEN) {
            return
        }

        val shouldBeShown = visibilityCheck.shouldBeShown()
        if (!shouldBeShown) return
        val button = buttonRef.get()
        if (button == null) return
        isVisible = false

        button.clearAnimation()
        button.startAnimation(fadeOutImmediate)
        button.visibility = View.GONE

        val placeholder = placeHolderRef.get()
        placeholder?.visibility = View.VISIBLE
    }

    fun setVisibilityImmediate(visible: Boolean) {
        if (visible) {
            if (placeholderExists) {
                // Fix button flickering, by pushing this call to the back of
                // the main thread and letting other layout code run first.
                Utils.runOnMainThread { privateSetVisibility(visible = true, animated = false) }
            } else {
                // Top buttons do not overlap with chapter titles.
                privateSetVisibility(visible = true, animated = false)
            }
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

            val button = buttonRef.get()
            if (button == null) return

            val placeholder = placeHolderRef.get()
            val shouldBeShown = visibilityCheck.shouldBeShown()

            if (visible && shouldBeShown) {
                button.clearAnimation()
                if (animated) {
                    button.startAnimation(fadeInAnimation)
                }
                button.visibility = View.VISIBLE

                placeholder?.visibility = View.GONE
            } else {
                if (button.visibility == View.VISIBLE) {
                    button.clearAnimation()
                    if (animated) {
                        button.startAnimation(fadeOutAnimation)
                    }
                    button.visibility = View.GONE
                }

                placeholder?.visibility = if (shouldBeShown)
                    View.VISIBLE
                else
                    View.GONE
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

        val button = buttonRef.get()
        if (button == null) return

        button.clearAnimation()
        val placeholder = placeHolderRef.get()

        if (visibilityCheck.shouldBeShown()) {
            if (isVisible) {
                button.visibility = View.VISIBLE
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
        var view: View? = buttonRef.get()
        if (view == null) return
        view.visibility = View.GONE

        view = placeHolderRef.get()
        view?.visibility = View.GONE
        isVisible = false
    }

    companion object {
        private val fadeInDuration: Int = ResourceUtils.getInteger("fade_duration_fast")
        private val fadeOutDuration: Int = ResourceUtils.getInteger("fade_duration_scheduled")

        private val fadeInAnimation = ResourceUtils.getAnimation("fade_in")
        private val fadeOutAnimation: Animation?
        private val fadeOutImmediate: Animation?

        init {
            fadeInAnimation?.duration = fadeInDuration.toLong()

            fadeOutAnimation = ResourceUtils.getAnimation("fade_out")
            fadeOutAnimation?.duration = fadeOutDuration.toLong()

            fadeOutImmediate = ResourceUtils.getAnimation("abc_fade_out")
            fadeOutImmediate?.duration = ResourceUtils.getInteger("fade_duration_fast").toLong()
        }
    }
}

package app.morphe.extension.youtube.patches.overlaybutton

import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.View
import android.widget.ImageView
import androidx.core.graphics.toColorInt
import app.morphe.extension.shared.utils.Logger
import app.morphe.extension.shared.utils.StringRef.str
import app.morphe.extension.shared.utils.Utils.showToastShort
import app.morphe.extension.youtube.settings.Settings
import app.morphe.extension.youtube.shared.PlayerControlButton
import app.morphe.extension.youtube.shared.RootView.isAdProgressTextVisible

@Suppress("unused")
object AlwaysRepeatButton {
    private val alwaysRepeat = Settings.ALWAYS_REPEAT
    private val alwaysRepeatPause = Settings.ALWAYS_REPEAT_PAUSE
    private val cf: ColorFilter =
        PorterDuffColorFilter("#fffffc79".toColorInt(), PorterDuff.Mode.SRC_ATOP)
    private var instance: PlayerControlButton? = null

    /**
     * Injection point.
     */
    @JvmStatic
    fun initializeButton(controlsView: View) {
        try {
            instance = PlayerControlButton(
                controlsViewGroup = controlsView,
                imageViewButtonId = "revanced_always_repeat_button",
                buttonVisibility = { isButtonEnabled() },
                onClickListener = { view: View -> onClick(view) },
                onLongClickListener = { view: View ->
                    onLongClick(view)
                    true
                }
            )
            instance?.changeSelected(selected = alwaysRepeat.get())
            instance?.setColorFilter(selected = alwaysRepeatPause.get())
        } catch (ex: Exception) {
            Logger.printException({ "initializeButton failure" }, ex)
        }
    }

    /**
     * injection point
     */
    @JvmStatic
    fun setVisibilityNegatedImmediate() {
        instance?.setVisibilityNegatedImmediate()
    }

    /**
     * injection point
     */
    @JvmStatic
    fun setVisibilityImmediate(visible: Boolean) {
        instance?.setVisibilityImmediate(visible)
    }

    /**
     * injection point
     */
    @JvmStatic
    fun setVisibility(visible: Boolean, animated: Boolean) {
        instance?.setVisibility(visible, animated)
    }

    private fun isButtonEnabled(): Boolean {
        return Settings.OVERLAY_BUTTON_ALWAYS_REPEAT.get()
                && !isAdProgressTextVisible()
    }

    private fun onClick(view: View) {
        instance?.changeSelected(!view.isSelected)
    }

    private fun onLongClick(view: View) {
        instance?.changeColorFilter()
    }

    private fun PlayerControlButton.changeColorFilter() {
        val imageView: ImageView? = imageView()
        if (imageView == null) return

        imageView.isSelected = true
        alwaysRepeat.save(true)

        val newValue: Boolean = !alwaysRepeatPause.get()
        alwaysRepeatPause.save(newValue)
        setColorFilter(imageView, newValue)
    }

    private fun PlayerControlButton.changeSelected(selected: Boolean) {
        val imageView: ImageView? = imageView()
        if (imageView == null) return

        if (imageView.colorFilter === cf) {
            showToastShort(str("revanced_overlay_button_not_allowed_warning"))
            return
        }

        imageView.isSelected = selected
        alwaysRepeat.save(selected)
    }

    private fun PlayerControlButton.setColorFilter(
        imageView: ImageView? = imageView(),
        selected: Boolean
    ) {
        if (selected) imageView?.colorFilter = cf
        else imageView?.clearColorFilter()
    }

}
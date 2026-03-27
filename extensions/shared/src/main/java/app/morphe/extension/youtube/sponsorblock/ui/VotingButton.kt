package app.morphe.extension.youtube.sponsorblock.ui

import android.view.View
import app.morphe.extension.shared.utils.Logger
import app.morphe.extension.youtube.settings.Settings
import app.morphe.extension.youtube.shared.PlayerControlButton
import app.morphe.extension.youtube.shared.RootView.isAdProgressTextVisible
import app.morphe.extension.youtube.sponsorblock.SegmentPlaybackController
import app.morphe.extension.youtube.sponsorblock.SponsorBlockUtils

object VotingButton {
    private var instance: PlayerControlButton? = null

    /**
     * injection point
     */
    @JvmStatic
    fun initializeButton(controlsView: View) {
        try {
            instance = PlayerControlButton(
                controlsViewGroup = controlsView,
                imageViewButtonId = "revanced_sb_voting_button",
                hasPlaceholder = false,
                buttonVisibility = { isButtonEnabled() },
                onClickListener = { view: View -> onClick(view) }
            )
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
        return Settings.SB_ENABLED.get() && Settings.SB_VOTING_BUTTON.get()
                && SegmentPlaybackController.videoHasSegments()
                && !isAdProgressTextVisible()
    }

    private fun onClick(view: View) {
        SponsorBlockUtils.onVotingClicked(view.context)
    }

    @JvmStatic
    fun hideControls() {
        instance?.hide()
    }
}

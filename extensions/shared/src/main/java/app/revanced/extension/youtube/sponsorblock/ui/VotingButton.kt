package app.revanced.extension.youtube.sponsorblock.ui

import android.view.View
import app.revanced.extension.shared.utils.Logger
import app.revanced.extension.youtube.settings.Settings
import app.revanced.extension.youtube.shared.PlayerControlButton
import app.revanced.extension.youtube.shared.RootView.isAdProgressTextVisible
import app.revanced.extension.youtube.sponsorblock.SegmentPlaybackController
import app.revanced.extension.youtube.sponsorblock.SponsorBlockUtils

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

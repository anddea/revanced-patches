package app.revanced.extension.youtube.shared

import app.revanced.extension.shared.utils.Logger

/**
 * FullscreenEngagementPanelState fullscreen engagement panel state.
 */
enum class FullscreenEngagementPanelState {
    ATTACHED,
    DETACHED;

    companion object {

        @JvmStatic
        fun set(enum: FullscreenEngagementPanelState) {
            current = enum
        }

        /**
         * The current fullscreen engagement panel state.
         */
        @JvmStatic
        var current
            get() = currentFullscreenEngagementPanelState
            private set(type) {
                if (currentFullscreenEngagementPanelState != type) {
                    Logger.printDebug { "Changed to: $type" }

                    currentFullscreenEngagementPanelState = type
                }
            }

        @Volatile // Read/write from different threads.
        private var currentFullscreenEngagementPanelState = DETACHED
    }

    /**
     * Check if the fullscreen engagement panel is [ATTACHED].
     */
    fun isAttached(): Boolean {
        return this == ATTACHED
    }
}
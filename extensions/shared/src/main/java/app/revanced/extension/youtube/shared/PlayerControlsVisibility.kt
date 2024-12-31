package app.revanced.extension.youtube.shared

import app.revanced.extension.shared.utils.Logger

/**
 * PlayerControls visibility state.
 */
enum class PlayerControlsVisibility {
    PLAYER_CONTROLS_VISIBILITY_UNKNOWN,
    PLAYER_CONTROLS_VISIBILITY_WILL_HIDE,
    PLAYER_CONTROLS_VISIBILITY_HIDDEN,
    PLAYER_CONTROLS_VISIBILITY_WILL_SHOW,
    PLAYER_CONTROLS_VISIBILITY_SHOWN;

    companion object {

        private val nameToPlayerControlsVisibility = entries.associateBy { it.name }

        @JvmStatic
        fun setFromString(enumName: String) {
            val state = nameToPlayerControlsVisibility[enumName]
            if (state == null) {
                Logger.printException { "Unknown PlayerControlsVisibility encountered: $enumName" }
            } else if (currentPlayerControlsVisibility != state) {
                Logger.printDebug { "PlayerControlsVisibility changed to: $state" }
                currentPlayerControlsVisibility = state
            }
        }

        /**
         * Depending on which hook this is called from,
         * this value may not be up to date with the actual playback state.
         */
        @JvmStatic
        var current: PlayerControlsVisibility?
            get() = currentPlayerControlsVisibility
            private set(value) {
                currentPlayerControlsVisibility = value
            }

        @Volatile // Read/write from different threads.
        private var currentPlayerControlsVisibility: PlayerControlsVisibility? = null
    }
}
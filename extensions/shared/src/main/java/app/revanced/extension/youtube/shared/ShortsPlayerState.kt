package app.revanced.extension.youtube.shared

import app.revanced.extension.shared.utils.Event
import app.revanced.extension.shared.utils.Logger

/**
 * ShortsPlayerState shorts player state.
 */
enum class ShortsPlayerState {
    CLOSED,
    OPEN;

    companion object {

        @JvmStatic
        fun set(enum: ShortsPlayerState) {
            if (current != enum) {
                Logger.printDebug { "ShortsPlayerState changed to: ${enum.name}" }
                current = enum
            }
        }

        /**
         * The current shorts player state.
         */
        @JvmStatic
        var current
            get() = currentShortsPlayerState
            private set(value) {
                currentShortsPlayerState = value
                onChange(value)
            }

        @Volatile // value is read/write from different threads
        private var currentShortsPlayerState = CLOSED

        /**
         * shorts player state change listener
         */
        @JvmStatic
        val onChange = Event<ShortsPlayerState>()
    }

    /**
     * Check if the shorts player is [CLOSED].
     * Useful for checking if a shorts player is closed.
     */
    fun isClosed(): Boolean {
        return this == CLOSED
    }
}
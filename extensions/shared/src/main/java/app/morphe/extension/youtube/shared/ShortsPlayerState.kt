package app.morphe.extension.youtube.shared

import app.morphe.extension.shared.utils.Event
import app.morphe.extension.shared.utils.Logger

/**
 * ShortsPlayerState shorts player state.
 */
enum class ShortsPlayerState {
    CLOSED,
    OPEN;

    companion object {

        @JvmStatic
        fun set(enum: ShortsPlayerState) {
            current = enum
        }

        /**
         * The current shorts player state.
         */
        @JvmStatic
        var current
            get() = currentShortsPlayerState
            private set(type) {
                if (currentShortsPlayerState != type) {
                    Logger.printDebug { "Changed to: $type" }

                    currentShortsPlayerState = type
                    onChange(type)
                }
            }

        @Volatile // Read/write from different threads.
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

    /**
     * Check if the shorts player is [OPEN].
     * Useful for checking if a shorts player is open.
     */
    fun isOpen(): Boolean {
        return this == OPEN
    }
}
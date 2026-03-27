package app.morphe.extension.youtube.shared

import app.morphe.extension.shared.utils.Event
import app.morphe.extension.shared.utils.Logger

/**
 * BottomSheetState bottom sheet state.
 */
enum class BottomSheetState {
    CLOSED,
    OPEN;

    companion object {

        @JvmStatic
        fun set(enum: BottomSheetState) {
            current = enum
        }

        /**
         * The current bottom sheet state.
         */
        @JvmStatic
        var current
            get() = currentBottomSheetState
            private set(type) {
                if (currentBottomSheetState != type) {
                    Logger.printDebug { "Changed to: $type" }

                    currentBottomSheetState = type
                    onChange(type)
                }
            }

        @Volatile // Read/write from different threads.
        private var currentBottomSheetState = CLOSED

        /**
         * bottom sheet state change listener
         */
        @JvmStatic
        val onChange = Event<BottomSheetState>()
    }

    /**
     * Check if the bottom sheet is [OPEN].
     * Useful for checking if a bottom sheet is open.
     */
    fun isOpen(): Boolean {
        return this == OPEN
    }
}
package app.morphe.extension.youtube.shared

import app.morphe.extension.shared.utils.Event
import app.morphe.extension.shared.utils.Logger

/**
 * LockModeState.
 */
enum class LockModeState {
    LOCK_MODE_STATE_ENUM_UNKNOWN,
    LOCK_MODE_STATE_ENUM_UNLOCKED,
    LOCK_MODE_STATE_ENUM_LOCKED,
    LOCK_MODE_STATE_ENUM_CAN_UNLOCK,
    LOCK_MODE_STATE_ENUM_UNLOCK_EXPANDED,
    LOCK_MODE_STATE_ENUM_LOCKED_TEMPORARY_SUSPENSION;

    companion object {

        private val nameToLockModeState = entries.associateBy { it.name }

        @JvmStatic
        fun setFromString(enumName: String) {
            val newType = nameToLockModeState[enumName]
            if (newType == null) {
                Logger.printException { "Unknown LockModeState encountered: $enumName" }
            } else if (current != newType) {
                Logger.printDebug { "LockModeState changed to: $newType" }
                current = newType
            }
        }

        /**
         * The current lock mode state.
         */
        @JvmStatic
        var current
            get() = currentLockModeState
            private set(value) {
                currentLockModeState = value
                onChange(value)
            }

        @Volatile // Read/write from different threads.
        private var currentLockModeState = LOCK_MODE_STATE_ENUM_UNKNOWN

        /**
         * player type change listener
         */
        @JvmStatic
        val onChange = Event<LockModeState>()
    }

    fun isLocked(): Boolean {
        return this == LOCK_MODE_STATE_ENUM_LOCKED || this == LOCK_MODE_STATE_ENUM_UNLOCK_EXPANDED
    }
}
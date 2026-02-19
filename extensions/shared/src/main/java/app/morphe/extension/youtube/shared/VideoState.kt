package app.morphe.extension.youtube.shared

import app.morphe.extension.shared.utils.Logger
import java.util.concurrent.CopyOnWriteArrayList

/**
 * VideoState playback state.
 */
enum class VideoState {
    NEW,
    PLAYING,
    PAUSED,
    RECOVERABLE_ERROR,
    UNRECOVERABLE_ERROR,
    ENDED;

    companion object {

        private val nameToVideoState = entries.associateBy { it.name }

        private val onPlayingListeners = CopyOnWriteArrayList<Runnable>()
        private val onNotPlayingListeners = CopyOnWriteArrayList<Runnable>()

        /** Add a listener that is run when state changes to PLAYING. Used e.g. by VOT to resume translation. */
        @JvmStatic
        fun addOnPlayingListener(listener: Runnable) {
            onPlayingListeners.add(listener)
        }

        /** Add a listener that is run when state changes to non-PLAYING (PAUSED, ENDED, etc). Used e.g. by VOT to pause translation immediately. */
        @JvmStatic
        fun addOnNotPlayingListener(listener: Runnable) {
            onNotPlayingListeners.add(listener)
        }

        @JvmStatic
        fun setFromString(enumName: String) {
            val state = nameToVideoState[enumName]
            current = state
        }

        /**
         * Depending on which hook this is called from,
         * this value may not be up to date with the actual playback state.
         */
        @JvmStatic
        var current
            get() = currentVideoState
            private set(type) {
                if (currentVideoState != type) {
                    Logger.printDebug { "Changed to: $type" }
                    currentVideoState = type
                    if (type == PLAYING) {
                        for (listener in onPlayingListeners) {
                            try {
                                listener.run()
                            } catch (e: Exception) {
                                Logger.printException { "OnPlaying listener error: ${e.message}" }
                            }
                        }
                    } else if (type != null) {
                        for (listener in onNotPlayingListeners) {
                            try {
                                listener.run()
                            } catch (e: Exception) {
                                Logger.printException { "OnNotPlaying listener error: ${e.message}" }
                            }
                        }
                    }
                }
            }

        @Volatile // Read/write from different threads.
        private var currentVideoState: VideoState? = null
    }
}
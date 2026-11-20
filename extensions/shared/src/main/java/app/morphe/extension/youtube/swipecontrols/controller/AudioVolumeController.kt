/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/3412
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.extension.youtube.swipecontrols.controller

import android.content.Context
import android.media.AudioManager
import app.morphe.extension.youtube.patches.SoundBoostPatch
import app.morphe.extension.shared.utils.Logger.printException
import app.morphe.extension.shared.utils.Utils.isSDKAbove
import app.morphe.extension.youtube.swipecontrols.misc.clamp
import kotlin.properties.Delegates

/**
 * controller to adjust the device volume level
 *
 * @param context the context to bind the audio service in
 * @param targetStream the stream that is being controlled. Must be one of the STREAM_* constants in [AudioManager]
 */
class AudioVolumeController(
    context: Context,
    private val targetStream: Int = AudioManager.STREAM_MUSIC,
) {

    /**
     * audio service connection
     */
    private lateinit var audioManager: AudioManager
    private var minimumVolumeIndex by Delegates.notNull<Int>()
    private var maximumVolumeIndex by Delegates.notNull<Int>()

    init {
        // bind audio service
        val mgr = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (mgr == null) {
            printException { "failed to acquire AUDIO_SERVICE" }
        } else {
            audioManager = mgr
            maximumVolumeIndex = audioManager.getStreamMaxVolume(targetStream)
            minimumVolumeIndex =
                if (isSDKAbove(28)) {
                    audioManager.getStreamMinVolume(
                        targetStream,
                    )
                } else {
                    0
                }
        }
    }

    /**
     * the current volume, ranging from 0.0 to [maxVolume]
     */
    var volume: Int
        get() {
            // check if initialized correctly
            if (!this::audioManager.isInitialized) return 0

            // get current volume
            return currentVolumeIndex - minimumVolumeIndex
        }
        set(value) {
            // check if initialized correctly
            if (!this::audioManager.isInitialized) return

            // set new volume
            currentVolumeIndex =
                (value + minimumVolumeIndex).clamp(minimumVolumeIndex, maximumVolumeIndex)
        }

    /**
     * the maximum possible volume
     */
    val maxVolume: Int
        get() = maximumVolumeIndex - minimumVolumeIndex

    /**
     * Whether the audio service was bound and the volume can be adjusted.
     * A controller that is not available must not be used, as the stream bounds are unknown.
     */
    val isAvailable: Boolean
        get() = this::audioManager.isInitialized

    /**
     * The current boost above the maximum volume, in steps. Always zero if volume boost is off.
     */
    val boostStep: Int
        get() = SoundBoostPatch.getBoostStep()

    /**
     * Adjusts the volume by whole steps, snapping to a multiple of [volumeStepSize]
     * so the levels stay the same ones the device volume UI uses.
     * If volume boost is allowed, steps up at the maximum volume raise the boost,
     * and steps down first remove the boost before lowering the volume.
     *
     * @param steps The number of steps to adjust by, negative to lower the volume.
     */
    fun adjustVolumeBySteps(steps: Int) {
        if (!isAvailable) return

        var remaining = steps
        while (remaining != 0) {
            val direction = if (remaining > 0) 1 else -1
            remaining -= direction

            if (direction > 0 && volume >= maxVolume) {
                SoundBoostPatch.adjustBoostStep(1)
                continue
            }
            if (direction < 0 && SoundBoostPatch.adjustBoostStep(-1)) {
                continue
            }
            adjustSystemVolumeBySteps(direction)
        }
    }

    /**
     * This controller uses stream indices for its existing volume sensitivity setting.
     */
    private fun adjustSystemVolumeBySteps(steps: Int) {
        volume += steps
    }

    /**
     * the current volume index of the target stream
     */
    private var currentVolumeIndex: Int
        get() = audioManager.getStreamVolume(targetStream)
        set(value) = audioManager.setStreamVolume(targetStream, value, 0)
}

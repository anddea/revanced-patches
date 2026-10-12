/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/3467
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.shared.misc.audio.silence

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.BytecodePatchBuilder
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.fingerprint.matchSingle
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import java.util.logging.Logger

private const val EXTENSION_CLASS = "Lapp/morphe/extension/shared/patches/SkipSilencePatch;"

@Suppress("unused")
internal fun skipSilencePatch(
    block: BytecodePatchBuilder.() -> Unit,
    targetCompatible: BytecodePatchBuilder.() -> Boolean = { true },
    executeBlock: BytecodePatchContext.() -> Unit,
) = bytecodePatch(
    name = "Skip silence",
    description = "Adds an option to automatically skip silent pauses in audio playback."
) {
    block()

    execute {
        // Older app versions use an older Media3 with a different silence skipping processor.
        if (!targetCompatible()) {
            return@execute Logger.getLogger(this::class.java.name).warning(
                "'Skip silence' does not support older legacy app targets."
            )
        }

        executeBlock()

        ApplySkipSilenceFingerprint.apply {
            val match = instructionMatches[1]
            val booleanRegister = match.getInstruction<TwoRegisterInstruction>().registerA

            method.addInstructions(
                match.index + 1,
                """
                    invoke-static { v$booleanRegister }, $EXTENSION_CLASS->isSkipSilenceEnabled(Z)Z
                    move-result v$booleanRegister
                """
            )
        }

        SetSkipSilenceEnabledFingerprint.matchSingle().method.addInstructions(
            0,
            """
                invoke-static { p1 }, $EXTENSION_CLASS->isSkipSilenceEnabled(Z)Z
                move-result p1
            """
        )

        val processorClass = classDefBy(
            SkipSilenceProcessorEnabledFingerprint.instructionMatches.last().getFieldAccessed().definingClass
        )
        val constructor = SilenceSkippingProcessorConstructorFingerprint.classFingerprint!!.matchOrNull()?.let {
            SilenceSkippingProcessorConstructorFingerprint.matchOrNull(processorClass)
        }
        if (constructor == null) {
            // Legacy processors inline these settings rather than accepting constructor arguments.
            val flush = LegacySilenceSkippingProcessorFlushFingerprint.match(processorClass)
            val duration = flush.instructionMatches.first()
            val durationRegister = duration.getInstruction<OneRegisterInstruction>().registerA
            flush.method.addInstructions(
                duration.index + 1,
                """
                    invoke-static/range { v$durationRegister .. v${durationRegister + 1} }, $EXTENSION_CLASS->minimumSilenceDurationUs(J)J
                    move-result-wide v$durationRegister
                """
            )

            LegacySilenceSkippingProcessorRetentionFingerprint.matchAllOrNull(processorClass).orEmpty().forEach {
                val ratio = it.instructionMatches.first()
                val register = ratio.getInstruction<OneRegisterInstruction>().registerA
                it.method.addInstructions(
                    ratio.index + 1,
                    """
                        invoke-static { v$register }, $EXTENSION_CLASS->silenceRetentionRatio(F)F
                        move-result v$register
                    """
                )
            }

            LegacySilenceSkippingProcessorNoiseFingerprint.matchAll(processorClass).forEach {
                val threshold = it.instructionMatches.last()
                val register = threshold.getInstruction<OneRegisterInstruction>().registerA
                it.method.addInstructions(
                    threshold.index + 1,
                    """
                        invoke-static { v$register }, $EXTENSION_CLASS->adaptiveSilenceThresholdLevel(S)S
                        move-result v$register
                    """
                )
            }

            LegacySilenceSkippingProcessorQueueInputFingerprint.match(processorClass).method.addInstruction(
                0,
                "invoke-static { p1 }, $EXTENSION_CLASS->updateSilenceThresholdLevel(Ljava/nio/ByteBuffer;)V"
            )
            flush.method.addInstruction(
                0,
                "invoke-static { }, $EXTENSION_CLASS->resetSilenceThresholdLevel()V"
            )
            return@execute
        }

        constructor.method.addInstructions(
            0,
            """
                invoke-static { p1, p2 }, $EXTENSION_CLASS->minimumSilenceDurationUs(J)J
                move-result-wide p1
                invoke-static { p3 }, $EXTENSION_CLASS->silenceRetentionRatio(F)F
                move-result p3
                invoke-static { p7 }, $EXTENSION_CLASS->silenceThresholdLevel(S)S
                move-result p7
            """
        )

        // Lower the silence threshold for quiet audio.
        SilenceSkippingProcessorQueueInputFingerprint.matchSingle().method.addInstruction(
            0,
            "invoke-static { p1 }, $EXTENSION_CLASS->updateSilenceThresholdLevel(Ljava/nio/ByteBuffer;)V"
        )

        SilenceSkippingProcessorFlushFingerprint.method.addInstruction(
            0,
            "invoke-static { }, $EXTENSION_CLASS->resetSilenceThresholdLevel()V"
        )

        SilenceSkippingProcessorIsNoiseFingerprint.apply {
            val match = instructionMatches.first()
            val thresholdRegister = match.getInstruction<TwoRegisterInstruction>().registerA

            method.addInstructions(
                match.index + 1,
                """
                    invoke-static { v$thresholdRegister }, $EXTENSION_CLASS->adaptiveSilenceThresholdLevel(S)S
                    move-result v$thresholdRegister
                """
            )
        }
    }
}

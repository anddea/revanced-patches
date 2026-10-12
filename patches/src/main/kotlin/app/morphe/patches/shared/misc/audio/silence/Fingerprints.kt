/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.shared.misc.audio.silence

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.anyInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

/**
 * DefaultAudioSink's applyAudioProcessorPlaybackParameters,
 * where the silence skipping processor flag is set.
 */
internal object ApplySkipSilenceFingerprint : Fingerprint(
    classFingerprint = Fingerprint(
        filters = listOf(
            anyInstruction(
                string("AudioTrackAudioOutput"),
                string("DefaultAudioSink")
            ),
            string("Failed to set playback params")
        ),
    ),
    returnType = "V",
    parameters = listOf("J"),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            definingClass = "this",
            type = "L"
        ),
        fieldAccess(
            opcode = Opcode.IGET_BOOLEAN,
            definingClass = "this",
            location = MatchAfterWithin(2)
        ),
        fieldAccess(
            definingClass = "this",
            opcode = Opcode.IPUT_BOOLEAN
        )
    )
)

/**
 * DefaultAudioSink's setSkipSilenceEnabled.
 */
internal object SetSkipSilenceEnabledFingerprint : Fingerprint(
    classFingerprint = ApplySkipSilenceFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Z")
)

/**
 * Writes the sink's silence flag to its audio processor, identifying the processor even on legacy players.
 */
internal object SkipSilenceProcessorEnabledFingerprint : Fingerprint(
    classFingerprint = ApplySkipSilenceFingerprint,
    returnType = "V",
    parameters = listOf("J"),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_BOOLEAN, definingClass = "this"),
        fieldAccess(opcode = Opcode.IPUT_BOOLEAN)
    )
)

/**
 * Legacy onFlush calculates its silence buffer length from an inlined minimum duration.
 */
internal object LegacySilenceSkippingProcessorFlushFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        anyInstruction(literal(100000L), literal(150000L)),
        opcode(Opcode.NEW_ARRAY)
    )
)

/**
 * Legacy queueInput, resolved inside the processor identified by the sink's enabled flag.
 */
internal object LegacySilenceSkippingProcessorQueueInputFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Ljava/nio/ByteBuffer;")
)

/**
 * Legacy processors compare the absolute sample value with an inlined threshold.
 * The oldest processor does this in both queueInput and its noise scanning method.
 */
internal object LegacySilenceSkippingProcessorNoiseFingerprint : Fingerprint(
    filters = listOf(
        methodCall(smali = "Ljava/lang/Math;->abs(I)I"),
        literal(1024, location = MatchAfterWithin(2))
    )
)

/**
 * Intermediate processors inline the retention ratio; the oldest ones retain a fixed padding duration.
 */
internal object LegacySilenceSkippingProcessorRetentionFingerprint : Fingerprint(
    returnType = "I",
    parameters = listOf("I"),
    filters = listOf(literal(0.2f.toRawBits().toLong()))
)

/**
 * Constructor of the silence skipping audio processor:
 * minimum silence duration (us), retention ratio, max silence to keep (us), min volume percentage, threshold level.
 */
internal object SilenceSkippingProcessorConstructorFingerprint : Fingerprint(
    classFingerprint = Fingerprint(
        filters = listOf(
            string("bytesConsumed is not aligned to frame size: %s")
        )
    ),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    parameters = listOf("J", "F", "J", "I", "S"),
)

/**
 * Silence skipping processor's queueInput, which receives the decoded audio.
 */
internal object SilenceSkippingProcessorQueueInputFingerprint : Fingerprint(
    classFingerprint = SilenceSkippingProcessorConstructorFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Ljava/nio/ByteBuffer;")
)

/**
 * Silence skipping processor's isNoise, which compares an audio sample with the silence threshold level.
 */
internal object SilenceSkippingProcessorIsNoiseFingerprint : Fingerprint(
    classFingerprint = SilenceSkippingProcessorConstructorFingerprint,
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf("B", "B"),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IGET_SHORT,
            definingClass = "this"
        )
    )
)

/**
 * Silence skipping processor's onFlush, the only method without parameters that allocates the silence buffers.
 */
internal object SilenceSkippingProcessorFlushFingerprint : Fingerprint(
    classFingerprint = SilenceSkippingProcessorConstructorFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        opcode(Opcode.NEW_ARRAY)
    )
)

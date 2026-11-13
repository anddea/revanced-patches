/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/3899
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.music.utils.gms

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.string
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

/**
 * MusicActivity listener for account state changes. If the account cannot cast to audio only
 * devices (first call) and the current cast session is on one, then the session is disconnected.
 */
internal object AudioCastAccountStateChangedFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/apps/youtube/music/activities/MusicActivity;",
    returnType = "V",
    parameters = listOf("L"),
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            parameters = listOf(),
            returnType = "Z"
        ),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        opcode(Opcode.IF_NEZ, location = MatchAfterImmediately()),
        methodCall(
            opcode = Opcode.INVOKE_INTERFACE,
            parameters = listOf(),
            returnType = "Z"
        ),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        opcode(Opcode.IF_EQZ, location = MatchAfterImmediately()),
        methodCall(
            opcode = Opcode.INVOKE_INTERFACE,
            parameters = listOf(),
            returnType = "V",
            location = MatchAfterImmediately()
        )
    )
)


/**
 * Device files search, which queries the Play Services app search index and returns the
 * content uris of the matching songs, albums or artists.
 */
internal object DeviceFilesSearchFingerprint : Fingerprint(
    returnType = "Lcom/google/common/util/concurrent/ListenableFuture;",
    parameters = listOf("Ljava/lang/String;", "[Ljava/lang/String;"),
    filters = listOf(
        string("thing_proto"),
        // The search executor. Its type is Object in app versions that merge this class with others.
        fieldAccess(opcode = Opcode.IGET_OBJECT, definingClass = "this"),
        // Futures.transform(), that turns the index results into content uris on the executor.
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = listOf(
                "Lcom/google/common/util/concurrent/ListenableFuture;",
                "L",
                "Ljava/util/concurrent/Executor;"
            ),
            returnType = "Lcom/google/common/util/concurrent/ListenableFuture;",
            location = MatchAfterImmediately()
        )
    )
)

/**
 * Guava Futures.getDone().
 */
internal object FuturesGetDoneFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/util/concurrent/Future;"),
    filters = listOf(
        string("Future was expected to be done: %s")
    )
)

/**
 * Guava Futures.submit(Callable, Executor).
 */
internal object FuturesSubmitFingerprint : Fingerprint(
    classFingerprint = FuturesGetDoneFingerprint,
    returnType = "Lcom/google/common/util/concurrent/ListenableFuture;",
    parameters = listOf("Ljava/util/concurrent/Callable;", "Ljava/util/concurrent/Executor;"),
    filters = listOf(
        // Old Guava builds use a task factory, newer ones a constructor.
        // The enclosing Futures class and exact signature disambiguate both from extension Guava.
        methodCall(definingClass = "Ljava/util/concurrent/Executor;", name = "execute")
    )
)

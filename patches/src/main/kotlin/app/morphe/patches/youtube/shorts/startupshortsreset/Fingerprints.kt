package app.morphe.patches.youtube.shorts.startupshortsreset

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation
import app.morphe.patcher.StringComparisonType
import app.morphe.patcher.checkCast
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

/**
 * YouTube v18.15.40+
 */
internal val userWasInShortsConfigFingerprint = legacyFingerprint(
    name = "userWasInShortsABConfigFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "Z",
    literals = listOf(45358360L)
)

/**
 * ~ YouTube 19.50.42
 */
internal val userWasInShortsFingerprint = legacyFingerprint(
    name = "userWasInShortsFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Ljava/lang/Object;"),
    strings = listOf("Failed to read user_was_in_shorts proto after successful warmup")
)

/**
 * YouTube 20.02.08 ~
 */
internal val userWasInShortsAlternativeFingerprint = legacyFingerprint(
    name = "userWasInShortsAlternativeFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Ljava/lang/Object;"),
    strings = listOf("userIsInShorts: ")
)

internal object UserWasInShortsListenerFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Ljava/lang/Object;"),
    filters = listOf(
        checkCast("Ljava/lang/Boolean;"),
        methodCall(
            smali = "Ljava/lang/Boolean;->booleanValue()Z",
            location = InstructionLocation.MatchAfterImmediately()
        ),
        opcode(Opcode.MOVE_RESULT, InstructionLocation.MatchAfterImmediately()),
        string(
            "ShortsStartup SetUserWasInShortsListener",
            StringComparisonType.CONTAINS,
            InstructionLocation.MatchAfterWithin(30)
        )
    )
)

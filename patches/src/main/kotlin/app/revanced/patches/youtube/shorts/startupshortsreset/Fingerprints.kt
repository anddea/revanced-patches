package app.revanced.patches.youtube.shorts.startupshortsreset

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags

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
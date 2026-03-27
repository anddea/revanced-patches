package app.morphe.patches.youtube.utils.fullscreen

import app.morphe.util.containsLiteralInstruction
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

private const val NEXT_GEN_WATCH_LAYOUT_CLASS_DESCRIPTOR =
    "Lcom/google/android/apps/youtube/app/watch/nextgenwatch/ui/NextGenWatchLayout;"

internal val nextGenWatchLayoutConstructorFingerprint = legacyFingerprint(
    name = "nextGenWatchLayoutConstructorFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    parameters = listOf("Landroid/content/Context;", "Landroid/util/AttributeSet;", "I"),
    opcodes = listOf(Opcode.CHECK_CAST),
    customFingerprint = { method, _ ->
        method.definingClass == NEXT_GEN_WATCH_LAYOUT_CLASS_DESCRIPTOR
    },
)

internal val nextGenWatchLayoutFullscreenModeFingerprint = legacyFingerprint(
    name = "nextGenWatchLayoutFullscreenModeFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("I"),
    opcodes = listOf(Opcode.INVOKE_DIRECT),
    customFingerprint = { method, _ ->
        method.definingClass == NEXT_GEN_WATCH_LAYOUT_CLASS_DESCRIPTOR &&
                method.containsLiteralInstruction(32)
    },
)



package app.morphe.patches.youtube.general.startpage

import app.morphe.util.fingerprint.legacyFingerprint
import com.android.tools.smali.dexlib2.Opcode

internal val browseIdFingerprint = legacyFingerprint(
    name = "browseIdFingerprint",
    returnType = "Lcom/google/android/apps/youtube/app/common/ui/navigation/PaneDescriptor;",
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.RETURN_OBJECT,
    ),
    strings = listOf("FEwhat_to_watch"),
)

internal val intentFingerprint = legacyFingerprint(
    name = "intentFingerprint",
    parameters = listOf("Landroid/content/Intent;"),
    strings = listOf("has_handled_intent"),
)

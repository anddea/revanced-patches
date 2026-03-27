package app.morphe.patches.music.general.startpage

import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

const val DEFAULT_BROWSE_ID = "FEmusic_home"

internal val coldStartIntentFingerprint = legacyFingerprint(
    name = "coldStartIntentFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/content/Intent;"),
    strings = listOf(
        "android.intent.action.SEARCH",
        "com.google.android.youtube.music.action.shortcut_type",
    )
)

internal val coldStartUpFingerprint = legacyFingerprint(
    name = "coldStartUpFingerprint",
    returnType = "Ljava/lang/String;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.CONST_STRING,
        Opcode.RETURN_OBJECT
    ),
    strings = listOf(
        "FEmusic_library_sideloaded_tracks",
        DEFAULT_BROWSE_ID
    )
)


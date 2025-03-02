package app.revanced.patches.music.utils.settings

import app.revanced.util.fingerprint.legacyFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val googleApiActivityFingerprint = legacyFingerprint(
    name = "googleApiActivityFingerprint",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    customFingerprint = { method, _ ->
        method.definingClass.endsWith("/GoogleApiActivity;") &&
                method.name == "onCreate"
    }
)

internal val preferenceFingerprint = legacyFingerprint(
    name = "preferenceFingerprint",
    accessFlags = AccessFlags.PROTECTED.value,
    returnType = "V",
    parameters = listOf("Z"),
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_INTERFACE,
    ),
    customFingerprint = { method, _ ->
        method.definingClass == "Landroidx/preference/Preference;"
    }
)

internal val settingsHeadersFragmentFingerprint = legacyFingerprint(
    name = "settingsHeadersFragmentFingerprint",
    returnType = "V",
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
    ),
    customFingerprint = { method, _ ->
        method.definingClass.endsWith("/SettingsHeadersFragment;") &&
                method.name == "onCreate"
    }
)

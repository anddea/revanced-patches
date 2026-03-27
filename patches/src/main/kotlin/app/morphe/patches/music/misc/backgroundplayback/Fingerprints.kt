package app.morphe.patches.music.misc.backgroundplayback

import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val backgroundPlaybackManagerFingerprint = legacyFingerprint(
    name = "backgroundPlaybackManagerFingerprint",
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("L"),
    literals = listOf(64657230L),
)

internal val dataSavingSettingsFragmentFingerprint = legacyFingerprint(
    name = "dataSavingSettingsFragmentFingerprint",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;", "Ljava/lang/String;"),
    strings = listOf("pref_key_dont_play_nma_video"),
    customFingerprint = { method, _ ->
        method.definingClass.endsWith("/DataSavingSettingsFragment;") &&
                method.name == "onCreatePreferences"
    }
)

internal val kidsBackgroundPlaybackPolicyControllerFingerprint = legacyFingerprint(
    name = "kidsBackgroundPlaybackPolicyControllerFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("I", "L", "Z"),
    opcodes = listOf(
        Opcode.IGET,
        Opcode.IF_NE,
        Opcode.IGET_OBJECT,
        Opcode.IF_NE,
        Opcode.IGET_BOOLEAN,
        Opcode.IF_EQ,
        Opcode.GOTO,
        Opcode.RETURN_VOID,
        Opcode.SGET_OBJECT,
        Opcode.CONST_4,
        Opcode.IF_NE,
        Opcode.IPUT_BOOLEAN
    )
)

internal val musicBrowserServiceFingerprint = legacyFingerprint(
    name = "musicBrowserServiceFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Ljava/lang/String;", "Landroid/os/Bundle;"),
    strings = listOf("android.service.media.extra.RECENT"),
    customFingerprint = { method, _ ->
        method.definingClass.endsWith("/MusicBrowserService;")
    },
)

internal val podCastConfigFingerprint = legacyFingerprint(
    name = "podCastConfigFingerprint",
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    literals = listOf(45388403L),
)

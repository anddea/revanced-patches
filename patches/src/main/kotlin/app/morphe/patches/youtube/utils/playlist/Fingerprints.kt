package app.morphe.patches.youtube.utils.playlist

import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

internal val editPlaylistConstructorFingerprint = legacyFingerprint(
    name = "editPlaylistConstructorFingerprint",
    returnType = "V",
    strings = listOf("browse/edit_playlist")
)

internal val editPlaylistFingerprint = legacyFingerprint(
    name = "editPlaylistFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Ljava/util/List;"),
    opcodes = listOf(
        Opcode.CHECK_CAST,
        Opcode.IGET_OBJECT,
    ),
)

internal val playlistEndpointFingerprint = legacyFingerprint(
    name = "playlistEndpointFingerprint",
    returnType = "L",
    parameters = listOf("L", "Ljava/lang/String;"),
    customFingerprint = { method, _ ->
        method.indexOfFirstInstruction {
            opcode == Opcode.SGET_OBJECT &&
                    getReference<FieldReference>()?.name == "playlistEditEndpoint"
        } >= 0 && indexOfSetVideoIdInstruction(method) >= 0
    }
)

internal fun indexOfSetVideoIdInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.IPUT_OBJECT &&
                getReference<FieldReference>()?.type == "Ljava/lang/String;"
    }

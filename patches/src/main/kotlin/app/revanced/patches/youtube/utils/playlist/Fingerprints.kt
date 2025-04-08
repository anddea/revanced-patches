package app.revanced.patches.youtube.utils.playlist

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.or
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

internal val setPivotBarVisibilityFingerprint = legacyFingerprint(
    name = "setPivotBarVisibilityFingerprint",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    returnType = "V",
    parameters = listOf("Z"),
    opcodes = listOf(
        Opcode.CHECK_CAST,
        Opcode.IF_EQZ,
    ),
)

internal val setPivotBarVisibilityParentFingerprint = legacyFingerprint(
    name = "setPivotBarVisibilityFingerprint",
    parameters = listOf("Z"),
    strings = listOf("FEnotifications_inbox"),
)

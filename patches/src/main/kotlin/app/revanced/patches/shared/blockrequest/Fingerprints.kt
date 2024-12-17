package app.revanced.patches.shared.blockrequest

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val buildInitPlaybackRequestFingerprint = legacyFingerprint(
    name = "buildInitPlaybackRequestFingerprint",
    returnType = "Lorg/chromium/net/UrlRequest\$Builder;",
    opcodes = listOf(
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IGET_OBJECT, // Moves the request URI string to a register to build the request with.
    ),
    strings = listOf(
        "Content-Type",
        "Range",
    ),
)

internal val buildPlayerRequestURIFingerprint = legacyFingerprint(
    name = "buildPlayerRequestURIFingerprint",
    returnType = "Ljava/lang/String;",
    strings = listOf(
        "key",
        "asig",
    ),
    customFingerprint = { method, _ ->
        indexOfToStringInstruction(method) >= 0
    },
)

internal fun indexOfToStringInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>().toString() == "Landroid/net/Uri;->toString()Ljava/lang/String;"
    }


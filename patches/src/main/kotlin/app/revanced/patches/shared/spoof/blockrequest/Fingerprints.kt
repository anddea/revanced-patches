package app.revanced.patches.shared.spoof.blockrequest

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
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
    customFingerprint = { method, _ ->
        indexOfUriToStringInstruction(method) >= 0
    },
)

internal val buildMediaDataSourceFingerprint = legacyFingerprint(
    name = "buildMediaDataSourceFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    returnType = "V",
    parameters = listOf(
        "Landroid/net/Uri;",
        "J",
        "I",
        "[B",
        "Ljava/util/Map;",
        "J",
        "J",
        "Ljava/lang/String;",
        "I",
        "Ljava/lang/Object;"
    )
)

internal val buildPlayerRequestURIFingerprint = legacyFingerprint(
    name = "buildPlayerRequestURIFingerprint",
    returnType = "Ljava/lang/String;",
    strings = listOf(
        "key",
        "asig",
    ),
    customFingerprint = { method, _ ->
        indexOfUriToStringInstruction(method) >= 0
    },
)

internal fun indexOfUriToStringInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>().toString() == "Landroid/net/Uri;->toString()Ljava/lang/String;"
    }
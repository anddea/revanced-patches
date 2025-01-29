package app.revanced.patches.music.utils.videotype

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val videoTypeFingerprint = legacyFingerprint(
    name = "videoTypeFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("L"),
    customFingerprint = { method, _ ->
        indexOfGetEnumInstruction(method) >= 0
    }
)

internal fun indexOfGetEnumInstruction(method: Method) =
    method.indexOfFirstInstruction {
        val reference = getReference<MethodReference>()
        opcode == Opcode.INVOKE_STATIC &&
                reference?.name == "a" &&
                reference.parameterTypes.firstOrNull() == "I" &&
                reference.definingClass == reference.returnType
    }

internal val videoTypeParentFingerprint = legacyFingerprint(
    name = "videoTypeParentFingerprint",
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("L", "L"),
    strings = listOf("RQ")
)

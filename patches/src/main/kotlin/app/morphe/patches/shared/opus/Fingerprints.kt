package app.morphe.patches.shared.opus

import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

internal val codecReferenceFingerprint = legacyFingerprint(
    name = "codecReferenceFingerprint",
    returnType = "J",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    opcodes = listOf(Opcode.INVOKE_SUPER),
    strings = listOf("itag")
)

internal val codecSelectorFingerprint = legacyFingerprint(
    name = "codecSelectorFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    opcodes = listOf(
        Opcode.NEW_INSTANCE,
        Opcode.NEW_INSTANCE,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT
    ),
    // modified by lavinhoque33, 2026-10-04
    // YouTube 21.39 removed the "Audio track id %s not in audio streams" string, so match either the
    // string (older versions) or the (..., String)L static method that builds a HashSet from a codec set.
    customFingerprint = { method, _ ->
        method.indexOfFirstInstruction {
            getReference<StringReference>()?.string == "Audio track id %s not in audio streams"
        } >= 0 ||
                (method.parameterTypes.size == 5 &&
                        method.parameterTypes.last() == "Ljava/lang/String;" &&
                        method.indexOfFirstInstruction {
                            val ref = getReference<MethodReference>()
                            opcode == Opcode.INVOKE_DIRECT &&
                                    ref?.definingClass == "Ljava/util/HashSet;" &&
                                    ref.name == "<init>" &&
                                    ref.parameterTypes.firstOrNull() == "Ljava/util/Collection;"
                        } >= 0)
    }
)


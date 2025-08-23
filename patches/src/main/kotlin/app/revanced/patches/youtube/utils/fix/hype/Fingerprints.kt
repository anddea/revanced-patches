package app.revanced.patches.youtube.utils.fix.hype

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionReversed
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val watchNextConstructorFingerprint = legacyFingerprint(
    name = "watchNextConstructorFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    opcodes = listOf(
        Opcode.INVOKE_DIRECT_RANGE,
        Opcode.RETURN_VOID,
    ),
)

internal val watchNextSyntheticFingerprint = legacyFingerprint(
    name = "watchNextSyntheticFingerprint",
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Lcom/google/protobuf/MessageLite;"),
    opcodes = listOf(
        Opcode.CHECK_CAST,
        Opcode.NEW_INSTANCE,
        Opcode.INVOKE_DIRECT,
        Opcode.RETURN_OBJECT,
    ),
    customFingerprint = custom@{ method, classDef ->
        if (classDef.methods.count() != 2) return@custom false
        val methodAccessFlags = method.accessFlags
        // 'public final bridge synthetic' or 'public final synthetic'
        if (!AccessFlags.SYNTHETIC.isSet(methodAccessFlags)) return@custom false
        if (!AccessFlags.FINAL.isSet(methodAccessFlags)) return@custom false
        if (!AccessFlags.PUBLIC.isSet(methodAccessFlags)) return@custom false

        method.indexOfFirstInstructionReversed {
            val reference = getReference<MethodReference>()
            opcode == Opcode.INVOKE_DIRECT &&
                    reference?.definingClass == "Lcom/google/android/libraries/youtube/innertube/model/WatchNextResponseModel;" &&
                    reference.returnType == "V" &&
                    reference.name == "<init>" &&
                    reference.parameterTypes.size == 1
        } >= 0
    }
)

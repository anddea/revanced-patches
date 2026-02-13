package app.morphe.patches.shared.spoof.watchnext

import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val watchNextResponseModelClassResolverFingerprint = legacyFingerprint(
    name = "watchNextResponseModelClassResolverFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    strings = listOf("Request being made from non-critical thread"),
    customFingerprint = { method, classDef ->
        method.name == "run" &&
                indexOfListenableFutureReference(method) >= 0
    }
)

internal fun indexOfListenableFutureReference(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_INTERFACE &&
                getReference<MethodReference>()?.toString() == "Lcom/google/common/util/concurrent/ListenableFuture;->get()Ljava/lang/Object;"
    }

internal val watchNextConstructorFingerprint = legacyFingerprint(
    name = "watchNextConstructorFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    opcodes = listOf(
        Opcode.INVOKE_DIRECT_RANGE,
        Opcode.RETURN_VOID,
    ),
)

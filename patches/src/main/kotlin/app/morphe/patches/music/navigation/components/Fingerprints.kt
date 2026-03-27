package app.morphe.patches.music.navigation.components

import app.morphe.patches.music.utils.resourceid.colorGrey
import app.morphe.patches.music.utils.resourceid.text1
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val tabLayoutFingerprint = legacyFingerprint(
    name = "tabLayoutFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    strings = listOf("FEmusic_radio_builder"),
    literals = listOf(colorGrey)
)

internal val tabLayoutTextFingerprint = legacyFingerprint(
    name = "tabLayoutTextFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.IGET,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IF_NEZ,
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT
    ),
    strings = listOf("FEmusic_search"),
    literals = listOf(text1),
    customFingerprint = { method, _ ->
        indexOfMapInstruction(method) >= 0 &&
                indexOfGetVisibilityInstruction(method) >= 0 &&
                indexOfSetTextInstruction(method) >= 0
    }
)

internal fun indexOfGetVisibilityInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.name == "getVisibility"
    }

internal fun indexOfSetTextInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.name == "setText"
    }

internal fun indexOfMapInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_INTERFACE &&
                getReference<MethodReference>()?.toString() == "Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
    }

package app.revanced.patches.reddit.layout.recentlyvisited

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

internal val communityDrawerPresenterConstructorFingerprint = legacyFingerprint(
    name = "communityDrawerPresenterConstructorFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    strings = listOf("communityDrawerSettings"),
    customFingerprint = { method, _ ->
        indexOfHeaderItemInstruction(method) >= 0
    }
)

fun indexOfHeaderItemInstruction(method: Method) =
    method.indexOfFirstInstruction {
        getReference<FieldReference>()?.name == "RECENTLY_VISITED"
    }

internal val communityDrawerPresenterFingerprint = legacyFingerprint(
    name = "communityDrawerPresenterFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.XOR_INT_2ADDR,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
    )
)

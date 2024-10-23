package app.revanced.patches.shared.gms.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.StringReference

internal object PrimesBackgroundInitializationFingerprint : MethodFingerprint(
    opcodes = listOf(Opcode.NEW_INSTANCE),
    customFingerprint = { methodDef, _ ->
        methodDef.indexOfFirstInstruction {
            opcode == Opcode.CONST_STRING &&
                    getReference<StringReference>()
                        ?.string.toString().startsWith("Primes init triggered from background in package:")
        } >= 0
    }
)
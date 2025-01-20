package app.revanced.patches.youtube.player.action

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val componentListFingerprint = legacyFingerprint(
    name = "componentListFingerprint",
    returnType = "Ljava/util/List;",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    customFingerprint = { method, _ ->
        method.indexOfFirstInstruction {
            opcode == Opcode.INVOKE_STATIC &&
                    getReference<MethodReference>()?.name == "nCopies"
        } >= 0
    }
)

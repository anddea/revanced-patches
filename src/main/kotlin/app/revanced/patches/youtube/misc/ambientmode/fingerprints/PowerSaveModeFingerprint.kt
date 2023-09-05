package app.revanced.patches.youtube.misc.ambientmode.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.YtBrandBackgroundSolid
import app.revanced.util.bytecode.isWideLiteralExists
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

object PowerSaveModeFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Ljava/lang/Object;"),
    opcodes = listOf(
        Opcode.IF_GT,
        Opcode.IGET,
        Opcode.ADD_INT_2ADDR
    ),
    customFingerprint = { methodDef, _ -> methodDef.isWideLiteralExists(YtBrandBackgroundSolid) && methodDef.name == "accept" }
)
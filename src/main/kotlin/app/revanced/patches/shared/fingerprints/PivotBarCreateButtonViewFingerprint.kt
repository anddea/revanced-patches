package app.revanced.patches.shared.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.imageOnlyTabId
import app.revanced.util.bytecode.isWideLiteralExists
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object PivotBarCreateButtonViewFingerprint : MethodFingerprint(
    returnType = "V",
    access = AccessFlags.PUBLIC or AccessFlags.FINAL,
    opcodes = listOf(
        Opcode.MOVE_OBJECT,
        Opcode.INVOKE_DIRECT_RANGE, // unique instruction anchor
    ),
    customFingerprint = { it.isWideLiteralExists(imageOnlyTabId) }
)
package app.revanced.patches.youtube.player.overlaybuttons.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.AccessibilityOfflineButtonSync
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

object SetVisibilityOfflineArrowViewFingerprint : LiteralValueFingerprint(
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "V",
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN_VOID,
        Opcode.INVOKE_SUPER,
        Opcode.RETURN_VOID
    ),
    literalSupplier = { AccessibilityOfflineButtonSync }
)
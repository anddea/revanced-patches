package app.revanced.patches.youtube.general.toolbar.fingerprints

import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ActionBarRingoBackground
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.Opcode

internal object ActionBarRingoBackgroundFingerprint : LiteralValueFingerprint(
    returnType = "Landroid/view/View;",
    parameters = listOf("Landroid/view/LayoutInflater;"),
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_STATIC
    ),
    literalSupplier = { ActionBarRingoBackground }
)
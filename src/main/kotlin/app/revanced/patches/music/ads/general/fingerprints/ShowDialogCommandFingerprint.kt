package app.revanced.patches.music.ads.general.fingerprints

import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.SlidingDialogAnimation
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.Opcode

internal object ShowDialogCommandFingerprint : LiteralValueFingerprint(
    returnType = "V",
    parameters = listOf("[B", "L"),
    opcodes = listOf(
        Opcode.IF_EQ,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.IGET, // get dialog code
    ),
    literalSupplier = { SlidingDialogAnimation }
)
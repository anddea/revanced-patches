package app.revanced.patches.music.video.information.fingerprints

import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.QualityAuto
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.Opcode

internal object VideoQualityListFingerprint : LiteralValueFingerprint(
    returnType = "V",
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.INVOKE_INTERFACE,
        Opcode.RETURN_VOID
    ),
    literalSupplier = { QualityAuto }
)
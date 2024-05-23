package app.revanced.patches.youtube.utils.fix.clientspoof.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patches.youtube.utils.fix.clientspoof.SpoofClientResourcePatch
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object SpoofSignaturePatchScrubbedPreviewLayoutFingerprint : LiteralValueFingerprint(
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    returnType = "V",
    parameters = listOf("Landroid/content/Context;", "Landroid/util/AttributeSet;", "I", "I"),
    opcodes = listOf(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.CONST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.IPUT_OBJECT, // preview imageview
    ),
    // This resource is used in ~ 40 different locations, but this method has a distinct list of parameters to match to.
    literalSupplier = { SpoofClientResourcePatch.scrubbedPreviewThumbnailResourceId },
)
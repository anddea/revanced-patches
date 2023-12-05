package app.revanced.patches.music.ads.general.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.MusicNotifierShelf
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

object NotifierShelfFingerprint : LiteralValueFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.IPUT_OBJECT
    ),
    literalSupplier = { MusicNotifierShelf }
)
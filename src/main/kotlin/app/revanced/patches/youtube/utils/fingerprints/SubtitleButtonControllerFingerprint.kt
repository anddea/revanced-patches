package app.revanced.patches.youtube.utils.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.AccessibilityCaptionsButtonName
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

object SubtitleButtonControllerFingerprint : LiteralValueFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Lcom/google/android/libraries/youtube/player/subtitles/model/SubtitleTrack;"),
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.IF_NEZ,
        Opcode.RETURN_VOID,
        Opcode.IGET_BOOLEAN,
        Opcode.CONST_4,
        Opcode.IF_NEZ,
        Opcode.CONST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.IGET_OBJECT
    ),
    literalSupplier = { AccessibilityCaptionsButtonName }
)
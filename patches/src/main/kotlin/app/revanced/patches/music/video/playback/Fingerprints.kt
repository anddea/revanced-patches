package app.revanced.patches.music.video.playback

import app.revanced.patches.music.utils.resourceid.qualityAuto
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val availableVideoFormatsFingerprint = legacyFingerprint(
    name = "availableVideoFormatsFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.STATIC,
    parameters = listOf("Ljava/util/List;", "I"),
    opcodes = listOf(
        Opcode.CHECK_CAST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT
    ),
)

internal val formatStreamModelBuilderFingerprint = legacyFingerprint(
    name = "formatStreamModelBuilderFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "L",
    strings = listOf("vprng")
)

internal val initFormatStreamFingerprint = legacyFingerprint(
    name = "initFormatStreamFingerprint",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    returnType = "V",
)

internal val initFormatStreamParentFingerprint = legacyFingerprint(
    name = "initFormatStreamParentFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    returnType = "V",
    strings = listOf("noopytm")
)

internal val userQualityChangeFingerprint = legacyFingerprint(
    name = "userQualityChangeFingerprint",
    returnType = "V",
    opcodes = listOf(
        Opcode.CONST_STRING,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IF_EQZ,
        Opcode.CHECK_CAST
    ),
    strings = listOf("VIDEO_QUALITIES_MENU_BOTTOM_SHEET_FRAGMENT")
)

internal val videoQualityListFingerprint = legacyFingerprint(
    name = "videoQualityListFingerprint",
    returnType = "V",
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.INVOKE_INTERFACE,
        Opcode.RETURN_VOID
    ),
    literals = listOf(qualityAuto)
)
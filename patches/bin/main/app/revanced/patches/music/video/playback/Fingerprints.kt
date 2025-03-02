package app.revanced.patches.music.video.playback

import app.revanced.util.fingerprint.legacyFingerprint
import com.android.tools.smali.dexlib2.Opcode

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
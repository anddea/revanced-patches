package app.morphe.patches.music.video.playback

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string
import app.morphe.patches.music.utils.resourceid.qualityAuto
import app.morphe.util.fingerprint.legacyFingerprint
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.AccessFlags

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

/**
 * Locates the playback speed bottom sheet fragment through its static factory, whose return type is the fragment class.
 */
// modified by lavinhoque33, 2026-10-04
// YouTube Music 9.40: the string moved out of the fragment class into `gxx.d(Activity)Lhwo;`.
internal object ModernPlaybackSpeedBottomSheetFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf("Landroid/app/Activity;"),
    filters = listOf(string("PLAYBACK_RATE_MENU_BOTTOM_SHEET_FRAGMENT"))
)

// modified by lavinhoque33, 2026-10-04
// YouTube Music 9.40: the quality list callback (`abdy.handleFormatStreamChangeEvent`) now ends with
// `invoke-virtual {v1, v5, v3, p0}, Lhxe;->d([Lwdz;IZ)V` instead of an interface call.
internal val videoQualityListModernFingerprint = legacyFingerprint(
    name = "videoQualityListModernFingerprint",
    returnType = "V",
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN_VOID
    ),
    literals = listOf(qualityAuto)
)

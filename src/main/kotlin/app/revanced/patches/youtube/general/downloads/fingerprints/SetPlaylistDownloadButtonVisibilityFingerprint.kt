package app.revanced.patches.youtube.general.downloads.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.Opcode

/**
 * Resolves using class found in [AccessibilityOfflineButtonSyncFingerprint].
 */
internal object SetPlaylistDownloadButtonVisibilityFingerprint : MethodFingerprint(
    returnType = "V",
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IF_NEZ,
        Opcode.IGET,
        Opcode.CONST_4
    )
)
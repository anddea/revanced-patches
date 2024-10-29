package app.revanced.patches.youtube.video.information.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.fingerprints.VideoEndFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

/**
 * Resolves using class found in [VideoEndFingerprint].
 */
internal object SeekRelativeFingerprint : MethodFingerprint(
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    // returnType = "Z", ~ YouTube 19.39.39
    // returnType = "V", YouTube 19.40.xx ~
    parameters = listOf("J", "L"),
    opcodes = listOf(
        Opcode.ADD_LONG_2ADDR,
        Opcode.INVOKE_VIRTUAL,
    )
)
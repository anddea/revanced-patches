package app.revanced.patches.youtube.player.buttons.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.Opcode

internal object PlayerControlsVisibilityModelFingerprint : MethodFingerprint(
    opcodes = listOf(Opcode.INVOKE_DIRECT_RANGE),
    strings = listOf("Missing required properties:", "hasNext", "hasPrevious")
)
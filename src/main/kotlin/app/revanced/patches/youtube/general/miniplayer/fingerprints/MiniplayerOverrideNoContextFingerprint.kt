package app.revanced.patches.youtube.general.miniplayer.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

@Suppress("SpellCheckingInspection")
internal object MiniplayerOverrideNoContextFingerprint : MethodFingerprint(
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    returnType = "Z",
    opcodes = listOf(Opcode.IGET_BOOLEAN), // anchor to insert the instruction
)
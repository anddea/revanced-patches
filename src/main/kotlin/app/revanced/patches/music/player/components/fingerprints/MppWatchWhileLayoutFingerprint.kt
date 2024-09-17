package app.revanced.patches.music.player.components.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.MiniPlayerPlayPauseReplayButton
import app.revanced.util.containsWideLiteralInstructionValue
import com.android.tools.smali.dexlib2.Opcode

internal object MppWatchWhileLayoutFingerprint : MethodFingerprint(
    returnType = "V",
    opcodes = listOf(Opcode.NEW_ARRAY),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass.endsWith("/MppWatchWhileLayout;")
                && methodDef.name == "onFinishInflate"
                && methodDef.containsWideLiteralInstructionValue(MiniPlayerPlayPauseReplayButton)
    }
)

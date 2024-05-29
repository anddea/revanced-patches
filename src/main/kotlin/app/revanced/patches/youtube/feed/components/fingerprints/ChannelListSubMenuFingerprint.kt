package app.revanced.patches.youtube.feed.components.fingerprints

import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ChannelListSubMenu
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.Opcode

internal object ChannelListSubMenuFingerprint : LiteralValueFingerprint(
    opcodes = listOf(
        Opcode.CONST,
        Opcode.CONST_4,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT
    ),
    literalSupplier = { ChannelListSubMenu }
)
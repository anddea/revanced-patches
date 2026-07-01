package app.morphe.patches.shared.textcomponent

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

/**
 * Matches the TextComponent render method through its constructor class.
 *
 * The constructor became public in YouTube Music 9.15, so access flags are intentionally omitted.
 */
internal object TextComponentContextFingerprint : Fingerprint(
    classFingerprint = Fingerprint(
        returnType = "V",
        parameters = emptyList(),
        filters = listOf(
            string("TextComponent"),
            opcode(Opcode.SGET_OBJECT),
            opcode(Opcode.IPUT_OBJECT)
        )
    ),
    returnType = "L",
    accessFlags = listOf(AccessFlags.PROTECTED, AccessFlags.FINAL),
    parameters = listOf("L"),
    filters = listOf(
        fieldAccess(type = "Ljava/util/Map;")
    )
)

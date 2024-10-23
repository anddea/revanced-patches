package app.revanced.patches.music.flyoutmenu.components.fingerprints

import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.Opcode

internal object ScreenWidthFingerprint : LiteralValueFingerprint(
    returnType = "Z",
    parameters = listOf("L"),
    opcodes = listOf(Opcode.IF_LT),
    literalSupplier = { 600 },
)
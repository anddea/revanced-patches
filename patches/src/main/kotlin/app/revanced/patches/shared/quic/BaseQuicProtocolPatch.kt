@file:Suppress("SpellCheckingInspection")

package app.revanced.patches.shared.quic

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.extension.Constants.PATCHES_PATH
import app.revanced.util.fingerprint.methodOrThrow

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$PATCHES_PATH/QUICProtocolPatch;"

fun baseQuicProtocolPatch() = bytecodePatch(
    description = "baseQuicProtocolPatch"
) {
    execute {
        arrayOf(
            cronetEngineBuilderFingerprint,
            experimentalCronetEngineBuilderFingerprint
        ).forEach {
            it.methodOrThrow().addInstructions(
                0, """
                    invoke-static {p1}, $EXTENSION_CLASS_DESCRIPTOR->disableQUICProtocol(Z)Z
                    move-result p1
                    """
            )
        }
    }
}


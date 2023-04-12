package app.revanced.patches.youtube.layout.navigation.shortsnavbar.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.Opcode

object NavigationEndpointFingerprint : MethodFingerprint(
    returnType = "V",
    opcodes = listOf(
        Opcode.RETURN_VOID,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT
    ),
    strings = listOf("r_pfvc", "navigation_endpoint_interaction_logging_extension")
)
package app.revanced.patches.spotify.extended

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.instructionsOrNull
import app.revanced.patcher.fingerprint
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.spotify.misc.privacy.shareLinkFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/spotify/misc/privacy/SanitizeSharingLinksPatch;"

@Suppress("unused")
val sanitizeSharingLinksPatch = bytecodePatch(
    name = "Sanitize sharing links",
    description = "Removes the tracking query parameters from links before they are shared.",
) {
    compatibleWith("com.spotify.music")

    execute {
        val originalMethod = shareLinkFingerprint.method
        val invokeDirectIndex = originalMethod.indexOfFirstInstructionOrThrow {
            opcode == Opcode.INVOKE_DIRECT &&
                    (this as ReferenceInstruction).reference is MethodReference &&
                    (getReference<MethodReference>()?.name == "<init>") &&
                    (getReference<MethodReference>()?.definingClass == "Ljava/lang/Object;")
        }

        val smaliCodeToInsert = """
                invoke-static {p1}, $EXTENSION_CLASS_DESCRIPTOR->sanitizeUrl(Ljava/lang/String;)Ljava/lang/String;
                move-result-object p1
            """.trimIndent()

        originalMethod.addInstructions(invokeDirectIndex + 1, smaliCodeToInsert)
    }
}

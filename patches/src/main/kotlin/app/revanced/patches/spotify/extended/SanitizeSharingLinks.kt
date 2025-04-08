package app.revanced.patches.spotify.extended

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.instructionsOrNull
import app.revanced.patcher.fingerprint
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

val shareLinkFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
    parameters(
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;"
    )
    returns("V")

    custom { _, classDef ->
        val toStringMethod = classDef.methods.firstOrNull {
            it.name == "toString" && it.parameters.isEmpty() && it.returnType == "Ljava/lang/String;"
        } ?: return@custom false

        val toStringInstructions = toStringMethod.instructionsOrNull ?: return@custom false
        toStringInstructions.any { instruction ->
            instruction.opcode == Opcode.CONST_STRING &&
                    (instruction as? ReferenceInstruction)?.reference?.let { ref ->
                        (ref as? StringReference)?.string?.startsWith("ShareUrl(url=") == true
                    } == true
        }
    }
}

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/revanced/extension/spotify/misc/UrlCleaner;"

@Suppress("unused")
val removeShareTrackingPatch = bytecodePatch(
    name = "Sanitize sharing links",
    description = "Removes the '?si=' tracking parameter from shared links (e.g., Copy Link, Share to...).",
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
                invoke-static {p1}, $EXTENSION_CLASS_DESCRIPTOR->removeSiParameter(Ljava/lang/String;)Ljava/lang/String;
                move-result-object p1
            """.trimIndent()

        originalMethod.addInstructions(invokeDirectIndex + 1, smaliCodeToInsert)
    }
}

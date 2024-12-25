package app.revanced.patches.youtube.utils.trackingurlhook

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private lateinit var trackingUrlMethod: MutableMethod

val trackingUrlHookPatch = bytecodePatch(
    description = "trackingUrlHookPatch"
) {
    execute {
        trackingUrlMethod = trackingUrlModelFingerprint.methodOrThrow()
    }
}

internal fun hookTrackingUrl(
    descriptor: String
) = trackingUrlMethod.apply {
    val targetIndex = indexOfFirstInstructionOrThrow {
        opcode == Opcode.INVOKE_STATIC &&
                getReference<MethodReference>()?.name == "parse"
    } + 1
    val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

    var smaliInstruction = "invoke-static {v$targetRegister}, $descriptor"

    if (!descriptor.endsWith("V")) {
        smaliInstruction += """
                move-result-object v$targetRegister
                
                """.trimIndent()
    }

    addInstructions(
        targetIndex + 1,
        smaliInstruction
    )
}

package app.revanced.patches.youtube.utils.trackingurlhook

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.utils.trackingurlhook.fingerprints.TrackingUrlModelFingerprint
import app.revanced.util.getTargetIndexWithMethodReferenceNameOrThrow
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

object TrackingUrlHookPatch : BytecodePatch(
    setOf(TrackingUrlModelFingerprint)
) {
    private lateinit var trackingUrlMethod: MutableMethod

    override fun execute(context: BytecodeContext) {
        trackingUrlMethod = TrackingUrlModelFingerprint.resultOrThrow().mutableMethod
    }

    internal fun hookTrackingUrl(
        descriptor: String
    ) = trackingUrlMethod.apply {
        val targetIndex = getTargetIndexWithMethodReferenceNameOrThrow("parse") + 1
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
}

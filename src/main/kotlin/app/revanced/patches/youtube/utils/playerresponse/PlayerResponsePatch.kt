package app.revanced.patches.youtube.utils.playerresponse

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.utils.fingerprints.PlayerParameterBuilderFingerprint

object PlayerResponsePatch : BytecodePatch(
    setOf(PlayerParameterBuilderFingerprint)
) {
    private lateinit var insertMethod: MutableMethod

    /**
     * Adds an invoke-static instruction, called with the new id when the video changes
     * @param methodDescriptor which method to call. Params have to be `Ljava/lang/String;`
     */
    internal fun injectCall(
        methodDescriptor: String
    ) {
        insertMethod.addInstructions(
            0, // move-result-object offset
            "invoke-static {p1}, $methodDescriptor"
        )
    }

    override fun execute(context: BytecodeContext) {

        PlayerParameterBuilderFingerprint.result?.let {
            insertMethod = it.mutableMethod
        } ?: throw PlayerParameterBuilderFingerprint.exception

    }

}


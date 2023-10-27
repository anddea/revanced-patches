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
    private const val VIDEO_ID_PARAMETER = 1
    private const val VIDEO_IS_OPENING_OR_PLAYING_PARAMETER = 11

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
            "invoke-static {p$VIDEO_ID_PARAMETER, p$VIDEO_IS_OPENING_OR_PLAYING_PARAMETER}, $methodDescriptor"
        )
    }

    override fun execute(context: BytecodeContext) {

        PlayerParameterBuilderFingerprint.result?.let {
            insertMethod = it.mutableMethod
        } ?: throw PlayerParameterBuilderFingerprint.exception

    }

}


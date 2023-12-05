package app.revanced.patches.youtube.utils.playerresponse

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.utils.fingerprints.PlayerParameterBuilderFingerprint
import app.revanced.util.exception
import java.io.Closeable

object PlayerResponsePatch : BytecodePatch(
    setOf(PlayerParameterBuilderFingerprint)
), Closeable, MutableSet<PlayerResponsePatch.Hook> by mutableSetOf() {
    private const val VIDEO_ID_PARAMETER = 1
    private const val PLAYER_PARAMETER = 3
    private const val IS_SHORT_AND_OPENING_OR_PLAYING_PARAMETER = 11

    private lateinit var playerResponseMethod: MutableMethod

    override fun execute(context: BytecodeContext) {
        playerResponseMethod = PlayerParameterBuilderFingerprint.result?.mutableMethod
            ?: throw PlayerParameterBuilderFingerprint.exception
    }

    override fun close() {
        fun hookVideoId(hook: Hook) = playerResponseMethod.addInstruction(
            0,
            "invoke-static {p$VIDEO_ID_PARAMETER, p$IS_SHORT_AND_OPENING_OR_PLAYING_PARAMETER}, $hook"
        )

        fun hookPlayerParameter(hook: Hook) = playerResponseMethod.addInstructions(
            0, """
                invoke-static {p$VIDEO_ID_PARAMETER, p$PLAYER_PARAMETER, p$IS_SHORT_AND_OPENING_OR_PLAYING_PARAMETER}, $hook
                move-result-object p$PLAYER_PARAMETER
                """
        )

        // Reverse the order in order to preserve insertion order of the hooks.
        val beforeVideoIdHooks = filterIsInstance<Hook.PlayerBeforeVideoId>().asReversed()
        val videoIdHooks = filterIsInstance<Hook.VideoId>().asReversed()
        val afterVideoIdHooks = filterIsInstance<Hook.PlayerParameter>().asReversed()

        // Add the hooks in this specific order as they insert instructions at the beginning of the method.
        afterVideoIdHooks.forEach(::hookPlayerParameter)
        videoIdHooks.forEach(::hookVideoId)
        beforeVideoIdHooks.forEach(::hookPlayerParameter)
    }

    internal abstract class Hook(private val methodDescriptor: String) {
        internal class VideoId(methodDescriptor: String) : Hook(methodDescriptor)

        internal class PlayerParameter(methodDescriptor: String) : Hook(methodDescriptor)
        internal class PlayerBeforeVideoId(methodDescriptor: String) : Hook(methodDescriptor)

        override fun toString() = methodDescriptor
    }
}


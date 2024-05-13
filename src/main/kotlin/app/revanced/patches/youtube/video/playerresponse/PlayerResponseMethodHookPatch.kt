package app.revanced.patches.youtube.video.playerresponse

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.video.playerresponse.fingerprint.PlayerParameterBuilderFingerprint
import app.revanced.util.resultOrThrow
import java.io.Closeable

object PlayerResponseMethodHookPatch :
    BytecodePatch(setOf(PlayerParameterBuilderFingerprint)),
    Closeable,
    MutableSet<PlayerResponseMethodHookPatch.Hook> by mutableSetOf() {

    // Parameter numbers of the patched method.
    private var PARAMETER_VIDEO_ID = 1
    private var PARAMETER_PLAYER_PARAMETER = 3
    private var PARAMETER_PLAYLIST_ID = 4
    private var PARAMETER_IS_SHORT_AND_OPENING_OR_PLAYING = 11

    private var freeRegister = 0
    private var shouldApplyNewMethod = false

    private lateinit var playerResponseMethod: MutableMethod

    override fun execute(context: BytecodeContext) {
        playerResponseMethod = PlayerParameterBuilderFingerprint.resultOrThrow().mutableMethod

        playerResponseMethod.apply {
            freeRegister = implementation!!.registerCount - parameters.size - 2
            shouldApplyNewMethod = freeRegister > 2
            if (shouldApplyNewMethod) {
                PARAMETER_IS_SHORT_AND_OPENING_OR_PLAYING = freeRegister
                PARAMETER_PLAYLIST_ID = freeRegister - 1
                PARAMETER_PLAYER_PARAMETER = freeRegister - 2
                PARAMETER_VIDEO_ID = freeRegister - 3
            }
        }
    }

    override fun close() {
        fun hookVideoId(hook: Hook) {
            val instruction =
                if (shouldApplyNewMethod)
                    "invoke-static {v$PARAMETER_VIDEO_ID, v$PARAMETER_IS_SHORT_AND_OPENING_OR_PLAYING}, $hook"
                else
                    "invoke-static {p$PARAMETER_VIDEO_ID, p$PARAMETER_IS_SHORT_AND_OPENING_OR_PLAYING}, $hook"

            playerResponseMethod.addInstruction(
                0, instruction
            )
        }

        fun hookPlayerParameter(hook: Hook) {
            val instruction =
                if (shouldApplyNewMethod)
                    """
                        invoke-static {v$PARAMETER_VIDEO_ID, v$PARAMETER_PLAYER_PARAMETER, v$PARAMETER_PLAYLIST_ID, v$PARAMETER_IS_SHORT_AND_OPENING_OR_PLAYING}, $hook
                        move-result-object p3
                        """
                else
                    """
                        invoke-static {p$PARAMETER_VIDEO_ID, p$PARAMETER_PLAYER_PARAMETER, p$PARAMETER_PLAYLIST_ID, p$PARAMETER_IS_SHORT_AND_OPENING_OR_PLAYING}, $hook
                        move-result-object p$PARAMETER_PLAYER_PARAMETER
                        """

            playerResponseMethod.addInstructions(
                0,
                instruction
            )
        }

        // Reverse the order in order to preserve insertion order of the hooks.
        val beforeVideoIdHooks = filterIsInstance<Hook.PlayerParameterBeforeVideoId>().asReversed()
        val videoIdHooks = filterIsInstance<Hook.VideoId>().asReversed()
        val afterVideoIdHooks = filterIsInstance<Hook.PlayerParameter>().asReversed()

        // Add the hooks in this specific order as they insert instructions at the beginning of the method.
        afterVideoIdHooks.forEach(::hookPlayerParameter)
        videoIdHooks.forEach(::hookVideoId)
        beforeVideoIdHooks.forEach(::hookPlayerParameter)

        if (shouldApplyNewMethod) {
            playerResponseMethod.addInstructions(
                0, """
                    move-object v$PARAMETER_VIDEO_ID, p1
                    move-object v$PARAMETER_PLAYER_PARAMETER, p3
                    move-object v$PARAMETER_PLAYLIST_ID, p4
                    move/from16 v$PARAMETER_IS_SHORT_AND_OPENING_OR_PLAYING, p11
                    """
            )
        }
    }

    internal abstract class Hook(private val methodDescriptor: String) {
        internal class VideoId(methodDescriptor: String) : Hook(methodDescriptor)

        internal class PlayerParameter(methodDescriptor: String) : Hook(methodDescriptor)
        internal class PlayerParameterBeforeVideoId(methodDescriptor: String) : Hook(methodDescriptor)

        override fun toString() = methodDescriptor
    }
}


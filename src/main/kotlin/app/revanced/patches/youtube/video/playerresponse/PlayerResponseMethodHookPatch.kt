package app.revanced.patches.youtube.video.playerresponse

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.video.playerresponse.fingerprint.PlayerParameterBuilderFingerprint
import app.revanced.util.resultOrThrow
import java.io.Closeable
import kotlin.properties.Delegates

object PlayerResponseMethodHookPatch :
    BytecodePatch(setOf(PlayerParameterBuilderFingerprint)),
    Closeable,
    MutableSet<PlayerResponseMethodHookPatch.Hook> by mutableSetOf() {

    // Parameter numbers of the patched method.
    private var PARAMETER_VIDEO_ID = 1
    private var PARAMETER_PLAYER_PARAMETER = 3
    private var PARAMETER_IS_SHORT_AND_OPENING_OR_PLAYING by Delegates.notNull<Int>()

    // Registers used to pass the parameters to integrations.
    private var playerResponseMethodCopyRegisters = false
    private lateinit var REGISTER_VIDEO_ID: String
    private lateinit var REGISTER_PLAYER_PARAMETER: String
    private lateinit var REGISTER_IS_SHORT_AND_OPENING_OR_PLAYING: String

    private lateinit var playerResponseMethod: MutableMethod
    private var numberOfInstructionsAdded = 0

    override fun execute(context: BytecodeContext) {
        playerResponseMethod = PlayerParameterBuilderFingerprint
            .resultOrThrow()
            .mutableMethod

        playerResponseMethod.apply {
            PARAMETER_IS_SHORT_AND_OPENING_OR_PLAYING = parameters.size - 2

            // On some app targets the method has too many registers pushing the parameters past v15.
            // If needed, move the parameters to 4-bit registers so they can be passed to integrations.
            playerResponseMethodCopyRegisters = implementation!!.registerCount -
                    parameterTypes.size + PARAMETER_IS_SHORT_AND_OPENING_OR_PLAYING > 15
        }

        if (playerResponseMethodCopyRegisters) {
            REGISTER_VIDEO_ID = "v0"
            REGISTER_PLAYER_PARAMETER = "v1"
            REGISTER_IS_SHORT_AND_OPENING_OR_PLAYING = "v2"
        } else {
            REGISTER_VIDEO_ID = "p$PARAMETER_VIDEO_ID"
            REGISTER_PLAYER_PARAMETER = "p$PARAMETER_PLAYER_PARAMETER"
            REGISTER_IS_SHORT_AND_OPENING_OR_PLAYING = "p$PARAMETER_IS_SHORT_AND_OPENING_OR_PLAYING"
        }
    }

    override fun close() {
        fun hookVideoId(hook: Hook) {
            playerResponseMethod.addInstruction(
                0,
                "invoke-static {$REGISTER_VIDEO_ID, $REGISTER_IS_SHORT_AND_OPENING_OR_PLAYING}, $hook"
            )
            numberOfInstructionsAdded++
        }

        fun hookPlayerParameter(hook: Hook) {
            playerResponseMethod.addInstructions(
                0, """
                    invoke-static {$REGISTER_VIDEO_ID, $REGISTER_PLAYER_PARAMETER, $REGISTER_IS_SHORT_AND_OPENING_OR_PLAYING}, $hook
                    move-result-object $REGISTER_PLAYER_PARAMETER
                    """
            )
            numberOfInstructionsAdded += 2
        }

        // Reverse the order in order to preserve insertion order of the hooks.
        val beforeVideoIdHooks = filterIsInstance<Hook.PlayerParameterBeforeVideoId>().asReversed()
        val videoIdHooks = filterIsInstance<Hook.VideoId>().asReversed()
        val afterVideoIdHooks = filterIsInstance<Hook.PlayerParameter>().asReversed()

        // Add the hooks in this specific order as they insert instructions at the beginning of the method.
        afterVideoIdHooks.forEach(::hookPlayerParameter)
        videoIdHooks.forEach(::hookVideoId)
        beforeVideoIdHooks.forEach(::hookPlayerParameter)

        if (playerResponseMethodCopyRegisters) {
            playerResponseMethod.apply {
                addInstructions(
                    0,
                    """
                        move-object/from16 $REGISTER_VIDEO_ID, p$PARAMETER_VIDEO_ID
                        move-object/from16 $REGISTER_PLAYER_PARAMETER, p$PARAMETER_PLAYER_PARAMETER
                        move/from16        $REGISTER_IS_SHORT_AND_OPENING_OR_PLAYING, p$PARAMETER_IS_SHORT_AND_OPENING_OR_PLAYING
                        """,
                )

                numberOfInstructionsAdded += 3

                // Move the modified register back.
                addInstruction(
                    numberOfInstructionsAdded,
                    "move-object/from16 p$PARAMETER_PLAYER_PARAMETER, $REGISTER_PLAYER_PARAMETER"
                )
            }
        }
    }

    internal abstract class Hook(private val methodDescriptor: String) {
        internal class VideoId(methodDescriptor: String) : Hook(methodDescriptor)

        internal class PlayerParameter(methodDescriptor: String) : Hook(methodDescriptor)
        internal class PlayerParameterBeforeVideoId(methodDescriptor: String) :
            Hook(methodDescriptor)

        override fun toString() = methodDescriptor
    }
}


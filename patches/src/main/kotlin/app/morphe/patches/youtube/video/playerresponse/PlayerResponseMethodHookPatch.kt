package app.morphe.patches.youtube.video.playerresponse

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.util.fingerprint.methodOrThrow
import kotlin.properties.Delegates

private val hooks = mutableSetOf<Hook>()

fun addPlayerResponseMethodHook(hook: Hook) {
    hooks += hook
}

// Parameter numbers of the patched method.
private var parameterVideoId = 1
private var parameterPlayerParameter = 3
private var parameterPlaylistId = 4
private var parameterIsShortAndOpeningOrPlaying by Delegates.notNull<Int>()

// Registers used to pass the parameters to the extension.
private var playerResponseMethodCopyRegisters = false
private lateinit var registerVideoId: String
private lateinit var registerPlayerParameter: String
private lateinit var registerPlaylistId: String
private lateinit var registerIsShortAndOpeningOrPlaying: String

private lateinit var playerResponseMethod: MutableMethod
private var numberOfInstructionsAdded = 0

val playerResponseMethodHookPatch = bytecodePatch(
    description = "playerResponseMethodHookPatch"
) {
    execute {
        playerResponseMethod = playerParameterBuilderFingerprint.second.methodOrNull
            ?: playerParameterBuilderLegacyFingerprint.methodOrThrow()

        playerResponseMethod.apply {
            val setIndex = parameterTypes.indexOfFirst { it == "Ljava/util/Set;" }
            val parameterSize = parameterTypes.size
            val relativeIndex =
                parameterTypes.subList(setIndex, parameterSize - 1).indexOfFirst { it == "Z" }

            // YouTube 18.29 ~ 19.22 : p11
            // YouTube 19.23 ~ 20.09 : p12
            // YouTube 20.10 ~ : p13
            parameterIsShortAndOpeningOrPlaying = setIndex + relativeIndex + 1
            // On some app targets the method has too many registers pushing the parameters past v15.
            // If needed, move the parameters to 4-bit registers so they can be passed to extension.
            playerResponseMethodCopyRegisters = implementation!!.registerCount -
                    parameterSize + parameterIsShortAndOpeningOrPlaying > 15
        }

        if (playerResponseMethodCopyRegisters) {
            registerVideoId = "v0"
            registerPlayerParameter = "v1"
            registerPlaylistId = "v2"
            registerIsShortAndOpeningOrPlaying = "v3"
        } else {
            registerVideoId = "p$parameterVideoId"
            registerPlayerParameter = "p$parameterPlayerParameter"
            registerPlaylistId = "p$parameterPlaylistId"
            registerIsShortAndOpeningOrPlaying = "p$parameterIsShortAndOpeningOrPlaying"
        }
    }

    finalize {
        fun hookVideoId(hook: Hook) {
            playerResponseMethod.addInstruction(
                0,
                "invoke-static {$registerVideoId, $registerIsShortAndOpeningOrPlaying}, $hook",
            )
            numberOfInstructionsAdded++
        }

        fun hookPlayerParameter(hook: Hook) {
            playerResponseMethod.addInstructions(
                0,
                """
                    invoke-static {$registerVideoId, $registerPlayerParameter, $registerPlaylistId, $registerIsShortAndOpeningOrPlaying}, $hook
                    move-result-object $registerPlayerParameter
                    """,
            )
            numberOfInstructionsAdded += 2
        }

        // Reverse the order in order to preserve insertion order of the hooks.
        val beforeVideoIdHooks =
            hooks.filterIsInstance<Hook.ProtoBufferParameterBeforeVideoId>().asReversed()
        val videoIdHooks = hooks.filterIsInstance<Hook.VideoId>().asReversed()
        val afterVideoIdHooks = hooks.filterIsInstance<Hook.PlayerParameter>().asReversed()

        // Add the hooks in this specific order as they insert instructions at the beginning of the method.
        afterVideoIdHooks.forEach(::hookPlayerParameter)
        videoIdHooks.forEach(::hookVideoId)
        beforeVideoIdHooks.forEach(::hookPlayerParameter)

        if (playerResponseMethodCopyRegisters) {
            playerResponseMethod.apply {
                addInstructions(
                    0,
                    """
                        move-object/from16 $registerVideoId, p$parameterVideoId
                        move-object/from16 $registerPlayerParameter, p$parameterPlayerParameter
                        move-object/from16 $registerPlaylistId, p$parameterPlaylistId
                        move/from16        $registerIsShortAndOpeningOrPlaying, p$parameterIsShortAndOpeningOrPlaying
                        """,
                )

                numberOfInstructionsAdded += 4

                // Move the modified register back.
                addInstruction(
                    numberOfInstructionsAdded,
                    "move-object/from16 p$parameterPlayerParameter, $registerPlayerParameter"
                )
            }
        }
    }
}

sealed class Hook(private val methodDescriptor: String) {
    class VideoId(methodDescriptor: String) : Hook(methodDescriptor)

    class PlayerParameter(methodDescriptor: String) : Hook(methodDescriptor)
    class ProtoBufferParameterBeforeVideoId(methodDescriptor: String) : Hook(methodDescriptor)

    override fun toString() = methodDescriptor
}

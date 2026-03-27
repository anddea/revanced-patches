package app.morphe.patches.music.video.playerresponse

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.music.utils.extension.sharedExtensionPatch
import app.morphe.patches.music.utils.playservice.is_7_03_or_greater
import app.morphe.patches.music.utils.playservice.versionCheckPatch
import app.morphe.util.fingerprint.methodOrThrow

private val hooks = mutableSetOf<Hook>()

fun addPlayerResponseMethodHook(hook: Hook) {
    hooks += hook
}

private const val REGISTER_VIDEO_ID = "p1"
private const val REGISTER_PLAYER_PARAMETER = "p3"
private const val REGISTER_PLAYLIST_ID = "p4"
private const val REGISTER_PLAYLIST_INDEX = "p5"

private lateinit var playerResponseMethod: MutableMethod
private var numberOfInstructionsAdded = 0

val playerResponseMethodHookPatch = bytecodePatch(
    description = "playerResponseMethodHookPatch"
) {
    dependsOn(
        sharedExtensionPatch,
        versionCheckPatch,
    )

    execute {
        playerResponseMethod = if (is_7_03_or_greater) {
            playerParameterBuilderFingerprint
        } else {
            playerParameterBuilderLegacyFingerprint
        }.methodOrThrow()
    }

    finalize {
        fun hookVideoId(hook: Hook) {
            playerResponseMethod.addInstruction(
                0,
                "invoke-static {$REGISTER_VIDEO_ID}, $hook",
            )
            numberOfInstructionsAdded++
        }

        fun hookVideoIdAndPlaylistId(hook: Hook) {
            playerResponseMethod.addInstruction(
                0,
                "invoke-static {$REGISTER_VIDEO_ID, $REGISTER_PLAYLIST_ID, $REGISTER_PLAYLIST_INDEX}, $hook",
            )
            numberOfInstructionsAdded++
        }

        fun hookPlayerParameter(hook: Hook) {
            playerResponseMethod.addInstructions(
                0,
                """
                    invoke-static {$REGISTER_VIDEO_ID, v0}, $hook
                    move-result-object v0
                    """,
            )
            numberOfInstructionsAdded += 2
        }

        // Reverse the order in order to preserve insertion order of the hooks.
        val beforeVideoIdHooks =
            hooks.filterIsInstance<Hook.ProtoBufferParameterBeforeVideoId>().asReversed()
        val videoIdHooks = hooks.filterIsInstance<Hook.VideoId>().asReversed()
        val videoIdAndPlaylistIdHooks =
            hooks.filterIsInstance<Hook.VideoIdAndPlaylistId>().asReversed()
        val afterVideoIdHooks = hooks.filterIsInstance<Hook.PlayerParameter>().asReversed()

        // Add the hooks in this specific order as they insert instructions at the beginning of the method.
        afterVideoIdHooks.forEach(::hookPlayerParameter)
        videoIdAndPlaylistIdHooks.forEach(::hookVideoIdAndPlaylistId)
        videoIdHooks.forEach(::hookVideoId)
        beforeVideoIdHooks.forEach(::hookPlayerParameter)

        playerResponseMethod.apply {
            addInstruction(
                0,
                "move-object/from16 v0, $REGISTER_PLAYER_PARAMETER"
            )
            numberOfInstructionsAdded++

            // Move the modified register back.
            addInstruction(
                numberOfInstructionsAdded,
                "move-object/from16 $REGISTER_PLAYER_PARAMETER, v0"
            )
        }
    }
}

sealed class Hook(private val methodDescriptor: String) {
    class VideoId(methodDescriptor: String) : Hook(methodDescriptor)
    class VideoIdAndPlaylistId(methodDescriptor: String) : Hook(methodDescriptor)

    class PlayerParameter(methodDescriptor: String) : Hook(methodDescriptor)
    class ProtoBufferParameterBeforeVideoId(methodDescriptor: String) : Hook(methodDescriptor)

    override fun toString() = methodDescriptor
}
